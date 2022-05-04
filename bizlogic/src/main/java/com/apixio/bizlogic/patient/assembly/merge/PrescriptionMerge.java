package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.Medication;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Prescription;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by alarocca on 5/5/20.
 */
public class PrescriptionMerge extends MergeBase
{
    private static PrescriptionMerge instance = new PrescriptionMerge();

    public static void merge(PatientSet patientSet)
    {
        instance.mergePrescription(patientSet);
    }

    public void mergePrescription(PatientSet patientSet)
    {
        Map<String, String> seenMap = new HashMap<>();

        merge(patientSet.basePatient, patientSet, seenMap);
        merge(patientSet.additionalPatient, patientSet, seenMap);
    }

    private void merge(Patient apo, PatientSet patientSet, Map<String, String> seenMap)
    {
        Iterable<Prescription> patientPrescriptions = apo.getPrescriptions();
        dedupClinicalActors(apo, patientSet);

        if (patientPrescriptions!=null)
        {
            Iterator<Prescription> it = patientPrescriptions.iterator();
            while (it.hasNext())
            {
                Prescription prescription = it.next();
                String partId = shouldProcess(prescription);

                if(!partId.isEmpty())
                {
                    //clinical actors and encounters need to be reconciled before computing the identity function
                    reconcileReferenceClinicalActorId(prescription, patientSet);
                    reconcileReferenceEncounterId(prescription, patientSet);

                    if (!seenMap.containsKey(partId))
                    {
                        reconcileReferenceSourceId(prescription, patientSet);
                        reconcileReferenceParsingDetailId(prescription, patientSet);

                        patientSet.mergedPatient.addPrescription(prescription);

                        AssemblerUtility.addClinicalActors(prescription, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addClinicalActors(prescription, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addEncounters(prescription, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addEncounters(prescription, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addParsingDetails(prescription, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addParsingDetails(prescription, patientSet.additionalPatient, patientSet.mergedPatient);

                        AssemblerUtility.addSources(prescription, patientSet.basePatient, patientSet.mergedPatient);
                        AssemblerUtility.addSources(prescription, patientSet.additionalPatient, patientSet.mergedPatient);

                        seenMap.put(partId, partId);
                    }
                }
            }
        }
    }

    // TODO: I re-implemented the toString for Medication because it wasn't correctly identifying duplicates
    // probably due to differing sourceId and parsingDetailsId values, which shouldn't affect the to String
    // This same problem will affect medications loaded encounters or clinical actors as those will have new UUIDs
    // We should improve merge across the board to better identify data that was identical in it's uploaded form.
    private String medicationToString(Medication medication) {
        String medicationCodeString =  medication.getCode() == null ? null : clinicalCodeToStringValue(medication.getCode());

        StringBuilder sb = new StringBuilder();
        sb.append("brandName").append(medication.getBrandName())
                .append("genericName").append(medication.getGenericName())
                .append("ingredients").append(medication.getIngredients())
                .append("strength").append(medication.getStrength())
                .append("form").append(medication.getForm())
                .append("routeOfAdministration").append(medication.getRouteOfAdministration())
                .append("units").append(medication.getUnits())
                .append("code").append(medicationCodeString);

        return sb.toString();
    }

    @Override
    protected String getIdentity(BaseObject baseObject)
    {
        Prescription prescription = (Prescription)baseObject;

        String metadata = getConsistentOrderedMetaData(prescription.getMetadata());
        String codeString =  prescription.getCode() == null ? null : clinicalCodeToStringValue(prescription.getCode());
        String medicationString = prescription.getAssociatedMedication() == null ? null : medicationToString(prescription.getAssociatedMedication());
        StringBuilder sb = new StringBuilder();
        sb.append("isActivePrescription").append(prescription.isActivePrescription())
                .append("amount").append(prescription.getAmount())
                .append("associatedMedication").append(medicationString)
                .append("dosage").append(prescription.getDosage())
                .append("endDate").append(prescription.getEndDate())
                .append("fillDate").append(prescription.getFillDate())
                .append("frequency").append(prescription.getFrequency())
                .append("prescriptionDate").append(prescription.getPrescriptionDate())
                .append("quantity").append(prescription.getQuantity())
                .append("refillsRemaining").append(prescription.getRefillsRemaining())
                .append("sig").append(prescription.getSig())
                .append("primaryClinicalActorId").append(prescription.getPrimaryClinicalActorId())
                .append("supplementaryClinicalActorIds").append(getConsistentOrderedList(prescription.getSupplementaryClinicalActorIds()))
                .append("sourceEncounter").append(prescription.getSourceEncounter())
                .append("code").append(codeString)
                .append("codeTranslations").append(getConsistentOrderedClinicalCodes(prescription.getCodeTranslations()))
                .append("originalId").append(AssemblerUtility.getIdentity(prescription.getOriginalId()))
                .append("metadata").append(metadata);

        return sb.toString();
    }
}