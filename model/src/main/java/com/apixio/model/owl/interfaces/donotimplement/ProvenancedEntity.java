package com.apixio.model.owl.interfaces.donotimplement;

import java.util.Collection;

/**
 OWL annotation driven imports 
 **/
import com.apixio.model.owl.interfaces.AmendmentAnnotation;
import com.apixio.model.owl.interfaces.DataProcessingDetail;
import com.apixio.model.owl.interfaces.ExternalIdentifier;
import org.joda.time.DateTime;

import java.net.URI;

import com.apixio.XUUID;


/**
 * <p/>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: ProvenancedEntity <br>
 *
 * @version generated on Wed Mar 23 15:56:35 PDT 2016 by jctoledo
 */

public interface ProvenancedEntity extends InformationalQuality {

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000090
     */

    /**
     * Gets all property values for the OriginalID property.<p>
     *
     * @returns a collection of values for the OriginalID property.
     */
    ExternalIdentifier getOriginalID();


    /**
     * Adds a OriginalID property value.<p>
     *
     * @param newOriginalID the OriginalID property value to be added
     */
    void setOriginalID(ExternalIdentifier newOriginalID);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000092
     */

    /**
     * Gets all property values for the OtherOriginalIDss property.<p>
     *
     * @returns a collection of values for the OtherOriginalIDss property.
     * Collection<? extends ExternalIdentifier> getOtherOriginalIDss();
     */
    Collection<ExternalIdentifier> getOtherOriginalIDss();

    /**
     * Add an element to the list
     */
    Collection<ExternalIdentifier> addOtherOriginalIDss(ExternalIdentifier anItem);

    /**
     * get element from list by Id
     */
    ExternalIdentifier getOtherOriginalIDssById(XUUID anID);

    /**
     * Adds a OtherOriginalIDss property value.<p>
     *
     * @param newOtherOriginalIDss the OtherOriginalIDss property value to be added
     * void setOtherOriginalIDss(Collection<? extends ExternalIdentifier> newOtherOriginalIDss);
     */
    void setOtherOriginalIDss(Collection<ExternalIdentifier> newOtherOriginalIDss);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000098
     */

    /**
     * Gets all property values for the DataProcessingDetails property.<p>
     *
     * @returns a collection of values for the DataProcessingDetails property.
     * Collection<? extends DataProcessingDetail> getDataProcessingDetails();
     */
    Collection<DataProcessingDetail> getDataProcessingDetails();

    /**
     * Add an element to the list
     */
    Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem);

    /**
     * get element from list by Id
     */
    DataProcessingDetail getDataProcessingDetailsById(XUUID anID);

    /**
     * Adds a DataProcessingDetails property value.<p>
     *
     * @param newDataProcessingDetails the DataProcessingDetails property value to be added
     * void setDataProcessingDetails(Collection<? extends DataProcessingDetail> newDataProcessingDetails);
     */
    void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#044044
     */

    /**
     * Gets all property values for the AmendmentAnnotations property.<p>
     *
     * @returns a collection of values for the AmendmentAnnotations property.
     * Collection<? extends AmendmentAnnotation> getAmendmentAnnotations();
     */
    Collection<AmendmentAnnotation> getAmendmentAnnotations();

    /**
     * Add an element to the list
     */
    Collection<AmendmentAnnotation> addAmendmentAnnotations(AmendmentAnnotation anItem);

    /**
     * get element from list by Id
     */
    AmendmentAnnotation getAmendmentAnnotationsById(XUUID anID);

    /**
     * Adds a AmendmentAnnotations property value.<p>
     *
     * @param newAmendmentAnnotations the AmendmentAnnotations property value to be added
     * void setAmendmentAnnotations(Collection<? extends AmendmentAnnotation> newAmendmentAnnotations);
     */
    void setAmendmentAnnotations(Collection<AmendmentAnnotation> newAmendmentAnnotations);


}
