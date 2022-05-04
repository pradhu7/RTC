package com.apixio.restbase.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.restbase.DaoBase;

/**
 * DataVersions provides companion support to a caching paradigm that requires that
 * the clients check if their in-JVM copy is stale.  It does this by maintaining a
 * map from data "name" (which is client-defined) and its current version number as
 * kept in redis.
 *
 * The save of any cacheable data within a data "name" (call it datatype) must bump
 * up the version number in redis by calling the method incrDataVersion.  Every
 * client that caches data within a datatype must call getDataVersion() to see
 * if its in-JVM version is the same as what's in redis, and if not, it must discard
 * its in-JVM copies and reload (and associate the current data version number with
 * those data).
 *
 * The implementation of this mechanism is via redis' "hincrby" command that increments
 * atomically a single field of a HASH.  In this case, the hashkeys are the datatype
 * names.
 *
 * IMPORTANT:  as the data version numbers are kept in redis, the names of the data types
 * should NOT ever change or all JVMs will incorrectly think they have the latest version.
 *  
 */
public final class DataVersions extends DaoBase {

    /**
     * Constants for redis key names
     */
    private final static String K_DATAVERSIONS  = "common-dataversions";

    /**
     * The final redis key for the HASH of datatype names.
     */
    private String keyDataversions;

    /**
     * This funky constructor form allows the caller to initialize a new
     * DAO with an existing DAO.
     */
    public DataVersions(DaoBase seed)
    {
        super(seed);

        keyDataversions = makeKey(K_DATAVERSIONS);
    }

    /**
     * Get the current data version number from redis.
     */
    public int getDataVersion(String typename)
    {
        try
        {
            return Integer.parseInt(redisOps.hget(keyDataversions, typename));
        }
        catch (Exception x)
        {
            return -1;
        }
    }

    /**
     * Get the current data version numbers from redis for all the given typenames.
     * The keys of the returned Map are the type names and the values are the
     * current counts.  Only one request to redis is made.  If there is no dataversion
     * with the given name, that map key won't exist.
     *
     * Null is returned if there is any error.
     */
    public Map<String, Integer> getDataVersions(String... types)
    {
        try
        {
            Map<String, Integer> typeCounts = new HashMap<String, Integer>();
            List<String>         counts     = redisOps.hmget(keyDataversions, types);

            if (types.length != typeCounts.size())
                throw new IllegalStateException("The size of returned counts list from redis doesn't match number sent in.");

            for (int i = 0; i < types.length; i++)
            {
                String count = counts.get(i);

                if (count != null)
                    typeCounts.put(types[i], Integer.parseInt(count));
            }

            return typeCounts;
        }
        catch (Exception x)
        {
            return null;
        }
    }

    /**
     * Increment the data version number in redis.
     */
    public void incrDataVersion(String typename)
    {
        redisOps.hincrBy(keyDataversions, typename, +1);
    }

}
