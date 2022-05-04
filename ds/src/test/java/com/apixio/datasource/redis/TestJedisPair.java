package com.apixio.datasource.redis;

import java.util.Random;

import static org.junit.Assert.*;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.apixio.datasource.redis.JedisPair;

/**
 * 2021-11-19 This used to run via junit but that was causing problems so testing
 * of this must be done via cmdline:
 *
 *  mvn -o -Dredishost=localhost -Dthreadcount=3 -Dtest=TestJedisPair test
 *
 * for full control.  If redishost is not defined, then use staging; if threadcount
 * is not defined, use more than the pool size, which should cause the bad test to
 * fail.
 */
public class TestJedisPair
{

    private Logger    logger = LoggerFactory.getLogger(TestJedisPair.class);
    private Random    random = new Random();
    private JedisPool jedisPoolShortBlock;
    private JedisPool jedisPoolBlocking;
    private int       threadCount;       // as a field allows better cmdline testing
    
    private final static int POOL_SIZE    = 10;
    private final static int LOOP_COUNT   = 5;  // 5 seems to be enough to cause expected failures

    private void log(String s)
    {
        System.out.println(s);
    }

    // silently close a Jedis connection, returning it to the pool
    private void jedisClose(Jedis j)
    {
        try
        {
            j.close();
        }
        catch (Exception x)
        {
        }
    }

    /**
     * Set up the block and non-blocking jedis pools
     */
    //-- don't junit test    @Before
    public void setUp() throws Exception
    {
        String                  host         = System.getProperty("redishost");
        String                  count        = System.getProperty("threadcount");
        GenericObjectPoolConfig jedisConfig;

        host  = (((host  != null) && (host.trim().length()  > 0)) ? host  : "redis-1-stg.apixio.com");
        count = (((count != null) && (count.trim().length() > 0)) ? count : "30");

        threadCount = Integer.parseInt(count);

        log("Using redis host " + host + " with thread count " + threadCount);

        jedisConfig = new GenericObjectPoolConfig();
        jedisConfig.setTestWhileIdle(true);
        jedisConfig.setTestOnBorrow(true);
        jedisConfig.setMaxTotal(POOL_SIZE);
        jedisConfig.setMaxWaitMillis(10);
        jedisConfig.setBlockWhenExhausted(false);

        // use default port--no one has ever set it to something else...
        jedisPoolShortBlock = new JedisPool(jedisConfig, host, 6379, 60000);

        // now create a blocking one to model the deadlock
        jedisConfig = new GenericObjectPoolConfig();
        jedisConfig.setTestWhileIdle(true);
        jedisConfig.setTestOnBorrow(true);
        jedisConfig.setMaxTotal(POOL_SIZE);
        jedisConfig.setMaxWaitMillis(100);         // different from above
        jedisConfig.setBlockWhenExhausted(true);   // different from above

        jedisPoolBlocking = new JedisPool(jedisConfig, host, 6379, 60000);
     }

    /**
     * uses JedisPair to atomically grab two connections from pool and all should work
     */
    //-- don't junit test    @Test
    public void betterNotDeadlockTest()
    {
        runnableDriver("[should run fine]",
                       () -> {
                           for (int l = 0; l < LOOP_COUNT; l++)
                           {
                               JedisPair jp = JedisPair.getPair(jedisPoolShortBlock);

                               try
                               {
                                   Thread.sleep(random.nextInt(10));
                               }
                               catch (InterruptedException ix)
                               {
                               }

                               jp.releasePair();
                           }
                       });
    }

    /**
     * sequentially grabs 2 jedis connections from the pool and with enough threads the
     * pool should become exhausted and problems should occur.
     */
    //-- don't junit test    @Test
    public void nonAtomicGrab2()
    {
        runnableDriver("[should fail miserably]",
                       () -> {
                           for (int l = 0; l < LOOP_COUNT; l++)
                           {
                               // this is a non-atomic grab of 2 resources from the pool, and should deadlock
                               Jedis j1 = jedisPoolBlocking.getResource();
                               Jedis j2 = jedisPoolBlocking.getResource();

                               try
                               {
                                   Thread.sleep(random.nextInt(10));
                               }
                               catch (InterruptedException ix)
                               {
                               }

                               jedisClose(j1);
                               jedisClose(j2);
                           }
                       });
    }

    // implementation
    private void runnableDriver(String info, Runnable r)
    {
        Thread[] threads = new Thread[threadCount];
        
        log(info + ":  starting up " + threadCount + " threads, each one doing " + LOOP_COUNT + " iterations");

        // each thread must:
        //  JedisPair jp = JedisPair.getPair(jedisPoolShortBlock);
        //  jp.releasePair();

        for (int i = 0; i < threadCount; i++)
        {
            threads[i] = new Thread(r);

            threads[i].start();
        }

        // wait for them all to die
        for (int i = 0; i < threadCount; i++)
        {
            try
            {
                threads[i].join();
            }
            catch (InterruptedException ix)
            {
            }
        }

        log(info + ":  no deadlocks");
    }

    //-- don't junit test    @After
    public void tearDown()
    {
    }

}
