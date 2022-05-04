package com.apixio.security;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by dyee on 2/9/17.
 */
class CompressedOutputStream extends ZipOutputStream
{
    private boolean isClosed;

    public CompressedOutputStream(OutputStream outputStream)
    {
        super(outputStream);

        try
        {
            this.putNextEntry(new ZipEntry("file"));
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage(), ex);
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
    protected void finalize() throws IOException {
        if (!isClosed)
            close();
    }
}
