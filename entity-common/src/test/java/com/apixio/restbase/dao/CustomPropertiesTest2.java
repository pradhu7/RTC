package com.apixio.restbase.dao;

import com.apixio.XUUID;
import com.apixio.restbase.*;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class CustomPropertiesTest2 {

    private final int THREAD_COUNT = 50;
    private final int WRITE_THREAD_COUNT = 20;

    private AtomicBoolean exit = new AtomicBoolean(false);

    Thread.UncaughtExceptionHandler h = (th, ex) -> {
        ex.printStackTrace();
    };

    PropertyUtil propUtil;
    XUUID[] theObjects = new XUUID[50];
    private Random random = new Random();

    private final static String PROP_NAME1 = "cptest_junk";
    private final static String PROP_NAME2 = "cptest_junk2";

    @Before
    public void setUp() throws IOException {
        File configFile = new File(CustomPropertiesTest2.class.getClassLoader().getResource("config.yaml").getFile());
        ConfigSet configSet = ConfigSet.fromYamlFile(configFile);
        PersistenceServices ps =  ConfigUtil.createPersistenceServices(configSet, null);
        DaoBase daoBase   = new DaoBase(ps);

        propUtil = new PropertyUtil("scott", new DataServices(daoBase, configSet));
        try
        {
            // note that property defs will be kept in redis server so we need to delete first (which
            // doesn't cause a problem if the def isn't there)
            propUtil.removePropertyDef(PROP_NAME1);
            propUtil.addPropertyDef(PROP_NAME1, PropertyType.STRING);

            propUtil.removePropertyDef(PROP_NAME2);
            propUtil.addPropertyDef(PROP_NAME2, PropertyType.STRING);

            // these XUUIDs will accumulate, one for each time the test is run
            for (int i = 0; i < theObjects.length; i++)
                theObjects[i] = XUUID.create("cptest");

            setPropValue("hi");
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    private void setPropValue(String val)
    {
        XUUID theObj = theObjects[random.nextInt(theObjects.length)];

        if (random.nextInt(10) < 5)
            propUtil.setEntityProperty(theObj, PROP_NAME1, val);
        else
            propUtil.setEntityProperty(theObj, PROP_NAME2, val);
    }

    @Test
    public void testSingleThreadGetAllEntityProperties() {
        Map<XUUID, Object> ret = propUtil.getCustomProperty(PROP_NAME1);
        System.out.println("SFM junk1 props: " + ret);
    }

    @Test
    public void testBadMultithreadedGetAllEntityProperties() throws InterruptedException {
        Thread[] thrd = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; ++i) {
            thrd[i] = new Thread(() -> {

                int count = 0;
                while (!exit.get()) {
                        count++;
                        try {
                            propUtil.getCustomProperty(PROP_NAME1);
                        } catch (final Throwable t) {
                            throw new RuntimeException(t);
                        }
                }
                System.out.println(String.format("ThreadId: %d, count: %d", Thread.currentThread().getId(), count));
            });
            thrd[i].setUncaughtExceptionHandler(h);
        }

        Thread[] produceThrd = new Thread[WRITE_THREAD_COUNT];
        for (int i = 0; i < WRITE_THREAD_COUNT; ++i) {
            produceThrd[i] = new Thread(() -> {
                int count = 0;
                while (!exit.get()) {
                    count++;
                    try {
                        setPropValue("curCount:" + count);
                    } catch (final Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
                System.out.println(String.format("Producer ThreadId: %d, count: %d", Thread.currentThread().getId(), count));
            });
            produceThrd[i].setUncaughtExceptionHandler(h);
        }
        Thread.currentThread().setUncaughtExceptionHandler(h);
        
        for (int i = 0; i < WRITE_THREAD_COUNT; ++i) {
            produceThrd[i].start();
        }

        for (int i = 0; i < THREAD_COUNT; ++i) {
            thrd[i].start();
        }

        sleep(60000);

        boolean prevStatus = exit.getAndSet(true);
        //System.out.println("prev exit status: " + prevStatus);

        for (int i = 0; i < WRITE_THREAD_COUNT; ++i) {
            produceThrd[i].join();
        }
        for (int i = 0; i < THREAD_COUNT; ++i) {
            thrd[i].join();
        }
    }
}
