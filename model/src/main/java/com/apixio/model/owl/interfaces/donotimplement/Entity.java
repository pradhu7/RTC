package com.apixio.model.owl.interfaces.donotimplement;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;

import java.net.URI;
import java.util.Collection;

/**
 * OWL annotation driven imports
 **/


/**
 * <p/>
 * Source Class: Entity <br>
 *
 * @version generated on Thu Mar 31 15:40:47 PDT 2016 by jctoledo
 */

public interface Entity {

/**
 * OWL annotation driven methods
 **/
    /**
     * get and set the XUUID
     */
    XUUID getInternalXUUID();

    void setInternalXUUID(XUUID anXUUID);

    /**
     * Set and get the Uri
     */
    URI getURI();

    void setURI(URI aURI);

    /**
     * OtherIds
     */
    Collection<XUUID> getOtherIds();

    void setOtherIds(Collection<XUUID> others);


    /**
     * Logical Id
     */
    String getLogicalID();

    void setLogicalID(String anID);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000146
     */

    /**
     * Gets all property values for the LastEditDate property.<p>
     *
     * @returns a collection of values for the LastEditDate property.
     */
    Date getLastEditDate();


    /**
     * Adds a LastEditDate property value.<p>
     *
     * @param newLastEditDate the LastEditDate property value to be added
     */
    void setLastEditDate(Date newLastEditDate);


}
