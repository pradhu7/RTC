package com.apixio.dao;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.apixio.dao.seqstore.utility.SeqStoreCFUtility;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.restbase.config.*;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.utility.StringUtil;

public class DAOTestUtils
{
    private static final String cYamlFile = "/Users/vramkouramajian/Projects/Experiments/config/configlocal.yaml";
    // private static final String cYamlFile = "/Users/drvk/config/configlocal.yaml";
    //private static final String cYamlFile = "/Users/dyee/config/configlocal.yaml";
    // private static final String cYamlFile = "configlocal.yaml";
    // private static final String cYamlFile = "configstage.yaml";
    // private static final String cYamlFile = "configdprod.yaml";

    public ConfigSet config;
    public DaoServices daoServices;
    public TestCassandraTables testCassandraTables;

    public DAOTestUtils() throws Exception
    {
        config = ConfigSet.fromYamlFile((new File(cYamlFile)));
        daoServices = DaoServicesSet.createDaoServices(config);

        // This is to prevent the bug when running test suites
        // The bug was introduced in 2.x series of Cassandra
        // Hopefully, solved in 3.x series of Cassandra
        daoServices.getScienceCqlCrud().setStatementCacheEnabled(false);

        if (config.getBoolean("customerPropertiesConfig.local", true))
        {
            setCustomerProperties();
        }

    }

    public void setSequenceStoreColumnFamily(String properties) throws Exception
    {
        CustomerProperties customerProperties =  daoServices.getCustomerProperties();

        customerProperties.setSequenceStoreColumnFamily(properties, testCassandraTables.testOrg);
    }

    public Map<String, String> getAllColumnFamiliesGut(String orgId)
            throws Exception
    {
        CustomerProperties customerProperties =  daoServices.getCustomerProperties();
        String property = customerProperties.getSequenceStoreColumnFamily(orgId);
        return StringUtil.mapFromString(property);
    }

     private void setCustomerProperties()  throws  Exception
     {
        CustomerProperties customerProperties =  daoServices.getCustomerProperties();
        customerProperties.setNonPersistentMode();
         customerProperties.setColumnFamily2(testCassandraTables.patient, testCassandraTables.testOrg);
        customerProperties.setSequenceStoreColumnFamily(getSeqStoreProperty(), testCassandraTables.testOrg);
        customerProperties.setAFSFolder(testCassandraTables.testOrg, testCassandraTables.testOrg);
    }

    public static class TestCassandraTables
    {
        public static final String testOrg = "1";
        public static final long   testOrgId = 1;

        public static final String patient = "test_patient_cf";
        public static final String nsummary = "test_nsummary_cf";
        public static final String seqStoreHistorical = "test_seqstore_historical_cf::1486146720897::true";
        public static final String seqStoreNonInferred = "test_seqstore_non_inferred_cf::1486146720897::true";
        public static final String seqStoreGlobal = "test_seqstore_global_cf::1486146720897::true";
    }

    public String getSeqStoreProperty()
    {
        Map<String, String> seqStoreMap = new HashMap<>();

        seqStoreMap.put(SeqStoreCFUtility.non_inferred, testCassandraTables.seqStoreNonInferred);
        seqStoreMap.put(SeqStoreCFUtility.historical, testCassandraTables.seqStoreHistorical);
        seqStoreMap.put(SeqStoreCFUtility.global, testCassandraTables.seqStoreGlobal);

        return StringUtil.mapToString(seqStoreMap);
    }
}
