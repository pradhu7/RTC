package com.apixio.useracct.buslog;

import com.apixio.XUUID;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.DataServices;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.lang.Thread.sleep;

public class BatchLogicTest {
    private BatchLogic batchLogic;

    @Before
    public void setUp() throws IOException {
        File configFile = new File(com.apixio.restbase.dao.CustomPropertiesTest.class.getClassLoader().getResource("config.yaml").getFile());
        ConfigSet configSet = ConfigSet.fromYamlFile(configFile);
        PersistenceServices ps =  ConfigUtil.createPersistenceServices(configSet,null);
        DaoBase daoBase = new DaoBase(ps);
        DataServices ds = new DataServices(daoBase, configSet);
        ds.doPostInit();
        batchLogic = ds.getBatchLogic();
    }

    @Test
    public void testBatchLogicWithBadBatches() throws InterruptedException {
        XUUID xuuid = XUUID.fromString("O_00000000-0000-0000-0000-000000000380");
        //get bad batches from redis
        List<BatchLogic.UploadBatch> badBatch1 = batchLogic.getBadUploadBatches(xuuid,0,0);
        Assert.assertTrue(badBatch1.size() > 0);
        //get bad batches from a local cache
        List<BatchLogic.UploadBatch> badBatch2 =  batchLogic.getBadUploadBatches(xuuid,0,0);
        Assert.assertEquals(badBatch1.size(), badBatch2.size());
        sleep(6000);
        //cache got expired, and then get bad batches from redis again
        List<BatchLogic.UploadBatch> badBatch3 =  batchLogic.getBadUploadBatches(xuuid,0,0);
        Assert.assertEquals(badBatch2.size(), badBatch3.size());
    }

    @Test
    public void testBatchLogicWithoutBadBatches() {
        XUUID xuuid = XUUID.fromString("O_00000000-0000-0000-0000-000000001675");
        List<BatchLogic.UploadBatch> lstBatch = batchLogic.getBadUploadBatches(xuuid,0,0);
        Assert.assertTrue(lstBatch.size() == 0);
    }

    @Test
    public void testBatchLogicWithnonexistedPds() {
        XUUID xuuid = XUUID.fromString("O_00000000-0000-0000-0000-000000000000");
        List<BatchLogic.UploadBatch> lstBatch = batchLogic.getBadUploadBatches(xuuid,0,0);
        Assert.assertNull(lstBatch);
    }

    @Test
    public void testBatchLogicWithGoodBatches() throws InterruptedException {
        XUUID xuuid = XUUID.fromString("O_00000000-0000-0000-0000-000000001675");
        //get good batches from redis
        List<BatchLogic.UploadBatch> goodBatch1 = batchLogic.getUploadBatches(xuuid,true);
        Assert.assertTrue(goodBatch1.size() > 0);
        //get good batches from a local cache
        List<BatchLogic.UploadBatch> goodBatch2 =  batchLogic.getUploadBatches(xuuid,true);
        Assert.assertEquals(goodBatch1.size(), goodBatch2.size());
        sleep(6000);
        //cache got expired, and then get good batches from redis again
        List<BatchLogic.UploadBatch> goodBatch3 =  batchLogic.getUploadBatches(xuuid,true);
        Assert.assertEquals(goodBatch2.size(), goodBatch3.size());
    }
}
