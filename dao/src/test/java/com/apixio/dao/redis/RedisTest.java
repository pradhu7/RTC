package com.apixio.dao.redis;


import com.apixio.dao.DAOTestUtils;
import com.apixio.datasource.redis.RedisOps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


@Ignore("Integration")
public class RedisTest {

    private DAOTestUtils       util;

    @Before
    public void setUp() throws Exception
    {
        util = new DAOTestUtils();
    }

    public String makeKey(String prefix, String key) {
        return prefix + "_" + key;
    }

    @Test
    public void testRedis() {
        try
        {
            RedisOps redisOps = util.daoServices.getRedisOps();
            RedisOps redisOpsGoldfish = util.daoServices.getRedisOps("goldfish");

            assertFalse(redisOps.get("test1001") != null && redisOps.get("test1001").equals("hello"));
            if (redisOps.get("test1001") == null)
            {
                redisOps.set(makeKey(util.daoServices.getRedisKeyPrefix(), "test1001"), "hello");
                redisOps.expire(makeKey(util.daoServices.getRedisKeyPrefix(), "test1001"), 1 );

                redisOpsGoldfish.set(makeKey(util.daoServices.getRedisKeyPrefix("goldfish"), "test1001"), "hello");
                redisOpsGoldfish.expire(makeKey(util.daoServices.getRedisKeyPrefix("goldfish"), "test1001"), 3 );

                assertEquals("hello", redisOps.get(makeKey(util.daoServices.getRedisKeyPrefix(), "test1001")));

                Thread.sleep(2000);

                assertNull(redisOps.get(makeKey(util.daoServices.getRedisKeyPrefix(), "test1001")));
                assertEquals("hello", redisOpsGoldfish.get(makeKey(util.daoServices.getRedisKeyPrefix("goldfish"), "test1001")));

                Thread.sleep(3000);

                assertNull(redisOps.get(makeKey(util.daoServices.getRedisKeyPrefix(), "test1001")));
                assertNull(redisOpsGoldfish.get(makeKey(util.daoServices.getRedisKeyPrefix("goldfish"), "test1001")));
            }
        } catch (Exception ex) {
            fail();
            ex.printStackTrace();
        }
    }
}
