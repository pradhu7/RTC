package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/
import com.apixio.model.owl.interfaces.donotimplement.PlanType;
import com.apixio.model.owl.interfaces.donotimplement.SocialEntity;

import com.apixio.model.owl.interfaces.donotimplement.*;


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: InsurancePlan <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface InsurancePlan  extends ProvenancedEntity, SocialEntity {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000048
     */

    /**
     * Gets all property values for the InsuranceType property.<p>
     *
     *
     * @returns a collection of values for the InsuranceType property.
     */
    PlanType getInsuranceType();


    /**
     * Adds a InsuranceType property value.<p>
     *
     *
     * @param newInsuranceType the InsuranceType property value to be added
     */
    void setInsuranceType(PlanType newInsuranceType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000057
     */

    /**
     * Gets all property values for the InsuranceCompany property.<p>
     *
     *
     * @returns a collection of values for the InsuranceCompany property.
     */
    InsuranceCompany getInsuranceCompany();


    /**
     * Adds a InsuranceCompany property value.<p>
     *
     *
     * @param newInsuranceCompany the InsuranceCompany property value to be added
     */
    void setInsuranceCompany(InsuranceCompany newInsuranceCompany);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000106
     */

    /**
     * Gets all property values for the InsurancePlanIdentifier property.<p>
     *
     *
     * @returns a collection of values for the InsurancePlanIdentifier property.
     */
    ExternalIdentifier getInsurancePlanIdentifier();


    /**
     * Adds a InsurancePlanIdentifier property value.<p>
     *
     *
     * @param newInsurancePlanIdentifier the InsurancePlanIdentifier property value to be added
     */
    void setInsurancePlanIdentifier(ExternalIdentifier newInsurancePlanIdentifier);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000122
     */

    /**
     * Gets all property values for the ContractIdentifier property.<p>
     *
     *
     * @returns a collection of values for the ContractIdentifier property.
     */
    ExternalIdentifier getContractIdentifier();


    /**
     * Adds a ContractIdentifier property value.<p>
     *
     *
     * @param newContractIdentifier the ContractIdentifier property value to be added
     */
    void setContractIdentifier(ExternalIdentifier newContractIdentifier);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000374
     */

    /**
     * Gets all property values for the GroupNumber property.<p>
     *
     *
     * @returns a collection of values for the GroupNumber property.
     */
    ExternalIdentifier getGroupNumber();


    /**
     * Adds a GroupNumber property value.<p>
     *
     *
     * @param newGroupNumber the GroupNumber property value to be added
     */
    void setGroupNumber(ExternalIdentifier newGroupNumber);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000053
     */

    /**
     * Gets all property values for the InsurancePlanName property.<p>
     *
     *
     * @returns a collection of values for the InsurancePlanName property.
     */
    String getInsurancePlanName();


    /**
     * Adds a InsurancePlanName property value.<p>
     *
     *
     * @param newInsurancePlanName the InsurancePlanName property value to be added
     */
    void setInsurancePlanName(String newInsurancePlanName);



}
