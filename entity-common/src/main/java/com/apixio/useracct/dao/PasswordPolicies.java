package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.PasswordPolicy;

/**
 * PasswordPolicys defines the persisted level operations on PasswordPolicy entities.
 *
 */
public final class PasswordPolicies extends CachingBase<PasswordPolicy> {

    /**
     * Constructor
     */
    public PasswordPolicies(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.PASSWORD_POLICIES, PasswordPolicy.OBJTYPE);
    }

    /**
     * Looks for an PasswordPolicy instance with the given ID in Redis and if found returns
     * a restored PasswordPolicy instance.  Null is returned if the ID is not found.
     */
    public PasswordPolicy findPasswordPolicyByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new PasswordPolicy(fields);
        else
            return null;
    }

    /**
     * Refreshes the cache of roles as necessary and returns the role with the
     * given name, if any.
     */
    public PasswordPolicy findPasswordPolicy(String policyName)
    {
        for (XUUID id : getAllEntityIDs())
        {
            ParamSet ps = findInCacheByID(id);

            if (PasswordPolicy.eqName(policyName, ps))
                return new PasswordPolicy(ps);
        }

        return null;
    }

    /**
     * Returns a list of all PasswordPolicy objects.
     */
    public List<PasswordPolicy> getAllPasswordPolicies()
    {
        List<PasswordPolicy> all = new ArrayList<PasswordPolicy>();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
                all.add(new PasswordPolicy(fields));
        }

        return all;
    }

}
