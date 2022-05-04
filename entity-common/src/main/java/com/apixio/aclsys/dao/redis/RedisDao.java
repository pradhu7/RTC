package com.apixio.aclsys.dao.redis;

import java.util.Map;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;

/**
 * Base class for DAOs that talk to Redis.
 */
public class RedisDao
{
    /**
     * ThreadLocal will be true iff DAO code actually called redisTrans.begin().
     */
    private static ThreadLocal<Boolean> cStartedTransaction = new ThreadLocal<>();

    /**
     * To talk with Cassandra
     */
    protected RedisOps     redisOps;
    protected Transactions redisTrans;
    protected DaoBase      base;

    /**
     * Validate that we have a good CqlCrud and column family name.
     */
    protected RedisDao(DaoBase base)
    {
        if (base == null)
            throw new IllegalArgumentException("Null DaoBase!");

        PersistenceServices ps = base.getPersistenceServices();

        this.redisOps   = ps.getRedisOps();
        this.redisTrans = ps.getRedisTransactions();
        this.base       = base;
    }

    protected Map<String, String> getKeyHash(String key)
    {
        return redisOps.hgetAll(makeKey(key));
    }

    protected String getKeyHashValue(String key, String fieldName)
    {
        return redisOps.hget(makeKey(key), fieldName);
    }

    protected void setKeyHashValue(String key, String fieldName, String fieldValue)
    {
        redisOps.hset(makeKey(key), fieldName, fieldValue);
    }

    protected void deleteKeyHashField(String key, String fieldName)
    {
        redisOps.hdel(makeKey(key), fieldName);
    }

    protected void deleteKey(String key)
    {
        redisOps.del(makeKey(key));
    }

    protected void openTransaction()
    {
        if (!redisTrans.inTransaction())
        {
            redisTrans.begin();
            cStartedTransaction.set(Boolean.TRUE);
        }
    }

    protected void commitTransaction()
    {
        if (Boolean.TRUE.equals(cStartedTransaction.get()))
            redisTrans.commit();
    }

    protected void closeTransaction(boolean wasCommitted)
    {
        if (Boolean.TRUE.equals(cStartedTransaction.get()))
        {
            cStartedTransaction.set(Boolean.FALSE);  // do this first

            if (!wasCommitted)
                redisTrans.abort();
        }
    }

    protected String makeKey(String key)
    {
        return base.makeKey(key);
    }

}
