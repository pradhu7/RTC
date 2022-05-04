package com.apixio.useracct.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.apixio.restbase.LogicBase;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.aclsys.buslog.Permission;
import com.apixio.aclsys.buslog.UserGroupLogic;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.OrgTypes;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dao.Projects;
import com.apixio.useracct.dao.RoleSets;
import com.apixio.useracct.dao.Roles;
import com.apixio.useracct.entity.OrgType;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.Privilege;
import com.apixio.useracct.entity.Project;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.RoleSet;
import com.apixio.useracct.entity.User;

/**
 */
public class RoleLogic extends LogicBase<SysServices> {

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType {
        /**
         * for modifyOrganization:
         */
        NAME_USED,
        PRIV_TARGET_NOT_ALLOWED,
        NO_OPERATION
    }

    /**
     * If organization operations fail they will throw an exception of this class.
     */
    public static class RoleException extends BaseException {

        public RoleException(FailureType failureType)
        {
            super(failureType);
        }

        public RoleException(FailureType failureType, String details, Object... args)
        {
            super(failureType);
            super.description(details, args);
        }
    }

    /**
     * AclGroupName manages the construction and deconstruction of the three-part UserGroup name.
     * This is needed because we have no easy way to persist extra information about/on a
     * UserGroup so we encode info in the group name itself.
     *
     * Recall that a UserGroup is the entity that is given permission on some object and that
     * we therefore need to manage group membership.  Only users who are assigned the same
     * role for the same target will be in a given usergroup.  It is further subdivided into
     * users that are members-of the target org and those who are not.  That means that each
     * group name tells what role and target it's for and enough info to know if it's for
     * members-of the target (this last item is needed because even though two users are assigned
     * the same role to the same target org, they could have different permissions because one
     * is a member-of that target and the other user isn't).
     *
     * Note that we aren't including the actual privilege in the user group name as those are
     * the things that are changeable:  it's always possible to add or remove a specific privilege
     * all the while keeping the group membership the same.
     */
    static class AclGroupName {
        /**
         * Let's version the AclGroupName format so that if we need to change it or
         * add to it in the future we can without killing ourselves.
         */
        private static final String VERSION_1 = "v1";

        private static final String SEP = ":";

        String   version;

        /**
         * The memberOrgID is the "member of" OrganizationID for ALL users in the given group.
         */
        String   memberOrgID;

        /**
         * The Role ID that each User in this UserGroup has with respect to the target.
         */
        String   roleID;

        /**
         * The Organization or Project id that the users have the role within.
         */
        String   targetID;

        /**
         * Returns true if and only if the the Users assigned to the role for that org/project
         * as also "members of" the org (note that users can't be "members of" a project so
         * if this is a project role assignment, this method will always return false).
         */
        boolean forMember()
        {
            return memberOrgID.equals(targetID);
        }

        AclGroupName(String flattened)
        {
            String[] comps = flattened.split(SEP);

            if (comps.length != 4)
                throw new IllegalArgumentException("Invalid flattened AclGroupName [" + flattened + "]");;

            version     = comps[0];
            memberOrgID = comps[1];
            roleID      = comps[2];
            targetID    = comps[3];
        }

        static String flatten(String memberOrgID, String roleID, String targetID)
        {
            // NEVER EVER CHANGE THIS as doing so will lose all existing role-based ACLs!
            // it's okay to bump version (and deal with complexity when that's done)
            return VERSION_1 + SEP + memberOrgID + SEP + roleID + SEP + targetID;
        }
    }

    /**
     * AccessorParams collects the full set of possible Accessor values.  It is used only when
     * we have enough info to be able to create an ACL UserGroup (since we might need more
     * than just the targetOrgID).
     */
    protected static class AccessorParams {
        String  memberOfOrgID;
        String  targetOrgID;
        String  targetProjectID;

        static AccessorParams forTargetOrg(String memberOfOrgID, String targetOrgID)
        {
            return new AccessorParams(memberOfOrgID, targetOrgID, null);
        }

        static AccessorParams forTargetProject(String memberOfOrgID, String targetProjectID)
        {
            return new AccessorParams(memberOfOrgID, null, targetProjectID);
        }

        private AccessorParams(String memberOfOrgID, String targetOrgID, String targetProjectID)
        {
            this.memberOfOrgID       = memberOfOrgID;
            this.targetOrgID         = targetOrgID;
            this.targetProjectID     = targetProjectID;
        }

        @Override
        public String toString()
        {
            return ("[accessor memberOfOrgID=" + memberOfOrgID +
                    "; targetOrgID=" + targetOrgID +
                    "; targetProjID=" + targetProjectID +
                    "]");
        }
    }

    /**
     * RbacTarget is a cheap class to help abstract the details away from the
     * differences between a target Organization and a target Project.
     */
    static class RbacTarget {
        XUUID   targetID;  // org or project
        boolean isOrganization;
        boolean isProject;

        RbacTarget(XUUID targetID)
        {
            this.targetID = targetID;
        }
    }

    /**
     * System Services used
     */
    private AclLogic          aclLogic;
    private OrgTypes          orgTypes;
    private OrganizationLogic orgLogic;
    private Organizations     organizations;
    private Projects          projects;
    private ProjectLogic      projectLogic;
    private RoleSetLogic      roleSetLogic;
    private RoleSets          roleSets;
    private Roles             roles;
    private UserGroupDao      aclGroups;     // groups that record "assigned to" relationships, etc.
    private UserGroupLogic    aclGroupLogic; // groups that record "assigned to" relationships, etc.
    private UserGroupDao      orgGroups;     // groups that record "member of" relationships (or "managed by"...)

    /**
     * Constructor.
     */
    public RoleLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    @Override
    public void postInit()
    {
        orgGroups     = sysServices.getUserGroupDaoExt(SysServices.UGDAO_MEMBEROF);
        aclGroups     = sysServices.getUserGroupDaoExt(SysServices.UGDAO_ACL);
        aclGroupLogic = sysServices.getUserGroupLogicExt(SysServices.UGDAO_ACL);
        aclLogic      = sysServices.getAclLogic();
        orgTypes      = sysServices.getOrgTypes();
        organizations = sysServices.getOrganizations();
        orgLogic      = sysServices.getOrganizationLogic();
        projects      = sysServices.getProjects();
        projectLogic  = sysServices.getProjectLogic();
        roleSetLogic  = sysServices.getRoleSetLogic();
        roleSets      = sysServices.getRoleSets();
        roles         = sysServices.getRoles();
    }

    // ################################################################
    // Role management
    // ################################################################

    /**
     * Look up the Role by its canonical name of "{OrgType}/{Role}"
     */
    public Role lookupRoleByName(String setRole)
    {
        return roleSetLogic.lookupRoleByName(setRole);
    }

    // ################################################################
    //  Role-based access control methods
    // ################################################################

    /**
     * Model: Organizations have org-specific Roles ("Role" in code).  Roles have a set
     * of Privileges.  Users are assigned to a particular Role within the Organization.
     * Each User with the same Role for the same Organization has the exact same
     * privileges as any other use with the same Role/Org.
     *
     * Because all Users with the same Role/Org are treated identically (that is the
     * defining feature of Role-Based Access Control), the design uses a UserGroup to
     * contain all Users for that [Role, Org] combination.  The name of the UserGroup is
     * a unique to the Role and Organization.  It is the UserGroups that are given the
     * actual low-level "aclsys" permission (via AclLogic.addPermission).
     *
     * A Privilege has a "selector" that indicates if it's to be applied to members of
     * the targetOrg or applied to non-members.  A Privilege also has an object
     * "accessor" (for lack of a better name).  An accessor is the thing that tells what
     * the ACL triplet value for the "Object" is:  if it's the targetOrg, a PatientDataSet
     * within the targetOrg, or the member-of (i.e., the Organization of the User that is
     * being given the role) organization.
     *
     * Note that this gets a bit complex as assigning UserA (who is a member-of OrgA) to
     * a role within OrgB could cause the user to be added to two UserGroups, one for the
     * role within OrgA, and one for the (same) role within OrgB.
     */

    /**
     * Returns the list of Users (well, their XUUIDs) that have the given role within
     * the given organization or project.  Both members and non-members of the
     * targetOrg are included, though the actual permissions of these two types of
     * users could be different.
     */
    public List<XUUID> getUsersWithRole(XUUID caller, XUUID orgOrProjID, XUUID orgRoleID) throws IOException
    {
        Role         orgRole  = roles.findCachedRoleByID(orgRoleID);
        List<XUUID>  userList = new ArrayList<>();
        String       id       = orgOrProjID.toString();

        for (String ugName : roles.getTrackedRoleUserGroups(orgRole))  // name is persisted (AclGroupName.flatten)
        {
            AclGroupName gn = new AclGroupName(ugName);

            if (gn.targetID.equals(id))
                userList.addAll(aclGroups.getGroupMembers(aclGroups.findGroupByName(ugName)));
        }

        return userList;
    }

    /**
     * Assigns & unassigns the User the given Role within the target Organization.  A side effect of this
     * is to possibly create a UserGroup that is specific to "member-ness", role, and organization
     * and assigning/removing privileges to/from this group.
     */
    public void assignUserToRole(XUUID caller, User user, Role role, XUUID targetOrgOrProject) throws IOException
    {
        modifyUserRoleAssignment(caller, user.getID(), role, targetOrgOrProject, true);
    }

    public void unassignUserFromRole(XUUID caller, User user, Role role, XUUID targetOrgOrProject) throws IOException
    {
        modifyUserRoleAssignment(caller, user.getID(), role, targetOrgOrProject, false);
    }

    public void unassignUserFromTarget(XUUID caller, XUUID userID, List<Role> roles, XUUID targetOrgOrProject) throws IOException
    {
        for (Role role : roles)
            modifyUserRoleAssignment(caller, userID, role, targetOrgOrProject, false);
    }

    public void unassignAllUsersFromTarget(XUUID caller, List<Role> roles, XUUID targetOrgOrProject) throws IOException
    {
        for (Role role : roles)
        {
            for (XUUID userID : getUsersWithRole(caller, targetOrgOrProject, role.getID()))
                modifyUserRoleAssignment(caller, userID, role, targetOrgOrProject, false);
        }
    }

    private void modifyUserRoleAssignment(XUUID caller, XUUID userID, Role role, XUUID targetOrgOrProjectID, boolean assign) throws IOException
    {
        RbacTarget target = validateRoleType(role, targetOrgOrProjectID);
        UserGroup  ug     = getAclUserGroup(caller, userID, role, targetOrgOrProjectID, makeAccessorParams(target, userID));

        if (assign)
            aclGroups.addMemberToGroup(ug, userID);
        else
            aclGroups.removeMemberFromGroup(ug, userID);
    }

    /**
     * Given a user, return the list of roles (and orgs/projects for each role)
     */
    public List<RoleAssignment> getRolesAssignedToUser(User user) throws IOException
    {
        List<RoleAssignment> assignments = new ArrayList<>();

        roles.getAllRoles();  // ensure latest

        for (XUUID groupID : aclGroups.getGroupsByMember(user.getID()))
        {
            UserGroup    ug  = aclGroups.findGroupByID(groupID);
            AclGroupName agn = null;

            // kind of ugly here but since UserGroups used at this level don't have an easy typing
            // mechanism, we try the parse and test the version to see if it's really an ACL
            // group.

            try
            {
                agn = new AclGroupName(ug.getName());

                if (!agn.VERSION_1.equals(agn.version))
                    agn = null;
            }
            catch (Exception x)
            {
                // purposely ignore as we're just testing syntax
            }

            if (agn != null)
            {
                assignments.add(new RoleAssignment(user, XUUID.fromString(agn.targetID),
                                                   roles.findCachedRoleByID(XUUID.fromString(agn.roleID, Role.OBJTYPE))));
            }
        }

        return assignments;
    }

    /**
     * Note that we have to pass in Role as we don't know how to get all
     * roles from the targetOrgOrProjectID (at least at this level I'm
     * saying RoleLogic shouldn't know about that iteration).
     */
    public void deleteRoleTarget(XUUID caller, Role role, XUUID targetOrgOrProjectID) throws IOException
    {
        String targetIDstr = targetOrgOrProjectID.toString();  // because AclGroupName fields are strings

        for (String ugName : roles.getTrackedRoleUserGroups(role))
        {
            AclGroupName     gn  = new AclGroupName(ugName);

            if (gn.targetID.equals(targetIDstr) || gn.memberOrgID.equals(targetIDstr))
                deleteAclUserGroup(caller, ugName, role);
        }
    }

    /**
     * A real corner case need:  produce the same flattened AclGroupName value that
     * RESOLVER_ACL_GROUPNAME produces.
     */
    public String makeAclGroupName(String userID, String roleName, String targetOrgOrProjectID) throws IOException
    {
        Role  role = lookupRoleByName(roleName);

        if (role == null)
            throw new IllegalArgumentException("Unable to find role '" + roleName + "' while making acl groupname");

        RbacTarget target = validateRoleType(role, XUUID.fromString(targetOrgOrProjectID));

        return getAclGroupName(role, makeAccessorParams(target, XUUID.fromString(userID)));
    }

    /**
     * Checks that the ID is an allowed type (project or organization) and for organizations
     * it also checks that the type of the organization is the same as what is declared in
     * the role
     */
    private RbacTarget validateRoleType(Role role, XUUID targetOrgOrProjectID)
    {
        RoleSet     roleSet = roleSets.findRoleSetByID(role.getRoleSet());
        String      xType   = targetOrgOrProjectID.getType();
        RbacTarget  target  = new RbacTarget(targetOrgOrProjectID);

        if (xType.equals(Organization.OBJTYPE))
        {
            Organization org   = organizations.findOrganizationByID(targetOrgOrProjectID);
            OrgType      oType = orgTypes.findCachedOrgTypeByID(org.getOrgType());

            if (roleSet.getRoleType() != RoleSet.RoleType.ORGANIZATION)
                throw new IllegalArgumentException("Role is not for ORGANIZATION but an OrgID [" + targetOrgOrProjectID + "] was used");
            else if (!oType.getRoleSet().equals(role.getRoleSet()))
                throw new IllegalArgumentException("Role is for a different type of organization than what was passed in");

            target.isOrganization = true;
        }
        else if (Project.isProjectType(xType))
        {
            if (roleSet.getRoleType() != RoleSet.RoleType.PROJECT)
                throw new IllegalArgumentException("Role is not for PROJECT but a ProjID [" + targetOrgOrProjectID + "] was used");

            target.isProject = true;
        }
        else
        {
            throw new IllegalArgumentException("Unsupported XUUID type of [" + xType + "]");
        }

        return target;
    }
    
    /**
     * The core method that finds/creates the right UserGroup to make the User a member
     * of it (for ACLs), or to remove from membership.  Note that since we're not
     * explicitly storing a link from Organization to its UserGroups, we actually end up
     * using the same naming scheme for both groups that the user is a member of and
     * groups that the user is not a member of.
     *
     * For example, if we have User1 who is a member of OrgA and User2 who is a member of OrgB
     * and RoleZ that is "under" OrgA (i.e., a User can be assigned RoleZ within OrgA), then
     * the possible UserGroup names are:
     *
     *  * v1:OrgA:OrgA:RoleZ    # call this UG1 (construction is in class AclGroupName.flatten)
     *  * v1:OrgA:OrgB:RoleZ    # call this UG2
     *
     * If User1 is assigned the role "RoleZ" then UG1 is returned.  If User2 is assigned the
     * same role (which is to the same target as User1 is assigned to), then UG2 is returned.
     *
     * The fact that roles are NOT reused across organizations allows this (and in the above
     * example, "RoleZ" is NOT the human readable name, it's the fake XUUID).
     *
     * IF the group doesn't yet exist (e.g., during the first time a User is assigned the
     * Role within the organization), then it will be created ONLY IF there is enough
     * information to actually assign privileges. This means that this method CAN return
     * null.
     */
    private UserGroup getAclUserGroup(XUUID caller, XUUID userID, Role role, XUUID targetOrg, AccessorParams accessorParams) throws IOException
    {
        boolean   member    = isMemberOfTarget(userID, targetOrg);
        String    memberOrg = getObjectFromAccessor(role, Privilege.RESOLVER_MEMBER_OF_ORG, accessorParams);   // just reuse functionality to get member-of org!
        String    name      = AclGroupName.flatten(memberOrg, role.getID().toString(), targetOrg.toString());
        UserGroup group     = aclGroups.findGroupByName(name);

        if (group == null)
        {
            group = aclGroups.createGroup(name);
            roles.trackRoleUserGroup(role, name);

            // side-effect of getting the group for the first time is to add permissions to it:
            if (accessorParams != null)
            {
                for (Privilege priv : role.getPrivileges())
                {
                    if (member == priv.forMember())
                    {
                        String tripletObject = getObjectFromAccessor(role, priv.getObjectResolver(), accessorParams);

                        // !! BIG HACK:  see AclLogic comment before this XUUID for more info.
                        // The following commented out code really _should_ be used, if I can figure
                        // it out.
                        // aclLogic.addPermission(caller, group.getID(), priv.getOperationName(), tripletObject);
                        aclLogic.addPermission(AclLogic.PRIVILEGED_CALLER_XUUID, group.getID(), priv.getOperationName(), tripletObject);
                    }
                }
            }
        }

        return group;
    }

    /**
     * Undoes what getAclUserGroup when it creates a group on demand.  Note that the UserGroupDao
     * code will remove membership and ACL permissions when the group is deleted.
     */
    protected void deleteAclUserGroup(XUUID caller, String name, Role role) throws IOException
    {
        UserGroup group = aclGroups.findGroupByName(name);

        if (group != null)
        {
            aclGroupLogic.deleteGroup(group.getID());
            roles.untrackRoleUserGroup(role, name);
        }
    }

    /**
     * Create an instance of AccessorParams that's as fully filled out as possible given
     * the targetOrg and the User.
     */
    private AccessorParams makeAccessorParams(RbacTarget target, XUUID userID) throws IOException
    {
        List<XUUID> groups = orgGroups.getGroupsByMember(userID);
        String      memberOrgID;
        String      targetStr;

        if (groups.size() != 1)
            throw new IllegalStateException("User [" + userID + "] is not a member-of exactly 1 organization: " + groups);

        // groups[0].name is the "member-of" organizationID:
        memberOrgID = orgGroups.findGroupByID(groups.get(0)).getName();
        targetStr   = target.targetID.toString();

        if (target.isOrganization)
            return AccessorParams.forTargetOrg(memberOrgID, targetStr);
        else if (target.isProject)
            return AccessorParams.forTargetProject(memberOrgID, targetStr);
        else
            throw new IllegalStateException("Programming error:  RbacTarget is neither ORG or PROJ");
    }

    /**
     * Create an instance of AccessorParams from a user group name
     */
    protected AccessorParams makeAccessorParams(AclGroupName agn) throws IOException
    {
        String memberOrgID = agn.memberOrgID;
        String targetStr   = agn.targetID;
        String type        = XUUID.fromString(targetStr).getType();

        // these tests MUST match what's in validateRoleType!
        if (type.equals(Organization.OBJTYPE))
            return AccessorParams.forTargetOrg(memberOrgID, targetStr);
        else if (Project.isProjectType(type))
            return AccessorParams.forTargetProject(memberOrgID, targetStr);
        else
            throw new IllegalStateException("Programming error:  RbacTarget is neither ORG or PROJ");
    }

    /**
     * Given the accessor type and the actual AccessorParams, extract out and return the
     * one that has been requested.
     */
    protected String getObjectFromAccessor(Role role, String accessor, AccessorParams params)
    {
        if (accessor.equals(Privilege.RESOLVER_ANY_ALL))
            return AclLogic.ANY_OBJECT;
        else if (accessor.equals(Privilege.RESOLVER_MEMBER_OF_ORG))
            return params.memberOfOrgID;
        else if (accessor.equals(Privilege.RESOLVER_TARGET_ORG))
            return params.targetOrgID;
        else if (accessor.equals(Privilege.RESOLVER_TARGET_PROJECT))
            return params.targetProjectID;
        else if (accessor.equals(Privilege.RESOLVER_TARGET_PROJORG))
            return getOrgOfProject(params.targetProjectID);
        else if (accessor.equals(Privilege.RESOLVER_TARGET_PROJPDS))
            return getPdsOfProject(params.targetProjectID);
        else if (accessor.equals(Privilege.RESOLVER_ACL_GROUPNAME))
            return getAclGroupName(role, params);
        else
            throw new IllegalArgumentException("Invalid Privilege ObjectAccessor:  [" + accessor + "]");
    }

    private String getAclGroupName(Role role, AccessorParams params)
    {
        return AclGroupName.flatten(params.memberOfOrgID, role.getID().toString(),
                                    (params.targetProjectID != null) ? params.targetProjectID : params.targetOrgID);
    }

    private String getOrgOfProject(String projectID)
    {
        Project proj = projectLogic.getProjectByID(XUUID.fromString(projectID));

        if (proj != null)
            return proj.getOrganizationID().toString();
        else
            return null;
    }

    private String getPdsOfProject(String projectID)
    {
        Project proj = projectLogic.getProjectByID(XUUID.fromString(projectID));

        if (proj != null)
            return proj.getPatientDataSetID().toString();
        else
            return null;
    }

    /**
     * Given a User and an Organization ID, return true if the user is a member of
     * the organization (i.e., addMemberToOrganization(org, user) was called).
     */
    private boolean isMemberOfTarget(XUUID u, XUUID orgID) throws IOException
    {
        UserGroup ug = orgLogic.getMemberOfUserGroup(orgID);

        return orgGroups.isMemberOfGroup(ug.getID(), u);
    }

    // ################################################################
    // ################################################################

    public String dumpOrgPermissions(XUUID orgID) throws IOException
    {
        Organization  org  = organizations.findOrganizationByID(orgID);
        OrgType       type = orgTypes.findCachedOrgTypeByID(org.getOrgType());
        RoleSet       rset = roleSets.findRoleSetByID(type.getRoleSet());
        Set<XUUID>    ors  = roleSets.getRolesBySet(rset);
        StringBuilder sb   = new StringBuilder();

        sb.append(com.apixio.DebugUtil.subargs("\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BEGIN Organization debug:  name={} id={}\n", org.getName(), orgID));
        sb.append(com.apixio.DebugUtil.subargs("OrgType={} RoleSet={}; {} role(s) within roleset\n", type.getName(), rset.getName(), ors.size()));

        dumpPermissions(sb, ors);

        sb.append(com.apixio.DebugUtil.subargs("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< END Organization debug:  name={} id={}\n\n", org.getName(), orgID));

        return sb.toString();
    }

    public String dumpProjPermissions(XUUID projID) throws IOException
    {
        Project       proj = projects.findProjectByID(projID);
        RoleSet       rset = roleSets.findRoleSetByNameID(ProjectLogic.getRoleSetNameID(proj));
        Set<XUUID>    ors  = roleSets.getRolesBySet(rset);
        StringBuilder sb   = new StringBuilder();

        sb.append(com.apixio.DebugUtil.subargs("\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BEGIN Project debug:  name={} id={}\n", proj.getName(), projID));
        sb.append(com.apixio.DebugUtil.subargs("Type={} RoleSet={}; {} role(s) within roleset\n", proj.getType(), rset.getName(), ors.size()));

        dumpPermissions(sb, ors);

        sb.append(com.apixio.DebugUtil.subargs("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< END Project debug:  name={} id={}\n\n", proj.getName(), projID));

        return sb.toString();
    }

    private void dumpPermissions(StringBuilder sb, Set<XUUID> ors) throws IOException
    {
        for (XUUID roleID : ors)
        {
            Role  role = roles.findRoleByID(roleID);

            sb.append(com.apixio.DebugUtil.subargs("\n------- Role {} [id={}]\n", role.getName(), roleID));

            for (String ugName : roles.getTrackedRoleUserGroups(role))
            {
                AclGroupName     gn  = new AclGroupName(ugName);
                Organization     mem = organizations.findOrganizationByID(XUUID.fromString(gn.memberOrgID));
                RbacTarget       tar = validateRoleType(role, XUUID.fromString(gn.targetID));
                UserGroup        grp = aclGroups.findGroupByName(ugName);
                boolean          some;

                if (grp == null)
                {
                    sb.append("\nUnable to find group by name " + ugName + "\n");
                }
                else
                {
                    sb.append(com.apixio.DebugUtil.subargs("\nTracked group name={} id={}:\n", ugName, grp.getID()));
                    sb.append(com.apixio.DebugUtil.subargs("  MemberOrg:  name={} id={}\n", mem.getName(), mem.getID()));
                    if (tar.isOrganization)
                    {                    
                        Organization targetOrg = organizations.findOrganizationByID(tar.targetID);

                        sb.append(com.apixio.DebugUtil.subargs("  TargetOrg:  name={} id={}\n", targetOrg.getName(), targetOrg.getID()));
                    }
                    else if (tar.isProject)
                    {
                        Project targetProj = projects.findProjectByID(tar.targetID);

                        sb.append(com.apixio.DebugUtil.subargs("  TargetProject:  name={} id={}", targetProj.getName(), targetProj.getID()));
                    }

                    sb.append("  Members:\n");
                    some = false;

                    for (XUUID member : aclGroups.getGroupMembers(grp))
                    {
                        User user = sysServices.getUsers().findUserByID(member);

                        sb.append(com.apixio.DebugUtil.subargs("    Email={} id={}\n", ((user != null) ? user.getEmailAddress() : "<no user>"), member));
                        some = true;
                    }
                    if (!some)
                        sb.append("    <no members>\n");

                    sb.append("  Permissions:\n");
                    for (Permission perm : aclLogic.getBySubject(grp.getID()))
                    {
                        sb.append(com.apixio.DebugUtil.subargs("    Operation={}, object={}\n", perm.operation, perm.object));
                    }
                }

                sb.append("\n");
            }
        }
    }

    // ################################################################
    // ################################################################

    public String dumpUserRoles(User user) throws IOException
    {
        StringBuilder sb   = new StringBuilder();

        sb.append(com.apixio.DebugUtil.subargs("\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BEGIN User debug:  id={} email={}\n", user.getID(), user.getEmailAddress()));

        for (RoleAssignment ra : getRolesAssignedToUser(user))
        {
            sb.append(com.apixio.DebugUtil.subargs("  User [{}] has role [{}] within org/proj ID [{}]\n", user.getEmailAddress(), ra.role.getName(), ra.targetID));
        }

        sb.append(com.apixio.DebugUtil.subargs("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< END User debug:  id={} email={}\n", user.getID(), user.getEmailAddress()));

        return sb.toString();
    }
}
