package com.apixio.aclsys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.utility.StringList;

/**
 * An AclConstraint that requires that the tested thing is a member of the
 * defined set of XUUIDs.
 */
public class SetConstraint extends AclConstraint {

    private Collection<XUUID> allowed;

    /**
     * Package-protection.
     */
    SetConstraint()
    {
    }

    SetConstraint(Collection<XUUID> allowed)
    {
        this.allowed = allowed;
    }

    @Override
    protected String toParams()
    {
        List<String> ids = new ArrayList<String>(allowed.size());

        for (XUUID id : allowed)
            ids.add(id.toString());

        return StringList.flattenList(ids);
    }

    @Override
    protected void fromParams(String params)
    {
        allowed = new ArrayList<XUUID>();

        for (String id : StringList.restoreList(params))
            allowed.add(XUUID.fromString(id));
    }

    @Override
    public boolean meetsCriteria(SysServices sysservices, XUUID thing)
    {
        return allowed.contains(thing);
    }

}
