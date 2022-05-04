package com.apixio.restbase.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.datasource.redis.MapResponse;
import com.apixio.datasource.redis.RedisPipeline;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * BaseEntities provides/defines simple CRUD semantics for items
 * persisted in Redis.  All DAOs should extend this class.
 */
public abstract class BaseEntities<T extends BaseEntity> extends DaoBase {

    /**
     * This funky constructor form allows the caller to initialize a new
     * DAO with an existing DAO.
     */
    protected BaseEntities(DaoBase seed)
    {
        super(seed);
    }

    /**
     * Looks for an entity instance with the given ID in Redis and if found returns
     * a restored instance.  Null is returned if the ID is not found.
     *
     * Only scalar data fields are filled in (no counters or lists).
     */
    protected ParamSet findByID(XUUID id)
    {
        Map<String, String> fields = redisOps.hgetAll(makeKey(id));

        if (fields.size() > 0)
            return new ParamSet(fields);
        else
            return null;
    }

    /**
     * Update the entity by writing out its field map.
     */
    public void update(T base)
    {
        redisOps.hmset(makeKey(base.getID()), base.produceFieldMap().toMap());
    }

    /**
     * Delete an entity by removing its Redis key.
     */
    protected void delete(T base)
    {
        redisOps.del(makeKey(base.getID()));
    }

    /**
     * Given a set of XUUIDs, look them all up (pipelined) and return
     * the ParamSets that can be used to reconstruct the actual objects.
     */
    protected List<ParamSet> getEntitiesByIDs(Collection<XUUID> ids)
    {
        List<ParamSet>     psets  = new ArrayList<>(ids.size());
        List<MapResponse>  maps   = new ArrayList<>();
        RedisPipeline      rp     = redisOps.beginPipeline();
        boolean            waited = false;

        try
        {
            for (XUUID id : ids)
                maps.add(rp.hgetAll(makeKey(id.toString())));

            rp.waitForAll();

            // otherwise a failure in rp.hgetAll will incorrectly force endPipeline
            waited = true;

            for (MapResponse mr : maps)
            {
                ParamSet ps = fromMapResponse(mr);

                if (ps != null)
                    psets.add(ps);
            }
        }
        finally
        {
            if (waited)
                rp.endPipeline();
        }

        return psets;
    }

    /**
     * Restore/recreate a ParamSet from a (Jedis) pipelined MapResponse.
     */
    protected ParamSet fromMapResponse(MapResponse mr)
    {
        Map<String, String> res = mr.get();

        if (res.size() > 0)
            return new ParamSet(res);
        else
            return null;
    }

}
