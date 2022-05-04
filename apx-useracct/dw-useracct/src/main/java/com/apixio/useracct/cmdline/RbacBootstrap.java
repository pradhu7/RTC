package com.apixio.useracct.cmdline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.XUUID;
import com.apixio.aclsys.dao.*;
import com.apixio.aclsys.entity.*;
//import com.apixio.datasource.cassandra.*;
import com.apixio.datasource.redis.*;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.dao.*;
import com.apixio.useracct.entity.*;

/**
 * RbacBootstrap is a NON-HTTP-CLIENT tool to bootstrap a system to the point
 * where role-based access control has been set up to the degree that the RESTful
 * API can be used.
 *
 * This tool *MUST* be run on a machine that has direct access into Redis as it
 * uses the user-account entities, DAOs, etc., to bootstrap.  This is differenbt
 * from the the command line tools that use the RESTful APIs.
 *
 * Bootstrapping for RBAC requires the following steps to be done:
 *
 *  1.  creating a RoleSet (call it "System" RoleSet) that will hold the
 *      "ROOT" role and that will be used by an organization type.
 *
 *  2.  creating an OrgType (call it "System" OrgType that references the
 *      System RoleSet and that will be used by a real Organization
 *      instance (call it the "Apixio" organization)
 *
 *  3.  creating or using a real User that will become a member-of the
 *      Apixio organization
 *
 *  4.  assigning the User the System/ROOT role within the Apixio
 *      organization.
 *
 * Given the appropriate privileges in the System/ROOT role and given the
 * right API ACLs, this User can then make RESTful API calls to set up
 * the rest of the system as needed.
 *
 * The above information is specified via a .yaml file, specified by a commandline
 * argument.
 */
public class RbacBootstrap extends CmdlineBase
{
    private static final String cUsage = ("Usage:  RbacBootstrap -c <conn-yamlfile> -b <bootstrap-yamlfile>\n\n");

    /**
     * The option/commands supported.
     */
    private static final String BOOTSTRAP_OPT   = "-b";
    private static final String ROOTUSER_OPT    = "-u";

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       bootstrapYamlFile;
        String       rootUserEmail;

        Options(String connectionYaml, String bootstrapYaml, String rootUserEmail)
        {
            this.connectionYamlFile = connectionYaml;
            this.bootstrapYamlFile  = bootstrapYaml;
            this.rootUserEmail      = rootUserEmail;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "; bootstrapYaml=" + bootstrapYamlFile +
                    "; rootUserEmail=" + rootUserEmail +
                    "]");
        }
    }

    // ################################################################

    private static PrivSysServices   sysServices;

    private static OrgTypes          orgTypes;
    private static OrganizationLogic orgLogic;
    private static Organizations     organizations;
    private static PrivOperations    operations;
    private static PrivRoleLogic     roleLogic;
    private static RoleSetLogic      roleSetLogic;
    private static RoleSets          roleSets;
    private static Transactions      transactions;
    private static Users             users;

    private static Map<String, User> idToUser = new HashMap<>();

    private static XUUID rootRoledUserID;
    private static Map<String, Organization> orgMap  = new HashMap<>();

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options              options   = parseOptions(new ParseState(args));
        ConfigSet            config    = null;
        Map<String, Object>  bootstrap = null;
        RedisOps             redisOps;

        if ((options    == null) ||
            ((config    = readConfig(options.connectionYamlFile)) == null) ||
            ((bootstrap = readBootstrap(options.bootstrapYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        sysServices = setupServices(config);

        operations    = sysServices.getPrivOperations();
        orgLogic      = sysServices.getOrganizationLogic();
        orgTypes      = sysServices.getOrgTypes();
        organizations = sysServices.getOrganizations();
        roleLogic     = sysServices.getPrivRoleLogic();
        roleSetLogic  = sysServices.getRoleSetLogic();
        roleSets      = sysServices.getRoleSets();
        transactions  = sysServices.getRedisTransactions();
        users         = sysServices.getUsers();
        redisOps      = sysServices.getRedisOps();

        // we have to load up all orgs using the old collection mechanism as we're not yet able to create
        // the list using the new collection (CachingBase) mechanism.  Blech.

        for (String userOrgID : redisOps.lrange(organizations.makeRedisKey("uorgs-x-all"), 0, -1))
        {
            ParamSet params = new ParamSet(redisOps.hgetAll(organizations.makeRedisKey(userOrgID)));

            orgMap.put(params.get("name"), new Organization(params));  // "name" from NamedEntity.F_NAME
        }

        try
        {
            writeBootstrapInfo(config, bootstrap, options);
        }
        catch (Exception x)
        {
            if (x instanceof BaseException)
                System.out.println("\nERROR:  " + ((BaseException) x).getDetails());

            x.printStackTrace();
            System.exit(1);
        }

        System.exit(0);  // jedis or cql creates non-daemon thread.  boo
    }

    /**
     *
     */
    private static void writeBootstrapInfo(ConfigSet config, Map<String, Object> bootstrap, Options options) throws IOException
    {
        //        CqlTransactionCrud         ctc     = (CqlTransactionCrud) sysServices.getCqlCrud();
        List<Map<String, Object>>  configs;

        // All of this MUST run on one thread as the transactions are per-thread.

        try
        {
            transactions.begin();
            //            ctc.beginTransaction();

            rootRoledUserID = users.findUserByEmail(options.rootUserEmail).getID();

            if ((configs = (List<Map<String, Object>>) bootstrap.get("Operations")) != null)
                handleOperations(configs);

            if ((configs = (List<Map<String, Object>>) bootstrap.get("RoleSets")) != null)
                handleRolesets(configs);

            if ((configs = (List<Map<String, Object>>) bootstrap.get("OrgTypes")) != null)
                handleOrgTypes(configs);

            if ((configs = (List<Map<String, Object>>) bootstrap.get("Organizations")) != null)
                handleOrganizations(configs);

            //            ctc.commitTransaction();
            transactions.commit();
        }
        catch (Exception x)
        {
            transactions.abort();
            //            ctc.abortTransaction();

            if (x instanceof BaseException)
                System.out.println("\nERROR:  " + ((BaseException) x).getDetails());

            x.printStackTrace();
            System.out.println("\n################# NO changes made to redis due to error\n");
        }

        // users and role assignments MUST be after the trans.commit as they use all the info set up above
        
        if ((configs = (List<Map<String, Object>>) bootstrap.get("Users")) != null)
            handleUsers(configs);

        if ((configs = (List<Map<String, Object>>) bootstrap.get("RoleAssignments")) != null)
            handleRoleAssignments(configs);
    }

    /**
     *
     */
    private static void handleOperations(List<Map<String, Object>> ops)
    {
        for (Map<String, Object> oper : ops)
        {
            String    name       = (String) oper.get("name");
            String    desc       = (String) oper.get("description");
            String    applies    = (String) oper.get("appliesTo");
            Operation operation  = operations.findOperationByName(name);

            System.out.println("#### Operation");
            System.out.println("  name:       " + name    + "\n" +
                               "  appliesTo:  " + applies + "\n" +
                               "  desc:       " + desc    + "\n");

            if (applies != null)
            {
                for (String ele : applies.split(","))
                {
                    ele = ele.trim();

                    if (ele.length() > 0)  // ignore bad syntax just to be not snotty
                    {
                        if (!Privilege.isValidResolver(ele))
                            throw new IllegalArgumentException("Unknown acl target type in 'appliesTo':  [" + ele + "]");
                    }
                }
            }

            if (operation == null)
            {
                operations.createOperation(name, desc, applies);
            }
            else
            {
                operation.setAppliesTo(applies);
                operations.update(operation);
            }
        }
    }

    /*    /**
     *
     */
    private static void handleRolesets(List<Map<String, Object>> roleSets) throws IOException
    {        
        for (Map<String, Object> roleSet : roleSets)
        {
            String                     rsNameID = (String) roleSet.get("nameID");
            String                     name     = (String) roleSet.get("name");
            String                     desc     = (String) roleSet.get("description");
            String                     type     = (String) roleSet.get("type");
            List<Map<String, Object>>  roles    = (List<Map<String, Object>>) roleSet.get("roles");
            RoleSet                    rs;

            System.out.println("#### Roleset");
            System.out.println("  nameID:  " + rsNameID + "\n" +
                               "  name:    " + name + "\n" +
                               "  desc:    " + desc + "\n" +
                               "  type:    " + type + "\n" +
                               "  roles:");

            rs = ensureRoleSet(rsNameID, name, desc, type);  // after this call the RoleSet is accessible via rsNameID

            for (Map<String, Object> role : roles)
            {
                if (role.size() != 1)
                    throw new IllegalArgumentException("Elements of RoleSets.Roles are expected to be a single entry Map<String, Object> but is " + role);

                // 'for' loop is the cheap way to get the first (and only) element
                for (Map.Entry<String, Object> entry : role.entrySet())
                {
                    String                     roleName  = entry.getKey();
                    List<Map<String, Object>>  privs     = (List<Map<String, Object>>) entry.getValue();
                    Role                       roleObj;
                    List<Privilege>            newPrivs;

                    System.out.println("    rolename:  " + roleName);
                    System.out.println("    privileges:  " + privs);

                    roleObj = ensureRole(rs, roleName);

                    newPrivs = new ArrayList<Privilege>();

                    for (Map<String, Object> priv : privs)
                    {
                        boolean forMember = ((Boolean) priv.get("forMember")).booleanValue();
                        String  operation = (String) priv.get("operation");
                        String  aclTarget = (String) priv.get("acltarget");

                        if ((operation == null) || (aclTarget == null))
                            throw new IllegalArgumentException("Missing value for operation or acltarget in role [" +
                                                               rsNameID + "/" + roleName + "]");
                        else if (operations.findOperationByName(operation) == null)
                            throw new IllegalArgumentException("Unknown operation name [" + operation + "] in role [" +
                                                               rsNameID + "/" + roleName + "]");

                        newPrivs.add(new Privilege(forMember, operation, aclTarget));
                    }

                    roleLogic.setRolePrivileges(rootRoledUserID, roleObj, newPrivs);

                    break;
                }

                System.out.println("");
            }

        }
    }

    private static RoleSet ensureRoleSet(String nameID, String name, String description, String type)
    {
        RoleSet rs = roleSets.findRoleSetByNameID(nameID);

        if (rs == null)
        {
            RoleSet.RoleType roleType = RoleSet.RoleType.valueOf(type);

            rs = roleSetLogic.createRoleSet(roleType, nameID, name, description);
        }

        return rs;
    }

    private static Role ensureRole(RoleSet rs, String roleName)
    {
        Role role = roleSetLogic.lookupRoleByName(rs.getNameID() + "/" + roleName);

        if (role == null)
            role = roleSetLogic.createRole(rs, roleName, null, null);

        return role;
    }

    /**
     *
     */
    private static void handleOrgTypes(List<Map<String, Object>> orgTypeList)
    {
        for (Map<String, Object> orgType : orgTypeList)
        {
            String  name        = (String) orgType.get("name");
            String  desc        = (String) orgType.get("description");
            String  roleSetName = (String) orgType.get("roleSet");
            RoleSet roleSet     = roleSets.findRoleSetByNameID(roleSetName);
            OrgType otObj;

            System.out.println("#### OrgType");
            System.out.println("  name:     " + name + "\n" +
                               "  desc:     " + desc + "\n" +
                               "  roleset:  " + roleSetName + "\n");

            if (roleSet == null)
                throw new IllegalArgumentException("Unknown roleset [" + roleSetName + "] for orgType [" + name + "]");

            otObj = orgTypes.findOrgTypeByName(name);

            if (otObj == null)
            {
                otObj = new OrgType(name);
                otObj.setDescription(desc);
                otObj.setRoleSet(roleSet.getID());

                orgTypes.create(otObj);
            }
        }
    }

    /**
     *
     */
    private static void handleOrganizations(List<Map<String, Object>> orgs)
    {
        for (Map<String, Object> org : orgs)
        {
            String                name        = (String) org.get("name");
            String                desc        = (String) org.get("description");
            String                type        = (String) org.get("type");
            OrgType               otObj       = orgTypes.findOrgTypeByName(type);
            Organization          organ;

            System.out.println("#### Organization");
            System.out.println("  name:  " + name + "\n" +
                               "  desc:  " + desc + "\n" +
                               "  type:  " + type + "\n");

            if (otObj == null)
                throw new IllegalArgumentException("Unknown OrgType [" + type + "] for Organization [" + name + "]");

            if ((organ = orgMap.get(name)) == null)
            {
                organ = new Organization(name, otObj.getID(), null);

                organ.setDescription(desc);

                organizations.create(organ);

                orgMap.put(name, organ);

                System.out.println("   CREATED organization " + organ);
            }
            else
            {
                organ.setOrgType(otObj.getID());  //!!! temporary
                organizations.update(organ);
            }
        }
    }

    /**
     *
     */
    private static void handleUsers(List<Map<String, Object>> userList) throws IOException
    {
        for (Map<String, Object> user : userList)
        {
            String             id        = (String) user.get("id");
            String             email     = (String) user.get("email-address");
            String             memberOf  = (String) user.get("member-of");
            String             create    = (String) user.get("create");
            boolean            inThatOrg = false;
            User               userObj;
            Organization       orgObj;
            List<Organization> curOrgs;

            //            orgObj = organizations.findOrganizationByName(memberOf);
            orgObj = orgMap.get(memberOf);

            System.out.println("#### User");
            System.out.println("  id:     " + id + "\n" +
                               "  email:  " + email + "\n" +
                               "  flag:   " + create + "\n");

            userObj = users.findUserByEmail(email);
            if (orgObj == null)
                throw new IllegalArgumentException("Unknown organization [" + memberOf + "]");

            if (userObj == null)
                throw new IllegalArgumentException("Current limitation:  bootstrap data file can only refer to existing users");

            curOrgs = orgLogic.getUsersOrganizations(userObj.getID());

            for (Organization tOrg : curOrgs)
            {
                if (tOrg == null)
                {
                    System.out.println("ERROR:  can't find Organization object for userID " + email);
                }
                else if (tOrg.getID().equals(orgObj.getID()))
                {
                    inThatOrg = true;
                    break;
                }
            }

            if (!inThatOrg)
                orgLogic.addMemberToOrganization(orgObj.getID(), userObj.getID());

            idToUser.put(id, userObj);
        }
    }

    /**
     *
     */
    private static void handleRoleAssignments(List<Map<String, Object>> assignments) throws IOException
    {
        System.out.println("#### Role assignments");

        for (Map<String, Object> assign : assignments)
        {
            // should be only 1 element in each map, with the key being the id (for this module) of the user and the
            // value being "add Organization(role)", like "add Apixio(System/ROOT)"

            if (assign.size() != 1)
                throw new IllegalArgumentException("Invalid syntax for RoleAssignment:  [" + assign + "]:  not exactly one n:v in assignment");

            for (Map.Entry<String, Object> entry : assign.entrySet())
            {
                String       userID  = entry.getKey();
                String       command = (String) entry.getValue();
                int          sp      = command.indexOf(' ');
                String       action  = command.substring(0, sp);
                User         user    = idToUser.get(userID);
                Role         role;
                String       org;
                String       rolename;
                Organization orgObj;

                if (user == null)
                    throw new IllegalArgumentException("Unknown user [" + userID + "]");

                command = command.substring(sp + 1);

                sp  = command.indexOf('(');
                org = command.substring(0, sp);
                //                orgObj = organizations.findOrganizationByName(org);
                orgObj = orgMap.get(org);
                if (orgObj == null)
                    throw new IllegalArgumentException("Unknown organization [" + org + "]");

                command = command.substring(sp + 1);

                if ((sp = command.indexOf(')')) == -1)
                    throw new IllegalArgumentException("Invalid syntax for RoleAssignment:  [" + assign + "]:  expected Organization(Rolename)");

                rolename = command.substring(0, sp);
                role     = roleSetLogic.lookupRoleByName(rolename);

                if (role == null)
                    throw new IllegalArgumentException("Unknown role [" + rolename + "]");

                System.out.println("  Assigning User [" + user.getEmailAddress() + "] to role [" + role.getName() + "] to organization [" + org + "]]");

                roleLogic.assignUserToRole(rootRoledUserID, user, role, orgObj.getID());

                break;
            }
        }
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  bootstrap  = ps.getOptionArg(BOOTSTRAP_OPT);
        String  rootEmail  = ps.getOptionArg(ROOTUSER_OPT);

        if ((connection == null) || (bootstrap == null) || (rootEmail == null))
            return null;

        return new Options(connection, bootstrap, rootEmail);
    }

    /**
     * Read the yaml bootstrap file and populate the raw map object from it.
     */
    private static Map<String, Object> readBootstrap(String filepath) throws Exception
    {
        return loadYaml(filepath);
    }

    /**
     * Load configuration from .yaml file into generic Map.
     */
    private static Map<String, Object> loadYaml(String filepath) throws Exception
    {
        InputStream input = new FileInputStream(new File(filepath));
        Yaml        yaml  = new Yaml();
        Object      data = yaml.load(input);

        input.close();

        return (Map<String, Object>) data;
    }

    private static boolean isEmpty(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
