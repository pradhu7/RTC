package com.apixio.datasource.redis;

import java.util.NoSuchElementException;
import java.util.Random;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisExhaustedPoolException;

/**
 * JedisPair is a support class (not public) that is a solution (enough of one)
 * to the "dining philosopher's problem" that comes up in the system due to the
 * need to have 2 Jedis connections for a single request: one connection is for
 * the MULTI (transaction) support, and one is for the then-required read
 * operations (since any read request in a MULTI won't get executed until the
 * EXEC on it).
 *
 * The operation of this class is to grab "atomically" two Jedis connection
 * objects from the configured JedisPool.  In order to do this the pool itself
 * MUST BE CONFIGURED so that it does *NOT* block on getResource() when the pool
 * is exhausted!  This configuration, sadly, cannot be done at this level so any
 * code/service that encounters a deadlock due to this philosopher's problem
 * must make the required configuration change.  The standard config name for
 * this is "jedisConfig.blockWhenExhausted".
 *
 * Additionally, the pool MUST be configured so that the maxWaitMillis timeout
 * (which is the max time the pool.getResource() call will wait if the pool is
 * exhausted) is SMALL (10s of millis perhaps); otherwise, the Jedis objects
 * grabbed during "phase 1" won't be returned to the pool quickly, and the
 * getResource for the second Jedis connection will take longer to fail, which
 * could slow down the overall throughput.  The standard config name for
 * this is "jedisConfig.maxWaitMillis".
 *
 * The actual algorithm is pretty simple: we do a blocking getResource to get
 * the first Jedis connection (where "blocking" means actually with the short
 * timeout described above, so repeated attempts are made, sleeping between
 * attempts), and if the number of callers waiting for a Jedis object from the
 * pool is small-ish, then attempt to grab the second one without blocking (with
 * the caveat that the time spent waiting for this second one is subject to the
 * same maxWaitMillis timeout).  If we actually can't get the second Jedis
 * connection, then return the first Jedis object to the pool (allowing another
 * requestor to get it) and wait for a small random time and then try it all
 * again.
 */
class JedisPair
{
    /**
     * FEW_WAITERS defines how many outstanding requests for Jedis objects
     * across all pool users are allowed before we just give up on the current
     * atomic attempt and give back the Jedis object already grabbed.
     */
    private final static int FEW_WAITERS  = 2;

    /**
     * Max number of milliseconds to wait before reattempting to grab both
     * required connections.
     */
    private final static int MAX_BACKOFF = 50;

    /**
     * For random backoff times
     */
    private static Random sleeper = new Random();

    /**
     * The two Jedis connections.  Both will be non-null
     */
    Jedis jedis1;
    Jedis jedis2;
    
    JedisPair(Jedis jedis1, Jedis jedis2)
    {
        if ((jedis1 == null) || (jedis2 == null))
            throw new IllegalArgumentException("Null Jedis objects are not allowed");

        this.jedis1 = jedis1;
        this.jedis2 = jedis2;
    }

    /**
     * Grabs a pair of Jedis connection objects in an atomic way:  the method won't
     * return unless both can connections can be obtained.  The algorithm used is
     * described above.
     */
    static JedisPair getPair(JedisPool pool)
    {
        Jedis jedis1 = null;
        Jedis jedis2 = null;

        while (true)
        {
            jedis1 = borrowWithBlocking(pool);

            // heuristic:  if the pool isn't so busy as to have many waiters then
            // try to grab a connection quickly
            if (pool.getNumWaiters() < FEW_WAITERS)
            {
                jedis2 = borrowNoBlocking(pool);

                if (jedis2 != null)
                    return new JedisPair(jedis1, jedis2);
            }

            jedis1.close();

            safeSleep(random(MAX_BACKOFF));
        }
    }

    /**
     * Release both Jedis connections
     */
    void releasePair()
    {
        release(jedis1);
        release(jedis2);
    }

    /**
     * Attempt to grab a Jedis connection, returning null if one isn't available
     * within the pool's configured timeout.
     *
     * Note that the code catches two different exceptions as both have been
     * documented as being thrown when the pool is exhausted (though during
     * testing only JedisExhaustedPoolException was thrown).
     */
    static private Jedis borrowNoBlocking(JedisPool pool)
    {
        try
        {
            return pool.getResource();
        }
        catch (NoSuchElementException | JedisExhaustedPoolException x)  // expected exceptions
        {
            return null;
        }
    }

    /**
     * Return a Jedis connection, waiting as necessary for one to become available.
     * If one isn't available then sleep a bit and try again (busy-ish wait).
     */
    static private Jedis borrowWithBlocking(JedisPool pool)
    {
        while (true)
        {
            try
            {
                return pool.getResource();
            }
            catch (NoSuchElementException | JedisExhaustedPoolException x)  // expected when busy--just wait some more
            {
                safeSleep(MAX_BACKOFF);
            }
        }
    }

    /**
     *
     */
    private void release(Jedis jedis)
    {
        try
        {
            jedis.close();
        }
        catch (Exception x)
        {
        }
    }

    private static int random(int max)
    {
        return sleeper.nextInt(max);
    }

    private static void safeSleep(int ms)
    {
        try
        {
            Thread.currentThread().sleep(ms);
        }
        catch (InterruptedException i)
        {
        }
    }

}
