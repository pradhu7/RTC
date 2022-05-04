package com.apixio.bizlogic.patient.assembly.procedure;

import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.ProcedureMerge;
import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Procedure;
import com.apixio.model.patient.Source;
import com.apixio.model.utility.PatientJSONParser;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
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
 * Created by dyee on 1/25/18.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcedureMergeMockTest
{
    @InjectMocks
    ProcedureMerge procedureMerge;

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
        public UUID problemUUID = UUID.randomUUID();
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

            Procedure procedure = mock(Procedure.class);
            when(procedure.getEditType()).thenReturn(editType);
            when(procedure.getInternalUUID()).thenReturn(problemUUID);
            when(procedure.getEndDate()).thenReturn(new DateTime(1000));
            when(procedure.getPrimaryClinicalActorId()).thenReturn(clinicalActorId);
            when(procedure.getSupplementaryClinicalActorIds()).thenReturn(Collections.<UUID>emptyList());
            when(procedure.getSourceEncounter()).thenReturn(sourceEncounterId);
            when(procedure.getCode()).thenReturn(clinicalCode);
            when(procedure.getCodeTranslations()).thenReturn(Collections.<ClinicalCode>emptyList());
            when(procedure.getMetadata()).thenReturn(Collections.<String, String>emptyMap());
            when(procedure.getSourceId()).thenReturn(sourceId);
            when(procedure.getParsingDetailsId()).thenReturn(parsingDetailsId);


            //
            // Setup Encounter
            //
            Encounter encounter = mock(Encounter.class);
            when(encounter.getEncounterId()).thenReturn(sourceEncounterId);

            // Setup return for source and parsing details by id interactions
            when(patientUnderTest.getSourceById(procedure.getSourceId())).thenReturn(source);
            when(patientUnderTest.getParsingDetailById(procedure.getParsingDetailsId())).thenReturn(parsingDetail);
            when(patientUnderTest.getClinicalActorById(procedure.getPrimaryClinicalActorId())).thenReturn(clinicalActor);
            when(patientUnderTest.getEncounterById(procedure.getSourceEncounter())).thenReturn(encounter);

            //
            // Setup Clinical Actors...
            //
            List<ClinicalActor> clinicalActorList1 = new ArrayList<>();
            clinicalActorList1.add(clinicalActor);

            //
            // Setup Problem
            //
            List<Procedure> procedureList = new ArrayList<>();
            procedureList.add(procedure);

            when(patientUnderTest.getClinicalActors()).thenReturn(clinicalActorList1);
            when(patientUnderTest.getProcedures()).thenReturn(procedureList);

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
    public void testTwoUniqueProblems_ACTIVE()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        procedureMerge.mergeProcedure(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Procedure> procedureList = Lists.newArrayList(mergedPatient.getProcedures());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(3, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(procedureList.get(0).getSourceId()));
        Assert.assertEquals(sources.get(2), apo1.getSourceById(procedureList.get(1).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(2, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, procedureList);
    }

    @Ignore
    @Test
    public void testDuplicateMerge() throws IOException {
        String apoProcedure1 = "{\"procedures\":[{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-10\",\"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"},\"sourceId\":\"d7c9bb7f-74dc-44d7-b12e-104366ef901f\",\"parsingDetailsId\":\"88163197-673d-49eb-b632-ba693cb3e22f\",\"internalUUID\":\"2765e6be-eb79-4f5b-965c-b16e61df5c74\",\"originalId\":{\"assignAuthority\":\"c24abaab-7538-437b-b29d-3ba506f21201\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:44:09.728-08:00\",\"primaryClinicalActorId\":\"711f0bb2-8df9-4904-bf4a-d10024d3209e\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"interpretation\":\"INTERPRETATION1\",\"supportingDiagnosis\":[{\"code\":\"S99291A\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"S99291A\"}]},{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-11\",\"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"},\"sourceId\":\"d7c9bb7f-74dc-44d7-b12e-104366ef901f\",\"parsingDetailsId\":\"88163197-673d-49eb-b632-ba693cb3e22f\",\"internalUUID\":\"87baf5a2-528d-4bfc-b8c3-f3c5e37e70af\",\"originalId\":{\"assignAuthority\":\"c24abaab-7538-437b-b29d-3ba506f21201\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:44:09.730-08:00\",\"primaryClinicalActorId\":\"96a17e69-1a1c-4ebc-8fe8-f4b67c45d220\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"supportingDiagnosis\":[{\"code\":\"M4141\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"M4141\"}]}],\"patientId\":\"84ff6c87-932a-4978-b27e-5c590bb1ef03\",\"primaryExternalID\":{\"id\":\"0001\",\"assignAuthority\":\"PAT_ID\"},\"externalIDs\":[{\"id\":\"0001\",\"assignAuthority\":\"PAT_ID\"}],\"sources\":[{\"sourceId\":\"d7c9bb7f-74dc-44d7-b12e-104366ef901f\",\"parsingDetailsId\":\"88163197-673d-49eb-b632-ba693cb3e22f\",\"internalUUID\":\"d7c9bb7f-74dc-44d7-b12e-104366ef901f\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:44:09.728-08:00\",\"sourceSystem\":\"EHR\",\"sourceType\":\"ACA_CLAIM\",\"creationDate\":\"2016-01-02T16:00:00.000-08:00\"}],\"parsingDetails\":[{\"metadata\":{\"sourceUploadBatch\":\"1136_merge_ffs_claims_1\",\"sourceFileArchiveUUID\":\"7baaefc6-eb28-41b7-82ae-16d8b3f1a522\",\"sourceFileHash\":\"d49ca2c66e4c12a5f66b69d313855bb5e0e6b8d0\"},\"parsingDetailsId\":\"88163197-673d-49eb-b632-ba693cb3e22f\",\"parsingDateTime\":\"2019-01-28T14:44:09.725-08:00\",\"contentSourceDocumentType\":\"STRUCTURED_DOCUMENT\",\"parser\":\"AXM\",\"parserVersion\":\"0.1\"}],\"clinicalActors\":[{\"sourceId\":\"d7c9bb7f-74dc-44d7-b12e-104366ef901f\",\"parsingDetailsId\":\"88163197-673d-49eb-b632-ba693cb3e22f\",\"internalUUID\":\"711f0bb2-8df9-4904-bf4a-d10024d3209e\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-29T13:11:03.208-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"711f0bb2-8df9-4904-bf4a-d10024d3209e\"},{\"sourceId\":\"d7c9bb7f-74dc-44d7-b12e-104366ef901f\",\"parsingDetailsId\":\"88163197-673d-49eb-b632-ba693cb3e22f\",\"internalUUID\":\"96a17e69-1a1c-4ebc-8fe8-f4b67c45d220\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:44:09.730-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"96a17e69-1a1c-4ebc-8fe8-f4b67c45d220\"}]}";
        Patient apo1 = new PatientJSONParser().parsePatientData(apoProcedure1);

        String apoProcedure2 = "{\"procedures\":[{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-10\",\"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"},\"sourceId\":\"507644ae-f72f-44fa-b8e3-2cf1456720f1\",\"parsingDetailsId\":\"9f1620e2-300f-48d8-9aa7-ee67aae2ab89\",\"internalUUID\":\"b276d99e-fa8c-4b4a-816b-a609d8f9c9de\",\"originalId\":{\"assignAuthority\":\"c24abaab-7538-437b-b29d-3ba506f21201\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:50:44.501-08:00\",\"primaryClinicalActorId\":\"d8d39f04-7421-452b-93af-99ead2fd8be6\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"interpretation\":\"INTERPRETATION1\",\"supportingDiagnosis\":[{\"code\":\"S99291A\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"S99291A\"}]},{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-11\",\"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"},\"sourceId\":\"507644ae-f72f-44fa-b8e3-2cf1456720f1\",\"parsingDetailsId\":\"9f1620e2-300f-48d8-9aa7-ee67aae2ab89\",\"internalUUID\":\"744f4c15-ce1d-45cd-a0e7-44ab7f301e12\",\"originalId\":{\"assignAuthority\":\"c24abaab-7538-437b-b29d-3ba506f21201\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:50:44.503-08:00\",\"primaryClinicalActorId\":\"9a5241da-7454-4196-9bec-39f6585fe05b\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"supportingDiagnosis\":[{\"code\":\"M4141\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"M4141\"}]}],\"patientId\":\"84ff6c87-932a-4978-b27e-5c590bb1ef03\",\"primaryExternalID\":{\"id\":\"0001\",\"assignAuthority\":\"PAT_ID\"},\"externalIDs\":[{\"id\":\"0001\",\"assignAuthority\":\"PAT_ID\"}],\"sources\":[{\"sourceId\":\"507644ae-f72f-44fa-b8e3-2cf1456720f1\",\"parsingDetailsId\":\"9f1620e2-300f-48d8-9aa7-ee67aae2ab89\",\"internalUUID\":\"507644ae-f72f-44fa-b8e3-2cf1456720f1\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:50:44.501-08:00\",\"sourceSystem\":\"EHR\",\"sourceType\":\"ACA_CLAIM\",\"creationDate\":\"2016-01-02T16:00:00.000-08:00\"}],\"parsingDetails\":[{\"metadata\":{\"sourceUploadBatch\":\"1136_merge_ffs_claims_2\",\"sourceFileArchiveUUID\":\"8422877c-be2e-40f8-8b8c-ba6372ad7af1\",\"sourceFileHash\":\"d49ca2c66e4c12a5f66b69d313855bb5e0e6b8d0\"},\"parsingDetailsId\":\"9f1620e2-300f-48d8-9aa7-ee67aae2ab89\",\"parsingDateTime\":\"2019-01-28T14:50:44.498-08:00\",\"contentSourceDocumentType\":\"STRUCTURED_DOCUMENT\",\"parser\":\"AXM\",\"parserVersion\":\"0.1\"}],\"clinicalActors\":[{\"sourceId\":\"507644ae-f72f-44fa-b8e3-2cf1456720f1\",\"parsingDetailsId\":\"9f1620e2-300f-48d8-9aa7-ee67aae2ab89\",\"internalUUID\":\"d8d39f04-7421-452b-93af-99ead2fd8be6\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:50:44.502-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"d8d39f04-7421-452b-93af-99ead2fd8be6\"},{\"sourceId\":\"507644ae-f72f-44fa-b8e3-2cf1456720f1\",\"parsingDetailsId\":\"9f1620e2-300f-48d8-9aa7-ee67aae2ab89\",\"internalUUID\":\"9a5241da-7454-4196-9bec-39f6585fe05b\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:50:44.503-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"9a5241da-7454-4196-9bec-39f6585fe05b\"}]}";
        Patient apo2 = new PatientJSONParser().parsePatientData(apoProcedure2);

        String apoProcedure3 = "{\"procedures\":[{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-10\",\"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"},\"sourceId\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"internalUUID\":\"98fa5428-2966-4b78-a94b-51eb9b14948a\",\"originalId\":{\"assignAuthority\":\"c24abaab-7538-437b-b29d-3ba506f21201\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:54:07.981-08:00\",\"primaryClinicalActorId\":\"b1e504bf-ec82-4edb-936e-c0e6ea071ca6\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"interpretation\":\"INTERPRETATION1\",\"supportingDiagnosis\":[{\"code\":\"S99291A\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"S99291A\"}]},{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-11\",\"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"},\"sourceId\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"internalUUID\":\"74fc5deb-8536-4e81-a696-54bc8267742f\",\"originalId\":{\"assignAuthority\":\"c24abaab-7538-437b-b29d-3ba506f21201\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:54:07.982-08:00\",\"primaryClinicalActorId\":\"2273adab-5721-45fc-aee4-3830ecb20d47\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"supportingDiagnosis\":[{\"code\":\"M4141\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"M4141\"}]},{\"metadata\":{\"TRANSACTION_DATE\":\"2016-03-05\",\"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"},\"sourceId\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"internalUUID\":\"ea8e833e-1806-4023-8298-176549718ccc\",\"originalId\":{\"assignAuthority\":\"ed24a6a8-0318-4b3e-b81a-f61d36e9d5e7\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:54:07.982-08:00\",\"primaryClinicalActorId\":\"a67d0726-85a0-465a-a5a5-1bb5c95e1e67\",\"code\":{\"code\":\"43100\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"43100\"},\"procedureName\":\"43100^43100^CPT4^2.16.840.1.113883.6.12^\",\"performedOn\":\"2016-03-04T16:00:00.000-08:00\",\"endDate\":\"2016-03-04T16:00:00.000-08:00\",\"supportingDiagnosis\":[{\"code\":\"0CN50Z\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"0CN50Z\"}]}],\"patientId\":\"84ff6c87-932a-4978-b27e-5c590bb1ef03\",\"primaryExternalID\":{\"id\":\"0001\",\"assignAuthority\":\"PAT_ID\"},\"externalIDs\":[{\"id\":\"0001\",\"assignAuthority\":\"PAT_ID\"}],\"sources\":[{\"sourceId\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"internalUUID\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:54:07.981-08:00\",\"sourceSystem\":\"EHR\",\"sourceType\":\"ACA_CLAIM\",\"creationDate\":\"2016-01-02T16:00:00.000-08:00\"}],\"parsingDetails\":[{\"metadata\":{\"sourceUploadBatch\":\"1136_merge_ffs_claims_3\",\"sourceFileArchiveUUID\":\"2a0146ff-9e06-4f10-b44a-e6dfedf606dc\",\"sourceFileHash\":\"03e7f912b1b37c2d6d13a71ebaefd2c6dc23155f\"},\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"parsingDateTime\":\"2019-01-28T14:54:07.978-08:00\",\"contentSourceDocumentType\":\"STRUCTURED_DOCUMENT\",\"parser\":\"AXM\",\"parserVersion\":\"0.1\"}],\"clinicalActors\":[{\"sourceId\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"internalUUID\":\"b1e504bf-ec82-4edb-936e-c0e6ea071ca6\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:54:07.981-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"b1e504bf-ec82-4edb-936e-c0e6ea071ca6\"},{\"sourceId\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"internalUUID\":\"2273adab-5721-45fc-aee4-3830ecb20d47\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:54:07.982-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"2273adab-5721-45fc-aee4-3830ecb20d47\"},{\"sourceId\":\"96caaf94-3bef-42fa-89d4-f575f719cb75\",\"parsingDetailsId\":\"ca53d0bf-83f5-4fa4-8cba-f05f17bcabf6\",\"internalUUID\":\"a67d0726-85a0-465a-a5a5-1bb5c95e1e67\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2019-01-28T14:54:07.982-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"a67d0726-85a0-465a-a5a5-1bb5c95e1e67\"}]}";
        Patient apo3 = new PatientJSONParser().parsePatientData(apoProcedure3);

        PatientSet patientSet = new PatientSet(apo1, apo2);
        procedureMerge.mergeProcedure(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Procedure> procedureList = Lists.newArrayList(mergedPatient.getProcedures());
        Assert.assertEquals(2, procedureList.size());
        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(1, clinicalActorList.size());


        PatientSet patientSet2 = new PatientSet(mergedPatient, apo3);
        procedureMerge.mergeProcedure(patientSet2);

        Patient mergedPatient2 = patientSet2.mergedPatient;

        procedureList = Lists.newArrayList(mergedPatient2.getProcedures());
        Assert.assertEquals(3, procedureList.size());
        clinicalActorList = Lists.newArrayList(mergedPatient2.getClinicalActors());
        Assert.assertEquals(1, clinicalActorList.size());

    }

    /**
     *  Two patients, one active - one deleted.
     */
    @Test
    public void testTwoUniqueProblems_MIXED()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.DELETE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        procedureMerge.mergeProcedure(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Procedure> procedureList = Lists.newArrayList(mergedPatient.getProcedures());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo1.getSourceById(procedureList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, procedureList);
        verifyClinicalActorAndEncounter(mergedPatient, procedureList);
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

        procedureMerge.mergeProcedure(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Procedure> procedureList = Lists.newArrayList(mergedPatient.getProcedures());

        Assert.assertEquals(1, procedureList.size());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(procedureList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, procedureList);
        verifyClinicalActorAndEncounter(mergedPatient, procedureList);
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
            procedureMerge.mergeProcedure(patientSet);

            mergedPatient = patientSet.mergedPatient;
        }

        //
        // Step #3 - Verify
        //
        List<Procedure> procedureList = Lists.newArrayList(mergedPatient.getProcedures());
        Assert.assertEquals(numberOfUniqueActive, procedureList.size());

        verifySourcesAndParsingDetails(mergedPatient, procedureList);
    }

    @Test
    public void testExtract()
    {
        String json = "{\"procedures\":[{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-10\"},\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"523bd944-ee3e-438b-b5f9-1452efc8a21b\",\"originalId\":{\"assignAuthority\":\"onefish\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.763-08:00\",\"primaryClinicalActorId\":\"96ea2470-c9e2-417b-9127-79c201e6a30e\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"interpretation\":\"INTERPRETATION1\",\"supportingDiagnosis\":[{\"code\":\"S99291A\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"S99291A\"}]},{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-11\"},\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"cb4c6fc7-217e-4cd4-a0c0-f81928c05462\",\"originalId\":{\"assignAuthority\":\"twofish\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.765-08:00\",\"primaryClinicalActorId\":\"f27eab82-687c-449e-8f31-f7d6cd470c67\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"supportingDiagnosis\":[{\"code\":\"M4141\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"M4141\"}]},{\"metadata\":{\"TRANSACTION_DATE\":\"2017-02-11\"},\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"e4769c15-c66e-49df-bf64-10539f83d44a\",\"originalId\":{\"assignAuthority\":\"redfish\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.765-08:00\",\"primaryClinicalActorId\":\"31f9860a-a210-44ab-8f7e-8517ecfbee47\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2017-02-09T16:00:00.000-08:00\",\"endDate\":\"2017-02-09T16:00:00.000-08:00\",\"supportingDiagnosis\":[{\"code\":\"M4141\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"M4141\"}]},{\"metadata\":{\"TRANSACTION_DATE\":\"2016-12-11\"},\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"ad5b1822-a8b5-44b7-a30c-0378f11d4284\",\"originalId\":{\"assignAuthority\":\"bluefish\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.765-08:00\",\"primaryClinicalActorId\":\"1b7fbdbd-51cf-4d18-a614-167c77b24759\",\"code\":{\"code\":\"64722\",\"codingSystem\":\"CPT_4\",\"codingSystemOID\":\"2.16.840.1.113883.6.12\",\"displayName\":\"64722\"},\"procedureName\":\"NOT PART OF UNIQUENESS^\",\"performedOn\":\"2016-12-09T16:00:00.000-08:00\",\"endDate\":\"2016-12-09T16:00:00.000-08:00\",\"supportingDiagnosis\":[{\"code\":\"M4141\",\"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\"codingSystemOID\":\"2.16.840.1.113883.6.90\",\"displayName\":\"M4141\"}]}],\"patientId\":\"31b67fcb-9567-426a-8cb9-d009b2b09771\",\"primaryExternalID\":{\"id\":\"proceduresWithSameNPI2\",\"assignAuthority\":\"PAT_ID\"},\"externalIDs\":[{\"id\":\"proceduresWithSameNPI2\",\"assignAuthority\":\"PAT_ID\"}],\"sources\":[{\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.763-08:00\",\"sourceSystem\":\"Apixio\",\"sourceType\":\"Test\",\"creationDate\":\"2018-01-29T16:00:00.000-08:00\"}],\"parsingDetails\":[{\"metadata\":{\"sourceUploadBatch\":\"1109_procedures_with_same_npi_2\",\"sourceFileArchiveUUID\":\"a176b7b0-ae8a-4f1c-848a-8c79bb0daeee\",\"sourceFileHash\":\"c7ac55ffb06becafd001cec2b01d4e9f09c19fac\"},\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"parsingDateTime\":\"2018-02-13T14:46:13.760-08:00\",\"contentSourceDocumentType\":\"STRUCTURED_DOCUMENT\",\"parser\":\"AXM\",\"parserVersion\":\"0.1\"}],\"clinicalActors\":[{\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"96ea2470-c9e2-417b-9127-79c201e6a30e\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.764-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"96ea2470-c9e2-417b-9127-79c201e6a30e\"},{\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"f27eab82-687c-449e-8f31-f7d6cd470c67\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.765-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"f27eab82-687c-449e-8f31-f7d6cd470c67\"},{\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"31f9860a-a210-44ab-8f7e-8517ecfbee47\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.765-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"31f9860a-a210-44ab-8f7e-8517ecfbee47\"},{\"sourceId\":\"673e6c02-21d5-4935-aa71-b0c404556ffb\",\"parsingDetailsId\":\"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\"internalUUID\":\"1b7fbdbd-51cf-4d18-a614-167c77b24759\",\"originalId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2018-02-13T14:46:13.765-08:00\",\"primaryId\":{\"id\":\"0123456789\",\"assignAuthority\":\"NPI\"},\"role\":\"ADMINISTERING_PHYSICIAN\",\"clinicalActorId\":\"1b7fbdbd-51cf-4d18-a614-167c77b24759\"}]}";

        ProcedureAssembler procedureAssembler = new ProcedureAssembler();
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

            for(Procedure procedure : originalPatient.getProcedures()) {
                procedure.setSourceEncounter(encounterId);
            }

            separatedPatient = procedureAssembler.separate(originalPatient).get(0).getPart();
        } catch (IOException e)
        {
            fail(e.getMessage());
        }

        verifySourcesAndParsingDetails(separatedPatient, Lists.<Procedure>newArrayList(separatedPatient.getProcedures()));
        verifyClinicalActorAndEncounter(separatedPatient, Lists.<Procedure>newArrayList(separatedPatient.getProcedures()));
    }

    protected void verifySourcesAndParsingDetails(Patient mergedPatient, List<Procedure> procedureList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(Procedure procedure: procedureList)
        {
            UUID referencedSource = procedure.getSourceId();
            Assert.assertNotNull(mergedPatient.getSourceById(referencedSource));

            UUID referencedParsingDetail = procedure.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }

    protected void verifyClinicalActorAndEncounter(Patient mergedPatient, List<Procedure> procedureList)
    {
        //
        // All sources, and parsing details pointed to by the Problem are present
        //
        for(Procedure procedure: procedureList)
        {
            UUID referencedClinicalActor = procedure.getPrimaryClinicalActorId();
            Assert.assertNotNull(mergedPatient.getClinicalActorById(referencedClinicalActor));

            List<UUID> supplementaryClinicalActors = procedure.getSupplementaryClinicalActorIds();
            for(UUID clinicalActor : supplementaryClinicalActors)
            {
                Assert.assertNotNull(mergedPatient.getClinicalActorById(clinicalActor));
            }

            UUID referencedEncounter = procedure.getSourceEncounter();
            Assert.assertNotNull(mergedPatient.getEncounterById(referencedEncounter));
        }
    }
}
