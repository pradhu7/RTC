package com.apixio.bizlogic.patient.assembly.clinicalactor;

import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dyee on 1/23/18.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClinicalActorMergeMockTest
{
    @InjectMocks
    ClinicalActorMerge clinicalActorMerge;

    static public class PatientBuilder
    {
        public DateTime creationDate;
        public EditType editType;
        public String externalID;
        public String assignAuthority;
        public ActorRole role;

        // Optional

        public UUID clinicalActorId;
        public UUID sourceId;
        public UUID parsingDetailsId;

        public PatientBuilder(DateTime creationDate, EditType editType, String externalID, String assignAuthority, ActorRole role)
        {
            this(creationDate, editType, externalID, assignAuthority, role, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        }

        public PatientBuilder(DateTime creationDate, EditType editType, String externalID, String assignAuthority, ActorRole role,
                              UUID  clinicalActorId, UUID sourceId, UUID parsingDetailsId)
        {
            this.creationDate = creationDate;
            this.editType = editType;
            this.externalID = externalID;
            this.assignAuthority = assignAuthority;
            this.role = role;
            this.clinicalActorId = clinicalActorId;
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
            // Setup the clinical Actor
            //
            ClinicalActor clinicalActor = mock(ClinicalActor.class);
            when(clinicalActor.getClinicalActorId()).thenReturn(clinicalActorId);
            when(clinicalActor.getEditType()).thenReturn(editType);
            when(clinicalActor.getParsingDetailsId()).thenReturn(parsingDetailsId);
            when(clinicalActor.getSourceId()).thenReturn(sourceId);
            when(clinicalActor.getPrimaryId()).thenReturn(externalId);
            when(clinicalActor.getRole()).thenReturn(role);

            // Setup return for source and parsing details by id interactions
            when(patientUnderTest.getSourceById(clinicalActor.getSourceId())).thenReturn(source);
            when(patientUnderTest.getParsingDetailById(clinicalActor.getParsingDetailsId())).thenReturn(parsingDetail);

            //
            // Setup Clinical Actors...
            //
            List<ClinicalActor> clinicalActorList1 = new ArrayList<>();
            clinicalActorList1.add(clinicalActor);

            when(patientUnderTest.getClinicalActors()).thenReturn(clinicalActorList1);

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
    public void testTwoUniqueClinicalActors_ACTIVE()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        clinicalActorMerge.mergeClinicalActors(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        //
        // Expect newest first
        //
        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(2, clinicalActorList.size());
        Assert.assertEquals(apo2.getClinicalActors().iterator().next(), clinicalActorList.get(0));
        Assert.assertEquals(apo1.getClinicalActors().iterator().next(), clinicalActorList.get(1));

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(3, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(clinicalActorList.get(0).getSourceId()));
        Assert.assertEquals(sources.get(2), apo1.getSourceById(clinicalActorList.get(1).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(2, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, clinicalActorList);
    }

    /**
     * TEST CASE:
     *
     * Patient1 later than Patient2, Active, different externalId and Assign Authority
     *
     * Expected: 2 clinical Actors, 2 sources, and 2 Parsing details...
     *
     */
    @Test
    public void testTwoUniqueClinicalActors_ACTIVE_reversed()
    {
        PatientBuilder patientBuilderForApo1 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo1 = patientBuilderForApo1.build();

        PatientBuilder patientBuilderForApo2 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN);
        Patient apo2 = patientBuilderForApo2.build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        clinicalActorMerge.mergeClinicalActors(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        //
        // Expect newest first
        //
        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(2, clinicalActorList.size());
        Assert.assertEquals(apo1.getClinicalActors().iterator().next(), clinicalActorList.get(0));
        Assert.assertEquals(apo2.getClinicalActors().iterator().next(), clinicalActorList.get(1));

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(3, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo1.getSourceById(clinicalActorList.get(0).getSourceId()));
        Assert.assertEquals(sources.get(2), apo2.getSourceById(clinicalActorList.get(1).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(2, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, clinicalActorList);

    }

    //
    // Both APOs marked "ARCHIVED" - so should not be included in merge...
    //
    @Test
    public void testTwoUniqueClinicalActors_ARCHIVE()
    {
        Patient apo1 = new PatientBuilder(
                new DateTime(0), EditType.ARCHIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN).build();

        Patient apo2 = new PatientBuilder(
                new DateTime(1000), EditType.ARCHIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN).build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        clinicalActorMerge.mergeClinicalActors(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(0, clinicalActorList.size());

        verifySourcesAndParsingDetails(mergedPatient, clinicalActorList);
    }

    //
    // One Patient marked ACTIVE, One Archive - only the active one is in the merge.
    //
    @Test
    public void testTwoUniqueClinicalActors_MixedEditTypes()
    {
        Patient apo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN).build();
        Patient apo2 = new PatientBuilder(new DateTime(1000), EditType.ARCHIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN).build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        clinicalActorMerge.mergeClinicalActors(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(1, clinicalActorList.size());

        Assert.assertEquals(apo1.getClinicalActors().iterator().next(), clinicalActorList.get(0));

        apo1 = new PatientBuilder(new DateTime(0), EditType.ARCHIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN).build();
        apo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId2", "aa2", ActorRole.ATTENDING_PHYSICIAN).build();
        patientSet = new PatientSet(apo1, apo2);
        clinicalActorMerge.mergeClinicalActors(patientSet);

        mergedPatient = patientSet.mergedPatient;

        clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(1, clinicalActorList.size());

        Assert.assertEquals(apo2.getClinicalActors().iterator().next(), clinicalActorList.get(0));

        verifySourcesAndParsingDetails(mergedPatient, clinicalActorList);
    }

    //
    // Test two clinical actors that are the same - should merge together.
    //
    @Test
    public void testTwoSameClinicalActors_TestCreationTimeOrder()
    {
        //
        // Make sure that the source is always the one with the latest date
        //
        // This time, make apo2 later in time..
        //
        Patient apo1 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN).build();
        Patient apo2 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN).build();

        PatientSet patientSet = new PatientSet(apo1, apo2);

        clinicalActorMerge.mergeClinicalActors(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(1, clinicalActorList.size());

        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(clinicalActorList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        //
        // Make sure that the source is always the one with the latest date
        //
        // This time, make apo1 later in time..
        //
        apo1 = new PatientBuilder(new DateTime(1000), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN).build();
        apo2 = new PatientBuilder(new DateTime(0), EditType.ACTIVE, "externalId1", "aa1", ActorRole.ATTENDING_PHYSICIAN).build();

        patientSet = new PatientSet(apo1, apo2);

        clinicalActorMerge.mergeClinicalActors(patientSet);

        mergedPatient = patientSet.mergedPatient;

        clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(1, clinicalActorList.size());

        sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo1.getSourceById(clinicalActorList.get(0).getSourceId()));

        parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, clinicalActorList);
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
            UUID clinicalActorId = UUID.randomUUID();
            UUID sourceId = UUID.randomUUID();
            UUID parsingDetailsId = UUID.randomUUID();

            for (int i = 0; i < numberOfDuplicatesOfEachUnique; i++)
            {
                DateTime creationDate = new DateTime(dateInstant++);

                PatientBuilder apoBuilder = new PatientBuilder(creationDate,
                        EditType.ACTIVE,
                        "externalId1" + n,
                        "aa1" + n,
                        ActorRole.ATTENDING_PHYSICIAN,
                        clinicalActorId,
                        sourceId,
                        parsingDetailsId);

                listOfPatientsToMerge.add(apoBuilder.build());

            }
        }

        // Setup Inactive patients
        for(int n=0; n<numberOfUniqueInActive; n++)
        {
            UUID clinicalActorId = UUID.randomUUID();
            UUID sourceId = UUID.randomUUID();
            UUID parsingDetailsId = UUID.randomUUID();

            for (int i = 0; i < numberOfDuplicatesOfEachUnique; i++)
            {
                DateTime creationDate = new DateTime(dateInstant++);

                PatientBuilder apoBuilder = new PatientBuilder(creationDate,
                        EditType.ACTIVE,
                        "externalId1" + n,
                        "aa1" + n,
                        ActorRole.ATTENDING_PHYSICIAN,
                        clinicalActorId,
                        sourceId,
                        parsingDetailsId);

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
            clinicalActorMerge.mergeClinicalActors(patientSet);

            mergedPatient = patientSet.mergedPatient;
        }

        //
        // Step #3 - Verify
        //
        List<ClinicalActor> clinicalActorList = Lists.newArrayList(mergedPatient.getClinicalActors());
        Assert.assertEquals(numberOfUniqueActive, clinicalActorList.size());

        verifySourcesAndParsingDetails(mergedPatient, clinicalActorList);
    }

    protected void verifySourcesAndParsingDetails(Patient mergedPatient, List<ClinicalActor> clinicalActorList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(ClinicalActor clinicalActor: clinicalActorList)
        {
            UUID referencedSource = clinicalActor.getSourceId();
            Assert.assertNotNull(mergedPatient.getSourceById(referencedSource));

            UUID referencedParsingDetail = clinicalActor.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }
}
