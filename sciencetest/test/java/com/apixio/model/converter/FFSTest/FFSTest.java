package com.apixio.model.converter.FFSTest;

import com.apixio.model.converter.MAOTest.MAOTest;
import com.apixio.model.converter.bin.converters.NewAPOToRDFModel;
import com.apixio.model.converter.bin.converters.OldAPOToNewAPO;
import com.apixio.model.converter.bin.converters.RDFModelToEventType;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.event.EventType;
import com.apixio.model.event.transformer.EventTypeJSONParser;
import com.apixio.model.patient.*;
import com.hp.hpl.jena.sparql.util.Convert;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.ConverterTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by jctoledo on 1/10/17.
 */
public class FFSTest {
    private static final String raRuleFileName = "ontology/RiskAdjustmentInsuranceClaim_1.10.4-SNAPSHOT.rule";
    private static String raRuleString = null;
    private static final String securityFileName = "apixio-security.properties";
    private static Patient singlePatientWithDelete = null;
    private static List<Patient> multiplePatientsWithDelete = null;
    private static Patient bogusPatient = null;



    @BeforeClass
    public static void setupOnce(){
        org.junit.Assume.assumeTrue(ConverterTestUtils.fileExists(securityFileName));
        InputStream is = FFSTest.class.getClassLoader().getResourceAsStream(raRuleFileName);
        raRuleString = ConverterTestUtils.makeStringFromInputStream(is);
        bogusPatient = initiPatientWithMultipleProcedures();

    }

    @Test
    public void testOne() throws IOException {
        com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(bogusPatient).getNewPatient();
        RDFModel mod = new NewAPOToRDFModel(np).getJenaModel();
        List<com.apixio.model.event.EventType> events = new RDFModelToEventType(mod, raRuleString).getEventTypes();
        assertEquals(4, events.size());
    }


    private static Patient initiPatientWithMultipleProcedures(){
        Patient rm = new Patient();
        DateTime now = DateTime.now();

        ExternalID orig = new ExternalID();
        orig.setAssignAuthority("BBBB");
        orig.setId("AAAA");

        ParsingDetail pd = new ParsingDetail();
        pd.setParsingDateTime(now);
        pd.setParserVersion("0.1");
        rm.addParsingDetail(pd);

        Source s = new Source();
        s.setCreationDate(now);
        s.setSourceType("ACA_CLAIM");
        rm.addSource(s);

        //create 2 procedures which are ffs claims
        Procedure proc1 = new Procedure();
        Procedure proc2 = new Procedure();

        proc1.setOriginalId(orig);
        proc2.setOriginalId(orig);

        proc1.setLastEditDateTime(now);
        proc2.setLastEditDateTime(now);

        proc1.setMetaTag("CLAIM_TYPE", "FEE_FOR_SERVICE_CLAIM");
        proc2.setMetaTag("CLAIM_TYPE", "FEE_FOR_SERVICE_CLAIM");


        proc1.setMetaTag("TRANSACTION_DATE", "2015-01-01");
        proc2.setMetaTag("TRANSACTION_DATE", "2015-01-01");

        proc1.setSourceId(s.getInternalUUID());
        proc2.setSourceId(s.getInternalUUID());

        proc1.setParsingDetailsId(pd.getParsingDetailsId());
        proc2.setParsingDetailsId(pd.getParsingDetailsId());

        //start and end date
        DateTime start = new DateTime(2015, 1,1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2015, 1,1,0,0,DateTimeZone.UTC);

        proc1.setPerformedOn(start);
        proc2.setPerformedOn(start);
        proc1.setEndDate(end);
        proc2.setEndDate(end);

        //add a diagnosis
        String randstr = UUID.randomUUID().toString();
        //add a DX
        ClinicalCode c1 = ConverterTestUtils.createCC("banana", "ICD9", "ICD9-OID", "V1", "banana");
        List<ClinicalCode> ccs = new ArrayList<>();
        ccs.add(c1);

        randstr = UUID.randomUUID().toString();
        ClinicalCode c2 = ConverterTestUtils.createCC("potato", "ICD9", "ICD9-OID", "V1", "potato");
        List<ClinicalCode> ccs2 = new ArrayList<>();
        ccs.add(c2);

        proc1.setSupportingDiagnosis(ccs);
        proc2.setSupportingDiagnosis(ccs2);


        ClinicalCode cpt1 = ConverterTestUtils.createCC("ABC123", "CPT_4", "CPT-OID", "V2.3", "ABC123");
        ClinicalCode cpt2 = ConverterTestUtils.createCC("ABC234", "CPT_4", "CPT-OID", "V2.3", "ABC234");

        proc1.setCode(cpt1);
        proc2.setCode(cpt2);

        rm.addProcedure(proc1);
        rm.addProcedure(proc2);

        return rm;
    }

}
