package com.apixio.bizlogic.patient.assembly.problem;

import java.util.ArrayList;
import java.util.List;

import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.EncounterMerge;
import com.apixio.bizlogic.patient.assembly.merge.ParsingDetailMerge;
import com.apixio.bizlogic.patient.assembly.merge.SourceMerge;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.ProblemMerge;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Problem;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by dyee on 5/8/17.
 */
public class ProblemAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<Problem> problems = (List<Problem>) apo.getProblems();

        if (problems == null || problems.isEmpty())
            return parts; // empty list

        Patient problemApo = new Patient();
        List<Problem> problemList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        // problem apo has no care sites!!!
        for (Problem problem : problems)
        {
            AssemblerUtility.addClinicalActors(problem, apo, problemApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(problem, apo, problemApo);
            AssemblerUtility.addParsingDetails(problem, apo, problemApo);
            AssemblerUtility.addSources(problem, apo, problemApo);

            problemList.add(problem);
        }

        problemApo.setPatientId(apo.getPatientId());
        problemApo.setProblems(problemList);

        addExternalIDs(apo, problemApo);

        addBatchIdToParsingDetails(apo, problemApo, problemApo.getProblems());

        // Problems are not individually addressable. They are all together in a APO
        parts.add(new Part(PatientAssembly.PatientCategory.PROBLEM.getCategory(), problemApo));

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
        ProblemMerge.merge(patientSet);
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
