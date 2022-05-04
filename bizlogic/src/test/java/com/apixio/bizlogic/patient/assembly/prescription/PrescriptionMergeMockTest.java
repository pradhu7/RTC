package com.apixio.bizlogic.patient.assembly.prescription;

import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.PrescriptionMerge;

import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Prescription;
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
public class PrescriptionMergeMockTest
{
    @InjectMocks
    PrescriptionMerge prescriptionMerge;

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
        public UUID prescriptionUUID = UUID.randomUUID();
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

            Prescription prescription = mock(Prescription.class);
            when(prescription.getEditType()).thenReturn(editType);
            when(prescription.getInternalUUID()).thenReturn(prescriptionUUID);
            when(prescription.getEndDate()).thenReturn(new DateTime(1000));
            when(prescription.getPrimaryClinicalActorId()).thenReturn(clinicalActorId);
            when(prescription.getSupplementaryClinicalActorIds()).thenReturn(Collections.<UUID>emptyList());
            when(prescription.getSourceEncounter()).thenReturn(sourceEncounterId);
            when(prescription.getCode()).thenReturn(clinicalCode);
            when(prescription.getCodeTranslations()).thenReturn(Collections.<ClinicalCode>emptyList());
            when(prescription.getMetadata()).thenReturn(Collections.<String, String>emptyMap());
            when(prescription.getSourceId()).thenReturn(sourceId);
            when(prescription.getParsingDetailsId()).thenReturn(parsingDetailsId);


            //
            // Setup Encounter
            //
            Encounter encounter = mock(Encounter.class);
            when(encounter.getEncounterId()).thenReturn(sourceEncounterId);

            // Setup return for source and parsing details by id interactions
            when(patientUnderTest.getSourceById(prescription.getSourceId())).thenReturn(source);
            when(patientUnderTest.getParsingDetailById(prescription.getParsingDetailsId())).thenReturn(parsingDetail);
            when(patientUnderTest.getClinicalActorById(prescription.getPrimaryClinicalActorId())).thenReturn(clinicalActor);
            when(patientUnderTest.getEncounterById(prescription.getSourceEncounter())).thenReturn(encounter);

            //
            // Setup Clinical Actors...
            //
            List<ClinicalActor> clinicalActorList1 = new ArrayList<>();
            clinicalActorList1.add(clinicalActor);

            //
            // Setup Problem
            //
            List<Prescription> prescriptionList = new ArrayList<>();
            prescriptionList.add(prescription);

            when(patientUnderTest.getClinicalActors()).thenReturn(clinicalActorList1);
            when(patientUnderTest.getPrescriptions()).thenReturn(prescriptionList);

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

        prescriptionMerge.mergePrescription(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Prescription> prescriptionList = Lists.newArrayList(mergedPatient.getPrescriptions());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(3, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(prescriptionList.get(0).getSourceId()));
        Assert.assertEquals(sources.get(2), apo1.getSourceById(prescriptionList.get(1).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(2, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, prescriptionList);
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

        prescriptionMerge.mergePrescription(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Prescription> prescriptionList = Lists.newArrayList(mergedPatient.getPrescriptions());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo1.getSourceById(prescriptionList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, prescriptionList);
        verifyClinicalActorAndEncounter(mergedPatient, prescriptionList);
    }

    @Test
    public void testDuplicateMerge() throws IOException {
        String apoPrescription1 = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"ceed6108-bf74-4910-8ae4-17c0ae342662\", \"sourceFileHash\": \"892142ce9b6da6e31afd46bc8ed0b49cbb2e3993\", \"sourceUploadBatch\": \"472_AxmPrescriptions10\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2017-03-31T21:55:27.766Z\", \"parsingDetailsId\": \"f8f9928d-3ada-430c-b402-5b72832d24e8\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"prescriptions\": [{\"amount\": \"20 pills\", \"associatedMedication\": {\"brandName\": \"Motrin IB\", \"code\": {\"code\": \"50580-0110\", \"codingSystem\": \"NDC\", \"codingSystemOID\": \"2.16.840.1.113883.6.69\", \"displayName\": \"Motrin IB\"}, \"editType\": \"ACTIVE\", \"genericName\": \"Ibuprofin\", \"internalUUID\": \"63023da2-dc2c-480b-843e-9e43d42a82bb\", \"lastEditDateTime\": \"2017-03-31T21:55:27.773Z\", \"parsingDetailsId\": \"f8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"1f874749-bde2-44ef-a769-8e6b130f022a\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"0d4c266b-f78e-43dd-b140-ef1041e01edd\", \"lastEditDateTime\": \"2017-03-31T21:55:27.772Z\", \"originalId\": {\"assignAuthority\": \"MED_TEST_AA\", \"id\": \"MED_TEST1\"}, \"parsingDetailsId\": \"f8f9928d-3ada-430c-b402-5b72832d24e8\", \"prescriptionDate\": \"2017-03-24T00:00:00.000Z\", \"sig\": \"Take one pill with dinner, each day\", \"sourceId\": \"1f874749-bde2-44ef-a769-8e6b130f022a\"}], \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2018-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"1f874749-bde2-44ef-a769-8e6b130f022a\", \"lastEditDateTime\": \"2017-03-31T21:55:27.772Z\", \"parsingDetailsId\": \"f8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"1f874749-bde2-44ef-a769-8e6b130f022a\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";
        Patient apo1 = new PatientJSONParser().parsePatientData(apoPrescription1);

        String apoPrescription2 = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"zeed6108-bf74-4910-8ae4-17c0ae342662\", \"sourceFileHash\": \"192142ce9b6da6e31afd46bc8ed0b49cbb2e3993\", \"sourceUploadBatch\": \"472_AxmPrescriptions10\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2018-03-31T21:55:27.766Z\", \"parsingDetailsId\": \"e8f9928d-3ada-430c-b402-5b72832d24e8\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"prescriptions\": [{\"amount\": \"20 pills\", \"associatedMedication\": {\"brandName\": \"Motrin IB\", \"code\": {\"code\": \"50580-0110\", \"codingSystem\": \"NDC\", \"codingSystemOID\": \"2.16.840.1.113883.6.69\", \"displayName\": \"Motrin IB\"}, \"editType\": \"ACTIVE\", \"genericName\": \"Ibuprofin\", \"internalUUID\": \"63023da2-dc2c-480b-843e-9e43d42a82bb\", \"lastEditDateTime\": \"2018-03-31T21:55:27.773Z\", \"parsingDetailsId\": \"e8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"1f874749-bde2-44ef-a769-8e6b130f022a\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"0d4c266b-f78e-43dd-b140-ef1041e01edd\", \"lastEditDateTime\": \"2018-03-31T21:55:27.772Z\", \"originalId\": {\"assignAuthority\": \"MED_TEST_AA\", \"id\": \"MED_TEST1\"}, \"parsingDetailsId\": \"e8f9928d-3ada-430c-b402-5b72832d24e8\", \"prescriptionDate\": \"2017-03-24T00:00:00.000Z\", \"sig\": \"Take one pill with dinner, each day\", \"sourceId\": \"2f874749-bde2-44ef-a769-8e6b130f022a\"}], \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2017-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"2f874749-bde2-44ef-a769-8e6b130f022a\", \"lastEditDateTime\": \"2017-03-31T21:55:27.772Z\", \"parsingDetailsId\": \"e8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"2f874749-bde2-44ef-a769-8e6b130f022a\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";
        Patient apo2 = new PatientJSONParser().parsePatientData(apoPrescription2);

        String apoPrescription3 = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"ceed6108-bf74-4910-8ae4-17c0ae342662\", \"sourceFileHash\": \"892142ce9b6da6e31afd46bc8ed0b49cbb2e3993\", \"sourceUploadBatch\": \"472_AxmPrescriptions10\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2017-03-31T21:55:27.766Z\", \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"prescriptions\": [{\"amount\": \"20 pills\", \"associatedMedication\": {\"brandName\": \"Advil\", \"code\": {\"code\": \"00573-0150\", \"codingSystem\": \"NDC\", \"codingSystemOID\": \"2.16.840.1.113883.6.69\", \"displayName\": \"Advil IB\"}, \"editType\": \"ACTIVE\", \"genericName\": \"Ibuprofin\", \"internalUUID\": \"63023da2-dc2c-480b-843e-9e43d42a82bb\", \"lastEditDateTime\": \"2017-03-31T21:55:27.773Z\", \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"3f874749-bde2-44ef-a769-8e6b130f022a\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"0d4c266b-f78e-43dd-b140-ef1041e01edd\", \"lastEditDateTime\": \"2017-03-31T21:55:27.772Z\", \"originalId\": {\"assignAuthority\": \"MED_TEST_AA\", \"id\": \"MED_TEST1\"}, \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\", \"prescriptionDate\": \"2017-03-24T00:00:00.000Z\", \"sig\": \"Take one pill with dinner, each day\", \"sourceId\": \"3f874749-bde2-44ef-a769-8e6b130f022a\"}], \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2016-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"3f874749-bde2-44ef-a769-8e6b130f022a\", \"lastEditDateTime\": \"2017-03-31T21:55:27.772Z\", \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"3f874749-bde2-44ef-a769-8e6b130f022a\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";
        Patient apo3 = new PatientJSONParser().parsePatientData(apoPrescription3);

        PatientSet patientSet = new PatientSet(apo1, apo2);
        prescriptionMerge.mergePrescription(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;
        List<Prescription> prescriptionList = Lists.newArrayList(mergedPatient.getPrescriptions());
        Assert.assertEquals(1, prescriptionList.size());
//        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
//        Assert.assertEquals(1, clinicalActorList.size());


        PatientSet patientSet2 = new PatientSet(mergedPatient, apo3);
        prescriptionMerge.mergePrescription(patientSet2);

        Patient mergedPatient2 = patientSet2.mergedPatient;

        prescriptionList = Lists.newArrayList(mergedPatient2.getPrescriptions());
        Assert.assertEquals(2, prescriptionList.size());
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

        prescriptionMerge.mergePrescription(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Prescription> prescriptionList = Lists.newArrayList(mergedPatient.getPrescriptions());

        Assert.assertEquals(1, prescriptionList.size());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(prescriptionList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, prescriptionList);
        verifyClinicalActorAndEncounter(mergedPatient, prescriptionList);
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
            prescriptionMerge.mergePrescription(patientSet);

            mergedPatient = patientSet.mergedPatient;
        }

        //
        // Step #3 - Verify
        //
        List<Prescription> prescriptionList = Lists.newArrayList(mergedPatient.getPrescriptions());
        Assert.assertEquals(numberOfUniqueActive, prescriptionList.size());

        verifySourcesAndParsingDetails(mergedPatient, prescriptionList);
    }

    @Test
    public void testExtract()
    {
        String json = "{\"externalIDs\": [{\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}], \"parsingDetails\": [{\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\", \"metadata\": {\"sourceFileArchiveUUID\": \"ceed6108-bf74-4910-8ae4-17c0ae342662\", \"sourceFileHash\": \"892142ce9b6da6e31afd46bc8ed0b49cbb2e3993\", \"sourceUploadBatch\": \"472_AxmPrescriptions10\"}, \"parser\": \"AXM\", \"parserVersion\": \"0.1\", \"parsingDateTime\": \"2017-03-31T21:55:27.766Z\", \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\"}], \"patientId\": \"8be8ad72-3d58-4219-88a0-543c2a06bf9a\", \"prescriptions\": [{\"amount\": \"20 pills\", \"associatedMedication\": {\"brandName\": \"Advil\", \"code\": {\"code\": \"00573-0150\", \"codingSystem\": \"NDC\", \"codingSystemOID\": \"2.16.840.1.113883.6.69\", \"displayName\": \"Advil IB\"}, \"editType\": \"ACTIVE\", \"genericName\": \"Ibuprofin\", \"internalUUID\": \"63023da2-dc2c-480b-843e-9e43d42a82bb\", \"lastEditDateTime\": \"2017-03-31T21:55:27.773Z\", \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"3f874749-bde2-44ef-a769-8e6b130f022a\"}, \"editType\": \"ACTIVE\", \"internalUUID\": \"0d4c266b-f78e-43dd-b140-ef1041e01edd\", \"lastEditDateTime\": \"2017-03-31T21:55:27.772Z\", \"originalId\": {\"assignAuthority\": \"MED_TEST_AA\", \"id\": \"MED_TEST1\"}, \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\", \"prescriptionDate\": \"2017-03-24T00:00:00.000Z\", \"sig\": \"Take one pill with dinner, each day\", \"sourceId\": \"3f874749-bde2-44ef-a769-8e6b130f022a\"}], \"primaryExternalID\": {\"assignAuthority\": \"PAT_ID\", \"id\": \"ABC123\"}, \"sources\": [{\"creationDate\": \"2016-11-01T00:00:00.000Z\", \"editType\": \"ACTIVE\", \"internalUUID\": \"3f874749-bde2-44ef-a769-8e6b130f022a\", \"lastEditDateTime\": \"2017-03-31T21:55:27.772Z\", \"parsingDetailsId\": \"a8f9928d-3ada-430c-b402-5b72832d24e8\", \"sourceId\": \"3f874749-bde2-44ef-a769-8e6b130f022a\", \"sourceSystem\": \"SAMPLE\", \"sourceType\": \"EHR\"}]}";

        PrescriptionAssembler prescriptionAssembler = new PrescriptionAssembler();
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

            for(Prescription prescription : originalPatient.getPrescriptions()) {
                prescription.setSourceEncounter(encounterId);
            }

            separatedPatient = prescriptionAssembler.separate(originalPatient).get(0).getPart();
        } catch (IOException e)
        {
            fail(e.getMessage());
        }

        verifySourcesAndParsingDetails(separatedPatient, Lists.newArrayList(separatedPatient.getPrescriptions()));
        verifyClinicalActorAndEncounter(separatedPatient, Lists.newArrayList(separatedPatient.getPrescriptions()));
    }

    protected void verifySourcesAndParsingDetails(Patient mergedPatient, List<Prescription> prescriptionList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(Prescription prescription: prescriptionList)
        {
            UUID referencedSource = prescription.getSourceId();
            Assert.assertNotNull(mergedPatient.getSourceById(referencedSource));

            UUID referencedParsingDetail = prescription.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }

    protected void verifyClinicalActorAndEncounter(Patient mergedPatient, List<Prescription> prescriptionList)
    {
        //
        // All sources, and parsing details pointed to by the Problem are present
        //
        for(Prescription prescription: prescriptionList)
        {
            UUID referencedClinicalActor = prescription.getPrimaryClinicalActorId();
            if (referencedClinicalActor != null) {
                Assert.assertNotNull(mergedPatient.getClinicalActorById(referencedClinicalActor));
            }

            List<UUID> supplementaryClinicalActors = prescription.getSupplementaryClinicalActorIds();
            for(UUID clinicalActor : supplementaryClinicalActors)
            {
                Assert.assertNotNull(mergedPatient.getClinicalActorById(clinicalActor));
            }

            UUID referencedEncounter = prescription.getSourceEncounter();
            if (referencedEncounter != null) {
                Assert.assertNotNull(mergedPatient.getEncounterById(referencedEncounter));
            }
        }
    }
}
