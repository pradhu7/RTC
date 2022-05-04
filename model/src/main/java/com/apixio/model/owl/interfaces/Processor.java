package com.apixio.model.owl.interfaces;

import com.apixio.model.owl.interfaces.donotimplement.ComputationalEntity;
import com.apixio.model.owl.interfaces.donotimplement.InputDataType;
import com.apixio.model.owl.interfaces.donotimplement.ProcessorType;

/**
 * OWL annotation driven imports
 **/


/**
 * <p/>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: Processor <br>
 *
 * @version generated on Thu Mar 31 16:06:25 PDT 2016 by jctoledo
 */

public interface Processor extends ComputationalEntity {



    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000103
     */

    /**
     * Gets all property values for the ProcessorType property.<p>
     *
     *
     * @returns a collection of values for the ProcessorType property.
     */
    ProcessorType getProcessorType();


    /**
     * Adds a ProcessorType property value.<p>
     *
     *
     * @param newProcessorType the ProcessorType property value to be added
     */
    void setProcessorType(ProcessorType newProcessorType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000190
     */

    /**
     * Gets all property values for the InputDataType property.<p>
     *
     *
     * @returns a collection of values for the InputDataType property.
     */
    InputDataType getInputDataType();


    /**
     * Adds a InputDataType property value.<p>
     *
     *
     * @param newInputDataType the InputDataType property value to be added
     */
    void setInputDataType(InputDataType newInputDataType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000188
     */

    /**
     * Gets all property values for the ProcessorVersion property.<p>
     *
     *
     * @returns a collection of values for the ProcessorVersion property.
     */
    String getProcessorVersion();


    /**
     * Adds a ProcessorVersion property value.<p>
     *
     *
     * @param newProcessorVersion the ProcessorVersion property value to be added
     */
    void setProcessorVersion(String newProcessorVersion);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000189
     */

    /**
     * Gets all property values for the ProcessorName property.<p>
     *
     *
     * @returns a collection of values for the ProcessorName property.
     */
    String getProcessorName();


    /**
     * Adds a ProcessorName property value.<p>
     *
     *
     * @param newProcessorName the ProcessorName property value to be added
     */
    void setProcessorName(String newProcessorName);


}
