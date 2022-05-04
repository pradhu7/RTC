package com.apixio.useracct.cmdline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.XUUID;
import com.apixio.datasource.cassandra.*;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.util.DateUtil;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.dao.*;
import com.apixio.useracct.entity.*;
import com.apixio.utility.StringUtil;

/**
 * MigClean does both migration and data cleaning:  migration from pre-RBAC to
 * full role-based access control support AND props-based Projects to entity-based
 * projects, and cleaning users and organizations.
 */
public class MigClean extends CmdlineBase
{
    private static final String cUsage = ("Usage:  MigClean -c <conn-yamlfile> -f <data.yaml> -u <email-of-root> -m [test|change]\n\n");

    private ObjectMapper objectMapper = new ObjectMapper();

    // names of keys within the "organizations" yaml map:
    private static final String ORGMAP_NAME   = "name";
    private static final String ORGMAP_XUUID  = "xuuid";
    private static final String ORGMAP_ACTIVE = "active";

    // names of keys within the "users" yaml map:
    private static final String USERMAP_EMAIL    = "email";
    private static final String USERMAP_STATE    = "state";
    private static final String USERMAP_MEMBEROF = "member-of";

    // names of keys within the other yaml maps:
    private static final String PROJMAP_XUUID    = "xuuid";
    private static final String PDSMAP_XUUID     = "xuuid";
    private static final String PDSMAP_OWNINGORG = "owningOrg";
    private static final String PDSMAP_ACTIVE    = "active";


    /**
     * Key names in the snakeyaml-produced Map<String,String> that are NOT to be
     * processed as setting property values.
     */
    private static Set<String> PREDEF_ORGKEYS = new HashSet<>();
    static {
        PREDEF_ORGKEYS.add(ORGMAP_ACTIVE);
        PREDEF_ORGKEYS.add(ORGMAP_XUUID);
        PREDEF_ORGKEYS.add(ORGMAP_NAME);
    }

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       rootishID;
        String       connectionYamlFile;
        String       dataYamlFile;
        boolean      testMode;

        Options(String email, String connectionYaml, String dataYaml, boolean testMode)
        {
            this.rootishID          = email;
            this.connectionYamlFile = connectionYaml;
            this.dataYamlFile       = dataYaml;
            this.testMode           = testMode;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    /**
     * Contains the full snakeyaml-produced migration data from the data .yaml file.
     */
    public static class DataYaml {
        public List<Map<String, String>> propdefs;
        public List<Map<String, String>> organizations;
        public List<Map<String, String>> users;
        public List<Map<String, String>> projects;
        public List<Map<String, String>> patientDataSets;
        
        @Override
        public String toString()
        {
            return ("[data " +
                    "organizations=" + organizations +
                    "; users=" + users +
                    "; projects=" + projects +
                    "; pdss=" + patientDataSets +
                    "]");
        }
    }

    /**
     * We need to keep track of the projectID that is to be preserved across
     * migration from props-based to entity-based.  The base DTO holds all
     * the creation values except for this XUUID.
     */
    private static class MigHccProjectDTO extends HccProjectDTO {
        private String  projectID;

        @Override
        public String toString()
        {
            return ("<%" + super.toString() + "; projectID=" + projectID + "%>");
        }
    }


    // ################################################################

    private PrivSysServices   sysServices;

    // all of these map from ID (should be an XUUID) to the name=value fields defined in the .yaml file
    private Map<String, Map<String, String>> propdefs;
    private Map<String, Map<String, String>> orgsByGivenID;
    private Map<String, Map<String, String>> users;
    private Map<String, Map<String, String>> projects;
    private Map<String, Map<String, String>> patientDataSets;

    private boolean testMode;
    private XUUID   rootID;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options            options   = parseOptions(new ParseState(args));
        ConfigSet          config    = null;
        DataYaml           data     = null;

        if ((options == null) ||
            ((config = readConfig(options.connectionYamlFile)) == null) ||
            ((data = readDataYaml(options.dataYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        try
        {
            (new MigClean(options, config, data)).migrate();
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
        finally
        {
            System.exit(0);  // jedis or cql creates non-daemon thread.  boo
        }

    }

    private MigClean(Options options, ConfigSet config, DataYaml data) throws Exception
    {
        User root;

        this.sysServices = setupServices(config);

        if ((root = sysServices.getUsers().findUserByEmail(options.rootishID)) == null)
            throw new IllegalStateException("Migration requires an email address that has ROOT role.  Email [" + options.rootishID + "] can't be found");
        // 8/31/2016:  retired the use of UserLogic.isRoot() as all paths into modifying a user test
        //  for access rights via calling UserLogic.getUsersByManageUserPermission:
        //        else if (!sysServices.getUserLogic().isRoot(root.getID()))
        //            throw new IllegalStateException("Migration requires an email address that has ROOT role.  Email [" + options.rootishID + "] is for a non-root user");
        
        this.rootID = root.getID();

        this.users           = createMap(data.users,           USERMAP_EMAIL);
        this.projects        = createMap(data.projects,        PROJMAP_XUUID);
        this.patientDataSets = createMap(data.patientDataSets, PDSMAP_XUUID);

        createPropdefsMaps(data.propdefs);
        createOrgsMaps(data.organizations);

        this.testMode = options.testMode;
    }

    private void createPropdefsMaps(List<Map<String, String>> data)
    {
        propdefs = new HashMap<String, Map<String, String>>();

        for (Map<String, String> defMap : data)
        {
            String id = null;

            for (Map.Entry<String, String> entry : defMap.entrySet())
            {
                if (entry.getValue() == null)
                {
                    if (id != null)
                        error(new IllegalStateException(".yaml propdef already has a given ID"),
                              "Propdef [{}] already has id of [{}]; ignoring this organization", defMap, id);

                    id = entry.getKey();
                }
            }

            if (id == null)
                warning(".yaml propdef [{}] doesn't have a given ID defined (null value marks the given ID); ignoring this propdef", defMap);
            else if (propdefs.get(id) != null)
                warning(".yaml propdef [{}] has a given ID [{}] that has already been used; ignoring this propdef", defMap, id);
            else
                propdefs.put(id, defMap);
        }
    }

    private void createOrgsMaps(List<Map<String, String>> data)
    {
        // "given id" will be the key of the entry whose value is null
        // orgsByGivenID is:  givenID => (attr => value)
        orgsByGivenID = new HashMap<String, Map<String, String>>();

        for (Map<String, String> orgMap : data)
        {
            String id  = null;

            for (Map.Entry<String, String> entry : orgMap.entrySet())
            {
                if (entry.getValue() == null)
                {
                    Object okey = entry.getKey();

                    if (id != null)
                        error(new IllegalStateException(".yaml organization already has a given ID"),
                              "Organization [{}] already has id of [{}]; ignoring this organization", orgMap, id);

                    // this pattern is kinda stupid but it's required as snakeYaml will automatically
                    // convert number-like strings to actual number objects even though the Map is
                    // declare Map<String,String> (type erasure at work here).  This avoids a ClassCastException
                    if (!(okey instanceof String))
                        id = okey.toString();
                    else
                        id = (String) okey;
                }
            }

            if (id == null)
                warning(".yaml organization [{}] doesn't have a given ID defined (null value marks the given ID); ignoring this organization", orgMap);
            else if (orgsByGivenID.get(id) != null)
                warning(".yaml organization [{}] has a given ID [{}] that has already been used; ignoring this organization", orgMap, id);
            else
                orgsByGivenID.put(id, orgMap);
        }
    }

    /**
     * All yaml data config is a list of maps, where each map instance has a key=value
     * entry that basically identifies the primary key of that entity.  This method
     * reads through the list and creates a map from the primary key (whose name is
     * given in the "key" method arg) to the map itself.  This allows looking up
     * the map given its primary key.
     */
    private Map<String, Map<String, String>> createMap(List<Map<String, String>> raw, String key)
    {
        Map<String, Map<String, String>> map = new HashMap<>();

        if (raw != null)
        {
            for (Map<String, String> entry : raw)
            {
                String kval = entry.get(key);

                if (kval == null)
                    warning("Entry [{}] has no field with the key name of [{}] and will not be referenceable", entry, key);
                else
                    map.put(kval, entry);
            }
        }

        return map;
    }

    private void beginTrans() throws Exception
    {
        sysServices.getRedisTransactions().begin();
        ((CqlTransactionCrud) sysServices.getCqlCrud()).beginTransaction();
    }
    private void commitTrans() throws Exception
    {
        sysServices.getRedisTransactions().commit();
        ((CqlTransactionCrud) sysServices.getCqlCrud()).commitTransaction();
    }
    private void abortTrans() throws Exception
    {
        sysServices.getRedisTransactions().abort();
        ((CqlTransactionCrud) sysServices.getCqlCrud()).abortTransaction();
    }

    private void migrate() throws Exception
    {
        info("Beginning migration");

        migratePropdefs();
        migrateOrganizations(); // before users and customers as they reference organizations
        migrateUsers();
        migrateOldCustomers();  // do this before migrateOldProjects as that requires that we can get owningOrg from PDS
        migrateOldProjects();
    }

    private void migratePropdefs() throws Exception
    {
        OrganizationLogic  ol  = sysServices.getOrganizationLogic();
        ProjectLogic       pl  = sysServices.getProjectLogic();

        // propdefs is something like "Organizations" => (propname => type, ...)

        try
        {
            info("========== Migrating Propdefs");

            beginTrans();

            for (Map.Entry<String, Map<String, String>> defMap : propdefs.entrySet())
            {
                String entityType = defMap.getKey();

                if (!entityType.equals("Organization"))
                {
                    warning("Propdefs are supported only for Organization.  Skipping propdef [{}]", entityType);
                }
                else
                {
                    for (Map.Entry<String, String> propdef : defMap.getValue().entrySet())
                    {
                        String propName = propdef.getKey();
                        String propType = propdef.getValue();

                        if (propType != null)   // main key (which is either "Organization" or "Project" won't have a type
                        {
                            info("Creating propdef on [{}] with name [{}] and type [{}]", entityType, propName, propType);

                            if (entityType.equals("Organization"))
                            {
                                if (ol.getPropertyDefinitions(false).get(propName) != null)
                                {
                                    warning("Organization already has a propdef with name [{}]; type has NOT been checked that it is the same as specified [{}].  Skipping add propdef", propName, propType);
                                }
                                else
                                {
                                    info("Adding Organization propdef with name [{}] and type [{}]", propName, propType);
                                    if (!testMode)
                                        ol.addPropertyDef(propName, propType);
                                }
                            }
                        }
                    }
                }
            }

            commitTrans();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            abortTrans();

            throw x;
        }
    }

    private void migrateOrganizations() throws Exception
    {
        OrganizationLogic  ol  = sysServices.getOrganizationLogic();
        Organizations      os  = sysServices.getOrganizations();
        List<Organization> all = os.getAllOrganizations(true);

        try
        {
            info("========== Migrating Organizations");

            beginTrans();

            if (orgsByGivenID.size() == 0)
                warning("No organizations to be migrated");

            for (Map.Entry<String, Map<String, String>> orgMaps : orgsByGivenID.entrySet())
            {
                String              given  = orgMaps.getKey();
                Map<String, String> orgMap = orgMaps.getValue();
                String              name   = orgMap.get(ORGMAP_NAME);
                String              xid    = orgMap.get(ORGMAP_XUUID);

                if ((name == null) && (xid == null))
                {
                    warning("Organization [{}] is missing 'name' and 'xuuid' fields in .yaml file so it can't be looked up.  Skipping", given);
                }
                else
                {
                    Organization orgByName = (name != null) ? findOrgByName(all, name) : null;
                    Organization orgByID   = (xid  != null) ? findOrgByID(all, xid)    : null;

                    if ((orgByName == null) && (orgByID == null))
                    {
                        warning("Organization [{}] can't be found by its name ({}) or xuuid ({})", given, name, xid);
                    }
                    else if (!equalOrgs(orgByName, orgByID))
                    {
                        warning("Organization mismatch:  both name ({}) and xuuid ({}) were given but they refer to different orgs", name, xid);
                    }
                    else
                    {
                        String        active = orgMap.get(ORGMAP_ACTIVE);
                        Organization  org    = (orgByName != null) ? orgByName : orgByID;

                        orgMap.put(ORGMAP_XUUID, org.getID().toString());

                        warning("Migrating organization [{}] (id={})", given, org.getID());

                        if (active != null)
                        {
                            if (!testMode)
                            {
                                org.setIsActive(Boolean.valueOf(active));
                                ol.updateOrganization(org);
                            }
                        }

                        // now handle custom props
                        for (Map.Entry<String, String> entry : orgMap.entrySet())
                        {
                            Object okey  = entry.getKey();
                            String key   = (okey instanceof String) ? ((String) okey) : okey.toString();
                            String value = entry.getValue();

                            if ((value != null) && !PREDEF_ORGKEYS.contains(key))
                            {
                                if (ol.getPropertyDefinitions(false).get(key) == null)
                                {
                                    warning("Attempt to add custom property {}={} to organization [{}] but propdef for it doesn't exist", key, value, given);
                                }
                                else
                                {
                                    info("Adding custom property {}={} to organization {}", key, value, given);

                                    if (!testMode)
                                    {
                                        try
                                        {
                                            ol.setOrganizationProperty(org, key, value);
                                        }
                                        catch (Exception x)
                                        {
                                            error(x, "Unable to set property {}={} on org [{}]", key, value, given);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            commitTrans();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            abortTrans();

            throw x;
        }
    }

    private boolean equalOrgs(Organization o1, Organization o2)
    {
        if ((o1 == null) || (o2 == null))
            return true;
        else
            return o1.getID().equals(o2.getID());
    }

    private Organization findOrgByName(List<Organization> all, String name)
    {
        Organization org = null;

        name = Organization.normalizeName(name);

        for (Organization o : all)
        {
            if (name.equals(o.normalizeName(o.getName())))
            {
                if (org != null)
                    throw new IllegalStateException("Multiple organizations found with name [" + name + "]");

                org = o;;
            }
        }

        return org;
    }

    private Organization findOrgByID(List<Organization> all, String xid)
    {
        for (Organization o : all)
        {
            if (xid.equals(o.getID().toString()))
                return o;
        }

        return null;
    }

    private void migrateUsers() throws Exception
    {
        Users             userDao = sysServices.getUsers();
        UserLogic         ul      = sysServices.getUserLogic();
        OrganizationLogic ol      = sysServices.getOrganizationLogic();
        boolean           badUser = false;
        List<User>        allUsers;

        // we can mark them as CLOSED and we can assign them to an organization

        // do NOT use transactions here as there are reads from redis/cassandra that
        // need the latest.

        info("========== Migrating Users");

        if (users.size() == 0)
            warning("No users to be migrated");

        for (Map<String, String> userMap : users.values())
        {
            String email = userMap.get(USERMAP_EMAIL);
            User   user  = ul.getUserByEmail(rootID, email, false);  // 'false' is pushing it but this code is not needed for production now (all was migrated spring 2016)

            if (user == null)
            {
                warning("Attempt to locate user by email address [{}] failed.  " +
                        "This could be due to an unknown address or because caller has no" +
                        " permission on that user's organization.", email);
            }
            else
            {
                String state    = userMap.get(USERMAP_STATE);
                String memberOf = userMap.get(USERMAP_MEMBEROF);

                info("Migrating user [{}]:  state={}, member-of={}", email, state, memberOf);

                if (state != null)
                {
                    try
                    {
                        AccountState as = AccountState.valueOf(state);

                        if (!testMode)
                        {
                            user.setState(as);
                            userDao.update(user);
                        }
                    }
                    catch (Exception x)
                    {
                        error(x, "Unable to set account state for user [{}] to [{}]", email, state);
                    }
                }

                if (memberOf != null)
                {
                    Organization userOrg = ol.getUsersOrganization(user.getID());
                    XUUID        moOrgID = getOrgXuuidFromGivenName(memberOf);

                    if (moOrgID == null)
                    {
                        warning("Unable to add user [{}] to be member-of org [{}] as that org can't be found", email, memberOf);
                    }
                    else
                    {
                        if (userOrg != null)
                            info("User [{}] currently member-of [{}]", email, userOrg);

                        info("Setting user [{}] to member-of [{}]", email, memberOf);

                        if (!testMode)
                        {
                            if (userOrg != null)
                                ol.removeMemberFromOrganization(userOrg.getID(), user.getID());

                            ol.addMemberToOrganization(moOrgID, user.getID());
                        }
                    }
                }
            }
        }

        // now make sure that all users are either CLOSED or have a place to call home.
        allUsers = ul.getUsersByManageUserPermission(rootID, null, null, AccountState.CLOSED);

        info("Number of CLOSED     users in system:  {}", ul.getUsersByManageUserPermission(rootID, null, AccountState.CLOSED, null).size());
        info("Number of non-CLOSED users in system:  {}", allUsers.size());

        for (User user : allUsers)
        {
            List<Organization> orgs = ol.getUsersOrganizations(user.getID());

            if (orgs.size() == 0)
            {
                warning("User {} (id={}) is not associated with an organization", user.getEmailAddress(), user.getID());
                badUser = true;
            }
            else if (orgs.size() > 1)
            {
                warning("User {} is associated with more than one organization!  {}", user.getEmailAddress(), orgs);
                badUser = true;
            }
        }

        if (!testMode && badUser)
            throw new IllegalStateException("At least one non-CLOSED user isn't associated with an Organization");
    }

    private void migrateOldCustomers() throws Exception
    {
        PatientDataSetLogic pdsLogic = sysServices.getPatientDataSetLogic();
        PatientDataSets     pdsDao   = sysServices.getPatientDataSets();

        info("========== Migrating Old Customers (new PatientDataSets)");

        try
        {
            beginTrans();

            // put in unassociated list first (only if not owned)
            for (PatientDataSet pds : pdsLogic.getAllPatientDataSets(false))
            {
                if (pds.getOwningOrganization() == null)
                {
                    info("  PDS {} is not owned", pds.getID());
                    pdsDao.disownPds(pds.getID());
                }
                else
                {
                    info("  PDS {} is owned by {}", pds.getID(), pds.getOwningOrganization());
                }
            }

            commitTrans();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            abortTrans();

            throw x;
        }

        try
        {
            beginTrans();

            info("Number of unowned PatientDataSets: {}", pdsLogic.getUnassociatedPds().size());

            if (patientDataSets.size() == 0)
                warning("No old customers to be migrated");

            for (Map.Entry<String, Map<String, String>> entry : patientDataSets.entrySet())
            {
                String              customerID = entry.getKey();
                Map<String, String> map        = entry.getValue();
                String              owningOrg  = map.get(PDSMAP_OWNINGORG);
                PatientDataSet      pds        = pdsLogic.getPatientDataSetByID(XUUID.fromString(customerID));
                Map<String, String> owner;

                if (pds == null)
                {
                    warning("Unable to Process customer/PDS [{}] as it weirdly can't be found in new system", customerID);
                }
                else if (strEmpty(owningOrg))
                {
                    warning("Migration of old customer/PDS [{}] requires data config to declare 'owningOrg'", map);
                }
                else if ((owner = orgsByGivenID.get(owningOrg)) == null)
                {
                    warning("Unable to migrate customer [{}] as .owningOrg [{}] can't be found in .yaml organizations list", customerID, owningOrg);
                }
                else
                {
                    String active = map.get(PDSMAP_ACTIVE);

                    info("Migrating old customer/PDS [{}]", customerID);

                    if (!testMode)
                    {
                        if (!strEmpty(active))
                            pds.setActive(Boolean.valueOf(active));

                        pdsLogic.associatePdsToID(pds.getID(), XUUID.fromString(owner.get(ORGMAP_XUUID)));

                        try
                        {
                            pdsLogic.updatePatientDataSet(pds);
                        }
                        catch (Exception x)
                        {
                            error(x, "Failure to update PDS [{}]", pds);
                        }
                    }
                }
            }

            commitTrans();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            abortTrans();

            throw x;
        }
    }

    /**
     * Hardest migration as we need to read in custom property-based project data and
     * create real project entities.
     */
    private void migrateOldProjects() throws Exception
    {
        Map<XUUID, String>     oldProjects = loadOldProjects();  // map from CustomerID (now PdsID) to JSON project def
        List<MigHccProjectDTO> newProjects = new ArrayList<>();
        Set<String>            desired     = new HashSet<>();
        int                    projCount   = 0;

        try
        {
            info("========== Migrating Old Projects");

            beginTrans();

            if (oldProjects.size() == 0)
                warning("No old projects to be migrated");

            // we need to invert the map key so that we are keyed by project XUUID
            // and not the CustomerID.
            for (Map.Entry<XUUID, String> entry : oldProjects.entrySet())
            {
                List<MigHccProjectDTO> oldDtos =  convertProjectToDto(entry.getValue());
                XUUID                  pdsID   = entry.getKey();

                for (MigHccProjectDTO dto : oldDtos)
                {
                    if (dto.patientDataSetID == null)  // this shouldn't ever happen...
                        dto.patientDataSetID = pdsID;
                    else if (!dto.patientDataSetID.equals(pdsID))
                        warning("PdsID mismatch (internal vs key):  {} :: {}", dto.patientDataSetID, pdsID);

                    projCount++;

                    // add to the new projects list only if we're supposed to migrate
                    if (projects.get(dto.projectID) != null)
                        newProjects.add(dto);
                }
            }

            info("Loaded {} old projects", projCount);

            // do a set diff so we can complain about projects that are wanting to be migrated that we can't find
            for (String id : projects.keySet())
                desired.add(id);

            for (MigHccProjectDTO newp : newProjects)
                desired.remove(newp.projectID);

            if (desired.size() > 0)
            {
                warning("Yaml configuration includes ProjectIDs to migrate that aren't in redis:");
                for (String id : desired)
                    warning("  {}", id);
            }

            // fill in the owning orgID from PDS
            for (MigHccProjectDTO newp : newProjects)
            {
                Map<String, String>  pds = patientDataSets.get(newp.patientDataSetID.toString());

                if (pds == null)
                    warning("Unable to get owningOrganizationID for project [{}] because it's referencing an unknown/unconfigured PatientDataSet [{}]", newp.projectID, newp.patientDataSetID);
                else
                    newp.organizationID = getOrgXuuidFromGivenName(pds.get(PDSMAP_OWNINGORG));
            }

            info("Migrating {} project(s) out of {} total in Customer entities:", newProjects.size(), projCount);

            if (testMode)
            {
                for (MigHccProjectDTO newp : newProjects)
                    validateProjDto(newp);
            }
            else
            {
                ProjectLogic pl = sysServices.getProjectLogic();

                pl.getAllProjects();  // caches all

                for (MigHccProjectDTO newp : newProjects)
                {
                    try
                    {
                        XUUID projID = XUUID.fromString(newp.projectID);

                        if (pl.getProjectByID(projID) != null)
                        {
                            warning("Skipping migration of project [{}] as it's already been migrated.", projID);
                        }
                        else if (validateProjDto(newp))
                        {
                            info("Migrating project [{}].", projID);
                            pl.createHccProject(XUUID.fromString(newp.projectID), newp);  // creates with a forced XUUID
                        }
                    }
                    catch (Exception x)
                    {
                        error(x, "While trying to create project [{}]", newp);
                    }
                }
            }

            commitTrans();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            abortTrans();

            throw x;
        }
    }

    /**
     * Utility to look up orgization XUUID given the "given name" (i.e., what's in the data .yaml file)
     */
    private XUUID getOrgXuuidFromGivenName(String name)
    {
        Map<String, String> org = orgsByGivenID.get(name);

        if (org != null)
            return XUUID.fromString(org.get(ORGMAP_XUUID));
        else
            return null;
    }

    /**
     * Loads up ALL projects by reading in all (old) Customer entities and getting the
     * custom property that holds project JSON off of them.  Note that there is nothing
     * other than what's in the JSON itself that has the actual Project XUUID so we
     * need to restore them all anyway in order to be able to migrate even one.
     *
     * Since this is a low-level load, we leave it keyed by the CustomerID
     */
    private Map<XUUID, String> loadOldProjects() throws IOException  // CustomerID -> JSON Project
    {
        PatientDataSetLogic pdsLogic = sysServices.getPatientDataSetLogic();
        Map<XUUID, Object>  loaded   = new HashMap<>();
        Map<XUUID, String>  old      = new HashMap<>();

        if (pdsLogic.getPropertyDefinitions(false).get("projects") == null)
            pdsLogic.addPropertyDef("projects", PropertyType.STRING);

        loaded = pdsLogic.getPatientDataSetsProperty("projects");  // "projects" define in old CustomerRS.PROJECTS
        // projects map is keyed by PDS ID
        // each value is a JSON string; "id" field of value is XUUID of project (CP_ prefix)

        for (Map.Entry<XUUID, Object> entry : loaded.entrySet())
            old.put(entry.getKey(), (String) entry.getValue());

        //        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> old projects:");
        //        for (String json : old.values())
        //            System.out.println("\n" + json);
        //        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

        return old;
    }

    /*
     * Old Project => New Project
     *
     *  "name"                                               => name in constructor
     *  "customerId"                                         => pdsID in constructor
     *                                                       => orgID:  Organization[customerId.owningOrganization]; used in constructor
     *                                                       => projClass; hardcode to ProjectClass.HCC in constructor
     *                                                       => type:  one of Type.{HCC, TEST, SCIENCE}; hardcode or use data.yaml
     *
     *  "dosStart" (2015-01-01T08:00:00.000Z)                => setDosStart(Date)
     *  "dosEnd"   (2015-01-01T08:00:00.000Z)                => setDosEnd(Date)          ! saw dd/mm/yyyy on staging data
     *  "paymentYear"                                        => setPaymentYear(String)
     *  "sweep ["midYear", "initial", "finalReconciliation"] => setSweep(Sweep)
     *  "passType" ["First Pass", "Second Pass"]             => setPassType(PassType)
     *  "rawRaf" (double as string)                          => setRawRaf(Double)
     *  "raf" (double as string)                             => setRaf(Double)
     *  "budget" (double as string)                          => setBudget(Double)
     *  "deadline" 2015-01-01T08:00:00.000Z)                 => setDeadline(Date)
     *  "dataSource"                                         => setDataSource(String)
     *  "patientList"                                        => setPatientList(String)
     *  "docFilterList"                                      => setDocFilterList(String) ? no examples in staging data
     *  "state" [Bundled, New, Paused] =>                    => setState(State)          ! extra "Paused" enum
     *
     *  "status" [New]                                       =>                          ! only "New" on staging data; new code has type 'boolean'
     *
     */

    private List<MigHccProjectDTO> convertProjectToDto(String json) throws IOException
    {
        JsonNode               root = objectMapper.readTree(json);
        List<MigHccProjectDTO> dtos;

        if (!root.isArray())
            throw new IllegalStateException("Fatal error:  JSON retrieved is not a JSON array:  " + json);

        dtos = new ArrayList<MigHccProjectDTO>();

        for (Iterator<JsonNode> iter = ((ArrayNode) root).elements(); iter.hasNext(); )
        {
            MigHccProjectDTO dto  = new MigHccProjectDTO();
            String           id;
            String           where;

            root = iter.next();

            id = findString(root, "id");

            dto.projectID = id;

            where = "Old projectID " + id;

            // Needed for construction
            dto.name             = findString(root, "name");
            dto.type             = getProjType(getProjectField(root, id, "type"));
            dto.patientDataSetID = getProjPds(getProjectField(root, id, "customerId"), where);
            // NOTE:  we can't get organizationID yet

            // Optional:
            dto.dosStart      = parseDate(getProjectField(root, id, "dosStart"));
            dto.dosEnd        = parseDate(getProjectField(root, id, "dosEnd"));
            dto.paymentYear   = getProjectField(root, id, "paymentYear");
            dto.sweep         = getProjSweep(getProjectField(root, id, "sweep"));
            dto.passType      = getProjPassType(getProjectField(root, id, "passType"));

            //        dto.status           = getProjStatus(findString(root, "status"));
            dto.state         = getProjState(findString(root, "state"));

            dto.rawRaf        = parseDouble(getProjectField(root, id, "rawRaf"),   where + " field rawRa");
            dto.raf           = parseDouble(getProjectField(root, id, "raf"),      where + " field raf");
            dto.budget        = parseDouble(getProjectField(root, id, "budget"),   where + "field budget");
            dto.deadline      = parseDate(getProjectField(root, id, "deadline"));
            dto.datasource    = getProjectField(root, id, "dataSource");
            dto.patientList   = getProjectField(root, id, "patientList");
            dto.docFilterList = getProjectField(root, id, "docFilterList");

            // fixup for at least staging data:
            if ((dto.dosStart != null) && (dto.dosEnd != null) && dto.dosEnd.before(dto.dosStart))
                dto.dosEnd = dto.dosStart;

            dtos.add(dto);
        }

        return dtos;
    }

    private boolean validateProjDto(HccProjectDTO dto)
    {
        boolean valid = true;
        
        if (dto.name == null)             { valid = false; warning("Project DTO is missing name:  [{}]", dto); }
        if (dto.type == null)             { valid = false; warning("Project DTO is missing type:  [{}]", dto); }
        if (dto.patientDataSetID == null) { valid = false; warning("Project DTO is missing pds:   [{}]", dto); }
        if (dto.organizationID == null)   { valid = false; warning("Project DTO is missing org:   [{}]", dto); }

        return valid;
    }

    /**
     * Project helper functions
     */

    private String findString(JsonNode root, String nodeName)
    {
        JsonNode node = root.findValue(nodeName);

        if (node != null)
            return node.asText();
        else
            return null;
    }

    private static boolean strEmpty(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

    private String getProjectField(JsonNode root, String projID, String field)
    {
        Map<String, String> projMap = projects.get(projID);
        String              value   = null;

        if (projMap != null)
            value = projMap.get(field);

        return (value != null) ? value : findString(root, field);
    }

    private Date parseDate(String date)        // allow for either full ISO8601 (2015-01-01T08:00:00.000Z) or mm/dd/yyyy
    {
        if (strEmpty(date))
            return null;

        Date dt = DateUtil.validateIso8601(date);  // handles ISO8601 w/ and w/o .000 (milliseconds)

        if (dt == null)
            dt = DateUtil.validateMMDDYYYY(date);

        return dt;
    }

    private Double parseDouble(String db, String fromWhere)
    {
        if (strEmpty(db))
            return null;

        try
        {
            return Double.parseDouble(db);
        }
        catch (Exception x)
        {
            warning("Expected string form of double but got [" + db + "]; location [{}]", fromWhere);

            return null;
        }
    }

    private XUUID getProjPds(String id, String fromWhere)
    {
        if (strEmpty(id))
            return null;

        try
        {
            return XUUID.fromString(id);
        }
        catch (Exception x)
        {
            warning("Invalid XUUID format [{}]; location [{}]", id, fromWhere);
            return null;
        }
    }
    private HccProject.Type getProjType(String type)  // overriding supposedly not needed
    {
        return (!strEmpty(type)) ? HccProject.Type.valueOf(type) : HccProject.Type.HCC;
    }

    private HccProject.State getProjState(String jsonValue)
    {
        if (strEmpty(jsonValue))
            return null;
        else if (jsonValue.equalsIgnoreCase("ready") || jsonValue.equalsIgnoreCase("reopen") || jsonValue.equalsIgnoreCase("new"))
            return HccProject.State.NEW;
        else if (jsonValue.equalsIgnoreCase("bundled"))
            return HccProject.State.BUNDLED;
        else if (jsonValue.equalsIgnoreCase("done") || jsonValue.equalsIgnoreCase("completed"))
            return HccProject.State.COMPLETED;

        /*
        if (strEmpty(jsonValue))
            return null;
        else if (jsonValue.equalsIgnoreCase("new"))
            return HccProject.State.NEW;
        else if (jsonValue.equalsIgnoreCase("bundled"))
            return HccProject.State.BUNDLED;
        else if (jsonValue.equalsIgnoreCase("completed"))
            return HccProject.State.COMPLETED;
        */

        warning("Invalid State value [{}]", jsonValue);
        return null;
    }

    private HccProject.Sweep getProjSweep(String jsonValue)
    {
        if (strEmpty(jsonValue))
            return null;
        else if (jsonValue.equalsIgnoreCase("initial"))
            return HccProject.Sweep.INITIAL;
        else if (jsonValue.equalsIgnoreCase("midYear"))
            return HccProject.Sweep.MID_YEAR;
        else if (jsonValue.equalsIgnoreCase("finalReconciliation"))
            return HccProject.Sweep.FINAL_RECONCILIATION;

        warning("Invalid Sweep value [{}]", jsonValue);
        return null;
    }

    private HccProject.PassType getProjPassType(String jsonValue)
    {
        if (strEmpty(jsonValue))
            return null;
        else if (jsonValue.equalsIgnoreCase("First Pass"))
            return HccProject.PassType.FIRST_PASS;
        else if (jsonValue.equalsIgnoreCase("Second Pass"))
            return HccProject.PassType.SECOND_PASS;

        warning("Invalid PassType value [{}]", jsonValue);
        return null;
    }

    /**
     *
     */
    private void info(String fmt, Object... args)
    {
        System.out.println("INFO:   " + StringUtil.subargsPos(fmt, args));
    }
    private void warning(String fmt, Object... args)
    {
        System.out.println("WARN:   " + StringUtil.subargsPos(fmt, args));
    }
    private void error(Exception x, String fmt, Object... args)
    {
        System.out.println("ERROR:  " + StringUtil.subargsPos(fmt, args));
        x.printStackTrace();
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  data       = ps.getOptionArg("-f");
        String  userEmail  = ps.getOptionArg("-u");
        String  mode       = ps.getOptionArg("-m");

        if (strEmpty(connection) || strEmpty(data) || strEmpty(mode) || strEmpty(userEmail) ||
            !(mode.equals("test") || mode.equals("change")))
            return null;

        return new Options(userEmail, connection, data, mode.equals("test"));
    }

    /**
     * Read the yaml configuration file and populate the Config object from it.
     */
    private static DataYaml readDataYaml(String filepath) throws Exception
    {
        InputStream   input  = new FileInputStream(new File(filepath));
        Yaml          yaml   = new Yaml();
        Object        data   = yaml.loadAs(input, DataYaml.class);

        input.close();

        return (DataYaml) data;
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
