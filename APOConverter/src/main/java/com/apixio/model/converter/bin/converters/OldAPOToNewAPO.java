package com.apixio.model.converter.bin.converters;

import com.apixio.XUUID;
import com.apixio.model.converter.exceptions.InsuranceClaimException;
import com.apixio.model.converter.exceptions.OldToNewAPOConversionException;
import com.apixio.model.converter.implementations.*;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.converter.vocabulary.CodingSystemVocabulary;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.*;
import com.apixio.model.patient.*;
import com.apixio.model.patient.Address;
import com.apixio.model.patient.Allergy;
import com.apixio.model.patient.BiometricValue;
import com.apixio.model.patient.CareSite;
import com.apixio.model.patient.CareSiteType;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.DocumentType;
import com.apixio.model.patient.FamilyHistory;
import com.apixio.model.patient.LabResult;
import com.apixio.model.patient.Medication;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.NameType;
import com.apixio.model.patient.Organization;
import com.apixio.model.patient.ParserType;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Prescription;
import com.apixio.model.patient.Problem;
import com.apixio.model.patient.Procedure;
import com.apixio.model.patient.SocialHistory;
import com.apixio.model.patient.Source;
import com.apixio.model.patient.TelephoneNumber;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by jctoledo on 3/10/16.
 */
//TODO - URIs need to be created for everything - still have not done this everywhere
public class OldAPOToNewAPO {
    static final Logger logger = Logger.getLogger(OldAPOToNewAPO.class);
    private Map<String, NewDataProcessingDetail> dataProcessingDetailMap = new HashMap<>();
    private Patient oldAPO = null;
    private NewPatient newPatient = null;


    public OldAPOToNewAPO(Patient anOldAPO) {
        oldAPO = anOldAPO;
        //create instance var for new patient
        newPatient = new NewPatient();
        //call the patient converter
        convertOldAPO(oldAPO);

    }

    private void convertOldAPO(Patient anOldAPO) {
        //pass over the uuids
        if (anOldAPO.getPatientId() != null) {
            UUID oldid = anOldAPO.getPatientId();
            XUUID newID = XUUID.fromString(oldid.toString());
            newPatient.setInternalXUUID(newID);
        }

        //Documents
        if (anOldAPO.getDocuments() != null) {
            Collection<com.apixio.model.owl.interfaces.Document> newDocs = portDocuments(anOldAPO.getDocuments());
            newPatient.setDocuments(newDocs);
        }

        //Procedures
        if (anOldAPO.getProcedures() != null && anOldAPO.getSources() != null) {
            List<Procedure> procedures = ConverterUtils.getOnlyProcedures(anOldAPO.getProcedures(), anOldAPO.getSources());
            Collection<com.apixio.model.owl.interfaces.Procedure> newProcs = portProcedures(procedures);
            newPatient.setMedicalProcedures(newProcs);
        }

        //RA Claims
        if (anOldAPO.getProblems() != null && anOldAPO.getSources() != null) {
            List<Problem> raClaimCandidates = ConverterUtils.getRAClaimCandidatesFromProblems(anOldAPO.getProblems(), anOldAPO.getSources());
            Collection<com.apixio.model.owl.interfaces.RiskAdjustmentInsuranceClaim> newRaClaims = portClaimProblems(raClaimCandidates, anOldAPO.getSources());
            newPatient.setRiskAdjustmentClaims(newRaClaims);
        }

        //FFS claims
        if (anOldAPO.getProcedures() != null && anOldAPO.getSources() != null) {
            List<Procedure> ffsClaimCandidates = ConverterUtils.getFFSCandidatesFromProcedures(anOldAPO.getProcedures());
            Collection<com.apixio.model.owl.interfaces.FeeForServiceInsuranceClaim> newFfsClaims = portClaimProcedures(ffsClaimCandidates);
            newPatient.setFeeForServiceClaims(newFfsClaims);
        }

        //Eligibility
        //the filtering is done pre-loading - there is no explicit concept of "eligibility" when loading

        //Demographics
        if (anOldAPO.getPrimaryDemographics() != null) {
            com.apixio.model.owl.interfaces.Demographics nd = portDemographics(anOldAPO.getPrimaryDemographics());
            newPatient.setDemographics(nd);
        }



        //problems
        if (anOldAPO.getProblems() != null && anOldAPO.getSources() != null) {
            List<Problem> problems = ConverterUtils.getOnlyProblems(anOldAPO.getProblems(), anOldAPO.getSources());
            Collection<com.apixio.model.owl.interfaces.Problem> newProbs = portProblems(problems);
            newPatient.setProblems(newProbs);
        }

        //Prescription
        if (anOldAPO.getPrescriptions() != null) {
            Collection<com.apixio.model.owl.interfaces.Prescription> npxs = portPrescriptions(anOldAPO.getPrescriptions());
            newPatient.setPrescriptions(npxs);
        }
        //allergies
        if (anOldAPO.getAllergies() != null) {
            Collection<com.apixio.model.owl.interfaces.Allergy> newAllergies = portAllergies(anOldAPO.getAllergies());
            newPatient.setAllergies(newAllergies);
        }

        //lab results
        if (anOldAPO.getLabs() != null) {
            Collection<com.apixio.model.owl.interfaces.LabResult> labResults = portLabResults(anOldAPO.getLabs());
            newPatient.setLabResults(labResults);
        }
        //biometricvalues
        if (anOldAPO.getBiometricValues() != null) {
            Collection<com.apixio.model.owl.interfaces.BiometricValue> bioVals = portBiometricValues(anOldAPO.getBiometricValues());
            newPatient.setBiometricValues(bioVals);
        }

        //Immunizations
        if (anOldAPO.getAdministrations() != null) {
            Collection<com.apixio.model.owl.interfaces.Immunization> immunizations = portAdministrations(anOldAPO.getAdministrations());
            newPatient.setImmunizations(immunizations);
        }

        //contact details
        if (anOldAPO.getPrimaryContactDetails() != null) {
            NewContactDetails pcd = portContactDetails(anOldAPO.getPrimaryContactDetails());
            newPatient.setContactDetails(pcd);
        }

        if (anOldAPO.getAlternateContactDetails() != null) {
            Collection<com.apixio.model.owl.interfaces.ContactDetails> ncds = portContactDetails(anOldAPO.getAlternateContactDetails());
            newPatient.setAlternateContactDetailss(ncds);
        }

        //family history
        if (anOldAPO.getFamilyHistories() != null) {
            Collection<com.apixio.model.owl.interfaces.FamilyHistory> nfhc = portFamilyHistories(anOldAPO.getFamilyHistories());
            newPatient.setFamilyHistories(nfhc);
        }

        //social history
        if (anOldAPO.getSocialHistories() != null) {
            Collection<com.apixio.model.owl.interfaces.SocialHistory> nshc = portSocialHistories(anOldAPO.getSocialHistories());
            newPatient.setSocialHistories(nshc);
        }

        //coverage
        if (anOldAPO.getCoverage() != null) {
            Collection<com.apixio.model.owl.interfaces.InsurancePolicy> insurancePolicies = portCoverages(anOldAPO.getCoverage());
            newPatient.setInsurancePolicies(insurancePolicies);
        }
        //encounter
        if (anOldAPO.getEncounters() != null) {
            Collection<com.apixio.model.owl.interfaces.PatientEncounter> encounters = portPatientEncounters(anOldAPO.getEncounters());
            newPatient.setEncounters(encounters);
        }
        //external ids
        if (anOldAPO.getPrimaryExternalID() != null) {
            ExternalIdentifier newExtId = portExternalId(anOldAPO.getPrimaryExternalID());
            newPatient.setOriginalID(newExtId);
        }

        Collection<ExternalIdentifier> otherExternalIds = new ArrayList<>();
        if (anOldAPO.getExternalIDs() != null) {
            for (ExternalID ei : anOldAPO.getExternalIDs()) {
                ExternalIdentifier e = portExternalId(ei);
                if (e != null) {
                    otherExternalIds.add(e);
                }
            }
        }
        newPatient.setOtherOriginalIDss(otherExternalIds);


    }


    private Collection<com.apixio.model.owl.interfaces.FamilyHistory> portFamilyHistories(Iterable<FamilyHistory> a) {
        Collection<com.apixio.model.owl.interfaces.FamilyHistory> rm = new ArrayList<>();
        for (FamilyHistory fh : a) {
            NewFamilyHistory nfh = portFamilyHistory(fh);
            if (nfh != null) {
                rm.add(nfh);
            }
        }
        return rm;
    }


    /**
     * Create a new APO clinical code object from an old APO clinical code
     * and the dataprocessing detail object of its containg object - for example anatomical entity contains a clinical code, so the new clinical code will re-use the container's data processing detail id
     *
     * @param oldCC an old APO clinical code
     * @return a new APO Clinical Code
     */
    private NewClinicalCode portClinicalCode(ClinicalCode oldCC, DataProcessingDetail dpd) {
        NewClinicalCode rm = new NewClinicalCode();
        XUUID id = XUUID.create("NewClinicalCode");
        rm.setInternalXUUID(id);

        //set lastEditTime
        //DateTime now = new DateTime(DateTimeZone.UTC);
        DateTime now = new DateTime();
        Date d = portDateTime(now);
        rm.setLastEditDate(d);

        if (dpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(dpd);
            rm.setDataProcessingDetails(ndpdc);
        }

        // String code;
        if (oldCC.getCode() != null) {
            rm.setClinicalCodeValue(oldCC.getCode());
        } else {
            logger.error("Clinical Code without code value found! "+this.oldAPO.getPatientId());
        }
        // String displayName;
        if (oldCC.getDisplayName() != null) {
            rm.setClinicalCodeDisplayName(oldCC.getDisplayName());
        }
        //now I need to create a clinical coding system object
        NewClinicalCodeSystem nccs = new NewClinicalCodeSystem();
        //create a random XUUID
        XUUID xIdCCs = XUUID.create("clinicalCodingSystem");
        nccs.setInternalXUUID(xIdCCs);
        // String codingSystem;
        if (oldCC.getCodingSystem() != null) {
            nccs.setClinicalCodingSystemName(oldCC.getCodingSystem());
        } else {
            nccs.setClinicalCodingSystemName(null);
            logger.error("Clinical code without coding system found!"+this.oldAPO.getPatientId());
        }
        // String codingSystemOID;
        if (oldCC.getCodingSystemOID() != null) {
            nccs.setClinicalCodingSystemOID(oldCC.getCodingSystemOID());
        } else {
            nccs.setClinicalCodingSystemOID(null);
            logger.debug("No clinical coding system oid found! "+this.oldAPO.getPatientId());
        }
        // String codingSystemVersion;
        if (oldCC.getCodingSystemVersions() != null) {
            nccs.setClinicalCodingSystemVersion(oldCC.getCodingSystemVersions());
        } else {
            nccs.setClinicalCodingSystemVersion(null);
            logger.debug("Clinical code without coding system version found ! "+this.oldAPO.getPatientId());
        }
        //now associate the clinical code to the clinical coding system
        rm.setClinicalCodingSystem(nccs);
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.SocialHistory> portSocialHistories(Iterable<SocialHistory> a) {
        Collection<com.apixio.model.owl.interfaces.SocialHistory> rm = new ArrayList<>();
        for (SocialHistory sh : a) {
            NewSocialHistory nsh = portSocialHistory(sh);
            if (nsh != null) {
                rm.add(nsh);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.Immunization> portAdministrations(Iterable<Administration> a) {
        Collection<com.apixio.model.owl.interfaces.Immunization> rm = new ArrayList<>();
        for (Administration ad : a) {
            NewImmunization ni = portAdministration(ad);
            if (ni != null) {
                rm.add(ni);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.LabResult> portLabResults(Iterable<LabResult> c) {
        Collection<com.apixio.model.owl.interfaces.LabResult> rm = new ArrayList<>();
        for (LabResult cd : c) {
            NewLabResult ncd = portLabResult(cd);
            if (ncd != null) {
                rm.add(ncd);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.ContactDetails> portContactDetails(Iterable<ContactDetails> c) {
        Collection<com.apixio.model.owl.interfaces.ContactDetails> rm = new ArrayList<>();
        for (ContactDetails cd : c) {
            NewContactDetails ncd = portContactDetails(cd);
            if (ncd != null) {
                rm.add(ncd);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.PatientEncounter> portPatientEncounters(Iterable<Encounter> encs) {
        List<com.apixio.model.owl.interfaces.PatientEncounter> rm = new ArrayList<>();
        for (Encounter e : encs) {
            try {
                NewPatientEncounter npe = portPatientEncounter(e);
                rm.add(npe);
            } catch (OldToNewAPOConversionException e1) {
                logger.debug("Could not convert PatientEncounter!"+e);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.InsurancePolicy> portCoverages(Iterable<Coverage> covs) {
        List<com.apixio.model.owl.interfaces.InsurancePolicy> rm = new ArrayList<>();
        for (Coverage c : covs) {
            try {
                NewInsurancePolicy nip = portCoveragePolicy(c);
                rm.add(nip);
            } catch (OldToNewAPOConversionException e) {
                logger.debug("Could not convert Coverage!"+c);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.BiometricValue> portBiometricValues(Iterable<BiometricValue> biometricVals) {
        Collection<com.apixio.model.owl.interfaces.BiometricValue> rm = new ArrayList<>();
        for (BiometricValue bv : biometricVals) {
            try {
                NewBiometricValue nbv = portBiometricValue(bv);
                rm.add(nbv);
            } catch (OldToNewAPOConversionException e) {
                logger.debug("Could not convert Biometric values"+bv);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.Prescription> portPrescriptions(Iterable<Prescription> pxs) {
        Collection<com.apixio.model.owl.interfaces.Prescription> rm = new ArrayList<>();
        for (Prescription p : pxs) {
            try {
                NewPrescription np = portPrescription(p);
                rm.add(np);
            } catch (OldToNewAPOConversionException e) {
                logger.debug("Could not convert PRescription!"+p);
            }
        }
        return rm;
    }

    /**
     * Convert a list of procedure objects - these are really procedures and not actually claims
     *
     * @param procedures the list of procedures to convert
     * @return a collection of newprocedures
     */
    private Collection<com.apixio.model.owl.interfaces.Procedure> portProcedures(Iterable<Procedure> procedures) {
        Collection<com.apixio.model.owl.interfaces.Procedure> rm = new ArrayList<>();
        for (Procedure p : procedures) {
            try {
                NewProcedure np = portProcedure(p);
                rm.add(np);
            } catch (OldToNewAPOConversionException e) {
                logger.warn("Could not convert Procedure! :\n" + p);
            }
        }
        return rm;
    }

    /**
     * Convert a list of problem objects - these are really procedures and not actually claims
     *
     * @param problems a list of problem objects to convert
     * @return a collection of newProblems
     */
    private Collection<com.apixio.model.owl.interfaces.Problem> portProblems(Iterable<Problem> problems) {
        Collection<com.apixio.model.owl.interfaces.Problem> rm = new ArrayList<>();
        for (Problem p : problems) {
            try {
                NewProblem np = portProblem(p);
                rm.add(np);
            } catch (OldToNewAPOConversionException e) {
                logger.warn("Could not convert Problem! :\n" + p);
            }
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.FeeForServiceInsuranceClaim> portClaimProcedures(Iterable<Procedure> procs) {
        Collection<com.apixio.model.owl.interfaces.FeeForServiceInsuranceClaim> rm = new ArrayList<>();
        for (Procedure p : procs) {
            try {
                NewFeeForServiceClaim nffsc = portClaimProcedure(p);
                if (nffsc != null) {
                    rm.add(nffsc);
                }
            } catch (InsuranceClaimException e) {
                logger.error("Found an invalid InsuranceClaim object! See problem id : "+p.getInternalUUID()+" / patient id: "+this.oldAPO.getPatientId());
            } catch (OldToNewAPOConversionException e) {
                logger.debug("Skipping RA Claim Problem : " + p);
            }
        }
        return rm;
    }

    /**
     * Convert an old APO Anatomy object into a new APO's new Anatomical Entity
     *
     * @param a an old APO Anatomy object to convert
     * @return a  new APO Anatomical entity
     */
    private NewAnatomicalEntity portAnatomicalEntity(Anatomy a) {
        NewAnatomicalEntity rm = new NewAnatomicalEntity();
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(a));
        if (a.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(a.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Anatomical entity without medical pro found! " + a.getInternalUUID());
        }
        //parsing details
        NewDataProcessingDetail ndpd = null;
        if (a.getSourceId() != null) {
            UUID source = a.getSourceId();
            UUID parsingDetailId = a.getParsingDetailsId();

            ndpd = createDataProcessingDetail(parsingDetailId, source);
        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = a.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (a.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : a.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("Anatomical entity without alternate external ids");
        }
        //lastEditTime
        if (a.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(a.getLastEditDateTime()));
        }
        //sourceencounter
        if (a.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(a.getSourceEncounter().toString()));
        } else {
            logger.debug("Anatomical entity without source encounter uuid");
        }
        //clinical codes
        if (a.getCode() != null) {
            rm.setCodedEntityClinicalCode(portClinicalCode(a.getCode(), ndpd));
        }
        if (a.getAnatomicalStructureName() != null) {
            rm.setAnatomicalEntityName(a.getAnatomicalStructureName());
        } else {
            logger.debug("anatomy without anatomical structure name! " + a.getInternalUUID());
        }

        //supplementary clinical actor ids
        if (a.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(a.getSupplementaryClinicalActorIds()));
        }
        return rm;
    }

    private Collection<com.apixio.model.owl.interfaces.RiskAdjustmentInsuranceClaim> portClaimProblems(Iterable<Problem> problems, Iterable<Source> sources) {
        Collection<com.apixio.model.owl.interfaces.RiskAdjustmentInsuranceClaim> rm = new ArrayList<>();
        for (Problem p : problems) {
            try {
                NewRiskAdjustmentClaim nrac = portClaimProblem(p, sources);
                if (nrac != null) {
                    rm.add(nrac);
                }
            } catch (InsuranceClaimException e) {
                logger.error("Found an invalid InsuranceClaim object! See problem id : "+p.getInternalUUID()+" / patient id: "+this.oldAPO.getPatientId());
            } catch (OldToNewAPOConversionException e) {
                logger.warn("Skipping Problem : " + p.getInternalUUID());
            }
        }
        return rm;
    }

    private NewDateRange createDateRange(DateTime start, DateTime end) {
        NewDateRange rm = new NewDateRange();
        XUUID id = XUUID.create("NewDateRange");
        rm.setInternalXUUID(id);
        if (start != null) {
            NewDate s = portDateTime(start);
            rm.setStartDate(s);
        } else {
            logger.debug("Date range with null start datetime !");
        }
        if (end != null) {
            NewDate e = portDateTime(end);
            rm.setEndDate(e);
        } else {
            logger.debug("Date range with end datetime !");
        }
        if (rm.getStartDate() == null) {
            String msg = "created daterange with a null start date";
            logger.debug(msg);
            throw new OldToNewAPOConversionException(msg);
        }
        if (rm.getStartDate() == null && rm.getEndDate() == null) {
            String msg = "created null date range object! \n";
            logger.warn(msg);
            throw new OldToNewAPOConversionException(msg);
        }
        return rm;
    }

    /**
     * Create an Apixio Date from a joda datetime
     *
     * @param aDateTime a Joda Datetime
     * @return a new Date object
     */
    private NewDate portDateTime(DateTime aDateTime) {
        //TODO - I am not dealing with time zones or doing any sort of smart conversion here
        NewDate rm = new NewDate();
        XUUID id = XUUID.create("NewDate");
        rm.setInternalXUUID(id);
        if (aDateTime != null) {
            rm.setDateValue(aDateTime);
        } else {
            logger.warn("Datetime with a null value was found! :\n");
        }
        //TODO -check that I am dealing with timezones correctly with Vishnu
        String timezoneid = aDateTime.getChronology().getZone().getID();
        if (timezoneid != null) {
            rm.setTimeZone(timezoneid);
        }
        return rm;
    }

    /**
     * Converts a list of old APO documents into new ones
     *
     * @param documents a list of old apo documents
     * @return a list of new APO docs
     */
    private Collection<com.apixio.model.owl.interfaces.Document> portDocuments(Iterable<Document> documents) {
        Collection<com.apixio.model.owl.interfaces.Document> rm = new ArrayList<>();
        for (Document oldDoc : documents) {
            try {
                NewDocument nd = portDocument(oldDoc);
                rm.add(nd);
            } catch (OldToNewAPOConversionException e) {
                e.printStackTrace();
            }
        }
        return rm;
    }

    /**
     * Convert an old APO document into a new one
     *
     * @param oldDoc an old APO document
     * @return a new APO document
     * @throws OldToNewAPOConversionException if required fields are not found
     */
    private NewDocument portDocument(Document oldDoc) {
        NewDocument rm = new NewDocument();
        ///source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = oldDoc.getParsingDetailsId();
        if (oldDoc.getSourceId() != null) {
            UUID source = oldDoc.getSourceId();

            ndpd = createDataProcessingDetail(parsingDetailId, source);
        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = oldDoc.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (oldDoc.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : oldDoc.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (oldDoc.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(oldDoc.getLastEditDateTime()));
        }
        //internalid
        XUUID id = ConverterUtils.createXUUIDFromInternalUUID(oldDoc);
        rm.setInternalXUUID(id);
        //primaryclinical actor
        if (oldDoc.getPrimaryClinicalActorId() != null) {
            logger.debug("New documents are no longer coded entities ! - no primary clinical actors");
        } else {
            logger.debug("Found problem without a clinical actor id! " + oldDoc.getPrimaryClinicalActorId());
        }
        //suppl clinical actors
        if (oldDoc.getSupplementaryClinicalActorIds() != null) {
            logger.debug("New documents are no longer coded entities ! - no supplementary clinical actors");
        }
        //source encounter
        if (oldDoc.getSourceEncounter() != null) {
            logger.debug("New documents are no longer coded entities ! - no encounters");
        }
        if (oldDoc.getCode() != null) {
            logger.debug("New documents are no longer coded entities ! - no codes");
        }

        //METADATA fields: TODO - finish me
        // DOCUMENT_AUTHOR, DOCUMENT_SIGNED_STATUS, PROVIDER_TITLE, "ENCOUNTER_DATE, DOCUMENT_UPDATE, DOCUMENT_FORMAT, PROVIDER_TYPE

        //document date
        if (oldDoc.getDocumentDate() != null) {
            Date d = portDateTime(oldDoc.getDocumentDate());
            rm.setDocumentDate(d);
        }
        //document title
        if (oldDoc.getDocumentTitle() != null) {
            rm.setTitle(oldDoc.getDocumentTitle());
        }

        if (oldDoc.getDocumentContents() != null) {
            //get the first element of document contents
            //this is where the file metadata is stored : uri, hash, mimetype, etc.
            try {
                //get the file
                DocumentContent dc = oldDoc.getDocumentContents().get(0);
                if (dc != null) {
                    NewFile nf = portDocumentContent(oldDoc.getInternalUUID(), dc);
                    if (rm.getDataProcessingDetails() != null) {
                        if (rm.getDataProcessingDetails().iterator().next().getSources() != null) {
                            rm.getDataProcessingDetails().iterator().next().getSources().iterator().next().setSourceFile(nf);
                        }
                    }


                    Iterable<ParsingDetail> parsingDetIrtbl = oldAPO.getParsingDetails();
                    ParsingDetail pd = getParsingDetail(parsingDetailId, parsingDetIrtbl);
                    String text_extracted = oldDoc.getMetaTag("text_extracted");
                    String mimetype = dc.getMimeType();

                    String stringContent = oldDoc.getStringContent();
                    //TODO - finish new Page conversion
                    /*
                    List<com.apixio.model.ocr.page.Page> oldPages = getPagesFromDocumentContent(stringContent);
                    List<com.apixio.model.owl.interfaces.Page> newPages = portPages(oldPages, pd, text_extracted, mimetype, oldDoc.getInternalUUID());

                    if (newPages != null) {
                        rm.setDocumentPages(newPages);
                    } else {
                        throw new OldToNewAPOConversionException("Null new pages found : \n" + oldDoc);
                    }
                    */
                }
            } catch (IndexOutOfBoundsException e) {
                throw new OldToNewAPOConversionException("Document without documentcontent \n" + oldDoc);
            }
        } else {
            throw new OldToNewAPOConversionException("Null DocumentContent found !");
        }
        return rm;
    }

    /**
     * Retrieve a parting detail object from a list given a uuid
     *
     * @param needle   the uuid to search for
     * @param haystack the list of parsing details to search through
     * @return an old APO parsing detail
     */
    private ParsingDetail getParsingDetail(UUID needle, Iterable<ParsingDetail> haystack) {
        for (ParsingDetail pd : haystack) {
            if (pd.getParsingDetailsId().equals(needle)) {
                return pd;
            }
        }
        return null;
    }

    /**
     * Create old APO pages from a string
     *
     * @param stringContent the string content of a old APO document
     * @return a list of old APO pages
     */
    private List<com.apixio.model.ocr.page.Page> getPagesFromDocumentContent(String stringContent) {
        List<com.apixio.model.ocr.page.Page> rm;
        if (stringContent != null) {
            try {
                JAXBContext contextObj = JAXBContext.newInstance(com.apixio.model.ocr.page.Ocrextraction.class);
                //create an inputStream
                InputStream stream = new ByteArrayInputStream(stringContent.getBytes(StandardCharsets.UTF_8));
                Unmarshaller um = contextObj.createUnmarshaller();
                com.apixio.model.ocr.page.Ocrextraction ocrextraction = (com.apixio.model.ocr.page.Ocrextraction) um.unmarshal(stream);
                com.apixio.model.ocr.page.Ocrextraction.Pages ps = ocrextraction.getPages();
                rm = ps.getPage();
            } catch (JAXBException e) {
                throw new OldToNewAPOConversionException(e);
            }
            return rm;
        } else {
            logger.debug("Null string content passed in!");
        }
        return null;

    }

    /**
     * Create a list of newPage objects given a list of old pages
     *
     * @param oldPages       the list of old APO pages
     * @param pd             the parsing details
     * @param text_extracted the text_extracted metadata field
     * @return a list of new apo pages
     */
    private List<com.apixio.model.owl.interfaces.Page> portPages(List<com.apixio.model.ocr.page.Page> oldPages, ParsingDetail pd, String text_extracted, String mimetype, UUID oldDocId) {
        //TODO - check me
        List<com.apixio.model.owl.interfaces.Page> rm = new ArrayList<>();
        if (oldPages != null) {
            for (com.apixio.model.ocr.page.Page p : oldPages) {
                try {
                    NewPage np = portPage(p, pd, text_extracted, mimetype, oldDocId);
                    if (np != null) {
                        //set page number
                        Integer pn = p.getPageNumber().intValue();
                        if (pn > 0) {
                            logger.debug("Negative pagenumber found!! \n");
                        }
                        np.setPageNumber(pn);
                        rm.add(np);
                    } else {
                        logger.debug("Null page returned by page port");
                    }
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert page!");
                }
            }
        } else {
            logger.debug("Null old pages passed in!");
        }
        return rm;

    }

    /**
     * Create a page from an oldPage object
     *
     * @param oldPage        a page object created in hadoop-jobs
     * @param pd             the parsing details object of the document where this page came from
     * @param text_extracted the metadata field
     * @param mimeType       the mimetype
     * @return a new APO page
     */
    //TODO - check that I am covering all usecases
    private NewPage portPage(com.apixio.model.ocr.page.Page oldPage, ParsingDetail pd, String text_extracted, String mimeType, UUID oldDocId) {
        NewPage rm = null;
        List<TextRendering> textRenderings = new ArrayList<>();

        //List<ImageRendering> imageRenderings = new ArrayList<>();
        com.apixio.model.ocr.page.ExtractedText et = oldPage.getExtractedText();
        if (et != null) {
            String content = et.getContent();
            //TODO - check if content is null
            if (pd != null) {
                //TODO - CHECKME
                if ((pd.getParser().name().equals("CCD") || pd.getParser().equals("CCR")) && mimeType.length() > 0) {
                    //then its html
                    TextRendering tr = new NewTextRendering();
                    tr.setTextRenderingType("HTML");
                    tr.setTextRenderingValue(content);
                    textRenderings.add(tr);
                }
                //TODO - CHECKME
                if (text_extracted != null && text_extracted.length() > 0 && (pd.getParser().name().equals("BLUE_BUTTON") || pd.getParser().name().equals("ECW"))) {
                    TextRendering tr = new NewTextRendering();
                    tr.setTextRenderingType("PLAINTEXT");
                    tr.setTextRenderingValue(content);
                    textRenderings.add(tr);
                }
            }

            //TXT,RTF,DOC,DOCX,ODT
            //TODO - CHECKME
            if ((text_extracted != null) &&
                    (mimeType.toLowerCase().contains("doc") ||
                            mimeType.toLowerCase().contains("msword") ||
                            mimeType.toLowerCase().contains("docx") ||
                            mimeType.toLowerCase().contains("odt") ||
                            mimeType.toLowerCase().contains("txt") ||
                            mimeType.toLowerCase().contains("rtf"))
                    ) {
                //string content has html rendering
                if (content != null) {
                    TextRendering htmlTr = new NewTextRendering();
                    htmlTr.setTextRenderingType("HTML");
                    htmlTr.setTextRenderingValue(content);
                    textRenderings.add(htmlTr);
                }
                //text_extracted has plain text
                TextRendering ptTr = new NewTextRendering();
                ptTr.setTextRenderingType("PLAINTEXT");
                ptTr.setTextRenderingValue(text_extracted);
                textRenderings.add(ptTr);
            }
            /**
             * TODO - Will skip image renderings for now
             */
            if (mimeType.toLowerCase().contains("pdf")) {
                ImageRendering ir = new NewImageRendering();
                String id = ConverterUtils.getPageID(oldDocId, oldPage.getImgType(), oldPage.getPageNumber().intValue());
                logger.debug("skiping pdf files for now!");
                //get the resolution
                //create a file
                // set the mimetype of the file
                // set the file hash
                // set the file URL
                // set the Date
            }

            if (textRenderings.size() > 0) {
                rm.setTextRenderings(textRenderings);
            } else {
                logger.debug("No text renderings could be created for : \n" + oldPage);
            }
        } else {
            logger.debug("Old page without extracted page found! ");
        }

        return rm;
    }

    /**
     * Convert an old APO claim procedure
     *
     * @param p
     * @return
     */
    private NewFeeForServiceClaim portClaimProcedure(Procedure p) {
        NewFeeForServiceClaim rm = new NewFeeForServiceClaim();
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //parsing details
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
             ndpd = createDataProcessingDetail(parsingDetailId, source);
        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("procedure without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }

        if (p.getPrimaryClinicalActorId() !=null){
            ClinicalActor ca = ConverterUtils.getClinicalActorFromIterable(p.getPrimaryClinicalActorId(), this.oldAPO.getClinicalActors());
            if (ca == null) {
                logger.debug("No clinical actor found for Procedure object: \n" + p.getInternalUUID());
                logger.debug("Searched for clinical actor uuid : \n" + p.getPrimaryClinicalActorId());
            } else {
                NewMedicalProfessional nca = portClinicalActor(ca);
                rm.addRenderingProviders(nca);
            }
        }


        //sourceencounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        } else {
            logger.debug("Procedure without source encounter uuid");
        }
        //iterate over supporting diagnosis
        List<com.apixio.model.owl.interfaces.ClinicalCode> nccl = new ArrayList<>();
        Iterator<ClinicalCode> itr = p.getSupportingDiagnosis().iterator();
        while (itr.hasNext()) {
            if (ndpd != null) {
                NewClinicalCode ncc = portClinicalCode(itr.next(), ndpd);
                nccl.add(ncc);
            }
        }
        rm.setDiagnosisCodes(nccl);

        //now set the procedure codes
        if (p.getCode() != null) {
            if (ndpd != null) {
                rm.addServicesRendereds(portClinicalCode(p.getCode(), ndpd));
            }
        }
        //create an annonymous claimable event
        //create an anonymous claimable event
        InsuranceClaimableEvent ice = new NewInsuranceClaimableEvent();
        Collection<InsuranceClaimableEvent> icec = new ArrayList<>();
        icec.add(ice);
        rm.setClaimedEvents(icec);

        NewFeeForServiceInsuranceClaimType nffct = new NewFeeForServiceInsuranceClaimType();
        nffct.setFeeForServiceInsuranceClaimTypeValue(Enum.valueOf(FeeForServiceInsuranceClaimTypeValue.class, "FFS"));
        rm.setFeeForServiceClaimTypeValue(nffct);


        //set the date of service range
        DateRange dr = createDateRange(p.getPerformedOn(), p.getEndDate());
        rm.setDateOfServiceRange(dr);
        if (p.getMetadata() != null && p.getMetadata().size()>0){
            if(p.getMetadata().containsKey("TRANSACTION_DATE") && p.getMetaTag("TRANSACTION_DATE").length()>0){
                DateTimeParser[] parsers = {
                        DateTimeFormat.forPattern( "yyyyMMdd" ).getParser(),
                        DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser() };
                DateTimeFormatter formatter = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter();
                try{
                    rm.setInsuranceClaimProcessingDate(portDateTime(formatter.parseDateTime(p.getMetaTag("TRANSACTION_DATE"))));
                } catch (IllegalArgumentException e) {
                    logger.error("COULD NOT PARSE FFS CLAIM TRANSACTION DATE");
                    logger.error(e.getMessage());
                }
            } else {
                String msg = "found an FFS claim without a transaction date! "+p.getInternalUUID()+" pat : "+this.oldAPO.getPatientId();
                logger.error(msg);
                throw new InsuranceClaimException(msg);
            }
            if(p.getMetadata().containsKey("HPLAN_ID") && p.getMetaTag("HPLAN_ID").length()>0){
                //create an insurance policy that isrt an insurance lcan with identifier
                NewInsurancePolicy nip = new NewInsurancePolicy();
                NewInsurancePlan plan = new NewInsurancePlan();
                NewExternalIdentifier nei = new NewExternalIdentifier();
                nei.setIDValue(p.getMetaTag("HPLAN_ID"));
                nei.setAssignAuthority("HPLAN_ID");
                plan.setInsurancePlanIdentifier(nei);
                nip.setInsurancePlan(plan);
                this.newPatient.addInsurancePolicies(nip);
            }
            if (p.getMetadata().containsKey("HEALTH_EXCHANGE_ID") && p.getMetaTag("HEALTH_EXCHANGE_ID").length()>0){
                //create an original id for this claim
                NewExternalIdentifier nei= new NewExternalIdentifier();
                nei.setIDValue(p.getMetaTag("HEALTH_EXCHANGE_ID"));
                nei.setAssignAuthority("EXCHANGE_ID");
                rm.addOtherOriginalIDss(nei);
            }
            if(p.getMetadata().containsKey("BILLING_PROVIDER_ID") && p.getMetaTag("BILLING_PROVIDER_ID").length()>0){
                //create a medical professional
                NewMedicalProfessional prof = new NewMedicalProfessional();
                NewClinicalRoleType ncrt = new NewClinicalRoleType();
                ncrt.setClinicalRoleTypeValue(ClinicalRoleTypeValue.ORDERING_PHYSICIAN);
                prof.setClinicalRole(ncrt);
                if(p.getMetadata().containsKey("BILLING_PROVIDER_NAME") && p.getMetaTag("BILLING_PROVIDER_NAME").length()>0){
                    NewDemographics  d = new NewDemographics();
                    NewName nn = new NewName();
                    nn.addGivenNames(p.getMetaTag("BILLING_PROVIDER_NAME"));
                    d.setName(nn);
                    prof.setDemographics(d);
                }
                rm.addBillingProviders(prof);
            }

            if(p.getMetadata().containsKey("DELETE_INDICATOR") && p.getMetaTag("DELETE_INDICATOR").length()> 0){
                if(p.getMetaTag("DELETE_INDICATOR").equalsIgnoreCase("true")){
                    rm.setDeleteIndicator(true);
                    rm.setAddIndicator(false);
                } else {
                    rm.setDeleteIndicator(false);
                    rm.setAddIndicator(true);
                }
            } else if(p.getMetadata().containsKey("ADD_DELETE_FLAG")){
                rm.setDeleteIndicator(true);
                rm.setAddIndicator(false);}
            else {
                rm.setDeleteIndicator(false);
                rm.setAddIndicator(true);
            }

            if (p.getMetadata().containsKey("PROVIDER_TYPE") && p.getMetaTag("PROVIDER_TYPE").length()>0){
                String [] code = p.getMetaTag("PROVIDER_TYPE").split("\\^");
                if(code.length == 4) {
                    //"I^I^PROV_TYPE_FFS^2.25.986811684062365523470895812567751821389" (Fact code value, Fact code name, Code system name, Code system oid)
                    String codeVal = code[0];
                    String codeName = code[1];
                    String codeSysName = code[2];
                    String codeSysOid = code[3];
                    if (codeVal.length() == 0 || codeName.length()==0){
                        String m = "found a bill type code without a code value or code name! see procedure : "+p.getInternalUUID()+" pat : "+this.oldAPO.getPatientId();
                        logger.debug(m);
                    }else {
                        // this needs to become a coding system
                        NewClinicalCode ncc = new NewClinicalCode();
                        ncc.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                        ncc.setClinicalCodeValue(codeVal);
                        ncc.setClinicalCodeDisplayName(codeName);
                        //Now create a new clinical coding system
                        NewClinicalCodeSystem nccs = new NewClinicalCodeSystem();
                        nccs.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                        nccs.setClinicalCodingSystemName(codeSysName);
                        nccs.setClinicalCodingSystemOID(codeSysOid);
                        //connect the two
                        ncc.setClinicalCodingSystem(nccs);
                        rm.setProviderType(ncc);
                    }
                } else {
                    String msg = "Found FFS claim without a valid Provider type code ! "+p.getInternalUUID() +" pat: "+ this.oldAPO.getPatientId() +" end date :"+ p.getEndDate();
                    logger.debug(msg);
                }
            } else {
                String msg = "No provider type found for FFS  insurance claim ! procedure id : " + p.getInternalUUID() + " patient id : " + this.oldAPO.getPatientId() + " lasteditdatestamp :"+p.getLastEditDateTime() +" end date of service :" +p.getEndDate();
                logger.debug(msg);
            }


            if (p.getMetadata().containsKey("BILL_TYPE") && p.getMetaTag("BILL_TYPE").length()>0){
                String [] code = p.getMetaTag("BILL_TYPE").split("\\^");
                if(code.length == 4) {
                    //"I^I^PROV_TYPE_FFS^2.25.986811684062365523470895812567751821389" (Fact code value, Fact code name, Code system name, Code system oid)
                    String codeVal = code[0];
                    String codeName = code[1];
                    String codeSysName = code[2];
                    String codeSysOid = code[3];
                    if (codeVal.length() == 0 || codeName.length()==0){
                        String m = "found a bill type code without a code value or code name! see procedure : "+p.getInternalUUID()+" pat : "+this.oldAPO.getPatientId();
                        logger.debug(m);
                    }else {
                        NewClinicalCode ncc = new NewClinicalCode();
                        ncc.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                        ncc.setClinicalCodeValue(codeVal);
                        ncc.setClinicalCodeDisplayName(codeName);
                        //Now create a new clinical coding system
                        NewClinicalCodeSystem nccs = new NewClinicalCodeSystem();
                        nccs.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                        nccs.setClinicalCodingSystemName(codeSysName);
                        nccs.setClinicalCodingSystemOID(codeSysOid);
                        //connect the two
                        ncc.setClinicalCodingSystem(nccs);
                        rm.setBillType(ncc);
                    }
                } else {
                    String msg = "Found FFS claim without a valid bill type code ! "+p.getInternalUUID() +" pat: "+ this.oldAPO.getPatientId()+" end date :"+ p.getEndDate();;
                    logger.debug(msg);
                }
            }
        } else {
            String msg = "Found FFS claim without any metdata fields! ";
            logger.error(msg);
            throw new InsuranceClaimException(msg);
        }
        return rm;
    }

    /**
     * Create a risk adjustment claim from a procedure object that has CMS_KNOWN as its
     * sourcetype
     *
     * @param p an old APO procedure object
     * @return a new APO Risk Adjusted Claim
     */
    private NewRiskAdjustmentClaim portClaimProblem(Problem p, Iterable<Source> sources) {
        //perform pre-conversion checks
        boolean isStandardRAPSClaim = ConverterUtils.isStandardRAPSClaim(p);
        boolean isNonStandardRAPSClaim = ConverterUtils.isNonStandardRAPSClaim(p);
        boolean isMAO004Claim = ConverterUtils.isMAO004Claim(p);
        boolean problablyRAPS = false;

        List<Boolean> boolz = new ArrayList<>();
        boolz.add(isStandardRAPSClaim);
        boolz.add(isMAO004Claim);
        boolz.add(isNonStandardRAPSClaim);
        int trueCount = 0;
        int falseCount = 0;
        for (Boolean b:boolz){
            if(b){
                trueCount ++;
            }else{
                falseCount ++;
            }
        }

        if (falseCount != 2 && trueCount != 1){
            String msg = "Found an Insurance Claim (problem APO) that cannot be uniquely identified as being one of (Standard RAPS,non-standard RAPS, MAO004 claim)! see problem with id : "+p.getInternalUUID().toString()
                    +" with patient uuid : "+this.oldAPO.getPatientId();
            if(p.getEndDate() != null){
               msg += " end date of service : "+ p.getEndDate();
            }
            msg += " and lasteditTimestamp  : "+p.getLastEditDateTime();
            logger.warn(msg);
            problablyRAPS = true;
        }
        // now we know what we are dealing with
        NewRiskAdjustmentClaim rm = new NewRiskAdjustmentClaim();
        // USE A FUCKIN LOGICAL ID HHER !
        //construct it as the method is called

        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));

        //parsing details
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }

        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            String msg = "Null DataProcessingDetail object found for patient/problem uuid: "+this.oldAPO.getPatientId()+"/"+p.getInternalUUID();
            logger.error(msg);
            throw new OldToNewAPOConversionException(msg);
        }

        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            NewExternalIdentifier nei = portExternalId(ei);
            nei.setInternalXUUID(rm.getInternalXUUID());
            rm.setOriginalID(nei);
        }

        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id for patient : "+this.oldAPO.getPatientId());
                }
            }
            rm.setOtherOriginalIDss(otherList);
        } else {
            logger.debug("problem without alternate external ids for patient : "+this.oldAPO.getPatientId());
        }

        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        } else {
            logger.error("found problem object without a last edit time! see problem with uuid : "+p.getInternalUUID().toString());
            throw new OldToNewAPOConversionException("found problem object without a last edit time! see problem with uuid : "+p.getInternalUUID().toString());
        }

        //sourceencounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        } else {
            logger.debug("problem without source encounter uuid");
        }
        try {
            DateRange dr = createDateRange(p.getStartDate(), p.getEndDate());
            rm.setDateOfServiceRange(dr);
        } catch (OldToNewAPOConversionException e) {
            String msg ="Could not set date range for ra claim : problem id "+p.getInternalUUID()+" patient id : " +this.oldAPO.getPatientId();
            logger.error(msg);
            throw new InsuranceClaimException(msg);
        }

        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            Collection<com.apixio.model.owl.interfaces.ClinicalCode> res = rm.addDiagnosisCodes(ncc);
            if (res == null || res.size() == 0){
                String msg = "Error while adding a diagnosis code!";
                logger.error(msg);
                throw new InsuranceClaimException(msg);
            }
        }else {
            logger.debug("Found an RA claim with out a diagnosis code ! problem id: "+p.getInternalUUID()+" problem enddate :" +p.getEndDate()+" patient id : "+this.oldAPO.getPatientId());
        }

        //get the sourceSystem of this claim
        if (p.getSourceId() != null) {
            String ss = ConverterUtils.getSourceSystem(p.getSourceId(), sources);
            if (ss != null) {
                rm.setSourceSystem(ss);
            }
        }

        if ((p.getMetadata() != null && isStandardRAPSClaim == true) || (p.getMetadata() != null && isNonStandardRAPSClaim == true || problablyRAPS == true)) {

            //create an anonymous claimable event
            InsuranceClaimableEvent ice = new NewInsuranceClaimableEvent();
            Collection<InsuranceClaimableEvent> icec = new ArrayList<>();
            icec.add(ice);
            rm.setClaimedEvents(icec);

            //create a CMSClaimValidationResult
            List<String> error_code_value_list = new ArrayList<>();
            String provider_type = "", transaction_date = "", file_mode = "", plan_number = "", overpayment_id = "", payment_year = "", patient_control_number = "", corrected_hic_number = "";

            //get the value of the error code
            //find all keys that contain the string "ERROR" in them
            List<String> errorCodes = ConverterUtils.getListOfErrorCodesFromMetadata(p.getMetadata());

            //init the indicators
            rm.setAddIndicator(false);
            rm.setDeleteIndicator(false);
            //set the delete indicator
            boolean deleteInd = ConverterUtils.hasRAPSDeleteIndicator(p.getMetadata());
            rm.setDeleteIndicator(deleteInd);
            if(deleteInd == false){
                rm.setAddIndicator(true);
            }else{
                rm.setAddIndicator(false);
            }
            if(rm.getAddIndicator().equals(rm.getDeleteIndicator())){
                int banaa =2;
            }

            //now create a list of CMSValidationErrors
            Collection<CMSValidationError> validationErrors = portCMSValidationErrors(errorCodes, p.getLastEditDateTime());
            //now create  a cmsvalidation result object and attach as many of the following instance vars as possible
            NewCMSClaimValidationResult nccvr = new NewCMSClaimValidationResult();
            nccvr.setLastEditDate(portDateTime(p.getLastEditDateTime()));
            nccvr.setCMSValidationErrors(validationErrors);
            if (p.getMetadata().containsKey("TRANSACTION_DATE")) {
                if (p.getMetadata().get("TRANSACTION_DATE") != null && p.getMetadata().get("TRANSACTION_DATE").length() > 0) {
                    transaction_date = p.getMetadata().get("TRANSACTION_DATE");
                    DateTimeParser[] parsers = {
                            DateTimeFormat.forPattern( "yyyyMMdd" ).getParser(),
                            DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser() };
                    DateTimeFormatter formatter = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter();
                    nccvr.setValidatedClaimTransactionDate(portDateTime(formatter.parseDateTime(transaction_date)));
                }
            } else {
                String msg = "Found an RA claim without a transaction date : " + p.getInternalUUID() + " patient " + this.oldAPO.getPatientId();
                if(isStandardRAPSClaim) {
                    logger.error(msg);
                    throw new InsuranceClaimException(msg);
                }
                if(isNonStandardRAPSClaim){
                    logger.debug(msg);
                }
            }
            if (p.getMetadata().containsKey("FILE_MODE")) {
                if (p.getMetadata().get("FILE_MODE") != null && p.getMetadata().get("FILE_MODE").length() > 0) {
                    file_mode = p.getMetadata().get("FILE_MODE");
                    nccvr.setValidatedClaimFileMode(file_mode);
                }
            }
            if (p.getMetadata().containsKey("PLAN_NUMBER")) {
                if (p.getMetadata().get("PLAN_NUMBER") != null && p.getMetadata().get("PLAN_NUMBER").length() > 0) {
                    plan_number = p.getMetadata().get("PLAN_NUMBER");
                    nccvr.setValidatedClaimPlanNumber(plan_number);
                }
            }
            if (p.getMetadata().containsKey("PROVIDER_TYPE")){
                String pt = p.getMetaTag("PROVIDER_TYPE");
                // this needs to become a coding system
                NewClinicalCode ncc = new NewClinicalCode();
                ncc.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                ncc.setClinicalCodeValue(pt);
                ncc.setClinicalCodeDisplayName(pt);
                //Now create a new clinical coding system
                NewClinicalCodeSystem nccs = new NewClinicalCodeSystem();
                nccs.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                nccs.setClinicalCodingSystemName("PROV_TYPE_RAPS");
                nccs.setClinicalCodingSystemOID(CodingSystemVocabulary.CodingSystemOID.PROV_TYPE_RAPS.toString());
                //connect the two
                ncc.setClinicalCodingSystem(nccs);
                rm.setProviderType(ncc);
            } else {
                if (isStandardRAPSClaim) {
                    String msg = "No provider type found for STANDARD RAPS insurance claim ! problem id : " + p.getInternalUUID() + " patient id : " + this.oldAPO.getPatientId() + " lasteditdatestamp :"+p.getLastEditDateTime() +" end date of service :" +p.getEndDate();
                    logger.error(msg);
                    throw new InsuranceClaimException(msg);
                } else {
                    String msg = "No provider type found for NON STANDARD RAPS insurance claim ! problem id : " + p.getInternalUUID() + " patient id : " + this.oldAPO.getPatientId() + " lasteditdatestamp :"+p.getLastEditDateTime() +" end date of service :" +p.getEndDate();
                    logger.debug(msg);
                }
            }
            if (p.getMetadata().containsKey("OVERPAYMENT_ID")) {
                if (p.getMetadata().get("OVERPAYMENT_ID") != null && p.getMetadata().get("OVERPAYMENT_ID").length() > 0) {
                    overpayment_id = p.getMetadata().get("OVERPAYMENT_ID");
                    NewExternalIdentifier nei = new NewExternalIdentifier();
                    nei.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                    nei.setIDValue(overpayment_id);
                    nei.setAssignAuthority("CMS");
                    nccvr.setClaimOverpaymentIdentifier(nei);
                }
            }

            if (p.getMetadata().containsKey("PAYMENT_YEAR")) {
                if (p.getMetadata().get("PAYMENT_YEAR") != null && p.getMetadata().get("PAYMENT_YEAR").length() > 0) {
                    payment_year = p.getMetadata().get("PAYMENT_YEAR");
                    nccvr.setValidatedClaimPaymentYear(payment_year);
                }

            }
            if (p.getMetadata().containsKey("PATIENT_CONTROL_NUMBER")) {
                if (p.getMetadata().get("PATIENT_CONTROL_NUMBER") != null && p.getMetadata().get("PATIENT_CONTROL_NUMBER").length() > 0) {
                    patient_control_number = p.getMetadata().get("PATIENT_CONTROL_NUMBER");
                    nccvr.setValidatedClaimControlNumber(patient_control_number);
                }
            }
            if (p.getMetadata().containsKey("CORRECTED_HIC_NUMBER")) {
                if (p.getMetadata().get("CORRECTED_HIC_NUMBER") != null && p.getMetadata().get("CORRECTED_HIC_NUMBER").length() > 0) {
                    corrected_hic_number = p.getMetadata().get("CORRECTED_HIC_NUMBER");
                    nccvr.setValidatedClaimPlanNumber(corrected_hic_number);
                }
            }

            NewRiskAdjustmentInsuranceClaimType nraict = new NewRiskAdjustmentInsuranceClaimType();
            nraict.setRiskAdjustmentInsuranceClaimTypeValue(Enum.valueOf(RiskAdjustmentInsuranceClaimTypeValue.class, "RAPS"));
            rm.setRiskAdjustmentInsuranceClaimType(nraict);
            rm.setCMSClaimValidationResult(nccvr);

        } else if(p.getMetadata() != null && isMAO004Claim == true) {
            //IF THIS RA CLAIM IS A MAO-004 RA claim
            //create a patient encounter
            NewPatientEncounter npe = new NewPatientEncounter();
            UUID encID = null;
            //NewPatientEncounterType
            /** USING ENCOUNTER ICN AS LOGICAL ID for claim **/
            if(p.getMetadata().containsKey("ENCOUNTER_ICN") && p.getMetaTag("ENCOUNTER_ICN") != null && p.getMetaTag("ENCOUNTER_ICN").length()>0){
                String encidStr = p.getMetaTag("ENCOUNTER_ICN");
                encID = UUID.nameUUIDFromBytes(encidStr.getBytes());
                XUUID raXID = XUUID.create("NewRiskAdjustmentClaim", encID, true, false);
                rm.setInternalXUUID(raXID);

                XUUID encXID = XUUID.create("NewPatientEncounter", encID, true, false);
                npe.setInternalXUUID(encXID);
                rm.setSourceEncounterId(encXID);

                /**
                 * add an original identifier to pass the encounter ICN
                 */
                NewExternalIdentifier nei = new NewExternalIdentifier();
                nei.setInternalXUUID(encXID);
                nei.setIDValue(p.getMetadata().get("ENCOUNTER_ICN"));
                nei.setAssignAuthority("ENCOUNTER_ICN");

                rm.addOtherOriginalIDss(nei);


            } else {
                //log aan error
                logger.error("Found a patient encounter without a corresponding encounter ICN for patient: "+ oldAPO.getPatientId()+" problem id : "+p.getInternalUUID() );
                throw new OldToNewAPOConversionException("Found a patient encounter without a corresponding encounter ICN for patient: "+ oldAPO.getPatientId()+" problem id : "+p.getInternalUUID() );
            }
            //original encounter icn
            String oeicn = p.getMetaTag("ORIGINAL_ENCOUNTER_ICN");
            if (oeicn != null){
                /**
                 * add an original identifier to pass the origincal encounter ICN
                 */
                encID = UUID.nameUUIDFromBytes(oeicn.getBytes());
                XUUID encXID = XUUID.create("ORIGINAL_ENCOUNTER_ICN", encID, true, false);
                NewExternalIdentifier nei = new NewExternalIdentifier();
                nei.setInternalXUUID(encXID);
                nei.setIDValue(oeicn);
                nei.setAssignAuthority("ORIGINAL_ENCOUNTER_ICN");
                rm.addOtherOriginalIDss(nei);
            }

            if (p.getMetadata().containsKey("ENCOUNTER_CLAIM_TYPE")){
                String pt = p.getMetaTag("ENCOUNTER_CLAIM_TYPE");
                // this needs to become a coding system
                NewClinicalCode ncc = new NewClinicalCode();
                ncc.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                ncc.setClinicalCodeValue(pt);
                ncc.setClinicalCodeDisplayName(pt);
                //Now create a new clinical coding system
                NewClinicalCodeSystem nccs = new NewClinicalCodeSystem();
                nccs.setLastEditDate(portDateTime(p.getLastEditDateTime()));
                nccs.setClinicalCodingSystemName("PROV_TYPE_RAPS");
                nccs.setClinicalCodingSystemOID(CodingSystemVocabulary.CodingSystemOID.PROV_TYPE_RAPS.toString());
                //connect the two
                ncc.setClinicalCodingSystem(nccs);
                rm.setProviderType(ncc);
            } else {
                String msg = "No provider type found for MAO insurance claim ! problem id : " + p.getInternalUUID() + " patient id : " + this.oldAPO.getPatientId() + " lasteditdatestamp :"+p.getLastEditDateTime() +" end date of service :" +p.getEndDate();
                logger.debug(msg);
            }

            //source n parsing dets
            NewDataProcessingDetail ndpdEnc = null;
            UUID parsingDetailIdEnc = p.getParsingDetailsId();
            if (p.getSourceId() != null) {
                UUID source = p.getSourceId();
                 ndpdEnc = createDataProcessingDetail(parsingDetailIdEnc, source);

            }
            if (ndpdEnc != null) {
                Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
                ndpdc.add(ndpdEnc);
                npe.setDataProcessingDetails(ndpdc);
            } else {
                logger.debug("Null DataProcessingDetail object found ");
            }
            //external ids
//            ExternalID eiEnc = p.getOriginalId();
//            if (eiEnc != null) {
//                npe.setOriginalID(portExternalId(ei));
//            }
            //other ids
            if (p.getOtherOriginalIds() != null) {
                Collection<ExternalIdentifier> otherList = new ArrayList<>();
                for (ExternalID i : p.getOtherOriginalIds()) {
                    try {
                        otherList.add(portExternalId(i));
                    } catch (OldToNewAPOConversionException e) {
                        logger.debug("Could not convert other external id!");
                    }
                }
                npe.setOtherOriginalIDss(otherList);
            } else {
                logger.debug("problem without alternate external ids");
            }
            //lastEditTime
            if (p.getLastEditDateTime() != null) {
                npe.setLastEditDate(portDateTime(p.getLastEditDateTime()));
            }

            //create an original id
            NewExternalIdentifier nei = new NewExternalIdentifier();
            UUID neidid = UUID.nameUUIDFromBytes(p.getMetadata().get("ENCOUNTER_ICN").getBytes());
            XUUID encXID = XUUID.create("NewExternalIdentifier", neidid, true, false);
            nei.setInternalXUUID(encXID);
            nei.setAssignAuthority("EDS");
            nei.setIDValue(p.getMetadata().get("ENCOUNTER_ICN"));
            //add other original ids if necessary
            if(p.getMetadata().containsKey("ORIGINAL_ENCOUNTER_ID")){
                NewExternalIdentifier nei2 = new NewExternalIdentifier();
                nei2.setAssignAuthority("EDS");
                nei2.setIDValue(p.getMetadata().get("ORIGINAL_ENCOUNTER_ID"));
                npe.addOtherOriginalIDss(nei2);
            }
            npe.setOriginalID(nei);

            //connect the patient encounter with new patient
            newPatient.addEncounters(npe);

            //create an anonymous claimable event
            InsuranceClaimableEvent ice = new NewInsuranceClaimableEvent();
            Collection<InsuranceClaimableEvent> icec = new ArrayList<>();
            icec.add(npe);
            rm.setClaimedEvents(icec);


            //create an insurance plan and a policy
            if (p.getMetadata().containsKey("MA_CONTRACT_ID")) {
                NewInsurancePlan nip = new NewInsurancePlan();
                //set the contract id
                NewExternalIdentifier contractId = new NewExternalIdentifier();
                contractId.setIDValue(p.getMetadata().get("MA_CONTRACT_ID"));
                contractId.setAssignAuthority("MEDICARE_ADVANTAGE");
                nip.setContractIdentifier(contractId);
                //create an insurance policy (b)
                //get the hicn from the external id of this patient
                if (p.getMetadata().containsKey("HICN")) {
                    MedicareInsurancePolicy nmip = new NewMedicareInsurancePolicy();
                    //create a hicn
                    if (p.getMetadata().get("HICN").length() > 0) {
                        NewHicNumber nhn = new NewHicNumber();
                        String hic = "";
                        String hicVal = p.getMetadata().get("HICN");
                        if(hicVal.contains("^^")){
                            hic = hicVal.split("\\^\\^")[0];
                        }else {
                            hic = hicVal;
                        }
                        nhn.setIDValue(hic);
                        nhn.setAssignAuthority("MEDICARE");
                        nmip.setHICNNumber(nhn);
                    }
                    nmip.setInsurancePlan(nip);
                    Collection<InsurancePolicy> nipCol = new ArrayList<>();
                    nipCol.add(nmip);
                    newPatient.setInsurancePolicies(nipCol);
                }
            }

            DateTimeParser[] parsers = {
                    DateTimeFormat.forPattern( "yyyyMMdd" ).getParser(),
                    DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser() };
            DateTimeFormatter formatter = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter();

            if(p.getMetadata().containsKey("PROCESSING_DATE")){
                try{
                    rm.setInsuranceClaimProcessingDate(portDateTime(formatter.parseDateTime(p.getMetadata().get("PROCESSING_DATE"))));
                } catch (IllegalArgumentException e) {
                        logger.error("COULD NOT PARSE RA CLAIM DATE");
                      logger.error(e.getMessage());
                }
            } else {
                String msg = "Fond MAO claim without transaction date! "+p.getInternalUUID()+" pat "+this.oldAPO.getPatientId();
                logger.error(msg);
                throw new InsuranceClaimException(msg);
            }
            if(p.getMetadata().containsKey("PLAN_SUBMISSION_DATE")){
                try {
                    rm.setClaimSubmissionDate(portDateTime(formatter.parseDateTime(p.getMetadata().get("PLAN_SUBMISSION_DATE"))));
                } catch (IllegalArgumentException e) {
                    logger.error("COULD NOT PARSE RA CLAIM DATE!");
                    logger.error(e.getMessage());
                }
            }

            rm.setDeleteIndicator(false);
            rm.setAddIndicator(false);
            if(p.getMetadata().containsKey("ADD_OR_DELETE_FLAG")) {
                if (p.getMetadata().get("ADD_OR_DELETE_FLAG").equalsIgnoreCase("D")) {
                    rm.setDeleteIndicator(true);
                } else if (p.getMetadata().get("ADD_OR_DELETE_FLAG").equalsIgnoreCase("A")) {
                    rm.setAddIndicator(true);
                }
            }else{
                rm.setAddIndicator(true);
                logger.debug("assuming add indicator in MAO claim " +p.getInternalUUID());
            }

            //now set the switch type
            if(p.getMetadata().containsKey("ENCOUNTER_TYPE_SWITCH")){
                if(ConverterUtils.ENCOUNTER_TYPE_SWITCH.containsKey(p.getMetadata().get("ENCOUNTER_TYPE_SWITCH"))){
                    NewPatientEncounterSwitchType npest = new NewPatientEncounterSwitchType();
                    String val = ConverterUtils.ENCOUNTER_TYPE_SWITCH.get(p.getMetadata().get("ENCOUNTER_TYPE_SWITCH"));
                    npest.setPatientEncounterSwitchTypeValue(Enum.valueOf(PatientEncounterSwitchTypeValue.class, val));
                    rm.setPatientEncounterSwitchType(npest);
                }
            }

            NewRiskAdjustmentInsuranceClaimType nraict = new NewRiskAdjustmentInsuranceClaimType();
            nraict.setRiskAdjustmentInsuranceClaimTypeValue(Enum.valueOf(RiskAdjustmentInsuranceClaimTypeValue.class, "MAO004"));
            rm.setRiskAdjustmentInsuranceClaimType(nraict);
        }

        else {
            String msg ="found an RA claim that could not be converted! problem id: "+p.getInternalUUID()+" patientid : " +this.oldAPO.getPatientId();
            logger.error(msg);
            throw new InsuranceClaimException(msg);
        }



        return rm;
    }





    /**
     * Create a list of CMSValidationErrors from a list of error code values an the last edit date of the old APO's problem object
     *
     * @param errorCodeValues a list of error code values
     * @param lastEditDate    the last edit date of the original problem object
     * @return a collection of CMSValidation errors
     */
    private Collection<CMSValidationError> portCMSValidationErrors(List<String> errorCodeValues, DateTime lastEditDate) {
        List<CMSValidationError> rm = new ArrayList<>();
        if (errorCodeValues.size() > 0) {
            for (String anErrorCode : errorCodeValues) {
                NewCMSValidationError ncve = new NewCMSValidationError();
                ncve.setLastEditDate(portDateTime(lastEditDate));
                ncve.setCMSValidationErrorCodeValue(anErrorCode);
                rm.add(ncve);
            }
        }
        return rm;
    }

    private NewProblem portProblem(Problem p) {
        NewProblem rm = new NewProblem();
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            logger.debug("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //primaryclinical actor
        if (p.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(p.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + p.getInternalUUID());
        }

        //supplementary clinical actor ids
        if (p.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(p.getSupplementaryClinicalActorIds()));
        }

        //source encounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        }
        //code
        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //problem name
        if (p.getProblemName() != null) {
            rm.setProblemName(p.getProblemName());
        }
        //resolution status
        ResolutionStatus rs = p.getResolutionStatus();
        if (rs != null) {
            NewResolutionStatus nrs = new NewResolutionStatus();
            String val = rs.name();
            nrs.setResolutionStatusTypeValue(Enum.valueOf(ResolutionStatusTypeValue.class, val));
            rm.setResolutionStatusType(nrs);
        } else {
           logger.debug("No resolution status specificed for Problem object : \n" + p);
        }
        //temporal status
        if (p.getTemporalStatus() != null) {
            rm.setProblemTemporalStatus(p.getTemporalStatus());
        }
        //start -end date
        NewDateRange ndr = createDateRange(p.getStartDate(), p.getEndDate());
        if (ndr != null) {
            rm.setProblemDateRange(ndr);
        }
        //diagnosis date
        if (p.getDiagnosisDate() != null) {
            rm.setProblemResolutionDate(portDateTime(p.getDiagnosisDate()));
        }
        //severity
        if (p.getSeverity() != null) {
            rm.setProblemSeverity(p.getSeverity());
        }
        return rm;
    }

    /**
     * Convert the appropiate portions of an Old APO object into a new APO insurance plan object
     *
     * @param c an old APO Coverage object
     * @return a new APO insurance plan object
     */
    private NewInsurancePlan portCoveragePlan(Coverage c) {
        NewInsurancePlan rm = new NewInsurancePlan();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = c.getParsingDetailsId();
        if (c.getSourceId() != null) {
            UUID source = c.getSourceId();
             ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found for patientID : "+this.oldAPO.getPatientId());
        }
        //external ids
        ExternalID ei = c.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (c.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : c.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (c.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(c.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(c));
        //primaryclinical actor
        if (c.getPrimaryClinicalActorId() != null) {
            logger.debug("Not converting primary clinical actor id - deprecated in new model! : \n" + c.getInternalUUID());
        }
        //suppl clinical actors
        if (c.getSupplementaryClinicalActorIds() != null) {
            logger.debug("Not converting supplemantary clinical actor ids - deprecated in new model! :\n" + c.getInternalUUID());
        }
        //source encounter
        if (c.getSourceEncounter() != null) {
            logger.debug("Not converting source encounter id  - deprecated in new model! :\n" + c.getInternalUUID());
        }
        if (c.getCode() != null) {
            logger.debug("Not converting clinical code - deprecated in new model! :\n" + c.getInternalUUID());
        }
        //create an Insurance Company

        //create an Insurance Plan
//        NewInsurancePlan nip = new NewInsurancePlan();
//        // plan name
//        if (c.getHealthPlanName() != null) {
//            nip.setInsurancePlanName(c.getHealthPlanName());
//        }
//        // group number
//        if (c.getGroupNumber() != null) {
//            nip.setGroupNumber(portExternalId(c.getGroupNumber()));
//        }
//        // coverage type
//        if (c.getType() != null) {
//            CoverageType ct = c.getType();
//            if (ct != null) {
//                NewInsurancePlanType nipt = new NewInsurancePlanType();
//                String val = ct.name();
//                nipt.setPayorTypeValue(Enum.valueOf(PayorTypeValue.class, val));
//                nip.setInsuranceType(nipt);
//            } else {
//                throw new OldToNewAPOConversionException("No CoverageType specificed for Coverage object : \n" + c);
//            }
//        }
//        rm.setInsurancePlan(nip);
//        //now fill in the list of policies -
//        // TODO - there will only one member to this list policies will need to be merged later in a smart way
//        //get an insurance policy
//        NewInsurancePolicy nip = portCoveragePolicy(c);
//        //add it to a list
//        if (nip != null) {
//            Collection<NewInsurancePolicy> nipc = new ArrayList<>();
//            nipc.add(nip);
//            rm.setInsurancePolicies(nipc);
//        } else {
//            logger.warn("Could not create insurance policies from coverage ! - \n" + c.getInternalUUID());
//        }

        return rm;
    }

    /**
     * Convert portions of the old Coverage object into a new APO Insurance policy object
     *
     * @param c an old APO coverage object
     * @return a new APO Insurance policy object
     */
    private NewInsurancePolicy portCoveragePolicy(Coverage c) {
        NewInsurancePolicy rm = new NewInsurancePolicy();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = c.getParsingDetailsId();
        if (c.getSourceId() != null) {
            UUID source = c.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = c.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (c.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : c.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (c.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(c.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(c));
        //primaryclinical actor
        if (c.getPrimaryClinicalActorId() != null) {
            logger.debug("Not converting primary clinical actor id - deprecated in new model! : \n" + c.getInternalUUID());
        }
        //suppl clinical actors
        if (c.getSupplementaryClinicalActorIds() != null) {
            logger.debug("Not converting supplemantary clinical actor ids - deprecated in new model! :\n" + c.getInternalUUID());
        }
        //source encounter
        if (c.getSourceEncounter() != null) {
            logger.debug("Not converting source encounter id  - deprecated in new model! :\n" + c.getInternalUUID());
        }
        if (c.getCode() != null) {
            logger.debug("Not converting clinical code - deprecated in new model! :\n" + c.getInternalUUID());
        }
        //sequence number
        if (c.getSequenceNumber() != 0) {
            rm.setInsuranceSequnceNumberValue(c.getSequenceNumber());
        } else {
            logger.warn("Found a sequence number with value 0 : \n" + c.getInternalUUID());
        }
        //policy date range
        DateTime startTimeAtStartOfDay = null;
        DateTime endTimeAtStartOfDay = null;
        if(c.getStartDate() != null){
            startTimeAtStartOfDay = c.getStartDate().toDateTimeAtStartOfDay();
        }
        if(c.getEndDate() != null){
            endTimeAtStartOfDay = c.getEndDate().toDateTimeAtStartOfDay();
        }

        NewDateRange ndr = createDateRange(startTimeAtStartOfDay, endTimeAtStartOfDay);
        rm.setInsurancePolicyDateRange(ndr);

        //memberid
        if (c.getMemberNumber() != null) {
            rm.setMemberNumber(portExternalId(c.getMemberNumber()));
        }
        //subscriber id
        if (c.getSubscriberID() != null) {
            rm.setSubscriberID(portExternalId(c.getSubscriberID()));
        }

        rm.setInsurancePlan(portCoveragePlan(c));
        return rm;
    }

    private NewFamilyHistory portFamilyHistory(FamilyHistory p) {
        NewFamilyHistory rm = new NewFamilyHistory();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //primaryclinical actor
        if (p.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(p.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + p.getInternalUUID());
        }
        //supplementary clinical actor ids
        if (p.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(p.getSupplementaryClinicalActorIds()));
        }
        //source encounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        }
        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //family history
        if (p.getFamilyHistory() != null) {
            rm.setFamilyHistoryValue(p.getFamilyHistory());
        }

        return rm;
    }

    /**
     * Convert an old APO social history object and convert it to a new APO Socialhistory
     *
     * @param p an old APO social history
     * @return a new APO Social history
     */
    private NewSocialHistory portSocialHistory(SocialHistory p) {
        NewSocialHistory rm = new NewSocialHistory();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //primaryclinical actor
        if (p.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(p.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + p.getInternalUUID());
        }
        //suppl clinical actors
        //supplementary clinical actor ids
        if (p.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(p.getSupplementaryClinicalActorIds()));
        }
        //source encounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        }
        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }

        //date
        if (p.getDate() != null) {
            rm.setSocialHistoryDate(portDateTime(p.getDate()));
        }
        //type
        if (p.getType() != null) {
            rm.setSocialHistoryType(portClinicalCode(p.getType(), ndpd));
        }
        //fieldName
        if (p.getFieldName() != null) {
            rm.setSocialHistoryFieldName(p.getFieldName());
        }
        //value
        if (p.getValue() != null) {
            rm.setSocialHistoryValue(p.getValue());
        }
        return rm;
    }

    /**
     * Convert an old APO Administration into a new APO Immunization object
     *
     * @param p an old APO administration object
     * @return a new APO Immunization object
     */
    private NewImmunization portAdministration(Administration p) {
        NewImmunization rm = new NewImmunization();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
             ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //primaryclinical actor
        if (p.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(p.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + p.getInternalUUID());
        }
        //suppl clinical actors
        //supplementary clinical actor ids
        if (p.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(p.getSupplementaryClinicalActorIds()));
        }
        //source encounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        }
        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //admin date
        if (p.getAdminDate() != null) {
            rm.setImmunizationAdminDate(portDateTime(p.getAdminDate()));
        }
        //quantity
        if (p.getQuantity() != 0.0) {
            rm.setImmunizationQuantity(p.getQuantity());
        } else {
            logger.debug("Found an empty quantity! : " + p.getInternalUUID());
        }

        //start date end date
        try {
            NewDateRange ndr = createDateRange(p.getStartDate(), p.getEndDate());
            rm.setImmunizationDateRange(ndr);
        } catch (OldToNewAPOConversionException e) {
            logger.debug("Could not create a valid date range! : " + p.getInternalUUID());
        }
        //medication
        if (p.getMedication() != null) {
            NewMedication nm = portMedication(p.getMedication());
            Collection<com.apixio.model.owl.interfaces.Medication> meds = new ArrayList<>();
            //check for amount
            if (p.getAmount() != null) {
                nm.setMedicationAmount(p.getAmount());
            }
            //check for dosage
            if (p.getDosage() != null) {
                nm.setMedicationDosage(p.getDosage());
            }
            meds.add(nm);
            rm.setImmunizationMedications(meds);
        }
        //medication series number
        if (p.getMedicationSeriesNumber() != 0.0) {
            rm.setImmunizationMedicationSeriesNumber(p.getMedicationSeriesNumber());
        } else {
            logger.debug("Found an empty medication series number ! " + p.getInternalUUID());
        }
        return rm;
    }

    private NewLabResult portLabResult(LabResult p) {
        NewLabResult rm = new NewLabResult();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //primaryclinical actor
        if (p.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(p.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + p.getInternalUUID());
        }
        //p clinical actor ids
        if (p.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(p.getSupplementaryClinicalActorIds()));
        }
        //source encounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        }
        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //lab name
        if (p.getLabName() != null) {
            rm.setLabResultNameValue(p.getLabName());
        }
        //value
        //TODO - how do I convert this either string of number object ??
        //range
        if (p.getRange() != null) {
            rm.setRange(p.getRange());
        }
        //flag
        LabFlags lf = p.getFlag();
        if (lf != null) {
            NewLabResultFlag nlrf = new NewLabResultFlag();
            String val = lf.name();
            nlrf.setLabFlagValue(Enum.valueOf(LabFlagValue.class, val));
            rm.setLabFlagType(nlrf);
        } else {
            logger.debug("No lab flag specificed for lab result object : \n" + p);
        }
        //units
        if (p.getUnits() != null) {
            rm.setLabResultUnit(p.getUnits());
        }
        //labnote
        if (p.getLabNote() != null) {
            rm.setLabNote(p.getLabNote());
        }
        //specimen
        if (p.getSpecimen() != null) {
            rm.setSpecimen(portClinicalCode(p.getSpecimen(), ndpd));
        }
        //panel
        if (p.getPanel() != null) {
            rm.setLabPanel(portClinicalCode(p.getPanel(), ndpd));
        }
        //super panel
        if (p.getSuperPanel() != null) {
            rm.setLabSuperPanel(portClinicalCode(p.getSuperPanel(), ndpd));
        }
        //sample date
        if (p.getSampleDate() != null) {
            rm.setSampleDate(portDateTime(p.getSampleDate()));
        }
        //caresite Id
        if (p.getCareSiteId() != null) {
            //TODO - notice the caresite containment - is this correct?
            CareSite cs = this.oldAPO.getCareSiteById(p.getCareSiteId());
            rm.setLabResultCareSite(portCareSite(cs));
        }
        //otherDates
        if (p.getOtherDates() != null) {
            //TODO - new model does not have typed dates
        }
        //sequence number
        if (p.getSequenceNumber() != null) {
            rm.setLabResultSequenceNumber(p.getSequenceNumber());
        }
        return rm;
    }

    /**
     * Convert an Old APO biometric value into a new APO biometric value
     *
     * @param p an old APO biometric value
     * @return a new APO BiometricValue object
     */
    private NewBiometricValue portBiometricValue(BiometricValue p) {
        NewBiometricValue rm = new NewBiometricValue();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null) {
            UUID source = p.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //primaryclinical actor
        if (p.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(p.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + p.getInternalUUID());
        }

        //supplementary clinical actor ids
        if (p.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(p.getSupplementaryClinicalActorIds()));
        }

        //source encounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        }
        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //TODO - i am ignoring eitherstring or number variable
        //units of measure
        if (p.getUnitsOfMeasure() != null) {
            rm.setMeasurmentUnit(p.getUnitsOfMeasure());
        }
        //result date
        if (p.getResultDate() != null) {
            rm.setMeasurmentResultDate(portDateTime(p.getResultDate()));
        }
        //name
        if (p.getName() != null) {
            rm.setBiometricValueName(p.getName());
        } else {
            logger.debug("found a biometric result without a name!");
        }
        return rm;
    }

    /**
     * Convert an old APO PRescription into a newAPO PRescription
     *
     * @param p a PRescription
     * @return a new Prescription
     */
    private NewPrescription portPrescription(Prescription p) {
        NewPrescription rm = new NewPrescription();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = p.getParsingDetailsId();
        if (p.getSourceId() != null && parsingDetailId != null) {
            UUID source = p.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = p.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (p.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : p.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (p.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(p.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(p));
        //primaryclinical actor
        if (p.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(p.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + p.getInternalUUID());
        }
        //suppl clinical actors
        if (p.getSupplementaryClinicalActorIds() != null) {
            Collection<XUUID> altMedPros = new ArrayList<>();
            for (UUID id : p.getSupplementaryClinicalActorIds()) {
                altMedPros.add(XUUID.fromString(id.toString()));
            }
            rm.setAlternateMedicalProfessionalIds(altMedPros);
        }
        //source encounter
        if (p.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(p.getSourceEncounter().toString()));
        }
        //code
        if (p.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(p.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //pxs date
        if (p.getPrescriptionDate() != null) {
            rm.setPrescriptionDate(portDateTime(p.getPrescriptionDate()));
        }
        //quantity
        if (p.getQuantity() != null) {
            rm.setPrescriptionQuantity(p.getQuantity());
        }
        //enddate
        if (p.getEndDate() != null) {
            rm.setPrescriptionEndDate(portDateTime(p.getEndDate()));
        }
        //fill date
        if (p.getFillDate() != null) {
            rm.setPrescriptionFillDate(portDateTime(p.getFillDate()));
        }
        //sig
        if (p.getSig() != null) {
            rm.setSig(p.getSig());
        }
        //associatedMed
        if (p.getAssociatedMedication() != null) {
            NewMedication m = portMedication(p.getAssociatedMedication());
            if (p.getAmount() != null) {
                m.setMedicationAmount(p.getAmount());
            }
            if (p.getDosage() != null) {
                m.setMedicationDosage(p.getDosage());
            }
            rm.addPrescribedDrugs(m);
        }

        //isActivePrescp
        if (p.isActivePrescription() != null) {
            rm.setIsActivePrescription(p.isActivePrescription());
        }
        //refilsremaining
        if (p.getRefillsRemaining() != null) {
            rm.setNumberOfRefillsRemaining(p.getRefillsRemaining());
        }
        return rm;
    }

    /**
     * Convert an Old APO medication into a new APO medication
     *
     * @param m an old APO medication
     * @return a new APO medication
     */
    private NewMedication portMedication(Medication m) {
        NewMedication rm = new NewMedication();
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = m.getParsingDetailsId();
        if (m.getSourceId() != null) {
            UUID source = m.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //external ids
        ExternalID ei = m.getOriginalId();
        if (ei != null) {
            rm.setOriginalID(portExternalId(ei));
        }
        //other ids
        if (m.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> otherList = new ArrayList<>();
            for (ExternalID i : m.getOtherOriginalIds()) {
                try {
                    otherList.add(portExternalId(i));
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Could not convert other external id!");
                }
            }
        } else {
            logger.debug("problem without alternate external ids");
        }
        //lastEditTime
        if (m.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(m.getLastEditDateTime()));
        }
        //internalid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(m));
        //primaryclinical actor
        if (m.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(m.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found problem without a clinical actor id! " + m.getInternalUUID());
        }
        //suppl clinical actors
        //supplementary clinical actor ids
        if (m.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(m.getSupplementaryClinicalActorIds()));
        }
        //source encounter
        if (m.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(m.getSourceEncounter().toString()));
        }
        //code
        if (m.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(m.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //brandname
        if (m.getBrandName() != null) {
            rm.setBrandName(m.getBrandName());
        }
        //genericname
        if (m.getGenericName() != null) {
            rm.setGenericName(m.getGenericName());
        }
        //ingredients
        if (m.getIngredients() != null) {
            Collection<String> ingredients = new ArrayList<>();
            for (String s : m.getIngredients()) {
                ingredients.add(s);
            }
            rm.setDrugIngredients(ingredients);
        }
        //strenght
        if (m.getStrength() != null) {
            rm.setDrugStrength(m.getStrength());
        }
        //form
        if (m.getForm() != null) {
            rm.setDrugForm(m.getForm());
        }
        //routeofadmin
        if (m.getRouteOfAdministration() != null) {
            rm.setRouteOfAdministration(m.getRouteOfAdministration());
        }
        //units
        if (m.getUnits() != null) {
            rm.setDrugUnits(m.getUnits());
        }
        return rm;
    }

    /**
     * Convert an old APO allergy into a new APo allergy
     *
     * @param oldAllergy an old apo allergy
     * @return a new apo allergy object
     */
    private NewAllergy portAllergy(Allergy oldAllergy) {
        NewAllergy rm = new NewAllergy();
        //id
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(oldAllergy));
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = oldAllergy.getParsingDetailsId();
        if (oldAllergy.getSourceId() != null) {
            UUID source = oldAllergy.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found");
        }
        //original id
        if (oldAllergy.getOriginalId() != null) {
            NewExternalIdentifier nei = portExternalId(oldAllergy.getOriginalId());
            rm.setOriginalID(nei);
        } else {
            logger.debug("Found a allergy without an external id! :" + oldAllergy.getInternalUUID());
        }
        //other orginal ids
        if (oldAllergy.getOtherOriginalIds() != null) {
            Collection<com.apixio.model.owl.interfaces.ExternalIdentifier> neis = new ArrayList<>();
            for (ExternalID e : oldAllergy.getOtherOriginalIds()) {
                NewExternalIdentifier nei = portExternalId(e);
                if (nei != null) {
                    neis.add(nei);
                } else {
                    logger.debug("could not convert external identifier !");
                }
            }
            if (neis.size() > 0) {
                rm.setOtherOriginalIDss(neis);
            }
        }
        //lastedit time
        if (oldAllergy.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(oldAllergy.getLastEditDateTime()));
        } else {
            logger.debug("Found procedure without a last edit time set !: " + oldAllergy.getInternalUUID());
        }
        //primary clinical actor id
        if (oldAllergy.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(oldAllergy.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found procedure without a clinical actor id! " + oldAllergy.getInternalUUID());
        }

        //supplementary clinical actor ids
        if (oldAllergy.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(oldAllergy.getSupplementaryClinicalActorIds()));
        }

        //source encounter
        if (oldAllergy.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(oldAllergy.getSourceEncounter().toString()));
        } else {
            logger.debug("Found Procedure without a source encounter  ! :\n" + oldAllergy.getInternalUUID());
        }
        //clinical code (codedbaseobject)
        if (oldAllergy.getCode() != null) {
            ClinicalCode code = oldAllergy.getCode();
            NewClinicalCode nCode = portClinicalCode(code, ndpd);
            rm.setCodedEntityClinicalCode(nCode);
        }
        //allergen - string
        if (oldAllergy.getAllergen() != null) {
            rm.setAllergyAllergenString(oldAllergy.getAllergen());
        }
        //reaction - clinical code
        if (oldAllergy.getReaction() != null) {
            rm.setCodedEntityClinicalCode(portClinicalCode(oldAllergy.getReaction(), ndpd));
        }
        //reactionseverity - string
        if (oldAllergy.getReactionSeverity() != null) {
            rm.setAllergyReactionSeverity(oldAllergy.getReactionSeverity());
        }
        //diagnosis -datetime
        if (oldAllergy.getDiagnosisDate() != null) {
            rm.setAllergyDiagnosisDate(portDateTime(oldAllergy.getDiagnosisDate()));
        }
        //resolveddate - datetime
        if (oldAllergy.getResolvedDate() != null) {
            rm.setAllergyResolvedDate(portDateTime(oldAllergy.getResolvedDate()));
        }
        //reaction - datetime
        if (oldAllergy.getReactionDate() != null) {
            rm.setAllergyReactionDate(portDateTime(oldAllergy.getReactionDate()));
        }
        return rm;
    }

    /**
     * Create a new APO medical procedure from an old APO procedure
     *
     * @param anOldProcedure an old APO procedure
     * @return a new Medical Procedure
     */
    private NewProcedure portProcedure(Procedure anOldProcedure) {
        NewProcedure rm = new NewProcedure();
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(anOldProcedure));
        //source n parsing dets
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = anOldProcedure.getParsingDetailsId();
        if (anOldProcedure.getSourceId() != null) {
            UUID source = anOldProcedure.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            logger.debug("Null DataProcessingDetail object found");
        }
        //original id
        if (anOldProcedure.getOriginalId() != null) {
            NewExternalIdentifier nei = portExternalId(anOldProcedure.getOriginalId());
            rm.setOriginalID(nei);
        } else {
            logger.debug("Found a procedure without an external id! :" + anOldProcedure.getInternalUUID());
        }
        //other orginal ids
        if (anOldProcedure.getOtherOriginalIds() != null) {
            Collection<com.apixio.model.owl.interfaces.ExternalIdentifier> neis = new ArrayList<>();
            for (ExternalID e : anOldProcedure.getOtherOriginalIds()) {
                NewExternalIdentifier nei = portExternalId(e);
                if (nei != null) {
                    neis.add(nei);
                } else {
                    logger.debug("could not convert external identifier ! ");
                }
            }
            if (neis.size() > 0) {
                rm.setOtherOriginalIDss(neis);
            }
        }
        //lastedit time
        if (anOldProcedure.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(anOldProcedure.getLastEditDateTime()));
        } else {
            logger.debug("Found procedure without a last edit time set !: " + anOldProcedure.getInternalUUID());
        }
        //primary clinical actor id
        if (anOldProcedure.getPrimaryClinicalActorId() != null) {
            rm.setMedicalProfessional(XUUID.fromString(anOldProcedure.getPrimaryClinicalActorId().toString()));
        } else {
            logger.debug("Found procedure without a clinical actor id! " + anOldProcedure.getInternalUUID());
        }

        //supplementary clinical actor ids
        if (anOldProcedure.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(anOldProcedure.getSupplementaryClinicalActorIds()));
        }

        //source encounter
        if (anOldProcedure.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(anOldProcedure.getSourceEncounter().toString()));
        } else {
            logger.debug("Found Procedure without a source encounter  ! :\n" + anOldProcedure.getInternalUUID());
        }

        //procedure name
        if (anOldProcedure.getProcedureName() != null) {
            rm.setMedicalProcedureNameValue(anOldProcedure.getProcedureName());
        } else {
            logger.debug("Procedure without a name found!\n" + anOldProcedure.getInternalUUID());
        }
        //date range
        //TODO - am I using the dates on the procedure object correctly?
        NewDateRange ndr = createDateRange(anOldProcedure.getPerformedOn(), anOldProcedure.getEndDate());
        rm.setMedicalProcedureDateRange(ndr);
        //interpretation
        if (anOldProcedure.getInterpretation() != null) {
            rm.setMedicalProcedureInterpretation(anOldProcedure.getInterpretation());
        } else {
            logger.debug("Procedure found without interpretation ! : " + anOldProcedure.getInternalUUID());
        }
        // diagnoses
        List<ClinicalCode> dxs = anOldProcedure.getSupportingDiagnosis();
        if (dxs != null) {
            Collection<com.apixio.model.owl.interfaces.ClinicalCode> diagnoses = new ArrayList<>();
            for (ClinicalCode cc : dxs) {
                try {
                    NewClinicalCode ncc = portClinicalCode(cc, ndpd);
                    diagnoses.add(ncc);
                } catch (OldToNewAPOConversionException e) {
                    logger.debug("Clinical code conversion problem!");
                }
            }
            rm.setDiagnosisCodes(diagnoses);
        } else {
            logger.warn("Procedure found without supporting diagnoses ! : " + anOldProcedure.getInternalUUID());
        }
        //body site
        if (anOldProcedure.getBodySite() != null) {
            NewAnatomicalEntity nae = portAnatomicalEntity(anOldProcedure.getBodySite());
            if (nae != null) {
                rm.setBodySite(nae);
            }
        } else {
            logger.debug("Procedure found without a body site ! : " + anOldProcedure.getInternalUUID());
        }
        //clinical code
        if (anOldProcedure.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(anOldProcedure.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        } else {
            logger.warn("Found procedure without a clinical code" + anOldProcedure.getInternalUUID());
        }
        return rm;
    }

    /**
     * Move an Old APO Source document to a new APO source doc
     *
     * @param s an old APO source
     * @return a new APO Source
     */
    private NewSource portSource(Source s, NewDataProcessingDetail ndpd, ParsingDetail pd) {
        //check if the source is in the map
        NewSource rm = new NewSource();

        //id
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(s));

        //externalid
        ExternalID ei = s.getOriginalId();
        if (ei != null) {
            ExternalIdentifier extIdentifier = portExternalId(ei);
            rm.setOriginalID(extIdentifier);
        } else {
            logger.debug("Source object found without an external id! ");
        }
        //source system
        String sourceSys = s.getSourceSystem();
        if (sourceSys != null && sourceSys.length()>0) {
            //deprecate
            logger.debug("Not converting source system string - deprecated in new model! ");
        }

        //source type
        String sourceType = s.getSourceType();
        if (sourceType != null && sourceType.length()>0) {
            NewSourceType nst = new NewSourceType();
            nst.setSourceTypeValue(SourceTypeValue.ASSERTED);
            nst.setSourceTypeStringValue(sourceType);
            rm.setSourceType(nst);
            logger.debug("Not converting source type string - deprecated in new model! ");
        }

        //clinical actor id
        Iterable<ClinicalActor> clinicalActorsItrbl = oldAPO.getClinicalActors();

        ClinicalActor ca = ConverterUtils.getClinicalActorFromIterable(s.getClinicalActorId(), clinicalActorsItrbl);
        if (ca == null) {
            logger.debug("No clinical actor found for Source object: \n" + s.getInternalUUID());
            logger.debug("Searched for clinical actor uuid : \n" + s.getClinicalActorId());
        } else {
            NewMedicalProfessional nca = portClinicalActor(ca);
            rm.setSourceAuthor(nca);
        }

        //organization
        if (s.getOrganization() != null) {
            NewOrganization no = portOrganization(s.getOrganization(), ndpd);
            rm.setSourceOrganization(no);
        }

        //creation date
        DateTime creationDate = s.getCreationDate();
        Date newCreationDate = portDateTime(creationDate);
        rm.setSourceCreationDate(newCreationDate);

        if(s.getMetadata()!=null){
            File sourceFile = new NewFile();
            if(s.getMetadata().containsKey("SOURCE_FILE_URI")){
                try {
                    URI u = new URI(s.getMetaTag("SOURCE_FILE_URI"));
                    sourceFile.setFileURL(u);
                    if(pd.getParsingDateTime() != null){
                        NewDate nd = portDateTime(pd.getParsingDateTime());
                        if(nd != null) {
                            sourceFile.setFileDate(nd);
                        }
                    }
                    rm.setSourceFile(sourceFile);
                } catch (URISyntaxException e) {
                    logger.error("Invalid source file URI found");
                }
            }
        }
        return rm;
    }

    /**
     * Move old APO's ExternalID to new APO's ExternalIdentifier object
     *
     * @param eI an Old APO's externalId obj
     * @return a new APO's ExternalIdentifier
     */
    private NewExternalIdentifier portExternalId(ExternalID eI) {
        NewExternalIdentifier rm = new NewExternalIdentifier();
        //String id
        if (eI.getId() != null) {
            String id = eI.getId();
            rm.setIDValue(id);
        } else {
            logger.debug("Found ExternalID object that does not have an ID value");
        }
        //string type
        if (eI.getType() != null) {
            rm.setIDType(eI.getType());
        }
        //string assign authority
        if (eI.getAssignAuthority() != null) {
            rm.setAssignAuthority(eI.getAssignAuthority());
        }
        //check for metadata
        Map<String, String> md = eI.getMetadata();
        if (md.size() != 0) {
            System.out.println(md);
            logger.debug("Non empty metadata field found! in external id ");
        }
        return rm;
    }

    /**
     * Convert old APO name to new APO name
     *
     * @param oldName an old APO name object
     * @return a new APO name object
     */
    private NewName portName(Name oldName) {
        NewName rm = new NewName();
        //sourceid
        //parsingdetails
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = oldName.getParsingDetailsId();
        if (oldName.getSourceId() != null) {
            UUID source = oldName.getSourceId();
             ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            logger.debug("Found a name with parsing details - now deprecated!");
        }
        //internalUUID
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(oldName));
        //originalId
        if (oldName.getOriginalId() != null) {
            logger.debug("Found a name with an external id!");
        }
        //otheroriginalids
        if (oldName.getOtherOriginalIds() != null && oldName.getOtherOriginalIds().size() > 0) {
            logger.debug("Found a name with other external ids!");
        }
        //lasteditdatetime
        if (oldName.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(oldName.getLastEditDateTime()));
        } else {
            logger.debug("Found a name without a last edit date set! : " + oldName.getInternalUUID());
        }
        //given names
        if (oldName.getGivenNames() != null) {
            List<String> givenNames = new ArrayList<>();
            for (String gn : oldName.getGivenNames()) {
                givenNames.add(gn);
            }
            rm.setGivenNames(givenNames);
        }
        //family names
        if (oldName.getFamilyNames() != null) {
            List<String> famNames = new ArrayList<>();
            for (String gn : oldName.getFamilyNames()) {
                famNames.add(gn);
            }
            rm.setFamilyNames(famNames);
        }
        //suffixes
        if (oldName.getSuffixes() != null) {
            List<String> suffixes = new ArrayList<>();
            for (String gn : oldName.getSuffixes()) {
                suffixes.add(gn);
            }
            rm.setSuffixNames(suffixes);
        }
        //prefixes
        if (oldName.getPrefixes() != null) {
            List<String> prefixes = new ArrayList<>();
            for (String gn : oldName.getPrefixes()) {
                prefixes.add(gn);
            }
            rm.setPrefixNames(prefixes);
        }
        //nametype
        NameType nt = oldName.getNameType();
        if (nt != null) {
            NewNameType nnt = new NewNameType();
            String val = nt.name();
            nnt.setNameTypeValue(Enum.valueOf(NameTypeValue.class, val));
            rm.setNameType(nnt);
        } else {
            logger.debug("No nametype specificed for Name object : \n" + oldName);
        }
        Map<String, String> md = oldName.getMetadata();
        if (md != null) {
            logger.debug("Non empty metadata field found! ");
        }
        return rm;
    }

    /**
     * Move the Old APO's Clinical actor to a new APO Medical professional
     *
     * @param ca an old APO clinical actor
     * @return a new APO medical professional
     */
    private NewMedicalProfessional portClinicalActor(ClinicalActor ca) {
        NewMedicalProfessional rm = new NewMedicalProfessional();
        //id
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(ca));
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = ca.getParsingDetailsId();
        if (ca.getSourceId() != null) {
            UUID source = ca.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);
        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found");
        }
        //lastedit date
        if (ca.getLastEditDateTime() != null) {
            Date d = portDateTime(ca.getLastEditDateTime());
            rm.setLastEditDate(d);
        }
        //original id
        if (ca.getOriginalId() != null) {
            ExternalIdentifier ei = portExternalId(ca.getOriginalId());
            rm.setOriginalID(ei);
        } else {
            throw new OldToNewAPOConversionException("No original identifier found for this clinical actor \n" + ca);
        }
        //other originalids
        if (ca.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> eis = new ArrayList<>();
            for (ExternalID ei : ca.getOtherOriginalIds()) {
                ExternalIdentifier nei = portExternalId(ei);
                eis.add(nei);
            }
            rm.setOtherOriginalIDss(eis);
        }
        if (ca.getContactDetails() != null) {
            rm.setContactDetails(portContactDetails(ca.getContactDetails()));
        }
        //clinical actor role
        ActorRole ar = ca.getRole();
        if (ar != null) {
            //create a newParsertype obj
            NewClinicalRoleType ncrt = new NewClinicalRoleType();
            String val = ar.name();
            ncrt.setClinicalRoleTypeValue(Enum.valueOf(ClinicalRoleTypeValue.class, val));
            rm.setClinicalRole(ncrt);
        } else {
            throw new OldToNewAPOConversionException("No Clinical Role specificed for ClinicalActor object ");
        }
        //demographics
        if (ca.getActorGivenName() != null) {
            NewDemographics nd = new NewDemographics();
            nd.setName(portName(ca.getActorGivenName()));
            if (ca.getActorSupplementalNames() != null) {
                Collection<com.apixio.model.owl.interfaces.Name> alternateNames = new ArrayList<>();
                for (Name n : ca.getActorSupplementalNames()) {
                    NewName nn = portName(n);
                    alternateNames.add(nn);
                }
                nd.setAlternateNames(alternateNames);
            }
            rm.setDemographics(nd);
        }
        //title
        if (ca.getTitle() != null) {
            rm.setTitle(ca.getTitle());
        }
        //organization
        if (ca.getAssociatedOrg() != null) {
            NewOrganization no = portOrganization(ca.getAssociatedOrg());
            rm.setOrganization(no);
        }
        Map<String, String> md = ca.getMetadata();
        if (md != null) {
            logger.debug("Non empty metadata field found! ");
        }

        return rm;
    }

    /**
     * Move old APO's Parsing detail into a new APO data processing detail object
     *
     * @return a DataProcessingDetail object
     */
    private NewDataProcessingDetail portParsingDetail(ParsingDetail pd) {
        NewDataProcessingDetail rm = new NewDataProcessingDetail();
        //transfer ids
        UUID i = UUID.randomUUID();
        XUUID x = XUUID.fromString(i.toString());
        rm.setInternalXUUID(x);

        //parsing date
        DateTime parsingDate = pd.getParsingDateTime();
        NewDate nd = portDateTime(parsingDate);
        rm.setDataProcessingDetailDate(nd);

        //create a processor
        NewProcessor np = new NewProcessor();


        DocumentType dt = pd.getContentSourceDocumentType();
        if(dt != null){
            NewInputDataType nidt = new NewInputDataType();
            String val = dt.name();
            //nidt.setInputDataTypeValue(Enum.valueOf(InputDataTypeValue.class, val));
            //np.setInputDataType(nidt);
        }

        ParserType pt = pd.getParser();
        if(pt!=null){
            NewProcessorType npt = new NewProcessorType();
            String val = pt.name();


            //npt.setProcessorTypeValue(Enum.valueOf(ProcessorTypeValue.class, val));
            //np.setProcessorType(npt);
        }

        if(pd.getParserVersion() != null){
            np.setProcessorVersion(pd.getParserVersion());
        }

        rm.setProcessor(np);

        //parser version
        String pv = pd.getParserVersion();
        if (pv != null) {
            rm.setVersion(pv);
        } else {
            throw new OldToNewAPOConversionException("No version specified for ParsingDetail object ");
        }
        if (pd.getMetadata() != null) {
            logger.debug("Non empty metadata field found!");
        }
        return rm;
    }

    private NewOrganization portOrganization(Organization o, NewDataProcessingDetail ndpd) {
        NewOrganization rm = new NewOrganization();
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(o));

        if (ndpd != null) {
            List<DataProcessingDetail> dpds = new ArrayList<>();
            dpds.add(ndpd);
            rm.setDataProcessingDetails(dpds);
        } else {
            logger.debug("Null DataProcessingDetail object found ");
        }
        //externalid
        if (o.getOriginalId() != null) {
            ExternalIdentifier ei = portExternalId(o.getOriginalId());
            rm.setOriginalID(ei);
        } else {
            logger.debug("Could not create ExternalIdentifier object : \n" + o.getInternalUUID());
        }
        if (o.getAlternateIds() != null) {
            Collection<ExternalIdentifier> ids = new ArrayList<>();
            for (ExternalID ei : o.getAlternateIds()) {
                ExternalIdentifier exid = portExternalId(ei);
                ids.add(exid);
            }
            rm.setOtherOriginalIDss(ids);
        }
        //name
        if (o.getName() != null) {
            rm.setOrganizationNameValue(o.getName());
        }
        //contact details
        if (o.getContactDetails() != null) {
            NewContactDetails ncd = portContactDetails(o.getContactDetails(), ndpd);
            rm.setContactDetails(ncd);
        }
        //last edit
        if (o.getLastEditDateTime() != null) {
            rm.setLastEditDate(portDateTime(o.getLastEditDateTime()));
        }
        Map<String, String> md = o.getMetadata();
        if (md != null) {
            logger.debug("Non empty metadata field found! ");
        }
        return rm;
    }

    private NewOrganization portOrganization(Organization o) {
        NewOrganization rm = new NewOrganization();
        //transfer ids
        XUUID id = XUUID.fromString(o.getInternalUUID().toString());
        if (id != null) {
            rm.setInternalXUUID(id);
        } else {
            throw new OldToNewAPOConversionException("No UUID set for ParsingDetail pd :\n" + o);
        }
        //uuid
        if (o != null) {
            XUUID nid = ConverterUtils.createXUUIDFromInternalUUID(o);
            rm.setInternalXUUID(nid);
            NewDataProcessingDetail ndpd = null;
            UUID parsingDetailId = o.getParsingDetailsId();
            if (o.getSourceId() != null) {
                UUID source = o.getSourceId();
                ndpd = createDataProcessingDetail(parsingDetailId, source);

            }
            if (ndpd != null) {
                Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
                ndpdc.add(ndpd);
                rm.setDataProcessingDetails(ndpdc);
            } else {
                logger.debug("Null DataProcessingDetail object found ");
            }
            //externalid
            if (o.getOriginalId() != null) {
                ExternalIdentifier ei = portExternalId(o.getOriginalId());
                rm.setOriginalID(ei);
            } else {
                logger.debug("Could not create ExternalIdentifier object : \n" + o.getInternalUUID());
            }
            if (o.getAlternateIds() != null) {
                Collection<ExternalIdentifier> ids = new ArrayList<>();
                for (ExternalID ei : o.getAlternateIds()) {
                    ExternalIdentifier exid = portExternalId(ei);
                    ids.add(exid);
                }
                rm.setOtherOriginalIDss(ids);
            }
            //name
            if (o.getName() != null) {
                rm.setOrganizationNameValue(o.getName());
            }
            //contact details
            if (o.getContactDetails() != null) {
                NewContactDetails ncd = portContactDetails(o.getContactDetails());
                rm.setContactDetails(ncd);
            }
            //last edit
            if (o.getLastEditDateTime() != null) {
                rm.setLastEditDate(portDateTime(o.getLastEditDateTime()));
            }
            Map<String, String> md = o.getMetadata();
            if (md != null) {
                logger.debug("Non empty metadata field found! ");
            }
            return rm;
        } else {
            logger.debug("Null organization passed in!");
        }
        return rm;
    }

    /**
     * Move an Old APO's DocumentContent object into a new APO file
     *
     * @param aFileInternalId the UUID of the File
     * @param dc              an old APO DocumentContent
     * @return a new APO File object
     */
    private NewFile portDocumentContent(UUID aFileInternalId, DocumentContent dc) {
        NewFile rm = new NewFile();
        //get the internalid of the Document's file
        if (aFileInternalId != null) {
            XUUID id = XUUID.fromString(aFileInternalId.toString());
            rm.setInternalXUUID(id);
        } else {
            throw new OldToNewAPOConversionException("Invalid file uuid");
        }
        //create dataprocessigndetail
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = dc.getParsingDetailsId();
        if (dc.getSourceId() != null) {
            UUID source = dc.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            logger.debug("Null DataProcessingDetail object found ");
        }
        //external id
        if (dc.getOriginalId() != null) {
            ExternalIdentifier ei = portExternalId(dc.getOriginalId());
            rm.setOriginalID(ei);
        } else {
            logger.debug("Could not create ExternalIdentifier object : \n" + dc.getInternalUUID());
        }
        //other original ids
        if (dc.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> eis = new ArrayList<>();
            for (ExternalID ei : dc.getOtherOriginalIds()) {
                ExternalIdentifier nei = portExternalId(ei);
                eis.add(nei);
            }
            rm.setOtherOriginalIDss(eis);
        }
        if (dc.getLastEditDateTime() != null) {
            Date led = portDateTime(dc.getLastEditDateTime());
            rm.setLastEditDate(led);
        }
        if (dc.getMimeType() != null) {
            rm.setMIMEType(dc.getMimeType());
        } else {
            logger.debug("DocumentContent object without mimetype ");
        }
        if (dc.getHash() != null) {
            rm.setHash(dc.getHash());
        } else {
            logger.debug("DocumentContetn object without hash ");
        }
        if (dc.getUri() != null) {
            rm.setURI(dc.getUri());
        }
        if (dc.getMetadata() != null) {
            logger.debug("Non Empty metadata field found!");
        }
        return rm;
    }

//    /**
//     * Check the data processing detail map. Given a key - (a concatenation of parsingDetailID and sourceID) return true if the map has the key false otherwise
//     *
//     * @param aKey a concatenation of parsingdetailid and sourceid
//     * @return true if the key is in the map false otherwise
//     */
//    private boolean checkDataProcessingDetailMap(String aKey) {
//        if (this.dataProcessingDetailMap.containsKey(aKey)) {
//            return true;
//        }
//        return false;
//    }

    private NewContactDetails portContactDetails(ContactDetails cd, NewDataProcessingDetail ndpd) {
        NewContactDetails rm = new NewContactDetails();
        //source uuid
        if (cd != null) {
            if (ndpd != null) {
                List<DataProcessingDetail> ndpdc = new ArrayList<>();
                ndpdc.add(ndpd);
                rm.setDataProcessingDetails(ndpdc);
            } else {
                throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
            }
            //internal uuid
            rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(cd));
            //externalid
            if (cd.getOriginalId() != null) {
                NewExternalIdentifier nei = portExternalId(cd.getOriginalId());
                rm.setOriginalID(nei);
            } else {
                logger.debug("ContactDetails object without external Id ");
            }
            //other originalids
            if (cd.getOtherOriginalIds() != null) {
                Collection<ExternalIdentifier> others = new ArrayList<>();
                for (ExternalID ei : cd.getOtherOriginalIds()) {
                    NewExternalIdentifier nei = portExternalId(ei);
                    others.add(nei);
                }
                rm.setOtherOriginalIDss(others);
            } else {
                logger.debug("ContactDetails object without other external ids ");
            }
            //lastedittime
            if (cd.getLastEditDateTime() != null) {
                NewDate nd = portDateTime(cd.getLastEditDateTime());
                rm.setLastEditDate(nd);
            } else {
                logger.debug("ContactDetails object without lastEditDateTime ");
            }
            //address
            if (cd.getPrimaryAddress() != null) {
                NewAddress na = portAddress(cd.getPrimaryAddress());
                rm.setPrimaryAddress(na);
            }
            //alternate addresses
            if (cd.getAlternateAddresses() != null) {
                Collection<com.apixio.model.owl.interfaces.Address> aa = new ArrayList<>();
                for (Address a : cd.getAlternateAddresses()) {
                    NewAddress na = portAddress(a);
                    aa.add(na);
                }
                rm.setAlternateAddresses(aa);
            }
            //primary email
            if (cd.getPrimaryEmail() != null) {
                rm.setPrimaryEmailAddress(cd.getPrimaryEmail());
            }
            //alternate emails
            if (cd.getAlternateEmails() != null) {
                rm.setAlternateEmailAddresses(cd.getAlternateEmails());
            }
            //primary phone
            if (cd.getPrimaryPhone() != null) {
                NewTelephoneNumber ntn = portPhoneNumber(cd.getPrimaryPhone());
                rm.setTelephoneNumber(ntn);
            }
            //alternate phones
            if (cd.getAlternatePhones() != null) {
                Collection<com.apixio.model.owl.interfaces.TelephoneNumber> nums = new ArrayList<>();
                for (TelephoneNumber tn : cd.getAlternatePhones()) {
                    NewTelephoneNumber ntn = portPhoneNumber(tn);
                    nums.add(ntn);
                }
                rm.setAlternateTelephoneNumbers(nums);
            }
            if (cd.getMetadata() != null) {
                logger.debug("Non Empty metadata field found");
            }
            return rm;
        } else {
            logger.debug("Null contact details object passed in!");
        }

        return null;
    }

    /**
     * Convert old APO's contact details to new APO's contact details
     *
     * @param cd old APO contact details
     * @return new APO contact details
     */
    private NewContactDetails portContactDetails(ContactDetails cd) {
        //TODO - not all values for this class have logging else statements if values are not null - scroll down a bit and see
        NewContactDetails rm = new NewContactDetails();
        //source uuid
        if (cd != null) {
            NewDataProcessingDetail ndpd = null;
            UUID parsingDetailId = cd.getParsingDetailsId();
            if (cd.getSourceId() != null) {
                UUID source = cd.getSourceId();
                ndpd = createDataProcessingDetail(parsingDetailId, source);

            }
            if (ndpd != null) {
                Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
                ndpdc.add(ndpd);
                rm.setDataProcessingDetails(ndpdc);
            } else {
                throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
            }
            //internal uuid
            rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(cd));
            //externalid
            if (cd.getOriginalId() != null) {
                NewExternalIdentifier nei = portExternalId(cd.getOriginalId());
                rm.setOriginalID(nei);
            } else {
                logger.debug("ContactDetails object without external Id ");
            }
            //other originalids
            if (cd.getOtherOriginalIds() != null) {
                Collection<ExternalIdentifier> others = new ArrayList<>();
                for (ExternalID ei : cd.getOtherOriginalIds()) {
                    NewExternalIdentifier nei = portExternalId(ei);
                    others.add(nei);
                }
                rm.setOtherOriginalIDss(others);
            } else {
                logger.debug("ContactDetails object without other external ids ");
            }
            //lastedittime
            if (cd.getLastEditDateTime() != null) {
                NewDate nd = portDateTime(cd.getLastEditDateTime());
                rm.setLastEditDate(nd);
            } else {
                logger.debug("ContactDetails object without lastEditDateTime ");
            }
            //address
            if (cd.getPrimaryAddress() != null) {
                NewAddress na = portAddress(cd.getPrimaryAddress());
                rm.setPrimaryAddress(na);
            }
            //alternate addresses
            if (cd.getAlternateAddresses() != null) {
                Collection<com.apixio.model.owl.interfaces.Address> aa = new ArrayList<>();
                for (Address a : cd.getAlternateAddresses()) {
                    NewAddress na = portAddress(a);
                    aa.add(na);
                }
                rm.setAlternateAddresses(aa);
            }
            //primary email
            if (cd.getPrimaryEmail() != null) {
                rm.setPrimaryEmailAddress(cd.getPrimaryEmail());
            }
            //alternate emails
            if (cd.getAlternateEmails() != null) {
                rm.setAlternateEmailAddresses(cd.getAlternateEmails());
            }
            //primary phone
            if (cd.getPrimaryPhone() != null) {
                NewTelephoneNumber ntn = portPhoneNumber(cd.getPrimaryPhone());
                rm.setTelephoneNumber(ntn);
            }
            //alternate phones
            if (cd.getAlternatePhones() != null) {
                Collection<com.apixio.model.owl.interfaces.TelephoneNumber> nums = new ArrayList<>();
                for (TelephoneNumber tn : cd.getAlternatePhones()) {
                    NewTelephoneNumber ntn = portPhoneNumber(tn);
                    nums.add(ntn);
                }
                rm.setAlternateTelephoneNumbers(nums);
            }
            if (cd.getMetadata() != null) {
                logger.debug("Non Empty metadata field found");
            }
            return rm;
        } else {
            logger.debug("Null contact details object passed in!");
        }

        return null;
    }

    /**
     * Convert old APO's Address to new APO's address
     *
     * @param oa old APO address
     * @return a new APO Address
     */
    private NewAddress portAddress(Address oa) {
        NewAddress rm = new NewAddress();
        rm.setInternalXUUID(XUUID.create("address"));
        //city
        if (oa.getCity() != null) {
            rm.setCity(oa.getCity());
        } else {
            logger.debug("Address without a city found ");
        }
        //state
        if (oa.getState() != null) {
            rm.setState(oa.getState());
        } else {
            logger.debug("Address without a state found ");
        }
        //zip
        if (oa.getZip() != null) {
            rm.setZipCode(oa.getZip());
        } else {
            logger.debug("Address without a zip  found ");
        }
        //country
        if (oa.getCountry() != null) {
            rm.setCountry(oa.getCountry());
        } else {
            logger.debug("Address without a country found ");
        }
        //county
        if (oa.getCounty() != null) {
            rm.setCounty(oa.getCounty());
        } else {
            logger.debug("Address without a county found ");
        }
        //addresstype
        if (oa.getAddressType() != null) {
            NewAddressType nat = new NewAddressType();
            nat.setInternalXUUID(XUUID.create("newAddressType"));
            String val = oa.getAddressType().name();
            nat.setAddressTypeValue(Enum.valueOf(AddressTypeValue.class, val));
            rm.setAddressType(nat);
        } else {
            logger.debug("Address without addresstype found ");
        }
        //streetaddresses
        if (oa.getStreetAddresses() != null) {
            rm.setStreetAddresses(oa.getStreetAddresses());
        } else {
            logger.debug("Address without a street address found ");
        }
        return rm;
    }

    /**
     * Move old APO's telephone number into new APO's telephone number
     *
     * @param tn an OldAPO telephonenumber
     * @return a new APO telephonenumber
     */
    private NewTelephoneNumber portPhoneNumber(TelephoneNumber tn) {
        NewTelephoneNumber rm = new NewTelephoneNumber();
        rm.setInternalXUUID(XUUID.create("phoneNumber"));
        if (tn.getPhoneNumber() != null) {
            rm.setPhoneNumber(tn.getPhoneNumber());
        } else {
            logger.debug("No telephone number value is set in object : \n" + tn);
        }
        if (tn.getPhoneType() != null) {
            NewTelephoneNumberType ntnt = new NewTelephoneNumberType();
            ntnt.setInternalXUUID(XUUID.create("newTelephoneNumberType"));
            String val = tn.getPhoneType().name();
            ntnt.setTelephoneTypeValue(Enum.valueOf(TelephoneTypeValue.class, val));
        } else {
            logger.debug("Found phone number without a telephone type ! ");
        }
        return rm;
    }

    /**
     * Move old APO's collection of allergies to new APO's allergies
     *
     * @param someOldAllergies old APO allergies
     * @return a collection of <? extends Allergy>
     */
    private Collection<com.apixio.model.owl.interfaces.Allergy> portAllergies(Iterable<Allergy> someOldAllergies) {
        Collection<com.apixio.model.owl.interfaces.Allergy> rm = new ArrayList<>();
        for (Allergy al : someOldAllergies) {
            NewAllergy na = portAllergy(al);
            rm.add(na);
        }
        return rm;
    }

    /**
     * Convert an old APO demographics object into a new one
     *
     * @param d an Old APO demographics object
     * @return a new APO demographics object
     */
    private NewDemographics portDemographics(Demographics d) {
        NewDemographics rm = new NewDemographics();
        NewDataProcessingDetail ndpd = null;
        if (d.getSourceId() != null) {
            //source id
            UUID source = d.getSourceId();
            //parsing details id
            UUID parsingDetsId = d.getParsingDetailsId();
            ndpd = createDataProcessingDetail(parsingDetsId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //internal uuid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(d));
        //originalid
        if (d.getOriginalId() != null) {
            NewExternalIdentifier nei = portExternalId(d.getOriginalId());
            rm.setOriginalID(nei);
        } else {
            logger.debug("Demographics object  without external id found !");
        }
        //otheroriginalids
        if (d.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> others = new ArrayList<>();
            for (ExternalID ei : d.getOtherOriginalIds()) {
                NewExternalIdentifier nei = portExternalId(ei);
                others.add(nei);
            }
            rm.setOtherOriginalIDss(others);
        } else {
            logger.debug("Demographics object without other external ids ");
        }
        //lastedit date
        if (d.getLastEditDateTime() != null) {
            NewDate nd = portDateTime(d.getLastEditDateTime());
            rm.setLastEditDate(nd);
        } else {
            logger.debug("ContactDetails object without lastEditDateTime ");
        }
        //name
        if (d.getName() != null) {
            NewName nn = portName(d.getName());
            rm.setName(nn);
        } else {
            logger.debug("found demographics without a name! ");
        }
        //dob
        if (d.getDateOfBirth() != null) {
            rm.setDateOfBirth(portDateTime(d.getDateOfBirth()));
        } else {
            logger.debug("found demographics without a dob! ");
        }
        //dod
        if (d.getDateOfDeath() != null) {
            rm.setDateOfDeath(portDateTime(d.getDateOfDeath()));
        } else {
            logger.debug("found demographics without a dod! ");
        }
        //race
        if (d.getRace() != null) {
            rm.setRace(portClinicalCode(d.getRace(), ndpd));
        } else {
            logger.debug("found demographics without a race!");
        }
        //ethnicity
        if (d.getEthnicity() != null) {
            rm.setEthnicity(portClinicalCode(d.getEthnicity(), ndpd));
        } else {
            logger.debug("found demographics without an ethnicity! ");
        }
        //languages
        if (d.getLanguages() != null) {
            Collection<String> langs = new ArrayList<>();
            for (String lang : d.getLanguages()) {
                if (lang != null && lang.length() > 0) {
                    langs.add(lang);
                } else {
                    logger.debug("Null or empty language found ! \n");
                }
            }
            rm.setLanguages(langs);
        }
        //religiousAffiliation
        if (d.getReligiousAffiliation() != null) {
            rm.setReligiousAffiliation(d.getReligiousAffiliation());
        } else {
            logger.debug("Found demographics object without a religious affiliation ! ");
        }

        if (d.getGender() != null) {
            GenderType gt = new NewGenderType();
            //get the value
            String val = d.getGender().name();
            gt.setGenderValues(Enum.valueOf(GenderValues.class, val));
            rm.setGenderType(gt);
        }

        //metadata
        if (d.getMetadata().size() != 0) {
            logger.debug("Found non-empty metadata fields :");
        }
        return rm;
    }

    private NewPatientEncounter portPatientEncounter(Encounter e) {
        NewPatientEncounter rm = new NewPatientEncounter();
        //parsing dets and Source
        NewDataProcessingDetail ndpd = null;
        UUID parsingDetailId = e.getParsingDetailsId();
        if (e.getSourceId() != null) {
            UUID source = e.getSourceId();
            ndpd = createDataProcessingDetail(parsingDetailId, source);

        }
        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);
            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //internal uuid
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(e));
        //externalid
        if (e.getOriginalId() != null) {
            NewExternalIdentifier nei = portExternalId(e.getOriginalId());
            rm.setOriginalID(nei);
        } else {
            logger.debug("Encounter object without external Id ");
        }
        //other originalids
        if (e.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> others = new ArrayList<>();
            for (ExternalID ei : e.getOtherOriginalIds()) {
                NewExternalIdentifier nei = portExternalId(ei);
                others.add(nei);
            }
            rm.setOtherOriginalIDss(others);
        } else {
            logger.debug("ContactDetails object without other external ids ");
        }
        //lastedittime
        if (e.getLastEditDateTime() != null) {
            NewDate nd = portDateTime(e.getLastEditDateTime());
            rm.setLastEditDate(nd);
        } else {
            logger.debug("ContactDetails object without lastEditDateTime ");
        }

        //primaryclinical actor
        Collection<NewMedicalProfessional> nmpc = new ArrayList<>();
        if (e.getPrimaryClinicalActorId() != null) {
            NewMedicalProfessional nmp = createMedicalProfessionalFromUUID(e.getPrimaryClinicalActorId());
            nmpc.add(nmp);
            rm.setMedicalProfessional(nmp.getInternalXUUID());
        }
        //supplementary clinicalActors
        //supplementary clinical actor ids
        if (e.getSupplementaryClinicalActorIds() != null) {
            rm.setAlternateMedicalProfessionalIds(ConverterUtils.createXUUIDCollectionFromUUIDList(e.getSupplementaryClinicalActorIds()));
        }


        //source encounter id
        if (e.getSourceEncounter() != null) {
            rm.setSourceEncounterId(XUUID.fromString(e.getSourceEncounter().toString()));
        } else {
            logger.debug("Source encounter ID not found !");
        }
        //code
        if (e.getCode() != null) {
            NewClinicalCode ncc = portClinicalCode(e.getCode(), ndpd);
            rm.setCodedEntityClinicalCode(ncc);
        }
        //code translations
        if (e.getCodeTranslations() != null) {
            logger.debug("found code translations! ");
        }
        //encounter start date end date
        NewDateRange ndr = createDateRange(e.getEncounterStartDate(), e.getEncounterEndDate());
        rm.setEncounterDateRange(ndr);

        //encType
        EncounterType et = e.getEncType();
        if (et != null) {
            //get the value
            NewPatientEncounterType npet = new NewPatientEncounterType();
            String val = et.name();
            npet.setPatientEncounterTypeValue(Enum.valueOf(PatientEncounterTypeValue.class, val));
            rm.setPatientEncounterType(npet);
        } else {
            throw new OldToNewAPOConversionException("No encountertype specified! : \n" + e);
        }

        //Caresite
        if (e.getSiteOfService() != null) {
            rm.setPatientEncounterCareSite(portCareSite(e.getSiteOfService()));
        } else {
            logger.debug("Found encounter wihtout a care site! ");
        }

        //chiefcomplaints
        if (e.getChiefComplaints() != null) {
            List<com.apixio.model.owl.interfaces.ClinicalCode> cc = new ArrayList<>();
            for (ClinicalCode acc : e.getChiefComplaints()) {
                try {
                    NewClinicalCode ncc = portClinicalCode(acc, ndpd);
                    cc.add(ncc);
                } catch (OldToNewAPOConversionException ex) {
                    logger.debug("Could not convert clinical code !");
                }
            }
            rm.setChiefComplaints(cc);
        } else {
            logger.debug("encounter found without any chief complaints ");
        }

        return rm;
    }


    /**
     * Convert old APO CareSite into new APO care sites
     *
     * @param cs an oldAPO care site
     * @return a new APO caresite
     */
    private NewCareSite portCareSite(CareSite cs) {
        NewCareSite rm = new NewCareSite();
        //source id && parsing details id
        NewDataProcessingDetail ndpd = null;


        UUID source = cs.getSourceId();
        ndpd = createDataProcessingDetail(cs.getParsingDetailsId(), source);

        if (ndpd != null) {
            Collection<DataProcessingDetail> ndpdc = new ArrayList<>();
            ndpdc.add(ndpd);

            rm.setDataProcessingDetails(ndpdc);
        } else {
            throw new OldToNewAPOConversionException("Null DataProcessingDetail object found ");
        }
        //internal id
        rm.setInternalXUUID(ConverterUtils.createXUUIDFromInternalUUID(cs));
        //original id  externalid
        if (cs.getOriginalId() != null) {
            NewExternalIdentifier nei = portExternalId(cs.getOriginalId());
            rm.setOriginalID(nei);
        } else {
            logger.debug("Encounter object without external Id ");
        }
        //other originalids
        if (cs.getOtherOriginalIds() != null) {
            Collection<ExternalIdentifier> others = new ArrayList<>();
            for (ExternalID ei : cs.getOtherOriginalIds()) {
                NewExternalIdentifier nei = portExternalId(ei);
                others.add(nei);
            }
            rm.setOtherOriginalIDss(others);
        } else {
            logger.debug("ContactDetails object without other external ids ");
        }
        //lastedittime
        if (cs.getLastEditDateTime() != null) {
            NewDate nd = portDateTime(cs.getLastEditDateTime());
            rm.setLastEditDate(nd);
        } else {
            logger.debug("ContactDetails object without lastEditDateTime ");
        }

        //casite name
        if (cs.getCareSiteName() != null) {
            rm.setCareSiteName(cs.getCareSiteName());
        } else {
            logger.debug("Caresite without a name found!");
        }
        //address
        if (cs.getAddress() != null) {
            rm.setPrimaryAddress(portAddress(cs.getAddress()));
        } else {
            logger.debug("Caresite without an address found !");
        }

        //carsite type
        CareSiteType cst = cs.getCareSiteType();
        if (cst != null) {
            NewCareSiteType ncst = new NewCareSiteType();
            String val = cst.name();
            ncst.setCareSiteTypeValue(Enum.valueOf(CareSiteTypeValue.class, val));
            rm.setCareSiteType(ncst);
        } else {
            throw new OldToNewAPOConversionException("No caresite type specificed for Caresite object : \n" + cs);
        }
        return rm;
    }

    /**
     * Create a new medical professional object given the UUID of an old APO clinical actor
     *
     * @param aClinicalActorId the uuid of a clinical actor
     * @return a new medical professional object
     */
    private NewMedicalProfessional createMedicalProfessionalFromUUID(UUID aClinicalActorId) {
        return portClinicalActor(oldAPO.getClinicalActorById(aClinicalActorId));
    }

    /**
     * Create a new patient encounter object given the UUID of an old APO encounter
     *
     * @param encounterUUID an old APO encounter
     * @return a new APO patient encounter object
     */
    private NewPatientEncounter createPatientEncounterFromUUID(UUID encounterUUID) {
        return portPatientEncounter(oldAPO.getEncounterById(encounterUUID));
    }

    /**
     * Create a DataProcessingDetail object given an oldAPO, a ParsingDetail UUID and a Source UUID
     *
     * @param parsingDetailUUID the UUID of an old APO ParsingDetail
     * @param sourceUUID        the UUID of an old APO source UUID
     * @return a DataProcessingDetail object
     */
    private NewDataProcessingDetail createDataProcessingDetail(UUID parsingDetailUUID, UUID sourceUUID) {

        NewDataProcessingDetail rm = new NewDataProcessingDetail();
        UUID i = UUID.randomUUID();
        XUUID x = XUUID.fromString(i.toString());
        rm.setInternalXUUID(x);
        //get the parsingdetail iterable
        Iterable<ParsingDetail> parsingDetailIterable = oldAPO.getParsingDetails();
        //get the source iterable
        Iterable<Source> sourceItrbl = oldAPO.getSources();
        //now use the uuids to create objects for source and parsing details
        Source oldSource;
        ParsingDetail oldParsingDetail;
        oldParsingDetail = ConverterUtils.getParsingDetailFromIterable(parsingDetailUUID, parsingDetailIterable);

        if (oldParsingDetail == null) {
            logger.error("No ParsingDetail object found! UUID : \n" + parsingDetailUUID);
            return rm;
        }

        oldSource = ConverterUtils.getSourceFromIterable(sourceUUID, sourceItrbl);
        if (oldSource == null) {
            logger.debug("No source object found - UUID : \n" + sourceUUID);
        }

        //now create a new dataprocessingdetail object
        if (oldParsingDetail.getParsingDetailsId() != null) {
            rm = portParsingDetail(oldParsingDetail);
        } else {
            logger.debug("No uuid found for parsing detail object : \n" + oldParsingDetail);
        }

        //now try to create a source object
        if (oldSource != null) {
            NewSource ns = portSource(oldSource, rm, oldParsingDetail);
            Collection<com.apixio.model.owl.interfaces.Source> s = new ArrayList<>();
            s.add(ns);
            rm.setSources(s);
        } else {
            logger.debug("Null source object UUID : " + sourceUUID);
        }
        return rm;
    }

    public Patient getOldAPO() {
        return this.oldAPO;
    }

    public com.apixio.model.owl.interfaces.Patient getNewPatient() {
        return this.newPatient;
    }
}
