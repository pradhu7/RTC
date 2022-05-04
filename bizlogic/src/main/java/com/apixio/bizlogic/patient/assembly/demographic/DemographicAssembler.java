package com.apixio.bizlogic.patient.assembly.demographic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.DemographicMerge;
import com.apixio.bizlogic.patient.assembly.merge.ParsingDetailMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.SourceMerge;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;

import static com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility.*;

public class DemographicAssembler extends PatientAssembler
{
    @Override
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();
        Patient demographicApo    = new Patient();

        if (EmptyDemographicUtil.isEmpty(apo))
            return parts;

        demographicApo.setPatientId(apo.getPatientId());

        addDemo(apo, demographicApo);
        addContact(apo, demographicApo);
        addExternalIDs(apo, demographicApo);

        // make sure to add source after adding demo and contact
        addDemographicSourceFromDemographicOrContact(apo, demographicApo);

        // Whenever you add top level meta, you have to remove the events meta data from it!!!
        addTopLevelMeta(apo, demographicApo);
        removeEventMetadata(demographicApo);

        parts.add(new Part(PatientAssembly.PatientCategory.DEMOGRAPHIC.getCategory(), demographicApo));

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
        // don't change the order.
        // First merge base objects and then coded based objects
        ParsingDetailMerge.merge(patientSet);
        SourceMerge.merge(patientSet);
    }

    @Override
    public void actualMerge(PatientSet patientSet)
    {
        DemographicMerge.mergeDemographics(patientSet);
    }

    private static void addDemo(Patient apo, Patient demographicApo)
    {
        Demographics       primeDemo = apo.getPrimaryDemographics();
        List<Demographics> altDemos  = (List<Demographics>) apo.getAlternateDemographics();

        if (!EmptyDemographicUtil.isEmpty(primeDemo))
        {
            demographicApo.setPrimaryDemographics(primeDemo);
            AssemblerUtility.addParsingDetails(primeDemo, apo, demographicApo);
        }

        if (altDemos != null)
        {
            for (Demographics ad : altDemos)
            {
                if (!EmptyDemographicUtil.isEmpty(ad))
                {
                    demographicApo.addAlternateDemographics(ad);
                    AssemblerUtility.addParsingDetails(ad, apo, demographicApo);
                }
            }
        }
    }

    private static void addTopLevelMeta(Patient apo, Patient separatedApo)
    {
        Map<String,String> metadata = apo.getMetadata();
        separatedApo.getMetadata().putAll(metadata);
    }

    // Takes care of dangling pointers!!!
    private void addDemographicSourceFromDemographicOrContact(Patient apo, Patient separatedApo)
    {
        Set<UUID>            uuids          = new HashSet<>();
        Demographics         primeDemo      = apo.getPrimaryDemographics();
        List<Demographics>   altDemos       = (List<Demographics>) apo.getAlternateDemographics();
        ContactDetails       primeContacts  = apo.getPrimaryContactDetails();
        List<ContactDetails> altContacts    = (List<ContactDetails>) apo.getAlternateContactDetails();

        if (!EmptyDemographicUtil.isEmpty(primeDemo) && primeDemo.getSourceId() != null)
            uuids.add(primeDemo.getSourceId());

        if (altDemos != null)
        {
            for (Demographics ad : altDemos)
            {
                if (!EmptyDemographicUtil.isEmpty(ad) && ad.getSourceId() != null)
                    uuids.add(ad.getSourceId());
            }
        }

        if (!EmptyDemographicUtil.isEmpty(primeContacts) && primeContacts.getSourceId() != null)
            uuids.add(primeContacts.getSourceId());

        if (altContacts != null)
        {
            for (ContactDetails ac : altContacts)
            {
                if (!EmptyDemographicUtil.isEmpty(ac) && ac.getSourceId() != null)
                    uuids.add(ac.getSourceId());
            }
        }

        Source source = getLatestSourceFromTheUuids(apo, new ArrayList<>(uuids));
        if (source == null)
            source = createSourceWithLatestSourceDate(apo);

        separatedApo.addSource(source);

        // Now, reconcile!!!

        UUID reconciledSourceUUID = source.getSourceId();

        if (!EmptyDemographicUtil.isEmpty(primeDemo) && primeDemo.getSourceId() != null)
            primeDemo.setSourceId(reconciledSourceUUID);

        if (altDemos != null)
        {
            for (Demographics ad : altDemos)
            {
                if (!EmptyDemographicUtil.isEmpty(ad) && ad.getSourceId() != null)
                    ad.setSourceId(reconciledSourceUUID);
            }
        }

        if (!EmptyDemographicUtil.isEmpty(primeContacts) && primeContacts.getSourceId() != null)
            primeContacts.setSourceId(reconciledSourceUUID);

        if (altContacts != null)
        {
            for (ContactDetails ac : altContacts)
            {
                if (!EmptyDemographicUtil.isEmpty(ac) && ac.getSourceId() != null)
                    ac.setSourceId(reconciledSourceUUID);
            }
        }
    }
}
