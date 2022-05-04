package com.apixio.bizlogic.patient.assembly.encounter;

import com.apixio.bizlogic.BizLogicTestCategories;
import com.apixio.bizlogic.patient.assembly.SummaryTestUtils;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by dyee on 4/28/17.
 */
@Category(BizLogicTestCategories.IntegrationTest.class)
public class EncounterAssemblyTest
{
    EncounterAssembler encounterAssembler;
    PatientJSONParser parser;

    @Before
    public void init()
    {
        encounterAssembler = new EncounterAssembler();
        parser = new PatientJSONParser();
    }

    protected Patient generateTestPatient()
    {
        Patient p = new Patient();
        p.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(10)));

        return p;
    }

    @Test
    public void testSeparate()
    {
        Patient patient1 = generateTestPatient();

        List<Encounter> encounters = SummaryTestUtils.createEncounters(5);
        Map<Encounter, Encounter> encountersToMatch = new HashMap<>();
        for (Encounter encounter : encounters)
        {
            patient1.addEncounter(encounter);
            encountersToMatch.put(encounter, encounter);
        }

        List<Part<Patient>> partList = encounterAssembler.separate(patient1);

        Assert.assertEquals(5, partList.size());


        for (Part<Patient> patientPart : partList)
        {
            Assert.assertEquals("encounter", patientPart.getCategory());
            Iterator<Encounter> encounterIterable = patientPart.getPart().getEncounters().iterator();

            encountersToMatch.remove(encounterIterable.next());

            Assert.assertFalse(encounterIterable.hasNext());
        }

        Assert.assertTrue(encountersToMatch.isEmpty());
    }

    @Test
    public void testMerge()
    {
        //
        // Create Patient #1
        //
        Patient patient1 = generateTestPatient();

        List<Encounter> encounters = SummaryTestUtils.createEncounters(5);
        Map<Encounter, Encounter> encountersToMatch = new HashMap<>();
        for (Encounter encounter : encounters)
        {
            patient1.addEncounter(encounter);
            encountersToMatch.put(encounter, encounter);
        }

        //
        // Create Patient #2
        //
        Patient patient2 = generateTestPatient();

        List<Encounter> encounters2 = SummaryTestUtils.createEncounters(5);
        for (Encounter encounter : encounters2)
        {
            patient2.addEncounter(encounter);
            encountersToMatch.put(encounter, encounter);
        }

        //
        // Merge Patient
        //
        Patient mergedPatient = encounterAssembler.merge(null, new PartEnvelope<Patient>(patient1, 0L), new PartEnvelope<Patient>(patient2, 0L)).part;

        Assert.assertNotNull(mergedPatient);

        for (Encounter mergedEncounter : mergedPatient.getEncounters())
        {
            encountersToMatch.remove(mergedEncounter);
        }

        Assert.assertTrue(encountersToMatch.isEmpty());
    }
}
