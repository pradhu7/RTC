package com.apixio.aclsys.buslog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.restbase.entity.Token;
import com.apixio.aclsys.dao.Operations;
import com.apixio.aclsys.entity.AccessType;
import com.apixio.aclsys.entity.Operation;
import com.apixio.useracct.PrivSysServices;

/**
 * Contains reusable Operation level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 *
 * Generally speaking Operations are referred to by their names and not their XUUIDs as
 * this is more natural and more readable when debugging.  The set of Operations doesn't
 * change much over time and they aren't user created so using a name doesn't present
 * any real problem.
 */
public class PrivOperationLogic extends OperationLogic {

    private PrivSysServices privSysServices;

    /**
     * Constructor.
     */
    public PrivOperationLogic(PrivSysServices sysServices)
    {
        super(sysServices);

        this.privSysServices = sysServices;
    }

    /**
     * Create a new role with the given name, throwing an exception if the name
     * is already taken.  No canonicalization is done.
     */
    public Operation createOperation(
        String       name,
        String       description,
        List<String> accessTypeNames
        )
    {
        if (services.getOperations().findOperationByName(name) != null)
            throw new OperationException(FailureType.OPERATIONNAME_ALREADYUSED);

        Operation           oper = privSysServices.getPrivOperations().createOperation(name, description);
        PrivAccessTypeLogic atl  = privSysServices.getPrivAccessTypeLogic();

        super.rebuildCache();

        if (accessTypeNames != null)
        {
            for (String atName : accessTypeNames)
            {
                AccessType at = atl.getAccessType(atName);

                if (at != null)
                    oper.addAccessType(at);
                else
                    throw new OperationException(FailureType.NO_SUCH_ACCESSTYPE);  // "Access type [" + atName + "] does not exist"
            }

            privSysServices.getPrivOperations().update(oper);
        }

        return oper;
    }

    /**
     * Deletes the given AccessType from all Operations, saving them if they
     * are modified.  
     */
    void removeAccessTypesFromOperations(String name)
    {
        Operations ops     = privSysServices.getPrivOperations();
        boolean    rebuild = false;

        for (Operation op : getAllOperations())
        {
            if (op.removeAccessType(name))
            {
                ops.update(op);
                rebuild = true;
            }
        }

        // makes it so that anyone doing getAllOperations will get new ones
        if (rebuild)
            super.rebuildCache();
    }

}
