package com.apixio.utility;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;

@Ignore("Integration")
public class PropertyHelperTest
{
    private Transactions   redisTransactions;
    private RedisOps       redisOps;
    private String         prefix;

    private PropertyHelper propertyHelper = new PropertyHelper();

    @Before
    public void setUp() throws Exception
    {
        prefix = "dev_";
        redisOps = new RedisOps("localhost", 6379);
        redisTransactions = redisOps.getTransactions();

        propertyHelper.setPrefix(prefix);
        propertyHelper.setRedisOps(redisOps);
        propertyHelper.setTransactions(redisTransactions);
    }

    @Test
    public void testSaveAndLoad()
    {
        String configName = "c1" + Math.random();
        System.out.println(configName);

        /// first saved properties
        Properties properties1 = new Properties();

        properties1.put("p1", "v1");
        properties1.put("p2", "v2");

        String versionConfigName1 = propertyHelper.saveProperties(configName, properties1);
        assertNotNull(versionConfigName1);
        System.out.println(versionConfigName1);

        Properties properties2 = propertyHelper.loadProperties(configName);
        assertNotNull(properties2);
        assertEquals(properties2.size(), 2 + 1);

        String version1 = properties2.getProperty("$propertyVersion");
        assertNotNull(version1);
        System.out.println(version1);

        sleep(500L);

        /// second saved properties
        Properties properties3 = new Properties();

        properties3.put("p1", "v1");
        properties3.put("p2", "v2");
        properties3.put("p3", "v3");

        String versionConfigName2 = propertyHelper.saveProperties(configName, properties3);
        assertNotNull(versionConfigName2);
        System.out.println(versionConfigName2);

        Properties properties4 = propertyHelper.loadProperties(configName);
        assertNotNull(properties4);
        assertEquals(properties4.size(), 3 + 1);

        String version2 = properties4.getProperty("$propertyVersion");
        assertNotNull(version2);
        System.out.println(version2);

        assertTrue(Long.valueOf(version1) < Long.valueOf(version2));
        assertNotSame(versionConfigName1, versionConfigName2);

        List<String> names = propertyHelper.getAllConfigNames(configName);
        assertNotNull(names);
        assertEquals(names.size(), 2);
        for (String name : names)
        {
            System.out.println(name);
        }
    }

    @Test
    public void testUpdateAndLoad()
    {
        String configName = "c1" + Math.random();
        System.out.println(configName);

        /// saved properties
        Properties properties1 = new Properties();

        properties1.put("p1", "v1");
        properties1.put("p2", "v2");

        String versionConfigName1 = propertyHelper.saveProperties(configName, properties1);
        assertNotNull(versionConfigName1);
        System.out.println(versionConfigName1);

        Properties properties2 = propertyHelper.loadProperties(configName);
        assertNotNull(properties2);
        assertEquals(properties2.size(), 2 + 1);

        String version1 = properties2.getProperty("$propertyVersion");
        assertNotNull(version1);
        System.out.println(version1);

        sleep(500L);

        /// second one  is an overwrite
        Properties properties3 = new Properties();

        properties3.put("p1", "v1_new");
        properties3.put("p3", "v3");

        String versionConfigName2 = propertyHelper.updateProperties(configName, properties3);
        assertNotNull(versionConfigName2);
        System.out.println(versionConfigName2);

        Properties properties4 = propertyHelper.loadProperties(configName);
        assertNotNull(properties4);
        assertEquals(properties4.size(), 3 + 1);

        String version2 = properties4.getProperty("$propertyVersion");
        assertNotNull(version2);
        System.out.println(version2);

        String p1 = properties4.getProperty("p1");
        assertNotNull(p1);
        assertEquals(p1, "v1_new");

        String p2 = properties4.getProperty("p2");
        assertNotNull(p2);
        assertEquals(p2, "v2");

        String p3 = properties4.getProperty("p3");
        assertNotNull(p3);
        assertEquals(p3, "v3");

        assertTrue(Long.valueOf(version1) < Long.valueOf(version2));
        assertNotSame(versionConfigName1, versionConfigName2);

        List<String> names = propertyHelper.getAllConfigNames(configName);
        assertNotNull(names);
        assertEquals(names.size(), 2);
        for (String name : names)
        {
            System.out.println(name);
        }
    }

    @Test
    public void testVersioning()
    {
        String configName = "c1" + Math.random();
        System.out.println(configName);

        /// first saved properties
        Properties properties1 = new Properties();

        properties1.put("p1", "v1");
        properties1.put("p2", "v2");

        String versionConfigName1 = propertyHelper.saveProperties(configName, properties1);
        assertNotNull(versionConfigName1);
        System.out.println(versionConfigName1);

        Properties properties2 = propertyHelper.loadProperties(configName);
        assertNotNull(properties2);
        assertEquals(properties2.size(), 2 + 1);

        String version1 = properties2.getProperty("$propertyVersion");
        assertNotNull(version1);
        System.out.println(version1);

        sleep(500L);

        /// second saved properties
        Properties properties3 = new Properties();

        properties3.put("p1", "v1");
        properties3.put("p2", "v2");
        properties3.put("p3", "v3");

        String versionConfigName2 = propertyHelper.saveProperties(configName, properties3);
        assertNotNull(versionConfigName2);
        System.out.println(versionConfigName2);

        Properties properties4 = propertyHelper.loadProperties(configName);
        assertNotNull(properties4);
        assertEquals(properties4.size(), 3 + 1);

        String version2 = properties4.getProperty("$propertyVersion");
        assertNotNull(version2);
        System.out.println(version2);

        assertTrue(Long.valueOf(version1) < Long.valueOf(version2));
        assertNotSame(versionConfigName1, versionConfigName2);

        // let's test versioning

        Properties pv1 = propertyHelper.loadProperties(configName, Long.valueOf(version1) - 1);
        assertNull(pv1);

        Properties pv2 = propertyHelper.loadProperties(configName, Long.valueOf(version1));
        assertNotNull(pv2);
        assertEquals(pv2.size(), 2 + 1);

        Properties pv3 = propertyHelper.loadProperties(configName, Long.valueOf(version1) + 1);
        assertNotNull(pv3);
        assertEquals(pv3.size(), 2 + 1);

        Properties pv4 = propertyHelper.loadProperties(configName, Long.valueOf(version2) - 1);
        assertNotNull(pv4);
        assertEquals(pv4.size(), 2 + 1);

        Properties pv5 = propertyHelper.loadProperties(configName, Long.valueOf(version2));
        assertNotNull(pv5);
        assertEquals(pv5.size(), 3 + 1);

        Properties pv6 = propertyHelper.loadProperties(configName, Long.valueOf(version2) + 1000);
        assertNotNull(pv6);
        assertEquals(pv6.size(), 3 + 1);

        List<String> names = propertyHelper.getAllConfigNames(configName);
        assertNotNull(names);
        assertEquals(names.size(), 2);
        for (String name : names)
        {
            System.out.println(name);
        }
    }

    @Test
    public void testError()
    {
        String d = String.format("%tFT%<tRZ", new java.util.Date());
        System.out.println(d);

        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("UTC");
        java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String nowAsString = df.format(new java.util.Date());
        System.out.println(nowAsString);

        String configName = "c1" + Math.random();
        System.out.println(configName);

        Properties properties1 = new Properties();
        String versionConfigName1 = propertyHelper.saveProperties(configName, properties1);
        assertNull(versionConfigName1);
    }

    private final static void sleep(Long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ix)
        {
        }
    }
}
