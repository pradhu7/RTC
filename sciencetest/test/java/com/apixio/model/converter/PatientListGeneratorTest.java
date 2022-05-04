package com.apixio.model.converter;

import com.apixio.SysServices;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.patient.PatientDAO;
import com.apixio.datasource.cassandra.CqlConnector;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.patient.*;
import com.apixio.restbase.config.TokenConfig;
import com.apixio.restbase.dao.Tokens;
import com.datastax.driver.core.ConsistencyLevel;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import testutils.ConverterTestUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;

/**
 * Created by jctoledo on 4/27/16.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PatientListGeneratorTest {
    private static final String patientListFileName = "patient-uuids.txt";
    private static final String patientListWithPorP = "patient-uuids-with-problems-or-procedures.txt";
    private static final String patientsListFileWithRAClaims = "patient-uuids-with-ra-claims.txt";
    private static final String cassandra_host = "52.27.56.1";
    //private static final String cassandra_host = "10.0.3.202";
    private static final String redis_host = "redis-1-stg.apixio.com";
    //private static final String redis_host = "10.0.3.195";
    private static final String redis_prefix = "development-";
    //private static final String redis_prefix = "productionauth-";
    private static final int redis_port = 6379;
    private static Logger logger = Logger.getLogger(ConverterTest.class);
    private static int sampleSize = 5000;
    private static CqlCrud cqlConnector = null;
    private static SysServices sysServices = null;
    private static PatientDAO dao = null;
    private static List<UUID> patientIds = null;
    private static Map<Patient, com.apixio.model.owl.interfaces.Patient> patientMap = null;
    private static Map<com.apixio.model.owl.interfaces.Patient, RDFModel> newAPOToJenaMap = null;
    private static List<UUID> patientsWithRAClaims = null;
    private static List<UUID> patientsWithProblemsOrProcedures = null;
    private static List<UUID> patientsWithCodableEntities = null;


    @BeforeClass
    public static void setupOnce() {
        logger.info("Starting setup of test");
        org.junit.Assume.assumeTrue(securityFileExists());
        org.junit.Assume.assumeTrue(patientListFileExists(patientListFileName));
        cqlConnector = setupCassandra(cassandra_host);
        sysServices = setupSysServices(cqlConnector, redis_prefix, redis_host, redis_port);
        dao = setupPatientDao(cqlConnector, sysServices);
        patientIds = ConverterTestUtils.getRandomPatients(patientListFileName, sampleSize);

        if (patientIds.size() == 0) {
            fail("Empty patient list - aborting test!");
        }

    }


    /**
     * Verify that an apixio-security.properties file exists in the test's resources directory
     *
     * @return true if the file exists
     */
    private static boolean securityFileExists() {
        try {
            ClassLoader classLoader = ConverterTest.class.getClassLoader();
            File inputF = new File(classLoader.getResource("apixio-security.properties").getFile());
            if (inputF.length() == 0) {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Security file not found!");
            return false;
        }
        return true;
    }

    /**
     * Verify that an patient-uuids.txt file exists in the test's resources directory
     *
     * @return true if the file exists
     */
    private static boolean patientListFileExists(String aFileName) {
        try {
            ClassLoader cl = ConverterTest.class.getClassLoader();
            File inputF = new File(cl.getResource(aFileName).getFile());
            if (ConverterTestUtils.countLines(inputF) == 0) {
                return false;
            }
        } catch (Exception e) {
            System.out.println(aFileName + " list not found!");
            return false;
        }
        return true;
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
            ss = new SysServices(ro, t, cc, tokens, "apx_cfAcl_development");
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



   // @Test
//    public void populatePatientListWithRAClaims() {
//        // org.junit.Assume.assumeFalse(patientListFileExists(patientsListFileWithRAClaims));
//        logger.info("Populating patient with RA claim list");
//        //only execute this test if the file does not exist
//        ClassLoader classLoader = PatientListGeneratorTest.class.getClassLoader();
//        //find patients with RA claims
//        patientsWithRAClaims = new ArrayList<>();
//        int c = 0;
//        for (UUID u : patientIds) {
//            try {
//                Patient p = dao.getPatient(u);
//                List<Problem> raClaims = ConverterUtils.getRAClaimCandidatesFromProblems(p.getProblems());
//                if (raClaims.size() > 0 && !patientsWithRAClaims.contains(u)) {
//                    patientsWithRAClaims.add(u);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            c++;
//        }
//        try {
//
//            FileUtils.writeLines(new File("/tmp/patientsListFileWithRAClaims.txt"), patientsWithRAClaims);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        logger.info("Finished - Populating patient with RA claim list");
//    }

    @Test
    public void populatePatientsWithProblemsOrProcedures() {
        //only execute this test if the file does not exist
        //org.junit.Assume.assumeFalse(patientListFileExists(patientListWithPorP));
        ClassLoader classLoader = PatientListGeneratorTest.class.getClassLoader();
        //find patients with problems or procedures
        patientsWithProblemsOrProcedures = new ArrayList<>();
        patientsWithCodableEntities = new ArrayList<>();
        for (UUID u : patientIds) {
            try {
                Patient p = dao.getPatient(u);
                Iterable<Problem> pItr = p.getProblems();
                Iterable<Procedure> ppItr = p.getProcedures();
                Iterable<Allergy> aItr = p.getAllergies();
                Iterable<Prescription> presItr = p.getPrescriptions();
                Iterable<BiometricValue> bItr = p.getBiometricValues();
                Iterable<FamilyHistory> fItr = p.getFamilyHistories();
                Iterable<LabResult> lItr = p.getLabs();

                while (pItr.iterator().hasNext()) {
                    if (!patientsWithCodableEntities.contains(u)) {
                        Problem prob = pItr.iterator().next();
                        UUID pSourceId = prob.getSourceId();
                        Source s = getSourceFromIterable(pSourceId, p.getSources());
                        if(!s.getSourceType().equalsIgnoreCase("CMS_KNOWN")) {
                            patientsWithCodableEntities.add(u);
                        }
                    }
                    break;
                }
                while (ppItr.iterator().hasNext()) {
                    if (!patientsWithCodableEntities.contains(u)) {
                        patientsWithCodableEntities.add(u);
                    }
                    break;
                }
                while(aItr.iterator().hasNext()){
                    if(!patientsWithCodableEntities.contains(u)){
                        patientsWithCodableEntities.add(u);
                    }
                    break;
                }
                while(presItr.iterator().hasNext()){
                    if(!patientsWithCodableEntities.contains(u)){
                        patientsWithCodableEntities.add(u);
                    }
                    break;
                }
                while(bItr.iterator().hasNext()){
                    if(!patientsWithCodableEntities.contains(u)){
                        patientsWithCodableEntities.add(u);
                    }
                    break;
                }
                while(fItr.iterator().hasNext()){
                    if(!patientsWithCodableEntities.contains(u)){
                        patientsWithCodableEntities.add(u);
                    }
                    break;
                }
                while(lItr.iterator().hasNext()){
                    if(!patientsWithCodableEntities.contains(u)){
                        patientsWithCodableEntities.add(u);
                    }
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            FileUtils.writeLines(new File("/tmp/patientListWithPorP.txt"), patientsWithProblemsOrProcedures);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Source getSourceFromIterable(UUID needle, Iterable<Source> haystack) {
        for (Source s : haystack) {
            if (s.getSourceId().equals(needle)) {
                return s;
            }
        }
        logger.warn("Null source object returned with this needle: \n" + needle );
        return null;
    }
}
