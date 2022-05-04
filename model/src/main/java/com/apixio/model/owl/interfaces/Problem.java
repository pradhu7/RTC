package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/
import com.apixio.model.owl.interfaces.donotimplement.*;

/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: Problem <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface Problem  extends CodedEntity, Observation {


   /* ***************************************************
     * Property http://apixio.com/ontology/alo#000001
     */

    /**
     * Gets all property values for the ProblemResolutionDate property.<p>
     *
     * @returns a collection of values for the ProblemResolutionDate property.
     */
    Date getProblemResolutionDate();


    /**
     * Adds a ProblemResolutionDate property value.<p>
     *
     * @param newProblemResolutionDate the ProblemResolutionDate property value to be added
     */
    void setProblemResolutionDate(Date newProblemResolutionDate);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000025
     */

    /**
     * Gets all property values for the ResolutionStatusType property.<p>
     *
     * @returns a collection of values for the ResolutionStatusType property.
     */
    ResolutionStatusType getResolutionStatusType();


    /**
     * Adds a ResolutionStatusType property value.<p>
     *
     * @param newResolutionStatusType the ResolutionStatusType property value to be added
     */
    void setResolutionStatusType(ResolutionStatusType newResolutionStatusType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000468
     */

    /**
     * Gets all property values for the ProblemDateRange property.<p>
     *
     * @returns a collection of values for the ProblemDateRange property.
     */
    DateRange getProblemDateRange();


    /**
     * Adds a ProblemDateRange property value.<p>
     *
     * @param newProblemDateRange the ProblemDateRange property value to be added
     */
    void setProblemDateRange(DateRange newProblemDateRange);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000026
     */

    /**
     * Gets all property values for the ProblemTemporalStatus property.<p>
     *
     * @returns a collection of values for the ProblemTemporalStatus property.
     */
    String getProblemTemporalStatus();


    /**
     * Adds a ProblemTemporalStatus property value.<p>
     *
     * @param newProblemTemporalStatus the ProblemTemporalStatus property value to be added
     */
    void setProblemTemporalStatus(String newProblemTemporalStatus);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000466
     */

    /**
     * Gets all property values for the ProblemName property.<p>
     *
     * @returns a collection of values for the ProblemName property.
     */
    String getProblemName();


    /**
     * Adds a ProblemName property value.<p>
     *
     * @param newProblemName the ProblemName property value to be added
     */
    void setProblemName(String newProblemName);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000471
     */

    /**
     * Gets all property values for the ProblemSeverity property.<p>
     *
     * @returns a collection of values for the ProblemSeverity property.
     */
    String getProblemSeverity();


    /**
     * Adds a ProblemSeverity property value.<p>
     *
     * @param newProblemSeverity the ProblemSeverity property value to be added
     */
    void setProblemSeverity(String newProblemSeverity);



}
