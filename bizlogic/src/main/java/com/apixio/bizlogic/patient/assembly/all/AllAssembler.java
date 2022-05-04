package com.apixio.bizlogic.patient.assembly.all;

import java.util.ArrayList;
import java.util.List;

import com.apixio.bizlogic.patient.assembly.merge.AllMerge;
import com.apixio.dao.patient2.PatientUtility;
import com.apixio.model.patient.Patient;
import com.apixio.model.assembly.Part;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;

public class AllAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient identityApo)
    {
        addBatchIdToParsingDetails(identityApo, identityApo, identityApo.getProblems());
        addBatchIdToParsingDetails(identityApo, identityApo, identityApo.getProcedures());
        addBatchIdToParsingDetails(identityApo, identityApo, identityApo.getEncounters());

        List<Part<Patient>> parts = new ArrayList<>();

        parts.add(new Part(PatientAssembly.PatientCategory.ALL.getCategory(), PatientUtility.getSourceFileArchiveUUID(identityApo).toString(), identityApo));

        removeEventMetadata(identityApo);

        return parts;
    }

    @Override
    public PartEnvelope<Patient> merge(MergeInfo mergeInfo, PartEnvelope<Patient> p1, PartEnvelope<Patient> p2)
    {
        if (mergeInfo.getMergeScope() == MergeScope.PARTS_ONLY)
        {
            long    ts = (p1.ts > p2.ts) ? p1.ts : p2.ts;
            Patient p  = (p1.ts > p2.ts) ? p1.part : p2.part;

            return new PartEnvelope<Patient>(p, ts);
        }
        else
        {
            return new PartEnvelope<Patient>(merge(new PatientSet(p1.part, p2.part, false)), 0L);
        }
    }

    @Override
    public void prerequisiteMerges(PatientSet patientSet)
    {
        // nothing to do!!!
    }

    @Override
    public void actualMerge(PatientSet patientSet)
    {
        AllMerge.merge(patientSet);

        removeEventMetadata(patientSet.mergedPatient);
    }
}
