package com.apixio.model.owl.interfaces.donotimplement;

import java.util.Collection;

/**
 * OWL annotation driven imports 
**/
import org.joda.time.DateTime;
import java.net.URI;
import com.apixio.XUUID;


/**
 * 
 * For CMS validated insurance claims, we use this class to annotate whether a particular error code is to be filtered from being passed for coding or being passed to the output report
	*
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: InsuranceClaimErrorCodeFilter <br>
 * @version generated on Thu Jun 09 15:50:23 PDT 2016 by jctoledo
 */

public interface InsuranceClaimErrorCodeFilter  extends SoftwareEntity {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000079
     */
     
    /**
     * Gets all property values for the ErrorCodeValue property.<p>
     * 
     * 
     * @returns a collection of values for the ErrorCodeValue property.
     */
    String getErrorCodeValue();


    /**
     * Adds a ErrorCodeValue property value.<p>
     * 
     * 
     * @param newErrorCodeValue the ErrorCodeValue property value to be added
     */
    void setErrorCodeValue(String newErrorCodeValue);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000254
     */
     
    /**
     * Gets all property values for the RouteToCoder property.<p>
     * 
     * if value is true then the associated insurance claim will be sent out for coding despite CMS's validation error
	*
     * @returns a collection of values for the RouteToCoder property.
     */
    Boolean getRouteToCoder();


    /**
     * Adds a RouteToCoder property value.<p>
     * 
     * if value is true then the associated insurance claim will be sent out for coding despite CMS's validation error
	*
     * @param newRouteToCoder the RouteToCoder property value to be added
     */
    void setRouteToCoder(Boolean newRouteToCoder);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000256
     */
     
    /**
     * Gets all property values for the InOutputReport property.<p>
     * 
     * if the value is set to true then insurance claim will be shown in output report
	*
     * @returns a collection of values for the InOutputReport property.
     */
    Boolean getInOutputReport();


    /**
     * Adds a InOutputReport property value.<p>
     * 
     * if the value is set to true then insurance claim will be shown in output report
	*
     * @param newInOutputReport the InOutputReport property value to be added
     */
    void setInOutputReport(Boolean newInOutputReport);



}
