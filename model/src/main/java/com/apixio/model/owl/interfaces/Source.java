package com.apixio.model.owl.interfaces;

/**
 OWL annotation driven imports 
**/
import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.donotimplement.Entity;
import com.apixio.model.owl.interfaces.donotimplement.Provenance;
import com.apixio.model.owl.interfaces.donotimplement.Human;
import com.apixio.model.owl.interfaces.donotimplement.SourceType;

import java.util.Collection;


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: Source <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface Source  extends Provenance {


    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000090
     */

    /**
     * Gets all property values for the OriginalID property.<p>
     *
     *
     * @returns a collection of values for the OriginalID property.
     */
    ExternalIdentifier getOriginalID();


    /**
     * Adds a OriginalID property value.<p>
     *
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
     *
     */
    Collection<ExternalIdentifier> getOtherOriginalIDss();

    /**
     * Add an element to the list
     *
     */
    Collection<ExternalIdentifier> addOtherOriginalIDss(ExternalIdentifier anItem);

    /**
     * get element from list by Id
     *
     */
    ExternalIdentifier getOtherOriginalIDssById(XUUID anID);

    /**
     * Adds a OtherOriginalIDss property value.<p>
     *
     * @param newOtherOriginalIDss the OtherOriginalIDss property value to be added
     * void setOtherOriginalIDss(Collection<? extends ExternalIdentifier> newOtherOriginalIDss);
     *
     */
    void setOtherOriginalIDss(Collection<ExternalIdentifier> newOtherOriginalIDss);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000166
     */

    /**
     * Gets all property values for the SourceAuthor property.<p>
     *
     *
     * @returns a collection of values for the SourceAuthor property.
     */
    Human getSourceAuthor();


    /**
     * Adds a SourceAuthor property value.<p>
     *
     *
     * @param newSourceAuthor the SourceAuthor property value to be added
     */
    void setSourceAuthor(Human newSourceAuthor);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000167
     */

    /**
     * Gets all property values for the SourceFile property.<p>
     *
     *
     * @returns a collection of values for the SourceFile property.
     */
    File getSourceFile();


    /**
     * Adds a SourceFile property value.<p>
     *
     *
     * @param newSourceFile the SourceFile property value to be added
     */
    void setSourceFile(File newSourceFile);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000194
     */

    /**
     * Gets all property values for the SourceType property.<p>
     *
     *
     * @returns a collection of values for the SourceType property.
     */
    SourceType getSourceType();


    /**
     * Adds a SourceType property value.<p>
     *
     *
     * @param newSourceType the SourceType property value to be added
     */
    void setSourceType(SourceType newSourceType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000197
     */

    /**
     * Gets all property values for the RefersToInternalObject property.<p>
     *
     *
     * @returns a collection of values for the RefersToInternalObject property.
     */
    Entity getRefersToInternalObject();


    /**
     * Adds a RefersToInternalObject property value.<p>
     *
     *
     * @param newRefersToInternalObject the RefersToInternalObject property value to be added
     */
    void setRefersToInternalObject(Entity newRefersToInternalObject);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000204
     */

    /**
     * Gets all property values for the SourceOrganization property.<p>
     *
     *
     * @returns a collection of values for the SourceOrganization property.
     */
    Organization getSourceOrganization();


    /**
     * Adds a SourceOrganization property value.<p>
     *
     *
     * @param newSourceOrganization the SourceOrganization property value to be added
     */
    void setSourceOrganization(Organization newSourceOrganization);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#151151
     */

    /**
     * Gets all property values for the SourceCreationDate property.<p>
     *
     *
     * @returns a collection of values for the SourceCreationDate property.
     */
    Date getSourceCreationDate();


    /**
     * Adds a SourceCreationDate property value.<p>
     *
     *
     * @param newSourceCreationDate the SourceCreationDate property value to be added
     */
    void setSourceCreationDate(Date newSourceCreationDate);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000184
     */

    /**
     * Gets all property values for the LineNumber property.<p>
     *
     *
     * @returns a collection of values for the LineNumber property.
     */
    Integer getLineNumber();


    /**
     * Adds a LineNumber property value.<p>
     *
     *
     * @param newLineNumber the LineNumber property value to be added
     */
    void setLineNumber(Integer newLineNumber);


}
