package com.apixio.bizlogic.patient.assembly.demographic;

import com.apixio.bizlogic.patient.assembly.merge.DemographicMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.*;
import com.apixio.model.utility.PatientJSONParser;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

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
public class DemographicMergeMockTest
{
    DemographicMerge demographicMerge = new DemographicMerge();

    static public class PatientBuilder
    {
        public DateTime creationDate;
        public EditType editType;
        public String externalID;
        public String assignAuthority;
        public ActorRole role;
        public Gender gender;

        // Optional

        public UUID demographicInternalUUID;
        public UUID sourceId;
        public UUID parsingDetailsId;

        public PatientBuilder(DateTime creationDate, EditType editType, String externalID, String assignAuthority, ActorRole role)
        {
            this(creationDate, editType, externalID, assignAuthority, role, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        }

        public PatientBuilder(DateTime creationDate, EditType editType, String externalID, String assignAuthority, ActorRole role,
                              UUID  demographicInternalUUID, UUID sourceId, UUID parsingDetailsId)
        {
            this.creationDate = creationDate;
            this.editType = editType;
            this.externalID = externalID;
            this.assignAuthority = assignAuthority;
            this.role = role;
            this.demographicInternalUUID = demographicInternalUUID;
            this.sourceId = sourceId;
            this.parsingDetailsId = parsingDetailsId;
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
            // Setup the Demographic
            //
            Demographics demographics = new Demographics();
            demographics.setGender(gender);
            demographics.setInternalUUID(demographicInternalUUID);
            demographics.setSourceId(sourceId);
            demographics.setParsingDetailsId(parsingDetailsId);
            demographics.setEditType(editType);

            // Setup return for source and parsing details by id interactions
            when(patientUnderTest.getSourceById(demographics.getSourceId())).thenReturn(source);
            when(patientUnderTest.getParsingDetailById(demographics.getParsingDetailsId())).thenReturn(parsingDetail);

            //
            // Setup Clinical Actors...
            //
            List<Demographics> demographicsList = new ArrayList<>();
            //demographicsList.add(demographics);

            when(patientUnderTest.getPrimaryDemographics()).thenReturn(demographics);
            when(patientUnderTest.getAlternateDemographics()).thenReturn(demographicsList);

            return patientUnderTest;
        }
    }

    @Test
    public void testSameDemographics()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        demographicMerge.mergeDemographics(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        Assert.assertEquals(apo2.getPrimaryDemographics(), mergedPatient.getPrimaryDemographics());

        List<Demographics> demographicsList = Lists.newArrayList(mergedPatient.getAlternateDemographics());
        Assert.assertEquals(1, demographicsList.size());
        Assert.assertEquals(apo2.getPrimaryDemographics(), demographicsList.get(0));

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(1, sources.size());

        verifyParsingDetails(mergedPatient, demographicsList);
    }


    @Test
    public void testDifferentDemographics()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        patientBuilderForApo1.gender = Gender.FEMALE;
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        patientBuilderForApo1.gender = Gender.MALE;
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        demographicMerge.mergeDemographics(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        Assert.assertEquals(apo2.getPrimaryDemographics(), mergedPatient.getPrimaryDemographics());

        List<Demographics> demographicsList = Lists.newArrayList(mergedPatient.getAlternateDemographics());
        Assert.assertEquals(2, demographicsList.size());
        Assert.assertEquals(apo2.getPrimaryDemographics(), demographicsList.get(0));
        Assert.assertEquals(apo1.getPrimaryDemographics(), demographicsList.get(1));

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(1, sources.size());

        verifyParsingDetails(mergedPatient, demographicsList);
    }

    protected void verifyParsingDetails(Patient mergedPatient, List<Demographics> problemList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(Demographics demographics: problemList)
        {
            UUID referencedParsingDetail = demographics.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }

    @Test
    public void testNewerMetadataMerge() {
        //pp(prd.dataorchestrator.patient_object_by_doc('b56a8e9c-9472-4b75-b614-06eaed5db140').json())
        //pp(prd.dataorchestrator.patient_object_by_doc('955d3293-9dcd-4b21-a219-1d52bd97ec87').json())
        // got the data from production, but removed the patient ids and the name and changed the DOB
        String partPat1 = "{\"patientId\":\"39953d7e-d21a-4917-8c37-d8f0b3784aea\",\"primaryExternalID\":{\"id\":\"FAKE\",\"assignAuthority\":\"PAT_ID\"},\"externalIDs\":[{\"id\":\"FAKE\",\"assignAuthority\":\"PAT_ID\"}],\"primaryDemographics\":{\"metadata\":{\"PRIMARY_CARE_PROVIDER\":\"1891716775^^NPI\"},\"sourceId\":\"dd233272-840f-4c23-8b4d-8555d60a98b6\",\"parsingDetailsId\":\"0ba2a190-5172-413b-ae34-b8ef60756155\",\"internalUUID\":\"6892d9e6-b2fe-4b0f-85f7-c67bcddd454b\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2021-03-26T20:29:55.990Z\",\"name\":{\"internalUUID\":\"e63d0e98-ad2b-424b-a895-154a70f19293\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2021-03-26T20:29:55.990Z\",\"givenNames\":[\"FAKE\"],\"familyNames\":[\"FAKE\"],\"nameType\":\"CURRENT_NAME\"},\"dateOfBirth\":\"1963-01-01T00:00:00.000Z\",\"gender\":\"MALE\"},\"sources\":[{\"sourceId\":\"dd233272-840f-4c23-8b4d-8555d60a98b6\",\"parsingDetailsId\":\"0ba2a190-5172-413b-ae34-b8ef60756155\",\"internalUUID\":\"dd233272-840f-4c23-8b4d-8555d60a98b6\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2021-03-26T20:29:55.990Z\",\"sourceSystem\":\"PDF\",\"sourceType\":\"PDF\",\"creationDate\":\"2021-03-26T00:00:00.000Z\"}],\"parsingDetails\":[{\"metadata\":{\"sourceUploadBatch\":\"10001057_GLOBALHEALTH_CA_INCR_20210325_DEMOGRAPHICS\",\"sourceFileArchiveUUID\":\"b56a8e9c-9472-4b75-b614-06eaed5db140\",\"sourceFileHash\":\"54b99b707a71aeb2947f4ca17570f419d2c55871\"},\"parsingDetailsId\":\"0ba2a190-5172-413b-ae34-b8ef60756155\",\"parsingDateTime\":\"2021-03-26T20:29:55.989Z\",\"contentSourceDocumentType\":\"STRUCTURED_DOCUMENT\",\"parser\":\"AXM\",\"parserVersion\":\"0.1\"}]}";
        String partPat2 = "{\"patientId\":\"39953d7e-d21a-4917-8c37-d8f0b3784aea\",\"primaryExternalID\":{\"id\":\"FAKE\",\"assignAuthority\":\"PAT_ID\"},\"externalIDs\":[{\"id\":\"FAKE\",\"assignAuthority\":\"PAT_ID\"}],\"primaryDemographics\":{\"metadata\":{\"PRIMARY_CARE_PROVIDER\":\"1407116270^^NPI\"},\"sourceId\":\"bae51bf3-cc56-46fe-8baf-5ea539d2e560\",\"parsingDetailsId\":\"2e474f88-20a9-43e3-ae8e-75471c114884\",\"internalUUID\":\"af718c30-b99c-4219-bd03-bcc21b9fbaa8\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2021-03-20T05:00:05.240Z\",\"name\":{\"internalUUID\":\"913b0c04-0b7a-469d-aea9-d74d7acff28d\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2021-03-20T05:00:05.240Z\",\"givenNames\":[\"FAKE\"],\"familyNames\":[\"FAKE\"],\"nameType\":\"CURRENT_NAME\"},\"dateOfBirth\":\"1963-01-01T00:00:00.000Z\",\"gender\":\"MALE\"},\"sources\":[{\"sourceId\":\"bae51bf3-cc56-46fe-8baf-5ea539d2e560\",\"parsingDetailsId\":\"2e474f88-20a9-43e3-ae8e-75471c114884\",\"internalUUID\":\"bae51bf3-cc56-46fe-8baf-5ea539d2e560\",\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2021-03-20T05:00:05.240Z\",\"sourceSystem\":\"PDF\",\"sourceType\":\"PDF\",\"creationDate\":\"2021-03-20T00:00:00.000Z\"}],\"parsingDetails\":[{\"metadata\":{\"sourceUploadBatch\":\"10001057_GLOBALHEALTH_CA_INCR_20210318_DEMOGRAPHICS\",\"sourceFileArchiveUUID\":\"955d3293-9dcd-4b21-a219-1d52bd97ec87\",\"sourceFileHash\":\"6e7f450259a31c90ecfdd634c09bf4f031178eac\"},\"parsingDetailsId\":\"2e474f88-20a9-43e3-ae8e-75471c114884\",\"parsingDateTime\":\"2021-03-20T05:00:05.240Z\",\"contentSourceDocumentType\":\"STRUCTURED_DOCUMENT\",\"parser\":\"AXM\",\"parserVersion\":\"0.1\"}]}";

        PatientJSONParser parser = new PatientJSONParser();

        try {
            Patient part1 = parser.parsePatientData(partPat1);
            Patient part2 = parser.parsePatientData(partPat2);

            String expectedPCP = part1.getPrimaryDemographics().getMetaTag("PRIMARY_CARE_PROVIDER");

            PatientSet patientSet = new PatientSet(part1, part2);

            demographicMerge.mergeDemographics(patientSet);

            Patient mergedPatient = patientSet.mergedPatient;
//            System.out.println(parser.toJSON(mergedPatient));

            System.out.println(expectedPCP);
            System.out.println(part1.getPrimaryDemographics().getMetaTag("PRIMARY_CARE_PROVIDER"));
            System.out.println(part2.getPrimaryDemographics().getMetaTag("PRIMARY_CARE_PROVIDER"));
            System.out.println(mergedPatient.getPrimaryDemographics().getMetaTag("PRIMARY_CARE_PROVIDER"));

            Assert.assertTrue( // confirm part1 has newer source date
                    AssemblerUtility.getLatestSourceDate(part1).isAfter(AssemblerUtility.getLatestSourceDate(part2)));

            Assert.assertEquals(
                    part1.getParsingDetails().iterator().next(), patientSet.basePatient.getParsingDetails().iterator().next());

            Assert.assertEquals(
                    part1.getParsingDetails().iterator().next().getSourceUploadBatch(),
                    mergedPatient.getParsingDetailById(mergedPatient.getPrimaryDemographics().getParsingDetailsId()).getSourceUploadBatch());

            Assert.assertEquals(
                    expectedPCP,
                    mergedPatient.getPrimaryDemographics().getMetaTag("PRIMARY_CARE_PROVIDER"));
        }
        catch (Throwable t) {
            t.printStackTrace();
            fail();
        }

    }

}
