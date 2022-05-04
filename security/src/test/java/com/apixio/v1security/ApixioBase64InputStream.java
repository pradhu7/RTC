package com.apixio.v1security;

import org.apache.commons.codec.binary.Base64InputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by dyee on 2/9/17.
 */
public class ApixioBase64InputStream extends InputStream
{
    private InputStream originalInputStream;
    private Base64InputStream base64InputStream;
    private boolean isClosed;

    public ApixioBase64InputStream(InputStream originalInputStream)
    {
        this.originalInputStream = originalInputStream;
        this.base64InputStream = new Base64InputStream(this.originalInputStream);
    }

    public ApixioBase64InputStream(InputStream originalInputStream, boolean encode)
    {
        this.originalInputStream = originalInputStream;
        this.base64InputStream = new Base64InputStream(this.originalInputStream, encode);
    }

    @Override
    public int read() throws IOException
    {
        return base64InputStream.read();
    }

    public InputStream getOriginalInputStream()
    {
        return originalInputStream;
    }

    public Base64InputStream getBase64InputStream()
    {
        return base64InputStream;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        base64InputStream.close();

        isClosed = true;
    }

    /**
     * Ensures that the <code>close</code> method of this file input stream is
     * called when there are no more references to it.
     *
     */
    protected void finalize() throws IOException {
        if(!isClosed) close();
    }
}

