package com.apixio.aclsys;

import com.apixio.SysServices;
import com.apixio.XUUID;

/**
 * An AclConstraint that always meets the constraint criteria.
 */
public class WildcardConstraint extends AclConstraint {

    /**
     * Package-protection.
     */
    WildcardConstraint()
    {
    }

    @Override
    protected String toParams()
    {
        return "";
    }

    @Override
    protected void fromParams(String params)
    {
    }

    @Override
    public boolean meetsCriteria(SysServices sysservices, XUUID thing)
    {
        return true;
    }

}
