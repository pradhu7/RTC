/**
 * 
 */
package com.apixio.converters.xml.creator;

import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import com.apixio.model.ocr.page.DocumentMetaData;
import com.apixio.model.ocr.page.ObjectFactory;
import com.apixio.model.ocr.page.Ocrextraction;
import com.apixio.model.ocr.page.Page;
import com.apixio.converters.html.creator.HTMLUtils;

/**
 * To wrap the html representation of extracted text into xml
 * 
 * @author gaurav
 * 
 */
public class XMLUtils {

	private static final Logger log = Logger.getLogger(XMLUtils.class);

	public static String getXMLRepresentation(InputStream is, String fileFormat) throws Exception {
		List<Page> ocrPageList = new ArrayList<Page>();
		String htmlRepresentation = "";
		if (fileFormat.equalsIgnoreCase("PDF")) {
			htmlRepresentation = HTMLUtils.getHtmlFromPdf(is);
		} else if (fileFormat.equalsIgnoreCase("TXT")) {
			htmlRepresentation = HTMLUtils.getHtmlFromTxt(is);
		} else if (fileFormat.equalsIgnoreCase("HTML")) {
			htmlRepresentation = HTMLUtils.getHtmlFromStream(is);
		} else if (fileFormat.equalsIgnoreCase("DOC") || fileFormat.equalsIgnoreCase("DOCX") || fileFormat.equalsIgnoreCase("RTF") || fileFormat.equalsIgnoreCase("ODT")) {
			htmlRepresentation = HTMLUtils.getHtmlFromWordDocument(is, fileFormat);
		}  else if (fileFormat.equalsIgnoreCase("CCD") || fileFormat.equalsIgnoreCase("CCDA")) {
			htmlRepresentation = HTMLUtils.getHtmlFromCDA(is);
		} else {
			// what if we do nothing?
			htmlRepresentation = "";
		}
		Page page = new Page();
		page.setPageNumber(BigInteger.valueOf(1L));
		page.setImgType(fileFormat);
		ObjectFactory factory = new ObjectFactory();
		log.info("Setting text extracted with replacing invalid and non-UTF characters including <BF><EF><BD> by empty string...");
		page.setPlainText(factory.createPagePlainText(htmlRepresentation.replaceAll("<BF>|<EF>|<BD>|[[\\uFFF9][\\uFFFA][\\uFFFB][\\uFFFC][\\uFFFD][^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]]", "")));
		ocrPageList.add(page);
		return getXMLContent(ocrPageList, 1L);
	}

	public static String getXMLContent(List<Page> ocrPageList, long pageCount) {

		String xmlString = "";
		try {
			// create ocr result object
			Ocrextraction ocrObj = new Ocrextraction();
			ocrObj.setMetadata(new DocumentMetaData());
			Ocrextraction.Pages pages = new Ocrextraction.Pages();
			pages.setNumPages(BigInteger.valueOf(pageCount));
			ocrObj.setPages(pages);
			if (ocrPageList != null) {
				for (Page page : ocrPageList) {
					pages.getPage().add(page);
				}
			}
			// marshall java class to xml string
			JAXBContext contextObj = JAXBContext.newInstance(Ocrextraction.class);
			Marshaller marshallerObj = contextObj.createMarshaller();
			marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter sw = new StringWriter();
			marshallerObj.marshal(ocrObj, sw);
			xmlString += sw.toString();
			sw.close();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return xmlString;
	}
}
