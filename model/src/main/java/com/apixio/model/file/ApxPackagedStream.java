package com.apixio.model.file;

import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.file.catalog.jaxb.utility.CatalogUtility;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

public class ApxPackagedStream extends ApxPackagedBase implements Closeable{

    //underlining stream
    private ZipInputStream zipInputStream;

    // this will be Sha1DigestInputStream if we're calculating SHA1 on the contents
    protected InputStream fileContent;

    protected String fileContentHash = null;

    //By default, we will calculate the hash value for the fileContent. Calculating the has has non trival costs
    //so, if we don't require it.. we can disable it... and save some CPU cycles.
    private boolean calculateHashEnable = true;

    public ApxPackagedStream() {
    }

    public ApxPackagedStream(ZipInputStream inputStream) throws JAXBException, IOException
    {
        super();

        this.zipInputStream = inputStream;

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

        boolean foundCatalogFile = false;

        while (zipEntry != null)
        {
            if (zipEntry.getName()!=null && zipEntry.getName().startsWith(Constants.VERSION_INFORMATION_PREFIX))
            { // It is a version file
                this.versionString = new String(toByteArray(inputStream), "UTF-8");
            }
            else if (zipEntry.getName()!=null && zipEntry.getName().toLowerCase().endsWith(Constants.CATALOG_FILE_EXT))
            { // It is a catalog file
                this.catalogFileName = zipEntry.getName();
                this.catalog = convertToObject(new ByteArrayInputStream(toByteArray(inputStream)));
                foundCatalogFile = true;
                System.out.println("Got catalog " + catalogFileName);
            }
            else if (zipEntry.getName()!=null && zipEntry.getName().toLowerCase().endsWith(Constants.METADATA_FILE_EXT))
            {
                metadata = new Properties(System.getProperties());
                metadata.load(inputStream);
                this.metadataFileName = zipEntry.getName();

                System.out.println("Got metadata " + metadataFileName);
            }
            else
            {
                //ASSUMPTION MADE: from examining the source code, we only ever make use of the catalogFile
                //                 to determine mimeType.. we don't always create or use metadata, so
                //                 we'll stream if we already found the metadata...
                if(foundCatalogFile)
                {
                    isStreamable = true;
                }

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
            addCatalogFile(zipOutputStream);
            addMetaData(zipOutputStream);
            addDocument(zipOutputStream);
        } finally
        {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    protected void handleDocument(final ZipInputStream inputStream, final ZipEntry zipEntry, boolean isStreamable) throws IOException
    {
        if(fileContent==null)
        {
            this.fileName= zipEntry.getName();
            //if it's not streamable, we need to read it into memory (yuck!) and wrap the bytes in an inputstream
            this.fileContent = isStreamable ? inputStream : new ByteArrayInputStream(toByteArray(inputStream));
            System.out.println("Got filecontent " + fileName);

            if (calculateHashEnable)
                this.fileContent = Sha1DigestInputStream.wrapInputStream(this.fileContent);
        }
    }

    /**
     *  put the version information, to let us know it can be streamed
     */
    protected void addVersionInformation(final ZipOutputStream zipOutputStream) throws IOException
    {
        zipOutputStream.putNextEntry(new ZipEntry(Constants.VERSION_INFORMATION_FILENAME));
        zipOutputStream.write(versionString.getBytes());
        zipOutputStream.closeEntry();
    }

    protected void addCatalogFile(final ZipOutputStream zipOutputStream) throws IOException, JAXBException
    {
        zipOutputStream.putNextEntry(new ZipEntry(this.catalogFileName));
        zipOutputStream.write(CatalogUtility.convertToBytes(this.catalog));
        zipOutputStream.closeEntry();
    }

    protected void addMetaData(final ZipOutputStream zipOutputStream) throws IOException, JAXBException
    {
        if (metadata!=null)
        {
            ByteArrayOutputStream metadataOS = new ByteArrayOutputStream();
            metadata.store(metadataOS, "");

            if (metadataOS!=null)
            {
                if (this.metadataFileName!=null) {
                    zipOutputStream.putNextEntry(new ZipEntry(this.metadataFileName));
                    zipOutputStream.write(metadataOS.toByteArray());
                }
                else {
                    zipOutputStream.putNextEntry(new ZipEntry("patientmetadata"+Constants.METADATA_FILE_EXT));
                    zipOutputStream.write(metadataOS.toByteArray());
                }
            }
            zipOutputStream.closeEntry();
        }
    }

    protected void addDocument(final ZipOutputStream zipOutputStream) throws IOException, ExecutionException, InterruptedException
    {
        if (fileName != null && fileContent != null) {

            zipOutputStream.putNextEntry(new ZipEntry(this.fileName));
            IOUtils.copy(fileContent,zipOutputStream);

            closeFileContent();

            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    /**
     * Closing the fileContent will close the input stream and will optionally calculate
     * and store the SHA1 hash if that's enabled.
     */
    protected void closeFileContent()
    {
        IOUtils.closeQuietly(fileContent);

        if (fileContent instanceof Sha1DigestInputStream)
            fileContentHash = ((Sha1DigestInputStream) fileContent).getSha1AsHex();
    }

    public InputStream getFileContent()
    {
        return fileContent;
    }

    public void setFileContent(InputStream fileContent)
    {
        setFileContent(fileContent, false);
    }

    /**
     * Private method to set file content optionally skips the wrapping of the stream
     * with the Sha1 digester.  This is required for ResettableApxPackagedStream to
     * work (note that the makeApxPackageStreamResettable closes the file content, which
     * also sets the hash so we don't lose the hash value).
     */
    protected void setFileContent(InputStream fileContent, boolean disableHashCalculation)
    {
        this.fileContent = fileContent;
        if (!disableHashCalculation && calculateHashEnable)
            this.fileContent = Sha1DigestInputStream.wrapInputStream(this.fileContent);
    }

    /**
     * Returns the fileContentHash if it has been calculated, or null if it hasn't been calculated yet.
     *
     * Since the fileContent is an InputStream, calculation of the hash can only be determined **after**
     * the stream has been consumed. To date, the only way we handle calculation is via the serialization
     * method call.
     *
     * If you wish to externally calculate the hash, you will need to handle it yourself and deal with any
     * stream issues that crop up.
     *
     * @return file Content Hash or null if it hasn't been calculated yet.
     */
    public String getFileContentHash()
    {
        return fileContentHash;
    }

    public void setCalculateHashEnable(boolean calculateHashEnable)
    {
        this.calculateHashEnable = calculateHashEnable;
    }

    public boolean isCalculateHashEnable()
    {
        return calculateHashEnable;
    }

    public void close() throws IOException
    {
        if(zipInputStream != null)
            zipInputStream.close();
    }

    protected void finalize() throws IOException {
        try {
            this.close();
        } catch (Throwable ex) {
            //ignore
        }
    }

    //
    // Convenience class that will create a resettable stream (freeing up the orginal input stream),
    // and auto calling reset on each getFileContent call.
    //
    // This should be used when the client knows that the stream will be consumed multuple times,
    // and keeping the bytes in memory once, and issuing stream resets is a desirable.
    //
    // The other alternative to consuming streams more than once is to used piped stream, which has
    // benefits and side effects - the approach depends on your particular use case - and care should
    // be taken in reaching a decision on which approach to use.
    //
    public static class ResettableApxPackagedStream extends ApxPackagedStream {
        ApxPackagedStream apxPackagedStream;

        public ResettableApxPackagedStream(ApxPackagedStream apxPackagedStream) throws IOException
        {
            this.apxPackagedStream = apxPackagedStream;
            makeApxPackageStreamResettable();
        }

        private void makeApxPackageStreamResettable() throws IOException
        {
            if(apxPackagedStream.getFileContent() == null) return;

            //We are going to be reading the file contents multiple times - let's consume the inputstream once.
            final byte [] fileContent = org.apache.commons.io.IOUtils.toByteArray(apxPackagedStream.getFileContent());

            //now that we've consumed that file content stream, make sure that inputstream is closed and
            // record hash if enabled
            apxPackagedStream.closeFileContent();

            // the "true" param disables wrapping file content stream with Sha1DigestInputStream as
            // this will kill the resettable nature of this stream and because it isn't necessary
            // because closeFileContent() calculates/records the hash for the content.
            apxPackagedStream.setFileContent(new ByteArrayInputStream(fileContent), true);
        }

        @Override
        public Properties getMetadata()
        {
            return apxPackagedStream.getMetadata();
        }

        @Override
        public void setMetadata(Properties metadata)
        {
            apxPackagedStream.setMetadata(metadata);
        }

        @Override
        public String getCatalogFileName()
        {
            return apxPackagedStream.getCatalogFileName();
        }

        @Override
        public void setCatalogFileName(String catalogFileName)
        {
            apxPackagedStream.setCatalogFileName(catalogFileName);
        }

        @Override
        public String getFileName()
        {
            return apxPackagedStream.getFileName();
        }

        @Override
        public void setFileName(String fileName)
        {
            apxPackagedStream.setFileName(fileName);
        }

        @Override
        public String getMetadataFileName()
        {
            return apxPackagedStream.getMetadataFileName();
        }

        @Override
        public void setMetadataFileName(String metadataFileName)
        {
            apxPackagedStream.setMetadataFileName(metadataFileName);
        }

        @Override
        public ApxCatalog getCatalog()
        {
            return apxPackagedStream.getCatalog();
        }

        @Override
        public void setCatalog(ApxCatalog catalog)
        {
            apxPackagedStream.setCatalog(catalog);
        }

        @Override
        public void serialize(OutputStream outputStream) throws IOException, JAXBException, ExecutionException, InterruptedException
        {
            apxPackagedStream.serialize(outputStream);
        }

        @Override
        protected void handleDocument(ZipInputStream inputStream, ZipEntry zipEntry, boolean isStreamable) throws IOException
        {
            apxPackagedStream.handleDocument(inputStream, zipEntry, isStreamable);
        }

        @Override
        protected void addVersionInformation(ZipOutputStream zipOutputStream) throws IOException
        {
            apxPackagedStream.addVersionInformation(zipOutputStream);
        }

        @Override
        protected void addCatalogFile(ZipOutputStream zipOutputStream) throws IOException, JAXBException
        {
            apxPackagedStream.addCatalogFile(zipOutputStream);
        }

        @Override
        protected void addMetaData(ZipOutputStream zipOutputStream) throws IOException, JAXBException
        {
            apxPackagedStream.addMetaData(zipOutputStream);
        }

        @Override
        protected void addDocument(ZipOutputStream zipOutputStream) throws IOException, ExecutionException, InterruptedException
        {
            apxPackagedStream.addDocument(zipOutputStream);
        }

        @Override
        public void setFileContent(InputStream fileContent)
        {
            apxPackagedStream.setFileContent(fileContent);
        }

        @Override
        public String getFileContentHash()
        {
            return apxPackagedStream.getFileContentHash();
        }

        @Override
        public void setCalculateHashEnable(boolean calculateHashEnable)
        {
            apxPackagedStream.setCalculateHashEnable(calculateHashEnable);
        }

        @Override
        public boolean isCalculateHashEnable()
        {
            return apxPackagedStream.isCalculateHashEnable();
        }

        @Override
        public void close() throws IOException
        {
            apxPackagedStream.close();
        }

        @Override
        public InputStream getFileContent()
        {
            try
            {
                if(apxPackagedStream.getFileContent() != null)
                {
                    apxPackagedStream.getFileContent().reset();
                }
            } catch (IOException e)
            {
                throw  new RuntimeException("Error resetting stream", e);
            }
            return apxPackagedStream.getFileContent();
        }

        protected void finalize() throws IOException {
            try {
                this.close();
            } catch (Throwable ex) {
                //ignore
            }
        }
    }
}
