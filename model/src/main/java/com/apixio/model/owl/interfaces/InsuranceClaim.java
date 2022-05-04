package com.apixio.model.owl.interfaces;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimStatusType;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimableEvent;
import com.apixio.model.owl.interfaces.donotimplement.Invoice;
import com.apixio.model.owl.interfaces.donotimplement.ProvenancedEntity;

import java.util.Collection;

/**
 * OWL annotation driven imports
 **/


/**
 * A claim is a detailed invoice that your health care provider sends to the health insurer. This invoice shows exactly what services you received.
 * <p/>
 * Generated by Apixio's code generation tool. <br>
 * Source Class: InsuranceClaim <br>
 *
 * @version generated on Tue Sep 13 13:37:11 PDT 2016 by jctoledo
 */

public interface InsuranceClaim extends Invoice, ProvenancedEntity {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000116
     */

    /**
     * Gets all property values for the InsuranceClaimCareSite property.<p>
     *
     * @returns a collection of values for the InsuranceClaimCareSite property.
     */
    CareSite getInsuranceClaimCareSite();


    /**
     * Adds a InsuranceClaimCareSite property value.<p>
     *
     * @param newInsuranceClaimCareSite the InsuranceClaimCareSite property value to be added
     */
    void setInsuranceClaimCareSite(CareSite newInsuranceClaimCareSite);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000140
     */

    /**
     * Gets all property values for the InsuranceClaimStatus property.<p>
     *
     * @returns a collection of values for the InsuranceClaimStatus property.
     */
    InsuranceClaimStatusType getInsuranceClaimStatus();


    /**
     * Adds a InsuranceClaimStatus property value.<p>
     *
     * @param newInsuranceClaimStatus the InsuranceClaimStatus property value to be added
     */
    void setInsuranceClaimStatus(InsuranceClaimStatusType newInsuranceClaimStatus);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000148
     */

    /**
     * Gets all property values for the InsuranceClaimProcessingDate property.<p>
     *
     * @returns a collection of values for the InsuranceClaimProcessingDate property.
     */
    Date getInsuranceClaimProcessingDate();


    /**
     * Adds a InsuranceClaimProcessingDate property value.<p>
     *
     * @param newInsuranceClaimProcessingDate the InsuranceClaimProcessingDate property value to be added
     */
    void setInsuranceClaimProcessingDate(Date newInsuranceClaimProcessingDate);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000250
     */

    /**
     * Gets all property values for the CMSClaimValidationResult property.<p>
     *
     * @returns a collection of values for the CMSClaimValidationResult property.
     */
    CMSClaimValidationResult getCMSClaimValidationResult();


    /**
     * Adds a CMSClaimValidationResult property value.<p>
     *
     * @param newCMSClaimValidationResult the CMSClaimValidationResult property value to be added
     */
    void setCMSClaimValidationResult(CMSClaimValidationResult newCMSClaimValidationResult);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#032024
     */

    /**
     * Gets all property values for the ClaimSubmissionDate property.<p>
     *
     * @returns a collection of values for the ClaimSubmissionDate property.
     */
    Date getClaimSubmissionDate();


    /**
     * Adds a ClaimSubmissionDate property value.<p>
     *
     * @param newClaimSubmissionDate the ClaimSubmissionDate property value to be added
     */
    void setClaimSubmissionDate(Date newClaimSubmissionDate);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#250025
     */

    /**
     * Gets all property values for the ClaimedEvents property.<p>
     *
     * @returns a collection of values for the ClaimedEvents property.
     * Collection<? extends InsuranceClaimableEvent> getClaimedEvents();
     * A pointer to the event that is claimed to an insurance plan through a person's policy
     */
    Collection<InsuranceClaimableEvent> getClaimedEvents();

    /**
     * Adds a ClaimedEvents property value.<p>
     *
     * @param newClaimedEvents the ClaimedEvents property value to be added
     *                         void setClaimedEvents(Collection<? extends InsuranceClaimableEvent> newClaimedEvents);
     *                         A pointer to the event that is claimed to an insurance plan through a person's policy
     */
    void setClaimedEvents(Collection<InsuranceClaimableEvent> newClaimedEvents);

    /**
     * Add an element to the list
     * A pointer to the event that is claimed to an insurance plan through a person's policy
     */
    Collection<InsuranceClaimableEvent> addClaimedEvents(InsuranceClaimableEvent anItem);

    /**
     * get element from list by Id
     * A pointer to the event that is claimed to an insurance plan through a person's policy
     */
    InsuranceClaimableEvent getClaimedEventsById(XUUID anID);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000156
     */

    /**
     * Gets all property values for the SourceSystem property.<p>
     *
     * @returns a collection of values for the SourceSystem property.
     */
    String getSourceSystem();


    /**
     * Adds a SourceSystem property value.<p>
     *
     * @param newSourceSystem the SourceSystem property value to be added
     */
    void setSourceSystem(String newSourceSystem);


}