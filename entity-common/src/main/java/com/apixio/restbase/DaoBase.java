package com.apixio.restbase;

import com.apixio.XUUID;
import com.apixio.datasource.redis.RedisOps;

/**
 * Every DAO that uses redis for persistence must extend this base class.
 *
 * DaoBase is the base DAO that provides common implementation setup and
 * presents a centrally configurable Redis-key prefix that allows separation
 * of namespaces of different useracct instances using the same Redis server.
 */
public class DaoBase
{
    /**
     *
     */
    protected PersistenceServices persistence;
    protected RedisOps            redisOps;

    /**
     * Master key prefix
     */
    private String masterPrefix = "";
    private int    prefixLength = 0;

    public DaoBase(PersistenceServices persistenceServices)
    {
        String keyPrefix = persistenceServices.getRedisKeyPrefix();

        if ((keyPrefix == null) || keyPrefix.equals(""))
            throw new IllegalStateException("Cowardly refusing to create a DaoBase with an empty redis keyPrefix");

        this.persistence  = persistenceServices;  // for safe-keeping (wish it didn't need to be done this way)
        this.redisOps     = persistenceServices.getRedisOps();
        this.masterPrefix = keyPrefix;
        this.prefixLength = keyPrefix.length();
    }

    /**
     * Convenience method to set up redis information given an already initialized
     * Persistable instance (this model assumes that the initial instance be
     * set up via a call to the other constructor).
     */
    protected DaoBase(DaoBase seed)
    {
        if ((seed == null) || (seed.redisOps == null))
            throw new IllegalArgumentException("Attempt to create a Persistable instance using a bad 'seed' object");

        this.persistence  = seed.persistence;
        this.redisOps     = seed.redisOps;
        this.masterPrefix = seed.masterPrefix;
        this.prefixLength = seed.prefixLength;
    }

    /**
     * Getters
     */
    final public PersistenceServices getPersistenceServices()
    {
        return persistence;
    }
    final public RedisOps getRedisOps()
    {
        return redisOps;
    }
    final public String getKeyPrefix()
    {
        return masterPrefix;
    }

    /**
     * Makes the actual Redis key for all scalar fields
     */
    final public String makeKey(String sub)
    {
        return masterPrefix + sub;
    }
    final public String makeKey(XUUID sub)
    {
        return masterPrefix + sub.toString();
    }

    /**
     * Undoes the effect of makeKey(String).
     */
    final protected String unmakeKey(String key)
    {
        if (key.startsWith(masterPrefix))
            return key.substring(prefixLength);
        else
            return key;
    }

}
