package com.apixio.useracct.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.aclsys.buslog.Permission;
import com.apixio.aclsys.buslog.UserGroupLogic;
import com.apixio.aclsys.dao.Operations;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.entity.Operation;
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
import com.apixio.useracct.entity.RoleSet.RoleType;
import com.apixio.useracct.entity.RoleSet;
import com.apixio.useracct.entity.User;

/**
 */
public class PrivRoleLogic extends RoleLogic {

    /**
     * System Services used
     */
    private AclLogic          aclLogic;
    private RoleSetLogic      roleSetLogic;
    private RoleSets          roleSets;
    private Roles             roles;
    private UserGroupDao      aclGroups;     // groups that record "assigned to" relationships, etc.
    private UserGroupLogic    aclGroupLogic; // groups that record "assigned to" relationships, etc.
    private Operations        operations;

    /**
     * Constructor.
     */
    public PrivRoleLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    @Override
    public void postInit()
    {
        super.postInit();

        aclGroups     = sysServices.getUserGroupDaoExt(SysServices.UGDAO_ACL);
        aclGroupLogic = sysServices.getUserGroupLogicExt(SysServices.UGDAO_ACL);
        aclLogic      = sysServices.getAclLogic();
        roleSetLogic  = sysServices.getRoleSetLogic();
        roleSets      = sysServices.getRoleSets();
        roles         = sysServices.getRoles();
        operations    = sysServices.getOperations();
    }

    /**
     * Update the Role's privileges to be exactly what's passed in.  newPrivs is NOT an incremental
     * set to add!
     *
     * Add/delete semantics are applied so ACL implications are immediate.
     */
    public void setRolePrivileges(XUUID caller, Role role, List<Privilege> newPrivs) throws IOException
    {
        List<Privilege>  curPrivs = role.getPrivileges();
        List<Privilege>  added    = new ArrayList<>();
        List<Privilege>  removed  = new ArrayList<>();

        // inefficient way, but prob not a problem given the very low counts on these lists
        for (Privilege priv : newPrivs)
        {
            validateTargetAllowed(priv);

            if (!inList(priv, curPrivs))
                added.add(priv);
        }

        for (Privilege priv : curPrivs)
        {
            if (!inList(priv, newPrivs))
                removed.add(priv);
        }

        //        System.out.println("SFM:  setRolePrivileges:  removing privileges " + removed);
        //        System.out.println("SFM:  setRolePrivileges:  adding privileges " + added);

        for (Privilege priv : added)
            addPrivilegeToRole(caller, role, priv);

        for (Privilege priv : removed)
            removePrivilegeFromRole(caller, role, priv);
    }

    /**
     * Check if the operation's list of what acl target it allows contains what the privilege
     * is declaring it's targeting.  An exception is thrown if it's not contained.
     */
    private void validateTargetAllowed(Privilege priv)
    {
        Operation op = operations.findOperationByName(priv.getOperationName());

        if (op == null)
            throw new RoleException(FailureType.NO_OPERATION, "Privilege {} refers to non-existent operation", priv.toString());

        String appliesTo = op.getAppliesTo();
        String target    = priv.getObjectResolver();

        if ((appliesTo == null) || (appliesTo.trim().length() == 0))
            return;

        for (String ele : appliesTo.split(","))  // really need to have some guarantees on this formatted string
        {
            ele = ele.trim();

            if ((ele.length() > 0) && ele.equals(target))
                return;
        }

        throw new RoleException(FailureType.PRIV_TARGET_NOT_ALLOWED,
                                "Bad configuration on privilege {}:  target [{}] not supported by {}'s appliesTo [{}]",
                                priv.toString(), target, priv.getOperationName(), appliesTo);
    }

    private boolean inList(Privilege priv, List<Privilege> list)
    {
        for (Privilege p : list)
        {
            if (p.equals(priv))
                return true;
        }

        return false;
    }

    // ################################################################
    // Role management
    // ################################################################

    /**
     * Delete the given Role by cleaning up all aspects of it.  THIS IS NOT
     * TRANSACTIONAL ACROSS REDIS AND CASSANDRA!
     */
    public void deleteRole(XUUID caller, Role role) throws IOException
    {
        RoleSet         set   = roleSets.findRoleSetByID(role.getRoleSet());
        List<Privilege> privs = role.getPrivileges();

        // delete all tracked groups, which deletes permissions and membership
        // delete org role itself

        for (String ugName : roles.getTrackedRoleUserGroups(role))
            deleteAclUserGroup(caller, ugName, role);

        roleSetLogic.deleteRole(role);
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
     * Manage privileges on an EXISTING Role (that might have already been used in some
     * User assignment).  This requires that all Organizations that have/use that Role
     * have low-level permissions updated to reflect the modified privilege.  This is done by
     * adding the permission to each UserGroup that is dependent on the Role.
     */
    public void addPrivilegeToRole(XUUID caller, Role orgRole, Privilege privilege) throws IOException
    {
        modifyPrivilegeOnRole(caller, orgRole, privilege, true);
    }

    public void removePrivilegeFromRole(XUUID caller, Role orgRole, Privilege privilege)  throws IOException
    {
        modifyPrivilegeOnRole(caller, orgRole, privilege, false);
    }

    private void modifyPrivilegeOnRole(XUUID caller, Role orgOrProjRole, Privilege privilege, boolean add)  throws IOException
    {
        List<Privilege> initPrivs = orgOrProjRole.getPrivileges();
        String          roleID    = orgOrProjRole.getID().toString();

        // modify persisted list of privs in Role itself, and if perm is already there don't add it
        // or if it's not there, then don't remove it
        boolean found = false;

        for (int i = 0, m = initPrivs.size(); i < m; i++)
        {
            Privilege tmpPriv = initPrivs.get(i);

            if (privilege.equals(tmpPriv))
            {
                if (add)
                    return;     // yuck, i hate early returns like this but in this case it's easiest:  don't add because it's already there

                initPrivs.remove(i);
                orgOrProjRole.setPrivileges(initPrivs);
                roles.update(orgOrProjRole);
                found = true;
                break;
            }
        }

        // actually add it to the list kept on the orgOrProjRole entity
        if (add)
            initPrivs.add(privilege);
        else if (found)
            initPrivs.remove(privilege);
        else
            return;  // asked to delete but it wasn't found; another early return...sigh

        orgOrProjRole.setPrivileges(initPrivs);
        roles.update(orgOrProjRole);

        for (String ugName : roles.getTrackedRoleUserGroups(orgOrProjRole))
        {
            AclGroupName aclGN     = new AclGroupName(ugName);
            boolean      forMember = aclGN.forMember();

            if (!aclGN.roleID.equals(roleID))
                throw new IllegalStateException("RoleID within Acl UserGroup name doesn't match tracked for role");

            if (forMember == privilege.forMember())
            {
                UserGroup    group  = aclGroups.findGroupByName(ugName);
                String       aclObj = getObjectFromAccessor(orgOrProjRole, privilege.getObjectResolver(),
                                                            makeAccessorParams(aclGN));

                if (add)
                    aclLogic.addPermission(caller, group.getID(), privilege.getOperationName(), aclObj);
                else
                    aclLogic.removePermission(caller, group.getID(), privilege.getOperationName(), aclObj);
            }
        }
    }

}
