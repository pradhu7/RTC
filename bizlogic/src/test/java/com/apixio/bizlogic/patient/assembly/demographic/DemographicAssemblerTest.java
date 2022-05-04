package com.apixio.bizlogic.patient.assembly.demographic;

import com.apixio.model.assembly.Part;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import com.apixio.model.utility.PatientJSONParser;
import com.apixio.bizlogic.patient.assembly.SummaryTestUtils;
import com.apixio.security.Config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by dyee on 4/27/17.
 */
public class DemographicAssemblerTest
{
    DemographicAssembler demographicAssembler;
    PatientJSONParser parser;

    @Before
    public void init() {
        System.setProperty(Config.SYSPROP_PREFIX + Config.ConfigKey.CONFIG_USE_ENCRYPTION.getKey(), Boolean.FALSE.toString());

        demographicAssembler = new DemographicAssembler();
        parser = new PatientJSONParser();
    }

    protected Patient generateTestPatient() {
        Patient p = new Patient();
        ParsingDetail parsingDetail = new ParsingDetail();

        p.addParsingDetail(parsingDetail);
        p.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(10)));

        Demographics d1 = SummaryTestUtils.createDemographics("First", "", "Last", "01/01/1980", "Female");
        Demographics d2 = SummaryTestUtils.createDemographics("First", "Middle", "Last", "01/01/1980", "Female");

        d1.setParsingDetailsId(parsingDetail.getParsingDetailsId());
        d2.setParsingDetailsId(parsingDetail.getParsingDetailsId());

        p.setPrimaryDemographics(d1);
        p.addAlternateDemographics(d2);

        ExternalID e1 = SummaryTestUtils.createExternalId("ID1", "PatientID1");
        ExternalID e2 = SummaryTestUtils.createExternalId("ID2", "PatientID2");
        p.setPrimaryExternalID(e1);
        p.addExternalId(e2);

        ContactDetails cd1 = SummaryTestUtils.createContactDetails("111 Test St","San Mateo", "CA", "11111", "test@apixio.com", "111-111-1111");
        ContactDetails cd2 = SummaryTestUtils.createContactDetails("222 Test St","San Mateo", "CA", "22222", "test@apixio.com", "222-222-2222");
        p.setPrimaryContactDetails(cd1);
        p.addAlternateContactDetails(cd2);

        return p;
    }

    @Test
    public void testSeparate() throws JsonProcessingException
    {
        Patient p = generateTestPatient();

        List<Part<Patient>> separatedPatientList =  demographicAssembler.separate(p);
        Assert.assertEquals(1, separatedPatientList.size());

        //ignore sources, for the purposes of this diff.
        p.setSources(new ArrayList<Source>());

        Patient separatedPatient = separatedPatientList.get(0).getPart();

        separatedPatient.setSources(new ArrayList<Source>());

        List<Demographics> demographicsList = Lists.newArrayList(separatedPatient.getAlternateDemographics());

        // make sure we add primary demographics to this test as well.
        demographicsList.add(separatedPatient.getPrimaryDemographics());

        verifyParsingDetails(separatedPatient, demographicsList);
    }

    @Test
    public void testMerge() throws JsonProcessingException
    {
        Patient seperatedPatient1;
        Patient seperatedPatient2;

        //------ Patient #1 - at time T1 ------------
        Patient p = generateTestPatient();

        List<Part<Patient>> seperatedPatientList =  demographicAssembler.separate(p);
        Assert.assertEquals(1, seperatedPatientList.size());

        seperatedPatient1 = seperatedPatientList.get(0).getPart();

        //------ Patient #1 - at time T2 ------------

        // Patient 2, changed first name
        Patient p2 = generateTestPatient();
        p2.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(6)));
        p2.setPrimaryDemographics(SummaryTestUtils.createDemographics("Tony", "", "LaRocca", "09/04/1982", "Male"));
        p2.addAlternateDemographics(SummaryTestUtils.createDemographics("David", "T", "Yee", "01/01/1901", "Male"));
        seperatedPatientList =  demographicAssembler.separate(p2);
        Assert.assertEquals(1, seperatedPatientList.size());

        seperatedPatient2 = seperatedPatientList.get(0).getPart();

        //------ START Merge ---------

        Patient mergedPatient = demographicAssembler.merge(null, new PartEnvelope<Patient>(seperatedPatient1, 0L), new PartEnvelope<Patient>(seperatedPatient2, 0L)).part;
        Assert.assertEquals(p2.getPrimaryDemographics(), mergedPatient.getPrimaryDemographics());

        Assert.assertTrue(checkForDemographic(seperatedPatient1.getPrimaryDemographics(), mergedPatient.getAlternateDemographics()));
        Assert.assertTrue(checkForDemographic(seperatedPatient2.getPrimaryDemographics(), mergedPatient.getAlternateDemographics()));

        for(Demographics demo : seperatedPatient1.getAlternateDemographics()) {
            Assert.assertTrue(checkForDemographic(demo, mergedPatient.getAlternateDemographics()));
        }

        for(Demographics demo : seperatedPatient2.getAlternateDemographics())
        {
            Assert.assertTrue(checkForDemographic(demo, mergedPatient.getAlternateDemographics()));
        }
    }

    @Test
    public void testMergeName() throws JsonProcessingException {
        Patient p1 = generateTestPatient();
        Patient p2 = generateTestPatient();
        p1.setPrimaryDemographics(SummaryTestUtils.createDemographics("Elizabeth", "", "Jones", "09/04/1982", "Female"));
        p2.setPrimaryDemographics(SummaryTestUtils.createDemographics("Elizabeth", "", "Randle", "09/04/1982", "Female"));

        Patient mergedPatient = demographicAssembler.merge(null, new PartEnvelope<Patient>(p1, 0L), new PartEnvelope<Patient>(p2, 60L)).part;

        List<String> expected = Collections.singletonList("Randle");
        List<String> newFamilyNames = mergedPatient.getPrimaryDemographics().getName().getFamilyNames();
        Assert.assertEquals(expected, newFamilyNames);

    }

    @Test
    public void testMergeReverseTimeOrder() throws JsonProcessingException
    {
        Patient seperatedPatient1;
        Patient seperatedPatient2;

        //------ Patient #1 - at time T1 ------------

        // Patient 2, changed first name
        Patient p2 = generateTestPatient();
        p2.setPrimaryDemographics(SummaryTestUtils.createDemographics("Tony", "", "LaRocca", "09/04/1982", "Male"));
        List<Part<Patient>> seperatedPatientList =  demographicAssembler.separate(p2);
        Assert.assertEquals(1, seperatedPatientList.size());

        seperatedPatient2 = seperatedPatientList.get(0).getPart();

        //------ Patient #1 - at time T2 ------------
        Patient p = generateTestPatient();
        p.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(6)));
        p.setPrimaryDemographics(SummaryTestUtils.createDemographics("David", "T", "Yee", "01/01/1985", "Male"));

        seperatedPatientList =  demographicAssembler.separate(p);
        Assert.assertEquals(1, seperatedPatientList.size());

        seperatedPatient1 = seperatedPatientList.get(0).getPart();


        //------ START Merge ---------

        Patient mergedPatient = demographicAssembler.merge(null, new PartEnvelope<Patient>(seperatedPatient1, 0L), new PartEnvelope<Patient>(seperatedPatient2, 0L)).part;
        Assert.assertEquals(p.getPrimaryDemographics(), mergedPatient.getPrimaryDemographics());

        Assert.assertTrue(checkForDemographic(seperatedPatient1.getPrimaryDemographics(), mergedPatient.getAlternateDemographics()));
        Assert.assertTrue(checkForDemographic(seperatedPatient2.getPrimaryDemographics(), mergedPatient.getAlternateDemographics()));

        for(Demographics demo : seperatedPatient1.getAlternateDemographics()) {
            Assert.assertTrue(checkForDemographic(demo, mergedPatient.getAlternateDemographics()));
        }

        for(Demographics demo : seperatedPatient2.getAlternateDemographics())
        {
            Assert.assertTrue(checkForDemographic(demo, mergedPatient.getAlternateDemographics()));
        }
    }


    protected boolean checkForDemographic(Demographics demo, Iterable<Demographics> demographicss) {
        boolean found = false;
        for(Demographics altDemo : demographicss) {
            if(demo.equals(altDemo)) {
                found = true;
                break;
            }
        }

        return found;
    }

    protected void verifyParsingDetails(Patient mergedPatient, List<Demographics> demoList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(Demographics demographics: demoList)
        {
            UUID referencedParsingDetail = demographics.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }

    @Test
    public void testSetupCompleteness() throws JsonProcessingException {
        // Patient 1
        Patient p1 = generateTestPatient();
        p1.addSource(SummaryTestUtils.createSource(DateTime.now().minusHours(6)));
        p1.setPrimaryDemographics(SummaryTestUtils.createDemographics("Jane", "", "Smith", "09/04/1982", "Female"));

        // Patient 2 - Changed Last Name
        Patient p2 = generateTestPatient();
        p2.addSource(SummaryTestUtils.createSource(DateTime.now()));
        p2.setPrimaryDemographics(SummaryTestUtils.createDemographics("Jane", "", "Doe", "09/04/1982", "Female"));

        // Merge
        Patient mergedPatient = demographicAssembler.merge(null, new PartEnvelope<>(p1, 0), new PartEnvelope<>(p2, 0L)).part;
        Assert.assertEquals(p2.getPrimaryDemographics(), mergedPatient.getPrimaryDemographics());
    }
}
