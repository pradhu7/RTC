package com.apixio.bizlogic.patient.assembly.problem;

import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.ProblemMerge;
import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Problem;
import com.apixio.model.patient.ResolutionStatus;
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
public class ProblemMergeMockTest
{
    @InjectMocks
    ProblemMerge problemMerge;

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

            Problem problem = mock(Problem.class);
            when(problem.getEditType()).thenReturn(editType);
            when(problem.getInternalUUID()).thenReturn(problemUUID);
            when(problem.getResolutionStatus()).thenReturn(ResolutionStatus.ACTIVE);
            when(problem.getTemporalStatus()).thenReturn("temporalStatus");
            when(problem.getStartDate()).thenReturn(new DateTime(0));
            when(problem.getEndDate()).thenReturn(new DateTime(1000));
            when(problem.getDiagnosisDate()).thenReturn(new DateTime(0));
            when(problem.getSeverity()).thenReturn("severity");
            when(problem.getPrimaryClinicalActorId()).thenReturn(clinicalActorId);
            when(problem.getSupplementaryClinicalActorIds()).thenReturn(Collections.<UUID>emptyList());
            when(problem.getSourceEncounter()).thenReturn(sourceEncounterId);
            when(problem.getCode()).thenReturn(clinicalCode);
            when(problem.getCodeTranslations()).thenReturn(Collections.<ClinicalCode>emptyList());
            when(problem.getMetadata()).thenReturn(Collections.<String, String>emptyMap());
            when(problem.getSourceId()).thenReturn(sourceId);
            when(problem.getParsingDetailsId()).thenReturn(parsingDetailsId);


            //
            // Setup Encounter
            //
            Encounter encounter = mock(Encounter.class);
            when(encounter.getEncounterId()).thenReturn(sourceEncounterId);

            // Setup return for source and parsing details by id interactions
            when(patientUnderTest.getSourceById(problem.getSourceId())).thenReturn(source);
            when(patientUnderTest.getParsingDetailById(problem.getParsingDetailsId())).thenReturn(parsingDetail);
            when(patientUnderTest.getClinicalActorById(problem.getPrimaryClinicalActorId())).thenReturn(clinicalActor);
            when(patientUnderTest.getEncounterById(problem.getSourceEncounter())).thenReturn(encounter);

            //
            // Setup Clinical Actors...
            //
            List<ClinicalActor> clinicalActorList1 = new ArrayList<>();
            clinicalActorList1.add(clinicalActor);

            //
            // Setup Problem
            //
            List<Problem> problemList = new ArrayList<>();
            problemList.add(problem);

            when(patientUnderTest.getClinicalActors()).thenReturn(clinicalActorList1);
            when(patientUnderTest.getProblems()).thenReturn(problemList);

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

        problemMerge.mergeProblem(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Problem> problemList = Lists.newArrayList(mergedPatient.getProblems());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(3, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(problemList.get(0).getSourceId()));
        Assert.assertEquals(sources.get(2), apo1.getSourceById(problemList.get(1).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(2, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, problemList);
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

        problemMerge.mergeProblem(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Problem> problemList = Lists.newArrayList(mergedPatient.getProblems());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo1.getSourceById(problemList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, problemList);
        verifyClinicalActorAndEncounter(mergedPatient, problemList);
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

        problemMerge.mergeProblem(patientSet);

        Patient mergedPatient = patientSet.mergedPatient;

        List<Problem> problemList = Lists.newArrayList(mergedPatient.getProblems());

        Assert.assertEquals(1, problemList.size());

        //
        // Verify Sources, expect newest first
        //
        List<Source> sources = Lists.newArrayList(mergedPatient.getSources());
        Assert.assertEquals(2, sources.size());
        Assert.assertEquals("Apixio Patient Object Merger", sources.get(0).getSourceSystem());
        Assert.assertEquals(sources.get(1), apo2.getSourceById(problemList.get(0).getSourceId()));

        List<ParsingDetail> parsingDetails = Lists.newArrayList(mergedPatient.getParsingDetails());
        Assert.assertEquals(1, parsingDetails.size());

        verifySourcesAndParsingDetails(mergedPatient, problemList);
        verifyClinicalActorAndEncounter(mergedPatient, problemList);
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
            problemMerge.mergeProblem(patientSet);

            mergedPatient = patientSet.mergedPatient;
        }

        //
        // Step #3 - Verify
        //
        List<Problem> problemList = Lists.newArrayList(mergedPatient.getProblems());
        Assert.assertEquals(numberOfUniqueActive, problemList.size());

        verifySourcesAndParsingDetails(mergedPatient, problemList);
    }

    protected void verifySourcesAndParsingDetails(Patient mergedPatient, List<Problem> problemList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(Problem problem: problemList)
        {
            UUID referencedSource = problem.getSourceId();
            Assert.assertNotNull(mergedPatient.getSourceById(referencedSource));

            UUID referencedParsingDetail = problem.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }

    protected void verifyClinicalActorAndEncounter(Patient mergedPatient, List<Problem> problemList)
    {
        //
        // All sources, and parsing details pointed to by the Problem are present
        //
        for(Problem problem: problemList)
        {
            UUID referencedClinicalActor = problem.getPrimaryClinicalActorId();
            Assert.assertNotNull(mergedPatient.getClinicalActorById(referencedClinicalActor));

            //List<UUID> supplementaryClinicalActors = problem.getSupplementaryClinicalActorIds();
            Assert.assertNotNull(mergedPatient.getClinicalActorById(referencedClinicalActor));

            UUID referencedEncounter = problem.getSourceEncounter();
            Assert.assertNotNull(mergedPatient.getEncounterById(referencedEncounter));
        }
    }
}

