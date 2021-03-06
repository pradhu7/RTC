package com.apixio.model.owl.interfaces;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimableEvent;
import com.apixio.model.owl.interfaces.donotimplement.MedicalEvent;
import com.apixio.model.owl.interfaces.donotimplement.PatientEncounterType;

import java.util.Collection;

/**
 * OWL annotation driven imports
 **/


/**
 *
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: PatientEncounter <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface PatientEncounter  extends InsuranceClaimableEvent, MedicalEvent {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000019
     */

    /**
     * Gets all property values for the PatientEncounterType property.<p>
     *
     *
     * @returns a collection of values for the PatientEncounterType property.
     */
    PatientEncounterType getPatientEncounterType();


    /**
     * Adds a PatientEncounterType property value.<p>
     *
     *
     * @param newPatientEncounterType the PatientEncounterType property value to be added
     */
    void setPatientEncounterType(PatientEncounterType newPatientEncounterType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000067
     */

    /**
     * Gets all property values for the PatientEncounterCareSite property.<p>
     *
     *
     * @returns a collection of values for the PatientEncounterCareSite property.
     */
    CareSite getPatientEncounterCareSite();


    /**
     * Adds a PatientEncounterCareSite property value.<p>
     *
     *
     * @param newPatientEncounterCareSite the PatientEncounterCareSite property value to be added
     */
    void setPatientEncounterCareSite(CareSite newPatientEncounterCareSite);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000086
     */

    /**
     * Gets all property values for the MedicalProfessionals property.<p>
     *
     * @returns a collection of values for the MedicalProfessionals property.
     * Collection<? extends MedicalProfessional> getMedicalProfessionals();
     *
     */
    Collection<MedicalProfessional> getMedicalProfessionals();

    /**
     * Add an element to the list
     *
     */
    Collection<MedicalProfessional> addMedicalProfessionals(MedicalProfessional anItem);

    /**
     * get element from list by Id
     *
     */
    MedicalProfessional getMedicalProfessionalsById(XUUID anID);

    /**
     * Adds a MedicalProfessionals property value.<p>
     *
     * @param newMedicalProfessionals the MedicalProfessionals property value to be added
     * void setMedicalProfessionals(Collection<? extends MedicalProfessional> newMedicalProfessionals);
     *
     */
    void setMedicalProfessionals(Collection<MedicalProfessional> newMedicalProfessionals);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000366
     */

    /**
     * Gets all property values for the EncounterDateRange property.<p>
     *
     *
     * @returns a collection of values for the EncounterDateRange property.
     */
    DateRange getEncounterDateRange();


    /**
     * Adds a EncounterDateRange property value.<p>
     *
     *
     * @param newEncounterDateRange the EncounterDateRange property value to be added
     */
    void setEncounterDateRange(DateRange newEncounterDateRange);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#200020
     */

    /**
     * Gets all property values for the ChiefComplaints property.<p>
     *
     * @returns a collection of values for the ChiefComplaints property.
     * Collection<? extends ClinicalCode> getChiefComplaints();
     *
     */
    Collection<ClinicalCode> getChiefComplaints();

    /**
     * Add an element to the list
     *
     */
    Collection<ClinicalCode> addChiefComplaints(ClinicalCode anItem);

    /**
     * get element from list by Id
     *
     */
    ClinicalCode getChiefComplaintsById(XUUID anID);

    /**
     * Adds a ChiefComplaints property value.<p>
     *
     * @param newChiefComplaints the ChiefComplaints property value to be added
     * void setChiefComplaints(Collection<? extends ClinicalCode> newChiefComplaints);
     *
     */
    void setChiefComplaints(Collection<ClinicalCode> newChiefComplaints);


}
