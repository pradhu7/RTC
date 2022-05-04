package com.apixio.converters.ccda.parser;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.eclipse.mdht.uml.cda.util.CDAUtil.ValidationHandler;
import org.eclipse.mdht.uml.hl7.datatypes.util.DatatypesUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ApixioValidationHandler implements ValidationHandler {


	private Set<String> errors = new HashSet<String>();

	private DatatypesUtil.ValidationStatistics validationStatistics = null;
	private boolean captureValidationStatistics = false;

	public void handleError(Diagnostic diagnostic) {

		if (diagnostic.getSource() == "javax.xml.validation.Validator") {
//			System.out.println("XML Validation Error");
		} else if (diagnostic.getSource() == "org.eclipse.emf.common") {
//			System.out.println("Model Conformance Error - Common");
		} else if (diagnostic.getSource() == "org.eclipse.emf.ecore"){
//			System.out.println("Model Conformance Error - Ecore");
		} else {
			System.out.println("Error: " + diagnostic.toString());
		}

		errors.add("Level " + diagnostic.getSeverity() + ":  "+ diagnostic.getMessage() + "(" + diagnostic.getSource() + ")");
	}

	public void handleInfo(Diagnostic arg0) {
		System.out.println("Info: " + arg0.getMessage());
		
	}

	public void handleWarning(Diagnostic arg0) {
		System.out.println("Warning: " + arg0.getMessage());
		
	}
	public void shouldCaptureValidationStatistics(boolean captureValidationStatistics) {
		this.captureValidationStatistics = captureValidationStatistics;
	}

	@Override
	public void setValidationStatistics(DatatypesUtil.ValidationStatistics validationStatistics) {
		this.validationStatistics = validationStatistics;
	}

	@Override
	public boolean isCaptureValidationStatistics() {
		return captureValidationStatistics;
	}

	@Override
	public DatatypesUtil.ValidationStatistics getValidationStatistics() {
		return this.validationStatistics;
	}

	public Set<String> getErrors() {
		return this.errors;
	}

}
