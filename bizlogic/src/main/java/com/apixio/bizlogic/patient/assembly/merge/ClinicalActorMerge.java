package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.MappedBaseObjectMerger;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.ClinicalActor;

public class ClinicalActorMerge extends MergeBase
{
    public static final String MERGE_DOWN_MAP = "clinicalActorMergeDownMap";
    public static final String MERGE_DOWN_OBJECT_KEY_MAP = "clinicalActorObjectKeyToAuthoritativeUUIDMap";

    public static void mergeClinicalActors(PatientSet patientSet)
    {
        MappedBaseObjectMerger clinicalActorMerger = new MappedBaseObjectMerger();
        clinicalActorMerger.addBaseObjects(patientSet.basePatient.getClinicalActors());
        clinicalActorMerger.addBaseObjects(patientSet.additionalPatient.getClinicalActors());

        for (BaseObject clinicalActorObject : clinicalActorMerger.getBaseObjects())
        {
            ClinicalActor mergedClinicalActor = (ClinicalActor) clinicalActorObject;

            reconcileReferenceSourceId(mergedClinicalActor, patientSet);
            reconcileReferenceParsingDetailId(mergedClinicalActor, patientSet);

            // no deep copy of sources, because we don't have real source info..
            AssemblerUtility.addParsingDetails(mergedClinicalActor, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addParsingDetails(mergedClinicalActor, patientSet.additionalPatient, patientSet.mergedPatient);

            AssemblerUtility.addSources(mergedClinicalActor, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addSources(mergedClinicalActor, patientSet.additionalPatient, patientSet.mergedPatient);

            patientSet.mergedPatient.addClinicalActor(mergedClinicalActor);
        }

        patientSet.addMergeContext(MERGE_DOWN_MAP, clinicalActorMerger.getMergedBaseObjectUUIDToAuthoritativeUUIDMap());
    }
}
