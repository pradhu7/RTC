package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/
import com.apixio.model.owl.interfaces.HICN;


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: MedicareInsurancePolicy <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface MedicareInsurancePolicy  extends com.apixio.model.owl.interfaces.InsurancePolicy {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000453
     */
     
    /**
     * Gets all property values for the HICNNumber property.<p>
     * 
     * @returns a collection of values for the HICNNumber property.
     */
    com.apixio.model.owl.interfaces.HICN getHICNNumber();


    /**
     * Adds a HICNNumber property value.<p>
     * 
     * @param newHICNNumber the HICNNumber property value to be added
     */
    void setHICNNumber(HICN newHICNNumber);



}
