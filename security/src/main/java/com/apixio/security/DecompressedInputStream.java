package com.apixio.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipInputStream;

/**
 * Created by dyee on 2/9/17.
 *
 * This is a thin wrapper over ZipInputStream and it really only provides the automatic
 * advance to the next ZipEntry in the stream.
 */
class DecompressedInputStream extends ZipInputStream
{
    private boolean isClosed;

    public DecompressedInputStream(InputStream in)
    {
        super(in);

        try
        {
            //automatically get the next entry, and advance the input stream
            getNextEntry();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public DecompressedInputStream(InputStream in, Charset charset)
    {
        super(in, charset);

        try
        {
            //automatically get the next entry, and advance the input stream
            getNextEntry();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void close() throws IOException
    {
        super.close();

        isClosed = true;
    }

    /**
     * Ensures that the <code>close</code> method of this file input stream is
     * called when there are no more references to it.
     *
     */
    protected void finalize() throws IOException
    {
        if (!isClosed)
            close();
    }
}
