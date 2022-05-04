package com.apixio.datasource.redis;

import java.util.Arrays;

/**
 * DistLock defines a generic locking mechanism that is described at the beginning
 * of: http://redis.io/topics/distlock
 *
 * 1. Locks will timeout
 * 2. Locks can be checked
 * 3. Locks can be released
 * 4. Locks can be unlocked prior to the timeout
 *
 * Usage:
 *
 * DistLock dlock = new DistLock(redisOps, "prefix-");
 * String lock = dlock.lock(key, duration); //returns null if unable to acquire
 * if (lock != null) {
 *   dlock.refresh(lock, key, duration); //boolean
 *   dlock.isLocked(lock, key);  //boolean
 *   dlock.unlock(lock, key); //void
 * }
 *
 */
public class DistLock {

    /**
     * LUA script for refreshing a lock.
     */
    private final static String REFRESH_SCRIPT = (
        "if redis.call(\"GET\",KEYS[1]) == ARGV[1] then\n" +
        "  redis.call(\"PEXPIRE\", KEYS[1], ARGV[2])\n" +
        "end\n"
        );

    /**
     * LUA script for unlocking.
     */
    private final static String UNLOCK_SCRIPT = (
        "if redis.call(\"GET\", KEYS[1]) == ARGV[1] then\n" +
        "  redis.call(\"DEL\", KEYS[1])\n" +
        "end\n"
        );

    /**
     * Access to all required Redis operations
     */
    private RedisOps redisOps;

    /**
     * The prefix for all lock keys.
     */
    private String prefix;

    /**
     * Spring setters
     */
    public void setRedisOps(RedisOps ops)
    {
        this.redisOps = ops;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Constructs an object for handling distibuted locking in redis.
     */
    public DistLock(RedisOps ops, String prefix)
    {
        this.redisOps = ops;
        this.prefix = prefix;
    }

    /**
     * Lock returns the lock string if a lock was able to be aquired. If the
     * lock was not aquired, null is returned.
     */
    public String lock(String key, long duration)
    {
        return redisOps.lockRandom(makeKey(key), duration);
    }

    /**
     * IsLocked returns a boolean if the lock is currently held.
     */
    public boolean isLocked(String lock, String key)
    {
        return lock.equals(redisOps.get(makeKey(key)));
    }

    /**
     * Check of there is any lock on this key.
     */
    public boolean anyLock(String key)
    {
        String lock = redisOps.get(makeKey(key));
        return !(lock == null || lock == "");
    }

    /**
     * Refresh returns a boolean if the held lock is refreshed.
     */
    public boolean refresh(String lock, String key, long duration)
    {
        redisOps.eval(REFRESH_SCRIPT, Arrays.asList(new String[] {makeKey(key)}),
            Arrays.asList(new String[] {lock, Long.toString(duration)}));
        return isLocked(lock, key);
    }

    /**
     * Unlock will release a held lock.
     */
    public void unlock(String lock, String key)
    {
        redisOps.eval(UNLOCK_SCRIPT, Arrays.asList(new String[] {makeKey(key)}),
            Arrays.asList(new String[] {lock}));
        return;
    }

    private String makeKey(String key)
    {
        return this.prefix + key + ".lock";
    }

    // wait the certain amount of time if the lock is still been hold by other
    public void sleep(long sleepingTime) {
        try {
            Thread.sleep(sleepingTime);
        } catch (InterruptedException ix) {
        }
    }
}
