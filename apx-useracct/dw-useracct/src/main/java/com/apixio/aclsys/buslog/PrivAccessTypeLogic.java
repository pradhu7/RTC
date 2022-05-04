package com.apixio.aclsys.buslog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.aclsys.entity.AccessType;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.PrivSysServices;

/**
 * Contains reusable role level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 *
 * Generally speaking Roles are referred to by their names and not their XUUIDs as
 * this is more natural and more readable when debugging.  The set of roles doesn't
 * change much over time and they aren't user created so using a name doesn't present
 * any real problem.
 */
public class PrivAccessTypeLogic extends AccessTypeLogic {

    private PrivSysServices privSysServices;

    /**
     * Constructor.
     */
    public PrivAccessTypeLogic(PrivSysServices sysServices)
    {
        super(sysServices);

        this.privSysServices = sysServices;
    }

    /**
     * Create a new role with the given name, throwing an exception if the name
     * is already taken.  No canonicalization is done.
     */
    public AccessType createAccessType(String name, String description)
    {
        if (services.getAccessTypes().findAccessTypeByName(name) != null)
            throw new BaseException(FailureType.TYPENAME_ALREADYUSED);

        AccessType type = privSysServices.getPrivAccessTypes().createAccessType(name, description);

        super.rebuildCache();

        return type;
    }

    /**
     * Deletes the AccessType with the given name and propagates the deletion to
     * all Operations so if any of them use that AccessType, it will be deleted
     * from those lists.
     */
    public void deleteAccessType(String name)
    {
        privSysServices.getPrivAccessTypes().deleteAccessType(name);
        privSysServices.getPrivOperationLogic().removeAccessTypesFromOperations(name);

        super.rebuildCache();
    }

}
