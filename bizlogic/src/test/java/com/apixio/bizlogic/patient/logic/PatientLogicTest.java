package com.apixio.bizlogic.patient.logic;

import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.restbase.config.ConfigSet;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.Thread.sleep;

/**
 * Created by dyee on 4/20/17.
 */
@Ignore("Integration")
public class PatientLogicTest
{
    PatientLogic patientLogic;

    private static final String cYamlFile = "/Users/dyee/config/configlocal.yaml";

    public ConfigSet config;
    public DaoServices daoServices;

    public PatientLogicTest() throws Exception
    {
        config = ConfigSet.fromYamlFile((new File(cYamlFile)));
        daoServices = DaoServicesSet.createDaoServices(config);

        // This is to prevent the bug when running test suites
        // The bug was introduced in 2.x series of Cassandra
        // Hopefully, solved in 3.x series of Cassandra
        daoServices.getScienceCqlCrud().setStatementCacheEnabled(false);

        patientLogic = new PatientLogic(daoServices);
        /*
        patientLogic.setDomainTranslator(new PatientLogicBase.AssemblyDomainTranslator()
        {
            @Override
            public String translate(String domain) throws IOException
            {
                return "assembly_23";
            }
        });
        */
    }


    @Test
    public void testPatientUUIDCreation() throws Exception
    {
        String pdsId = String.valueOf(RandomUtils.nextInt());
        String patientKey = "testPatientKey_" + System.currentTimeMillis();

        UUID uuid = patientLogic.reservePatientUUID(pdsId, patientKey, false);

        sleep(300);

        UUID uuid2 = patientLogic.reservePatientUUID(pdsId, patientKey, false);

        Assert.assertEquals(uuid, uuid2);

        sleep(300);

        UUID uuid3 = patientLogic.reservePatientUUID(pdsId, patientKey, false);

        Assert.assertEquals(uuid, uuid3);
    }

    @Test
    public void testConcurrentPatientUUIDCreation() throws Exception
    {
        final String pdsId = String.valueOf(RandomUtils.nextInt());
        final String patientKey = "testPatientKey_" + System.currentTimeMillis();

        List<Thread> threads = new ArrayList<Thread>();
        final Map<Integer, UUID> resultMap = new HashMap<>();

        for(int i = 0; i<20; i++)
        {
            final Integer count = i;
            threads.add(new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        UUID uuid = patientLogic.reservePatientUUID(pdsId, patientKey, false);

                        resultMap.put(count, uuid);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }));
        }

        for(Thread thread : threads) {
            thread.start();
        }

        for(Thread thread : threads) {
            thread.join();
        }

        final UUID testUUID = resultMap.values().iterator().next();

        for(UUID uuid : resultMap.values()) {
            Assert.assertEquals(testUUID, uuid);
        }

    }
}
