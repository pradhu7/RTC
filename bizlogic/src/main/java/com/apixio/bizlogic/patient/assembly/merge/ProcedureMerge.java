package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Procedure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by dyee on 5/7/17.
 */
public class ProcedureMerge extends MergeBase
{
    private static ProcedureMerge instance = new ProcedureMerge();

    public static void merge(PatientSet patientSet)
    {
        instance.mergeProcedure(patientSet);
    }

    public void mergeProcedure(PatientSet patientSet)
    {
        Map<String, String> seenMap = new HashMap<>();

        merge(patientSet.basePatient, patientSet, seenMap);
        merge(patientSet.additionalPatient, patientSet, seenMap);
    }

    private void merge(Patient apo, PatientSet patientSet, Map<String, String> seenMap)
    {
        Iterable<Procedure> patientProcedures = apo.getProcedures();
        dedupClinicalActors(apo, patientSet);

        if (patientProcedures!=null)
        {
            Iterator<Procedure> it = patientProcedures.iterator();
            while (it.hasNext())
            {
                Procedure procedure = it.next();
                String partId = shouldProcess(procedure);
                if(!partId.isEmpty())
                {
                    //clinical actors and encounters need to be reconciled before computing the identity function
                    reconcileReferenceClinicalActorId(procedure, patientSet);
                    reconcileReferenceEncounterId(procedure, patientSet);

                    if (!seenMap.containsKey(partId))
                    {
                        reconcileReferenceSourceId(procedure, patientSet);
                        reconcileReferenceParsingDetailId(procedure, patientSet);

                        patientSet.mergedPatient.addProcedure(procedure);

                        AssemblerUtility.addClinicalActors(procedure, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addClinicalActors(procedure, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addEncounters(procedure, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addEncounters(procedure, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addParsingDetails(procedure, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addParsingDetails(procedure, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addSources(procedure, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addSources(procedure, patientSet.additionalPatient, patientSet.mergedPatient);

                        seenMap.put(partId, partId);
                    }
                }
            }
        }
    }

    @Override
    protected String getIdentity(BaseObject baseObject)
    {
        Procedure procedure = (Procedure)baseObject;

        String metadata = getConsistentOrderedMetaData(procedure.getMetadata());
        String codeString =  procedure.getCode() == null ? null : clinicalCodeToStringValue(procedure.getCode());
        String supportingDiagnosisString = getConsistentSupportingDiagnosis(procedure.getSupportingDiagnosis());

        StringBuilder sb = new StringBuilder();
        sb.append("procedureName").append(procedure.getProcedureName())
                .append("performedOn").append(procedure.getPerformedOn())
                .append("endDate").append(procedure.getEndDate())
                .append("interpretation").append(procedure.getInterpretation())
                .append("bodySite").append(procedure.getBodySite() == null ? null : procedure.getBodySite().getAnatomicalStructureName())
                .append("supportingDiagnosis").append(supportingDiagnosisString)
                .append("primaryClinicalActorId").append(procedure.getPrimaryClinicalActorId())
                .append("supplementaryClinicalActorIds").append(getConsistentOrderedList(procedure.getSupplementaryClinicalActorIds()))
                .append("sourceEncounter").append(procedure.getSourceEncounter())
                .append("code").append(codeString)
                .append("codeTranslations").append(getConsistentOrderedClinicalCodes(procedure.getCodeTranslations()))
                .append("originalId").append(AssemblerUtility.getIdentity(procedure.getOriginalId()))
                .append("metadata").append(metadata);

        return sb.toString();
    }
}
