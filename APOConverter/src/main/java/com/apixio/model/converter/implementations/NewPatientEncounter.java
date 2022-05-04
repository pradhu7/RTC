package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.PatientEncounterType;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/25/16.
 */
public class NewPatientEncounter implements PatientEncounter {
    //TODO - there is redundancy here between medicalProfessional list and xuuid medical pro list and medical pro xuuid
    private XUUID xuuid;
    private PatientEncounterType patientEncType;
    private CareSite careSite;
    private Collection<MedicalProfessional> medicalPros;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private DateRange encounterDateRange;
    private Collection<ClinicalCode> chiefComplaints;
    private XUUID sourceId;
    private Collection<XUUID> alternateMedProsIds;
    private XUUID medicalPro;
    private ExternalIdentifier externalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDetails;
    private ClinicalCode code;
    private String logicalId;

    public NewPatientEncounter() {
        this.xuuid = XUUID.create("NewPatientEncounter");
        this.setLastEditDate(ConverterUtils.createApixioDate(new DateTime()));
    }

    @Override
    public PatientEncounterType getPatientEncounterType() {
        return this.patientEncType;
    }

    @Override
    public void setPatientEncounterType(PatientEncounterType newPatientEncounterType) {
        this.patientEncType = newPatientEncounterType;
    }

    @Override
    public CareSite getPatientEncounterCareSite() {
        return this.careSite;
    }

    @Override
    public void setPatientEncounterCareSite(CareSite newPatientEncounterCareSite) {
        this.careSite = newPatientEncounterCareSite;
    }

    @Override
    public Collection<MedicalProfessional> getMedicalProfessionals() {
        return this.medicalPros;
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
        this.medicalPros = newMedicalProfessionals;
    }


    @Override
    public DateRange getEncounterDateRange() {
        return this.encounterDateRange;
    }

    @Override
    public void setEncounterDateRange(DateRange newEncounterDateRange) {
        this.encounterDateRange = newEncounterDateRange;
    }

    @Override
    public Collection<ClinicalCode> getChiefComplaints() {
        return this.chiefComplaints;
    }

    @Override
    public Collection<ClinicalCode> addChiefComplaints(ClinicalCode anItem) {
        return null;
    }

    @Override
    public ClinicalCode getChiefComplaintsById(XUUID anID) {
        return null;
    }

    @Override
    public void setChiefComplaints(Collection<ClinicalCode> newChiefComplaints) {
        this.chiefComplaints = newChiefComplaints;
    }

    @Override
    public XUUID getSourceEncounterId() {
        return this.sourceId;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {
        this.sourceId = anID;
    }

    @Override
    public XUUID getPrimaryMedicalProfessional() {
        return this.medicalPro;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.medicalPro = anId;
    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return this.alternateMedProsIds;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.alternateMedProsIds = medicalPros;
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
    public XUUID getInternalXUUID() {
        return xuuid;
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
        this.logicalId = anID;
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
        if (this.otherOriginalIds == null) {
            this.otherOriginalIds = new ArrayList<>();
        }
        if (anItem != null) {
            this.otherOriginalIds.add(anItem);
            return this.otherOriginalIds;
        }
        return null;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        if (this.otherOriginalIds != null && anID != null) {
            for (ExternalIdentifier ei : this.otherOriginalIds) {
                if (ei.getInternalXUUID().equals(anID)) {
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
        return null;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcDetails = newDataProcessingDetails;
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
