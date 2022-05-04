package com.apixio.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Reusable utility methods
 */
public class ZipUtil
{

    /**
     * Utility methods to perform GZip/GUnzip on strings
     */

    public static byte[] zipString(String s) throws IOException
    {
        return zipBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] zipBytesSafe(byte[] s)
    {
        try
        {
            return zipBytes(s);
        }
        catch (IOException x)
        {
            throw new RuntimeException("Unable to zip byte[]", x); 
        }
    }

    public static byte[] zipBytes(byte[] s) throws IOException
    {
        ByteArrayOutputStream baos  = new ByteArrayOutputStream(s.length / 2);  // will contain gzipped data; assume at least 50% compression
        byte[]                bytes = new byte[2048];
        int                   c;

        try (
            OutputStream zip = new GZIPOutputStream(baos);
            InputStream  src = new ByteArrayInputStream(s);
            )
        {
            while ((c = src.read(bytes)) > 0)
                zip.write(bytes, 0, c);
        }

        //        float ol = s.length();
        //        float zl = baos.toByteArray().length;
        //        System.out.println("SFM:  zipString compression:  " + ((1.0 - zl/ol) * 100.) + "%");

        return baos.toByteArray();
    }

    public static String unzipString(byte[] b) throws IOException
    {
        return new String(unzipBytes(b), StandardCharsets.UTF_8);
    }

    public static byte[] unzipBytesSafe(byte[] s)
    {
        try
        {
            return unzipBytes(s);
        }
        catch (IOException x)
        {
            throw new RuntimeException("Unable to unzip byte[]", x); 
        }
    }

    public static byte[] unzipBytes(byte[] b) throws IOException
    {
        ByteArrayOutputStream dst   = new ByteArrayOutputStream(b.length * 2);
        byte[]                bytes = new byte[2048];
        int                   c;

        try (
            ByteArrayInputStream bais = new ByteArrayInputStream(b);
            InputStream          zip  = new GZIPInputStream(bais);
            )
        {
            while ((c = zip.read(bytes)) > 0)
                dst.write(bytes, 0, c);
        }

        return dst.toByteArray();
    }

    /**
     * testing
     */
    public static void main(String... args) throws IOException
    {
        for (String a : args)
        {
            byte[]  zipped   = zipString(a);
            String  unzipped = unzipString(zipped);
            float   zl       = zipped.length;
            float   uzl      = a.length();

            System.out.println("zip(" + a + ") len " + uzl + " produced byte[" + zl + "]; compression=" + (1.0 - zl / uzl) * 100.f);

            if (!a.equals(unzipped))
                throw new IllegalStateException("unzip(zip(" + a + ") failed");
        }
    }

}
