package com.apixio.model.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.bind.JAXBException;

import com.apixio.model.file.catalog.jaxb.utility.CatalogUtility;

public class ApxPackagedFile extends ApxPackagedBase {
	
	protected byte[] fileContent;
	
	public ApxPackagedFile(Map<String, byte[]> input) throws JAXBException, IOException 
	{
		super();

		parse(input);
	}
	
	public ApxPackagedFile() {
	}

	public Map<String, byte[]> serialize() throws IOException, JAXBException 
	{
		//It is important to keep the files in insert order...
		Map<String, byte[]> plainFiles = new LinkedHashMap<>();

		//put version file
		plainFiles.put(Constants.VERSION_INFORMATION_FILENAME, versionString.getBytes());

		//put catalog file
		plainFiles.put(this.catalogFileName, CatalogUtility.convertToBytes(this.catalog));

		//put metadata..
		if (metadata!=null)
		{
			ByteArrayOutputStream metadataOS = new ByteArrayOutputStream();
			metadata.store(metadataOS, "");
			
			if (metadataOS!=null)
			{
				if (this.metadataFileName!=null)
					plainFiles.put(this.metadataFileName, metadataOS.toByteArray());
				else
					plainFiles.put("patientmetadata"+Constants.METADATA_FILE_EXT, metadataOS.toByteArray());
			}
		}

		//Put file content
		if (fileName != null && fileContent != null)
			plainFiles.put(this.fileName, fileContent);

		return plainFiles;
	}

	public byte[] getFileContent()
	{
		return fileContent;
	}

	public void setFileContent(byte[] fileContent)
	{
		this.fileContent = fileContent;
	}

    protected void parse(Map<String, byte[]> input) throws JAXBException, IOException
    {
        for (String fileName : input.keySet())
        {
            if (fileName!=null && fileName.startsWith(Constants.VERSION_INFORMATION_PREFIX))
            { // It is a version file
                this.versionString = new String(input.get(fileName));
            }
            else if (fileName!=null && fileName.toLowerCase().endsWith(Constants.CATALOG_FILE_EXT))
            { // It is a catalog file
                this.catalogFileName = fileName;
                this.catalog = CatalogUtility.convertToObject(new ByteArrayInputStream(input.get(fileName)));
            }
            else if (fileName!=null && fileName.toLowerCase().endsWith(Constants.METADATA_FILE_EXT))
            {
                metadata = new Properties(System.getProperties());
                metadata.load(new ByteArrayInputStream(input.get(fileName)));
                this.metadataFileName = fileName;
            }
            else
            {
                this.fileName= fileName;
                this.fileContent = input.get(fileName);
            }
        }
    }
}
