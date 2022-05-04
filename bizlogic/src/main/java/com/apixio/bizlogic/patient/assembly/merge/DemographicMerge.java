package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.demographic.EmptyDemographicUtil;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.MappedBaseObjectMerger;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Gender;

public class DemographicMerge extends MergeBase
{
    public static void mergeDemographics(PatientSet patientSet)
    {
        mergeDemo(patientSet);
        mergeContact(patientSet);
        mergeMeta(patientSet);
    }

    private static void mergeDemo(PatientSet patientSet)
    {
        MappedBaseObjectMerger demographicsMerger = new MappedBaseObjectMerger();
        demographicsMerger.addBaseObject(patientSet.basePatient.getPrimaryDemographics());
        demographicsMerger.addBaseObjects(patientSet.basePatient.getAlternateDemographics());
        demographicsMerger.addBaseObject(patientSet.additionalPatient.getPrimaryDemographics());
        demographicsMerger.addBaseObjects(patientSet.additionalPatient.getAlternateDemographics());

        for (BaseObject demographicsObject : demographicsMerger.getBaseObjects())
        {
            patientSet.mergedPatient.addAlternateDemographics((Demographics) demographicsObject);
        }

        patientSet.mergedPatient.setPrimaryDemographics(combine(patientSet.mergedPatient.getAlternateDemographics()));

        for (BaseObject demographicsObject : demographicsMerger.getBaseObjects())
        {
            reconcileReferenceParsingDetailId(demographicsObject, patientSet);
            reconcileReferenceSourceId(demographicsObject, patientSet);

            // no deep copy of sources, because we don't have real source info..
            AssemblerUtility.addParsingDetails(demographicsObject, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addParsingDetails(demographicsObject, patientSet.additionalPatient, patientSet.mergedPatient);
        }
    }

    private static void mergeContact(PatientSet patientSet)
    {
        MappedBaseObjectMerger contactDetailsMerger = new MappedBaseObjectMerger();
        contactDetailsMerger.addBaseObject(patientSet.basePatient.getPrimaryContactDetails());
        contactDetailsMerger.addBaseObjects(patientSet.basePatient.getAlternateContactDetails());
        contactDetailsMerger.addBaseObject(patientSet.additionalPatient.getPrimaryContactDetails());
        contactDetailsMerger.addBaseObjects(patientSet.additionalPatient.getAlternateContactDetails());

        for (BaseObject contactDetailsObject : contactDetailsMerger.getBaseObjects())
        {
            if (patientSet.mergedPatient.getPrimaryContactDetails() == null)
            {
                patientSet.mergedPatient.setPrimaryContactDetails((ContactDetails) contactDetailsObject);
            }
            else
            {
                patientSet.mergedPatient.addAlternateContactDetails((ContactDetails) contactDetailsObject);
            }
        }

        for (BaseObject contactDetailsObject : contactDetailsMerger.getBaseObjects())
        {
            reconcileReferenceParsingDetailId(contactDetailsObject, patientSet);
            reconcileReferenceSourceId(contactDetailsObject, patientSet);

            AssemblerUtility.addParsingDetails(contactDetailsObject, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addParsingDetails(contactDetailsObject, patientSet.additionalPatient, patientSet.mergedPatient);
        }
    }

    private static void mergeMeta(PatientSet patientSet)
    {
        // initialize the merged patient metadata to the base patient
        patientSet.mergedPatient.setMetadata(MergeUtility.mergeStringMap(
                patientSet.basePatient.getMetadata(), patientSet.additionalPatient.getMetadata()));
    }

    // intended to get sorted demographics out of the merger, so the first should be the newest
    private static Demographics combine(Iterable<Demographics> partialDemographics)
    {
        Demographics combinedDemographics = null;
        for (Demographics pd : partialDemographics)
        {
            if (combinedDemographics == null)
                combinedDemographics = pd;
            else
            {
                if (combinedDemographics.getGender() == Gender.UNKNOWN)
                    combinedDemographics.setGender(pd.getGender());
                if (combinedDemographics.getDateOfBirth() == null)
                    combinedDemographics.setDateOfBirth(pd.getDateOfBirth());
                if (EmptyDemographicUtil.isEmpty(combinedDemographics.getName()))
                    combinedDemographics.setName(pd.getName());
            }
        }

        return combinedDemographics;
    }
}
