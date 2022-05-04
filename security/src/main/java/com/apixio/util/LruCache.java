package com.apixio.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple reusable LRU cache that supports max size and TTL.
 *
 * TTL is supported by occasionally checking for expired keys; this occasional
 * checking is done whenever a get() or put() is done, or the client can explicitly
 * cause a check via removeExpired().
 */
public class LruCache<K,V>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LruCache.class);

    private static final long CLEANUP_MIN_DELAY = 60 * 1000L;  // don't cleanup more frequently than once a minute

    /**
     * Keeps track of last access time in order to implement TTL
     */
    private static class CacheEntry<V>
    {
        long lastAccessMs;
        V    value;

        CacheEntry(V value)
        {
            this.value = value;
            touch();
        }

        void touch()
        {
            lastAccessMs = System.currentTimeMillis();
        }
    }

    /**
     * Fields
     */
    private LinkedHashMap<K, CacheEntry<V>> cache;
    private long                            cacheTtlMs;
    private long                            nextCleanup;

    /**
     * Create LRU cache with the given cache size and TTL.  If TTL is less than 0
     * then no TTL is enforced.
     */
    public LruCache(final int cacheSize, final int cacheTtlMs)
    {
        if (cacheSize <= 0)
            throw new IllegalArgumentException("Cache size must be > 0 but is " + cacheSize);

        this.nextCleanup = System.currentTimeMillis() + CLEANUP_MIN_DELAY;

        this.cache = new LinkedHashMap<K, CacheEntry<V>>(cacheSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> entry)
                {
                    return (size() > cacheSize);
                }
            };

        this.cacheTtlMs = cacheTtlMs;
    }

    /**
     * Looks up the given key and if it exists its value is returned.  If it exists then
     * its access time is updated, thereby resetting its TTL.
     */
    public V get(K key)
    {
        synchronized(cache)
        {
            CacheEntry<V> ce = cache.get(key);

            removeExpired();  // do it after get() to avoid a needless loss

            if (ce != null)
            {
                ce.touch();
                return ce.value;
            }

            return null;
        }
    }

    /**
     * Record a new decryption key.
     */
    public void put(K key, V value)
    {
        synchronized(cache)
        {
            removeExpired();
            cache.put(key, new CacheEntry<V>(value));
        }
    }

    /**
     * Remove entries that are past the TTL
     */
    public void removeExpired()
    {
        synchronized(cache)
        {
            if (cache.size() > 0)
            {
                long now   = System.currentTimeMillis();
                int  count = 0;

                if (now > nextCleanup)
                {
                    Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator();

                    nextCleanup = now + CLEANUP_MIN_DELAY;

                    while (it.hasNext())
                    {
                        CacheEntry<V> entry = it.next().getValue();
                        long          old   = now - (entry.lastAccessMs + cacheTtlMs);

                        if (old > 0)
                        {
                            count++;
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * test
    public static void main(String... args)
    {
        LruCache         cache  = new LruCache<Integer,String>(100, 177);
        java.util.Random random = new java.util.Random(0L);

        for (int i = 0; i < 100; i++)
            cache.put(Integer.valueOf(i), Integer.toString(-i));

        LOGGER.info("Cache size: "  + cache.cache.size());

        for (int i = 0; i < 100; i++)
        {
            sleep(Math.max(1L, random.nextLong() % 100));

            cache.get(Integer.valueOf(random.nextInt(100)));
        }

        // this will flush things out
        for (int i = 100; i < 200; i++)
            cache.put(Integer.valueOf(i), Integer.toString(-i));
    }

    private static void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException x)
        {
        }
    }
     */

}
