package com.apixio.converters.raps;

import java.io.InputStream;

import com.apixio.converters.base.Parser;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.model.patient.Patient;

public class RAPSParser extends Parser {
	
	StateFullRAPSParser srapsParser;

	@Override
	public void parse(InputStream rapsDocument, CatalogEntry catalogEntry)
			throws Exception {
		srapsParser = new StateFullRAPSParser(rapsDocument);
	}
	
	@Override
	public Patient getPatient() throws Exception {
		throw new UnsupportedOperationException("Cannot call getPatient() on " + this.getClass().getCanonicalName() + ". Try getAllPatients() method instead.");
	}	
	
	@Override
	public Iterable<Patient> getPatients() throws Exception {
		return srapsParser;
	}
}
