package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/


import com.apixio.model.owl.interfaces.donotimplement.CodedEntity;
import com.apixio.model.owl.interfaces.donotimplement.PatientHistory;

/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: FamilyHistory <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface FamilyHistory  extends CodedEntity, PatientHistory {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000013
     */
     
    /**
     * Gets all property values for the FamilyHistoryValue property.<p>
     * 
     * @returns a collection of values for the FamilyHistoryValue property.
     */
    String getFamilyHistoryValue();


    /**
     * Adds a FamilyHistoryValue property value.<p>
     * 
     * @param newFamilyHistoryValue the FamilyHistoryValue property value to be added
     */
    void setFamilyHistoryValue(String newFamilyHistoryValue);



}
