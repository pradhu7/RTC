package com.apixio.converters.html.creator;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

import com.apixio.converters.pdf.PDFUtils;
import com.apixio.converters.text.extractor.TextExtractionUtils;
import com.apixio.converters.word.WordConverter;
import com.apixio.converters.word.WordConverter.ExportFormat;

public class HTMLUtils {

	public static String convertXmlToHtml(StreamSource xslStreamSource, InputStream xmlStream) {
		ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
		try {

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(xslStreamSource);
			StreamSource xmlStreamSource = new StreamSource(xmlStream);
			transformer.transform(xmlStreamSource, new StreamResult(outputBytes));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return outputBytes.toString();
	}

	public static String convertXmlToHtml(InputStream xslStream, InputStream xmlStream, String resourcePath) {
		ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
		try {

			TransformerFactory factory = TransformerFactory.newInstance();
			StreamSource xslStreamSource = new StreamSource(xslStream);

			if (resourcePath != null && !resourcePath.equals("")) {
				xslStreamSource.setSystemId(resourcePath);
			}
			Transformer transformer = factory.newTransformer(xslStreamSource);
			StreamSource xmlStreamSource = new StreamSource(xmlStream);

			transformer.transform(xmlStreamSource, new StreamResult(outputBytes));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return outputBytes.toString();
	}

	public static String getHtmlRepresentation(InputStream is, String fileFormat) throws Exception {
		String htmlRepresentation = "";
		if (fileFormat.equalsIgnoreCase("PDF")) {
			htmlRepresentation = getHtmlFromPdf(is);
		} else if (fileFormat.equalsIgnoreCase("TXT")) {
			htmlRepresentation = getHtmlFromTxt(is);
		} else if (fileFormat.equalsIgnoreCase("HTML")) {
			htmlRepresentation = getHtmlFromStream(is);
		} else if (fileFormat.equalsIgnoreCase("DOC") || fileFormat.equalsIgnoreCase("DOCX") || fileFormat.equalsIgnoreCase("RTF")
				|| fileFormat.equalsIgnoreCase("ODT")) {
			htmlRepresentation = getHtmlFromWordDocument(is, fileFormat);
		} else {
			// what if we do nothing?
			htmlRepresentation = "";
		}
		return htmlRepresentation;
	}

	public static String getHtmlFromPdf(InputStream is) throws Exception {
		String pdfContent = TextExtractionUtils.getTextFromPdf(is);
		if (pdfContent != null && pdfContent.length() > 0)
			pdfContent = pdfContent.replaceAll("\n", "<BR>");
		return pdfContent;
	}

	public static String getHtmlFromStream(InputStream is) throws Exception {
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "UTF8");
		return  writer.toString();
	}

	public static String getHtmlFromTxt(InputStream is) throws IOException {
		String txt = TextExtractionUtils.getTextFromTxt(is);
		return "<pre>" + txt + "</pre>";
	}

	public static String getHtmlFromCDA(InputStream is) throws IOException {
		// TODO: Figure out why the 4.0.1 CDA Stylesheet gives an NPE
//		String resourcePath = this.getClass().getClassLoader().getResource(stylesheetName).toString();
//		htmlRender = HTMLUtils.convertXmlToHtml(ClassLoader.getSystemResourceAsStream(stylesheetName), new ByteArrayInputStream(docBytes), resourcePath);
//		String stylesheetName = "hl7_cda_r2_4.0.1_.xsl";
		String htmlString = IOUtils.toString(is);

		Pattern pattern = Pattern.compile("\\{\\{.*?}}");
		Matcher matchPattern = pattern.matcher(htmlString);
		while(matchPattern.find()) {
			String replacement = null;
			String match = matchPattern.group(0);
			for (String option : match.replace("{{","").replace("}}","").split("\\|")) {
				if (option.endsWith("*")) {
					replacement = option.replace("*","");
				} else if (option.endsWith("#")) {
					replacement = option.replace("#","");
				}
			}
			if (replacement != null) {
				htmlString = htmlString.replace(match,replacement);
			}
		}
		String stylesheetName = "hl7_cda_3.0_updated.xsl";
		String xsltSystemId = HTMLUtils.class.getClassLoader().getResource(stylesheetName).toExternalForm();
		StreamSource ss = new StreamSource(xsltSystemId);
		return convertXmlToHtml(ss, new ByteArrayInputStream(htmlString.getBytes()));
	}

	public static String getTxtFromHtml(InputStream is) throws Exception {
		return TextExtractionUtils.getTextFromHtml(is);
	}

	public static String getHtmlFromWordDocument(InputStream is, String originalFormat) throws Exception {
		byte[] htmlStringBytes = WordConverter.convertOfficeDocument(is, originalFormat, ExportFormat.HTML);
		String htmlString = "";
		if (htmlStringBytes != null)
			htmlString = new String(htmlStringBytes, "UTF-8");

		return htmlString;
	}

}
