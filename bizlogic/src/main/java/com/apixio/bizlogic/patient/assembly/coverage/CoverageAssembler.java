package com.apixio.bizlogic.patient.assembly.coverage;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.CoverageMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Coverage;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CoverageAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        if (CoverageMerge.hasNoCoverage(apo))
        {
            return parts;
        }

        Map<String, Map<Source, List<Coverage>>> coverageByHealthPlan = CoverageMerge.groupByHealthPlanAndSourceInitial(apo);

        if (coverageByHealthPlan.isEmpty()) {
            throw new IllegalArgumentException("No coverage has a health plan");
        }

        for (String healthPlan : coverageByHealthPlan.keySet())
        {
            for (Map.Entry<Source, List<Coverage>> entry : coverageByHealthPlan.get(healthPlan).entrySet())
            {
                Source source = entry.getKey();
                CoverageMerge.restrictCoverageToInterval(entry.getValue(), source.getDciStart(), source.getDciEnd());
            }

            CoverageMerge.mergeHealthPlanData(coverageByHealthPlan.get(healthPlan));
        }

        for (String partID : coverageByHealthPlan.keySet())
        {
            Patient summaryPatient = CoverageMerge.assembleSummaryPatient(Arrays.asList(coverageByHealthPlan.get(partID)), apo);

            summaryPatient.setPatientId(apo.getPatientId());
            AssemblerUtility.addExternalIDs(apo, summaryPatient);

            parts.add(new Part(PatientAssembly.PatientCategory.COVERAGE.getCategory(), partID, summaryPatient));
        }

        return parts;
    }

    @Override
    public PartEnvelope<Patient> merge(MergeInfo mergeInfo, PartEnvelope<Patient> p1, PartEnvelope<Patient> p2)
    {
        return new PartEnvelope<Patient>(merge(new PatientSet(p1.part, p2.part, false)), 0L);
    }

    @Override
    public void prerequisiteMerges(PatientSet patientSet)
    {
        // Nothing to do!!!
    }

    @Override
    public void actualMerge(PatientSet patientSet)
    {
        CoverageMerge.mergeCoverage(patientSet, false);
    }
}
