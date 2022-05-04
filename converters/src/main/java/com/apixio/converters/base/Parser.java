package com.apixio.converters.base;

import com.apixio.model.patient.ExternalID;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.model.patient.Patient;

public abstract class Parser {

//	private void populateParsingDetails() {}
//	private void populatePrimarySource() {}
//	private void populatePatient() throws Exception {}
	/**
	 * Document name for reference. (It gets set in metadata of the parsing details of this parser)
	 */
	protected String sourceDocumentName;

    protected String primaryAssignAuthority;

	public abstract void parse(InputStream document, CatalogEntry catalogEntry) throws Exception;
	public abstract Patient getPatient() throws Exception;
	
	
	// we add another method here, primarily for parsing population details -
	// when called from a regular Parser that doesn't handle population files
	// simply returns an iterator with a single object.
	// typically this method should only be used when parsing popluation style
	// files.
	public Iterable<Patient> getPatients() throws Exception {
		List<Patient> patients = new LinkedList<Patient>();
		return patients;
	}

	public String getSourceDocumentName() {
		return sourceDocumentName;
	}

	public void setSourceDocumentName(String sourceDocumentName) {
		this.sourceDocumentName = sourceDocumentName;
	}

    public String getPrimaryAssignAuthority() {
   		return primaryAssignAuthority;
   	}

   	public void setPrimaryAssignAuthority(String primaryAssignAuthority) {
   		this.primaryAssignAuthority = primaryAssignAuthority;
   	}

    protected ExternalID getPrimaryExternalId(Iterable<ExternalID> externalIDs) throws Exception
    {
        if (primaryAssignAuthority == null)
            throw new Exception("No primaryAssignAuthority.");

        Iterator<ExternalID> iterators = (externalIDs == null) ? null : externalIDs.iterator();
        if (iterators == null)
            throw new Exception("No externalIDs.");

        ExternalID primaryExternalId = null;

        while (iterators.hasNext())
        {
            ExternalID externalID = iterators.next();

            if (externalID.getAssignAuthority().equals(primaryAssignAuthority))
            {
                if (primaryExternalId != null)
                    throw new Exception("Multiple external IDs match the primaryAssignAuthority.");

                primaryExternalId =  externalID;
            }
        }

        if (primaryExternalId == null)
            throw new Exception("No primaryExternalId.");

        return primaryExternalId;
    }
}
