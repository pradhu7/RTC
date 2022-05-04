package com.apixio.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.security.exception.ApixioSecurityException;
import com.apixio.security.utility.DataCompression;

public class FileEncryptor
{
    private static final Logger logger = LoggerFactory.getLogger(FileEncryptor.class);

    private Security security = Security.getInstance();


    public FileEncryptor()
    {

    }

    /**
     * This method encrypts a file.. (if it's not a zip file, it will compress
     * the bytes and will encrypt)
     * 
     * @param filename
     * @param filebytes
     * @return
     */
    public Map<String, byte[]> encryptFileInMemory(String filename, byte[] filebytes) throws Exception
    {
        Map<String, byte[]> result = new HashMap<String, byte[]>();

        try
        {
            if (!filename.toLowerCase().endsWith(".zip"))
            {
                DataCompression compr = new DataCompression();
                Map<String, byte[]> file = new HashMap<String, byte[]>();
                file.put(filename, filebytes);
                filebytes = compr.zipFilesInMemory(file);
            }
            String encryptedFileName = security.encryptToHex(filename);
            byte[] encryptedBytes = security.encryptBytesToBytes(filebytes);
            result.put(encryptedFileName, encryptedBytes);

        }
        catch (ApixioSecurityException e)
        {
            logger.error("Error while encrypting the file bytes:", e);
            throw new Exception("Error from Apixio Security API. Could not encrypt:" + e.getMessage());
        }

        return result;
    }
    /**
     * @author nkrishna This method zips the files given and encrypts the
     *         resultant zip file. If there is only one file submitted, then it
     *         just encrypts that file.
     * @param files
     * @return
     */
    public Map<String, byte[]> encryptFilesInMemory(Map<String, byte[]> files)
    {
        Map<String, byte[]> result = new HashMap<String, byte[]>();
        byte[] fileBytes = null;
        String fileName = null;

        try
        {
            if (files != null && !files.isEmpty())
            {
                if (files.size() > 1)
                {
                    DataCompression compr = new DataCompression();
                    fileBytes = compr.zipFilesInMemory(files);
                    fileName = "apxzip_" + UUID.randomUUID() + ".zip";
                }
                else
                {
                    fileName = files.keySet().iterator().next();
                    fileBytes = files.get(fileName);
                }
                result = encryptFileInMemory(fileName, fileBytes);
            }
        }
        catch (Exception e)
        {
            System.err.println("Exception cannot happen: " + e);
            throw new IllegalArgumentException(e);
        }

        return result;
    }

    public Map<String, byte[]> decryptFileInMemory(String filename, byte[] filebytes) throws Exception
    {
        Map<String, byte[]> result = new HashMap<String, byte[]>();

        try
        {
            String decryptedFileName = security.decryptFromHex(filename);

            boolean decompress = !decryptedFileName.toLowerCase().endsWith(".zip");
            byte[] zipStream = security.decryptBytesToBytes(filebytes,decompress);
            result.put(decryptedFileName, zipStream);

        }
        catch (ApixioSecurityException e)
        {
            logger.error("Error while encrypting the file bytes:", e);
            throw new Exception("Error from Apixio Security API. Could not encrypt:" + e.getMessage());
        }

        return result;
    }

    public Map<String, byte[]> decryptFilesInMemory(String filename, InputStream fileStream)
    {
        if (fileStream != null)
        {
            try
            {
                return decryptFilesInMemory(filename, IOUtils.toByteArray(fileStream));
            }
            catch (IOException e)
            {
                logger.error("Error while reading the stream.", e);
            }
        }
        return null;
    }

    public Map<String, byte[]> decryptFilesInMemory(byte[] inFile) throws Exception
    {
        byte[] zipStream = security.decryptBytesToBytes(inFile);

        DataCompression compr = new DataCompression();

        return compr.unzipFileInMemory(zipStream);
    }

    /**
     * Accepts fileName as a parameter, decrypts the file name, decrypts the
     * file, unzips the file (if there are more than one file). If there is only
     * one file after decrypting, it returns that file.
     * 
     * @author Tony Sun
     * @param filename
     * @return String
     */
    public Map<String, byte[]> decryptFilesInMemory(String filename, byte[] inFile)
    {
        Map<String, byte[]> fileMap = null;
        try
        {

            Map<String, byte[]> decrypted = decryptFileInMemory(filename, inFile);

            String decryptedFileName = decrypted.keySet().iterator().next();
            byte[] decryptedStream = decrypted.get(decryptedFileName);

            DataCompression c = new DataCompression();
            if (decryptedFileName.toLowerCase().endsWith(".zip"))
            {
                fileMap = c.unzipFileInMemory(decryptedStream);
            }
            else
            {
                fileMap = new HashMap<String, byte[]>();
                fileMap.put(decryptedFileName, decryptedStream);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return fileMap;
    }
}
