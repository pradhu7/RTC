package com.apixio.bizlogic.patient.assembly.allergy;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.*;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Allergy;
import com.apixio.model.patient.Patient;

import java.util.ArrayList;
import java.util.List;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by alarocca on 4/30/20.
 */
public class AllergyAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<Allergy> allergys = (List<Allergy>) apo.getAllergies();
        if (allergys == null || allergys.isEmpty())
            return parts; // empty list

        Patient allergyApo = new Patient();
        List<Allergy> allergyList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (Allergy allergy : allergys)
        {
            AssemblerUtility.addClinicalActors(allergy, apo, allergyApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(allergy, apo, allergyApo);
            AssemblerUtility.addSources(allergy, apo, allergyApo);
            AssemblerUtility.addParsingDetails(allergy, apo, allergyApo);

            allergyList.add(allergy);
        }

        allergyApo.setPatientId(apo.getPatientId());
        allergyApo.setAllergies(allergyList);

        addBatchIdToParsingDetails(apo, allergyApo, allergyApo.getAllergies());

        addExternalIDs(apo, allergyApo);

        parts.add(new Part(PatientAssembly.PatientCategory.ALLERGY.getCategory(), allergyApo));
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
        AllergyMerge.merge(patientSet);
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
