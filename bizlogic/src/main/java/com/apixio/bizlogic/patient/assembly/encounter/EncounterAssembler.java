package com.apixio.bizlogic.patient.assembly.encounter;

import java.util.ArrayList;
import java.util.List;

import com.apixio.bizlogic.patient.assembly.merge.CareSiteMerge;
import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.ParsingDetailMerge;
import com.apixio.bizlogic.patient.assembly.merge.SourceMerge;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.EncounterMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.Patient;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addBatchIdToParsingDetails;
import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.addExternalIDs;

public class EncounterAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<Encounter> encounters = (List<Encounter>) apo.getEncounters();

        if (encounters == null || encounters.isEmpty())
            return parts; // empty list

        for (Encounter encounter : encounters)
        {
            String partID = getPartID(encounter);
            if (partID != null && !partID.isEmpty())
            {
                Patient encounterApo = separate(apo, encounter);
                encounterApo.setPatientId(apo.getPatientId());
                parts.add(new Part(PatientAssembly.PatientCategory.ENCOUNTER.getCategory(), partID, encounterApo));
            }
        }

        return parts;
    }

    @Override
    public PartEnvelope<Patient> merge(MergeInfo mergeInfo, PartEnvelope<Patient> p1, PartEnvelope<Patient> p2)
    {
        return new PartEnvelope<Patient>(merge(new PatientSet(p1.part, p2.part)), 0L);
    }

    @Override
    public void prerequisiteMerges(PatientSet patientSet)
    {
        // Need to make sure we do these merges, since the encounter will use
        // merge down metadata that is set by these merges
        ParsingDetailMerge.merge(patientSet);
        SourceMerge.merge(patientSet);
        ClinicalActorMerge.mergeClinicalActors(patientSet);
        CareSiteMerge.merge(patientSet);
    }

    @Override
    public void actualMerge(PatientSet patientSet)
    {
        EncounterMerge.mergeEncounters(patientSet);
    }

    private String getPartID(Encounter e)
    {
        // the key will be a standard form for an external id
        return AssemblerUtility.getPartID(e.getOriginalId());
    }

    // No need to set before add since we are not copying encounters but using the original ones
    private Patient separate(Patient apo, Encounter encounter)
    {
        // the patient object needs to contain the single encounter along with the referenced providers
        Patient encounterPatient = new Patient();
        encounterPatient.addEncounter(encounter);

        AssemblerUtility.addSourceEncounter(encounterPatient, apo, encounter.getSourceEncounter());
        AssemblerUtility.addClinicalActors(encounter, apo, encounterPatient);
        AssemblerUtility.addSources(encounter, apo, encounterPatient);
        AssemblerUtility.addParsingDetails(encounter, apo, encounterPatient);
        AssemblerUtility.addCareSite(encounter, apo, encounterPatient);

        addBatchIdToParsingDetails(apo, encounterPatient, encounterPatient.getEncounters());
        addExternalIDs(apo, encounterPatient);

        return encounterPatient;
    }
}
