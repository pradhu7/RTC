package com.apixio.aclsys.buslog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.SysServices;
import com.apixio.aclsys.dao.AccessTypes;
import com.apixio.aclsys.entity.AccessType;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.web.BaseException;

/**
 * Contains reusable role level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 *
 * Generally speaking Roles are referred to by their names and not their XUUIDs as
 * this is more natural and more readable when debugging.  The set of roles doesn't
 * change much over time and they aren't user created so using a name doesn't present
 * any real problem.
 */
public class AccessTypeLogic extends LogicBase<SysServices> {

    /**
     * Records the last time a full read from Redis was done.
     */
    private long lastLoadTime;

    /**
     * The various types of Role management failure.
     */
    public enum FailureType {
        /**
         * for createAccessType
         */ 
        TYPENAME_ALREADYUSED,
    }

    /**
     * If role operations fail they will throw an exception of this class.
     */
    public static class AccessTypeException extends BaseException {

        private FailureType failureType;

        public AccessTypeException(FailureType failureType)
        {
            // this means that the string returned to RESTful clients will receive the Enum.toString()
            // as the reason for failure:
            super(failureType);

            this.failureType = failureType;
        }

        /**
         * This is to support a "try/catch(AccessTypeException)" that needs more info
         * on what happened.
         */
        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * The list of all Roles is cached here; the rebuildCache flag indicates if the cache should
     * flushed and rebuilt.
     */
    private List<AccessType>        allTypes;
    private Map<String, AccessType> allTypesMap;
    private boolean                 rebuildCache = true;

    /**
     * Constructor.
     */
    public AccessTypeLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    /**
     * Refreshes the cache of roles as necessary and returns the role with the
     * given name, if any.
     */
    public AccessType getAccessType(
        String   typeName
        )
    {
        rebuild();

        return allTypesMap.get(typeName);
    }

    /**
     * Return a list of all Roles known by the system.  Caller should NOT hold onto this
     * list as that list won't reflect updates/deletions.
     */
    public List<AccessType> getAllAccessTypes()
    {
        List<AccessType> types = new ArrayList<AccessType>();

        rebuild();

        types.addAll(allTypes);

        return types;
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
        AccessTypes ats = services.getAccessTypes();

        if (rebuildCache || (ats.getLastModificationTime() > lastLoadTime))
        {
            allTypes    = ats.getAllAccessTypes();
            allTypesMap = new HashMap<String, AccessType>();

            for (AccessType type : allTypes)
                allTypesMap.put(type.getName(), type);

            rebuildCache = false;
            lastLoadTime = System.currentTimeMillis();
        }
    }

}
