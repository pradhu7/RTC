package com.apixio.bizlogic.patient.assembly.familyhistory;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.*;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.FamilyHistory;
import com.apixio.model.patient.Patient;

import java.util.ArrayList;
import java.util.List;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by alarocca on 4/30/20.
 */
public class FamilyHistoryAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<FamilyHistory> familyHistorys = (List<FamilyHistory>) apo.getFamilyHistories();
        if (familyHistorys == null || familyHistorys.isEmpty())
            return parts; // empty list

        Patient familyHistoryApo = new Patient();
        List<FamilyHistory> familyHistoryList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (FamilyHistory familyHistory : familyHistorys)
        {
            AssemblerUtility.addClinicalActors(familyHistory, apo, familyHistoryApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(familyHistory, apo, familyHistoryApo);
            AssemblerUtility.addSources(familyHistory, apo, familyHistoryApo);
            AssemblerUtility.addParsingDetails(familyHistory, apo, familyHistoryApo);

            familyHistoryList.add(familyHistory);
        }

        familyHistoryApo.setPatientId(apo.getPatientId());
        familyHistoryApo.setFamilyHistories(familyHistoryList);

        addBatchIdToParsingDetails(apo, familyHistoryApo, familyHistoryApo.getFamilyHistories());

        addExternalIDs(apo, familyHistoryApo);

        parts.add(new Part(PatientAssembly.PatientCategory.FAMILY_HISTORY.getCategory(), familyHistoryApo));
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
        FamilyHistoryMerge.merge(patientSet);
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
