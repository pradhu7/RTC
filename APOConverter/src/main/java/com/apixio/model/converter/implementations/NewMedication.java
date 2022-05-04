package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/28/16.
 */
public class NewMedication implements Medication {
    private XUUID xuuid;
    private String brandName;
    private String genericName;
    private String strength;
    private String form;
    private String units;
    private String routeOfAdmin;
    private Collection<String> ingredients;
    private XUUID sourceEncounter;
    private XUUID primaryMedPro;
    private Collection<XUUID> altMedProIds;
    private ExternalIdentifier externalId;
    private Collection<DataProcessingDetail> dataProcDets;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private ClinicalCode code;
    private String logicalId;
    private String amount;
    private String dosage;

    public NewMedication(){
        this.xuuid = XUUID.create("NewMedication");
    }

    @Override
    public String getMedicationDosage() {
        return this.dosage;
    }

    @Override
    public void setMedicationDosage(String newMedicationDosage) {
        this.dosage = newMedicationDosage;
    }

    @Override
    public String getMedicationAmount() {
        return this.amount;
    }

    @Override
    public void setMedicationAmount(String newMedicationAmount) {
        this.amount = newMedicationAmount;
    }

    @Override
    public String getBrandName() {
        return this.brandName;
    }

    @Override
    public void setBrandName(String newBrandName) {
        this.brandName = newBrandName;
    }

    @Override
    public String getGenericName() {
        return this.genericName;
    }

    @Override
    public void setGenericName(String newGenericName) {
        this.genericName = newGenericName;
    }

    @Override
    public String getDrugStrength() {
        return this.strength;
    }

    @Override
    public void setDrugStrength(String newDrugStrength) {
        this.strength = newDrugStrength;
    }

    @Override
    public String getDrugForm() {
        return this.form;
    }

    @Override
    public void setDrugForm(String newDrugForm) {
        this.form = newDrugForm;
    }

    @Override
    public String getDrugUnits() {
        return this.units;
    }

    @Override
    public void setDrugUnits(String newDrugUnits) {
        this.units = newDrugUnits;
    }

    @Override
    public String getRouteOfAdministration() {
        return this.routeOfAdmin;
    }

    @Override
    public void setRouteOfAdministration(String newRouteOfAdministration) {
        this.routeOfAdmin = newRouteOfAdministration;
    }

    @Override
    public Collection<String> getDrugIngredients() {
        return this.ingredients;
    }

    @Override
    public Collection<String> addDrugIngredients(String anItem) {
        return null;
    }

    @Override
    public String getDrugIngredientsById(XUUID anID) {
        return null;
    }

    @Override
    public void setDrugIngredients(Collection<String> newDrugIngredients) {
        this.ingredients = newDrugIngredients;
    }

    @Override
    public XUUID getSourceEncounterId() {
        return this.sourceEncounter;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {
        this.sourceEncounter = anID;
    }

    @Override
    public XUUID getPrimaryMedicalProfessional() {
        return this.primaryMedPro;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.primaryMedPro = anId;
    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return this.altMedProIds;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.altMedProIds = medicalPros;
    }

    @Override
    public ClinicalCode getCodedEntityClinicalCode() {
        return this.code;
    }

    @Override
    public void setCodedEntityClinicalCode(ClinicalCode newCodedEntityClinicalCode) {
        this.code = newCodedEntityClinicalCode;
    }

    @Override
    public Collection<String> getCodedEntityNames() {
        return null;
    }

    @Override
    public Collection<String> addCodedEntityNames(String anItem) {
        return null;
    }

    @Override
    public String getCodedEntityNamesById(XUUID anID) {
        return null;
    }

    @Override
    public void setCodedEntityNames(Collection<String> newCodedEntityNames) {

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
        }else{
            this.otherOriginalIds = new ArrayList<>();
            this.otherOriginalIds.add(anItem);
        }
        return this.otherOriginalIds;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        if(this.otherOriginalIds != null){
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
        return this.dataProcDets;
    }

    @Override
    public Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem) {
        return null;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcDets = newDataProcessingDetails;
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
        return null;
    }

    @Override
    public void setURI(URI aURI) {

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
        this.logicalId= anID;
    }

    @Override
    public Date getLastEditDate() {
        return this.lastEditDate;
    }

    @Override
    public void setLastEditDate(Date newLastEditDate) {
        this.lastEditDate = newLastEditDate;
    }
}
