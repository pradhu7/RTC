package com.apixio.utility;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

/**
 * 
 * Use SHA-1 only. This implementation is not known to have match implementations in Windows or other language.
 * 
 * @author lance
 *
 */
public class HashCalculator
{
    public static String getFileHash(byte[] file)
    {
        String hash = "";

        try
        {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            hash = calculateHash(sha1, file);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Hash calculation failure: cannot happen! " + e.getMessage());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("Hash calculation failure: cannot happen!" + e.getMessage());
        }

        return hash;
    }

    static String calculateHash(MessageDigest algorithm, byte[] filebytes) throws IOException
    {
        InputStream is = new ByteArrayInputStream(filebytes);
        BufferedInputStream bis = new BufferedInputStream(is);
        DigestInputStream dis = new DigestInputStream(bis, algorithm);

        try
        {
            // read the file and update the hash calculation
            while (dis.read() != -1)
                ;

            // get the hash value as byte array
            byte [] hash = algorithm.digest();
            return Hex.encodeHexString(hash);
        }
        finally
        {
            IOUtils.closeQuietly(dis);

            // defensively close these, the dis.close() call should close all wrapped IS, but just in case :-)
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }
    }
}
