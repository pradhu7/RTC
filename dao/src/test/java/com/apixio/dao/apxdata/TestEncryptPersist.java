package com.apixio.dao.apxdata;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.apxdata.vkc.VendorKnownCodesContainer;
import com.apixio.apxdata.vkc.VendorKnownCodesContainer.VendorKnownCode;


public class TestEncryptPersist
{
    private ApxDataDao    apxDataDao;

    private TestEncryptPersist(String yaml) throws Exception
    {
        ConfigSet   root    = ConfigSet.fromYamlFile(new File(yaml));
        DaoServices daos    = DaoServicesSet.createDaoServices(root);

        apxDataDao = new ApxDataDao(daos);

        apxDataDao.registerDataType(
            (new DataType(VendorKnownCode.class,
                          "VendorKnownCode",       // ostensibly for logging/debugging but is unused currently
                          "vendorKnownCode",       // short & never-changing, for Cassandra rowkey conflict avoidance; must be unique across all registered
                          0)).setCanContainPHI(true)
        );
    }

    private void testPersist() throws Exception
    {
        List<VendorKnownCode> testData = makeVkcData();

        System.out.println("___ testPersist testData=" + testData.size());

        apxDataDao.deleteData(VendorKnownCode.class, "mygroupingid");
        //        apxDataDao.putData(VendorKnownCode.class, "mygroupingid", null, null, true, testData);
        apxDataDao.putData(VendorKnownCode.class, "mygroupingid", "mypartitionid", null, false, testData);
    }

    private void testRestore() throws Exception
    {
        List<VendorKnownCode> testData;

        testData = apxDataDao.getData(VendorKnownCode.class, "mygroupingid");

        System.out.println("___ testRestore got " + testData.size());
    }

    private void testIterator() throws Exception
    {
        Iterator<VendorKnownCode> testData;

        testData = apxDataDao.getDataIterator(VendorKnownCode.class, "mygroupingid");

        System.out.println("___ testIterator got " + testData);

        while (testData.hasNext())
            System.out.println("  ___ testIterator got " + testData.next());
    }

    private List<VendorKnownCodesContainer.VendorKnownCode> makeVkcData()
    {
        List<VendorKnownCode> all = new ArrayList<>();

        for (int i = 0; i < 10; i++)
        {
            VendorKnownCode.Builder builder = VendorKnownCode.newBuilder();

            builder.setMemberID("hi " + i);
            all.add(builder.build());
        }

        return all;
    }

    public static void main(String... args) throws Exception
    {
        try
        {
            TestEncryptPersist driver = new TestEncryptPersist(args[0]);

            driver.testPersist();
            driver.testRestore();
            driver.testIterator();
        }
        catch (Throwable x)
        {
            x.printStackTrace();
        }
        finally
        {
            System.out.flush();
            System.err.flush();
            System.exit(0);
        }
    }

}
