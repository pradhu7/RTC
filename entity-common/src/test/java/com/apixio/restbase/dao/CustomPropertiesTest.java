package com.apixio.restbase.dao;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.DataServices;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.PropertyUtil;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.entity.CustomProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertTrue;

public class CustomPropertiesTest {

    private final static int PROPS_KEY_VALUE_COUNT = 2;
    private final static String TEST_SCOPE_NAME = "TEST_SCOPE";
    private final static String PROPS_KEY_PREFIX = "test_key";

    //a little ugly here, the two constants should be exposed as constants in the Datanames class
    private final static String KEY_SCOPEMEMBERS = "custscope:";
    private final static String KEY_ENTITYPROPERTIES = "custprop:";
    private final static String K_DATAVERSIONS  = "common-dataversions";

    private final static int READ_THREAD_COUNT = 20;
    private final static int WRITE_THREAD_COUNT = 10;

    private AtomicBoolean exit = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    Thread.UncaughtExceptionHandler h = (th, ex) -> {
        ex.getStackTrace();
    };

    DaoBase daoBase;
    CustomProperties cpObj;
    PropertyUtil propUtil;
    RedisOps redisOps;
    String scopeKey;

    @Before
    public void setUp() throws IOException {
        File configFile = new File(CustomPropertiesTest.class.getClassLoader().getResource("config.yaml").getFile());
        ConfigSet configSet = ConfigSet.fromYamlFile(configFile);
        PersistenceServices ps =  ConfigUtil.createPersistenceServices(configSet, null);
        daoBase = new DaoBase(ps);
        DataServices ds = new DataServices(daoBase, configSet);
        redisOps = daoBase.getRedisOps();

        propUtil = new PropertyUtil(TEST_SCOPE_NAME, ds);
        cpObj = ds.getCustomProperties();
        scopeKey = cpObj.makeKey(KEY_SCOPEMEMBERS + TEST_SCOPE_NAME);

        cleanup();
    }

    @After
    public void tearDown() {
        try {
            Set<String> entityProps = redisOps.smembers(scopeKey);
            for (String key : entityProps) {
                String entityPropKey = cpObj.makeKey(KEY_ENTITYPROPERTIES + key);
                redisOps.del(entityPropKey);
            }
            cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        String allCPKey = cpObj.makeKey(Datanames.CUSTOM_PROPERTIES + "_all");
        List<CustomProperty> properties = cpObj.getAllCustomPropertiesByScope(TEST_SCOPE_NAME);
        for (CustomProperty cp : properties) {
            String key = cpObj.makeKey(cp.getID().getID());
            redisOps.del(key);
            redisOps.srem(allCPKey, cp.getID().getID());
        }
        redisOps.del(scopeKey);
        redisOps.hdel(cpObj.makeKey(K_DATAVERSIONS), Datanames.CUSTOM_SCOPE + TEST_SCOPE_NAME);
    }

    private void createScopeCache() {
        for (int i = 0; i < PROPS_KEY_VALUE_COUNT; ++i) {
            String propNameSuffix = UUID.randomUUID().toString();
            String propName = PROPS_KEY_PREFIX + "_" + propNameSuffix;
            XUUID uuid = XUUID.create("");
            propUtil.addPropertyDef(propName, PropertyType.STRING);
            propUtil.setEntityProperty(uuid, propName, "val");
        }
    }

    @Test
    public void testSingleThreadGetAllEntityProperties() {
        createScopeCache();
        CustomProperty cp = new CustomProperty(PROPS_KEY_PREFIX, PropertyType.STRING, TEST_SCOPE_NAME, "");
        Map<XUUID, Object> ret = cpObj.getAllEntityProperties(cp);
        assertTrue(ret.isEmpty());
    }

    @Test
    public void testBadMultithreadedGetAllEntityProperties() throws InterruptedException {
        CountDownLatch readlatch = new CountDownLatch(READ_THREAD_COUNT);
        Thread[] thrd = new Thread[READ_THREAD_COUNT];
        for (int i = 0; i < READ_THREAD_COUNT; ++i) {
            thrd[i] = new Thread(() -> {
                CustomProperty cp = new CustomProperty(PROPS_KEY_PREFIX, PropertyType.STRING, TEST_SCOPE_NAME, "");
                cpObj.getAllEntityProperties(cp);
                readlatch.countDown();
                System.out.println(String.format("after call getAllEntityProperties, read ThreadId: %d, read latch: %d",
                    Thread.currentThread().getId(), readlatch.getCount()));
            });
            thrd[i].setUncaughtExceptionHandler(h);
        }

        CountDownLatch writelatch = new CountDownLatch(WRITE_THREAD_COUNT);
        Thread[] produceThrd = new Thread[WRITE_THREAD_COUNT];
        for (int i = 0; i < WRITE_THREAD_COUNT; ++i) {
            produceThrd[i] = new Thread(() -> {
                createScopeCache();
                writelatch.countDown();
                System.out.println(String.format("after call createScopeCache, write ThreadId: %d, write latch: %d",
                    Thread.currentThread().getId(), writelatch.getCount()));
            });
            produceThrd[i].setUncaughtExceptionHandler(h);
        }
        Thread.currentThread().setUncaughtExceptionHandler(h);
        
        for (int i = 0; i < WRITE_THREAD_COUNT; ++i) {
            System.out.println("start write thrd: " + i);
            produceThrd[i].start();
        }

        for (int i = 0; i < READ_THREAD_COUNT; ++i) {
            System.out.println("start read thrd: " + i);
            thrd[i].start();
        }

        readlatch.await(20, TimeUnit.SECONDS);
        writelatch.await(20, TimeUnit.SECONDS);
        System.out.println(String.format("readLatch: %d, writelatch: %d", readlatch.getCount(), writelatch.getCount()));
    }

    @Test
    public void testGoodMultithreadedGetAllEntityProperties() throws InterruptedException {
        CountDownLatch readlatch = new CountDownLatch(READ_THREAD_COUNT);
        Thread[] thrd = new Thread[READ_THREAD_COUNT];
        for (int i = 0; i < READ_THREAD_COUNT; ++i) {
            thrd[i] = new Thread(() -> {
            CustomProperty cp = new CustomProperty(PROPS_KEY_PREFIX, PropertyType.STRING, TEST_SCOPE_NAME, "");
                lock.lock();
                try {
                    cpObj.getAllEntityProperties(cp);
                } finally {
                    lock.unlock();
                    readlatch.countDown();
                }
                System.out.println(String.format("after call getAllEntityProperties, read ThreadId: %d, isLocked: %b, Lock Hold Count: %d, read latch: %d",
                    Thread.currentThread().getId(), lock.isLocked(), lock.getHoldCount(), readlatch.getCount()));
            });
            thrd[i].setUncaughtExceptionHandler(h);
        }

        CountDownLatch writelatch = new CountDownLatch(WRITE_THREAD_COUNT);
        Thread[] produceThrd = new Thread[WRITE_THREAD_COUNT];
        for (int i = 0; i < WRITE_THREAD_COUNT; ++i) {
            produceThrd[i] = new Thread(() -> {
            lock.lock();
            try {
                createScopeCache();
            } finally {
                lock.unlock();
                writelatch.countDown();
            }
            System.out.println(String.format("after call createScopeCache, write ThreadId: %d, isLocked: %b, Lock Hold Count: %d, write latch: %d",
                Thread.currentThread().getId(), lock.isLocked(), lock.getHoldCount(), writelatch.getCount()));
            });
            produceThrd[i].setUncaughtExceptionHandler(h);
        }
        Thread.currentThread().setUncaughtExceptionHandler(h);

        for (int i = 0; i < WRITE_THREAD_COUNT; ++i) {
            System.out.println("start write thrd: " + i);
            produceThrd[i].start();
        }

        for (int i = 0; i < READ_THREAD_COUNT; ++i) {
            System.out.println("start read thrd: " + i);
            thrd[i].start();
        }

        readlatch.await(20, TimeUnit.SECONDS);
        writelatch.await(20, TimeUnit.SECONDS);
        System.out.println(String.format("readLatch: %d, writelatch: %d", readlatch.getCount(), writelatch.getCount()));
    }
}
