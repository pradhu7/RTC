package com.apixio.model.file;

import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.apixio.model.file.catalog.jaxb.utility.CatalogUtility.convertToObject;
import static org.apache.commons.io.IOUtils.toByteArray;

/**
 * Created by dyee on 2/14/17.
 */
public class APOPackagedStream extends ApxPackagedStream implements Closeable{
    protected Patient patient;
    PatientJSONParser patientParser = new PatientJSONParser();

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public APOPackagedStream(){
    }

    public APOPackagedStream(ApxPackagedStream apxPackage) throws Exception
    {
        this.fileContent = apxPackage.getFileContent();
        this.catalog = apxPackage.getCatalog();
        this.catalogFileName = apxPackage.getCatalogFileName();
        this.fileName = apxPackage.getFileName();
        this.fileContentHash = apxPackage.getFileContentHash();
        this.metadata = apxPackage.getMetadata();
        this.metadataFileName = apxPackage.getMetadataFileName();
    }

    public APOPackagedStream(ZipInputStream inputStream) throws Exception
    {
        ZipEntry zipEntry = inputStream.getNextEntry();

        boolean isStreamable = true;

        // For version 1, we do not need to check the version content.. since if this file exists as
        // the first element - we know that it is streamable...
        //
        if(zipEntry==null || (zipEntry.getName()!=null  && !zipEntry.getName().equals(Constants.VERSION_INFORMATION_FILENAME))) {
            isStreamable = false;
        } else {
            zipEntry = inputStream.getNextEntry();
        }

        while (zipEntry != null)
        {
            if (zipEntry.getName()!=null && zipEntry.getName().startsWith(Constants.VERSION_INFORMATION_PREFIX))
            { // It is a version file
                this.versionString = new String(toByteArray(inputStream), "UTF-8");
            }
            else if (zipEntry.getName()!=null && zipEntry.getName().toLowerCase().endsWith(Constants.APO_FILE_EXT))
            {
                PatientJSONParser patientParser = new PatientJSONParser();
                String patientJSON = new String(toByteArray(inputStream), "UTF-8");
                patient = patientParser.parsePatientData(patientJSON);
            }
            else if (zipEntry.getName()!=null && zipEntry.getName().toLowerCase().endsWith(Constants.CATALOG_FILE_EXT))
            { // It is a catalog file
                this.catalogFileName = zipEntry.getName();
                this.catalog = convertToObject(new ByteArrayInputStream(toByteArray(inputStream)));
            }
            else if (zipEntry.getName()!=null && zipEntry.getName().toLowerCase().endsWith(Constants.METADATA_FILE_EXT))
            {
                metadata = new Properties(System.getProperties());
                metadata.load(inputStream);
                this.metadataFileName = zipEntry.getName();
            }
            else
            {
                handleDocument(inputStream, zipEntry, isStreamable);

                //if we are streamable, this is the last entry in the zipfile,
                //we need to break here - since inputStream.getNextEntry()
                //will cause the input stream to the document to be reset.
                if(isStreamable) break;
            }
            zipEntry = inputStream.getNextEntry();
        }
    }

    public void serialize(OutputStream outputStream) throws IOException, JAXBException, ExecutionException, InterruptedException
    {
        final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        try
        {
            addVersionInformation(zipOutputStream);

            addPatient(zipOutputStream);

            addCatalogFile(zipOutputStream);
            addMetaData(zipOutputStream);
            addDocument(zipOutputStream);
        } finally
        {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    protected void addPatient(ZipOutputStream zipOutputStream) throws IOException
    {
        zipOutputStream.putNextEntry(new ZipEntry("patient"+Constants.APO_FILE_EXT));
        String patientStr = patientParser.toJSON(patient);
        zipOutputStream.write(patientStr.getBytes("UTF-8"));
        zipOutputStream.closeEntry();
    }

    public void close() throws IOException
    {
        super.close();
    }

    protected void finalize() throws IOException {
        try {
            this.close();
        } catch (Throwable ex) {
            //ignore
        }
    }

    public static class ResettableApoPackagedStream extends APOPackagedStream
    {
        APOPackagedStream apoPackagedStream;

        public ResettableApoPackagedStream(APOPackagedStream apoPackagedStream) throws IOException
        {
            this.apoPackagedStream = apoPackagedStream;
            makeApoPackageStreamResettable();
        }

        private void makeApoPackageStreamResettable() throws IOException
        {
            if(apoPackagedStream.getFileContent() == null) return;

            //We are going to be reading the file contents multiple times - let's consume the inputstream once.
            final byte [] fileContent = org.apache.commons.io.IOUtils.toByteArray(apoPackagedStream.getFileContent());

            //now that we've consume that file content stream, make sure that inputstream is closed.
            apoPackagedStream.closeFileContent();

            apoPackagedStream.setFileContent(new ByteArrayInputStream(fileContent), true);
        }

        @Override
        public Properties getMetadata()
        {
            return apoPackagedStream.getMetadata();
        }

        @Override
        public void setMetadata(Properties metadata)
        {
            apoPackagedStream.setMetadata(metadata);
        }

        @Override
        public String getCatalogFileName()
        {
            return apoPackagedStream.getCatalogFileName();
        }

        @Override
        public void setCatalogFileName(String catalogFileName)
        {
            apoPackagedStream.setCatalogFileName(catalogFileName);
        }

        @Override
        public String getFileName()
        {
            return apoPackagedStream.getFileName();
        }

        @Override
        public void setFileName(String fileName)
        {
            apoPackagedStream.setFileName(fileName);
        }

        @Override
        public String getMetadataFileName()
        {
            return apoPackagedStream.getMetadataFileName();
        }

        @Override
        public void setMetadataFileName(String metadataFileName)
        {
            apoPackagedStream.setMetadataFileName(metadataFileName);
        }

        @Override
        public ApxCatalog getCatalog()
        {
            return apoPackagedStream.getCatalog();
        }

        @Override
        public void setCatalog(ApxCatalog catalog)
        {
            apoPackagedStream.setCatalog(catalog);
        }

        @Override
        public void serialize(OutputStream outputStream) throws IOException, JAXBException, ExecutionException, InterruptedException
        {
            apoPackagedStream.serialize(outputStream);
        }

        @Override
        protected void handleDocument(ZipInputStream inputStream, ZipEntry zipEntry, boolean isStreamable) throws IOException
        {
            apoPackagedStream.handleDocument(inputStream, zipEntry, isStreamable);
        }

        @Override
        protected void addVersionInformation(ZipOutputStream zipOutputStream) throws IOException
        {
            apoPackagedStream.addVersionInformation(zipOutputStream);
        }

        @Override
        protected void addCatalogFile(ZipOutputStream zipOutputStream) throws IOException, JAXBException
        {
            apoPackagedStream.addCatalogFile(zipOutputStream);
        }

        @Override
        protected void addMetaData(ZipOutputStream zipOutputStream) throws IOException, JAXBException
        {
            apoPackagedStream.addMetaData(zipOutputStream);
        }

        @Override
        protected void addDocument(ZipOutputStream zipOutputStream) throws IOException, ExecutionException, InterruptedException
        {
            apoPackagedStream.addDocument(zipOutputStream);
        }

        @Override
        public void setFileContent(InputStream fileContent)
        {
            apoPackagedStream.setFileContent(fileContent);
        }

        @Override
        public String getFileContentHash()
        {
            return apoPackagedStream.getFileContentHash();
        }

        @Override
        public void setCalculateHashEnable(boolean calculateHashEnable)
        {
            apoPackagedStream.setCalculateHashEnable(calculateHashEnable);
        }

        @Override
        public boolean isCalculateHashEnable()
        {
            return apoPackagedStream.isCalculateHashEnable();
        }

        @Override
        public void close() throws IOException
        {
            apoPackagedStream.close();
        }

        @Override
        public InputStream getFileContent()
        {
            try
            {
                if(apoPackagedStream.getFileContent() != null)
                {
                    apoPackagedStream.getFileContent().reset();
                }
            } catch (IOException e)
            {
                throw  new RuntimeException("Error resetting stream", e);
            }
            return apoPackagedStream.getFileContent();
        }

        protected void finalize() throws IOException {
            try {
                this.close();
            } catch (Throwable ex) {
                //ignore
            }
        }

        public Patient getPatient() {
            return apoPackagedStream.getPatient();
        }

        public void setPatient(Patient patient) {
            apoPackagedStream.setPatient(patient);
        }
    }

}
