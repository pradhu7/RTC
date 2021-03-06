package com.apixio.model.owl.interfaces;

import java.util.Collection;

/**
 * OWL annotation driven imports 
**/
import com.apixio.model.owl.interfaces.donotimplement.OperationAnnotationType;
import com.apixio.model.owl.interfaces.donotimplement.Provenance;
import com.apixio.model.owl.interfaces.donotimplement.StatusAnnotationType;
import org.joda.time.DateTime;
import java.net.URI;
import com.apixio.XUUID;


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: AmendmentAnnotation <br>
 * @version generated on Mon Apr 04 12:43:21 PDT 2016 by jctoledo
 */

public interface AmendmentAnnotation  extends Provenance {




    /* ***************************************************
     * Property http://apixio.com/ontology/alo#050050
     */
     
    /**
     * Gets all property values for the OperationAnnotationType property.<p>
     * 
     * @returns a collection of values for the OperationAnnotationType property.
     */
    OperationAnnotationType getOperationAnnotationType();


    /**
     * Adds a OperationAnnotationType property value.<p>
     * 
     * @param newOperationAnnotationType the OperationAnnotationType property value to be added
     */
    void setOperationAnnotationType(OperationAnnotationType newOperationAnnotationType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#490049
     */
     
    /**
     * Gets all property values for the StatusAnnotationType property.<p>
     * 
     * @returns a collection of values for the StatusAnnotationType property.
     */
    StatusAnnotationType getStatusAnnotationType();


    /**
     * Adds a StatusAnnotationType property value.<p>
     * 
     * @param newStatusAnnotationType the StatusAnnotationType property value to be added
     */
    void setStatusAnnotationType(StatusAnnotationType newStatusAnnotationType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#980040
     */
     
    /**
     * Gets all property values for the BitMaskValue property.<p>
     * 
     * @returns a collection of values for the BitMaskValue property.
     */
    String getBitMaskValue();


    /**
     * Adds a BitMaskValue property value.<p>
     * 
     * @param newBitMaskValue the BitMaskValue property value to be added
     */
    void setBitMaskValue(String newBitMaskValue);



}
