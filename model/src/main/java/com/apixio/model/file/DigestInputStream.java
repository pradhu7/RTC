package com.apixio.model.file;

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
public class DigestInputStream extends InputStream
{
    /**
     * real data and digest object
     */
    private InputStream   real;
    private MessageDigest digester;

    public DigestInputStream(InputStream real, MessageDigest digester)
    {
        this.real     = real;
        this.digester = digester;
    }

    /**
     * This is the only thing that needs to be overridden, luckily
     */
    @Override
    public int read() throws IOException
    {
        int b = real.read();

        if (b != -1)
            digester.update((byte) b);

        return b;
    }
    
}
