package com.apixio.datasource.redis;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test Redis DistLock
 */
@Ignore("Integration")
public class TestDistLock
{
    private RedisOps ops;
    private DistLock dlock;

    @Before
    public void setUp() throws Exception
    {
        ops = new RedisOps("localhost", 6379);
        dlock = new DistLock(ops, "slydon-local-");
    }

    @Test
    public void testLocking()
    {
        String key = UUID.randomUUID().toString();
        String lock = dlock.lock(key, 1000);
        assertNotNull(lock);
        assertTrue(dlock.isLocked(lock, key));
        assertNotNull(dlock.refresh(lock, key, 1000));
        dlock.unlock(lock, key);
    }

    @Test
    public void testTimingOut() throws Exception
    {
        String key = UUID.randomUUID().toString();
        String lock = dlock.lock(key, 1000);
        assertNotNull(lock);
        assertTrue(dlock.isLocked(lock, key));
        Thread.sleep(1000);
        assertFalse(dlock.isLocked(lock, key));
    }

    @Test
    public void testContention()
    {
        String key = UUID.randomUUID().toString();
        String lock = dlock.lock(key, 1000);
        assertNull(dlock.lock(key, 1000));
        dlock.unlock(lock, key);
        String lock2 = dlock.lock(key, 1000);
        assertNotNull(lock2);
        dlock.unlock(lock2, key);
    }
}
