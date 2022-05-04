package com.apixio.security.utility;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCompression {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(DataCompression.class);
	
	public byte[] zipFilesInMemory(Map<String,byte[]> files) throws UnsupportedEncodingException, IOException{
		byte[] zippedBytes = null;
		if(files!=null){
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ZipOutputStream os = new ZipOutputStream(bos);

			try
			{
				for (Map.Entry<String, byte[]> e : files.entrySet())
				{
					ZipEntry entry = new ZipEntry(e.getKey());
					os.putNextEntry(entry);
					os.write(e.getValue(), 0, e.getValue().length);
				}
			}
			finally
			{
				os.close();
				bos.close();
			}
			zippedBytes = bos.toByteArray();
			
			if(zippedBytes==null)
				throw new IOException("Could not get zipped bytes.");
		}		
		return zippedBytes;
	}
	
	public Map<String, byte[]> unzipFileInMemory(byte[] file) {
		Map<String, byte[]> filemap = new LinkedHashMap<>();
		try {
		    final int BUFFER = 2048;

		    CheckedInputStream checksum = new CheckedInputStream(
			    new ByteArrayInputStream(file), new Adler32());
		    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
			    checksum));
		    ZipEntry entry;
			ByteArrayOutputStream streamBuilder = null;
		    try
			{
				while ((entry = zis.getNextEntry()) != null)
				{
					int count;
					byte data[] = new byte[BUFFER];
					streamBuilder = new ByteArrayOutputStream();
					while ((count = zis.read(data, 0, BUFFER)) != -1)
					{
						streamBuilder.write(data, 0, count);
					}
					filemap.put(entry.getName(), streamBuilder.toByteArray());
					streamBuilder.close();
				}
			} finally
			{
				zis.close();
				checksum.close();
				IOUtils.closeQuietly(streamBuilder);
			}
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return filemap;
	}

}
