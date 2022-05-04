package com.apixio.datasource.redis;

import java.util.Map;

import redis.clients.jedis.Response;

/**
 * MapResponse wraps Jedis' Response<> object so RedisOps clients don't need to know about
 * Jedis and so we can keep track of the key that the response belongs to.
 */
public class MapResponse {

    /**
     * Response:  wrapped (future) Map<String, String> for when the result comes back
     * Key:  so we know what key the results are for
     */
    private Response<Map<String, String>> response;
    private String key;

    /**
     * Package protection
     */
    MapResponse(String key, Response<Map<String, String>> response)
    {
        this.key      = key;
        this.response = response;
    }

    /**
     * Return the key used to fetch the HASH exactly as it was presented to RedisPipeline.hgetAll
     */
    public String key()
    {
        return key;
    }

    /**
     * Return the final fetched Map<String, String>.  An exception might be thrown (e.g., if no
     * RedisPipeline.waitForAll has not been done).
     */
    public Map<String, String> get()
    {
        return response.get();
    }
}
