package com.apixio.model.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.apixio.model.file.catalog.jaxb.utility.CatalogUtility;
import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;

public class APOPackagedFile extends ApxPackagedFile {

	protected Patient patient;
	PatientJSONParser patientParser = new PatientJSONParser();
	
	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	public APOPackagedFile(ApxPackagedFile apxPackage) throws Exception
	{
		this.fileContent = apxPackage.fileContent;
		this.catalog = apxPackage.catalog;
		this.catalogFileName = apxPackage.catalogFileName;
		this.fileName = apxPackage.fileName;
		this.metadata = apxPackage.metadata;
		this.metadataFileName = apxPackage.metadataFileName;
	}
	
	public APOPackagedFile(Map<String, byte[]> input) throws Exception
	{
		super();
		parse(input);
	}

	public Map<String, byte[]> serialize() throws IOException, JAXBException
	{
		//It is important to keep the files in insert order...
		Map<String, byte[]> plainFiles = new LinkedHashMap<>();

		//put version file
		plainFiles.put(Constants.VERSION_INFORMATION_FILENAME, versionString.getBytes());

		if(plainFiles != null && patient != null)
		{
			String patientStr = patientParser.toJSON(patient);
			plainFiles.put("patient"+Constants.APO_FILE_EXT, patientStr.getBytes("UTF-8"));
		}

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

    protected void parse(Map<String, byte[]> input) throws JAXBException, IOException
    {
        //Create a shallow copy, so we can filter out all entries handled by this subclass
        Map<String, byte[]> filteredInput = new HashMap<>(input);

        for(String fileName : input.keySet())
        {
            if (fileName!=null && fileName.toLowerCase().endsWith(Constants.APO_FILE_EXT))
            {
                PatientJSONParser patientParser = new PatientJSONParser();
                String patientJSON = new String(input.get(fileName), "UTF-8");
                patient = patientParser.parsePatientData(patientJSON);

                //Now that we've dealt with the patient parsing, we can filter it so it won't
                //impact superclass parsing logic
                filteredInput.remove(fileName);
                break;
            }
        }

        //allow the super class to continue parsing...
        super.parse(filteredInput);
    }
}
