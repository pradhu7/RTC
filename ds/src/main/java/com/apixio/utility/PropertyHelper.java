package com.apixio.utility;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import redis.clients.jedis.Tuple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;

/**
 * The propertyHelper saves and loads java properties from Redis. The properties are saved in Redis as a Redis hashes.
 *
 * Note:
 * - All operations are performed as transactions.
 * - All the property values are of type String. It is the responsibility of the clients of this class
 *   to interpret the String values as String, boolean, int, etc.
 * - Properties are versioned. A new version is created each time a property is saved or updated.
 * - You can get the latest property file or any version.
 */

public class PropertyHelper
{
    private static Logger logger = LoggerFactory.getLogger(PropertyHelper.class);

    private final static String VERSION_SEPARATOR = "_";

    /**
     * Note that if redisTransactions is null, then it assumes a transaction has been started by the client.
     */
    private Transactions redisTransactions;

    private RedisOps     redisOps;
    private String       prefix;
    private int          prefixLen;

    public void setTransactions(Transactions trans)
    {
        redisTransactions = trans;
    }

    public void setRedisOps(RedisOps ops)
    {
        this.redisOps = ops;
    }

    public void setPrefix(String prefix)
    {
        this.prefix    = prefix;
        this.prefixLen = (prefix != null) ? prefix.length() : 0;
    }

    /**
     * Saves java properties to a Redis under the name configName.
     *
     * Note: A new version of properties is stored in Redis. This is performed in a transaction.
     *
     * @param configName
     * @param properties
     * @param version
     * @return versioned config name or null
     */
    public String saveProperties(String configName, Properties properties, long version)
    {
        return savePropertiesGuts(configName, properties, version);
    }

    /**
     * Saves java properties to a Redis under the name configName.
     *
     * Note: A new version of properties is stored in Redis. This is performed in a transaction.
     *
     * @param configName
     * @param properties
     * @return versioned config name or null
     */
    public String saveProperties(String configName, Properties properties)
    {
        return savePropertiesGuts(configName, properties, 0);
    }

    private String savePropertiesGuts(String configName, Properties properties, long timeInMillis)
    {
        if (timeInMillis == 0L)
            timeInMillis = System.currentTimeMillis();

        String canonicalConfigName = makeConfigNameCanonical(configName);
        String versionedConfigName = makeVersionedConfigName(canonicalConfigName, timeInMillis);

        Enumeration enums = properties.propertyNames();

        Map<String, String> redisPropertyMap = new HashMap<String, String>();

        while (enums.hasMoreElements())
        {
            String propertyName  = (String) enums.nextElement();
            String propertyValue = properties.getProperty(propertyName);

            redisPropertyMap.put(propertyName, propertyValue);
        }

        if (redisPropertyMap.isEmpty())
            return null;

        addUpdateTime(redisPropertyMap, timeInMillis);

        try
        {
            if (redisTransactions != null)
                redisTransactions.begin();

            redisOps.zadd(canonicalConfigName.getBytes(), timeInMillis, versionedConfigName.getBytes());
            redisOps.hmset(versionedConfigName, redisPropertyMap);

            if (redisTransactions != null)
                redisTransactions.commit();
        }
        catch (Exception x)
        {
            if (redisTransactions != null)
                redisTransactions.abort();

            logger.error("Failure in saving property {} ", configName, x);
        }

        return versionedConfigName;
    }

    /**
     * Update java properties to a Redis under the name configName.
     *
     * Note: A new version of properties is stored in Redis after merging it with the latest version.
     * This is performed in a transaction.
     *
     * @param configName
     * @param properties
     * @param version
     * @return versioned config name or null
     */
    public String updateProperties(String configName, Properties properties, long version)
    {
        return updatePropertiesGuts(configName, properties, version);
    }

    /**
     * Update java properties to a Redis under the name configName.
     *
     * Note: A new version of properties is stored in Redis after merging it with the latest version.
     * This is performed in a transaction.
     *
     * @param configName
     * @param properties
     * @return versioned config name or null
     */
    public String updateProperties(String configName, Properties properties)
    {
        return updatePropertiesGuts(configName, properties, 0);
    }

    private String updatePropertiesGuts(String configName, Properties properties, long version)
    {
        Properties existingProperties = loadProperties(configName);
        Properties mergedProperties   = new Properties();

        if (existingProperties != null)
            mergedProperties.putAll(existingProperties);

        mergedProperties.putAll(properties);

        return saveProperties(configName, mergedProperties, version);
    }

    /**
     * Delete java properties from Redis saved under the name configName.
     *
     * - It removes the versioned config name
     * - It deletes the versioned name from the global space
     *
     * @param configName
     */
    public void deleteProperties(String configName, long before)
    {
        String canonicalConfigName = makeConfigNameCanonical(configName);
        String versionedConfigName = findConfig(canonicalConfigName, before);

        if (versionedConfigName == null)
            return;

        try
        {
            if (redisTransactions != null)
                redisTransactions.begin();

            redisOps.zrem(canonicalConfigName.getBytes(), versionedConfigName.getBytes());
            redisOps.del(versionedConfigName);

            if (redisTransactions != null)
                redisTransactions.commit();
        }
        catch (Exception x)
        {
            if (redisTransactions != null)
                redisTransactions.abort();

            logger.error("Failure in deleting property {} ", configName, x);
        }
    }

    /**
     * Delete the latest java properties from Redis saved under the name configName.
     *
     * - It removes the versioned config name
     * - It deletes the versioned name from the global space
     *
     * @param configName
     */
    public void deleteProperties(String configName)
    {
        deleteProperties(configName, Long.MAX_VALUE);
    }

    /**
     * load java properties from Redis with name "configName" and version before the time "before".
     * If it doesn't exist, return null.
     *
     * @param configName
     * @return java properties or null.
     *
     */
    public Properties loadProperties(String configName, long before)
    {
        String canonicalConfigName = makeConfigNameCanonical(configName);
        String versionedConfigName = findConfig(canonicalConfigName, before);

        return getProperties(versionedConfigName);
    }

    /**
     * load latest version of java properties from Redis with name "configName".
     * If it doesn't exist, return null.
     *
     * @param configName
     * @return java properties or null.
     *
     */
    public Properties loadProperties(String configName)
    {
        String canonicalConfigName = makeConfigNameCanonical(configName);
        String latestConfigName    = getLatestConfig(canonicalConfigName);

        return getProperties(latestConfigName);
    }

    /**
     * Get the names of all config files (including the version).
     *
     * @param configName
     * @return The names of all config files (including the version) or empty list.
     *
     */
    public List<String> getAllConfigNames(String configName)
    {
        String canonicalConfigName = makeConfigNameCanonical(configName);
        Set<Tuple> tuples = redisOps.zrangeByScoreWithScores(canonicalConfigName.getBytes(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, -1);

        List<String> names = new ArrayList<String>();

        if (tuples == null || tuples.isEmpty())
            return names;

        for (Tuple tuple : tuples)
        {
            names.add(tuple.getElement());
        }

        return names;
    }

    /**
     * Get the original, but canonicalized, names of all configNames (including the version).
     * This is identical to getAllConfigNames but strips off matching prefix strings.
     *
     * @param configName
     * @return The names of all config files (including the version) or empty list.
     *
     */
    public List<String> getAllCanonicalConfigNames(String configName)
    {
        List<String> names = getAllConfigNames(configName);

        for (int idx = 0, max = names.size(); idx < max; idx++)
        {
            String name = names.get(idx);

            if (name.startsWith(prefix))
                names.set(idx, name.substring(prefixLen));
        }

        return names;
    }

    /**
     * Returns the version part (as a Long) of the configName.  Needed because the
     * strings returned from getAllConfigNames are a compound name that contains
     * more than the version of the config.
     *
     * @param configName
     * @return The Long version of the configName, null if there isn't a version.
     *
     */
    public Long getVersionFromConfigName(String configName)
    {
        int  under = configName.indexOf(VERSION_SEPARATOR);

        try
        {
            if (under != -1)
                return Long.valueOf(configName.substring(under + 1));
        }
        catch (Exception x)
        {
        }

        return null;
    }

    public String getNonVersionFromConfigName(String configName)
    {
        int  under = configName.indexOf(VERSION_SEPARATOR);

        if (under != -1)
            return configName.substring(0, under);
        else  // this should never happen...
            return configName;
    }

    private Properties getProperties(String configName)
    {
        if (configName == null)
            return null;

        Map<String, String> redisPropertyMap = redisOps.hgetAll(configName);
        if (redisPropertyMap == null)
            return null;

        Properties properties = new Properties();

        for (Map.Entry<String, String> element : redisPropertyMap.entrySet())
        {
            String propertyName  = element.getKey();
            String propertyValue = element.getValue();

            properties.setProperty(propertyName, propertyValue);
        }

        return properties;
    }

    private String findConfig(String configName, long before)
    {
        Set<Tuple> tuples = redisOps.zrangeByScoreWithScores(configName.getBytes(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, -1);

        if (tuples == null || tuples.isEmpty())
            return null;

        String config = null;

        for (Tuple tuple : tuples)
        {
            String element = tuple.getElement();
            double score   = tuple.getScore();

            if (score <= before)
                config = element;
            else
                break;
        }

        return config;
    }

    private String getLatestConfig(String configName)
    {
        return findConfig(configName, Long.MAX_VALUE);
    }

    private String makeConfigNameCanonical(String configName)
    {
        configName = configName.toLowerCase().trim();

        return (prefix + configName);
    }

    private static String makeVersionedConfigName(String configName, long version)
    {
        return configName + VERSION_SEPARATOR + version;
    }

    private static void addUpdateTime(Map map, long timeInMillis)
    {
        map.put("$propertyVersion", String.valueOf(timeInMillis));
    }
}
