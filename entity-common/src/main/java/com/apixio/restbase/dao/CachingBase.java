package com.apixio.restbase.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.datasource.redis.MapResponse;
import com.apixio.datasource.redis.RedisPipeline;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.DaoBase;

/**
 * Provides a caching layer for entity types that are accessed often as collections.
 *
 * Entity types that want caching on the entire collection of entities of that type
 * can have that DAO extend this class.  The DAO, buslog, and client patterns that
 * must be followed are:
 *
 *  * the buslog class method that creates new entities must call CachingBase.create
 *    to have the new instance recorded in the cache and the "all entities" index.
 *
 *  * the DAO method(s) that want/need to deal with all entities (say, for a search
 *    or in Java join) must call getAllEntityIDs and then call findInCacheByID with
 *    the IDs or performance will suffer as the normal findByID still goes directly
 *    to redis
 *
 * Other tidbits:
 *
 *  * the entire collection of objects is versioned and will be reloaded if the
 *    version number is old
 *
 *  * update() does NOT check if the cache is of date with respect to redis
 *    
 */
public abstract class CachingBase<T extends BaseEntity> extends EntitySetBase<T> {

    private Map<XUUID, ParamSet> allEntities = Collections.synchronizedMap(new HashMap<XUUID, ParamSet>());

    protected DataVersions dataVersions;
    private   int          cachedVersion;

    /**
     * Do NOT change the datatype name or ALL existing entities will NOT be loadable!!
     */
    public CachingBase(DaoBase seed, DataVersions dv, String datatype, String... xuuidObjTypes)
    {
        this(seed, null, dv, datatype, xuuidObjTypes);
    }

    public CachingBase(DaoBase seed, String domain, DataVersions dv, String datatype, String... xuuidObjTypes)
    {
        super(seed, domain, datatype, xuuidObjTypes);

        if ((datatype == null) || (datatype.trim().length() == 0))
            throw new IllegalArgumentException("Datatype name must not be null/empty");

        this.dataVersions  = dv;
    }

    /**
     * General business logic entity creation pattern is to do a new() on the POJO entity
     * (which creates/assigns the XUUID), and then calls this method.
     */
    public void create(T entity)
    {
        super.create(entity);

        dataVersions.incrDataVersion(getDatatypeName());

        // put last just in case there's an exception
        allEntities.put(entity.getID(), entity.produceFieldMap());
    }
    
    /**
     * Write-through cache.
     */
    @Override
    public void update(T entity)
    {
        XUUID id = validateEntity(entity);

        super.update(entity);

        dataVersions.incrDataVersion(getDatatypeName());

        // put last just in case there's an exception
        allEntities.put(id, entity.produceFieldMap());
    }
    
    /**
     * Write-through cache.
     */
    @Override
    public void delete(T base)
    {
        XUUID id = super.validateEntity(base);

        super.delete(base);

        dataVersions.incrDataVersion(getDatatypeName());

        // remove last just in case there's an exception
        allEntities.remove(id);
    }
    
    /**
     * Returns a (copied) collection of all the known entity IDs.  It's expected generally
     * that the calling code will follow this with calls to findInCacheByID in a loop.
     */
    @Override
    public Collection<XUUID> getAllEntityIDs()
    {
        cacheLatest();

        return new ArrayList<XUUID>(allEntities.keySet());  // return copy of it
    }

    /**
     * Provides the caching by just looking in the up-to-date "allEntities" map.
     */
    protected ParamSet findInCacheByID(XUUID id)
    {
        return allEntities.get(id);
    }

    /**
     * Intercepts the find() call to update cache.  Note that this does *not* look
     * in the cache--it only updates it so that subsequent calls to findInCacheByID
     * will get the updated one (this could be a rare or even non-existent case in
     * reality...).
     */
    @Override
    protected ParamSet findByID(XUUID id)
    {
        if (!allowedType(id.getType()))
            throw new IllegalArgumentException("Attempt to find object using the wrong type of XUUID; ID is " + id + " but allowed types are " +
                                               getAllowedObjType());

        ParamSet ps = super.findByID(id);

        if ((ps != null) && (ps.size() > 0))
            allEntities.put(id, ps);

        return ps;
    }

    /**
     * Checks if the current version of the data kept in cache is up to date with what's
     * in redis.  If the version is out of date, then *all* elements in the collection
     * are read in (as we don't keep per-entity markers for out-of-date-ness).
     */
    public boolean cacheLatest()
    {
        int      latestVersion = dataVersions.getDataVersion(getDatatypeName());
        boolean  cached        = false;

        if (cachedVersion < latestVersion)
        {
            synchronized (allEntities)
            {
                List<MapResponse>  maps  = new ArrayList<>();
                RedisPipeline      rp    = redisOps.beginPipeline();

                allEntities.clear();

                try
                {
                    for (XUUID xid : super.getAllEntityIDs())
                    {
                        maps.add(rp.hgetAll(super.makeKey(xid.toString())));
                    }

                    rp.waitForAll();

                    for (MapResponse mr : maps)
                    {
                        Map<String, String> map = mr.get();
                        String              key = unmakeKey(mr.key());

                        if ((map != null) && (map.size() > 0))
                            allEntities.put(XUUID.fromString(key), new ParamSet(map));
                    }
                }
                finally
                {
                    rp.endPipeline();
                }
            }

            cachedVersion = latestVersion;

            cached = true;

            // notify subclasses of this
            cacheWasUpdated();
        }

        return cached;
    }

    /**
     * cacheWasUpdated should be overridden by subclasses that care to be notified that the
     * lowest level cache of ParamSets was updated so they can rebuild things as necessary.
     */
    protected void cacheWasUpdated()
    {
        // intentionally do nothing
    }

}
