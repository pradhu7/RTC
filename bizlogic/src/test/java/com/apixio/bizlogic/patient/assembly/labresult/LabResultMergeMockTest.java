package com.apixio.bizlogic.patient.assembly.labresult;

import com.apixio.bizlogic.patient.assembly.merge.LabResultMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.LabResult;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import com.apixio.model.utility.PatientJSONParser;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by alarocca on 5/5/20.
 */
@RunWith(MockitoJUnitRunner.class)
public class LabResultMergeMockTest
{
    @InjectMocks
    LabResultMerge labResultMerge;

    static public class PatientBuilder
    {
        public DateTime creationDate;
        public EditType editType;
        public String externalID;
        public String assignAuthority;
        public ActorRole role;

        // Optional

        public UUID sourceId = UUID.randomUUID();
        public UUID parsingDetailsId = UUID.randomUUID();
        public UUID labResultUUID = UUID.randomUUID();
        public UUID sourceEncounterId = UUID.randomUUID();
        public UUID clinicalActorId = UUID.randomUUID();
        public ClinicalCode clinicalCode = mock(ClinicalCode.class);

        public PatientBuilder(DateTime creationDate, EditType editType, String externalID, String assignAuthority, ActorRole role)
        {
            this.creationDate = creationDate;
            this.editType = editType;
            this.externalID = externalID;
            this.assignAuthority = assignAuthority;
            this.role = role;
        }

        public Patient build() {
            //
            // Setup a patient
            //
            Patient patientUnderTest = mock(Patient.class);

            //
            // Configure Source and Expected interactions
            //
            Source source = mock(Source.class);
            when(source.getCreationDate()).thenReturn(creationDate);
            when(source.getInternalUUID()).thenReturn(sourceId);
            when(patientUnderTest.getSources()).thenReturn(Collections.singletonList(source));

            ParsingDetail parsingDetail = mock(ParsingDetail.class);
            when(parsingDetail.getParsingDetailsId()).thenReturn(parsingDetailsId);

            //
            // Setup the External ID
            //
            ExternalID externalId = mock(ExternalID.class);
            when(externalId.getId()).thenReturn(externalID);
            when(externalId.getAssignAuthority()).thenReturn(assignAuthority);

            //
            // Setup the clinical Actor
            //
            ClinicalActor clinicalActor = mock(ClinicalActor.class);
            when(clinicalActor.getClinicalActorId()).thenReturn(clinicalActorId);
            when(clinicalActor.getEditType()).thenReturn(editType);
            when(clinicalActor.getParsingDetailsId()).thenReturn(parsingDetailsId);
            when(clinicalActor.getSourceId()).thenReturn(sourceId);
            when(clinicalActor.getPrimaryId()).thenReturn(externalId);
            when(clinicalActor.getRole()).thenReturn(role);

            LabResult labResult = mock(LabResult.class);
            when(labResult.getEditType()).thenReturn(editType);
            when(labResult.getInternalUUID()).thenReturn(labResultUUID);
            when(labResult.getSampleDate()).thenReturn(new DateTime(1000));
            when(labResult.getPrimaryClinicalActorId()).thenReturn(clinicalActorId);
            when(labResult.getSupplementaryClinicalActorIds()).thenReturn(Collections.<UUID>emptyList());
            when(labResult.getSourceEncounter()).thenReturn(sourceEncounterId);
            when(labResult.getCode()).thenReturn(clinicalCode);
            when(labResult.getCodeTranslations()).thenReturn(Collections.<ClinicalCode>emptyList());
            when(labResult.getMetadata()).thenReturn(Collections.<String, String>emptyMap());
            when(labResult.getSourceId()).thenReturn(sourceId);
            when(labResult.getParsingDetailsId()).thenReturn(parsingDetailsId);


            //
            // Setup Encounter
            //
            Encounter encounter = mock(Encounter.class);
            when(encounter.getEncounterId()).thenReturn(sourceEncounterId);

            // Setup return for source and parsing details by id interactions
            when(patientUnderTest.getSourceById(labResult.getSourceId())).thenReturn(source);
            when(patientUnderTest.getParsingDetailById(labResult.getParsingDetailsId())).thenReturn(parsingDetail);
            when(patientUnderTest.getClinicalActorById(labResult.getPrimaryClinicalActorId())).thenReturn(clinicalActor);
            when(patientUnderTest.getEncounterById(labResult.getSourceEncounter())).thenReturn(encounter);

            //
            // Setup Clinical Actors...
            //
            List<ClinicalActor> clinicalActorList1 = new ArrayList<>();
            clinicalActorList1.add(clinicalActor);

            //
            // Setup Problem
            //
            List<LabResult> labResultList = new ArrayList<>();
            labResultList.add(labResult);

            when(patientUnderTest.getClinicalActors()).thenReturn(clinicalActorList1);
            when(patientUnderTest.getLabs()).thenReturn(labResultList);

            return patientUnderTest;
        }
    }

    /**
     * TEST CASE:
     *
     * Patient1 earlier than Patient2, Active, different externalId and Assign Authority
     *
     * Expected: 2 clinical Actors, 2 sources, and 2 Parsing details...
     *
     */
    @Test
    public void testPatients1()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        labResultMerge.mergeLabResult(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<LabResult> labResultList = Lists.newArrayList(mergedPatient.getLabs());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(3, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(labResultList.get(0).getSourceId()));
        Assert.assertEquals(sources.get(2), apo1.getSourceById(labResultList.get(1).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(2, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, labResultList);
    }

    /**
     *  Two patients, one active - one deleted.
     */
    @Test
    public void testPatients2()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.DELETE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        labResultMerge.mergeLabResult(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<LabResult> labResultList = Lists.newArrayList(mergedPatient.getLabs());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo1.getSourceById(labResultList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, labResultList);
        verifyClinicalActorAndEncounter(mergedPatient, labResultList);
    }

    @Test
    public void testDuplicateMerge() throws IOException {
        String apoLabResult1 = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"labs\": [{\"code\": {\"code\": \"5902-2\", \"codingSystem\": \"LOINC\", \"codingSystemOID\": \"2.16.840.1.113883.6.1\", \"displayName\": \"5902-2\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"49ffa342-ca00-4c5c-b342-892388463787\", \"labName\": \"5902-2\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"metadata\": {\"CPT_CODE\": \"36415\"}, \"originalId\": {\"assignAuthority\": \"123-LabResult\"}, \"parsingDetailsId\": \"02605460-2e6d-45c7-995b-3708d2bc183d\", \"sampleDate\": \"2017-01-01T00:00:00.000Z\", \"sourceId\": \"9f969a99-4784-454f-a801-ea110b2c1b3f\", \"value\": \"\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"4e0d9d85-248d-4137-92e0-eb5bdb7aac4e\", \"sourceFileHash\": \"58b5bb67b12c1b53f80ddb9c8a9fc1f6ade9ecb4\", \"sourceUploadBatch\": \"472_AxmPrescriptions14\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2017-04-07T00:38:46.193Z\", \"parsingDetailsId\": \"02605460-2e6d-45c7-995b-3708d2bc183d\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2016-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"9f969a99-4784-454f-a801-ea110b2c1b3f\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"parsingDetailsId\": \"02605460-2e6d-45c7-995b-3708d2bc183d\", \"sourceId\": \"9f969a99-4784-454f-a801-ea110b2c1b3f\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";
        Patient apo1 = new PatientJSONParser().parsePatientData(apoLabResult1);

        // Differs only by parsing details and source id
        String apoLabResult2 = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"labs\": [{\"code\": {\"code\": \"5902-2\", \"codingSystem\": \"LOINC\", \"codingSystemOID\": \"2.16.840.1.113883.6.1\", \"displayName\": \"5902-2\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"49ffa342-ca00-4c5c-b342-892388463787\", \"labName\": \"5902-2\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"metadata\": {\"CPT_CODE\": \"36415\"}, \"originalId\": {\"assignAuthority\": \"123-LabResult\"}, \"parsingDetailsId\": \"01605460-2e6d-45c7-995b-3708d2bc183d\", \"sampleDate\": \"2017-01-01T00:00:00.000Z\", \"sourceId\": \"8f969a99-4784-454f-a801-ea110b2c1b3f\", \"value\": \"\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"4e0d9d85-248d-4137-92e0-eb5bdb7aac4e\", \"sourceFileHash\": \"58b5bb67b12c1b53f80ddb9c8a9fc1f6ade9ecb4\", \"sourceUploadBatch\": \"472_AxmPrescriptions14\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2017-04-07T00:38:46.193Z\", \"parsingDetailsId\": \"01605460-2e6d-45c7-995b-3708d2bc183d\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2016-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"8f969a99-4784-454f-a801-ea110b2c1b3f\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"parsingDetailsId\": \"01605460-2e6d-45c7-995b-3708d2bc183d\", \"sourceId\": \"8f969a99-4784-454f-a801-ea110b2c1b3f\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";
        Patient apo2 = new PatientJSONParser().parsePatientData(apoLabResult2);

        String apoLabResult3 = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"labs\": [{\"code\": {\"code\": \"5902-1\", \"codingSystem\": \"LOINC\", \"codingSystemOID\": \"2.16.840.1.113883.6.1\", \"displayName\": \"5902-1\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"49ffa342-ca00-4c5c-b342-892388463787\", \"labName\": \"5902-1\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"metadata\": {\"CPT_CODE\": \"36414\"}, \"originalId\": {\"assignAuthority\": \"456-LabResult\"}, \"parsingDetailsId\": \"82605460-2e6d-45c7-995b-3708d2bc183d\", \"sampleDate\": \"2018-01-01T00:00:00.000Z\", \"sourceId\": \"af969a99-4784-454f-a801-ea110b2c1b3f\", \"value\": \"\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"4e0d9d85-248d-4137-92e0-eb5bdb7aac4e\", \"sourceFileHash\": \"18b5bb67b12c1b53f80ddb9c8a9fc1f6ade9ecb4\", \"sourceUploadBatch\": \"472_AxmPrescriptions14\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2017-04-07T00:38:46.193Z\", \"parsingDetailsId\": \"82605460-2e6d-45c7-995b-3708d2bc183d\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2016-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"af969a99-4784-454f-a801-ea110b2c1b3f\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"parsingDetailsId\": \"82605460-2e6d-45c7-995b-3708d2bc183d\", \"sourceId\": \"af969a99-4784-454f-a801-ea110b2c1b3f\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";
        Patient apo3 = new PatientJSONParser().parsePatientData(apoLabResult3);

        PatientSet patientSet = new PatientSet(apo1, apo2);
        labResultMerge.mergeLabResult(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;
        List<LabResult> labResultList = Lists.newArrayList(mergedPatient.getLabs());
        Assert.assertEquals(1, labResultList.size());
//        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
//        Assert.assertEquals(1, clinicalActorList.size());


        PatientSet patientSet2 = new PatientSet(mergedPatient, apo3);
        labResultMerge.mergeLabResult(patientSet2);

        Patient mergedPatient2 = patientSet2.mergedPatient;

        labResultList = Lists.newArrayList(mergedPatient2.getLabs());
        Assert.assertEquals(2, labResultList.size());
//        clinicalActorList = Lists.newArrayList(mergedPatient2.getClinicalActors());
//        Assert.assertEquals(1, clinicalActorList.size());

    }

    @Test
    public void testTwoSameProblems_MIXED()
    {
        UUID sourceEncounterId = UUID.randomUUID();
        UUID clinicalActorId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID parsingDetailsId = UUID.randomUUID();

        ClinicalCode clinicalCode = new ClinicalCode();
        clinicalCode.setCode("abc");
        clinicalCode.setCodingSystem("123");
        clinicalCode.setCodingSystemOID("oid");
        clinicalCode.setCodingSystemVersions("versions");
        clinicalCode.setDisplayName("display name");


        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        patientBuilderForApo1.sourceEncounterId = sourceEncounterId;
        patientBuilderForApo1.clinicalActorId = clinicalActorId;
        patientBuilderForApo1.sourceId = sourceId;
        patientBuilderForApo1.parsingDetailsId = parsingDetailsId;
        patientBuilderForApo1.clinicalCode = clinicalCode;
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        patientBuilderForApo2.sourceEncounterId = sourceEncounterId;
        patientBuilderForApo2.clinicalActorId = clinicalActorId;
        patientBuilderForApo2.sourceId = sourceId;
        patientBuilderForApo2.parsingDetailsId = parsingDetailsId;
        patientBuilderForApo2.clinicalCode = clinicalCode;
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        labResultMerge.mergeLabResult(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<LabResult> labResultList = Lists.newArrayList(mergedPatient.getLabs());

        Assert.assertEquals(1, labResultList.size());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(labResultList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, labResultList);
        verifyClinicalActorAndEncounter(mergedPatient, labResultList);
    }

    @Test
    public void testMultipleClinicalActorMerge()
    {
        List<Patient> listOfPatientsToMerge = new ArrayList<>();

        int numberOfUniqueActive = 5;
        int numberOfUniqueInActive = 2;
        int numberOfDuplicatesOfEachUnique = 8;

        int dateInstant = 0;

        //
        // Step #1: Setup Active patients
        //
        for(int n=0; n<numberOfUniqueActive; n++)
        {
            UUID sourceEncounterId = UUID.randomUUID();
            UUID clinicalActorId = UUID.randomUUID();
            UUID sourceId = UUID.randomUUID();
            UUID parsingDetailsId = UUID.randomUUID();

            ClinicalCode clinicalCode = new ClinicalCode();
            clinicalCode.setCode("abc");
            clinicalCode.setCodingSystem("123");
            clinicalCode.setCodingSystemOID("oid");
            clinicalCode.setCodingSystemVersions("versions");
            clinicalCode.setDisplayName("display name");


            for (int i = 0; i < numberOfDuplicatesOfEachUnique; i++)
            {
                DateTime creationDate = new DateTime(dateInstant++);

                PatientBuilder apoBuilder = new PatientBuilder(creationDate,
                        EditType.ACTIVE,
                        "externalId1" + n,
                        "aa1" + n,
                        ActorRole.ATTENDING_PHYSICIAN);

                apoBuilder.sourceEncounterId = sourceEncounterId;
                apoBuilder.clinicalActorId = clinicalActorId;
                apoBuilder.sourceId = sourceId;
                apoBuilder.parsingDetailsId = parsingDetailsId;
                apoBuilder.clinicalCode = clinicalCode;

                listOfPatientsToMerge.add(apoBuilder.build());
            }
        }

        // Setup Inactive patients
        for(int n=0; n<numberOfUniqueInActive; n++)
        {
            UUID sourceEncounterId = UUID.randomUUID();
            UUID clinicalActorId = UUID.randomUUID();
            UUID sourceId = UUID.randomUUID();
            UUID parsingDetailsId = UUID.randomUUID();

            ClinicalCode clinicalCode = new ClinicalCode();
            clinicalCode.setCode("abc");
            clinicalCode.setCodingSystem("123");
            clinicalCode.setCodingSystemOID("oid");
            clinicalCode.setCodingSystemVersions("versions");
            clinicalCode.setDisplayName("display name");

            for (int i = 0; i < numberOfDuplicatesOfEachUnique; i++)
            {
                DateTime creationDate = new DateTime(dateInstant++);

                PatientBuilder apoBuilder = new PatientBuilder(creationDate,
                        EditType.DELETE,
                        "externalId1" + n,
                        "aa1" + n,
                        ActorRole.ATTENDING_PHYSICIAN);
                apoBuilder.sourceEncounterId = sourceEncounterId;
                apoBuilder.clinicalActorId = clinicalActorId;
                apoBuilder.sourceId = sourceId;
                apoBuilder.parsingDetailsId = parsingDetailsId;
                apoBuilder.clinicalCode = clinicalCode;

                listOfPatientsToMerge.add(apoBuilder.build());
            }
        }

        Assert.assertEquals((numberOfUniqueActive*numberOfDuplicatesOfEachUnique) +
                (numberOfUniqueInActive*numberOfDuplicatesOfEachUnique), listOfPatientsToMerge.size());

        //
        // Step #2 - Do the actual merge
        //
        Patient mergedPatient = null;
        for(Patient patient: listOfPatientsToMerge)
        {
            if(mergedPatient == null)
            {
                mergedPatient = patient;
                continue;
            }

            PatientSet patientSet = new PatientSet(mergedPatient, patient);
            labResultMerge.mergeLabResult(patientSet);

            mergedPatient = patientSet.mergedPatient;
        }

        //
        // Step #3 - Verify
        //
        List<LabResult> labResultList = Lists.newArrayList(mergedPatient.getLabs());
        Assert.assertEquals(numberOfUniqueActive, labResultList.size());

        verifySourcesAndParsingDetails(mergedPatient, labResultList);
    }

    @Test
    public void testExtract()
    {
        String json = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"labs\": [{\"code\": {\"code\": \"5902-1\", \"codingSystem\": \"LOINC\", \"codingSystemOID\": \"2.16.840.1.113883.6.1\", \"displayName\": \"5902-1\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"49ffa342-ca00-4c5c-b342-892388463787\", \"labName\": \"5902-1\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"metadata\": {\"CPT_CODE\": \"36414\"}, \"originalId\": {\"assignAuthority\": \"456-LabResult\"}, \"parsingDetailsId\": \"82605460-2e6d-45c7-995b-3708d2bc183d\", \"sampleDate\": \"2018-01-01T00:00:00.000Z\", \"sourceId\": \"af969a99-4784-454f-a801-ea110b2c1b3f\", \"value\": \"\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"4e0d9d85-248d-4137-92e0-eb5bdb7aac4e\", \"sourceFileHash\": \"18b5bb67b12c1b53f80ddb9c8a9fc1f6ade9ecb4\", \"sourceUploadBatch\": \"472_AxmPrescriptions14\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2017-04-07T00:38:46.193Z\", \"parsingDetailsId\": \"82605460-2e6d-45c7-995b-3708d2bc183d\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2016-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"af969a99-4784-454f-a801-ea110b2c1b3f\", \"lastEditDateTime\": \"2017-04-07T00:38:46.199Z\", \"parsingDetailsId\": \"82605460-2e6d-45c7-995b-3708d2bc183d\", \"sourceId\": \"af969a99-4784-454f-a801-ea110b2c1b3f\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";

        LabResultAssembler labResultAssembler = new LabResultAssembler();
        Patient separatedPatient = null;
        try
        {
            Patient originalPatient = new PatientJSONParser().parsePatientData(json);

            //
            // Set up an encounter...
            //
            UUID encounterId = UUID.randomUUID();
            Encounter encounter = new Encounter();
            encounter.setEncounterId(encounterId);
            originalPatient.addEncounter(encounter);

            for(LabResult labResult : originalPatient.getLabs()) {
                labResult.setSourceEncounter(encounterId);
            }

            separatedPatient = labResultAssembler.separate(originalPatient).get(0).getPart();
        } catch (IOException e)
        {
            fail(e.getMessage());
        }

        verifySourcesAndParsingDetails(separatedPatient, Lists.newArrayList(separatedPatient.getLabs()));
        verifyClinicalActorAndEncounter(separatedPatient, Lists.newArrayList(separatedPatient.getLabs()));
    }

    protected void verifySourcesAndParsingDetails(Patient mergedPatient, List<LabResult> labResultList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(LabResult labResult: labResultList)
        {
            UUID referencedSource = labResult.getSourceId();
            Assert.assertNotNull(mergedPatient.getSourceById(referencedSource));

            UUID referencedParsingDetail = labResult.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }

    protected void verifyClinicalActorAndEncounter(Patient mergedPatient, List<LabResult> labResultList)
    {
        //
        // All sources, and parsing details pointed to by the Problem are present
        //
        for(LabResult labResult: labResultList)
        {
            UUID referencedClinicalActor = labResult.getPrimaryClinicalActorId();
            if (referencedClinicalActor != null) {
                Assert.assertNotNull(mergedPatient.getClinicalActorById(referencedClinicalActor));
            }

            List<UUID> supplementaryClinicalActors = labResult.getSupplementaryClinicalActorIds();
            for(UUID clinicalActor : supplementaryClinicalActors)
            {
                Assert.assertNotNull(mergedPatient.getClinicalActorById(clinicalActor));
            }

            UUID referencedEncounter = labResult.getSourceEncounter();
            if (referencedEncounter != null) {
                Assert.assertNotNull(mergedPatient.getEncounterById(referencedEncounter));
            }
        }
    }
}
