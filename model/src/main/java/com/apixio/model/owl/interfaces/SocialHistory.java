package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/
import com.apixio.model.owl.interfaces.ClinicalCode;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.CodedEntity;
import com.apixio.model.owl.interfaces.donotimplement.PatientHistory;


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: SocialHistory <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface SocialHistory  extends CodedEntity, PatientHistory {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000058
     */
     
    /**
     * Gets all property values for the SocialHistoryDate property.<p>
     * 
     * @returns a collection of values for the SocialHistoryDate property.
     */
    com.apixio.model.owl.interfaces.Date getSocialHistoryDate();


    /**
     * Adds a SocialHistoryDate property value.<p>
     * 
     * @param newSocialHistoryDate the SocialHistoryDate property value to be added
     */
    void setSocialHistoryDate(Date newSocialHistoryDate);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000064
     */
     
    /**
     * Gets all property values for the SocialHistoryType property.<p>
     * 
     * @returns a collection of values for the SocialHistoryType property.
     */
    com.apixio.model.owl.interfaces.ClinicalCode getSocialHistoryType();


    /**
     * Adds a SocialHistoryType property value.<p>
     * 
     * @param newSocialHistoryType the SocialHistoryType property value to be added
     */
    void setSocialHistoryType(ClinicalCode newSocialHistoryType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000060
     */
     
    /**
     * Gets all property values for the SocialHistoryFieldName property.<p>
     * 
     * @returns a collection of values for the SocialHistoryFieldName property.
     */
    String getSocialHistoryFieldName();


    /**
     * Adds a SocialHistoryFieldName property value.<p>
     * 
     * @param newSocialHistoryFieldName the SocialHistoryFieldName property value to be added
     */
    void setSocialHistoryFieldName(String newSocialHistoryFieldName);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000061
     */
     
    /**
     * Gets all property values for the SocialHistoryValue property.<p>
     * 
     * @returns a collection of values for the SocialHistoryValue property.
     */
    String getSocialHistoryValue();


    /**
     * Adds a SocialHistoryValue property value.<p>
     * 
     * @param newSocialHistoryValue the SocialHistoryValue property value to be added
     */
    void setSocialHistoryValue(String newSocialHistoryValue);



}
