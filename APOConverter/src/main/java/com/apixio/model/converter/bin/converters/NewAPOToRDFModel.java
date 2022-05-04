package com.apixio.model.converter.bin.converters;

import com.apixio.XUUID;
import com.apixio.model.converter.exceptions.NewAPOToJenaModelException;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.ApixioDate;
import com.apixio.model.owl.interfaces.donotimplement.Human;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimableEvent;
import com.apixio.model.owl.interfaces.donotimplement.SourceType;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by jctoledo on 4/11/16.
 */
//TODO - convertFile is orphaned -> nobody is calling this method WTF
public class NewAPOToRDFModel implements Closeable {
    static final Logger logger = Logger.getLogger(NewAPOToRDFModel.class);
    private RDFModel model;
    private Patient newPatient;

    public NewAPOToRDFModel(Patient newPatient) {
        this();
        this.newPatient = newPatient;
        convertPatient(newPatient);
        this.model.setPatientXUUID(newPatient.getInternalXUUID());
    }

    public NewAPOToRDFModel() {
        this.model = new RDFModel();

    }

    public Resource convertInsuranceClaimableEvent(InsuranceClaimableEvent a) {
        if (a.getInternalXUUID() != null) {
            if (a instanceof Immunization) {
                return convertImmunization((Immunization) a);
            } else if (a instanceof Prescription) {
                return convertPrescription((Prescription) a);
            } else if (a instanceof PatientEncounter){
                return convertPatientEncounter((PatientEncounter) a);
            } else if (a instanceof Procedure) {
                return convertProcedure((Procedure) a);
            } else if (a instanceof InsuranceClaimableEvent) {
                if (a.getInternalXUUID() != null) {
                    try {
                        URI classUri = new URI(OWLVocabulary.OWLClass.InsuranceClaimableEvent.uri());
                        URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.InsuranceClaimableEvent.name()+"/"+ a.getInternalXUUID().toString());
                        String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                        String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                        Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                        if (a.getLastEditDate() != null) {
                            Resource dr = convertDate(a.getLastEditDate());
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                        }
                        if (a.getOtherIds() != null) {
                            for (XUUID i : a.getOtherIds()) {
                                rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                            }
                        }
                        if (a.getOriginalID() != null) {
                            Resource er = convertExternalIdentifier(a.getOriginalID());
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                        }
                        if (a.getOtherOriginalIDss() != null) {
                            for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                                Resource eir = convertExternalIdentifier(ei);
                                rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                            }
                        }
                        if (a.getDataProcessingDetails() != null) {
                            for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                                Resource dpdr = convertDataProcessingDetails(dpd);
                                rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                            }
                        }
                        return rm;
                    } catch (URISyntaxException e) {
                        logger.error(e);
                        throw new NewAPOToJenaModelException(e);
                    }
                } else {
                    throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
                }
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
        return null;
    }


    public Resource convertApixioDate(ApixioDate a) {
        if (a.getInternalXUUID() != null) {
            if (a instanceof Date) {
                return convertDate((Date) a);
            } else if (a instanceof DateRange) {
                return convertDateRange((DateRange) a);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
        return null;
    }


    public Resource convertFile(File a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.File.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.File.name()+"/"+  a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());


                if (a.getFileDate() != null) {
                    Resource r = convertDate(a.getFileDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasFileDate.property(), r);
                }
                if (a.getMIMEType() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMIMEType.property(), a.getMIMEType());
                }
                if (a.getHash() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasHash.property(), a.getHash());
                }
                if (a.getFileURL() != null) {
                    //TODO -fix this - this should be an Object property - I am not entirely sure though :-S
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasFileURL.property(), a.getFileURL());
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public List<Resource> convertFeeForServiceInsuranceClaims(Collection<FeeForServiceInsuranceClaim> claims){
        List<Resource> addedResources = new ArrayList<>();
        Iterator<FeeForServiceInsuranceClaim> itr= claims.iterator();
        while(itr.hasNext()){
            FeeForServiceInsuranceClaim a = itr.next();
            String claimLogicalId  = null;
            //String.valueOf(DigestUtils.md5Hex(claim_orig_id_val+claimOriginalIDAssignAuthority+transactionDate))
            if(a.getOriginalID() != null && a.getOriginalID().getIDValue() != null && a.getOriginalID().getAssignAuthority() != null
                    && a.getInsuranceClaimProcessingDate() != null &&a.getInsuranceClaimProcessingDate().getDateValue().toString()!=null){
                ArrayList<String> ids = new ArrayList<>();
                ids.add(a.getOriginalID().getIDValue());
                ids.add(a.getOriginalID().getAssignAuthority());
                ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                XUUID x = createLogicalXUUID(ids,"FeeForServiceInsuranceClaim");
                a.setInternalXUUID(x);
                claimLogicalId = x.toString();
            } else{
                logger.debug("Could not find all required fields to create a logical identifier for a FeeForServiceInsuranceClaim! patientId : "+ this.model.getPatientXUUID().toString()+" FFS claimid "+ a.getInternalXUUID().toString() + " using XUUID instead!");
                claimLogicalId = a.getInternalXUUID().toString();
            }

            if (a.getInternalXUUID() != null) {
                try {
                    URI classUri = new URI(OWLVocabulary.OWLClass.FeeForServiceInsuranceClaim.uri());
                    URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.FeeForServiceInsuranceClaim.name()+"/"+  claimLogicalId);
                    String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                    String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                    Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, claimLogicalId);

                    if (a.getInsuranceClaimStatus() != null) {
                        //create a resource for the type
                        if (a.getInsuranceClaimStatus().getInternalXUUID() != null) {
                            ArrayList<String> ids = new ArrayList<>();
                            ids.add(a.getOriginalID().getIDValue());
                            ids.add(a.getOriginalID().getAssignAuthority());
                            ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids,"InsuranceClaimStatus");

                            URI classUri2 = new URI(OWLVocabulary.OWLClass.InsuranceClaimStatusType.uri());
                            URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + x.toString());
                            String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                            String instanceLabel2 = classLabel2 + " with XUUID : " + x.toString();

                            Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getInsuranceClaimStatus().getInternalXUUID().toString());
                            //add a value to the typed resource
                            if (a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name() != null) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name());
                            }
                            if (a.getInsuranceClaimStatus().getLastEditDate() != null) {
                                ids = new ArrayList<>();
                                ids.add(a.getInsuranceClaimStatus().getLastEditDate().getDateValue().toString());
                                x = createLogicalXUUID(ids,"InsuranceClaimStatusLastEditDate");
                                a.getInsuranceClaimStatus().getLastEditDate().setInternalXUUID(x);
                                Resource dateR = convertDate(a.getInsuranceClaimStatus().getLastEditDate());
                                r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                            }
                            if (a.getInsuranceClaimStatus().getOtherIds() != null) {
                                for (XUUID id : a.getInsuranceClaimStatus().getOtherIds()) {
                                    r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                                }
                            }
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimStatus.property(), r);
                        }
                    }

                    if (a.getInsuranceClaimCareSite() != null) {
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getOriginalID().getIDValue());
                        ids.add(a.getOriginalID().getAssignAuthority());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        XUUID x = createLogicalXUUID(ids,"InsuranceClaimCareSite");
                        a.getInsuranceClaimCareSite().setInternalXUUID(x);
                        Resource r = convertCareSite(a.getInsuranceClaimCareSite());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimCareSite.property(), r);
                    }

                    if(a.getFeeForServiceClaimTypeValue() != null){
                        if(a.getFeeForServiceClaimTypeValue().getFeeForServiceInsuranceClaimTypeValue() != null){
                            if(a.getFeeForServiceClaimTypeValue().getFeeForServiceInsuranceClaimTypeValue().name() !=null){
                                String val = a.getFeeForServiceClaimTypeValue().getFeeForServiceInsuranceClaimTypeValue().name();
                                TypeMapper tm = TypeMapper.getInstance();
                                RDFDatatype ratype = tm.getSafeTypeByName("http://apixio.com/ontology/alo#000226");
                                Literal l = this.model.createTypedLiteral(val, ratype);
                                rm.addLiteral(OWLVocabulary.OWLDataProperty.HasFeeForServiceInsuranceClaimTypeValue.property(), l);
                            }
                        }
                    }

                    if (a.getProviderType() != null){
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getOriginalID().getIDValue());
                        ids.add(a.getOriginalID().getAssignAuthority());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        ids.add(a.getProviderType().getClinicalCodeValue());
                        ids.add(a.getProviderType().getClinicalCodingSystem().getClinicalCodingSystemOID());

                        XUUID x = createLogicalXUUID(ids,"ProviderType");
                        a.getProviderType().setInternalXUUID(x);
                        Resource r = convertClinicalCode(a.getProviderType());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProviderType.property(), r);
                    }
                    if (a.getClaimSubmissionDate() != null) {
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getOriginalID().getIDValue());
                        ids.add(a.getOriginalID().getAssignAuthority());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        XUUID x = createLogicalXUUID(ids, "ClaimSubmissionDate");
                        a.getClaimSubmissionDate().setInternalXUUID(x);
                        Resource r = convertDate(a.getClaimSubmissionDate());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimSubmissionDate.property(), r);
                    }
                    if(a.getBillType() != null){
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getOriginalID().getIDValue());
                        ids.add(a.getOriginalID().getAssignAuthority());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        ids.add(a.getProviderType().getClinicalCodeValue());
                        ids.add(a.getProviderType().getClinicalCodingSystem().getClinicalCodingSystemOID());
                        XUUID x = createLogicalXUUID(ids, "BillType");
                        a.getBillType().setInternalXUUID(x);
                        Resource r = convertClinicalCode(a.getBillType());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasBillType.property(),r);
                    }
                    if (a.getSourceSystem() != null) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceSystem.property(), a.getSourceSystem());
                    }

                    if (a.getClaimedEvents() != null) {
                        for (InsuranceClaimableEvent ice : a.getClaimedEvents()) {
                            ArrayList<String> ids = new ArrayList<>();
                            ids.add(a.getOriginalID().getIDValue());
                            ids.add(a.getOriginalID().getAssignAuthority());
                            ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "ClaimedEvents");
                            ice.setInternalXUUID(x);

                            Resource r = convertInsuranceClaimableEvent(ice);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property(), r);
                        }
                    }

                    if (a.getSourceEncounterId() != null) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                    }
                    if (a.getOriginalID() != null) {
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getOriginalID().getIDValue());
                        ids.add(a.getOriginalID().getAssignAuthority());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        XUUID x = createLogicalXUUID(ids, "OriginalID");
                        a.getOriginalID().setInternalXUUID(x);

                        Resource er = convertExternalIdentifier(a.getOriginalID());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                    }
                    if (a.getOtherOriginalIDss() != null) {
                        for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                            ArrayList<String> ids = new ArrayList<>();
                            ids.add(ei.getIDValue());
                            ids.add(ei.getAssignAuthority());
                            ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "OtherOriginalIds");
                            ei.setInternalXUUID(x);
                            Resource eir = convertExternalIdentifier(ei);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                        }
                    }
                    if (a.getOtherIds() != null) {
                        for (XUUID i : a.getOtherIds()) {
                            rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                        }
                    }
                    if (a.getDataProcessingDetails() != null) {
                        for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {

                            Resource dpdr = convertDataProcessingDetails(dpd);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                        }
                    }
                    if (a.getLastEditDate() != null) {
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getOriginalID().getIDValue());
                        ids.add(a.getOriginalID().getAssignAuthority());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        ids.add(a.getLastEditDate().getDateValue().toString());
                        XUUID x = createLogicalXUUID(ids, "LastEditDate");
                        a.getLastEditDate().setInternalXUUID(x);

                        Resource dr = convertDate(a.getLastEditDate());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                    }
                    if (a.getServicesRendereds() != null) {
                        for (ClinicalCode cc : a.getServicesRendereds()) {
                            ArrayList<String> ids = new ArrayList<>();
                            ids.add(cc.getClinicalCodeValue());
                            ids.add(cc.getClinicalCodingSystem().getClinicalCodingSystemOID());
                            ids.add(cc.getClinicalCodeDisplayName());
                            ids.add(a.getOriginalID().getIDValue());
                            ids.add(a.getOriginalID().getAssignAuthority());
                            ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "ServicesRendered");
                            cc.setInternalXUUID(x);

                            Resource r = convertClinicalCode(cc);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasServicesRendered.property(), r);
                        }
                    }
                    if (a.getDeleteIndicator() != null && a.getDeleteIndicator() == true) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDeleteIndicator.property(), a.getDeleteIndicator());
                    }
                    if(a.getAddIndicator() != null && a.getAddIndicator() == true){
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAddIndicator.property(), a.getAddIndicator());
                    }
                    if (a.getDiagnosisCodes() != null) {
                        for (ClinicalCode cc : a.getDiagnosisCodes()) {
                            ArrayList<String> ids = new ArrayList<>();
                            ids.add(cc.getClinicalCodeValue());
                            ids.add(cc.getClinicalCodingSystem().getClinicalCodingSystemOID());
                            ids.add(cc.getClinicalCodeDisplayName());
                            ids.add(a.getOriginalID().getIDValue());
                            ids.add(a.getOriginalID().getAssignAuthority());
                            ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "Diagnosis");
                            cc.setInternalXUUID(x);

                            Resource r = convertClinicalCode(cc);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDiagnosisCode.property(), r);
                        }
                    }
                    if (a.getInsuranceClaimProcessingDate() != null){
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        XUUID x = createLogicalXUUID(ids, "ClaimProcessingDate");
                        a.getInsuranceClaimProcessingDate().setInternalXUUID(x);

                        Resource r = convertDate(a.getInsuranceClaimProcessingDate());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimProcessingDate.property(), r);
                    }
                    if(a.getRenderingProviders() != null){
                        for (MedicalProfessional mp : a.getRenderingProviders()){
                             ArrayList<String> ids = new ArrayList<>();
                             ids.add(mp.getOriginalID().getIDValue());
                             ids.add(mp.getOriginalID().getAssignAuthority());

                            XUUID x = createLogicalXUUID(ids, "MedicalProfessional");
                            mp.setInternalXUUID(x);
                            Resource r = convertMedicalProfessional(mp);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasRenderingProvider.property(), r);
                        }
                    }
                    if (a.getDateOfServiceRange() != null) {
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                        ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());

                        XUUID x = createLogicalXUUID(ids, "DateofServiceRange");
                        a.getDateOfServiceRange().setInternalXUUID(x);

                        Resource r = convertDateRange(a.getDateOfServiceRange());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateOfServiceRange.property(), r);
                    }
                    if (a.getCMSClaimValidationResult() != null) {
                        ArrayList<String> ids = new ArrayList<>();
                        ids.add(a.getOriginalID().getIDValue());
                        ids.add(a.getOriginalID().getAssignAuthority());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                        ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                        XUUID x = createLogicalXUUID(ids, "CMSClaimValidationResult");

                        a.getCMSClaimValidationResult().setInternalXUUID(x);

                        Resource r = convertCMSClaimValidationResult(a.getCMSClaimValidationResult());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCMSClaimValidationResult.property(), r);
                    }

                    if(a.getBillingProviders() != null){
                        for(MedicalProfessional p : a.getBillingProviders()){
                            ArrayList<String> ids = new ArrayList<>();
                            if (p.getOriginalID() != null && p.getOriginalID().getIDValue() != null && p.getOriginalID().getAssignAuthority() !=null) {
                                ids.add(p.getOriginalID().getIDValue());
                                ids.add(p.getOriginalID().getAssignAuthority());

                                XUUID x = createLogicalXUUID(ids, "ProviderType");
                                p.setInternalXUUID(x);
                            }
                            Resource r = convertMedicalProfessional(p);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasBillingProvider.property(), r);
                        }
                    }
                    addedResources.add(rm);
                } catch (URISyntaxException e) {
                    logger.error(e);
                    throw new NewAPOToJenaModelException(e);
                }
            } else {
                throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
            }

        }
        return addedResources;
    }


    public Resource convertFeeForServiceInsuranceClaims(FeeForServiceInsuranceClaim a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.FeeForServiceInsuranceClaim.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.FeeForServiceInsuranceClaim.name()+"/"+  a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getInsuranceClaimStatus() != null) {
                    //create a resource for the type
                    if (a.getInsuranceClaimStatus().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.InsuranceClaimStatusType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + a.getInsuranceClaimStatus().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getInsuranceClaimStatus().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getInsuranceClaimStatus().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name());
                        }
                        if (a.getInsuranceClaimStatus().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getInsuranceClaimStatus().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getInsuranceClaimStatus().getOtherIds() != null) {
                            for (XUUID id : a.getInsuranceClaimStatus().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimStatus.property(), r);
                    }
                }

                if (a.getInsuranceClaimCareSite() != null) {
                    Resource r = convertCareSite(a.getInsuranceClaimCareSite());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimCareSite.property(), r);
                }

                if(a.getFeeForServiceClaimTypeValue() != null){
                    if(a.getFeeForServiceClaimTypeValue().getFeeForServiceInsuranceClaimTypeValue() != null){
                        if(a.getFeeForServiceClaimTypeValue().getFeeForServiceInsuranceClaimTypeValue().name() !=null){
                            String val = a.getFeeForServiceClaimTypeValue().getFeeForServiceInsuranceClaimTypeValue().name();
                            TypeMapper tm = TypeMapper.getInstance();
                            RDFDatatype ratype = tm.getSafeTypeByName("http://apixio.com/ontology/alo#000226");
                            Literal l = this.model.createTypedLiteral(val, ratype);
                            rm.addLiteral(OWLVocabulary.OWLDataProperty.HasFeeForServiceInsuranceClaimTypeValue.property(), l);
                        }
                    }
                }

                if (a.getProviderType() != null){
                    Resource r = convertClinicalCode(a.getProviderType());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProviderType.property(), r);
                }
                if (a.getClaimSubmissionDate() != null) {
                    Resource r = convertDate(a.getClaimSubmissionDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimSubmissionDate.property(), r);
                }
                if(a.getBillType() != null){
                    Resource r = convertClinicalCode(a.getBillType());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasBillType.property(),r);
                }
                if (a.getSourceSystem() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceSystem.property(), a.getSourceSystem());
                }

                if (a.getClaimedEvents() != null) {
                    for (InsuranceClaimableEvent ice : a.getClaimedEvents()) {
                        Resource r = convertInsuranceClaimableEvent(ice);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property(), r);
                    }
                }

                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getServicesRendereds() != null) {
                    for (ClinicalCode cc : a.getServicesRendereds()) {
                        Resource r = convertClinicalCode(cc);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasServicesRendered.property(), r);
                    }
                }
                if (a.getDeleteIndicator() != null && a.getDeleteIndicator() == true) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDeleteIndicator.property(), a.getDeleteIndicator());
                }
                if(a.getAddIndicator() != null && a.getAddIndicator() == true){
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAddIndicator.property(), a.getAddIndicator());
                }
                if (a.getDiagnosisCodes() != null) {
                    for (ClinicalCode cc : a.getDiagnosisCodes()) {
                        Resource r = convertClinicalCode(cc);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDiagnosisCode.property(), r);
                    }
                }
                if (a.getInsuranceClaimProcessingDate() != null){
                    Resource r = convertDate(a.getInsuranceClaimProcessingDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimProcessingDate.property(), r);
                }
                if(a.getRenderingProviders() != null){
                    for (MedicalProfessional mp : a.getRenderingProviders()){
                        Resource r = convertMedicalProfessional(mp);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasRenderingProvider.property(), r);
                    }
                }
                if (a.getDateOfServiceRange() != null) {
                    Resource r = convertDateRange(a.getDateOfServiceRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateOfServiceRange.property(), r);
                }
                if (a.getCMSClaimValidationResult() != null) {
                    Resource r = convertCMSClaimValidationResult(a.getCMSClaimValidationResult());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCMSClaimValidationResult.property(), r);
                }

                if(a.getBillingProviders() != null){
                    for(MedicalProfessional p : a.getBillingProviders()){
                        Resource r = convertMedicalProfessional(p);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasBillingProvider.property(), r);
                    }
                }

                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    public List<Resource> convertRiskAdjustmentInsuranceClaims(Collection<RiskAdjustmentInsuranceClaim> claims){
        List<Resource> addedResources = new ArrayList<>();

        Iterator<RiskAdjustmentInsuranceClaim> itr= claims.iterator();
        while(itr.hasNext()) {
            RiskAdjustmentInsuranceClaim a = itr.next();
            boolean isMAO = isMAO(a);
            boolean isRAPS = isRAPS(a);
            List<Boolean> boolz = new ArrayList<>();
            boolz.add(isRAPS); boolz.add(isMAO);
            int trueCount = 0;
            int falseCount = 0;
            for (Boolean b:boolz){
                if(b){
                    trueCount ++;
                }else{
                    falseCount ++;
                }
            }
            if(trueCount != falseCount){
                logger.error("Found a RiskAdjustedInsuranceClaim that I cannot distinguish between RAPS and MAO - patientid "+this.newPatient.getInternalXUUID());
                return addedResources;
            }

            String claimLogicalId = null;
            //String.valueOf(DigestUtils.md5Hex(claim_orig_id_val+claimOriginalIDAssignAuthority+transactionDate))
                ArrayList<String> ids = new ArrayList<>();
                if (isMAO){
                    ExternalIdentifier ei = null;
                    for(ExternalIdentifier i : a.getOtherOriginalIDss()){
                        if(i.getAssignAuthority().equals("ENCOUNTER_ICN")){
                            ei = i;
                            break;
                        }
                    }
                    if (ei != null){
                        ids.add(ei.getIDValue());
                        ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        XUUID x = createLogicalXUUID(ids, "RiskAdjustmentInsuranceClaim");
                        a.setInternalXUUID(x);
                        claimLogicalId = x.toString();
                    }else{
                        logger.debug("found a MAO claim without an encounter ICN! in patient : "+this.newPatient.getInternalXUUID());
                        continue;
                    }
                } else if (isRAPS){
                    if(a.getDateOfServiceRange() !=null && a.getDateOfServiceRange().getStartDate().getDateValue() != null && a.getDateOfServiceRange().getEndDate().getDateValue()!=null){
                        if(a.getDiagnosisCodes() != null && a.getProviderType() != null && a.getProviderType().getClinicalCodeValue() != null && a.getCMSClaimValidationResult() !=null && a.getCMSClaimValidationResult().getValidatedClaimTransactionDate() !=null && a.getCMSClaimValidationResult().getValidatedClaimTransactionDate().getDateValue() != null){
                            List<String> dxs = new ArrayList<>();
                            for(ClinicalCode cc : a.getDiagnosisCodes()){
                                dxs.add(cc.getClinicalCodeValue()+cc.getClinicalCodingSystem());
                            }
                            Collections.sort(dxs);
                            String dxStr = null;
                            for (String s: dxs){
                                dxStr+= s;
                            }
                            ids = new ArrayList<>();
                            ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            ids.add(dxStr);
                            ids.add(a.getProviderType().getClinicalCodeValue());
                            ids.add(a.getProviderType().getClinicalCodingSystem().getClinicalCodingSystemOID());
                            ids.add(a.getCMSClaimValidationResult().getValidatedClaimTransactionDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "RiskAdjustedInsuranceClaim");
                            a.setInternalXUUID(x);
                            claimLogicalId = x.toString();
                        } else if( a.getCMSClaimValidationResult() != null && a.getCMSClaimValidationResult().getValidatedClaimTransactionDate() != null && a.getCMSClaimValidationResult().getValidatedClaimTransactionDate().getDateValue().toString() != null && a.getProviderType() == null && a.getDiagnosisCodes()!=null){
                            List<String> dxs = new ArrayList<>();
                            for(ClinicalCode cc : a.getDiagnosisCodes()){
                                dxs.add(cc.getClinicalCodeValue()+cc.getClinicalCodingSystem());
                            }
                            Collections.sort(dxs);
                            String dxStr = null;
                            for (String s: dxs){
                                dxStr+= s;
                            }
                            ids = new ArrayList<>();
                            ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            ids.add(dxStr);
                            ids.add("PT");
                            ids.add(a.getCMSClaimValidationResult().getValidatedClaimTransactionDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "RiskAdjustedInsuranceClaim");
                            a.setInternalXUUID(x);
                            claimLogicalId = x.toString();

                        } else if ( a.getCMSClaimValidationResult() != null && a.getCMSClaimValidationResult().getValidatedClaimTransactionDate() != null && a.getCMSClaimValidationResult().getValidatedClaimTransactionDate().getDateValue().toString() != "" && a.getDiagnosisCodes() != null) {
                            List<String> dxs = new ArrayList<>();
                            for(ClinicalCode cc : a.getDiagnosisCodes()){
                                dxs.add(cc.getClinicalCodeValue()+cc.getClinicalCodingSystem());
                            }
                            Collections.sort(dxs);
                            String dxStr = null;
                            for (String s: dxs){
                                dxStr+= s;
                            }
                            ids = new ArrayList<>();
                            ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            ids.add(dxStr);
                            ids.add("PT");
                            ids.add(a.getCMSClaimValidationResult().getValidatedClaimTransactionDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "RiskAdjustedInsuranceClaim");
                            a.setInternalXUUID(x);
                            claimLogicalId = x.toString();

                        } else {
                            ids = new ArrayList<>();
                            ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            ids.add("PT");
                            ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            XUUID x = createLogicalXUUID(ids, "RiskAdjustedInsuranceClaim");
                            a.setInternalXUUID(x);
                            claimLogicalId = x.toString();
                        }
                    }
                }

                try {
                    URI classUri = new URI(OWLVocabulary.OWLClass.RiskAdjustmentInsuranceClaim.uri());
                    URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.RiskAdjustmentInsuranceClaim.name()+"/"+ claimLogicalId);
                    String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                    String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                    Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, claimLogicalId);

                    if (a.getDateOfServiceRange() != null) {
                        ids = new ArrayList<>();
                        ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                        ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());

                        XUUID x = createLogicalXUUID(ids, "DateofServiceRange");
                        a.getDateOfServiceRange().setInternalXUUID(x);

                        Resource r = convertDateRange(a.getDateOfServiceRange());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateOfServiceRange.property(), r);
                    }

                    if (a.getInsuranceClaimStatus() != null) {
                        //create a resource for the type
                        if (a.getInsuranceClaimStatus().getInternalXUUID() != null) {
                            URI classUri2 = new URI(OWLVocabulary.OWLClass.InsuranceClaimStatusType.uri());
                            URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + a.getInsuranceClaimStatus().getInternalXUUID());
                            String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                            String instanceLabel2 = classLabel2 + " with XUUID : " + a.getInsuranceClaimStatus().getInternalXUUID();
                            Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getInsuranceClaimStatus().getInternalXUUID().toString());
                            //add a value to the typed resource
                            if (a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name() != null) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name());
                            }
                            if (a.getInsuranceClaimStatus().getLastEditDate() != null) {
                                Resource dateR = convertDate(a.getInsuranceClaimStatus().getLastEditDate());
                                r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                            }
                            if (a.getInsuranceClaimStatus().getOtherIds() != null) {
                                for (XUUID id : a.getInsuranceClaimStatus().getOtherIds()) {
                                    r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                                }
                            }
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimStatus.property(), r);
                        }
                    }

                    if(a.getRiskAdjustmentInsuranceClaimType() != null){
                        if(a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue() != null){
                            if(a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name() !=null){
                                String val = a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name();
                                TypeMapper tm = TypeMapper.getInstance();
                                RDFDatatype ratype = tm.getSafeTypeByName("http://apixio.com/ontology/alo#000203");
                                Literal l = this.model.createTypedLiteral(val, ratype);
                                rm.addLiteral(OWLVocabulary.OWLDataProperty.HasRiskAdjustmentInsuranceClaimTypeValue.property(), l);
                            }
                        }
                    }

                    if(a.getPatientEncounterSwitchType() != null){
                        if(a.getPatientEncounterSwitchType().getPatientEncounterSwitchTypeValue() != null){
                            if(a.getPatientEncounterSwitchType().getPatientEncounterSwitchTypeValue().name() != null){
                                rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPatientEncounterSwitchTypeValue.property(), a.getPatientEncounterSwitchType().getPatientEncounterSwitchTypeValue().name());
                            }
                        }
                    }
                    if (a.getMedicareAdvantagePolicyURI() != null){
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMedicareAdvantagePolicyURI.property(), a.getMedicareAdvantagePolicyURI());
                    }
                    if(a.getInsuranceClaimProcessingDate() != null){
                        ids = new ArrayList<>();
                        ids.add(a.getInsuranceClaimProcessingDate().toString());
                        XUUID x = createLogicalXUUID(ids, "ProcessingDate");
                        a.getInsuranceClaimProcessingDate().setInternalXUUID(x);
                        Resource r = convertDate(a.getInsuranceClaimProcessingDate());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimProcessingDate.property(), r);
                    }
                    if (a.getDeleteIndicator() != null) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDeleteIndicator.property(), a.getDeleteIndicator());
                    }
                    if(a.getAddIndicator() != null){
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAddIndicator.property(), a.getAddIndicator());
                    }
                    if (a.getClaimSubmissionDate() != null) {
                        ids = new ArrayList<>();
                        ids.add(a.getInsuranceClaimProcessingDate().toString());
                        XUUID x = createLogicalXUUID(ids, "SubmissionDate");
                        a.getClaimSubmissionDate().setInternalXUUID(x);
                        Resource r = convertDate(a.getClaimSubmissionDate());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimSubmissionDate.property(), r);
                    }
                    if (a.getClaimedEvents() != null) {
                        for (InsuranceClaimableEvent ice : a.getClaimedEvents()) {
                            ids = new ArrayList<>();
                            ids.add(a.getOriginalID().getIDValue());
                            ids.add(a.getOriginalID().getAssignAuthority());
                            if (a.getInsuranceClaimProcessingDate() != null) {
                                ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                            }
                            if(a.getDateOfServiceRange() != null) {
                                ids.add(a.getDateOfServiceRange().getStartDate().getDateValue().toString());
                                ids.add(a.getDateOfServiceRange().getEndDate().getDateValue().toString());
                            }
                            XUUID x = createLogicalXUUID(ids, "ClaimedEvents");
                            ice.setInternalXUUID(x);


                            Resource r = convertInsuranceClaimableEvent(ice);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property(), r);
                        }
                    }
                    if (a.getProviderType() != null){
                        ids = new ArrayList<>();
                        if(a.getOriginalID() != null) {
                            ids.add(a.getOriginalID().getIDValue());
                            ids.add(a.getOriginalID().getAssignAuthority());
                        }
                        if(a.getInsuranceClaimProcessingDate() != null) {
                            ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        }
                        ids.add(a.getProviderType().getClinicalCodeValue());
                        ids.add(a.getProviderType().getClinicalCodingSystem().getClinicalCodingSystemOID());

                        XUUID x = createLogicalXUUID(ids,"ProviderType");
                        a.getProviderType().setInternalXUUID(x);
                        Resource r = convertClinicalCode(a.getProviderType());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProviderType.property(), r);
                    }
                    if (a.getInsuranceClaimCareSite() != null) {
                        ids = new ArrayList<>();
                        if(a.getOriginalID() != null) {
                            ids.add(a.getOriginalID().getIDValue());
                            ids.add(a.getOriginalID().getAssignAuthority());
                        }
                        if(a.getInsuranceClaimProcessingDate() != null) {
                            ids.add(a.getInsuranceClaimProcessingDate().getDateValue().toString());
                        }
                        XUUID x = createLogicalXUUID(ids, "InsuranceClaimeCareSite");
                        a.getInsuranceClaimCareSite().setInternalXUUID(x);
                        Resource r = convertCareSite(a.getInsuranceClaimCareSite());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimCareSite.property(), r);
                    }

                    if (a.getCMSClaimValidationResult() != null) {
                        ids = new ArrayList<>();
                        ids.add(a.getInternalXUUID().toString());
                        XUUID x = createLogicalXUUID(ids, "CMSValidationResult");
                        a.getCMSClaimValidationResult().setInternalXUUID(x);
                        Resource r = convertCMSClaimValidationResult(a.getCMSClaimValidationResult());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCMSClaimValidationResult.property(), r);
                    }
                    if (a.getSourceEncounterId() != null) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                    }
                    if (a.getOriginalID() != null) {
                        ids = new ArrayList<>();
                        ids.add(a.getInternalXUUID().toString());
                        ids.add(a.getOriginalID().getIDValue());
                        if(a.getOriginalID().getAssignAuthority()!=null) {
                            ids.add(a.getOriginalID().getAssignAuthority());
                        }
                        XUUID x = createLogicalXUUID(ids, "OriginalID");
                        a.getOriginalID().setInternalXUUID(x);
                        Resource er = convertExternalIdentifier(a.getOriginalID());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                    }
                    if (a.getOtherOriginalIDss() != null) {
                        for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                            ids = new ArrayList<>();
                            ids.add(a.getInternalXUUID().toString());
                            ids.add(ei.getIDValue());
                            if(ei.getAssignAuthority()!=null) {
                                ids.add(ei.getAssignAuthority());
                            }
                            XUUID x = createLogicalXUUID(ids, "OtherOriginalID");
                            ei.setInternalXUUID(x);
                            Resource eir = convertExternalIdentifier(ei);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                        }
                    }
                    if (a.getOtherIds() != null) {
                        for (XUUID i : a.getOtherIds()) {
                            rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                        }
                    }
                    if (a.getDataProcessingDetails() != null) {
                        for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                            Resource dpdr = convertDataProcessingDetails(dpd);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                        }
                    }
                    if (a.getLastEditDate() != null) {
                        ids = new ArrayList<>();
                        ids.add(a.getInternalXUUID().toString());
                        ids.add(a.getLastEditDate().getDateValue().toString());

                        XUUID x = createLogicalXUUID(ids, "LastEditDate");
                        a.getLastEditDate().setInternalXUUID(x);
                        Resource dr = convertDate(a.getLastEditDate());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                    }

                    if (a.getSourceSystem() != null) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceSystem.property(), a.getSourceSystem());
                    }
                    if (a.getDiagnosisCodes() != null) {
                        for (ClinicalCode cc : a.getDiagnosisCodes()) {
                            ids = new ArrayList<>();
                            ids.add(a.getInternalXUUID().toString());
                            ids.add(cc.getClinicalCodeValue());
                            if(cc.getClinicalCodingSystem() != null){
                                ids.add(cc.getClinicalCodingSystem().getClinicalCodingSystemOID());
                            }
                            XUUID x = createLogicalXUUID(ids, "Diagnosis");
                            cc.setInternalXUUID(x);
                            Resource r = convertClinicalCode(cc);
                            rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDiagnosisCode.property(), r);
                        }
                    }
                    addedResources.add(rm);
                } catch (URISyntaxException e) {
                    logger.error(e);
                    throw new NewAPOToJenaModelException(e);
                }

        }
        return addedResources;
    }

    public Resource convertRiskAdjustmentInsuranceClaim(RiskAdjustmentInsuranceClaim a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.RiskAdjustmentInsuranceClaim.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.RiskAdjustmentInsuranceClaim.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getDateOfServiceRange() != null) {
                    Resource r = convertDateRange(a.getDateOfServiceRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateOfServiceRange.property(), r);
                }

                if (a.getInsuranceClaimStatus() != null) {
                    //create a resource for the type
                    if (a.getInsuranceClaimStatus().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.InsuranceClaimStatusType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + a.getInsuranceClaimStatus().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getInsuranceClaimStatus().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getInsuranceClaimStatus().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getInsuranceClaimStatus().getInsuranceClaimStatusValue().name());
                        }
                        if (a.getInsuranceClaimStatus().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getInsuranceClaimStatus().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getInsuranceClaimStatus().getOtherIds() != null) {
                            for (XUUID id : a.getInsuranceClaimStatus().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimStatus.property(), r);
                    }
                }

                if(a.getRiskAdjustmentInsuranceClaimType() != null){
                    if(a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue() != null){
                        if(a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name() !=null){
                            String val = a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name();
                            TypeMapper tm = TypeMapper.getInstance();
                            RDFDatatype ratype = tm.getSafeTypeByName("http://apixio.com/ontology/alo#000203");
                            Literal l = this.model.createTypedLiteral(val, ratype);
                            rm.addLiteral(OWLVocabulary.OWLDataProperty.HasRiskAdjustmentInsuranceClaimTypeValue.property(), l);
                        }
                    }
                }

                if(a.getPatientEncounterSwitchType() != null){
                    if(a.getPatientEncounterSwitchType().getPatientEncounterSwitchTypeValue() != null){
                        if(a.getPatientEncounterSwitchType().getPatientEncounterSwitchTypeValue().name() != null){
                            rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPatientEncounterSwitchTypeValue.property(), a.getPatientEncounterSwitchType().getPatientEncounterSwitchTypeValue().name());
                        }
                    }
                }
                if (a.getMedicareAdvantagePolicyURI() != null){
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMedicareAdvantagePolicyURI.property(), a.getMedicareAdvantagePolicyURI());
                }
                if(a.getInsuranceClaimProcessingDate() != null){
                    Resource r = convertDate(a.getInsuranceClaimProcessingDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimProcessingDate.property(), r);
                }
                if (a.getDeleteIndicator() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDeleteIndicator.property(), a.getDeleteIndicator());
                }
                if(a.getAddIndicator() != null){
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAddIndicator.property(), a.getAddIndicator());
                }
                if (a.getClaimSubmissionDate() != null) {
                    Resource r = convertDate(a.getClaimSubmissionDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimSubmissionDate.property(), r);
                }
                if (a.getClaimedEvents() != null) {
                    for (InsuranceClaimableEvent ice : a.getClaimedEvents()) {
                        Resource r = convertInsuranceClaimableEvent(ice);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property(), r);
                    }
                }
                if (a.getProviderType() != null){
                    Resource r = convertClinicalCode(a.getProviderType());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProviderType.property(), r);
                }
                if (a.getInsuranceClaimCareSite() != null) {
                    Resource r = convertCareSite(a.getInsuranceClaimCareSite());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceClaimCareSite.property(), r);
                }

                if (a.getCMSClaimValidationResult() != null) {
                    Resource r = convertCMSClaimValidationResult(a.getCMSClaimValidationResult());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCMSClaimValidationResult.property(), r);
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }

                if (a.getSourceSystem() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceSystem.property(), a.getSourceSystem());
                }
                if (a.getDiagnosisCodes() != null) {
                    for (ClinicalCode cc : a.getDiagnosisCodes()) {
                        Resource r = convertClinicalCode(cc);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDiagnosisCode.property(), r);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    private boolean isMAO(RiskAdjustmentInsuranceClaim a){
        if(a.getRiskAdjustmentInsuranceClaimType() != null && a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue() !=null && a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name()!=null) {
            if (a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name().contains("MAO")) {
                return true;
            }
        }
        return false;
    }

    private boolean isRAPS(RiskAdjustmentInsuranceClaim a){
        if(a.getRiskAdjustmentInsuranceClaimType() != null && a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue() !=null && a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name()!=null) {
            if (a.getRiskAdjustmentInsuranceClaimType().getRiskAdjustmentInsuranceClaimTypeValue().name().contains("RAPS")) {
                return true;
            }
        }

        return false;
    }


    public Resource convertPatient(Patient a) {
        if (a.getInternalXUUID() != null) {
            try {
                this.model.setPatientXUUID(a.getInternalXUUID());
                URI classUri = new URI(OWLVocabulary.OWLClass.Patient.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Patient.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getAllergies() != null) {
                    for (Allergy al : a.getAllergies()) {
                        Resource r = convertAllergy(al);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAllergy.property(), r);
                    }
                }
                if (a.getDocuments() != null) {
                    for (Document d : a.getDocuments()) {
                        Resource r = convertDocument(d);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDocument.property(), r);
                    }
                }
                if (a.getFeeForServiceClaims() != null) {
                    List<Resource> ffsclaims = convertFeeForServiceInsuranceClaims(a.getFeeForServiceClaims());
                    for (Resource r: ffsclaims){
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasFeeForServiceClaim.property(), r);
                    }
                }
//                if (a.getRiskAdjustmentClaims() != null) {
//                    List<Resource> claimz = convertRiskAdjustmentInsuranceClaims(a.getRiskAdjustmentClaims());
//                    for(Resource r: claimz){
//                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasRiskAdjustmentClaim.property(),r);
//                    }
//                }
                if (a.getRiskAdjustmentClaims() != null) {
                    for (RiskAdjustmentInsuranceClaim ax : a.getRiskAdjustmentClaims()) {
                        Resource r = convertRiskAdjustmentInsuranceClaim(ax);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasRiskAdjustmentClaim.property(), r);
                    }
                }
                if (a.getMedicalProcedures() != null) {
                    for (Procedure p : a.getMedicalProcedures()) {
                        Resource r = convertProcedure(p);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasMedicalProcedure.property(), r);
                    }
                }
                if (a.getDemographics() != null) {
                    Resource r = convertDemographics(a.getDemographics());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDemographics.property(), r);
                }
                if (a.getProblems() != null) {
                    for (Problem p : a.getProblems()) {
                        Resource r = convertProblem(p);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProblem.property(), r);
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getEncounters() != null) {
                    for (PatientEncounter pe : a.getEncounters()) {
                        Resource r = convertPatientEncounter(pe);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasEncounter.property(), r);
                    }
                }
                if (a.getPrescriptions() != null) {
                    for (Prescription p : a.getPrescriptions()) {
                        Resource r = convertPrescription(p);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrescription.property(), r);
                    }
                }
                if (a.getBiometricValues() != null) {
                    for (BiometricValue b : a.getBiometricValues()) {
                        Resource r = convertBiometricValue(b);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasBiometricValue.property(), r);
                    }
                }
                if (a.getImmunizations() != null) {
                    for (Immunization i : a.getImmunizations()) {
                        Resource r = convertImmunization(i);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasImmunization.property(), r);
                    }
                }
                if (a.getSocialHistories() != null) {
                    for (SocialHistory s : a.getSocialHistories()) {
                        Resource r = convertSocialHistory(s);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSocialHistory.property(), r);
                    }
                }
                if (a.getFamilyHistories() != null) {
                    for (FamilyHistory f : a.getFamilyHistories()) {
                        Resource r = convertFamilyHistory(f);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasFamilyHistory.property(), r);
                    }
                }
                if (a.getPatientCareSites() != null) {
                    for (CareSite c : a.getPatientCareSites()) {
                        Resource r = convertCareSite(c);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCareSite.property(), r);
                    }
                }
                if (a.getMedicalProfessionals() != null) {
                    for (MedicalProfessional p : a.getMedicalProfessionals()) {
                        Resource r = convertMedicalProfessional(p);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasMedicalProfessional.property(), r);
                    }
                }
                //TODO - write convert insurance policy
                if (a.getInsurancePolicies() != null) {
                    for (InsurancePolicy i : a.getInsurancePolicies()) {
                        Resource r = convertInsurancePolicy(i);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsurancePolicy.property(), r);
                    }
                }
                if (a.getAlternateContactDetailss() != null) {
                    for (ContactDetails c : a.getAlternateContactDetailss()) {
                        Resource r = convertContactDetails(c);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAlternateContactDetails.property(), r);
                    }
                }
                if (a.getContactDetails() != null) {
                    Resource r = convertContactDetails(a.getContactDetails());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasContactDetails.property(), r);
                }
                if (a.getAlternateDemographicss() != null) {
                    for (Demographics d : a.getAlternateDemographicss()) {
                        Resource r = convertDemographics(d);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAlternateDemographics.property(), r);
                    }
                }
                if (a.getPrimaryCareProviders() != null) {
                    for (PrimaryCareProvider p : a.getPrimaryCareProviders()) {
                        Resource r = convertPrimaryCareProvider(p);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrimaryCareProvider.property(), r);
                    }
                }
                if (a.getLabResults() != null) {
                    for (LabResult l : a.getLabResults()) {
                        Resource r = convertLabResult(l);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLabResult.property(), r);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertSourceType(SourceType s){
        if(s.getInternalXUUID() != null){
            try{
                Random rand = new Random();
                int  n = rand.nextInt(500000000) + 1;
                URI classUri = new URI(OWLVocabulary.OWLClass.Source.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Source.name()+"/"+ s.getInternalXUUID().toString()+Integer.toString(n));
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + s.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, s.getInternalXUUID().toString());
                if(s.getLastEditDate() != null) {
                    Resource dr = convertDate(s.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if(s.getSourceTypeStringValue() != null){
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceTypeStringValue.property(), s.getSourceTypeStringValue());
                }
                if(s.getSourceTypeValue().name() != null){
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceTypeValue.property(), s.getSourceTypeValue().name());
                }
                return rm;

            } catch (URISyntaxException e){
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            } }
            else {
                throw new NewAPOToJenaModelException("found an " + s.getClass().getName() + " without an XUUID");
            }

    }

    public Resource convertSource(Source s) {
        if (s.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Source.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Source.name()+"/"+ s.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + s.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, s.getInternalXUUID().toString());
                if (s.getOriginalID() != null) {
                    Resource origId = convertExternalIdentifier(s.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), origId);
                }
                if (s.getOtherOriginalIDss() != null) {
                    //TODO - I could make use of the RDFList pattern : https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/RDFList.html I am not doing so because this will affect how SPARQL queries are constructed
                    for (ExternalIdentifier ei : s.getOtherOriginalIDss()) {
                        Resource o = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), o);
                    }
                }
                if (s.getSourceAuthor() != null) {
                    Resource author = convertHuman(s.getSourceAuthor());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSourceAuthor.property(), author);
                }
                if(s.getSourceType() != null){
                    Resource st = convertSourceType(s.getSourceType());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSourceType.property(), st);
                }

                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + s.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertMedicareInsurancePolicy(MedicareInsurancePolicy a){
        if(a.getInternalXUUID() != null){
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.MedicareInsurancePolicy.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.MedicareInsurancePolicy.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if(a.getHICNNumber() != null){
                    Resource r = convertHICN(a.getHICNNumber());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasHICNNumber.property(), r);
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getInsurancePolicyDateRange() != null) {
                    Resource dr = convertDateRange(a.getInsurancePolicyDateRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsurancePolicyDateRange.property(), dr);
                }
                if (a.getInsuranceSequnceNumberValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasInsuranceSequnceNumberValue.property(), a.getInsuranceSequnceNumberValue());
                }
                if (a.getMemberNumber() != null) {
                    Resource r = convertExternalIdentifier(a.getMemberNumber());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasMemberNumber.property(), r);
                }
                if (a.getInsurnacePolicyName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasInsurnacePolicyName.property(), a.getInsurnacePolicyName());
                }
                if (a.getSubscriberID() != null) {
                    Resource r = convertExternalIdentifier(a.getSubscriberID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSubscriberID.property(), r);
                }
                if (a.getInsurancePlan() != null) {
                    Resource r = convertInsurancePlan(a.getInsurancePlan());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsurancePlan.property(), r);
                }
                if(a.getHICNNumber() != null){
                    Resource r = convertHICN(a.getHICNNumber());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasHICNNumber.property(), r);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertInsurancePolicy(InsurancePolicy a) {
        if (a.getInternalXUUID() != null) {
            try {
                if (a instanceof MedicareInsurancePolicy) {
                    return convertMedicareInsurancePolicy((MedicareInsurancePolicy) a);
                }
                URI classUri = new URI(OWLVocabulary.OWLClass.InsurancePolicy.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.InsurancePolicy.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }

                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getInsurancePolicyDateRange() != null) {
                    Resource dr = convertDateRange(a.getInsurancePolicyDateRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsurancePolicyDateRange.property(), dr);
                }
                if (a.getInsuranceSequnceNumberValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasInsuranceSequnceNumberValue.property(), a.getInsuranceSequnceNumberValue());
                }
                if (a.getMemberNumber() != null) {
                    Resource r = convertExternalIdentifier(a.getMemberNumber());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasMemberNumber.property(), r);
                }
                if (a.getInsurnacePolicyName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasInsurnacePolicyName.property(), a.getInsurnacePolicyName());
                }
                if (a.getSubscriberID() != null) {
                    Resource r = convertExternalIdentifier(a.getSubscriberID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSubscriberID.property(), r);
                }
                if (a.getInsurancePlan() != null) {
                    Resource r = convertInsurancePlan(a.getInsurancePlan());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsurancePlan.property(), r);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertInsurancePlan(InsurancePlan a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.InsurancePlan.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.InsurancePlan.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getGroupNumber() != null) {
                    Resource r = convertExternalIdentifier(a.getGroupNumber());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSubscriberID.property(), r);
                }
                if (a.getInsurancePlanName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasInsurancePlanName.property(), a.getInsurancePlanName());
                }

                if (a.getInsuranceType() != null) {
                    //create a resource for the type
                    if (a.getInsuranceType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.PlanType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.PlanType.name()+"/"+ a.getInsuranceType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getInsuranceType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getInsuranceType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getInsuranceType().getPayorTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getInsuranceType().getPayorTypeValue().name());
                        }
                        if (a.getInsuranceType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getInsuranceType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getInsuranceType().getOtherIds() != null) {
                            for (XUUID id : a.getInsuranceType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceType.property(), r);
                    }

                }
                if (a.getInsuranceCompany() != null) {
                    Resource r = convertInsuranceCompany(a.getInsuranceCompany());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasInsuranceCompany.property(), r);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertInsuranceCompany(InsuranceCompany a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.InsuranceCompany.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.InsuranceCompany.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getContactDetails() != null) {
                    Resource r = convertContactDetails(a.getContactDetails());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasContactDetails.property(), r);
                }
                if (a.getNationalProviderIdentifier() != null) {
                    Resource r = convertExternalIdentifier(a.getNationalProviderIdentifier());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasNationalProviderIdentifier.property(), r);
                }
                if (a.getOrganizationNameValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOrganizationNameValue.property(), a.getOrganizationNameValue());
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }
//    public Resource createResourceWithTypeAndLabel(java.lang.Class klass){
//        URI classUri, instanceUri = null;
//        String classLabel, instanceLabel = null;
//        if(klass.isInstance(OWLVocabulary.OWLClass.class)){
//            klass.getClass()
//            classUri = new URI(OWLVocabulary.OWLClass.klass.uri());
//            instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + s.getInternalXUUID().toString());
//            String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
//            String instanceLabel = classLabel + " with XUUID : " + s.getInternalXUUID();
//
//
//        }else if (o instanceof OWLVocabulary.OWLObjectProperty){
//
//        }else if(o instanceof OWLVocabulary.OWLDataProperty){
//
//        }else{
//            throw new NewAPOToJenaModelException("Tried to create resource from invalid object type!");
//        }
//    }


    private XUUID createLogicalXUUID(ArrayList<String>idList, String aType){
        String seed = "";
        for (String s:idList){
            seed += s;
        }
        UUID u = java.util.UUID.nameUUIDFromBytes(seed.getBytes());
        XUUID rm = XUUID.create(aType, u, false,false);
        return rm;
    }


    /**
     * Create a resource that has been typed and labeled
     *
     * @param instanceURI   the uri of the resource to return
     * @param classURI      the uri of the type of the resource to return
     * @param classLabel    the label to be assigned to the type resource
     * @param instanceLabel the label given the the instance
     * @param anXUUID       the XUUID given to the entity to be transformed into a resource
     * @return a resource with type and label
     */
    public Resource createResourceWithTypeAndLabel(URI instanceURI, URI classURI, String classLabel, String instanceLabel, String anXUUID) {

        if (instanceURI.toString().length() > 10) {
            Resource r = this.model.createResource(instanceURI.toString());
            //type it
            if (classURI != null && classURI.toString().length() > 3) {
                Resource tr = this.model.createResource(classURI.toString());
                //add an OWL class to every type
                tr.addProperty(RDF.type, OWL.Class);
                if (classLabel != null && classLabel.length() > 2) {
                    tr.addProperty(RDFS.label, this.model.createLiteral(classLabel));
                }
                r.addProperty(RDF.type, tr);
            } else {
                logger.error("Found invalid type URI! " + classURI);
            }
            if (instanceLabel != null && instanceLabel.length() > 3) {
                //label it
                r.addLiteral(RDFS.label, this.model.createLiteral(instanceLabel));
            } else {
                logger.error("Found invalid label : " + instanceLabel);
            }
            if (anXUUID != null) {
                r.addLiteral(OWLVocabulary.OWLDataProperty.HasXUUID.property(), anXUUID);
            } else {
                logger.debug("null xuuid passed in to resource creator");
            }
            return r;
        } else {
            throw new NewAPOToJenaModelException("Invalid URI found! : " + instanceURI);
        }
    }

    public Resource convertExternalIdentifier(ExternalIdentifier ei) {
        if (ei.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.ExternalIdentifier.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ExternalIdentifier.name()+"/"+ ei.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + ei.getInternalXUUID();

                Resource r = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, ei.getInternalXUUID().toString());

                if (ei.getIDValue() != null) {
                    model.add(r, OWLVocabulary.OWLDataProperty.HasIDValue.property(), ei.getIDValue());
                }
                if (ei.getIDType() != null) {
                    model.add(r, OWLVocabulary.OWLDataProperty.HasIDType.property(), ei.getIDType());
                }
                if (ei.getAssignAuthority() != null) {
                    model.add(r, OWLVocabulary.OWLDataProperty.HasAssignAuthority.property(), ei.getAssignAuthority());
                }
                //data processing details
                if (ei.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : ei.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        r.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (ei.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(ei.getOriginalID());
                    r.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (ei.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier eid : ei.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(eid);
                        r.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                //last edit date
                if (ei.getLastEditDate() != null) {
                    Resource dr = convertDate(ei.getLastEditDate());
                    r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                return r;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + ei.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertHICN(HICN a){
        if(a.getInternalXUUID() != null){
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.HICN.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.HICN.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource r = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getIDValue() != null) {
                    model.add(r, OWLVocabulary.OWLDataProperty.HasIDValue.property(), a.getIDValue());
                }
                if (a.getIDType() != null) {
                    model.add(r, OWLVocabulary.OWLDataProperty.HasIDType.property(), a.getIDType());
                }
                if (a.getAssignAuthority() != null) {
                    model.add(r, OWLVocabulary.OWLDataProperty.HasAssignAuthority.property(), a.getAssignAuthority());
                }
                //data processing details
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        r.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getMedicarePlanURI() != null){
                    r.addLiteral(OWLVocabulary.OWLDataProperty.HasMedicarePlanURI.property(), a.getMedicarePlanURI());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    r.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier eid : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(eid);
                        r.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                //last edit date
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                return r;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertTextRendering(TextRendering a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.TextRendering.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.TextRendering.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());


                if (a.getTextRenderingType() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasTextRenderingType.property(), a.getTextRenderingType());
                }
                if (a.getTextRenderingValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasTextRenderingValue.property(), a.getTextRenderingValue());
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertImageRendering(ImageRendering a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.ImageRendering.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ImageRendering.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getImageResolution() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasImageResolution.property(), a.getImageResolution());
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertOrganization(Organization a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Organization.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Organization.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getNationalProviderIdentifier() != null) {
                    Resource r = convertExternalIdentifier(a.getNationalProviderIdentifier());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasNationalProviderIdentifier.property(), r);
                }
                if (a.getOrganizationNameValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOrganizationNameValue.property(), a.getOrganizationNameValue());
                }
                if (a.getContactDetails() != null) {
                    Resource r = convertContactDetails(a.getContactDetails());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasContactDetails.property(), r);
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }

                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertPrimaryCareProvider(PrimaryCareProvider a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.PrimaryCareProvider.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.PrimaryCareProvider.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOrganization() != null) {
                    Resource r = convertOrganization(a.getOrganization());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOrganization.property(), r);
                }

                if (a.getClinicalRole() != null) {
                    //create a resource for the type
                    if (a.getClinicalRole().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.ClinicalRoleType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ClinicalRoleType.name()+"/"+ a.getClinicalRole().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getClinicalRole().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getClinicalRole().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getClinicalRole().getClinicalRoleTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getClinicalRole().getClinicalRoleTypeValue().name());
                        }
                        if (a.getClinicalRole().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getClinicalRole().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getClinicalRole().getOtherIds() != null) {
                            for (XUUID id : a.getClinicalRole().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClinicalRole.property(), r);
                    }
                }
                if (a.getDemographics() != null) {
                    Resource r = convertDemographics(a.getDemographics());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDemographics.property(), r);
                }
                if (a.getTitle() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasTitle.property(), a.getTitle());
                }
                if (a.getContactDetails() != null) {
                    Resource r = convertContactDetails(a.getContactDetails());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasContactDetails.property(), r);
                }
                if (a.getAlternateContactDetailss() != null) {
                    for (ContactDetails c : a.getAlternateContactDetailss()) {
                        Resource r = convertContactDetails(c);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAlternateContactDetails.property(), r);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getDateRangeForCareOfPatient() != null) {
                    Resource dr = convertDateRange(a.getDateRangeForCareOfPatient());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateRange.property(), dr);
                }

                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertMedicalProfessional(MedicalProfessional a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.MedicalProfessional.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.MedicalProfessional.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOrganization() != null) {
                    Resource r = convertOrganization(a.getOrganization());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOrganization.property(), r);
                }

                if (a.getClinicalRole() != null) {
                    //create a resource for the type
                    if (a.getClinicalRole().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.ClinicalRoleType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ClinicalRoleType.name()+"/"+ a.getClinicalRole().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getClinicalRole().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getClinicalRole().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getClinicalRole().getClinicalRoleTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getClinicalRole().getClinicalRoleTypeValue().name());
                        }
                        if (a.getClinicalRole().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getClinicalRole().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getClinicalRole().getOtherIds() != null) {
                            for (XUUID id : a.getClinicalRole().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClinicalRole.property(), r);
                    }
                }
                if (a.getDemographics() != null) {
                    Resource r = convertDemographics(a.getDemographics());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDemographics.property(), r);
                }
                if (a.getTitle() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasTitle.property(), a.getTitle());
                }
                if (a.getContactDetails() != null) {
                    Resource r = convertContactDetails(a.getContactDetails());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasContactDetails.property(), r);
                }
                if (a.getAlternateContactDetailss() != null) {
                    for (ContactDetails c : a.getAlternateContactDetailss()) {
                        Resource r = convertContactDetails(c);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAlternateContactDetails.property(), r);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }

                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertDocument(Document a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Document.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Document.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getDocumentAuthors() != null) {
                    for (MedicalProfessional m : a.getDocumentAuthors()) {
                        Resource r = convertMedicalProfessional(m);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDocumentAuthor.property(), r);
                    }
                }
                if (a.getDocumentCareSite() != null) {
                    Resource r = convertCareSite(a.getDocumentCareSite());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDocumentCareSite.property(), r);
                }
                if (a.getDocumentDate() != null) {
                    Resource r = convertApixioDate(a.getDocumentDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDocumentDate.property(), r);
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getTitle() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasTitle.property(), a.getTitle());
                }
                if (a.getDocumentPages() != null) {
                    for (Page p : a.getDocumentPages()) {
                        Resource r = convertPage(p);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPage.property(), r);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertPage(Page a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Page.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Page.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getPageNumber() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPageNumber.property(), a.getPageNumber());
                }
                if (a.getTextRenderings() != null) {
                    for (TextRendering tr : a.getTextRenderings()) {
                        Resource r = convertTextRendering(tr);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasTextRendering.property(), r);
                    }
                }
                if (a.getImageRenderings() != null) {
                    for (ImageRendering ir : a.getImageRenderings()) {
                        Resource r = convertImageRendering(ir);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasImageRendering.property(), r);
                    }
                }
                if (a.getPageClassifications() != null) {
                    for (String s : a.getPageClassifications()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPageClassification.property(), s);
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertSocialHistory(SocialHistory a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.SocialHistory.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.SocialHistory.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getSocialHistoryDate() != null) {
                    Resource d = convertDate(a.getSocialHistoryDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSocialHistoryDate.property(), d);
                }

                if (a.getSocialHistoryType() != null) {
                    Resource c = convertClinicalCode(a.getSocialHistoryType());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSocialHistoryType.property(), c);
                }
                if (a.getSocialHistoryFieldName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSocialHistoryFieldName.property(), a.getSocialHistoryFieldName());
                }
                if (a.getSocialHistoryValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSocialHistoryValue.property(), a.getSocialHistoryValue());
                }

                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }


                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional());
                }

                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cr = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertFamilyHistory(FamilyHistory a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.FamilyHistory.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.FamilyHistory.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getFamilyHistoryValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasFamilyHistoryValue.property(), a.getFamilyHistoryValue());
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }

                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }

                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cr = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertPatientEncounter(PatientEncounter a) {
        if (a.getInternalXUUID() != null){
            try{
                URI classUri = new URI(OWLVocabulary.OWLClass.PatientEncounter.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.PatientEncounter.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if(a.getEncounterDateRange() != null){
                    Resource dr = convertDateRange(a.getEncounterDateRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasEncounterDateRange.property(), dr);
                }
                if(a.getChiefComplaints()!= null){
                    for(ClinicalCode cc : a.getChiefComplaints()){
                        Resource c = convertClinicalCode(cc);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasChiefComplaint.property(), c);
                    }
                }
                //add a value to the typed resource
                if (a.getPatientEncounterType() !=null){
                    if(a.getPatientEncounterType().getPatientEncounterTypeValue() != null){
                        if(a.getPatientEncounterType().getPatientEncounterTypeValue().name() != null) {
                            rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPatientEncounterQualityValue.property(), a.getPatientEncounterType().getPatientEncounterTypeValue().name());
                        }
                    }
                }
                if(a.getMedicalProfessionals() != null){
                    for (MedicalProfessional mp : a.getMedicalProfessionals()){
                        Resource rmp = convertMedicalProfessional(mp);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasMedicalProfessional.property(), rmp);
                    }
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getPatientEncounterCareSite() != null){
                    Resource r = convertCareSite(a.getPatientEncounterCareSite());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPatientEncounterCareSite.property(), r);
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cr = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID for patient : "+this.newPatient.getInternalXUUID().toString());
        }
    }

    public Resource convertPrescription(Prescription a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Prescription.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Prescription.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getPrescribedDrugs() != null) {
                    for (Medication m : a.getPrescribedDrugs()) {
                        Resource mr = convertMedication(m);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrescribedDrug.property(), mr);
                    }
                }
                if (a.getPrescriptionFillDate() != null) {
                    Resource fr = convertDate(a.getPrescriptionFillDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrescriptionFillDate.property(), fr);
                }
                if (a.getPrescriptionEndDate() != null) {
                    Resource er = convertDate(a.getPrescriptionEndDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrescriptionEndDate.property(), er);
                }
                if (a.getPrescriptionDate() != null) {
                    Resource dr = convertDate(a.getPrescriptionDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrescriptionDate.property(), dr);
                }
                if (a.getNumberOfRefillsRemaining() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasNumberOfRefillsRemaining.property(), a.getNumberOfRefillsRemaining());
                }
                if (a.getIsActivePrescription() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.IsActivePrescription.property(), a.getIsActivePrescription());
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getSig() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSig.property(), a.getSig());
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getPrescriptionQuantity() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrescriptionQuantity.property(), a.getPrescriptionQuantity());
                }
                if (a.getPrescriptionFrequency() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrescriptionFrequency.property(), a.getPrescriptionFrequency());
                }

                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cr = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertProcedure(Procedure a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Procedure.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Procedure.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getBodySite() != null) {
                    Resource r = convertAnatomicalEntity(a.getBodySite());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasBodySite.property(), r);
                }
                if (a.getDiagnosisCodes() != null) {
                    for (ClinicalCode cc : a.getDiagnosisCodes()) {
                        Resource r = convertClinicalCode(cc);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDiagnosisCode.property(), r);
                    }
                }
                if (a.getMedicalProcedureNameValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMedicalProcedureNameValue.property(), a.getMedicalProcedureNameValue());
                }
                if (a.getMedicalProcedureDateRange() != null) {
                    Resource r = convertDateRange(a.getMedicalProcedureDateRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasMedicalProcedureDateRange.property(), r);
                }
                if (a.getMedicalProcedureInterpretation() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMedicalProcedureInterpretation.property(), a.getMedicalProcedureInterpretation());
                }

                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cr = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cr);
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertProblem(Problem a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Problem.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Problem.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getResolutionStatusType() != null) {
                    //create a resource for the type
                    if (a.getResolutionStatusType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.ResolutionStatusType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ResolutionStatusType.name()+"/"+ a.getResolutionStatusType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getResolutionStatusType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getResolutionStatusType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getResolutionStatusType().getResolutionStatusTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getResolutionStatusType().getResolutionStatusTypeValue().name());
                        }
                        if (a.getResolutionStatusType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getResolutionStatusType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getResolutionStatusType().getOtherIds() != null) {
                            for (XUUID id : a.getResolutionStatusType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasResolutionStatusType.property(), r);
                    }
                }
                if (a.getProblemDateRange() != null) {
                    Resource dr = convertDateRange(a.getProblemDateRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProblemDateRange.property(), dr);
                }
                if (a.getProblemTemporalStatus() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasProblemTemporalStatus.property(), a.getProblemTemporalStatus());
                }
                if (a.getProblemName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasProblemName.property(), a.getProblemName());
                }
                if (a.getProblemSeverity() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasProblemSeverity.property(), a.getProblemSeverity());
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional());
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cr = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cr);
                }
                if (a.getProblemResolutionDate() != null) {
                    Resource d = convertDate(a.getProblemResolutionDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProblemResolutionDate.property(), d);
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }

                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertHuman(Human h) {
        if (h.getInternalXUUID() != null) {

            if (h instanceof MedicalProfessional) {
                return convertMedicalProfessional((MedicalProfessional) h);

            } else if (h instanceof PrimaryCareProvider) {
                return convertPrimaryCareProvider((PrimaryCareProvider) h);
            } else if (h instanceof Patient) {
                return convertPatient((Patient) h);
            } else {
                throw new NewAPOToJenaModelException(" could not resolve human's subClass");
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + h.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertMedication(Medication a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Medication.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Medication.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getBrandName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasBrandName.property(), a.getBrandName());
                }
                if (a.getGenericName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasGenericName.property(), a.getGenericName());
                }
                if (a.getDrugStrength() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDrugStrength.property(), a.getDrugStrength());
                }
                if (a.getDrugForm() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDrugForm.property(), a.getDrugForm());
                }
                if (a.getDrugUnits() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDrugUnits.property(), a.getDrugUnits());
                }
                if (a.getRouteOfAdministration() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasRouteOfAdministration.property(), a.getRouteOfAdministration());
                }
                if (a.getDrugIngredients() != null) {
                    for (String i : a.getDrugIngredients()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDrugIngredient.property(), i);
                    }
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId());
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource er = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), er);
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID i : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), i.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cr = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cr);
                }
                if (a.getMedicationAmount() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMedicationAmount.property(), a.getMedicationAmount());
                }
                if (a.getMedicationDosage() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMedicationDosage.property(), a.getMedicationDosage());
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }




    public Resource convertClinicalCode(ClinicalCode cc) {
        if (cc.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.ClinicalCode.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ClinicalCode.name()+"/"+ cc.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + cc.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, cc.getInternalXUUID().toString());
                if (cc.getClinicalCodingSystem() != null) {
                    Resource ccs = convertClinicalCodingSystem(cc.getClinicalCodingSystem());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClinicalCodingSystem.property(), ccs);
                }
                if (cc.getClinicalCodeDisplayName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasClinicalCodeDisplayName.property(), cc.getClinicalCodeDisplayName());
                }
                if (cc.getClinicalCodeValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasClinicalCodeValue.property(), cc.getClinicalCodeValue());
                }
                if (cc.getOriginalID() != null) {
                    Resource eir = convertExternalIdentifier(cc.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), eir);
                }
                if (cc.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : cc.getOtherOriginalIDss()) {
                        Resource eir2 = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir2);
                    }
                }
                if (cc.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : cc.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (cc.getLastEditDate() != null) {
                    Resource dr = convertDate(cc.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + cc.getClass().getName() + " without an XUUID");
        }

    }

    public Resource convertClinicalCodingSystem(ClinicalCodingSystem ccs) {
        if (ccs.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.ClinicalCodingSystem.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ClinicalCodingSystem.name()+"/"+ ccs.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + ccs.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, ccs.getInternalXUUID().toString());
                if (ccs.getClinicalCodingSystemName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasClinicalCodingSystemName.property(), ccs.getClinicalCodingSystemName());
                }
                if (ccs.getClinicalCodingSystemOID() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasClinicalCodingSystemOID.property(), ccs.getClinicalCodingSystemOID());
                }
                if (ccs.getClinicalCodingSystemVersion() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasClinicalCodingSystemVersion.property(), ccs.getClinicalCodingSystemVersion());
                }
                if (ccs.getOriginalID() != null) {
                    Resource orid = convertExternalIdentifier(ccs.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), orid);
                }
                if (ccs.getOtherIds() != null) {
                    for (XUUID id : ccs.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (ccs.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : ccs.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (ccs.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : ccs.getOtherOriginalIDss()) {
                        Resource rei = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), rei);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + ccs.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertDemographics(Demographics d) {
        if (d.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Demographics.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Demographics.name()+"/"+ d.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + d.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, d.getInternalXUUID().toString());

                if (d.getName() != null) {
                    Resource nr = convertName(d.getName());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasName.property(), nr);
                }
                if (d.getAlternateNames() != null) {
                    for (Name n : d.getAlternateNames()) {
                        Resource nr2 = convertName(n);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAlternateName.property(), nr2);
                    }
                }
                if (d.getDateOfBirth() != null) {
                    Resource dobr = convertDate(d.getDateOfBirth());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateOfBirth.property(), dobr);
                }
                if (d.getDateOfDeath() != null) {
                    Resource dodr = convertDate(d.getDateOfDeath());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateOfDeath.property(), dodr);
                }
                if (d.getRace() != null) {
                    Resource cr = convertClinicalCode(d.getRace());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasRace.property(), cr);
                }
                if (d.getEthnicity() != null) {
                    Resource er = convertClinicalCode(d.getEthnicity());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasEthnicity.property(), er);
                }
                if (d.getLanguages() != null) {
                    for (String l : d.getLanguages()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasLanguage.property(), l);
                    }
                }
                if (d.getReligiousAffiliation() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasReligiousAffiliation.property(), d.getReligiousAffiliation());
                }
                if (d.getOtherIds() != null) {
                    for (XUUID id : d.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (d.getOriginalID() != null) {
                    Resource or = convertExternalIdentifier(d.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), or);
                }
                if (d.getLastEditDate() != null) {
                    Resource led = convertDate(d.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), led);
                }
                if (d.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : d.getOtherOriginalIDss()) {
                        Resource or2 = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), or2);
                    }
                }
                if (d.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : d.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + d.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertName(Name n) {
        if (n.getInternalXUUID() != null) {
            try {
                OWLVocabulary.OWLClass.getByClass(n.getClass());
                URI classUri = new URI(OWLVocabulary.OWLClass.Name.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE+ OWLVocabulary.OWLClass.Name.name()+"/" + n.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + n.getInternalXUUID();

                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, n.getInternalXUUID().toString());
                if (n.getFamilyNames() != null) {
                    for (String fn : n.getFamilyNames()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasFamilyName.property(), fn);
                    }
                }
                if (n.getSuffixNames() != null) {
                    for (String sn : n.getSuffixNames()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSuffixName.property(), sn);
                    }
                }
                if (n.getPrefixNames() != null) {
                    for (String pn : n.getPrefixNames()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrefixName.property(), pn);
                    }
                }
                if (n.getGivenNames() != null) {
                    for (String gn : n.getGivenNames()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasGivenName.property(), gn);
                    }
                }
                if (n.getLastEditDate() != null) {
                    Resource dr = convertDate(n.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (n.getOtherIds() != null) {
                    for (XUUID id : n.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (n.getNameType() != null) {
                    //create a resource for the type
                    if (n.getNameType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.NameType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.NameType.name()+"/"+ n.getNameType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + n.getNameType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, n.getNameType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (n.getNameType().getNameTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasNameTypeValue.property(), n.getNameType().getNameTypeValue().name());
                        }
                        if (n.getNameType().getLastEditDate() != null) {
                            Resource dateR = convertDate(n.getNameType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasDate.property(), dateR);
                        }
                        if (n.getNameType().getOtherIds() != null) {
                            for (XUUID id : n.getNameType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasNameType.property(), r);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + n.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertBiometricValue(BiometricValue a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.BiometricValue.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.BiometricValue.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getMeasurmentResultDate() != null) {
                    Resource dr = convertDate(a.getMeasurmentResultDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasMeasurmentResultDate.property(), dr);
                }
                if (a.getMeasurmentUnit() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasMeasurmentUnit.property(), a.getMeasurmentUnit());
                }
                if (a.getBiometricValueString() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasBiometricValueString.property(), a.getBiometricValueString());
                }
                if (a.getBiometricValueName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasBiometricValueName.property(), a.getBiometricValueName());
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }

                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cc = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cc);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertAnatomicalEntity(AnatomicalEntity a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.AnatomicalEntity.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.AnatomicalEntity.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getAnatomicalEntityName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAnatomicalEntityName.property(), a.getAnatomicalEntityName());
                }

                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }

                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cc = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cc);
                }

                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertImmunization(Immunization a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Immunization.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Immunization.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getImmunizationAdminDate() != null) {
                    Resource cc = convertDate(a.getImmunizationAdminDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasImmunizationAdminDate.property(), cc);
                }
                if (a.getImmunizationDateRange() != null) {
                    Resource d = convertDateRange(a.getImmunizationDateRange());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateRange.property(), d);
                }
                if (a.getImmunizationQuantity() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasImmunizationQuantity.property(), a.getImmunizationQuantity());
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getImmunizationMedicationSeriesNumber() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasImmunizationMedicationSeriesNumber.property(), a.getImmunizationMedicationSeriesNumber());
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getImmunizationMedications() != null) {
                    for (Medication m : a.getImmunizationMedications()) {
                        Resource mr = convertMedication(m);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasImmunizationMedication.property(), mr);
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cc = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cc);
                }

                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertLabResult(LabResult a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.LabResult.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.LabResult.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getLabFlagType() != null) {
                    //create a resource for the type
                    if (a.getLabFlagType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.LabFlagType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.LabFlagType.name()+"/"+ a.getLabFlagType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getLabFlagType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getLabFlagType().toString());
                        //add a value to the typed resource
                        if (a.getLabFlagType().getLabFlagValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getLabFlagType().getLabFlagValue().name());
                        }
                        if (a.getLabFlagType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getLabFlagType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getLabFlagType().getOtherIds() != null) {
                            for (XUUID id : a.getLabFlagType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLabFlagType.property(), r);
                    }
                }
                if (a.getSampleDate() != null) {
                    Resource d = convertDate(a.getSampleDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSampleDate.property(), d);
                }
                if (a.getLabResultCareSite() != null) {
                    Resource d = convertCareSite(a.getLabResultCareSite());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCareSite.property(), d);
                }
                if (a.getSpecimen() != null) {
                    Resource d = convertClinicalCode(a.getSpecimen());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSpecimen.property(), d);
                }
                if (a.getLabPanel() != null) {
                    Resource d = convertClinicalCode(a.getLabPanel());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLabPanel.property(), d);
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getRange() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasRange.property(), a.getRange());
                }
                if (a.getLabResultUnit() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultUnit.property(), a.getLabResultUnit());
                }
                if (a.getLabResultNameValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultNameValue.property(), a.getLabResultNameValue());
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getLabNote() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasLabNote.property(), a.getLabNote());
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getLabResultSequenceNumber() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultSequenceNumber.property(), a.getLabResultSequenceNumber());
                }
                if (a.getLabResultNumberOfSamples() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultNumberOfSamples.property(), a.getLabResultNumberOfSamples());
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cc = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cc);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    private Resource convertCMSClaimValidationResult(CMSClaimValidationResult a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.CMSClaimValidationResult.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.CMSClaimValidationResult.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getCMSValidationErrors() != null) {
                    for (CMSValidationError cve : a.getCMSValidationErrors()) {
                        Resource r = convertCMSValidationError(cve);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCMSValidationError.property(), r);
                    }
                }
                if (a.getClaimOverpaymentIdentifier() != null) {
                    Resource r = convertExternalIdentifier(a.getClaimOverpaymentIdentifier());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasClaimOverpaymentIdentifier.property(), r);
                }
                if (a.getValidatedClaimPaymentYear() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasValidatedClaimPaymentYear.property(), a.getValidatedClaimPaymentYear());
                }
                if (a.getValidatedClaimControlNumber() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasValidatedClaimControlNumber.property(), a.getValidatedClaimControlNumber());
                }
                if (a.getValidatedClaimPatientDateOfBirth() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasValidatedClaimPatientDateOfBirth.property(), a.getValidatedClaimPatientDateOfBirth());
                }
                if (a.getValidatedClaimProviderType() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasValidatedClaimProviderType.property(), a.getValidatedClaimProviderType());
                }
                if (a.getLastEditDate() != null) {
                    Resource r = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), r);
                }
                if (a.getRiskAssessmentCodeClusters() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasRiskAssessmentCodeClusters.property(), a.getRiskAssessmentCodeClusters());
                }
                if (a.getValidatedClaimTransactionDate() != null) {
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasValidatedClaimTransactionDate.property(), convertDate(a.getValidatedClaimTransactionDate()));
                }
                if (a.getValidatedClaimPlanNumber() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasValidatedClaimPlanNumber.property(), a.getValidatedClaimPlanNumber());
                }
                if (a.getValidatedClaimFileMode() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasValidatedClaimFileMode.property(), a.getValidatedClaimFileMode());
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    private Resource convertCMSValidationError(CMSValidationError a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.CMSValidationError.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.CMSValidationError.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getCMSValidationErrorCodeValue() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasCMSValidationErrorCodeValue.property(), a.getCMSValidationErrorCodeValue());
                }

                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertAllergy(Allergy a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Allergy.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Allergy.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getAllergenCode() != null) {
                    Resource cc = convertClinicalCode(a.getAllergenCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAllergenCode.property(), cc);
                }
                if (a.getAllergyReactionDate() != null) {
                    Resource d = convertDate(a.getAllergyReactionDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAllergyReactionDate.property(), d);
                }
                if (a.getAllergyResolvedDate() != null) {
                    Resource d = convertDate(a.getAllergyResolvedDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAllergyResolvedDate.property(), d);
                }
                if (a.getAllergyDiagnosisDate() != null) {
                    Resource d = convertDate(a.getAllergyDiagnosisDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAllergyDiagnosisDate.property(), d);
                }
                if (a.getAllergyReactionSeverity() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAllergyReactionSeverity.property(), a.getAllergyReactionSeverity());
                }
                if (a.getSourceEncounterId() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasSourceEncounterId.property(), a.getSourceEncounterId().toString());
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getPrimaryMedicalProfessional() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryMedicalProfessional.property(), a.getPrimaryMedicalProfessional().toString());
                }
                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getAlternateMedicalProfessionalIds() != null) {
                    for (XUUID i : a.getAlternateMedicalProfessionalIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateMedicalProfessional.property(), i.toString());
                    }
                }
                if (a.getCodedEntityClinicalCode() != null) {
                    Resource cc = convertClinicalCode(a.getCodedEntityClinicalCode());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCodedEntityClinicalCode.property(), cc);
                }
                if (a.getAllergyAllergenString() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAllergyAllergenString.property(), a.getAllergyAllergenString());
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }


    public Resource convertCareSite(CareSite a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.CareSite.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.CareSite.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getPrimaryAddress() != null) {
                    Resource ar = convertAddress(a.getPrimaryAddress());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrimaryAddress.property(), ar);
                }

                if (a.getCareSiteType() != null) {
                    //create a resource for the type
                    if (a.getCareSiteType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.CareSiteType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.CareSiteType.name()+"/"+ a.getCareSiteType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getCareSiteType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getCareSiteType().toString());
                        //add a value to the typed resource
                        if (a.getCareSiteType().getCareSiteTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasLabResultValue.property(), a.getCareSiteType().getCareSiteTypeValue().name());
                        }
                        if (a.getCareSiteType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getCareSiteType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getCareSiteType().getOtherIds() != null) {
                            for (XUUID id : a.getCareSiteType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasCareSiteType.property(), r);
                    }
                }
                if (a.getCareSiteName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasCareSiteName.property(), a.getCareSiteName());
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getNationalProviderIdentifier() != null) {
                    Resource r = convertExternalIdentifier(a.getNationalProviderIdentifier());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasNationalProviderIdentifier.property(), r);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertContactDetails(ContactDetails cd) {
        if (cd.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.ContactDetails.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ContactDetails.name()+"/"+ cd.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + cd.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, cd.getInternalXUUID().toString());

                if (cd.getPrimaryAddress() != null) {
                    Resource primaryAddress = convertAddress(cd.getPrimaryAddress());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasPrimaryAddress.property(), primaryAddress);
                }
                if (cd.getAlternateAddresses() != null) {
                    for (Address a : cd.getAlternateAddresses()) {
                        Resource anAddr = convertAddress(a);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAlternateAddress.property(), anAddr);
                    }
                }
                if (cd.getTelephoneNumber() != null) {
                    Resource tn = convertTelephoneNumber(cd.getTelephoneNumber());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasTelephoneNumber.property(), tn);
                }
                if (cd.getAlternateTelephoneNumbers() != null) {
                    for (TelephoneNumber tn : cd.getAlternateTelephoneNumbers()) {
                        Resource tn2 = convertTelephoneNumber(cd.getTelephoneNumber());
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAlternateTelephoneNumber.property(), tn2);
                    }
                }
                if (cd.getPrimaryEmailAddress() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPrimaryEmailAddress.property(), cd.getPrimaryEmailAddress());
                }
                if (cd.getAlternateEmailAddresses() != null) {
                    for (String anEmail : cd.getAlternateEmailAddresses()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasAlternateEmailAddress.property(), anEmail);
                    }
                }
                if (cd.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : cd.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (cd.getOriginalID() != null) {
                    Resource ei = convertExternalIdentifier(cd.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), ei);
                }
                if (cd.getOtherIds() != null) {
                    for (XUUID id : cd.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (cd.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : cd.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (cd.getLastEditDate() != null) {
                    Resource d = convertDate(cd.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDate.property(), d);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + cd.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertAddress(Address ad) {
        if (ad.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Address.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Address.name()+"/"+ ad.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + ad.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, ad.getInternalXUUID().toString());
                if (ad.getCounty() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasCounty.property(), ad.getCounty());
                }
                if (ad.getCountry() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasCountry.property(), ad.getCountry());
                }
                if (ad.getZipCode() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasZipCode.property(), ad.getZipCode());
                }
                if (ad.getState() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasState.property(), ad.getState());
                }
                if (ad.getCity() != null) {
                    rm.addProperty(OWLVocabulary.OWLDataProperty.HasCity.property(), ad.getCity());
                }
                if (ad.getAddressType() != null) {
                    //create a resource for the type
                    if (ad.getAddressType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.AddressType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.AddressType.name()+"/"+ ad.getAddressType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + ad.getAddressType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, ad.getAddressType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (ad.getAddressType().getAddressTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasAddressTypeQualityValue.property(), ad.getAddressType().getAddressTypeValue().name());
                        }
                        if (ad.getAddressType().getLastEditDate() != null) {
                            Resource dateR = convertDate(ad.getAddressType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (ad.getAddressType().getOtherIds() != null) {
                            for (XUUID id : ad.getAddressType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasAddressType.property(), r);
                    }
                }
                if (ad.getOtherIds() != null) {
                    for (XUUID id : ad.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (ad.getStreetAddresses() != null) {
                    for (String sa : ad.getStreetAddresses()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasStreetAddress.property(), sa);
                    }
                }
                if (ad.getLastEditDate() != null) {
                    Resource d = convertDate(ad.getLastEditDate());
                    if (d != null) {
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDate.property(), d);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + ad.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertTelephoneNumber(TelephoneNumber a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.TelephoneNumber.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.TelephoneNumber.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getTelephoneNumberType() != null) {
                    //create a resource for the type
                    if (a.getTelephoneNumberType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.TelephoneType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.TelephoneType.name()+"/"+ a.getTelephoneNumberType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getTelephoneNumberType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getTelephoneNumberType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getTelephoneNumberType().getTelephoneTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasTelephoneNumberValue.property(), a.getTelephoneNumberType().getTelephoneTypeValue().name());
                        }
                        if (a.getTelephoneNumberType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getTelephoneNumberType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getTelephoneNumberType().getOtherIds() != null) {
                            for (XUUID id : a.getTelephoneNumberType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasTelephoneNumberType.property(), r);
                    }
                }
                if (a.getPhoneNumber() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasPhoneNumber.property(), a.getPhoneNumber());
                }
                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertDataProcessingDetails(DataProcessingDetail a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.DataProcessingDetail.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.DataProcessingDetail.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

//                if (a.getParserType() != null) {
//                    //create a resource for the type
//                    if (a.getParserType().getInternalXUUID() != null) {
//                        URI classUri2 = new URI(OWLVocabulary.OWLClass.ParserType.uri());
//                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + a.getParserType().getInternalXUUID());
//                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
//                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getParserType().getInternalXUUID();
//                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getParserType().getInternalXUUID().toString());
//                        //add a value to the typed resource
//                        if (a.getParserType().getParserTypeValue().name() != null) {
//                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasTelephoneNumberValue.property(), a.getParserType().getParserTypeValue().name());
//                        }
//                        if (a.getParserType().getLastEditDate() != null) {
//                            Resource dateR = convertDate(a.getParserType().getLastEditDate());
//                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
//                        }
//                        if (a.getParserType().getOtherIds() != null) {
//                            for (XUUID id : a.getParserType().getOtherIds()) {
//                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
//                            }
//                        }
//                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasParserType.property(), r);
//                    }
//                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getVersion() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasVersion.property(), a.getVersion());
                }
                if (a.getProcessor() != null) {
                    Resource r = convertProcessor(a.getProcessor());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProcessor.property(), r);
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getDataProcessingDetailDate() != null) {
                    Resource r = convertDate(a.getDataProcessingDetailDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetailDate.property(), r);
                }
                if (a.getSources() != null) {
                    for (Source s : a.getSources()) {
                        Resource r = convertSource(s);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasSource.property(), r);
                    }
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertProcessor(Processor a) {
        if (a.getInternalXUUID() != null) {
            try {

                URI classUri = new URI(OWLVocabulary.OWLClass.Processor.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Processor.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getProcessorType() != null) {
                    //create a resource for the type
                    if (a.getProcessorType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.ProcessorType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.ProcessorType.name()+"/"+ a.getProcessorType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getProcessorType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getProcessorType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getProcessorType().getProcessorTypeValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasProcessorTypeValue.property(), a.getProcessorType().getProcessorTypeValue().name());
                        }

                        if (a.getProcessorType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getProcessorType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getProcessorType().getOtherIds() != null) {
                            for (XUUID id : a.getProcessorType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasProcessorType.property(), r);
                    }
                }
//                if(a.getInputDataType().getInputDataTypeValue().name()!=null){
//                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasInputDataTypeValue.property(), a.getInputDataType().getInputDataTypeValue().name());
//                }

                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getProcessorVersion() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasProcessorVersion.property(), a.getProcessorVersion());
                }

                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                if (a.getProcessorName() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasProcessorName.property(), a.getProcessorName());
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertDate(Date a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.Date.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.Date.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());

                if (a.getDateType() != null) {
                    //create a resource for the type
                    if (a.getDateType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.DateType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.DateType.name()+"/"+ a.getDateType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getDateType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getDateType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getDateType().getDateQualityValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasTelephoneNumberValue.property(), a.getDateType().getDateQualityValue().name());
                        }
                        if (a.getDateType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getDateType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getDateType().getOtherIds() != null) {
                            for (XUUID id : a.getDateType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateType.property(), r);
                    }
                }
                if (a.getDateValue() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasDateValue.property(), this.model.createTypedLiteral(a.getDateValue().toString()));
                }
                if (a.getTimeZone() != null) {
                    rm.addLiteral(OWLVocabulary.OWLDataProperty.HasTimeZone.property(), a.getTimeZone());
                }

                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }
                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public Resource convertDateRange(DateRange a) {
        if (a.getInternalXUUID() != null) {
            try {
                URI classUri = new URI(OWLVocabulary.OWLClass.DateRange.uri());
                URI instanceUri = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.DateRange.name()+"/"+ a.getInternalXUUID().toString());
                String classLabel = OWLVocabulary.getLabelFromURI(classUri.toString());
                String instanceLabel = classLabel + " with XUUID : " + a.getInternalXUUID();
                Resource rm = createResourceWithTypeAndLabel(instanceUri, classUri, classLabel, instanceLabel, a.getInternalXUUID().toString());
                if (a.getStartDate() != null) {
                    Resource r = convertDate(a.getStartDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasStartDate.property(), r);
                }
                if (a.getEndDate() != null) {
                    Resource r = convertDate(a.getEndDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasEndDate.property(), r);
                }
                if (a.getDateType() != null) {
                    //create a resource for the type
                    if (a.getDateType().getInternalXUUID() != null) {
                        URI classUri2 = new URI(OWLVocabulary.OWLClass.DateType.uri());
                        URI instanceUri2 = new URI(OWLVocabulary.BASE_NAMESPACE + OWLVocabulary.OWLClass.DateType.name()+"/"+ a.getDateType().getInternalXUUID());
                        String classLabel2 = OWLVocabulary.getLabelFromURI(classUri2.toString());
                        String instanceLabel2 = classLabel2 + " with XUUID : " + a.getDateType().getInternalXUUID();
                        Resource r = createResourceWithTypeAndLabel(instanceUri2, classUri2, classLabel2, instanceLabel2, a.getDateType().getInternalXUUID().toString());
                        //add a value to the typed resource
                        if (a.getDateType().getDateQualityValue().name() != null) {
                            r.addLiteral(OWLVocabulary.OWLDataProperty.HasTelephoneNumberValue.property(), a.getDateType().getDateQualityValue().name());
                        }
                        if (a.getDateType().getLastEditDate() != null) {
                            Resource dateR = convertDate(a.getDateType().getLastEditDate());
                            r.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dateR);
                        }
                        if (a.getDateType().getOtherIds() != null) {
                            for (XUUID id : a.getDateType().getOtherIds()) {
                                r.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                            }
                        }
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDateType.property(), r);
                    }
                }
                if (a.getOriginalID() != null) {
                    Resource idr = convertExternalIdentifier(a.getOriginalID());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOriginalID.property(), idr);
                }
                if (a.getOtherIds() != null) {
                    for (XUUID id : a.getOtherIds()) {
                        rm.addLiteral(OWLVocabulary.OWLDataProperty.HasOtherId.property(), id.toString());
                    }
                }
                if (a.getDataProcessingDetails() != null) {
                    for (DataProcessingDetail dpd : a.getDataProcessingDetails()) {
                        Resource dpdr = convertDataProcessingDetails(dpd);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property(), dpdr);
                    }
                }
                if (a.getOtherOriginalIDss() != null) {
                    for (ExternalIdentifier ei : a.getOtherOriginalIDss()) {
                        Resource eir = convertExternalIdentifier(ei);
                        rm.addProperty(OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property(), eir);
                    }
                }
                if (a.getLastEditDate() != null) {
                    Resource dr = convertDate(a.getLastEditDate());
                    rm.addProperty(OWLVocabulary.OWLObjectProperty.HasLastEditDate.property(), dr);
                }

                return rm;
            } catch (URISyntaxException e) {
                logger.error(e);
                throw new NewAPOToJenaModelException(e);
            }
        } else {
            throw new NewAPOToJenaModelException("found an " + a.getClass().getName() + " without an XUUID");
        }
    }

    public RDFModel getJenaModel() {
        return this.model;
    }

    public Patient getPatient() {
        return this.newPatient;
    }


    @Override
    public void close() throws IOException {
        if(!this.model.isClosed()){
            this.model.close();
        }
    }
}
