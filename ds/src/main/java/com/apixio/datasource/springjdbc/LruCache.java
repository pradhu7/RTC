package com.apixio.datasource.springjdbc;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU Cache for java.lang.AutoCloseable objects.
 */
public class LruCache<K, V extends AutoCloseable>
{
    /**
     * The size-limited cache
     */
    private LinkedHashMap<K,V> cache;

    /**
     * Create a new cache with the given max # of entries.  When a new item is put to
     * the cache, if that would push the size beyond the max, then the oldest entry
     * (ordered by access via get()) is removed and closed.
     */
    public LruCache(final int maxEntries)
    {
        if (maxEntries <= 0)
            throw new IllegalArgumentException("LruCache size must be > 0");

        cache = new LinkedHashMap<K,V>(maxEntries, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K,V> entry)
                {
                    if (size() > maxEntries)
                    {
                        closeEntry(entry);

                        return true;
                    }

                    return false;
                }
            };
    }

    /**
     * Put in the cache.  A possible side-effect is that an existing entry is is removed and closed.
     */
    public synchronized void put(K key, V value)
    {
        cache.put(key, value);
    }

    /**
     * Get from the cache.
     */
    public synchronized V get(K key)
    {
        return cache.get(key);
    }
    
    /**
     * Closes and removes all items from the cache
     */
    public synchronized void cleanCache()
    {
        for (Map.Entry<K,V> entry : cache.entrySet())
            closeEntry(entry);

        cache.clear();
    }

    /**
     * closes the entry, translating an Exception into a RuntimeException
     */
    private void closeEntry(Map.Entry<K,V> entry)
    {
        try
        {
            entry.getValue().close();
        }
        catch (Exception x)
        {
            throw new RuntimeException("Unable to close eldest entry in LRU cache", x);
        }
    }

    /**
     * test only
     */
    public static void main(String... args)
    {
        // make 15 keys, loop for 100 times and do a get on one of the 15 at random, ...

        LruCache<String, TestCloseable> cache = new LruCache<>(5);
        String[]                        keys  = new String[15];
        java.util.Random                rand  = new java.util.Random(0L);  // fixed seed for reproducibility
        
        for (int i = 0; i < keys.length; i++)
            keys[i] = "random key is " + rand.nextInt(1000);

        for (int i = 0; i < 100; i++)
        {
            int           keyIdx = rand.nextInt(15);
            String        key    = keys[keyIdx];
            TestCloseable item;

            System.out.println("Attempting to get from cache: k=" + key);

            item = cache.get(key);

            if (item == null)
            {
                String itemID = "closeable for key " + key;

                System.out.println("  Key " + key + " was not in cache--creating value " + itemID + " and putting into cache");
                item = new TestCloseable(itemID);

                cache.put(key, item);
            }
            else
            {
                System.out.println("  Got item with ID " + item.id + " for key " + key);
            }
        }

        cache.cleanCache();
    }

    private final static class TestCloseable implements AutoCloseable
    {
        String id;

        TestCloseable(String id)
        {
            this.id = id;
        }

        public void close() throws IOException
        {
            System.out.println("Closing TestCloseable id=" + id);
        }
    }

}
