package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.RoleSet;
import com.apixio.restbase.DaoBase;

/**
 * Data access object representing sets of roles.  The collection of RoleSets are cached
 * and no indexing by name (in redis) is required.
 *
 *  * to keep track of roles within a type:
 *     - type:  SET
 *     - key:   orgtype.{orgTypeID}
 *     - value: {orgRoleID}
 */
public final class RoleSets extends CachingBase<RoleSet>
{
    /**
     * Redis key name base for keeping track of the roles within each set.
     * Do NOT change this or all persisted instances and properties will be lost!
     */
    private final static String SET_MEMBERS_BASE = "rolesetemembers.";

    /**
     * Key is nameID.
     */
    private Map<String, RoleSet> nameToSet = new HashMap<String, RoleSet>();

    /**
     * Caching info to maintain map of org set ID to set of members (org roles)
     */
    private Map<XUUID, Set<XUUID>> setToMembers = new HashMap<XUUID, Set<XUUID>>();
    private int                    cachedVersion;

    /**
     * Hold on to dataVersions as we do explicit caching ourselves.
     */
    private DataVersions dataVersions;

    /**
     * Creates a new RoleSets DAO instance.
     */
    public RoleSets(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.ROLESETS, RoleSet.OBJTYPE);

        this.dataVersions = dv;
    }

    /**
     * Persists the new RoleSet
     */
    public void create(RoleSet rs)
    {
        if ((rs == null) || (findRoleSetByNameID(rs.getNameID()) != null))
            throw new IllegalArgumentException("RoleSet is null or nameID is already used");

        super.create(rs);
    }

    /**
     *
     */
    public List<RoleSet> getAllRoleSets()
    {
        List<RoleSet> all = new ArrayList<RoleSet>();

        for (XUUID id : getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
                all.add(new RoleSet(fields));
        }

        return all;
    }

    /**
     * Looks for an RoleSet instance with the given ID in Redis and if found returns
     * a restored RoleSet instance.  Null is returned if the ID is not found.
     */
    public RoleSet findCachedRoleSetByID(XUUID id)
    {
        return fromParamSet(findInCacheByID(id));
    }

    /**
     * Looks for an RoleSet instance with the given ID in Redis and if found returns
     * a restored RoleSet instance.  Null is returned if the ID is not found.
     */
    public RoleSet findRoleSetByID(XUUID id)
    {
        return fromParamSet(findByID(id));
    }

    /**
     * Methods to manage roles within a set.
     */
    public void addRoleToSet(RoleSet set, Role role)
    {
        redisOps.sadd(makeMembersKey(set.getID()), role.getID().toString());
        dataVersions.incrDataVersion(Datanames.ROLESET_MEMBERS);

        getMembersSet(set.getID()).add(role.getID());

        cacheWasUpdated();
    }

    public void removeRoleFromSet(RoleSet set, Role role)
    {
        redisOps.srem(makeMembersKey(set.getID()), role.getID().toString());
        dataVersions.incrDataVersion(Datanames.ROLESET_MEMBERS);

        getMembersSet(set.getID()).remove(role.getID());

        cacheWasUpdated();
    }

    public Set<XUUID> getRolesBySet(RoleSet set)
    {
        updateMembersCache();

        return getMembersSet(set.getID());
    }

    /**
     * We allow a delete so expose as public.
     */
    @Override
    public void delete(RoleSet rs)
    {
        super.delete(rs);

        synchronized (nameToSet)
        {
            nameToSet.remove(rs.getNameID());
        }
    }

    /**
     *
     */
    public RoleSet findRoleSetByNameID(String nameID)
    {
        updateMembersCache();

        synchronized (nameToSet)
        {
            return nameToSet.get(nameID);
        }
    }

    /**
     *
     */
    private RoleSet fromParamSet(ParamSet fields)
    {
        if (fields != null)
            return new RoleSet(fields);
        else
            return null;
    }

    /**
     * Rebuild nameToSet as cache was reloaded from under us.
     */
    @Override
    protected void cacheWasUpdated()
    {
        synchronized (nameToSet)
        {
            nameToSet.clear();

            for (RoleSet rs : getAllRoleSets())
                nameToSet.put(rs.getNameID(), rs);
        }
    }

    /**
     * Reads in the data that links members (which are Roles) to their parent
     * RoleSet and rebuilds the cache setToMembers map.
     */
    private boolean updateMembersCache()
    {
        int      latestVersion = dataVersions.getDataVersion(Datanames.ROLESET_MEMBERS);
        boolean  cached        = false;

        if (cachedVersion < latestVersion)
        {
            Collection<XUUID> allRoleSets = getAllEntityIDs();  // updates cache

            //!! need to use pipelining here!! (eventually).
            // when i do that, just do a pipeline.sync() at the end and deal with Response<> after that

            synchronized (setToMembers)
            {
                setToMembers.clear();

                for (XUUID orgSetID : allRoleSets)
                {
                    Set<XUUID> members = getMembersSet(orgSetID);

                    for (String id : redisOps.smembers(makeMembersKey(orgSetID)))
                        members.add(XUUID.fromString(id));
                }
            }

            cachedVersion = latestVersion;

            cached = true;
        }

        return cached;
    }

    private Set<XUUID> getMembersSet(XUUID orgSetID)
    {
        synchronized (setToMembers)
        {
            Set<XUUID> set = setToMembers.get(orgSetID);

            if (set == null)
            {
                set = new HashSet<XUUID>();
                setToMembers.put(orgSetID, set);
            }

            return set;
        }
    }

    /**
     *
     */
    private String makeMembersKey(XUUID setID)
    {
        return super.makeKey(SET_MEMBERS_BASE + setID.toString());
    }

}
