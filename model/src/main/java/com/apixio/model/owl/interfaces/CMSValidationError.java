package com.apixio.model.owl.interfaces;

import com.apixio.model.owl.interfaces.donotimplement.SoftwareEntity;

/**
 * OWL annotation driven imports
 **/


/**
 * 
 * Any validation error created by CMS when processing Risk adjustment insurance claims (RAPS) or patient encounter data (EDPS)
	*
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: CMSValidationError <br>
 * @version generated on Wed Jun 08 11:20:18 PDT 2016 by jctoledo
 */

public interface CMSValidationError  extends SoftwareEntity {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000239
     */

    /**
     * Gets all property values for the CMSValidationErrorCodeValue property.<p>
     *
     * the value of the error code itself
     *
     * @returns a collection of values for the CMSValidationErrorCodeValue property.
     */
    String getCMSValidationErrorCodeValue();


    /**
     * Adds a CMSValidationErrorCodeValue property value.<p>
     *
     * the value of the error code itself
     *
     * @param newCMSValidationErrorCodeValue the CMSValidationErrorCodeValue property value to be added
     */
    void setCMSValidationErrorCodeValue(String newCMSValidationErrorCodeValue);



}
