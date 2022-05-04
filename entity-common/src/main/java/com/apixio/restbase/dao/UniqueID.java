package com.apixio.restbase.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.DaoBase;
import com.apixio.XUUID;

/**
 * UniqueID is meant to be a helper class for DAOs for managing entities that
 * have a client-defined unique ID.  Breaking it out as a class like this allows
 * the actual entity DAO to extend something possibly more useful (e.g., Caching
 * or NAssocs).
 *
 * The use pattern is for a real DAO to create an instance of this helper and
 * to use the methods as follows:
 *
 *  * before a new entity is added, (optionally?) call getUniqueID() to see if the ID is already
 *    used
 *
 *  * when adding a new id, call addUniqueID.  If the ID is already in use
 *
 *  * when deleting an id, call removeUniqueID.
 *
 * The redis structure used is a HASH:
 *
 *  * redis key:   {prefix}unique-{typename}
 *  * redis type:  HASH
 *    HASH field name:   the unique ID
 *    HASH field value:  the entity ID
 */
public class UniqueID {

    /**
     * Constants for redis key names
     */
    private final static String K_UNIQUEIDX  = "unique-";

    /**
     * 
     */
    private String    keyUniqueIndex;
    private RedisOps  redisOps;

    public UniqueID(DaoBase outer, String typename)
    {
        this.redisOps       = outer.getRedisOps();
        this.keyUniqueIndex = outer.makeKey(K_UNIQUEIDX + typename);
    }

    /**
     * 
     */
    public XUUID getUniqueID(String id)
    {
        String  xuuid = redisOps.hget(keyUniqueIndex, id);

        if (xuuid != null)
            return XUUID.fromString(xuuid);
        else
            return null;
    }

    public void addUniqueID(String id, XUUID entityID)
    {
        // race condition possible here... not much can be done about it other than
        // not automatically starting a transaction in the filter and then doing a
        // WATCH on the key and then an HSETNX...  not worth it right now

        redisOps.hset(keyUniqueIndex, id, entityID.toString());
    }

    public void removeUniqueID(String id)
    {
        redisOps.hdel(keyUniqueIndex, id);
    }

    public void delete()
    {
        redisOps.del(keyUniqueIndex);
    }

    /**
     * Returns all the HASH field values.
     */
    public List<XUUID> getUniqueIDs()
    {
        List<XUUID> ids = new ArrayList<>();

        for (String id : redisOps.hvals(keyUniqueIndex))
            ids.add(XUUID.fromString(id));

        return ids;
    }

}
