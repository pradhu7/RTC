package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/
import com.apixio.model.owl.interfaces.donotimplement.Contract;
import com.apixio.model.owl.interfaces.donotimplement.ProvenancedEntity;


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: InsurancePolicy <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface InsurancePolicy  extends ProvenancedEntity, Contract {





    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000036
     */

    /**
     * Gets all property values for the InsurancePlan property.<p>
     *
     *
     * @returns a collection of values for the InsurancePlan property.
     */
    InsurancePlan getInsurancePlan();


    /**
     * Adds a InsurancePlan property value.<p>
     *
     *
     * @param newInsurancePlan the InsurancePlan property value to be added
     */
    void setInsurancePlan(InsurancePlan newInsurancePlan);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000373
     */

    /**
     * Gets all property values for the MemberNumber property.<p>
     *
     *
     * @returns a collection of values for the MemberNumber property.
     */
    ExternalIdentifier getMemberNumber();


    /**
     * Adds a MemberNumber property value.<p>
     *
     *
     * @param newMemberNumber the MemberNumber property value to be added
     */
    void setMemberNumber(ExternalIdentifier newMemberNumber);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000375
     */

    /**
     * Gets all property values for the SubscriberID property.<p>
     *
     *
     * @returns a collection of values for the SubscriberID property.
     */
    ExternalIdentifier getSubscriberID();


    /**
     * Adds a SubscriberID property value.<p>
     *
     *
     * @param newSubscriberID the SubscriberID property value to be added
     */
    void setSubscriberID(ExternalIdentifier newSubscriberID);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000452
     */

    /**
     * Gets all property values for the InsurancePolicyDateRange property.<p>
     *
     *
     * @returns a collection of values for the InsurancePolicyDateRange property.
     */
    DateRange getInsurancePolicyDateRange();


    /**
     * Adds a InsurancePolicyDateRange property value.<p>
     *
     *
     * @param newInsurancePolicyDateRange the InsurancePolicyDateRange property value to be added
     */
    void setInsurancePolicyDateRange(DateRange newInsurancePolicyDateRange);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000052
     */

    /**
     * Gets all property values for the InsurnacePolicyName property.<p>
     *
     *
     * @returns a collection of values for the InsurnacePolicyName property.
     */
    String getInsurnacePolicyName();


    /**
     * Adds a InsurnacePolicyName property value.<p>
     *
     *
     * @param newInsurnacePolicyName the InsurnacePolicyName property value to be added
     */
    void setInsurnacePolicyName(String newInsurnacePolicyName);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000376
     */

    /**
     * Gets all property values for the InsuranceSequnceNumberValue property.<p>
     *
     *
     * @returns a collection of values for the InsuranceSequnceNumberValue property.
     */
    Integer getInsuranceSequnceNumberValue();


    /**
     * Adds a InsuranceSequnceNumberValue property value.<p>
     *
     *
     * @param newInsuranceSequnceNumberValue the InsuranceSequnceNumberValue property value to be added
     */
    void setInsuranceSequnceNumberValue(Integer newInsuranceSequnceNumberValue);



}
