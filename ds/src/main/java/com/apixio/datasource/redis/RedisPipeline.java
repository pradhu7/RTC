package com.apixio.datasource.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * RedisPipeline is a smaller wrapper over the Jedis/redis pipeline construct.  While
 * it is similar to a transaction on the redis/jedis side, it's exposed here as
 * something more explicit, mostly because we can get by with a very few number
 * of commands that need to be pipelined.
 *
 * The client usage pattern should be:
 *
 *  List<MapResponse> maps = new ArrayList<>();
 *  RedisPipeline     rp   = redisOps.beginPipeline();
 *
 *  try {
 *      for (some looping var)
 *          maps.add(rp.hgetAll(makeKey(key)));
 *
 *      rp.waitForAll();
 *
 *      for (MapResponse mr : maps)
 *        doSomethingWithMap(mr.get());
 *
 *  }
 *  finally {
 *      rp.endPipeline();
 *  }
 */
public class RedisPipeline
{
    /**
     * Pipeline:  Jedis object
     * RedisOps:  so we can clean up
     * JWrap:     to follow RedisOps' pattern re alloc/free
     */
    private Pipeline pipeline;
    private RedisOps redisOps;
    private Jedis    jedis;
    private boolean  closeJedis;
    private boolean  syncDone;

    /**
     * Package-protected; only RedisOps.beginPipeline should call this.
     */
    RedisPipeline(Jedis jedis, boolean closeJedis)
    {
        this.jedis      = jedis;
        this.closeJedis = closeJedis;

        pipeline = jedis.pipelined();
    }

    /**
     * End the pipeline by freeing up resources
     */
    public void endPipeline()
    {
        if (!syncDone)
            throw new IllegalStateException("RedisPipeline.waitForAll MUST be done before endPipeline");

        if (closeJedis)
            jedis.close();
    }

    /**
     * Push another pipelined command to redis to get all fields from a HASH
     */
    public MapResponse hgetAll(String key)
    {
        return new MapResponse(key, pipeline.hgetAll(key));
    }

    /**
     * Do a jedis 'sync' command to wait for the results from all the commands.
     */
    public void waitForAll()
    {
        pipeline.sync();
        syncDone = true;
    }
}

