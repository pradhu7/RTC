package com.apixio.restbase.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TimeoutCache provides a cache that expires keys in a fixed amount of time
 * (usually on the order of a few seconds).  Periodic cleanup is done to make
 * sure the cache doesn't grow without bounds.
 */
public class TimeoutCache<K,V> {

    private final static long REAPER_SLEEP = 5000L;

    private long timeoutMs;  // max amount of milliseconds a cache entry is valid

    private ConcurrentMap<K, Entry<V>> cache;
    private Reaper                     reaperThread;

    public TimeoutCache(long ms)
    {
        cache     = new ConcurrentHashMap<K, Entry<V>>();
        timeoutMs = ms;
    }

    /**
     * Put the K=V entry into the cache.  If the entry already exists, reset the
     * time when added so it will be kept as though it were newly added.
     */
    public void put(K key, V value)
    {
        Entry<V>  v   = cache.get(key);
        long      now = System.currentTimeMillis();

        if (v != null)
            v.addedMs = now;
        else
            cache.putIfAbsent(key, new Entry(now, value));  // use putIfAbsent in case another thread added key

        ensureReaper();
    }

    /**
     * Looks up the key in the cache, returning any existing value.  If the value has
     * expired, the key is removed and null is returned.
     */
    public V get(K key)
    {
        Entry<V>  v   = cache.get(key);
        V         ret = null;

        if (v != null)
        {
            if (v.addedMs + timeoutMs < System.currentTimeMillis())
                cache.remove(key);
            else
                ret = v.value;
        }

        return ret;
    }

    /**
     * Looks up the key in the cache, returning any existing value.  If the value has
     * expired, the key is removed and null is returned.
     */
    public void remove(K key)
    {
        cache.remove(key);
    }

    /**
     * Start up a reaper thread if there isn't one already
     */
    private void ensureReaper()
    {
        if (reaperThread == null)
            (reaperThread = new Reaper()).start();
    }

    /**
     * Wraps the actual value to include the time added.
     */
    private class Entry<V> {
        long addedMs;   // epoch ms when entry was added
        V    value;

        Entry(long when, V value)
        {
            this.addedMs = when;
            this.value   = value;
        }
    }

    /**
     * Reaper thread to keep things clean.  It's a self-terminating thread so
     * that even if multiple ones are started up all but the active one will
     * automatically terminate.
     */
    private class Reaper extends Thread {
        @Override
        public void run()
        {
            while (true)
            {
                boolean keepAlive = false;

                try
                {
                    Iterator<Map.Entry<K,Entry<V>>> entries = cache.entrySet().iterator();

                    while (entries.hasNext())
                    {
                        if (get(entries.next().getKey()) != null)
                            keepAlive = true;
                    }
                }
                catch (Exception x)
                {
                }

                if (keepAlive)
                {
                    try
                    {
                        Thread.sleep(REAPER_SLEEP);
                    }
                    catch (InterruptedException x)
                    {
                    }
                }
                else
                {
                    reaperThread = null;
                    break;
                }
            }
        }
    }

    /*
    // testing
    private static long baseMs;
    public static void main(String[] args)
    {
        TimeoutCache tc = new TimeoutCache<String, Boolean>(10000L);

        baseMs = System.currentTimeMillis();
        System.out.println("Putting 'scott' in cache at " + time(baseMs));
        tc.put("scott", true);

        int count = 0;
        

        while (tc.get("scott") != null)
        {
            System.out.println("'scott' still exists at " + time(System.currentTimeMillis()));

            if (count++ == 5)
            {
                System.out.println(" touched 'scott' at " + time(System.currentTimeMillis()));
                tc.put("scott", true);
            }

            sleep(1000L);
        }

        System.out.println("'scott' no longer exists at " + time(System.currentTimeMillis()));
    }

    private static long time(long theTime)
    {
        return theTime - baseMs;
    }

    private static void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ix)
        {
        }
    }
    */

}
