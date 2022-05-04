package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.Allergy;
import com.apixio.model.owl.interfaces.SocialHistory;
import com.apixio.model.owl.interfaces.RiskAdjustmentInsuranceClaim;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/10/16.
 */
public class NewPatient implements Patient{

    private Collection<Allergy> allergies;
    private Collection<Document> documents;
    private Collection<FeeForServiceInsuranceClaim> ffsClaims;
    private Collection<RiskAdjustmentInsuranceClaim> raClaims;
    private Collection<Procedure> medicalProcedures;
    private Demographics demographics;
    private Collection<Problem> problems;
    private XUUID xuuid;
    private Date lastEditDate;
    private ExternalIdentifier externalId;
    private ContactDetails contactDets;
    private Collection<PatientEncounter> encounters;
    private Collection<Prescription> prescriptions;
    private Collection<BiometricValue> biometricVals;
    private Collection<Immunization> immunizations;
    private Collection<SocialHistory> socialHistories;
    private Collection<FamilyHistory> familyHistories;
    private Collection<CareSite> caresites;
    private Collection<MedicalProfessional> medicalProfessionals;
    private Collection<InsurancePolicy> policies;
    private Collection<ContactDetails> alternateContactDetails;
    private Collection<Demographics> alternateDemos;
    private Collection<PrimaryCareProvider> pcps;
    private Collection<LabResult> labResults;
    private Collection<XUUID> otherIds;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDetails;
    private String logicalId;
    private URI uri;

    public NewPatient(){
        this.xuuid = XUUID.create("NewPatient");

    }

    @Override
    public Collection<RiskAdjustmentInsuranceClaim> getRiskAdjustmentClaims() {
        return this.raClaims;
    }

    @Override
    public Collection<RiskAdjustmentInsuranceClaim> addRiskAdjustmentClaims(RiskAdjustmentInsuranceClaim anItem) {
        return null;
    }

    @Override
    public RiskAdjustmentInsuranceClaim getRiskAdjustmentClaimsById(XUUID anID) {
        return null;
    }

    @Override
    public void setRiskAdjustmentClaims(Collection<RiskAdjustmentInsuranceClaim> newRiskAdjustmentClaims) {
        this.raClaims =  newRiskAdjustmentClaims;
    }

    @Override
    public Collection<FeeForServiceInsuranceClaim> getFeeForServiceClaims() {
        return this.ffsClaims;
    }

    @Override
    public Collection<FeeForServiceInsuranceClaim> addFeeForServiceClaims(FeeForServiceInsuranceClaim anItem) {
        return null;
    }

    @Override
    public FeeForServiceInsuranceClaim getFeeForServiceClaimsById(XUUID anID) {
        return null;
    }

    @Override
    public void setFeeForServiceClaims(Collection<FeeForServiceInsuranceClaim> newFeeForServiceClaims) {
        this.ffsClaims = newFeeForServiceClaims;
    }


    @Override
    public Collection<Procedure> getMedicalProcedures() {
        return this.medicalProcedures;
    }

    @Override
    public Collection<Procedure> addMedicalProcedures(Procedure anItem) {
        return null;
    }

    @Override
    public Procedure getMedicalProceduresById(XUUID anID) {
        return null;
    }

    @Override
    public void setMedicalProcedures(Collection<Procedure> newMedicalProcedures) {
        this.medicalProcedures = newMedicalProcedures;
    }

    @Override
    public Collection<FamilyHistory> getFamilyHistories() {
        return this.familyHistories;
    }

    @Override
    public Collection<FamilyHistory> addFamilyHistories(FamilyHistory anItem) {
        return null;
    }

    @Override
    public FamilyHistory getFamilyHistoriesById(XUUID anID) {
        return null;
    }

    @Override
    public void setFamilyHistories(Collection<FamilyHistory> newFamilyHistories) {
        this.familyHistories = newFamilyHistories;
    }

    @Override
    public Collection<SocialHistory> getSocialHistories() {
        return this.socialHistories;
    }

    @Override
    public Collection<SocialHistory> addSocialHistories(SocialHistory anItem) {
        return null;
    }

    @Override
    public SocialHistory getSocialHistoriesById(XUUID anID) {
        return null;
    }

    @Override
    public void setSocialHistories(Collection<SocialHistory> newSocialHistories) {
        this.socialHistories = newSocialHistories;
    }

    @Override
    public Collection<CareSite> getPatientCareSites() {
        return this.caresites;
    }

    @Override
    public Collection<CareSite> addPatientCareSites(CareSite anItem) {
        return null;
    }

    @Override
    public CareSite getPatientCareSitesById(XUUID anID) {
        return null;
    }

    @Override
    public void setPatientCareSites(Collection<CareSite> newPatientCareSites) {
        this.caresites = newPatientCareSites;
    }

    @Override
    public Collection<MedicalProfessional> getMedicalProfessionals() {
        return this.medicalProfessionals;
    }

    @Override
    public Collection<MedicalProfessional> addMedicalProfessionals(MedicalProfessional anItem) {
        return null;
    }

    @Override
    public MedicalProfessional getMedicalProfessionalsById(XUUID anID) {
        return null;
    }

    @Override
    public void setMedicalProfessionals(Collection<MedicalProfessional> newMedicalProfessionals) {
        this.medicalProfessionals = newMedicalProfessionals;
    }

    @Override
    public Collection<PatientEncounter> getEncounters() {
        return this.encounters;
    }

    @Override
    public Collection<PatientEncounter> addEncounters(PatientEncounter anItem) {
        if (this.encounters == null){
            this.encounters = new ArrayList<>();
            this.encounters.add(anItem);
            return this.encounters;
        } else {
            this.encounters.add(anItem);
            return this.encounters;
        }
    }

    @Override
    public PatientEncounter getEncountersById(XUUID anID) {
        return null;
    }

    @Override
    public void setEncounters(Collection<PatientEncounter> newEncounters) {
        this.encounters = newEncounters;
    }

    @Override
    public Collection<Document> getDocuments() {
        return this.documents;
    }

    @Override
    public Collection<Document> addDocuments(Document anItem) {
        return null;
    }

    @Override
    public Document getDocumentsById(XUUID anID) {
        return null;
    }

    @Override
    public void setDocuments(Collection<Document> newDocuments) {
        this.documents = newDocuments;
    }


    @Override
    public Demographics getDemographics() {
        return this.demographics;
    }

    @Override
    public void setDemographics(Demographics newDemographics) {
        this.demographics = newDemographics;
    }



    @Override
    public ContactDetails getContactDetails() {
        return this.contactDets;
    }

    @Override
    public void setContactDetails(ContactDetails newContactDetails) {
        this.contactDets = newContactDetails;
    }

    @Override
    public Collection<ContactDetails> getAlternateContactDetailss() {
        return this.alternateContactDetails;
    }

    @Override
    public Collection<ContactDetails> addAlternateContactDetailss(ContactDetails anItem) {
        return null;
    }

    @Override
    public ContactDetails getAlternateContactDetailssById(XUUID anID) {
        return null;
    }

    @Override
    public void setAlternateContactDetailss(Collection<ContactDetails> newAlternateContactDetailss) {
        this.alternateContactDetails = newAlternateContactDetailss;
    }

    @Override
    public Collection<Demographics> getAlternateDemographicss() {
        return this.alternateDemos;
    }

    @Override
    public Collection<Demographics> addAlternateDemographicss(Demographics anItem) {
        return null;
    }

    @Override
    public Demographics getAlternateDemographicssById(XUUID anID) {
        return null;
    }

    @Override
    public void setAlternateDemographicss(Collection<Demographics> newAlternateDemographicss) {
        this.alternateDemos = newAlternateDemographicss;
    }

    @Override
    public Collection<PrimaryCareProvider> getPrimaryCareProviders() {
        return this.pcps;
    }

    @Override
    public Collection<PrimaryCareProvider> addPrimaryCareProviders(PrimaryCareProvider anItem) {
        return null;
    }

    @Override
    public PrimaryCareProvider getPrimaryCareProvidersById(XUUID anID) {
        return null;
    }

    @Override
    public void setPrimaryCareProviders(Collection<PrimaryCareProvider> newPrimaryCareProviders) {
        this.pcps = newPrimaryCareProviders;
    }

    @Override
    public Collection<InsurancePolicy> getInsurancePolicies() {
        return this.policies;
    }

    @Override
    public Collection<InsurancePolicy> addInsurancePolicies(InsurancePolicy anItem) {
        if(this.policies != null){
            this.policies.add(anItem);
        }else{
            this.policies = new ArrayList<>();
            this.policies.add(anItem);
        }
        return this.policies;
    }

    @Override
    public InsurancePolicy getInsurancePoliciesById(XUUID anID) {
        if(this.policies != null){
            for(InsurancePolicy p : this.policies){
                if(p.getInternalXUUID().equals(anID)){
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public void setInsurancePolicies(Collection<InsurancePolicy> newInsurancePolicies) {
        this.policies = newInsurancePolicies;
    }

    @Override
    public Collection<Problem> getProblems() {
        return this.problems;
    }

    @Override
    public Collection<Problem> addProblems(Problem anItem) {
        return null;
    }

    @Override
    public Problem getProblemsById(XUUID anID) {
        return null;
    }

    @Override
    public void setProblems(Collection<Problem> newProblems) {
        this.problems = newProblems;
    }

    @Override
    public Collection<Prescription> getPrescriptions() {
        return this.prescriptions;
    }

    @Override
    public Collection<Prescription> addPrescriptions(Prescription anItem) {
        return null;
    }

    @Override
    public Prescription getPrescriptionsById(XUUID anID) {
        return null;
    }

    @Override
    public void setPrescriptions(Collection<Prescription> newPrescriptions) {
        this.prescriptions = newPrescriptions;
    }

    @Override
    public Collection<BiometricValue> getBiometricValues() {
        return this.biometricVals;
    }

    @Override
    public Collection<BiometricValue> addBiometricValues(BiometricValue anItem) {
        return null;
    }

    @Override
    public BiometricValue getBiometricValuesById(XUUID anID) {
        return null;
    }

    @Override
    public void setBiometricValues(Collection<BiometricValue> newBiometricValues) {
        this.biometricVals = newBiometricValues;
    }

    @Override
    public Collection<Allergy> getAllergies() {
        return this.allergies;
    }

    @Override
    public Collection<Allergy> addAllergies(Allergy anItem) {
        return null;
    }

    @Override
    public Allergy getAllergiesById(XUUID anID) {
        return null;
    }

    @Override
    public void setAllergies(Collection<Allergy> newAllergies) {
        this.allergies = newAllergies;
    }

    @Override
    public Collection<LabResult> getLabResults() {
        return this.labResults;
    }

    @Override
    public Collection<LabResult> addLabResults(LabResult anItem) {
        return null;
    }

    @Override
    public LabResult getLabResultsById(XUUID anID) {
        return null;
    }

    @Override
    public void setLabResults(Collection<LabResult> newLabResults) {
        this.labResults = newLabResults;
    }

    @Override
    public Collection<Immunization> getImmunizations() {
        return this.immunizations;
    }

    @Override
    public Collection<Immunization> addImmunizations(Immunization anItem) {
        return null;
    }

    @Override
    public Immunization getImmunizationsById(XUUID anID) {
        return null;
    }

    @Override
    public void setImmunizations(Collection<Immunization> newImmunizations) {
        this.immunizations = newImmunizations;
    }

    @Override
    public XUUID getInternalXUUID() {
        return this.xuuid;
    }

    @Override
    public void setInternalXUUID(XUUID anXUUID) {
        this.xuuid = anXUUID;
    }

    @Override
    public URI getURI() {
        String uriStr = "http://apixio.com/"+Patient.class.getName()+"/"+this.getLogicalID();
        return this.uri;
    }

    @Override
    public void setURI(URI aURI) {
        this.uri = aURI;
    }

    @Override
    public Collection<XUUID> getOtherIds() {
        return this.otherIds;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {
        this.otherIds = others;
    }

    @Override
    public String getLogicalID() {
        return this.logicalId;
    }

    @Override
    public void setLogicalID(String anID) {
        this.logicalId = anID;
    }

    @Override
    public ExternalIdentifier getOriginalID() {
        return this.externalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.externalId = newEntityOriginalID;
    }

    @Override
    public Collection<ExternalIdentifier> getOtherOriginalIDss() {
        return this.otherOriginalIds;
    }

    @Override
    public Collection<ExternalIdentifier> addOtherOriginalIDss(ExternalIdentifier anItem) {
        if(this.otherOriginalIds != null){
            this.otherOriginalIds.add(anItem);
            return this.otherOriginalIds;
        }
        return null;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        if(this.otherOriginalIds != null && anID != null){
            for(ExternalIdentifier ei : this.otherOriginalIds){
                if(ei.getInternalXUUID().equals(anID)){
                    return ei;
                }
            }
        }
        return null;
    }

    @Override
    public void setOtherOriginalIDss(Collection<ExternalIdentifier> newOtherOriginalIDss) {
        this.otherOriginalIds = newOtherOriginalIDss;
    }



    @Override
    public Collection<DataProcessingDetail> getDataProcessingDetails() {
        return this.dataProcDetails;
    }

    @Override
    public Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem) {
        if(this.dataProcDetails != null){
            this.dataProcDetails.add(anItem);
        }else{
            this.dataProcDetails = new ArrayList<>();
            this.dataProcDetails.add(anItem);
        }
        return this.dataProcDetails;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        if(anID != null && this.dataProcDetails != null){
            for(DataProcessingDetail dpd: this.dataProcDetails){
                if(dpd.getInternalXUUID().equals(anID))
                    return dpd;
            }
        }
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcDetails = newDataProcessingDetails;
    }


    @Override
    public Date getLastEditDate() {
        return this.lastEditDate;
    }

    @Override
    public void setLastEditDate(Date newLastEditDate) {
        this.lastEditDate = newLastEditDate;
    }

    @Override
    public Collection<AmendmentAnnotation> getAmendmentAnnotations() {
        return null;
    }

    @Override
    public Collection<AmendmentAnnotation> addAmendmentAnnotations(AmendmentAnnotation anItem) {
        return null;
    }

    @Override
    public AmendmentAnnotation getAmendmentAnnotationsById(XUUID anID) {
        return null;
    }

    @Override
    public void setAmendmentAnnotations(Collection<AmendmentAnnotation> newAmendmentAnnotations) {

    }
}
