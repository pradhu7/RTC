package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Problem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by dyee on 5/7/17.
 */
public class ProblemMerge extends MergeBase
{
    private static ProblemMerge instance = new ProblemMerge();

    public static void merge(PatientSet patientSet)
    {
        instance.mergeProblem(patientSet);
    }

    public void mergeProblem(PatientSet patientSet)
    {
        Map<String, String> seenMap = new HashMap<>();

        merge(patientSet.basePatient, patientSet, seenMap);
        merge(patientSet.additionalPatient, patientSet, seenMap);
    }

    private void merge(Patient apo, PatientSet patientSet, Map<String, String> seenMap)
    {
        Iterable<Problem> patientProblems = apo.getProblems();
        dedupClinicalActors(apo, patientSet);

        if (patientProblems!=null)
        {
            Iterator<Problem> it = patientProblems.iterator();
            while (it.hasNext())
            {
                Problem problem = it.next();
                String partId = shouldProcess(problem);
                if(!partId.isEmpty())
                {
                    if (!seenMap.containsKey(partId))
                    {
                        reconcileReferenceClinicalActorId(problem, patientSet);
                        reconcileReferenceEncounterId(problem, patientSet);
                        reconcileReferenceSourceId(problem, patientSet);
                        reconcileReferenceParsingDetailId(problem, patientSet);

                        patientSet.mergedPatient.addProblem(problem);

                        AssemblerUtility.addClinicalActors(problem, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addClinicalActors(problem, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addEncounters(problem, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addEncounters(problem, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addParsingDetails(problem, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addParsingDetails(problem, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addSources(problem, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addSources(problem, patientSet.additionalPatient, patientSet.mergedPatient);

                        seenMap.put(partId, partId);
                    }
                }
            }
        }
    }

    @Override
    protected String getIdentity(BaseObject baseObject)
    {
        Problem problem = (Problem)baseObject;

        String metadata = getConsistentOrderedMetaData(problem.getMetadata());
        String codeString =  problem.getCode() == null ? null : clinicalCodeToStringValue( problem.getCode());
        StringBuilder sb = new StringBuilder();
        sb.append("problemName").append(problem.getProblemName())
                .append("resolutionStatus").append(problem.getResolutionStatus())
                .append("temporalStatus").append(problem.getTemporalStatus())
                .append("startDate").append(problem.getStartDate())
                .append("endDate").append(problem.getEndDate())
                .append("diagnosisDate").append(problem.getDiagnosisDate())
                .append("severity").append(problem.getSeverity())
                .append("primaryClinicalActorId").append(problem.getPrimaryClinicalActorId())
                .append("supplementaryClinicalActorIds").append(getConsistentOrderedList(problem.getSupplementaryClinicalActorIds()))
                .append("sourceEncounter").append(problem.getSourceEncounter())
                .append("code").append(codeString)
                .append("codeTranslations").append(getConsistentOrderedClinicalCodes(problem.getCodeTranslations()))
                .append("originalId").append(AssemblerUtility.getIdentity(problem.getOriginalId()))
                .append("metadata").append(metadata);

        return sb.toString();
    }
}
