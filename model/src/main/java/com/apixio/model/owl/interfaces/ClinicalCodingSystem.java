package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/


import com.apixio.model.owl.interfaces.donotimplement.MedicalCodeEntity;

/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: ClinicalCodingSystem <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface ClinicalCodingSystem  extends MedicalCodeEntity {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000164
     */

    /**
     * Gets all property values for the ClinicalCodingSystemName property.<p>
     *
     * @returns a collection of values for the ClinicalCodingSystemName property.
     */
    String getClinicalCodingSystemName();


    /**
     * Adds a ClinicalCodingSystemName property value.<p>
     *
     * @param newClinicalCodingSystemName the ClinicalCodingSystemName property value to be added
     */
    void setClinicalCodingSystemName(String newClinicalCodingSystemName);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000397
     */

    /**
     * Gets all property values for the ClinicalCodingSystemOID property.<p>
     *
     * @returns a collection of values for the ClinicalCodingSystemOID property.
     */
    String getClinicalCodingSystemOID();


    /**
     * Adds a ClinicalCodingSystemOID property value.<p>
     *
     * @param newClinicalCodingSystemOID the ClinicalCodingSystemOID property value to be added
     */
    void setClinicalCodingSystemOID(String newClinicalCodingSystemOID);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000398
     */

    /**
     * Gets all property values for the ClinicalCodingSystemVersion property.<p>
     *
     * @returns a collection of values for the ClinicalCodingSystemVersion property.
     */
    String getClinicalCodingSystemVersion();


    /**
     * Adds a ClinicalCodingSystemVersion property value.<p>
     *
     * @param newClinicalCodingSystemVersion the ClinicalCodingSystemVersion property value to be added
     */
    void setClinicalCodingSystemVersion(String newClinicalCodingSystemVersion);

}
