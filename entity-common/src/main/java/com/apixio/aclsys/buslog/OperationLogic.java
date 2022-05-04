package com.apixio.aclsys.buslog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.SysServices;
import com.apixio.aclsys.dao.Operations;
import com.apixio.aclsys.entity.Operation;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.web.BaseException;

/**
 * Contains reusable logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 *
 * Generally speaking Operations are referred to by their names and not their XUUIDs as
 * this is more natural and more readable when debugging.  The set of Operations doesn't
 * change much over time and they aren't user created so using a name doesn't present
 * any real problem.
 */
public class OperationLogic extends LogicBase<SysServices> {

    /**
     * Records the last time a full read from Redis was done.
     */
    private long lastLoadTime;

    /**
     * The various types of Operation management failure.
     */
    public enum FailureType {
        /**
         * for createOperation
         */
        OPERATIONNAME_ALREADYUSED,
        NO_SUCH_ACCESSTYPE;
    }

    /**
     * If role operations fail they will throw an exception of this class.
     */
    public static class OperationException extends BaseException {

        private FailureType failureType;

        public OperationException(FailureType failureType)
        {
            // this means that the string returned to RESTful clients will receive the Enum.toString()
            // as the reason for failure:
            super(failureType);

            this.failureType = failureType;
        }

        /**
         * This is to support a "try/catch(OperationException)" that needs more info
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
    private List<Operation>        allOps;
    private Map<String, Operation> allOpsMap;
    private boolean                rebuildCache = true;

    /**
     * Constructor.
     */
    public OperationLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    /**
     * Refreshes the cache of roles as necessary and returns the role with the
     * given name, if any.
     */
    public Operation getOperation(
        String   opName
        )
    {
        rebuild();

        return allOpsMap.get(opName);
    }

    /**
     * Return a list of all Roles known by the system.  Caller should NOT hold onto this
     * list as that list won't reflect updates/deletions.
     */
    public List<Operation> getAllOperations()
    {
        List<Operation> ops = new ArrayList<Operation>();

        rebuild();

        ops.addAll(allOps);

        return ops;
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
        Operations ops = services.getOperations();

        if (rebuildCache || (ops.getLastModificationTime() > lastLoadTime))
        {
            allOps    = ops.getAllOperations();
            allOpsMap = new HashMap<String, Operation>();

            for (Operation op : allOps)
                allOpsMap.put(op.getName(), op);

            rebuildCache = false;
            lastLoadTime = System.currentTimeMillis();
        }
    }

}
