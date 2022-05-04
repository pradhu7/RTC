package com.apixio.bizlogic.patient.assembly.clinicalactor;

import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;
import com.apixio.bizlogic.patient.assembly.SummaryTestUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by dyee on 4/27/17.
 */
public class ClinicalActorAssemblerTest
{
    ClinicalActorAssembler clinicalActorAssembler;
    PatientJSONParser parser;

    @Before
    public void init()
    {
        clinicalActorAssembler = new ClinicalActorAssembler();
        parser = new PatientJSONParser();
    }

    protected Patient generateTestPatient()
    {
        Patient p = new Patient();
        p.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(10)));

        List<ClinicalActor> clinicalActors = SummaryTestUtils.createClinicalActors(5);

        for(ClinicalActor actor : clinicalActors) {
            p.addClinicalActor(actor);
        }

        return p;
    }

    protected Patient generateTestPatientWithDuplicateClinicalActors(UUID uuid)
    {
        Patient p = new Patient();
        p.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(10)));

        List<ClinicalActor> clinicalActors = SummaryTestUtils.createLogicallyDuplicateActors(uuid,5);

        for(ClinicalActor actor : clinicalActors) {
            p.addClinicalActor(actor);
        }

        return p;
    }

    @Test
    public void testSeperate() {
        Patient patient = generateTestPatient();
        List<Part<Patient>> partList = clinicalActorAssembler.separate(patient);

        assertEquals(5, partList.size());

        //
        // Create a seen map, so we can see if we've seen all of the clinical actors in the org Patient
        //
        Map<UUID, Boolean> seenMap = new HashMap<>();
        for(ClinicalActor ca : patient.getClinicalActors()) {
            seenMap.put(ca.getClinicalActorId(), Boolean.FALSE);
        }

        // Test various conditions
        for(Part<Patient> patientPart : partList) {
            ClinicalActor clinicalActor = patientPart.getPart().getClinicalActors().iterator().next();
            assertEquals("clinicalActor", patientPart.getCategory());
            assertEquals(patientPart.getID(), clinicalActor.getPrimaryId().getId()+"^"+clinicalActor.getPrimaryId().getAssignAuthority());
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
            assertTrue(test);
        }
    }

    @Test
    public void testMerge() {
        Patient patient1 = generateTestPatient();
        Patient patient2 = generateTestPatient();

        Patient mergedPatient = clinicalActorAssembler.merge(null, new PartEnvelope<Patient>(patient1, 0L), new PartEnvelope<Patient>(patient2, 0L)).part;

        int count = 0;

        Iterator<ClinicalActor> mergedPatientCAIterator = mergedPatient.getClinicalActors().iterator();
        while(mergedPatientCAIterator.hasNext()) {
            mergedPatientCAIterator.next();
            count++;
        }

        assertEquals(10, count);

        for(ClinicalActor actor : patient1.getClinicalActors()) {
            assertNotNull(mergedPatient.getClinicalActorById(actor.getClinicalActorId()));
        }

        for(ClinicalActor actor : patient2.getClinicalActors()) {
            assertNotNull(mergedPatient.getClinicalActorById(actor.getClinicalActorId()));
        }
    }

    @Test
    public void testDuplicate() {
        UUID uuid = UUID.randomUUID();
        Patient patient1 = generateTestPatientWithDuplicateClinicalActors(uuid);
        Patient patient2 = generateTestPatientWithDuplicateClinicalActors(uuid);

        Patient mergedPatient = clinicalActorAssembler.merge(null, new PartEnvelope<Patient>(patient1, 0L), new PartEnvelope<Patient>(patient2, 0L)).part;

        int count = 0;

        Iterator<ClinicalActor> mergedPatientCAIterator = mergedPatient.getClinicalActors().iterator();
        while(mergedPatientCAIterator.hasNext()) {
            ClinicalActor ca = mergedPatientCAIterator.next();
            System.out.println(ca.getInternalUUID());
            count++;
        }

        assertEquals(1, count);

        uuid = UUID.randomUUID();
        patient1 = generateTestPatientWithDuplicateClinicalActors(uuid);

        uuid = UUID.randomUUID();
        patient2 = generateTestPatientWithDuplicateClinicalActors(uuid);

        mergedPatient = clinicalActorAssembler.merge(null, new PartEnvelope<Patient>(patient1, 0L), new PartEnvelope<Patient>(patient2, 0L)).part;

        count = 0;

        mergedPatientCAIterator = mergedPatient.getClinicalActors().iterator();
        while(mergedPatientCAIterator.hasNext()) {
            ClinicalActor ca = mergedPatientCAIterator.next();
            System.out.println(ca.getInternalUUID());
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void testDuplicateMap() {
        UUID uuid = UUID.randomUUID();
        Patient patient1 = generateTestPatientWithDuplicateClinicalActors(uuid);
        Patient patient2 = generateTestPatientWithDuplicateClinicalActors(uuid);

        PatientSet patientSet = new PatientSet(patient1, patient2);
        ClinicalActorMerge.mergeClinicalActors(patientSet);

        Map<UUID, UUID> mergeDownMap = patientSet.getMergeContext(ClinicalActorMerge.MERGE_DOWN_MAP);

        Set<UUID> testSet = new HashSet<>();
        for(UUID authUUID: mergeDownMap.values())
        {
            testSet.add(authUUID);
        }

        assertEquals(1, testSet.size());

        testSet = new HashSet<>();
        for(UUID keys: mergeDownMap.keySet())
        {
            testSet.add(keys);
        }

        assertEquals(10, testSet.size());
    }

    @Test
    public void testDuplicateMap2() {
        Patient patient1 = generateTestPatientWithDuplicateClinicalActors(UUID.randomUUID());
        Patient patient2 = generateTestPatientWithDuplicateClinicalActors(UUID.randomUUID());

        PatientSet patientSet = new PatientSet(patient1, patient2);
        ClinicalActorMerge.mergeClinicalActors(patientSet);

        Map<UUID, UUID> mergeDownMap = patientSet.getMergeContext(ClinicalActorMerge.MERGE_DOWN_MAP);

        Set<UUID> testSet = new HashSet<>();
        for(UUID authUUID: mergeDownMap.values())
        {
            testSet.add(authUUID);
        }

        assertEquals(2, testSet.size());

        testSet = new HashSet<>();
        for(UUID keys: mergeDownMap.keySet())
        {
            testSet.add(keys);
        }

        assertEquals(10, testSet.size());
    }
}
