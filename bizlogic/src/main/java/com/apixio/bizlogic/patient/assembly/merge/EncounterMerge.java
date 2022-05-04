package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.MappedBaseObjectMerger;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.Encounter;

public class EncounterMerge extends MergeBase
{
    public static final String MERGE_DOWN_MAP = "encounterMergeDownMap";

    public static void mergeEncounters(PatientSet patientSet)
    {
        MappedBaseObjectMerger encounterMerger = new MappedBaseObjectMerger();
        encounterMerger.addBaseObjects(patientSet.basePatient.getEncounters());
        encounterMerger.addBaseObjects(patientSet.additionalPatient.getEncounters());

        // now for each group of encounters, we create an encounter in the output object
        // NOTE: if this is for a single encounter key, there should be only one group
        for (BaseObject encounterObject : encounterMerger.getBaseObjects())
        {
            Encounter mergedEncounter = (Encounter) encounterObject;

            // Reconcile the Id with the Auth ones
            reconcileReferenceSourceId(mergedEncounter, patientSet);
            reconcileReferenceParsingDetailId(mergedEncounter, patientSet);
            reconcileReferenceClinicalActorId(mergedEncounter, patientSet);
            reconcileContainedCareSiteReferences(mergedEncounter, patientSet);

            // add providers to patient
            AssemblerUtility.addClinicalActors(mergedEncounter, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addClinicalActors(mergedEncounter, patientSet.additionalPatient, patientSet.mergedPatient);

            AssemblerUtility.addEncounters(mergedEncounter, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addEncounters(mergedEncounter, patientSet.additionalPatient, patientSet.mergedPatient);

            AssemblerUtility.addParsingDetails(mergedEncounter, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addParsingDetails(mergedEncounter, patientSet.additionalPatient, patientSet.mergedPatient);

            AssemblerUtility.addSources(mergedEncounter, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addSources(mergedEncounter, patientSet.additionalPatient, patientSet.mergedPatient);

            patientSet.mergedPatient.addEncounter(mergedEncounter);
        }

        ParsingDetailMerge.merge(patientSet);

        patientSet.addMergeContext(MERGE_DOWN_MAP, encounterMerger.getMergedBaseObjectUUIDToAuthoritativeUUIDMap());
    }
}
