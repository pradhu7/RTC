package com.apixio.bizlogic.patient.assembly.procedure;

import java.util.ArrayList;
import java.util.List;

import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.EncounterMerge;
import com.apixio.bizlogic.patient.assembly.merge.ParsingDetailMerge;
import com.apixio.bizlogic.patient.assembly.merge.SourceMerge;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.ProcedureMerge;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Procedure;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by dyee on 8/15/17.
 */
public class ProcedureAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<Procedure> procedures = (List<Procedure>) apo.getProcedures();
        if (procedures == null || procedures.isEmpty())
            return parts; // empty list

        Patient procedureApo = new Patient();
        List<Procedure> procedureList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (Procedure procedure : procedures)
        {
            AssemblerUtility.addClinicalActors(procedure, apo, procedureApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(procedure, apo, procedureApo);
            AssemblerUtility.addSources(procedure, apo, procedureApo);
            AssemblerUtility.addParsingDetails(procedure, apo, procedureApo);

            procedureList.add(procedure);
        }

        procedureApo.setPatientId(apo.getPatientId());
        procedureApo.setProcedures(procedureList);

        addBatchIdToParsingDetails(apo, procedureApo, procedureApo.getProcedures());

        addExternalIDs(apo, procedureApo);

        parts.add(new Part(PatientAssembly.PatientCategory.PROCEDURE.getCategory(), procedureApo));
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
        ProcedureMerge.merge(patientSet);
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
