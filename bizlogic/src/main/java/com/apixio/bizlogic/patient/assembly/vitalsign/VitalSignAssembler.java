package com.apixio.bizlogic.patient.assembly.vitalsign;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.*;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.BiometricValue;
import com.apixio.model.patient.BiometricValue;
import com.apixio.model.patient.Patient;

import java.util.ArrayList;
import java.util.List;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by alarocca on 4/30/20.
 */
public class VitalSignAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<BiometricValue> biometricValues = (List<BiometricValue>) apo.getBiometricValues();
        if (biometricValues == null || biometricValues.isEmpty())
            return parts; // empty list

        Patient biometricValueApo = new Patient();
        List<BiometricValue> biometricValueList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (BiometricValue biometricValue : biometricValues)
        {
            AssemblerUtility.addClinicalActors(biometricValue, apo, biometricValueApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(biometricValue, apo, biometricValueApo);
            AssemblerUtility.addSources(biometricValue, apo, biometricValueApo);
            AssemblerUtility.addParsingDetails(biometricValue, apo, biometricValueApo);

            biometricValueList.add(biometricValue);
        }

        biometricValueApo.setPatientId(apo.getPatientId());
        biometricValueApo.setBiometricValues(biometricValueList);

        addBatchIdToParsingDetails(apo, biometricValueApo, biometricValueApo.getBiometricValues());

        addExternalIDs(apo, biometricValueApo);

        parts.add(new Part(PatientAssembly.PatientCategory.VITAL_SIGN.getCategory(), biometricValueApo));
        return parts;
    }

    @Override
    public PartEnvelope<Patient> merge(MergeInfo mergeInfo, PartEnvelope<Patient> p1, PartEnvelope<Patient> p2)
    {
        return new PartEnvelope<Patient>(merge(new PatientSet(p1.part, p2.part)), 0L);
    }

    @Override
    public void actualMerge(PatientSet patientSet)
    {
        BiometricValueMerge.merge(patientSet);
    }

    @Override
    public void prerequisiteMerges(PatientSet patientSet)
    {
        SourceMerge.merge(patientSet);
        ParsingDetailMerge.merge(patientSet);
        ClinicalActorMerge.mergeClinicalActors(patientSet);
        EncounterMerge.mergeEncounters(patientSet);
    }
}
