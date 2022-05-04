package com.apixio.aclsys.buslog;

import java.io.IOException;

import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.aclsys.entity.Operation;
import com.apixio.useracct.PrivSysServices;

/**
 * Privileged ACL operations.
 */
public class PrivAclLogic extends AclLogic {

    private PrivSysServices privSysServices;

    /**
     * Constructor.
     */
    public PrivAclLogic(PrivSysServices sysServices) throws IOException
    {
        super(sysServices);

        this.privSysServices = sysServices;
    }

    /**
     * Delete an Operation by deleting ALL references to it.  THIS IS EXTREMELY DESTRUCTIVE!
     * All granted permissions will be removed, etc.
     */
    public void deleteOperation(String opname) throws IOException
    {
        if (aclDao.canGetSubjectsByOperation())
        {
            Operation op = getOperation(opname);

            privSysServices.getPrivOperations().deleteOperation(opname);  // should be easier to recover from

            aclDao.deleteOperation(op);
        }
        else
        {
            throw new IllegalStateException("Unable to deleteOperation from ACL system because Cassandra hasn't been scanned");
        }
    }

}
