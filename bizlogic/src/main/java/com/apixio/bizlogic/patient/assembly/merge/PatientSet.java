package com.apixio.bizlogic.patient.assembly.merge;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.apixio.bizlogic.patient.assembly.utility.MappedObjectMerger;
import com.apixio.model.patient.ExternalID;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.Patient;

public class PatientSet
{
    public PatientSet(Patient patient1, Patient patient2)
    {
        this(patient1, patient2, true);
    }

    public PatientSet(Patient patient1, Patient patient2, boolean addNewSource)
    {
        orderPatients(patient1, patient2);
        mergedPatient = new Patient();

        // Patient Id should always be the same, take the base patient Id,
        // and include it in the merge.
        mergedPatient.setPatientId(basePatient.getPatientId());
        mergeExternalIDs(this);

        if (addNewSource) mergedPatient.addSource(AssemblerUtility.createSourceWithLatestSourceDate(basePatient));
    }

    private void orderPatients(Patient patient1, Patient patient2)
    {
        DateTime patient1SourceDate = AssemblerUtility.getLatestSourceDate(patient1);
        DateTime patient2SourceDate = AssemblerUtility.getLatestSourceDate(patient2);

        if (patient1SourceDate.isEqual(patient2SourceDate))
        {
            //
            // Use parsing details source date, for tie breaking.
            //
            DateTime patient1ParsingDetailsSourceDate = AssemblerUtility.getLatestSourceDateFromParsingDetails(patient1);
            DateTime patient2ParsingDetailsSourceDate = AssemblerUtility.getLatestSourceDateFromParsingDetails(patient2);

            if(patient1ParsingDetailsSourceDate.isEqual(patient2ParsingDetailsSourceDate))
            {
                // tie break if the source date is the same, we will use the parsing detail source date
                // update: we always sort in ASC order, so patient2 is newer than patient1
                setPatients(patient2, patient1);
            }
            else if (patient1ParsingDetailsSourceDate.isAfter(patient2ParsingDetailsSourceDate))
            {
                setPatients(patient1, patient2);
            }
            else
            {
                setPatients(patient2, patient1);
            }
        }
        else if (patient1SourceDate.isAfter(patient2SourceDate))
        {
            setPatients(patient1, patient2);
        }
        else
        {
            setPatients(patient2, patient1);
        }

    }

    public <T> void addMergeContext(String key, T value)
    {
        mergeContext.put(key, value);
    }

    public <T> T getMergeContext(String key)
    {
        return (T) mergeContext.get(key);
    }

    private void setPatients(Patient basePatient, Patient additionalPatient)
    {
        this.basePatient       = basePatient;
        this.additionalPatient = additionalPatient;
    }

    private void mergeExternalIDs(PatientSet patientSet)
    {
        MappedObjectMerger externalIdMerger = new MappedObjectMerger();

        externalIdMerger.addObject(patientSet.basePatient.getPrimaryExternalID());
        externalIdMerger.addObjects(patientSet.basePatient.getExternalIDs());
        externalIdMerger.addObject(patientSet.additionalPatient.getPrimaryExternalID());
        externalIdMerger.addObjects(patientSet.additionalPatient.getExternalIDs());

        for (Object externalIdObject : externalIdMerger.getObjects())
        {
            if (patientSet.mergedPatient.getPrimaryExternalID() == null)
            {
                patientSet.mergedPatient.setPrimaryExternalID((ExternalID) externalIdObject);
            }
            else
            {
                patientSet.mergedPatient.addExternalId((ExternalID) externalIdObject);
            }
        }
    }

    public Patient basePatient;          // new object
    public Patient additionalPatient;    // old object
    public Patient mergedPatient;

    private Map<String, Object> mergeContext = new HashMap<>();
}
