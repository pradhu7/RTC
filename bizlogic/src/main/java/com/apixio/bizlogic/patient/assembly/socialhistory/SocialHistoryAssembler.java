package com.apixio.bizlogic.patient.assembly.socialhistory;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.*;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.ClinicalActorMetaInfo;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.SocialHistory;
import com.apixio.model.patient.Patient;

import java.util.ArrayList;
import java.util.List;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

/**
 * Created by alarocca on 4/30/20.
 */
public class SocialHistoryAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<SocialHistory> socialHistorys = (List<SocialHistory>) apo.getSocialHistories();
        if (socialHistorys == null || socialHistorys.isEmpty())
            return parts; // empty list

        Patient socialHistoryApo = new Patient();
        List<SocialHistory> socialHistoryList = new ArrayList<>();

        ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo);

        for (SocialHistory socialHistory : socialHistorys)
        {
            AssemblerUtility.addClinicalActors(socialHistory, apo, socialHistoryApo, clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);
            AssemblerUtility.addEncounters(socialHistory, apo, socialHistoryApo);
            AssemblerUtility.addSources(socialHistory, apo, socialHistoryApo);
            AssemblerUtility.addParsingDetails(socialHistory, apo, socialHistoryApo);

            socialHistoryList.add(socialHistory);
        }

        socialHistoryApo.setPatientId(apo.getPatientId());
        socialHistoryApo.setSocialHistories(socialHistoryList);

        addBatchIdToParsingDetails(apo, socialHistoryApo, socialHistoryApo.getSocialHistories());

        addExternalIDs(apo, socialHistoryApo);

        parts.add(new Part(PatientAssembly.PatientCategory.SOCIAL_HISTORY.getCategory(), socialHistoryApo));
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
        SocialHistoryMerge.merge(patientSet);
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
