package com.apixio.bizlogic.patient.assembly.immunization;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.*;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Administration;
import com.apixio.model.patient.Patient;

import java.util.ArrayList;
import java.util.List;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by alarocca on 4/30/20.
 */
public class ImmunizationAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<Administration> administrations = (List<Administration>) apo.getAdministrations();
        if (administrations == null || administrations.isEmpty())
            return parts; // empty list

        Patient administrationApo = new Patient();
        List<Administration> administrationList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (Administration administration : administrations)
        {
            AssemblerUtility.addClinicalActors(administration, apo, administrationApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(administration, apo, administrationApo);
            AssemblerUtility.addSources(administration, apo, administrationApo);
            AssemblerUtility.addParsingDetails(administration, apo, administrationApo);

            administrationList.add(administration);
        }

        administrationApo.setPatientId(apo.getPatientId());
        administrationApo.setAdministrations(administrationList);

        addBatchIdToParsingDetails(apo, administrationApo, administrationApo.getAdministrations());

        addExternalIDs(apo, administrationApo);

        parts.add(new Part(PatientAssembly.PatientCategory.IMMUNIZATION.getCategory(), administrationApo));
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
        AdministrationMerge.merge(patientSet);
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
