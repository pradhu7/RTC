package com.apixio.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Wraps an InputStream so that reads from it will update the given
 * MessageDigest so that a digest can be created while another piece
 * of code reads the full stream of data.
 *
 * Example use:
 *
 *  InputStream is = new DigestInputStream(rawInputStream, DigestUtils.getDigest(MessageDigestAlgorithms.MD5))
 */
public class DigestInputStream extends FilterInputStream
{
    /**
     * real data and digest object
     */
    private MessageDigest digester;

    public DigestInputStream(InputStream real, MessageDigest digester)
    {
        super(real);

        this.digester = digester;
    }

    /**
     * This is the only thing that needs to be overridden, luckily.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int n = super.read(b, off, len);

        if (n >= 0)
            digester.update(b, off, n);

        return n;
    }

    /**
     * test only
     */
    public static void main(String... args) throws Exception
    {
        MessageDigest     md  = org.apache.commons.codec.digest.DigestUtils.getDigest(org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5);
        InputStream       is  = new java.io.FileInputStream(new java.io.File("/tmp/a"));
        DigestInputStream dis = new DigestInputStream(is, md);
        byte[]            bs  = new byte[1024];
        int               c;
        
        while ((c = dis.read(bs)) != -1)
            ;

        is.close();

        System.out.println("Digest is " + com.apixio.restbase.util.ConversionUtil.toHex(md.digest()));
    }
    
}
