package com.apixio.useracct.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.restbase.LogicBase;
import com.apixio.ConfigConstants;
import com.apixio.Datanames;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.buslog.UserGroupLogic;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.restbase.PropertyUtil;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dao.OrgTypes;
import com.apixio.useracct.dao.PatientDataSets;
import com.apixio.useracct.dao.RoleSets;
import com.apixio.useracct.dao.Roles;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.OrgType;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.RoleSet;
import com.apixio.useracct.entity.User;

/**
 * Contains reusable organization level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the JAX-RS-invoked methods).
 */
public class OrganizationLogic extends LogicBase<SysServices>
{
    /**
     * DESIGN NOTES:
     *
     *  1.  The name of the UserGroup that records "member-of" relationship between a
     *      User and an Organization (as compared to "assigned-to" relationship) is
     *      the XUUID of the organization.
     *
     *  2.  The name of the UserGroup that records "assigned-to" relationship contains
     *      3 parts:  the XUUID of the memberOf organization, the XUUID of the Role,
     *      and the XUUID of the targetOrg (which could be the same as the memberOf
     *      organization).  We record it this way as all that info is needed when
     *      modifying the privileges on a role.
     */

    /**
     * CODE NOTES:
     *
     *  1.  there are two uses of the UserGroup construct (as defined by UserGroupDao), one
     *      for keeping track of an Organizations members (e.g., employees), and one for
     *      keeping track of ACL groups for an Organization (for assigning Users to roles
     *      within an Organization).  The first use is referred to in the code as "MemberOf
     *      Group" (which can be confusing since that phrase could be construed as a
     *      boolean test), and the second use is referred to as an "Acl Group"
     */

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType {
        /**
         * for modifyOrganization:
         */
        USER_IN_ORG_ALREADY,
        USER_NOT_IN_ORG,
        UNKNOWN_ORG_TYPE,
        NAME_USED,
        MISSING_NAME,
        NAME_TOO_LONG,
        BAD_PROPERTY
    }

    public static final int MAX_NAMELENGTH = 256;

    /**
     * If organization operations fail they will throw an exception of this class.
     */
    public static class OrganizationException extends BaseException {

        public OrganizationException(FailureType failureType, String details, Object... args)
        {
            super(failureType);
            super.description(details, args);
        }
    }

    /**
     * PropertyUtil helps manage per-Organization CustomProperties
     */
    private PropertyUtil propertyUtil;

    /**
     * Important org types to enforce business logic.
     */
    private OrgType customerOrgType;

    /**
     * System Services used
     */
    private OrgTypes        orgTypes;
    private Organizations   organizations;
    private PatientDataSets patientDataSets;
    private Roles           roles;
    private RoleSetLogic    roleSetLogic;
    private RoleSets        roleSets;
    private UserGroupDao    orgGroups;      // groups that record "member of" relationships (or "managed by"...)
    private UserGroupLogic  orgGroupLogic;  // bus logic for groups that record "member of" relationships (or "managed by"...)

    /**
     * Constructor.
     */
    public OrganizationLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    @Override
    public void postInit()
    {
        orgGroups       = sysServices.getUserGroupDaoExt(SysServices.UGDAO_MEMBEROF);
        orgGroupLogic   = sysServices.getUserGroupLogicExt(SysServices.UGDAO_MEMBEROF);
        orgTypes        = sysServices.getOrgTypes();
        patientDataSets = sysServices.getPatientDataSets();
        organizations   = sysServices.getOrganizations();
        roles           = sysServices.getRoles();
        roleSets        = sysServices.getRoleSets();
        roleSetLogic    = sysServices.getRoleSetLogic();

        propertyUtil  = new PropertyUtil(Datanames.SCOPE_ORGANIZATION, sysServices);
        customerOrgType = orgTypes.findOrgTypeByName(ConfigConstants.CUSTOMER_ORG_TYPE);
    }

    /**
     * Creates an Organization of the given type with the given name and description.  The
     * underlying "ACL UserGroup" and "Member UserGroup" are not created here as they're
     * an "on demand" creation.
     */
    public Organization createOrganization(String orgTypeName, String name, String description, String externalID)
    {
        Organization org = getOrganization(orgTypeName, name, externalID);

        org.setDescription(description);

        organizations.create(org);

        return org;
    }

    private Organization getOrganization(String orgTypeName, String name, String externalID)
    {
        OrgType type = orgTypes.findOrgTypeByName(orgTypeName);

        if (type == null)
            throw new OrganizationException(FailureType.UNKNOWN_ORG_TYPE, "Type [{}] is unknown", orgTypeName);
        else if ((name == null) || ((name = name.trim()).length() == 0))
            throw new OrganizationException(FailureType.MISSING_NAME, "Missing name");
        else if (name.length() > MAX_NAMELENGTH)
            throw new OrganizationException(FailureType.NAME_TOO_LONG, "Name [{}] is too long (max of {})", name, MAX_NAMELENGTH);

        return new Organization(name, type.getID(), externalID);
    }

    /**
     *
     * Create with org with Two factor
     */
    public Organization createOrganization(String orgTypeName, String name, String description, String externalID, boolean needsTwoFactor)
    {
        Organization org = getOrganization(orgTypeName, name, externalID);

        org.setNeedsTwoFactor(needsTwoFactor);
        org.setDescription(description);

        organizations.create(org);

        return org;
    }

    /**
     * Looks up the Organization by its XUUID and returns it if the user is either the caller (as identified
     * by the token that's in the thread local) or if the caller has root or admin privileges.
     */
    public Organization getOrganizationByID(String id)
    {
        return organizations.findOrganizationByID(XUUID.fromString(id, Organization.OBJTYPE));
    }

    /**
     * Returns a list of all Organization objects.
     */
    public List<Organization> getAllOrganizations(boolean includeInactive)
    {
        return organizations.getAllOrganizations(includeInactive);
    }

    /**
     * Returns a list of all Organization objects of the given type.
     */
    public List<Organization> getOrganizations(OrgType type, boolean includeInactive)
    {
        List<Organization> orgs = new ArrayList<Organization>();
        XUUID              tid  = type.getID();

        for (Organization org : organizations.getAllOrganizations(includeInactive))
        {
            if (org.getOrgType().equals(tid))
                orgs.add(org);
        }

        return orgs;
    }

    /**
     * Modify the UserOrg object by setting the properties to the values in the modifications
     * parameter.  Note that ALL fields are set (as there's no way to specify a 'use current
     * value' flag), so the caller should prefill with existing values as necessary.
     */
    public void updateOrganization(Organization org)
    {
        organizations.update(org);
    }

    public List<Role> getRolesForOrg(Organization org)
    {
        OrgType      ot       = orgTypes.findOrgTypeByID(org.getOrgType());
        RoleSet      rs       = roleSets.findRoleSetByID(ot.getRoleSet());
        List<Role>   allRoles = roles.getAllRoles();
        List<Role>   rRoles   = new ArrayList<>();

        for (XUUID role : roleSets.getRolesBySet(rs))
            rRoles.add(roles.findCachedRoleByID(role));

        return rRoles;
    }

    public Role getRoleFromOrgAndName(Organization org, String roleName)
    {
        OrgType ot = orgTypes.findOrgTypeByID(org.getOrgType());
        RoleSet rs = roleSets.findRoleSetByID(ot.getRoleSet());

        return roleSetLogic.lookupRoleByName(rs.getNameID() + "/" + roleName);
    }

    /**
     * A test-only method to delete an organization.  It does minimal cleanup of relationships and is
     * meant only as a way to run some basic testing of the code.
     */
    public void deleteOrganization(XUUID orgID) throws IOException
    {
        Organization org = organizations.findOrganizationByID(orgID);

        if (org != null)
        {
            UserGroup group = orgGroups.findGroupByName(orgID.toString());

            // remove members and usergroup in one fell swoop
            // delete actual org entity

            if (group != null)  // no members assigned yet.
                orgGroupLogic.deleteGroup(group.getID());

            organizations.delete(org);
        }
    }

    // ################################################################
    //  Organization membership (e.g., User as an employee of Apixio) methods.
    // ################################################################

    /**
     * Model: Organizations can have members and the recording/management of this
     * membership is done via UserGroups.  However, these UserGroups are NOT included
     * with the access-control groups but are used solely for aiding user management
     * of the Organization.
     *  
     * These UserGroups are in the UGDAO_MEMBEROF UserGroupDao.
     *
     * The link from Organization to UserGroup is by using the XUUID of the Organization
     * as the name of the (system) UserGroup.
     */

    /**
     * Adds the User (specified by XUUID) to the given Organization.
     */
    public void addMemberToOrganization(XUUID orgID, XUUID userID) throws IOException
    {
        if (!orgID.getType().equals(Organization.OBJTYPE) || !userID.getType().equals(User.OBJTYPE))
            throw OrganizationException.badRequest("addMemberToOrganization requires orgID and UserID");

        List<XUUID> groups = orgGroups.getGroupsByMember(userID);

        if (groups.size() >= 1)
            throw OrganizationException.badRequest("User [{}] is already a member-of an Organization", userID);

        orgGroups.addMemberToGroup(getMemberOfUserGroup(orgID), userID);
    }

    /**
     * Removes the User (specified by XUUID) from the given Organization.  No error is
     * produced if the user is not a member of the organization.
     */
    public void removeMemberFromOrganization(XUUID orgID, XUUID userID) throws IOException
    {
        if (!orgID.getType().equals(Organization.OBJTYPE) || !userID.getType().equals(User.OBJTYPE))
            throw OrganizationException.badRequest("removeMemberToOrganization requires orgID and UserID");

        orgGroups.removeMemberFromGroup(getMemberOfUserGroup(orgID), userID);
    }

    /**
     * Returns the list of Users (well, their XUUIDs) that belong to the given group.
     */
    public List<XUUID> getUsersBelongingToOrg(XUUID orgID) throws IOException
    {
        return orgGroups.getGroupMembers(getMemberOfUserGroup(orgID));
    }

    /**
     * Given a User, return the single group that the user is a member-of.
     */
    public List<Organization> getUsersOrganizations(XUUID userID) throws IOException
    {
        List<XUUID>        groups = orgGroups.getGroupsByMember(userID);
        List<Organization> orgs   = new ArrayList<>();

        // name of group is the ID of the Organization object

        for (XUUID groupID : groups)
            orgs.add(organizations.findOrganizationByID(XUUID.fromString(orgGroups.findGroupByID(groupID).getName(), Organization.OBJTYPE)));

        return orgs;
    }

    /**
     * Given a User, return the single group that the user is a member-of.
     */
    public Organization getUsersOrganization(XUUID userID) throws IOException
    {
        List<XUUID> groups = orgGroups.getGroupsByMember(userID);

        if (groups.size() > 1)
            throw new OrganizationException(FailureType.USER_NOT_IN_ORG, "User [{}] is not a member-of exactly 1 organization; groups", userID, groups);
        else if (groups.size() == 0)
            return null;
        else
            return organizations.findOrganizationByID(XUUID.fromString(orgGroups.findGroupByID(groups.get(0)).getName(), Organization.OBJTYPE));
    }

    /**
     * Lookup the "member UserGroup" for the organization, creating it if it's not there.
     * The user group type is marked as a system group so that it isn't editable by
     * an UI mechanism.
     */
    UserGroup getMemberOfUserGroup(XUUID organizationID) throws IOException
    {
        UserGroup group = orgGroups.findGroupByName(organizationID.toString());  // intentionally bypass UserGroupLogic as it doesn't use extended DAO

        if (group == null)
            group = orgGroups.createGroup(organizationID.toString());

        return group;
    }

    // ################################################################
    //  CustomerOrg stuff
    // ################################################################

    /**
     * Method to create a patient data set without Customer Care.
     *
     * This method will enforce that only organization of type Customer may own a PDS. It
     * will throw an IllegalArgumentException if organization does not exist or is not a Customer type.
     */
    public PatientDataSet createPatientDataSetWithOwningOrg(String name, String description, String owningOrgID)
    {
        XUUID          orgID = null;
        Long           id;
        PatientDataSet patientDataSet;

        if (owningOrgID != null)
        {
            // Verify that this organization if of type Customer. Do this first before we persist the PDS!
            // This call will throw an IllegalArgumentException if type is not of Customer, which will be propagated to the client.

            orgID = XUUID.fromString(owningOrgID, Organization.OBJTYPE);
            getAndConfirmIsCustomerOrg(orgID);
        }

        id = sysServices.getPatientDataSets().getNextPdsID(true);

        // Do we need to verify uniqueness of this PDS's name?
        patientDataSet = sysServices.getPatientDataSetLogic().createPatientDataSet(name, description, id);

        if (orgID != null)
        {
            // If we got here, we have already validated org's type
            addPdsToCustomerOrg(orgID, patientDataSet);
        }

        return patientDataSet;
    }

    public List<PatientDataSet> getCustomerOrgPdsList(XUUID orgID)
    {
        Organization org = getAndConfirmIsCustomerOrg(orgID);

        return sysServices.getPatientDataSetLogic().getPdsList(orgID);
    }

    public boolean addPdsToCustomerOrg(XUUID orgID, XUUID pds)
    {
        Organization org = getAndConfirmIsCustomerOrg(orgID);

        return sysServices.getPatientDataSetLogic().associatePdsToID(pds, orgID);
    }

    public boolean addPdsToCustomerOrg(XUUID orgID, PatientDataSet pds)
    {
        return sysServices.getPatientDataSetLogic().associatePdsToID(pds, orgID);
    }

    public void removePdsFromCustomerOrg(XUUID orgID, XUUID pds)
    {
        Organization org = getAndConfirmIsCustomerOrg(orgID);

        sysServices.getPatientDataSetLogic().unassociatePdsFromID(pds, orgID);
    }

    public Organization getAndConfirmIsCustomerOrg(XUUID orgID)
    {
        Organization org = organizations.findOrganizationByID(orgID);

        if (org == null)
            throw new IllegalArgumentException("Organization with id [" + orgID + "] not found");
        else if ((customerOrgType == null) || (!org.getOrgType().equals(customerOrgType.getID())))
            throw new IllegalArgumentException("Attempt to get Organization PatientDataSet list on a non-customer Organization [" + orgID + "]");

        return org;
    }

    // ################################################################
    //  Role-based access control methods
    // ################################################################

    public List<?> getUserOrgsInfo(XUUID caller, User user)
    {
        throw new RuntimeException("Unimplemented");

        // this one is ugly???  loop on all Roles:
        //  loop on all Orgs for the OrgType of the orgRole:
        //    if isGroupMember(userGroupFromOrgAndRole(org, orgRole)
        //      add [user, orgID, orgRole] to list
        //
        // !!! NO!!! the right way to do this is to get all groups the user is
        // a member of and for each one that is the RBAC, it can decompose the
        // group name back into role and orgID

        // return { orgRole1 : [ "orgID1", "orgID2", ...], "orgRole2" : [ ... ] }
    }

    // ################################################################
    //  Custom Properties
    // ################################################################

    /**
     * The Custom Property model allows a set of uniquely named properties that are
     * then available to have values associated with those property names added to
     * a Organization object.  Custom properties also have a type that limits the types
     * of values that can be added to customer objects.
     *
     * Custom Properties have a lifetime independent of Organizations and their property values.
     * Actual custom property values on Organizations are tied to the lifetime of Custom Properties
     * as a deletion of the global Custom Property definition will delete all properties
     * of that type/name from all Organizations.
     */

    /**
     * Bulk update/set of custom property values on the given organization.  Do NOT ignore errors on
     * individual setting.
     */
    public void updateOrgCustomProperties(Organization org, Map<String, String> properties)
    {
        for (Map.Entry<String, String> entry : properties.entrySet())
        {
            String key   = entry.getKey();
            String value = entry.getValue();

            if (value != null)
                setOrganizationProperty(org, key, value);
            else
                removeOrganizationProperty(org, key);
        }
    }

    /**
     * Add a new custom property to the global set of properties.  The name must be unique
     * when lowercased.
     */ 
    public void addPropertyDef(String name, String typeMeta)   // throws exception if name.trim.tolowercase is not unique
    {
        propertyUtil.addPropertyDef(name, typeMeta);
    }

    /**
     * Returns a map from unique property name to the declared PropertyType of that property.
     */
    public Map<String, String> getPropertyDefinitions(boolean includeMeta) // <propName, propType>
    {
        return propertyUtil.getPropertyDefs(includeMeta);
    }

    /**
     * Removes the custom property definition.  This removal will cascade to a deletion of
     * property values on Organization objects.
     */
    public void removePropertyDef(String name)
    {
        propertyUtil.removePropertyDef(name);
    }

    /**
     * Add a property value to the given Organization.  The actual value
     * will be converted--as possible--to the declared property type.
     */
    public void setOrganizationProperty(Organization userOrg, String propertyName, String valueStr) // throws exception if name not known
    {
        propertyUtil.setEntityProperty(userOrg.getID(), propertyName, valueStr);
    }

    /**
     * Remove a custom property value from the given Organization.
     */
    public void removeOrganizationProperty(Organization userOrg, String propertyName) // throws exception if name not known
    {
        propertyUtil.removeEntityProperty(userOrg.getID(), propertyName);
    }

    /**
     * Returns a map from OrganizationXUUIDs to a map that contains all the name=value pairs for each Organization.
     */
    public Map<XUUID, Map<String, Object>> getAllOrganizationProperties()                 // <OrganizationID, <propname, propvalue>>
    {
        return propertyUtil.getAllCustomProperties();
    }

    /**
     * Given a property name, return a map from OrganizationXUUID to the property value for that Organization.
     */
    public Map<XUUID, Object> getOrganizationsProperty(String propName)     // <OrganizationID, propvalue>
    {
        return propertyUtil.getCustomProperty(propName);
    }

    /**
     * Given a Organization, return a map from property name to the property value for that Organization.
     */
    public Map<String, Object> getOrganizationProperties(Organization org)   // <propname, propvalue>
    {
        return getOrganizationProperties(org.getID());
    }

    public Map<String, Object> getOrganizationProperties(XUUID orgID)   // <propname, propvalue>
    {
        return propertyUtil.getEntityProperties(orgID);
    }

}
