package com.apixio.useracct.buslog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.restbase.LogicBase;
import com.apixio.SysServices;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.entity.OldRole;

/**
 * Contains reusable role level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 *
 * Generally speaking Roles are referred to by their names and not their XUUIDs as
 * this is more natural and more readable when debugging.  The set of roles doesn't
 * change much over time and they aren't user created so using a name doesn't present
 * any real problem.
 */
public class OldRoleLogic extends LogicBase<SysServices> {

    /**
     * The various types of Role management failure.
     */
    public enum FailureType {
        /**
         * for createRole
         */ 
        ROLENAME_ALREADYUSED,
    }

    /**
     * If role operations fail they will throw an exception of this class.
     */
    public static class RoleException extends BaseException {

        private FailureType failureType;

        public RoleException(FailureType failureType)
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
     * The list of all Roles is cached here; the rebuildCache flag indicates if the cache should
     * flushed and rebuilt.
     */
    private List<OldRole>        allRoles;
    private Map<String, OldRole> allRolesMap;
    private boolean              rebuildCache = true;

    /**
     * Constructor.
     */
    public OldRoleLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    /**
     * Refreshes the cache of roles as necessary and returns the role with the
     * given name, if any.
     */
    public OldRole getRole(
        String   roleName
        )
    {
        rebuild();

        return allRolesMap.get(roleName);
    }

    /**
     * Return a list of all Roles known by the system.
     */
    public List<OldRole> getAllRoles()
    {
        List<OldRole> roles = new ArrayList<OldRole>();

        rebuild();

        roles.addAll(allRoles);

        return roles;
    }

    /**
     * Sets the flag to rebuild the cache next time it's accessed.
     */
    protected void rebuildCache()
    {
        this.rebuildCache = true;
    }

    /**
     * Checks if the Role cache needs to be rebuilt by rereading from redis.
     */
    private void rebuild()
    {
        if (rebuildCache)
        {
            allRoles    = sysServices.getOldRoles().getAllRoles();
            allRolesMap = new HashMap<String, OldRole>();

            for (OldRole role : allRoles)
                allRolesMap.put(role.getName(), role);

            rebuildCache = false;
        }
    }

}
