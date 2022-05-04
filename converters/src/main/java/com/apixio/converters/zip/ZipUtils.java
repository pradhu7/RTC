package com.apixio.converters.zip;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

	private static final int BUFFER = 2048;
	
	public static Hashtable<String, byte[]> extractPackage(InputStream zipStream) throws IOException {
		Hashtable<String, byte[]> documents = new Hashtable<String, byte[]>();
		
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipStream));
		ZipEntry entry;
		while((entry = zis.getNextEntry()) != null) {
			int count;
			byte data[] = new byte[BUFFER];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				bos.write(data, 0, count);
			}
			bos.flush();

			String documentName = entry.getName();
			byte[] documentBytes = bos.toByteArray();
			documents.put(documentName, documentBytes);
		}	
		zis.close();
			
		return documents;
	}
	
	

	// given a list of files, try to simulate an zipped package.
	public static InputStream createPackage(Map<String,byte[]> files) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream os = new ZipOutputStream(bos);
		for(Map.Entry<String,byte[]> e : files.entrySet()) {
			ZipEntry entry = new ZipEntry(e.getKey());
			os.putNextEntry(entry);
			os.write(e.getValue(),0,e.getValue().length);
		}
		os.close();
		bos.close();
		return new ByteArrayInputStream(bos.toByteArray());
	}
}
