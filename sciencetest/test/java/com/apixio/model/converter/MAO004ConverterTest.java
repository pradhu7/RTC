package com.apixio.model.converter;

import com.apixio.SysServices;
import com.apixio.dao.patient.PatientDAO;
import com.apixio.datasource.cassandra.CqlCrud;
import org.junit.BeforeClass;
import testutils.ConverterTestUtils;

import java.util.List;
import java.util.UUID;

/**
 * Created by jctoledo on 11/3/16.
 */
public class MAO004ConverterTest {
    private static final String raRuleFileName = "ontology/RiskAdjustmentInsuranceClaim_1.10.4-SNAPSHOT.rule";
    private static final String maoPatientListFileName = "sample_mao_patient-uuids.txt";
    private static final String securityFileName = "apixio-security.properties";
    private static ClassLoader cl = null;
    private static final String cassandra_host = "54.148.81.174,54.213.141.193,54.201.203.33,54.213.120.152,54.213.19.243,54.203.11.218,54.202.255.55,54.200.209.74,54.191.235.45,54.218.125.136,54.149.123.48,54.213.115.43,54.218.176.141,54.244.193.218,54.218.80.119,54.201.113.170,54.213.57.209,54.213.240.163,54.201.41.162,54.213.216.251,54.201.114.126,54.214.141.198,54.149.222.104,54.218.135.68,54.244.63.100,54.200.165.254,54.201.141.141,54.149.110.232,54.244.201.219,52.89.224.241,54.69.187.188,54.187.33.63";

    //private static final String cassandra_host = "10.0.3.202,10.0.2.78,10.0.2.79,10.0.2.80,10.0.3.28,10.0.3.29,10.0.3.30,10.0.3.58,10.0.0.67,10.0.0.68,10.0.1.126,10.0.2.192,10.0.3.110,10.0.1.138,10.0.1.53,10.0.0.75,10.0.2.96,10.0.3.164,10.0.3.204,10.0.0.55,10.0.0.115,10.0.0.99,10.0.0.114,10.0.3.205,10.0.3.206,10.0.3.207,10.0.3.209,10.0.3.210";

    private static final String redis_host = "52.11.105.117";
    private static final String redis_prefix = "productionauth-";
    private static final int redis_port = 6379;
    private static CqlCrud cqlConnector = null;
    private static SysServices sysServices = null;
    private static PatientDAO dao = null;
    private static List<UUID> maoPatientList = null;


    @BeforeClass
    public static void setupOnce() {
        cl = MAO004ConverterTest.class.getClassLoader();
        org.junit.Assume.assumeTrue(ConverterTestUtils.fileExists(raRuleFileName));
        org.junit.Assume.assumeTrue(ConverterTestUtils.fileExists(securityFileName));
        org.junit.Assume.assumeTrue(ConverterTestUtils.fileExists(maoPatientListFileName));
        cqlConnector = ConverterTestUtils.setupCassandra(cassandra_host);
        sysServices = ConverterTestUtils.setupSysServices(cqlConnector, redis_prefix, redis_host, redis_port);
        dao = ConverterTestUtils.setupPatientDao(cqlConnector, sysServices);
        maoPatientList = ConverterTestUtils.getRandomPatients(maoPatientListFileName, 5);

    }




/*
    @Test
    public  void testSinglePatientWithReasoner() {
        try {
            UUID u = UUID.fromString("62c4dc94-f87c-4457-9435-53b5c8fc1dc7");
            List<PatientWithTS> pList = dao.getAllPartialPatients(u, false);
            NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
            for (PatientWithTS pwts : pList) {
                pwts.patient.setPatientId(u);
                com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(pwts.patient).getNewPatient();
                NewAPOToRDFModel partialPatient = new NewAPOToRDFModel(np);
                newAPOModel.getJenaModel().add(partialPatient.getJenaModel());
            }
            RDFModel am = newAPOModel.getJenaModel();
            int raCount = SPARQLUtils.getCountsByType(am, new URI(OWLVocabulary.OWLClass.RiskAdjustmentInsuranceClaim.uri()));


            File raRules = new File(raRuleFileName);

            RDFModelToEventType rmtet = new RDFModelToEventType(am, raRules);
            RDFModel deductions = rmtet.getModel().getDeductions();
            System.out.println(deductions.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}
