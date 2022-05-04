package com.apixio.bizlogic.patient.assembly.prescription;

import java.util.ArrayList;
import java.util.List;

import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.EncounterMerge;
import com.apixio.bizlogic.patient.assembly.merge.ParsingDetailMerge;
import com.apixio.bizlogic.patient.assembly.merge.SourceMerge;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.PrescriptionMerge;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Prescription;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by alarocca on 4/30/20.
 */
public class PrescriptionAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<Prescription> prescriptions = (List<Prescription>) apo.getPrescriptions();
        if (prescriptions == null || prescriptions.isEmpty())
            return parts; // empty list

        Patient prescriptionApo = new Patient();
        List<Prescription> prescriptionList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (Prescription prescription : prescriptions)
        {
            AssemblerUtility.addClinicalActors(prescription, apo, prescriptionApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(prescription, apo, prescriptionApo);
            AssemblerUtility.addSources(prescription, apo, prescriptionApo);
            AssemblerUtility.addParsingDetails(prescription, apo, prescriptionApo);

            prescriptionList.add(prescription);
        }

        prescriptionApo.setPatientId(apo.getPatientId());
        prescriptionApo.setPrescriptions(prescriptionList);

        addBatchIdToParsingDetails(apo, prescriptionApo, prescriptionApo.getPrescriptions());

        addExternalIDs(apo, prescriptionApo);

        parts.add(new Part(PatientAssembly.PatientCategory.PRESCRIPTION.getCategory(), prescriptionApo));
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
        PrescriptionMerge.merge(patientSet);
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
