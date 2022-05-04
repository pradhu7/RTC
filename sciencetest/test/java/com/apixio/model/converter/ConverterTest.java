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
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.converter.utils.SPARQLUtils;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.apixio.model.event.transformer.EventTypeJSONParser;
import com.apixio.model.patient.*;
import com.apixio.restbase.config.TokenConfig;
import com.apixio.restbase.dao.Tokens;
import com.datastax.driver.core.ConsistencyLevel;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.*;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runners.MethodSorters;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import testutils.ConverterTestUtils;

import static org.junit.Assert.*;

/**
 * Created by jctoledo on 3/10/16.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConverterTest {
    private static final String claimFilterFileName = "ontology/test.rule";
    private static final String securityFileName = "apixio-security.properties";
    private static final String patientListFileName = "patient-uuids.txt";
    private static final String patientListWithPorP = "patient-uuids-with-problems-or-procedures.txt";
    private static final String patientsListFileWithRAClaims = "patient-uuids-with-ra-claims.txt";
    private static final String cassandra_host = "52.27.56.1";
    //private static final String cassandra_host = "10.0.3.202";
    private static final String redis_host = "redis-1-stg.apixio.com";
    //private static final String redis_host = "10.0.3.195";
    private static final String redis_prefix = "development-";
    //private static final String redis_prefix = "production-";
    private static final String columnFamily = "apx_cfAcl_development";
    //private static final  String columnFamily = "apx_cfAcl";
    private static final int redis_port = 6379;
    private static InputStream claimFilterIs = null;
    private static Logger logger = Logger.getLogger(ConverterTest.class);
    private static int sampleSize = 200;
    private static CqlCrud cqlConnector = null;
    private static SysServices sysServices = null;
    private static PatientDAO dao = null;
    private static List<UUID> patientIds = null;
    private static Map<com.apixio.model.patient.Patient, com.apixio.model.owl.interfaces.Patient> patientMap = null;
    private static Map<com.apixio.model.owl.interfaces.Patient, RDFModel> newAPOToJenaMap = null;
    private static List<RDFModel> RDFModels = new ArrayList<>();
    private static List<UUID> patientsWithPorP = null;
    private static List<UUID> patientsWithRAClaims = null;
    private static InputStream claimFilterInputStream = null;
    private static String claimFilterString = "";
    private static ClassLoader cl = null;
    @BeforeClass
    public static void setupOnce() {

        cl = ConverterTest.class.getClassLoader();
        try {
            logger.info("Starting setup of test");
            org.junit.Assume.assumeTrue(fileExists(securityFileName));
            org.junit.Assume.assumeTrue(fileExists(patientListFileName));
            org.junit.Assume.assumeTrue(fileExists(patientsListFileWithRAClaims));
            org.junit.Assume.assumeTrue(fileExists(claimFilterFileName));

            cqlConnector = setupCassandra(cassandra_host);
            sysServices = setupSysServices(cqlConnector, redis_prefix, redis_host, redis_port);
            dao = setupPatientDao(cqlConnector, sysServices);
            patientIds = ConverterTestUtils.getRandomPatients(patientListFileName, sampleSize);
            patientsWithPorP = ConverterTestUtils.getRandomPatients(patientListWithPorP, sampleSize);
            patientsWithRAClaims = ConverterTestUtils.getRandomPatients(patientsListFileWithRAClaims, sampleSize);
            if (patientIds.size() == 0) {
                fail("Empty patient list - aborting test!");
            }
        /*if (patientsWithPorP.size() == 0) {
            fail("Empty patient list - aborting test!");
        }*/
            patientMap = new HashMap<>();
            newAPOToJenaMap = new HashMap<>();

            File claimF = getFileFromPath(claimFilterFileName);
            claimFilterInputStream = FileUtils.openInputStream(claimF);
            claimFilterString = makeStringFromInputStream(claimFilterInputStream);

            logger.info("Finished test setup");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setup(){
        try {
            File claimF = getFileFromPath(claimFilterFileName);
            claimFilterInputStream = FileUtils.openInputStream(claimF);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @After
    public void after(){
        try {
            claimFilterInputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
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

    /***
     * Create a map of old apos to new apos and test some fields
     */
    //@Test
    public void testAaa() {
        for (UUID u : patientIds) {
            try {
                Patient p = dao.getPatient(u);
                com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(p).getNewPatient();

                //check for demographics
                if (p.getPrimaryDemographics() != null) {
                    if (!p.getPrimaryDemographics().getGender().name().equals(np.getDemographics().getGenderType().getGenderValues().name())) {
                        fail("Gender values did not match!");
                    }
                    if (!p.getPatientId().toString().equals(np.getInternalXUUID().toString())) {
                        fail("Patient ids  do not match!");
                    }
                    //TODO - fill me in more with other fields to test
                }

                this.patientMap.put(p, np);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }//for
    }

    @Test
    public void testSingleRAClaimMAOPatient() {
        /**
         * a patient with a problem that is a mao-004 ra claim
         *
         */
        UUID u = UUID.fromString("7826d0b3-d83a-40e8-a31b-97b2421c28df");
        try{
            List<PatientWithTS> partials = dao.getAllPartialPatients(u, false);
            NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
            for (PatientWithTS pwts : partials) {
                pwts.patient.setPatientId(u);
                Iterable<Problem> pitrbl = pwts.patient.getProblems();
                for (Problem p: pitrbl){
                    //p.me
                }
                com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(pwts.patient).getNewPatient();
                NewAPOToRDFModel partialPatient = new NewAPOToRDFModel(np);
                newAPOModel.getJenaModel().add(partialPatient.getJenaModel());
            }
            RDFModel am = newAPOModel.getJenaModel();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }


    @Test
    public void testSinglePatient() throws Exception {
        //UUID u = UUID.fromString("50664044-47e3-4980-ade6-45568fd5c5d3");
        UUID u = UUID.fromString("7826d0b3-d83a-40e8-a31b-97b2421c28df");
        //UUID u = UUID.fromString("000036ba-90a7-43d8-93a4-3c681dee47f7");

        List<PatientWithTS> pList = dao.getAllPartialPatients(u, false);
        NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
        for (PatientWithTS pwts : pList) {
            pwts.patient.setPatientId(u);
            com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(pwts.patient).getNewPatient();
            NewAPOToRDFModel partialPatient = new NewAPOToRDFModel(np);
            newAPOModel.getJenaModel().add(partialPatient.getJenaModel());
        }
        RDFModel am = newAPOModel.getJenaModel();
        FileOutputStream fos = FileUtils.openOutputStream(new File("/tmp/patient.nt"));
        am.write(fos, "N-Triples");
        String baseid = am.getPatientXUUID().toString();
        File dir = new File("/tmp/events/" + baseid);
        int event_count = 1;
        List<com.apixio.model.event.EventType> events = new RDFModelToEventType(am, claimFilterString).getEventTypes();
        for (com.apixio.model.event.EventType et : events) {

            if (!dir.exists()) {
                FileUtils.forceMkdir(dir);
            }
            EventTypeJSONParser etjp = new EventTypeJSONParser();
            String toJson = etjp.toJSON(et);


            File oEvents = new File(dir, baseid + "_" + event_count + ".json");
            FileUtils.write(oEvents, toJson);
            event_count++;
        }
    }

    @Test
    public void testSinglePatientWithReasoner()throws  Exception{
//        FileOutputStream fos2 = new FileOutputStream("/tmp/test.inf");
//
//        UUID u = UUID.fromString("7826d0b3-d83a-40e8-a31b-97b2421c28df"); //raps
//        //u = UUID.fromString("c58aaa96-e2a0-47eb-b16a-4b12924864c1"); //MAO patient id
//        UUID udoc =  UUID.fromString("37a39e71-ff62-4349-8121-f07cf09f77ff"); //MAO document id
//        List<PatientWithTS> pList = dao.getAllPartialPatients(u, false);
//        com.apixio.model.patient.Patient p = dao.getSinglePartialPatientByDocumentUUID(udoc);
//        NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
//        for (PatientWithTS pwts : pList) {
//            pwts.patient.setPatientId(u);
//            com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(pwts.patient).getNewPatient();
//            NewAPOToRDFModel partialPatient = new NewAPOToRDFModel(np);
//            newAPOModel.getJenaModel().add(partialPatient.getJenaModel());
//        }
//        RDFModel am = newAPOModel.getJenaModel();
//
//        int raCount = SPARQLUtils.getCountsByType(am, new URI(OWLVocabulary.OWLClass.RiskAdjustmentInsuranceClaim.uri()));
//        String s = makeStringFromInputStream(cl.getResourceAsStream("ontology/RiskAdjustmentInsuranceClaim_1.10.4-SNAPSHOT.rule"));
//        //Reasoner r = new GenericRuleReasoner(com.hp.hpl.jena.reasoner.rulesys.Rule.parseRules(s));
//
//
//
//        Reasoner r2 = GenericRuleReasonerFactory.theInstance().create(null);
//        InfModel infmodel = ModelFactory.createInfModel(r2, am);
//        Model inferences = infmodel.getDeductionsModel();
//        inferences.write(fos2, "TTL");
//        int  rapsCount = SPARQLUtils.getCountsByType(inferences, new URI("http://apixio.com/ontology/ilo#RAPSRAInsuranceClaim"));
//        assertEquals(raCount, rapsCount);
////
////
////        String str = cl.getResource("ontology/RiskAdjustmentInsuranceClaim_1.10.4-SNAPSHOT.rule").getFile();
////        RDFModelToEventType rmtet = new RDFModelToEventType(am, new File(str));
////        RDFModel deductions = rmtet.getModel().getDeductions();
////        System.out.println(deductions.size());
////        List<com.apixio.model.event.EventType> events = rmtet.getEventTypes();

    }



    private void writeStringToFile(String aString, File aFile) throws IOException {
        FileUtils.writeStringToFile(aFile, aString);
    }

    public String stringStatements(Model m, Resource s, Property p, Resource o) {
        StringBuilder sb = new StringBuilder();
        for (StmtIterator i = m.listStatements(s,p,o); i.hasNext(); ) {
            Statement stmt = i.nextStatement();
            sb.append(" - " + PrintUtil.print(stmt)+"\n");
        }
        return sb.toString();
    }

    @Test
    public void findPatientsWithRAPSErrors() {
        File fout = new File("/tmp/ids_with_errors_1000385.txt");
        File f = getFileFromPath("new_loader/10000385_patient_uuids.txt");
        List<String> idsTowrite = new ArrayList<>();
        try {
            int idcount = 0;
            List<String> uuids = FileUtils.readLines(f);
            outer:
            for (String u : uuids) {
                UUID id = UUID.fromString(u.trim());
                idcount++;
                List<PatientWithTS> pList = dao.getAllPartialPatients(id, false);
                for (PatientWithTS pwts : pList) {
                    pwts.patient.setPatientId(id);
                    Iterator<Problem> probs = pwts.patient.getProblems().iterator();
                    while (probs.hasNext()) {
                        Problem p = probs.next();
                        if (p.getMetadata() != null) {
                            //i need to find metadata fields which contain the word "error"
                            List<String> x = getListOfErrorCodesFromMetadata(p.getMetadata());
                            if (x.size() > 0) {
                                if (!idsTowrite.contains(u)) {
                                    idsTowrite.add(u);
                                    continue outer;
                                }
                            }
                        }
                    }
                }
            }
            FileUtils.writeLines(fout, idsTowrite, "\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * Iterate over the metadata of a problem object to return a list with all of the values of keys that contain the word 'error'
     *
     * @param someProblemMetadata a metadata map for a problem object
     * @return a list of all error codes found in this metadata list
     */
    private static List<String> getListOfErrorCodesFromMetadata(Map<String, String> someProblemMetadata) {
        List<String> rm = new ArrayList<>();
        for (Map.Entry<String, String> entry : someProblemMetadata.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (key.toUpperCase().contains("ERROR")) {
                //this value is the one that has an error code
                if (!rm.contains(val)) {
                    rm.add(val);
                }
            }
        }
        return rm;
    }

    //@Test
    public void errorCodeRobustnessTest(){
        File f = getFileFromPath("new_loader/patientuuids_dev_pds_472_with_errors.txt");
        org.junit.Assume.assumeTrue(fileExists(claimFilterFileName));
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

    @Test
    public void testSinglePatientWithClaimFilter() {
        UUID u = UUID.fromString("0015e785-e560-45c7-88f3-eac67d9d775b"); //with error
        //UUID u = UUID.fromString("ecebeea6-3416-4bf8-b405-3b9538e0b4a3"); //without error
        org.junit.Assume.assumeTrue(fileExists(claimFilterFileName));
        File claimF = getFileFromPath(claimFilterFileName);
        try {
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

                String baseid = am.getPatientXUUID().toString();
                File dir = new File("/tmp/events/" + baseid);
                int event_count = 1;
                FileOutputStream fos = FileUtils.openOutputStream(new File("/tmp/patient_withoutFilters.nt"));
                am.write(fos, "N-Triples");
                claimFilterIs = FileUtils.openInputStream(claimF);
                RDFModelToEventType rmtet = new RDFModelToEventType(am, claimFilterString);
                List<com.apixio.model.event.EventType> events = rmtet.getEventTypes();
                for (com.apixio.model.event.EventType et : events) {

                    if (!dir.exists()) {
                        FileUtils.forceMkdir(dir);
                    }
                    EventTypeJSONParser etjp = new EventTypeJSONParser();
                    String toJson = etjp.toJSON(et);


                    File oEvents = new File(dir, baseid + "_" + event_count + ".json");
                    FileUtils.write(oEvents, toJson);
                    event_count++;
                }
            }
        } catch (IOException e) {
            System.out.println("Could not open claim filter file!");
        } catch (Exception e) {
            System.out.println("Something went wrong!");
        }

    }

//    @Test
//    public void testA() throws Exception {
//
//        UUID u = UUID.fromString("5cd9dc99-d741-4676-85f2-9f432a538fdf");
//        NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
//        List<Problem> raClaims = new ArrayList<>();
//        List<PatientWithTS> pList = dao.getAllPartialPatients(u, false);
//        //count the number of claims
//        for (PatientWithTS pwts : pList) {
//            pwts.patient.setPatientId(u); // set the same id for all partials
//            Iterable<Problem> itr = pwts.patient.getProblems();
//            Iterable<Source> sitr = pwts.patient.getSources();
//
//            List<Problem> candidates = ConverterUtils.getRAClaimCandidatesFromProblems(itr);
//            if (candidates.size() > 0 && candidates != null) {
//                raClaims.addAll(candidates);
//            }
//
//        }
//        System.out.println(raClaims);
//    }



    /**
     * Given a list of oldAPOs create a list of APOModels
     */
    @Test
    public void testB() {
        for (UUID u : patientsWithPorP) {
            try {
                List<PatientWithTS> pList = dao.getAllPartialPatients(u, false);
                NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
                for (PatientWithTS pwts : pList) {
                    pwts.patient.setPatientId(u); // set the same id for all partials
                    com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(pwts.patient).getNewPatient();
                    NewAPOToRDFModel partialPatient = new NewAPOToRDFModel(np);
                    newAPOModel.getJenaModel().add(partialPatient.getJenaModel());
                }
                RDFModel patientModel = newAPOModel.getJenaModel();

                int count = ConverterTestUtils.countPatients(patientModel);
                if (count != 1) {
                    fail("Invalid count of patients found ! UUID: " + u);
                }
                if (!SPARQLUtils.askForDemographics(patientModel)) {
                    logger.info("Patient without demographics ! UUID: " + u);
                }
                if (SPARQLUtils.askForInsuranceClaim(patientModel)) {
                    logger.info("patient with insurance claim found");
                    //check what kind of claim it is
                    if (SPARQLUtils.askForRAClaim(patientModel)) {
                        logger.info("Patient with ra claim found");
                    }
                }
                //TODO add more tests here
                RDFModels.add(patientModel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Given a list of  APOModels create a list of event types
     */
    @Test
    public void testC() throws IOException {
        for (RDFModel am : RDFModels) {
            String baseid = am.getPatientXUUID().toString();
            int event_count = 1;
            List<com.apixio.model.event.EventType> events = new RDFModelToEventType(am, claimFilterString).getEventTypes();
            for (com.apixio.model.event.EventType et : events) {
                File oEvents = new File("/tmp/events/" + baseid + "/" + baseid + "_" + event_count + ".json");
                if (!oEvents.exists()) {
                    FileUtils.forceMkdir(oEvents);
                }
                EventTypeJSONParser etjp = new EventTypeJSONParser();
                String toJson = etjp.toJSON(et);
                FileUtils.write(oEvents, toJson);
                event_count++;
            }
        }
    }

    //@Test
    public void testzz() throws IOException {
        List<Long> timer1 = new ArrayList<>();
        List<Long> timer2 = new ArrayList<>();
        Set<UUID> ids = new HashSet<>();
        for (UUID u : patientIds) {
            try {
                com.apixio.model.patient.Patient p = dao.getPatient(u);
                long OldToNewAPOStartTime = System.currentTimeMillis();
                com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(p).getNewPatient();
                long OldToNewAPOEndTime = System.currentTimeMillis();
                timer1.add(OldToNewAPOEndTime - OldToNewAPOStartTime);

                long start = System.currentTimeMillis();
                new NewAPOToRDFModel(np);
                long end = System.currentTimeMillis();
                timer2.add(end - start);

                Iterable<Problem> pItr = p.getProblems();
                Iterable<Procedure> ppItr = p.getProcedures();

                while (pItr.iterator().hasNext()) {
                    int op = 0;
                    if (!ids.contains(p.getPatientId())) {
                        ids.add(p.getPatientId());
                    }
                    break;
                }
                while (ppItr.iterator().hasNext()) {
                    int q = 0;
                    if (!ids.contains(p.getPatientId())) {
                        ids.add(p.getPatientId());
                    }
                    break;
                }
                if (ids.size() > 0) {
                    FileUtils.writeLines(new File("/tmp/ids_with_problems_or_procedures.txt"), ids, true);
                    ids.removeAll(ids);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FileUtils.writeStringToFile(new File("/tmp/avg_conversion_time_old_to_new_apo.txt"), "Average conversion time old APO to New APO : " + ConverterTestUtils.average(timer1) + " for " + timer1.size() + " APOs");
        FileUtils.writeStringToFile(new File("/tmp/avg_conversion_time_new_to_graph.txt"), "Average conversion time new APO to graph: " + ConverterTestUtils.average(timer2) + " for " + timer2.size() + " APOs");
    }

    /**
     * A simple name validator
     *
     * @param p  an old APO to check validation against
     * @param np a new APO from which a name will be taken
     * @return true if names are identical, false otherwise
     */
    private boolean checkName(Patient p, com.apixio.model.owl.interfaces.Patient np) {
        if (p.getPrimaryDemographics().getName() != null && np.getDemographics().getName() != null) {
            Name n = p.getPrimaryDemographics().getName();
            com.apixio.model.owl.interfaces.Name nn = np.getDemographics().getName();
            Collection<String> intersectGn = CollectionUtils.retainAll(n.getGivenNames(), nn.getGivenNames());
            Collection<String> intersectFn = CollectionUtils.retainAll(n.getFamilyNames(), nn.getFamilyNames());
            Collection<String> intersectSn = CollectionUtils.retainAll(n.getSuffixes(), nn.getSuffixNames());
            Collection<String> intersectPref = CollectionUtils.retainAll(n.getPrefixes(), nn.getPrefixNames());

            if (intersectFn.size() != n.getFamilyNames().size()) {
                return false;
            }
            if (intersectGn.size() != n.getGivenNames().size()) {
                return false;
            }
            if (intersectSn.size() != n.getSuffixes().size()) {
                return false;
            }
            if (!n.getNameType().name().equals(nn.getNameType().getNameTypeValue().name())) {
                return false;
            }
            if (intersectPref.size() != n.getPrefixes().size()) {
                return false;
            }
            return true;
        }
        return false;
    }

    public RDFModel convertOneNewPatientToRDF(com.apixio.model.owl.interfaces.Patient np) throws IOException {
        NewAPOToRDFModel natjm = new NewAPOToRDFModel(np);
        RDFModel m = natjm.getJenaModel();
        return m;
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

}
