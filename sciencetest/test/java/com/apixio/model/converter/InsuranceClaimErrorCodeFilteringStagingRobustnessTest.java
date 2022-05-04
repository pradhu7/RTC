package com.apixio.model.converter;

import com.apixio.SysServices;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.patient.PatientDAO;
import com.apixio.dao.patient.PatientWithTS;
import com.apixio.datasource.cassandra.CqlConnector;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.model.converter.bin.converters.NewAPOToRDFModel;
import com.apixio.model.converter.bin.converters.OldAPOToNewAPO;
import com.apixio.model.converter.bin.converters.RDFModelToEventType;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.restbase.config.TokenConfig;
import com.apixio.restbase.dao.Tokens;
import com.datastax.driver.core.ConsistencyLevel;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import testutils.ConverterTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * Created by jctoledo on 6/15/16.
 */
public class InsuranceClaimErrorCodeFilteringStagingRobustnessTest {
    private static final String pdis_10000385_with_errors = "new_loader/patientuuids_dev_pds_472_test_2.txt";
    private static final String claimFilterFileName = "claim_filters/insurance_claim_filter_correct.csv";
    private static final String securityFileName = "apixio-security.properties";
    private static final String cassandra_host = "52.27.56.1";
    private static final String redis_host = "redis-1-stg.apixio.com";
    private static final String columnFamily = "apx_cfAcl_development";
    private static final String redis_prefix = "development-";
    private static final int redis_port = 6379;
    private static InputStream claimFilterIs = null;
    private static Logger logger = Logger.getLogger(ConverterTest.class);
    private static int sampleSize = 200;
    private static CqlCrud cqlConnector = null;
    private static SysServices sysServices = null;
    private static PatientDAO dao = null;
    private static String claimFilterString = "";

    @BeforeClass
    public static void setupOnce(){
        try {
            logger.info("Starting setup of test");
            org.junit.Assume.assumeTrue(fileExists(securityFileName));
            org.junit.Assume.assumeTrue(fileExists(claimFilterFileName));
            org.junit.Assume.assumeTrue(fileExists(pdis_10000385_with_errors));
            cqlConnector = setupCassandra(cassandra_host);
            sysServices = setupSysServices(cqlConnector, redis_prefix, redis_host, redis_port);
            dao = setupPatientDao(cqlConnector, sysServices);

            File claimF = getFileFromPath(claimFilterFileName);
            claimFilterIs = FileUtils.openInputStream(claimF);
            claimFilterString = makeStringFromInputStream(claimFilterIs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static CqlCrud setupCassandra(String hosts) {
        CqlCrud crud = null;
        try {
            CqlConnector connector = new CqlConnector();
            connector.setBinaryPort(9042);
            connector.setKeyspaceName("apixio");
            connector.setHosts(hosts);
            connector.setMinConnections(5);
            connector.setMaxConnections(30);
            connector.setDefaultReadConsistencyLevel(ConsistencyLevel.ONE.name());
            connector.init();
            crud = new CqlCrud();
            crud.setStatementCacheEnabled(true);
            crud.setCqlConnector(connector);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return crud;
    }

    private static SysServices setupSysServices(CqlCrud cc, String prefix, String host, int port) {
        int timeout = 60000;
        SysServices ss = null;
        try {
            JedisPoolConfig jpc = new JedisPoolConfig();
            jpc.setTestWhileIdle(true);
            jpc.setTestOnBorrow(true);
            jpc.setMaxTotal(100);
            JedisPool jp = new JedisPool(jpc, host, port, timeout);
            RedisOps ro = new RedisOps(jp);
            Transactions t = new Transactions(jp);
            ro.setTransactions(t);
            Tokens tokens = new Tokens(ro, prefix, new TokenConfig());
            ss = new SysServices(ro, t, cc, tokens, columnFamily);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ss;
    }

    private static PatientDAO setupPatientDao(CqlCrud cc, SysServices ss) {
        PatientDAO pdao = new PatientDAO();
        CustomerProperties cp = new CustomerProperties();
        cp.setSysServices(ss);
        pdao.setCqlCrud(cc);
        pdao.setCustomerProperties(cp);
        pdao.setLinkCF("apx_cflink_new");
        return pdao;
    }


    @Test
    public void errorCodeRobustnessTest(){
        File f = getFileFromPath(pdis_10000385_with_errors);
        File claimF = getFileFromPath(claimFilterFileName);
        List<com.apixio.model.event.EventType> totalEvents = new ArrayList<>();
        try {
            List<String> lines = FileUtils.readLines(f);
            for (String l :lines){
                UUID u = UUID.fromString(l.trim());
                if (claimF != null) {
                    List<PatientWithTS> pList = dao.getAllPartialPatients(u, false);
                    NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
                    for (PatientWithTS pwts : pList) {
                        pwts.patient.setPatientId(u);
                        com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(pwts.patient).getNewPatient();
                        NewAPOToRDFModel partialPatient = new NewAPOToRDFModel(np);
                        newAPOModel.getJenaModel().add(partialPatient.getJenaModel());
                    }
                    RDFModel am = newAPOModel.getJenaModel();
                    claimFilterIs = FileUtils.openInputStream(claimF);
                    RDFModelToEventType rmtet = new RDFModelToEventType(am, claimFilterString);
                    List<com.apixio.model.event.EventType> events = rmtet.getEventTypes();
                    totalEvents.addAll(events);
                }
            }
            boolean sizeFlag = false;
            if(totalEvents.size()>2600){
                sizeFlag = true;
            }
            assertTrue(sizeFlag);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String makeStringFromInputStream(InputStream ais){
        StringBuilder sb=new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(ais));
        String read;
        try {
            while((read=br.readLine()) != null) {
                sb.append(read.trim()+"\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    /**
     * Verify that an patient-uuids.txt file exists in the test's resources directory
     *
     * @return true if the file exists
     */
    private static boolean fileExists(String aFileName) {
        try {
            ClassLoader cl = ConverterTest.class.getClassLoader();
            File inputF = new File(cl.getResource(aFileName).getFile());
            if (ConverterTestUtils.countLines(inputF) == 0) {
                return false;
            }
        } catch (Exception e) {
            System.out.println(aFileName + " not found!");
            return false;
        }
        return true;
    }



    private static File getFileFromPath(String aFilePath) {
        File rm;
        try {
            ClassLoader cl = ConverterTest.class.getClassLoader();
            rm = new File(cl.getResource(aFilePath).getFile());
            return rm;
        } catch (Exception e) {
            System.out.println("Could not create file object!");
            return null;
        }
    }
}
