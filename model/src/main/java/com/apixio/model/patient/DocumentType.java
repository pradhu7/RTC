package com.apixio.model.patient;

public enum DocumentType {
	STRUCTURED_DOCUMENT, 	// items like CCD, CCR, blue button, etc..
	NARRATIVE,				// items like office notes and other electronic documents containing narrative
	ENCOUNTER_NOTE,			// any documentation that is generated for the encounter
	IMAGE_OR_SCANNED,		// any image or scanned document.
}