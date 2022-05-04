package com.apixio.v1security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dyee on 2/9/17.
 */
public class DecompressedInputStream extends ZipInputStream
{
    boolean isClosed;
    public DecompressedInputStream(InputStream in)
    {
        super(in);
        try
        {
            //automatically get the next entry, and advance the input stream
            this.getNextEntry();
        } catch (IOException e)
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
            this.getNextEntry();
        } catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public int read() throws IOException
    {
        return super.read();
    }

    public int read(byte[] b) throws IOException
    {
        return super.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        return super.read(b, off, len);
    }

    public long skip(long n) throws IOException
    {
        return super.skip(n);
    }

    public int available() throws IOException
    {
        return super.available();
    }

    public void close() throws IOException
    {
        super.close();
        isClosed = true;
    }

    public void mark(int readlimit)
    {
        super.mark(readlimit);
    }

    public void reset() throws IOException
    {
        super.reset();
    }

    public boolean markSupported()
    {
        return super.markSupported();
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
