package com.apixio.aclsys.buslog;

import java.io.IOException;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.SysServices;
import com.apixio.aclsys.dao.AclDao;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.entity.Operation;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.entity.OldRole;

/**
 * Contains reusable logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 *
 */
public class UserGroupLogic extends LogicBase<SysServices> {

    /**
     * The type of UserGroup that keeps track of Users that have a given role.
     */
    private final static String ROLE_USERGROUP = UserGroup.SYSTYPE_PREFIX + "Role";

    private UserGroupDao userGroupDao;
    private AclDao       aclDao;

    /**
     * The various types of UserGroup management failure.
     */
    public enum FailureType {
        /**
         * for createUserGroup
         */ 
        USERGROUPNAME_ALREADYUSED,

        /**
         * for modifyRole:
         */
        NO_SUCH_USERGROUP,

        /**
         * Untyped failure
         */
        GENERAL_ERROR
    }

    /**
     * If role userGroups fail they will throw an exception of this class.
     */
    public static class UserGroupException extends BaseException {

        private FailureType failureType;

        public UserGroupException(FailureType failureType)
        {
            this.failureType = failureType;
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * Constructor.
     */
    public UserGroupLogic(SysServices sysServices, UserGroupDao dao)
    {
        super(sysServices);

        this.aclDao       = sysServices.getAclDao();
        this.userGroupDao = dao;
    }

    // ################################################################
    //  Useful debugging code
    // ################################################################

    public String dumpUserGroups() throws IOException
    {
        StringBuilder                    sb  = new StringBuilder();

        sb.append("\n\n");
        sb.append(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> START Low-level UserGroups\n");

        for (UserGroup ug : userGroupDao.getAllGroups())
        {
            sb.append("\n\n>>>>>>>>>>>>> UserGroup ");
            sb.append(ug.toString());
            sb.append("\nMembers:\n");

            for (XUUID member : userGroupDao.getGroupMembers(ug))
            {
                sb.append("  ");
                sb.append(member.toString());
            }
        }

        sb.append("\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< END Low-level UserGroups\n\n");

        return sb.toString();
    }
    
    // ################################################################
    // ################################################################

    /**
     * Create a group by cleaning name and making sure it's uniquely named.
     */
    public UserGroup createGroup(String name) throws UserGroupException, IOException  // IOException => internal server error; UserGroupException => bad request
    {
        try
        {
            return userGroupDao.createGroup(name);
        }
        catch (IllegalArgumentException x)
        {
            throw new UserGroupException(FailureType.USERGROUPNAME_ALREADYUSED);
        }
    }

    /**
     * Creates a UserGroup that is a system group with the subtype equal to the role name.
     */
    public UserGroup createRoleGroup(OldRole role) throws IOException
    {
        try
        {
            UserGroup ug = userGroupDao.createGroup(makeRoleUserGroupName(role),
                                                    makeRoleUserGroupType(role));

            return ug;
        }
        catch (IllegalArgumentException x)
        {
            throw new UserGroupException(FailureType.USERGROUPNAME_ALREADYUSED);
        }
    }

    public UserGroup findOrganizationGroup(XUUID orgID) throws IOException
    {
        return userGroupDao.findGroupByName(orgID.toString());
    }

    /**
     * Finds the UserGroup (which is a system group) for the given Role.  This should always return
     * a group in a non-corrupt system.
     */
    public UserGroup findRoleGroup(OldRole role) throws IOException
    {
        String     name = makeRoleUserGroupName(role);
        UserGroup  ug   = userGroupDao.findGroupByName(name);

        if ((ug == null) || !ug.isSystemGroupSubtype(makeRoleUserGroupType(role)))
            ug = findRoleGroupInefficient(role);
        
        return ug;
    }

    public UserGroup findRoleGroupInefficient(OldRole role) throws IOException
    {
        final long start = System.currentTimeMillis();

        System.out.println("WARNING:  using inefficient findRoleGroupInefficient(" + role + ")");

        // ugly, but searching through all groups is the easiest right now
        for (UserGroup ug : getAllGroups())
        {
            if (ug.isSystemGroupSubtype(makeRoleUserGroupType(role)))
                return ug;
        }

        return null;
    }

    /**
     * Rename a group to the new name.  Fails if the name already exists.
     */
    public UserGroup renameGroup(UserGroup curGroup, String newName) throws UserGroupException, IOException
    {
        validateGroup(curGroup);

        if (curGroup.isSystemGroup())
            throw UserGroupException.badRequest("Attempt to rename system UserGroup {}", curGroup.getName());

        try
        {
            return userGroupDao.renameGroup(curGroup, newName);
        }
        catch (IllegalArgumentException x)
        {
            throw new UserGroupException(FailureType.USERGROUPNAME_ALREADYUSED);
        }
    }

    /**
     * Deletes the group, including all things related to the group (e.g., membership
     * list and the permissions granted the group).
     *
     * Note that this method causes some design problems in that the operation it performs
     * require that it reaches outside its domain and into the ACL dao area.  This is
     * kind of ugly and in some ways calls out for a non-denominational module that
     * manages cross-domain (where domains == UserGroups & ACLs) operations...
     *
     * Also note that this group deletion is done atomically in that all row/column ops
     * are collected into a single MultiOp object that is then used to perform a single
     * CQL batch operation.
     */
    public void deleteGroup(XUUID groupID) throws UserGroupException, IOException
    {
        validateGroup(groupID);

        UserGroup        group = userGroupDao.findGroupByID(groupID);
        OperationLogic   opLog = services.getOperationLogic();

        validateGroup(group);  // might have had a valid-looking ID that didn't refer to an existing group.

        if (group.isSystemGroup())
            throw UserGroupException.badRequest("Attempt to rename system UserGroup {}", group.getName());

        // delete membership
        userGroupDao.removeAllMembersFromGroup(groupID);
                     
        // delete permissions on group itself
        for (Operation op : opLog.getAllOperations())
        {
            for (String obj : aclDao.getBySubjectOperation(groupID, op))
            {
                aclDao.removePermission(groupID, services.getAclLogic().getOperation(op.getName()), obj);
            }
        }

        // finally delete the group meta itself
        userGroupDao.deleteGroup(group);
    }

    /**
     * Return a list of all Roles known by the system.
     */
    public List<UserGroup> getAllGroups() throws IOException
    {
        return userGroupDao.getAllGroups();
    }

    /**
     */
    public UserGroup getUserGroupByID(XUUID groupID) throws IOException
    {
        return userGroupDao.findGroupByID(groupID);
    }

    /**
     */
    public UserGroup getUserGroupByName(String name) throws IOException
    {
        return userGroupDao.findGroupByName(name);
    }

    /**
     * Add user to given user group (which is the default user group)
     */
    public void addMemberToGroup(UserGroup ug, XUUID userID) throws IOException
    {
        userGroupDao.addMemberToGroup(ug, userID);
    }

    /**
     * Return list of members (XUUID) of group.
     */
    public List<XUUID> getGroupMembers(UserGroup ug) throws IOException
    {
        return userGroupDao.getGroupMembers(ug);
    }

    /**
     * Given a Role, form the (System) type of the UserGroup and its human readable name.
     */
    private String makeRoleUserGroupType(OldRole role)
    {
        return ROLE_USERGROUP + "." + role.getName();
    }

    private String makeRoleUserGroupName(OldRole role)
    {
        return role.getName() + " Users";  //!! blech hardcoding
    }

    /**
     * Checks that the POJO group exists and has a well-formed XUUID.
     */
    private void validateGroup(UserGroup group)
    {
        XUUID id;

        if ((group == null) || ((id = group.getID()) == null) || !id.getType().equals(UserGroup.OBJTYPE))
            throw new UserGroupException(FailureType.NO_SUCH_USERGROUP);
    }

    private void validateGroup(XUUID groupID)
    {
        if ((groupID == null) || !groupID.getType().equals(UserGroup.OBJTYPE))
            throw new UserGroupException(FailureType.NO_SUCH_USERGROUP);
    }

}
