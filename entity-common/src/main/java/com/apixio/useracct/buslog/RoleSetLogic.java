package com.apixio.useracct.buslog;

import java.util.ArrayList;
import java.util.List;

import com.apixio.restbase.LogicBase;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.RoleSets;
import com.apixio.useracct.dao.Roles;
import com.apixio.useracct.entity.Privilege;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.RoleSet.RoleType;
import com.apixio.useracct.entity.RoleSet;

/**
 */
public class RoleSetLogic extends LogicBase<SysServices> {

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType {
        /**
         * for createRole
         */
        NAME_USED
    }

    /**
     * If organization operations fail they will throw an exception of this class.
     */
    public static class RoleSetException extends BaseException {

        private FailureType failureType;

        public RoleSetException(FailureType failureType)
        {
            super(failureType);
            this.failureType = failureType;
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * System Services used
     */
    private RoleSets       roleSets;
    private Roles          roles;

    /**
     * Constructor.
     */
    public RoleSetLogic(SysServices sysServices)
    {
        super(sysServices);

        roleSets = sysServices.getRoleSets();
        roles    = sysServices.getRoles();
    }

    /**
     *
     */
    public RoleSet createRoleSet(RoleType type, String nameID, String name, String description)
    {
        RoleSet set = roleSets.findRoleSetByNameID(nameID);

        if (set != null)
            throw new IllegalArgumentException("RoleSet with nameID [" + nameID + "] already exists"); //!! use RoleSetException instead

        set = new RoleSet(type, nameID, name);
        set.setDescription(description);

        roleSets.create(set);

        return set;
    }

    /**
     */
    public RoleSet getRoleSetByNameID(String nameID)
    {
        return roleSets.findRoleSetByNameID(nameID);
    }

    /**
     * Returns a list of all Organization objects.
     */
    public List<RoleSet> getAllRoleSets()
    {
        return roleSets.getAllRoleSets();
    }

    /* OrgType and Role management.
     *
     * The design is for uniquely named OrgTypes to contain zero or more Roles.
     * Within the context of a specific OrgType, Role names must be unique.
     *
     * Each Role has a set of Privileges.
     */

    public Role createRole(RoleSet roleSet, String roleName, String description, List<Privilege> privileges)
    {
        if (lookupRoleByName(roleSet, roleName) != null)
            throw new RoleSetException(FailureType.NAME_USED);

        Role role = new Role(roleSet.getID(), roleName);

        role.setDescription(description);

        if (privileges != null)
        {
            for (Privilege priv : privileges)
                role.addPrivilege(priv);
        }

        roles.create(role);
        roleSets.addRoleToSet(roleSet, role);

        return role;
    }

    /**
     */
    public void deleteRole(Role role)
    {
        RoleSet  set  = roleSets.findRoleSetByID(role.getRoleSet());

        roleSets.removeRoleFromSet(set, role);
        roles.delete(role);
    }

    /**
     * Look up the Role by its canonical name of "{Roleset}/{Role}"
     */
    public Role lookupRoleByName(String setRole)
    {
        String[]  comps = setRole.split("/");

        if (comps.length != 2)
            throw new IllegalArgumentException("Invalid full Role name [" + setRole + "]:  must of the form roleSetName/roleName");

        roles.getAllRoles();  // force cache loading.  grrrr.

        return lookupRoleByName(roleSets.findRoleSetByNameID(comps[0]), comps[1]);
    }

    public List<Role> getRolesInSet(RoleSet roleSet)
    {
        List<Role> rolesInSet = new ArrayList<>();

        roles.getAllRoles();  // force cache loading.  grrrr.

        for (XUUID roleID : roleSets.getRolesBySet(roleSet))
            rolesInSet.add(roles.findCachedRoleByID(roleID));

        return rolesInSet;
    }

    private Role lookupRoleByName(RoleSet roleSet, String roleName)
    {
        if (roleSet != null)
        {
            Role role;

            roles.getAllRoles();  // force cache loading.  grrrr.

            for (XUUID roleID : roleSets.getRolesBySet(roleSet))
            {
                if (((role = roles.findCachedRoleByID(roleID)) != null) && role.getName().equals(roleName))
                    return role;
            }
        }

        return null;
    }

}
