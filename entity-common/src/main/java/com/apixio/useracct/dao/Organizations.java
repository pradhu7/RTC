package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.Organization;

/**
 * Organizations defines the persisted level operations on Organization entities.
 *
 * Redis structures:
 *
 *  * key={XUUID-of-org};    HASH for user org metadata (object); keys are field names, values are field values
 *  * key="org2-x-byid";     HASH for INDEX_BY_XID; keys are external IDs, values are internal IDs
 *
 */
public final class Organizations extends CachingBase<Organization> {

    /**
     * Constructor
     */
    public Organizations(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.ORGANIZATIONS, Organization.OBJTYPE);
    }

    /**
     * Looks for an Organization instance with the given ID in Redis and if found returns
     * a restored Organization instance.  Null is returned if the ID is not found.
     */
    public Organization findOrganizationByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new Organization(fields);
        else
            return null;
    }

    /**
     * Looks for an Organization instance with the given externalID.
     */
    public Organization findOrganizationByExternalID(String extID)
    {
        for (XUUID id : getAllEntityIDs())
        {
            ParamSet ps = findInCacheByID(id);

            if (Organization.eqExternalID(extID, ps))
                return new Organization(ps);
        }

        return null;
    }

    /**
     * Looks for an Organization instance with the given name
     */
    public Organization findOrganizationByName(String name)
    {
        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet ps = findInCacheByID(id);

            if (Organization.eqName(name, ps))
                return new Organization(ps);
        }

        return null;
    }

    /**
     * Returns a list of all Organization objects.
     */
    public List<Organization> getAllOrganizations(boolean includeInactive)
    {
        List<Organization> all = new ArrayList<Organization>();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
            {
                Organization org = new Organization(fields);

                if (includeInactive || org.getIsActive())
                    all.add(org);
            }
        }

        return all;
    }

    /**
     * Migration support.  Blech.
     */
    public String makeRedisKey(String thing)
    {
        return super.makeKey(thing);
    }

}
