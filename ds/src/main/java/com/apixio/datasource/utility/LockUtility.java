package com.apixio.datasource.utility;

import com.apixio.datasource.redis.DistLock;
import com.apixio.datasource.redis.RedisOps;

public class LockUtility
{
    private final static long  baseSleep        = 10000L;  //10 seconds
    private final static long  maxSleep         = 1000L * 60L * 5L; // 5 minutes
    private final static long  lockLeasePeriod  = 10000L;
    private static final int[] FIBONACCI        = new int[]{0,2, 3, 5, 8, 13, 21, 34};

    private DistLock distLock;

    public LockUtility(RedisOps redisOps, String name)
    {
        this.distLock = new DistLock(redisOps, name);
    }

    public String getLock(String key, boolean keepTryingAgain)
    {
        return getLock(key, lockLeasePeriod, keepTryingAgain);
    }

    public String getLock(String key, long period, boolean keepTryingAgain)
    {
        String lock     = distLock.lock(key, period);
        int    attempts = 0;

        while (keepTryingAgain && lock == null)
        {
            sleep(++attempts, -1);
            lock = distLock.lock(key, period);
        }

        return lock;
    }

    public String getLock(String key, long period, int maxAttempts)
    {
        String lock     = distLock.lock(key, period);
        int    attempts = 0;

        while (attempts < maxAttempts && lock == null)
        {
            sleep(++attempts, -1);
            lock = distLock.lock(key, period);
        }

        return lock;
    }

    public boolean isLocked(String key, String lock)
    {
        return distLock.isLocked(lock, key);
    }

    public boolean anyLock(String key)
    {
        return distLock.anyLock(key);
    }

    public boolean refreshLock(String key, String lock, int maxAttempts)
    {
        return refreshLock(key, lock, lockLeasePeriod, maxAttempts);
    }

    public boolean refreshLock(String key, String lock, long period, int maxAttempts)
    {
        boolean status   = distLock.refresh(lock, key, period);
        int     attempts = 0;

        while (attempts < maxAttempts && !status)
        {
            sleep(++attempts, -1);
            status = distLock.refresh(lock, key, period);
        }

        return status;
    }

    public boolean refreshLock(String key, String lock, boolean keepTryingAgain)
    {
        return refreshLock(key, lock, lockLeasePeriod, keepTryingAgain);
    }

    public boolean refreshLock(String key, String lock, long period,  boolean keepTryingAgain)
    {
        boolean status   = distLock.refresh(lock, key, period);
        int     attempts = 0;

        while (keepTryingAgain && !status)
        {
            sleep(++attempts, -1);
            status = distLock.refresh(lock, key, period);
        }

        return status;
    }

    public String getLockWithTimeout(String key, long period, long timeout) throws Exception
    {
        String lock     = distLock.lock(key, period);
        int    attempts = 0;
        Long   start    = System.currentTimeMillis();

        while (System.currentTimeMillis() - start <= timeout && lock == null)
        {
            sleep(++attempts, System.currentTimeMillis() - start);
            lock = distLock.lock(key, period);
        }

        if (lock == null && System.currentTimeMillis() - start > timeout) {
            throw new Exception(String.format("Lock wait timeout of %s milliseconds exceeded", timeout));
        }

        return lock;
    }

    public void unlock(String key, String lock)
    {
        distLock.unlock(lock, key);
    }

    private final static void sleep(int attempt, long timeRemaining) {
        try
        {
            Thread.sleep(getSleepDuration(attempt, timeRemaining));

        } catch (InterruptedException ix) { }
    }

    private static final long getSleepDuration(int index, long timeRemaining)
    {
        index = index >= FIBONACCI.length ? FIBONACCI.length - 1 : index;
        long fibSleepMs = FIBONACCI[index] * baseSleep;
        return timeRemaining == -1 ? Math.min(fibSleepMs, maxSleep) : Math.min(Math.min(fibSleepMs, maxSleep), timeRemaining);
    }
}
