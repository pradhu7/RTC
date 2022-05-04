package com.apixio.bizlogic.patient.assembly.clinicalactor;

import java.util.ArrayList;
import java.util.List;

import com.apixio.bizlogic.patient.assembly.merge.ParsingDetailMerge;
import com.apixio.bizlogic.patient.assembly.merge.SourceMerge;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.Patient;

public class ClinicalActorAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
       List<Part<Patient>>   parts          = new ArrayList<>();
       ClinicalActorMetaInfo clinicalActors = AssemblerUtility.dedupClinicalActors(apo);

       // case of no clinical actor - return empty list
       if (clinicalActors.dedupClinicalActors == null || clinicalActors.dedupClinicalActors.isEmpty())
           return parts; // empty list

       for (ClinicalActor ca : clinicalActors.dedupClinicalActors)
       {
           Patient separatedApo = separate(apo, ca);
           String partID = getPartID(ca);

           if (partID != null && !partID.isEmpty()) // if the key is empty the summary won't be queryable anyway
           {
               parts.add(new Part(PatientAssembly.PatientCategory.CLINICAL_ACTOR.getCategory(), partID, separatedApo));
           }
       }

       return parts;
    }

    public static Patient separate(Patient apo, ClinicalActor clinicalActor)
    {
        Patient clinicalActorPatient = new Patient();

        clinicalActorPatient.addClinicalActor(clinicalActor);

        clinicalActorPatient.setPatientId(apo.getPatientId());

        AssemblerUtility.addParsingDetails(clinicalActor, apo, clinicalActorPatient);
        AssemblerUtility.addSources(clinicalActor, apo, clinicalActorPatient);

        return clinicalActorPatient;
    }

    @Override
    public PartEnvelope<Patient> merge(MergeInfo mergeInfo, PartEnvelope<Patient> p1, PartEnvelope<Patient> p2)
    {
        return new PartEnvelope<Patient>(merge(new PatientSet(p1.part, p2.part)), 0L);
    }

    @Override
    public void prerequisiteMerges(PatientSet patientSet)
    {
        ParsingDetailMerge.merge(patientSet);
        SourceMerge.merge(patientSet);
    }

    @Override
    public void actualMerge(PatientSet patientSet)
    {
        ClinicalActorMerge.mergeClinicalActors(patientSet);
    }

    private String getPartID(ClinicalActor ca)
    {
        return AssemblerUtility.getPartID(ca.getPrimaryId());
    }
}
