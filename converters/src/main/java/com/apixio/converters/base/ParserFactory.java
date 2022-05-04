package com.apixio.converters.base;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.apixio.converters.ccda.parser.CCDAParser;
import com.apixio.converters.ccr.parser.CCRParser;
import com.apixio.converters.ecw.parser.ECWParser;

public class ParserFactory {

	public static Parser getParser(InputStream is) throws Exception {
		Parser p = null;
		
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(is);
        if (doc != null) {
            // normalize text representation
            doc.getDocumentElement().normalize();
            String rootNode = doc.getDocumentElement().getNodeName();
            if (!rootNode.isEmpty()) {
                if (rootNode.equals(ConvertersConstants.ccdRootNode)) {
                    p = new CCDAParser();
                } else if (rootNode.equals(ConvertersConstants.ccrRootNode)) {
                    p = new CCRParser();
                } else if (rootNode.equals(ConvertersConstants.ecwRootNode)) {
                    p = new ECWParser();
                } else {
                    throw new Exception("Unrecognized root node.");
                }
            } else {
                throw new Exception("Unable to get the name of the root node of the document");
            }
        }
		
		return p;
	}
}
