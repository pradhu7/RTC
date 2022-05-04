package com.apixio.converters.text.extractor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import com.apixio.converters.html.creator.HTMLUtils;
import org.apache.commons.io.IOUtils;

import com.apixio.converters.pdf.PDFUtils;
import com.apixio.converters.word.WordConverter;
import com.apixio.converters.word.WordConverter.ExportFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

public class TextExtractionUtils {
	
	public static String extractText(InputStream is, String fileFormat) throws Exception 
	{
		String text = "";
		if (fileFormat.equalsIgnoreCase("PDF")) {
			text = getTextFromPdf(is);
		} else if (fileFormat.equalsIgnoreCase("TXT")) {
			text = getTextFromTxt(is);
		} else if (fileFormat.equalsIgnoreCase("DOC") || fileFormat.equalsIgnoreCase("DOCX") || fileFormat.equalsIgnoreCase("RTF")
				|| fileFormat.equalsIgnoreCase("ODT")) {
			text = getTextFromWordDocument(is, fileFormat);
		} else if (fileFormat.equalsIgnoreCase("CCD") || fileFormat.equalsIgnoreCase("CCDA")) {
			text = getTextFromCDA(is);
		} else if (fileFormat.equalsIgnoreCase("HTML")) {
			text = getTextFromHtml(is);
		}  else {
			// what if we do nothing?
			text = "";
		}
		return text;
	}
	
	public static String getTextFromPdf(InputStream is) throws Exception {
		String pdfContent = PDFUtils.getTextFromPDF(is);
		return pdfContent;
	}
	
	public static String getTextFromTxt(InputStream is) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "UTF8");
		return  writer.toString();
	}

	// if the original html contains newline(\n), it gets preserved
	// if the original html contains br or p tags, they gets translated to newline(\n).
	// add a space after spans to separate words
	public static String getTextFromHtml(InputStream is) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "UTF8");
		Document document = Jsoup.parse(writer.toString());
		document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
		document.select("br").append("\\n");
		document.select("p").prepend("\\n");
		document.select("span").append(" ");
		document.select("td").append("\\t");
		String s = document.html().replaceAll("\\\\n", "\n").replaceAll("\\\\t", "\t");
		return Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
	}
	
	public static String getTextFromWordDocument(InputStream is, String originalFormat) throws Exception {
		byte[] textBytes = WordConverter.convertOfficeDocument(is, originalFormat, ExportFormat.TEXT);
		String textString = "";
		if (textBytes != null)
			textString = new String(textBytes, "UTF-8");

		return textString;
	}

	public static String getTextFromCDA(InputStream is) throws IOException {
		String cdaHtml = HTMLUtils.getHtmlFromCDA(is);
		return getTextFromHtml(new ByteArrayInputStream(cdaHtml.getBytes()));
	}

}
