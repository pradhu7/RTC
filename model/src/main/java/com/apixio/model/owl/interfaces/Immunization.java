package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/
import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.donotimplement.BiologicalEntity;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimableEvent;

import java.util.Collection;


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: Immunization <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface Immunization  extends  BiologicalEntity, InsuranceClaimableEvent {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#110010
     */

    /**
     * Gets all property values for the ImmunizationAdminDate property.<p>
     *
     * @returns a collection of values for the ImmunizationAdminDate property.
     */
    Date getImmunizationAdminDate();


    /**
     * Adds a ImmunizationAdminDate property value.<p>
     *
     * @param newImmunizationAdminDate the ImmunizationAdminDate property value to be added
     */
    void setImmunizationAdminDate(Date newImmunizationAdminDate);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#120012
     */

    /**
     * Gets all property values for the ImmunizationMedications property.<p>
     *
     * @returns a collection of values for the ImmunizationMedications property.
     * Collection<? extends Medication> getImmunizationMedications();
     */
    Collection<Medication> getImmunizationMedications();

    /**
     * Add an element to the list
     */
    Collection<Medication> addImmunizationMedications(Medication anItem);

    /**
     * get element from list by Id
     */
    Medication getImmunizationMedicationsById(XUUID anID);

    /**
     * Adds a ImmunizationMedications property value.<p>
     *
     * @param newImmunizationMedications the ImmunizationMedications property value to be added
     * void setImmunizationMedications(Collection<? extends Medication> newImmunizationMedications);
     */
    void setImmunizationMedications(Collection<Medication> newImmunizationMedications);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#770007
     */

    /**
     * Gets all property values for the ImmunizationDateRange property.<p>
     *
     * @returns a collection of values for the ImmunizationDateRange property.
     */
    DateRange getImmunizationDateRange();


    /**
     * Adds a ImmunizationDateRange property value.<p>
     *
     * @param newImmunizationDateRange the ImmunizationDateRange property value to be added
     */
    void setImmunizationDateRange(DateRange newImmunizationDateRange);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#343003
     */

    /**
     * Gets all property values for the ImmunizationQuantity property.<p>
     *
     * @returns a collection of values for the ImmunizationQuantity property.
     */
    Double getImmunizationQuantity();


    /**
     * Adds a ImmunizationQuantity property value.<p>
     *
     * @param newImmunizationQuantity the ImmunizationQuantity property value to be added
     */
    void setImmunizationQuantity(Double newImmunizationQuantity);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#870008
     */

    /**
     * Gets all property values for the ImmunizationMedicationSeriesNumber property.<p>
     *
     * @returns a collection of values for the ImmunizationMedicationSeriesNumber property.
     */
    Integer getImmunizationMedicationSeriesNumber();


    /**
     * Adds a ImmunizationMedicationSeriesNumber property value.<p>
     *
     * @param newImmunizationMedicationSeriesNumber the ImmunizationMedicationSeriesNumber property value to be added
     */
    void setImmunizationMedicationSeriesNumber(Integer newImmunizationMedicationSeriesNumber);



}