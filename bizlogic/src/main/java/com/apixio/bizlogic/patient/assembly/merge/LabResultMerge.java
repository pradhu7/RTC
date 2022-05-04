package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.LabResult;
import com.apixio.model.patient.Patient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Updated by alarocca on 5/5/20.
 */
public class LabResultMerge extends MergeBase
{
    private static LabResultMerge instance = new LabResultMerge();

    public static void merge(PatientSet patientSet)
    {
        instance.mergeLabResult(patientSet);
    }

    public void mergeLabResult(PatientSet patientSet)
    {
        Map<String, String> seenMap = new HashMap<>();

        merge(patientSet.basePatient, patientSet, seenMap);
        merge(patientSet.additionalPatient, patientSet, seenMap);
    }

    private void merge(Patient apo, PatientSet patientSet, Map<String, String> seenMap)
    {
        Iterable<LabResult> patientLabResults = apo.getLabs();
        dedupClinicalActors(apo, patientSet);

        if (patientLabResults!=null)
        {
            Iterator<LabResult> it = patientLabResults.iterator();
            while (it.hasNext())
            {
                LabResult labResult = it.next();
                String partId = shouldProcess(labResult);
                if(!partId.isEmpty())
                {
                    //clinical actors and encounters need to be reconciled before computing the identity function
                    reconcileReferenceClinicalActorId(labResult, patientSet);
                    reconcileReferenceEncounterId(labResult, patientSet);

                    if (!seenMap.containsKey(partId))
                    {
                        reconcileReferenceSourceId(labResult, patientSet);
                        reconcileReferenceParsingDetailId(labResult, patientSet);

                        patientSet.mergedPatient.addLabResult(labResult);

                        AssemblerUtility.addClinicalActors(labResult, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addClinicalActors(labResult, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addEncounters(labResult, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addEncounters(labResult, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addParsingDetails(labResult, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addParsingDetails(labResult, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addSources(labResult, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addSources(labResult, patientSet.additionalPatient, patientSet.mergedPatient);

                        seenMap.put(partId, partId);
                    }
                }
            }
        }
    }

    @Override
    protected String getIdentity(BaseObject baseObject)
    {
        LabResult labResult = (LabResult)baseObject;

        String metadata = getConsistentOrderedMetaData(labResult.getMetadata());
        String codeString =  labResult.getCode() == null ? null : clinicalCodeToStringValue(labResult.getCode());
        String specimenCodeString =  labResult.getSpecimen() == null ? null : clinicalCodeToStringValue(labResult.getSpecimen());
        String panelCodeString =  labResult.getPanel() == null ? null : clinicalCodeToStringValue(labResult.getPanel());
        String superPanelCodeString =  labResult.getSuperPanel() == null ? null : clinicalCodeToStringValue(labResult.getSuperPanel());

        StringBuilder sb = new StringBuilder();
        sb.append("labName").append(labResult.getLabName())
                .append("value").append(labResult.getValue())
                .append("range").append(labResult.getRange())
                .append("flag").append(labResult.getFlag())
                .append("units").append(labResult.getUnits())
                .append("labNote").append(labResult.getLabNote())
                .append("specimen").append(specimenCodeString)
                .append("panel").append(panelCodeString)
                .append("superPanel").append(superPanelCodeString)
                .append("sampleDate").append(labResult.getSampleDate())
                .append("careSiteId").append(labResult.getCareSiteId())
                .append("otherDates").append(labResult.getOtherDates())
                .append("sequenceNumber").append(labResult.getSequenceNumber())
                .append("primaryClinicalActorId").append(labResult.getPrimaryClinicalActorId())
                .append("supplementaryClinicalActorIds").append(getConsistentOrderedList(labResult.getSupplementaryClinicalActorIds()))
                .append("sourceEncounter").append(labResult.getSourceEncounter())
                .append("code").append(codeString)
                .append("codeTranslations").append(getConsistentOrderedClinicalCodes(labResult.getCodeTranslations()))
                .append("originalId").append(AssemblerUtility.getIdentity(labResult.getOriginalId()))
                .append("metadata").append(metadata);

        return sb.toString();
    }
}