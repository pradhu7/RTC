package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.OrgType;

/**
 * Data access object representing organization types.  The collection of OrgTypes are cached
 * and no indexing by name (in redis) is required.
 */
public final class OrgTypes extends CachingBase<OrgType>
{
    /**
     * Hold on to dataVersions as we do explicit caching ourselves.
     */
    private DataVersions dataVersions;

    /**
     * Creates a new OrgTypes DAO instance.
     */
    public OrgTypes(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.ORGTYPES, OrgType.OBJTYPE);

        this.dataVersions = dv;
    }

    /**
     * Persists the new OrgType
     */
    public void create(OrgType ot)
    {
        if ((ot == null) || (findOrgTypeByName(ot.getName()) != null))
            throw new IllegalArgumentException("OrgType is null or name is already used");

        super.create(ot);
    }

    /**
     *
     */
    public List<OrgType> getAllOrgTypes()
    {
        List<OrgType> all = new ArrayList<OrgType>();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
                all.add(new OrgType(fields));
        }

        return all;
    }

    /**
     * Looks for an OrgType instance with the given ID in Redis and if found returns
     * a restored OrgType instance.  Null is returned if the ID is not found.
     */
    public OrgType findCachedOrgTypeByID(XUUID id)
    {
        return fromParamSet(findInCacheByID(id));
    }

    /**
     * Looks for an OrgType instance with the given ID in Redis and if found returns
     * a restored OrgType instance.  Null is returned if the ID is not found.
     */
    public OrgType findOrgTypeByID(XUUID id)
    {
        return fromParamSet(findByID(id));
    }

    /**
     *
     */
    public OrgType findOrgTypeByName(String name)
    {
        for (XUUID id : getAllEntityIDs())
        {
            ParamSet ps = findInCacheByID(id);

            if (OrgType.eqName(name, ps))
                return new OrgType(ps);
        }

        return null;
    }

    /**
     *
     */
    private OrgType fromParamSet(ParamSet fields)
    {
        if (fields != null)
            return new OrgType(fields);
        else
            return null;
    }

}
