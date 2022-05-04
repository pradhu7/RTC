package com.apixio.bizlogic.patient.assembly.all;

import com.apixio.bizlogic.BizLogicTestCategories;
import com.apixio.bizlogic.patient.assembly.SummaryTestUtils;
import com.apixio.bizlogic.patient.assembly.coverage.CoverageAssemblerTest;
import com.apixio.model.assembly.Assembler;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.*;
import com.apixio.model.utility.PatientJSONParser;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.*;

import static org.junit.Assert.fail;

/**
 * Created by dyee on 5/9/17.
 */
@Category(BizLogicTestCategories.IntegrationTest.class)
public class AllAssemblyTest
{
    private static final String HEALTH_PLAN_META_KEY = "HEALTH_PLAN";

    AllAssembler allAssembler;
    PatientJSONParser parser;
    Map<Encounter, Encounter> encountersToMatch = new HashMap<>();
    Map<UUID, Boolean> seenMap = new HashMap<>();

    Document document1 = null;
    @Before
    public void init() {
        allAssembler = new AllAssembler();
        parser = new PatientJSONParser();
    }

    protected Patient generateTestPatientWithDuplicateClinicalActors(UUID uuid)
    {
        Patient p = new Patient();
        p.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(10)));

        List<ClinicalActor> clinicalActors = SummaryTestUtils.createLogicallyDuplicateActors(uuid,5);

        for(ClinicalActor actor : clinicalActors) {
            Problem problem = Problem.newBuilder().setProblemName("test problem").setStartDate(new DateTime(0)).setEndDate(new DateTime()).build();
            problem.setPrimaryClinicalActorId(actor.getClinicalActorId());
            p.addProblem(problem);

            Procedure procedure = Procedure.newBuilder().setProcedureName("procedureName").setCode("code").setCodingSystem("codiing system").setInterpretation("iii").setPerformedOn(new DateTime(0)).build();
            procedure.setOriginalId(SummaryTestUtils.createExternalId(UUID.randomUUID().toString(), "claim id")); // so each procedure is unique even though clinAct is duplicate
            procedure.setPrimaryClinicalActorId(actor.getClinicalActorId());
            p.addProcedure(procedure);

            p.addClinicalActor(actor);
        }

        return p;
    }

    protected Patient generateTestPatient() {
        Patient p = new Patient();
        p.addSource(CoverageAssemblerTest.createSource("2016-01-01", "2015-01-01", "2015-12-31"));

        // Set Demographics
        Demographics d1 = SummaryTestUtils.createDemographics("First", "", "Last", "01/01/1980", "Female");
        Demographics d2 = SummaryTestUtils.createDemographics("First", "Middle", "Last", "01/01/1980", "Female");
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

        //Set Encounters
        List<Encounter> encounters = SummaryTestUtils.createEncounters(5);
        for (Encounter encounter : encounters)
        {
            p.addEncounter(encounter);
            encountersToMatch.put(encounter, encounter);
        }

        //Set document
        document1 = new Document();
        document1.setDocumentDate(DateTime.now().minusYears(10));
        document1.setDocumentTitle("Test Title");
        document1.setStringContent("hello there");

        p.addDocument(document1);

        //Set coverage
        p.setPrimaryExternalID(CoverageAssemblerTest.createExternalId("002-coverage-summary", "PAT_AA"));

        Source s1 = CoverageAssemblerTest.createSource("2016-01-01", "2015-01-01", "2015-12-31");
        p.addSource(s1);
        Coverage patientCoverage1 = CoverageAssemblerTest.createCoverage(s1, "WellCare HMO", "001^^PAT_AA", "2015-01-01", "2015-05-31");
        p.addCoverage(patientCoverage1);

        Source s2 = CoverageAssemblerTest.createSource("2016-01-01", "2015-01-01", "2015-12-31");
        s2.setMetaTag(HEALTH_PLAN_META_KEY, "CMS");
        p.addSource(s2);
        Coverage patientCoverage2 = CoverageAssemblerTest.createCoverage(s2, "CMS", "001^^PAT_AA", "2015-06-01", "2015-12-31");
        p.addCoverage(patientCoverage2);

        //Set clinical actor
        List<ClinicalActor> clinicalActors = SummaryTestUtils.createClinicalActors(5);

        for(ClinicalActor actor : clinicalActors) {
            p.addClinicalActor(actor);
        }

        return p;
    }

    @Test
    public void testSeparate()
    {
        Patient patient1 = generateTestPatient();

        List<Part<Patient>> partList = allAssembler.separate(patient1);

        Assert.assertEquals(1, partList.size());

        for (Part<Patient> patientPart : partList)
        {
            Assert.assertEquals("all", patientPart.getCategory());
            Assert.assertEquals(patientPart.getID(), patientPart.getPart().getDocuments().iterator().next().getInternalUUID().toString());

            //Test encounters
            Iterator<Encounter> encounterIterable = patientPart.getPart().getEncounters().iterator();
            while(encounterIterable.hasNext())
            {
                encountersToMatch.remove(encounterIterable.next());
            }
            Assert.assertTrue(encountersToMatch.isEmpty());

            //Test documents
            Iterator<Document> documentIterator = partList.get(0).getPart().getDocuments().iterator();
            org.junit.Assert.assertEquals(document1.getDocumentTitle(), documentIterator.next().getDocumentTitle());
            org.junit.Assert.assertFalse(documentIterator.hasNext());


            //Test clinical actors
            for(ClinicalActor ca : patientPart.getPart().getClinicalActors()) {
                seenMap.put(ca.getClinicalActorId(), Boolean.FALSE);
            }

            // Test various conditions
            for(ClinicalActor clinicalActor : patientPart.getPart().getClinicalActors()) {
                patientPart.getPart().getClinicalActorById(clinicalActor.getClinicalActorId());

                //
                // Mark the seen list, and make sure we don't see things twice.
                //
                Boolean seen = seenMap.get(clinicalActor.getClinicalActorId());
                if(seen == Boolean.FALSE) {
                    seenMap.put(clinicalActor.getClinicalActorId(), Boolean.TRUE);
                } else {
                    fail("seen more than once");
                }
            }

            // We've seen every clinical actor from the orginal patient in all of the parts/
            for(Boolean test: seenMap.values()) {
                Assert.assertTrue(test);
            }
        }
    }

    @Test
    public void testMerge()
    {
        Patient patient1 = generateTestPatient();
        Patient patient2 = generateTestPatient();

        //
        // @TODO: add all the checks here.. skipping for now, since we test individually..
        //

        //Verify Encounters merged correctly
        Map<Encounter, Encounter> encountersToMatch = new HashMap<>();
        for (Encounter encounter : patient1.getEncounters())
        {
            encountersToMatch.put(encounter, encounter);
        }

        for (Encounter encounter : patient2.getEncounters())
        {
            encountersToMatch.put(encounter, encounter);
        }


        PartEnvelope<Patient> mergedPatient = allAssembler.merge(new Assembler.MergeInfo(Assembler.MergeScope.PARTS_AND_AGGREGATE, true), new PartEnvelope<>(patient1, 0L), new PartEnvelope<>(patient2, 0L));

        int count = 0;
        for(Encounter encounter : mergedPatient.part.getEncounters()) {
            count++;
        }

        Assert.assertEquals(10, count);

    }

    @Test
    public void testMergeDuplicateClinicalActors()
    {
        UUID uuid = UUID.randomUUID();
        Patient patient1 = generateTestPatientWithDuplicateClinicalActors(uuid);
        Patient patient2 = generateTestPatientWithDuplicateClinicalActors(uuid);

        PartEnvelope<Patient> mergedPatient = allAssembler.merge(new Assembler.MergeInfo(Assembler.MergeScope.PARTS_AND_AGGREGATE, true), new PartEnvelope<>(patient1, 0L), new PartEnvelope<>(patient2, 0L));

        Set<UUID> primaryClinicalActorOnProcedures = new HashSet<>();
        Set<UUID> primaryClinicalActorOnProblem = new HashSet<>();

        int count = 0;
        for(Procedure procedure : mergedPatient.part.getProcedures()) {
            primaryClinicalActorOnProcedures.add(procedure.getPrimaryClinicalActorId());
            count++;
        }

        int problemCount = 0;
        for(Problem problem : mergedPatient.part.getProblems()) {
            primaryClinicalActorOnProblem.add(problem.getPrimaryClinicalActorId());
            problemCount++;
        }

        Assert.assertEquals(10, count);
        Assert.assertEquals(10, problemCount);

        Assert.assertEquals(1, primaryClinicalActorOnProcedures.size());
        Assert.assertEquals(1, primaryClinicalActorOnProblem.size());


        boolean success = false;
        for(UUID id : primaryClinicalActorOnProcedures) {
            for(ClinicalActor clinicalActor: mergedPatient.part.getClinicalActors()) {
                if(clinicalActor.getInternalUUID().equals(id)) {
                    success = true;
                    break;
                }
            }
        }
        Assert.assertTrue("clinical actors referenced in procedures, not found in merged patient", success);
    }

    @Test
    public void testMergeDuplicateClinicalActors2()
    {
        Patient patient1 = generateTestPatientWithDuplicateClinicalActors(UUID.randomUUID());
        Patient patient2 = generateTestPatientWithDuplicateClinicalActors(UUID.randomUUID());

        PartEnvelope<Patient> mergedPatient = allAssembler.merge(new Assembler.MergeInfo(Assembler.MergeScope.PARTS_AND_AGGREGATE, true), new PartEnvelope<Patient>(patient1, 0L), new PartEnvelope<Patient>(patient2, 0L));

        Set<UUID> primaryClinicalActorOnProcedures = new HashSet<>();
        Set<UUID> primaryClinicalActorOnProblem = new HashSet<>();

        int count = 0;
        for(Procedure procedure : mergedPatient.part.getProcedures()) {
            primaryClinicalActorOnProcedures.add(procedure.getPrimaryClinicalActorId());
            count++;
        }

        int problemCount = 0;
        for(Problem problem : mergedPatient.part.getProblems()) {
            primaryClinicalActorOnProblem.add(problem.getPrimaryClinicalActorId());
            problemCount++;
        }

        Assert.assertEquals(10, count);
        Assert.assertEquals(10, problemCount);

        Assert.assertEquals(2, primaryClinicalActorOnProcedures.size());
        Assert.assertEquals(2, primaryClinicalActorOnProblem.size());

        boolean success = false;
        for(UUID uuid : primaryClinicalActorOnProcedures) {
            for(ClinicalActor clinicalActor: mergedPatient.part.getClinicalActors()) {
                if(clinicalActor.getInternalUUID().equals(uuid)) {
                    success = true;
                    break;
                }
            }
        }
        Assert.assertTrue("clinical actors referenced in procedures, not found in merged patient", success);
    }

    @Test
    public void testMerge2PatientsDiffFamilyNames()
    {
        // Create two new patients and attach a demographic to each.
        Patient patient1 = new Patient();
        Demographics d1 = SummaryTestUtils.createDemographics(
                "Elizabeth", "", "Jones", "09/04/1982", "Female");
        patient1.setPrimaryDemographics(d1);
        Patient patient2 = new Patient();
        Demographics d2 = SummaryTestUtils.createDemographics(
                "Elizabeth", "", "Randle", "09/04/1982", "Female");
        patient2.setPrimaryDemographics(d2);

        PartEnvelope<Patient> mergedPatient = allAssembler.merge(
                new Assembler.MergeInfo(Assembler.MergeScope.PARTS_AND_AGGREGATE, true),
                new PartEnvelope<>(patient1, 0L),
                new PartEnvelope<>(patient2, 60L));

        // Verify merged patient has the updated family name.
        List<String> expected = Collections.singletonList("Randle");
        List<String> newFamilyNames = mergedPatient.part.getPrimaryDemographics().getName().getFamilyNames();
        Assert.assertEquals(expected, newFamilyNames);
    }

}
