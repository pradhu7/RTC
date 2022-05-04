package com.apixio.converters.pdf;

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFUtils {
	
	public static String getTextFromPDF(InputStream is) throws Exception 
	{
		//logger.info("Reading pdf bytes");
		StringWriter sw = new StringWriter();
		PDDocument doc = null;
		try 
		{
			doc = PDDocument.load(is);
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(1);
			stripper.setEndPage(Integer.MAX_VALUE);
			stripper.writeText(doc, sw);
			doc.close();
		} 
		catch (Exception e) 
		{
			//logger.error("Not able to read PDF.",e);
		} 
		finally 
		{
			if (doc != null) 
			{
				doc.close();
			}
		}
		
		return sw.toString();
	}
}
