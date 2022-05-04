package com.apixio.converters.word;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.apixio.converters.base.ResourceLoader;
import com.aspose.words.HtmlSaveOptions;
import com.aspose.words.License;
import com.aspose.words.LoadFormat;
import com.aspose.words.LoadOptions;
import com.aspose.words.PdfSaveOptions;
import com.aspose.words.SaveFormat;
import com.aspose.words.SaveOptions;
import com.aspose.words.TxtSaveOptions;

public class WordConverter {
	private static License license;
	public enum ExportFormat { PDF, HTML, TEXT };

	public static byte[] convertOfficeDocument(InputStream is, String fileFormat, ExportFormat exportFormat) throws Exception {
		if (license == null)
		{
			license = new License();
			InputStream licStream = ResourceLoader.getResourceStream("Aspose.Words.Java.lic");
			if(licStream!=null)
				license.setLicense(licStream);
			else
				throw new Exception("License not found.");
		}

		LoadOptions loadOptions = new LoadOptions();
		if (fileFormat.equalsIgnoreCase("RTF"))
			loadOptions.setLoadFormat(LoadFormat.RTF);
		else if (fileFormat.equalsIgnoreCase("ODT"))
			loadOptions.setLoadFormat(LoadFormat.ODT);
		else
			loadOptions.setLoadFormat(LoadFormat.AUTO);
		
		com.aspose.words.Document doc = new com.aspose.words.Document(is, loadOptions);
	
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		SaveOptions options = null;
		if (exportFormat.equals(ExportFormat.PDF)) {
			options = new PdfSaveOptions();
		} else if (exportFormat.equals(ExportFormat.HTML)) {
			HtmlSaveOptions htmlOptions = new HtmlSaveOptions(SaveFormat.HTML);
			
			String tempDir = System.getProperty("java.io.tmpdir");
			htmlOptions.setEncoding(Charset.defaultCharset());
			htmlOptions.setExportImagesAsBase64(true);
			htmlOptions.setImagesFolder(tempDir);
			options = htmlOptions;
		} else if(exportFormat.equals(ExportFormat.TEXT)) {
			options = new TxtSaveOptions();
			options.setSaveFormat(SaveFormat.TEXT);
		}
		//options.set
		doc.save(bos, options);
		return bos.toByteArray();
	}
}
