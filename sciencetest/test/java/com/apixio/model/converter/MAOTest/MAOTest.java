package com.apixio.model.converter.MAOTest;

import com.apixio.model.converter.ConverterTest;
import com.apixio.model.converter.bin.converters.NewAPOToRDFModel;
import com.apixio.model.converter.bin.converters.OldAPOToNewAPO;
import com.apixio.model.converter.bin.converters.RDFModelToEventType;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.event.AttributeType;
import com.apixio.model.patient.*;
import com.apixio.model.patient.event.EventType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.ConverterTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by jctoledo on 12/21/16.
 */
public class MAOTest {
    private static final String raRuleFileName = "ontology/RiskAdjustmentInsuranceClaim_1.10.4-SNAPSHOT.rule";
    private static String raRuleString = null;
    private static final String securityFileName = "apixio-security.properties";
    private static Patient singlePatientWithDelete = null;
    private static List<Patient> multiplePatientsWithDelete = null;
    private static Patient bogusPatient = null;
    @BeforeClass
    public static void setupOnce(){
        org.junit.Assume.assumeTrue(ConverterTestUtils.fileExists(securityFileName));
        singlePatientWithDelete = initSinglePatientWithDelete();
        multiplePatientsWithDelete = initMultipleAPOsWithDelete();
        InputStream is = MAOTest.class.getClassLoader().getResourceAsStream(raRuleFileName);
        raRuleString = ConverterTestUtils.makeStringFromInputStream(is);
    }


    @Test
    public void testingSinglePatientWithDelete(){
        com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(singlePatientWithDelete).getNewPatient();
        RDFModel mod = new NewAPOToRDFModel(np).getJenaModel();
        List<com.apixio.model.event.EventType> events = new RDFModelToEventType(mod, raRuleString).getEventTypes();
        assertEquals(events.size(), 2);
        //now check that the event that is generated has the right evidence attribute
        int check = 0;
        for (com.apixio.model.event.EventType et : events) {
            List<AttributeType> atrs = et.getEvidence().getAttributes().getAttribute();
            for (AttributeType at : atrs) {
                if (at.getName().equals("transactionType") && at.getValue().equals("DELETE")) {
                    check++;
                }
            }
        }
        assertEquals(check, 2);
    }

    @Test
    public void testingMAOClaimWithMultipleDx(){
        int check = 0;
        for (Patient p: multiplePatientsWithDelete){
            com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(p).getNewPatient();
            RDFModel mod = new NewAPOToRDFModel(np).getJenaModel();
            List<com.apixio.model.event.EventType> events = new RDFModelToEventType(mod, raRuleString).getEventTypes();
            //check that all events have an evidence.attributes.transactionType set to delete

            for (com.apixio.model.event.EventType e: events){
                List<AttributeType> attrs =e.getEvidence().getAttributes().getAttribute();
                for (AttributeType at : attrs){
                    if(at.getName().equals("transactionType") && at.getValue().equals("DELETE")){
                        check +=1;
                    }
                }
            }
        }
        assertEquals(check,200);
    }


    private static List<Patient> initMultipleAPOsWithDelete(){

        List<Patient> rm = new ArrayList<>();
        Source s = new Source();
        s.setSourceType("CMS_KNOWN");
        Patient pat = new Patient();
        for (int i = 0; i<10;i++){
            //create a problem that is a RAPS claim
            Problem prob = new Problem();
            prob.setLastEditDateTime(DateTime.now());

            //start and end date
            DateTime start = new DateTime(2015, 1,1, 0, 0, DateTimeZone.UTC);
            DateTime end = new DateTime(2015, 1,1,0,0,DateTimeZone.UTC);
            prob.setStartDate(start);
            prob.setEndDate(end);
            //add the correct sourcetype

            //set the source uuid on the problem
            prob.setSourceId(s.getInternalUUID());
            //add the source to the patient's sources
            pat.addSource(s);

            //now add the metadata fields to identify this RA claim as a MAO claim
            prob.setMetaTag("CLAIM_TYPE", "MAO_004_ENCOUNTER_CLAIM");
            //now add an original id for this problem with the appropiate assigne authority
            ExternalID ei = new ExternalID();
            ei.setId("ABC123");
            ei.setAssignAuthority("MAO_004_PARSER");
            prob.setOriginalId(ei);

            //add an encounter ICN
            prob.setMetaTag("ENCOUNTER_ICN", "123456");
            //add an encounter claim type
            prob.setMetaTag("ENCOUNTER_CLAIM_TYPE", "I");

            //add a processing date
            prob.setMetaTag("PROCESSING_DATE", "2015-01-01");
            //add submission date
            prob.setMetaTag("PLAN_SUBMISSION_DATE","2015-01-02");
            //add delete flag
            prob.setMetaTag("ADD_OR_DELETE_FLAG", "D");
            //add an encounter type switch
            prob.setMetaTag("ENCOUNTER_TYPE_SWITCH", "1");
            //create a random string
            String randstr = UUID.randomUUID().toString();
            //add a DX
            ClinicalCode c1 = ConverterTestUtils.createCC(randstr, "ICD9", "ICD9-OID", "V1", randstr);
            prob.setCode(c1);
            pat.addProblem(prob);
            rm.add(pat);
        }
        return rm;
    }

    private static Patient initSinglePatientWithDelete(){
        singlePatientWithDelete = new Patient();

        //create a problem that is a RAPS claim
        Problem prob = new Problem();
        prob.setLastEditDateTime(DateTime.now());

        //start and end date

        DateTime start = new DateTime(2015, 1,1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2015, 1,1,0,0,DateTimeZone.UTC);
        prob.setStartDate(start);
        prob.setEndDate(end);
        //add the correct sourcetype
        Source s = new Source();
        s.setSourceType("CMS_KNOWN");
        //set the source uuid on the problem
        prob.setSourceId(s.getInternalUUID());
        //add the source to the patient's sources
        singlePatientWithDelete.addSource(s);

        //now add the metadata fields to identify this RA claim as a MAO claim
        prob.setMetaTag("CLAIM_TYPE", "MAO_004_ENCOUNTER_CLAIM");
        //now add an original id for this problem with the appropiate assigne authority
        ExternalID ei = new ExternalID();
        ei.setId("ABC123");
        ei.setAssignAuthority("MAO_004_PARSER");
        prob.setOriginalId(ei);

        //add an encounter ICN
        prob.setMetaTag("ENCOUNTER_ICN", "123456");
        //add an encounter claim type
        prob.setMetaTag("ENCOUNTER_CLAIM_TYPE", "I");

        //add a processing date
        prob.setMetaTag("PROCESSING_DATE", "2015-01-01");
        //add submission date
        prob.setMetaTag("PLAN_SUBMISSION_DATE","2015-01-02");
        //add delete flag
        prob.setMetaTag("ADD_OR_DELETE_FLAG", "D");
        //add an encounter type switch
        prob.setMetaTag("ENCOUNTER_TYPE_SWITCH", "1");

        //add a DX
        ClinicalCode c1 = ConverterTestUtils.createCC("25000", "ICD9", "ICD9-OID", "V1", "Diabetes");
        prob.setCode(c1);
        singlePatientWithDelete.addProblem(prob);
        return singlePatientWithDelete;
    }






    private static Patient initBogusPatient(){
        bogusPatient = new Patient();
        return bogusPatient;
    }




}
