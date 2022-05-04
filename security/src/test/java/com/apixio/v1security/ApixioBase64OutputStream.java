package com.apixio.v1security;

import org.apache.commons.codec.binary.Base64OutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by dyee on 2/9/17.
 */
public class ApixioBase64OutputStream extends OutputStream
{
    private OutputStream originalOutputStream;
    private Base64OutputStream base64OutputStream;
    private boolean isClosed;

    public ApixioBase64OutputStream(OutputStream originalOutputStream)
    {
        this.originalOutputStream = originalOutputStream;
        this.base64OutputStream = new Base64OutputStream(this.originalOutputStream, true);
    }

    @Override
    public void write(int b) throws IOException
    {
        originalOutputStream.write(b);
    }

    public OutputStream getOriginalOutputStream()
    {
        return originalOutputStream;
    }

    public Base64OutputStream getBase64OutputStream()
    {
        return base64OutputStream;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        base64OutputStream.close();
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
