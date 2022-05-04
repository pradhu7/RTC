package com.apixio.converters.ccda.parser;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.eclipse.mdht.uml.cda.util.CDAUtil.LoadHandler;


public class ApixioLoadHandler implements LoadHandler {
	private Logger log = Logger.getLogger(ApixioLoadHandler.class.getName());

	public boolean handleUnknownFeature(EObject arg0, EStructuralFeature arg1, Object arg2) {
		String path = CDAUtil.getPath(arg0);
		log.info("handleUnknownFeature: " + path + " - " + arg1.toString() + "(" + arg2.toString() + ")");
		return false;
	}

}
