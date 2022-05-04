package com.apixio.bizlogic.patient.assembly.labresult;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.*;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.LabResult;
import com.apixio.model.patient.Patient;

import java.util.ArrayList;
import java.util.List;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by alarocca on 4/30/20.
 */
public class LabResultAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<LabResult> labResults = (List<LabResult>) apo.getLabs();
        if (labResults == null || labResults.isEmpty())
            return parts; // empty list

        Patient labResultApo = new Patient();
        List<LabResult> labResultList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (LabResult labResult : labResults)
        {
            AssemblerUtility.addClinicalActors(labResult, apo, labResultApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(labResult, apo, labResultApo);
            AssemblerUtility.addSources(labResult, apo, labResultApo);
            AssemblerUtility.addParsingDetails(labResult, apo, labResultApo);

            labResultList.add(labResult);
        }

        labResultApo.setPatientId(apo.getPatientId());
        labResultApo.setLabs(labResultList);

        addBatchIdToParsingDetails(apo, labResultApo, labResultApo.getLabs());

        addExternalIDs(apo, labResultApo);

        parts.add(new Part(PatientAssembly.PatientCategory.LAB_RESULT.getCategory(), labResultApo));
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
        LabResultMerge.merge(patientSet);
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
