package com.apixio.security;

import java.io.IOException;
import java.io.OutputStream;
import javax.crypto.CipherOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * This class might be better named EncryptingOutputStream as it encrypts the
 * stream of bytes being written to the destination OutputStream with the current
 * AES256 key.  Metadata is prefixed to the encrypted stream of bytes.  No
 * other transformations are done here so if the output stream of bytes is to be
 * base64 encoded, for example, then the client must do that.
 */
public class EncryptedOutputStream extends OutputStream
{
    private OutputStream outputStream;
    private boolean      isClosed = false;

    public EncryptedOutputStream(OutputStream outputStream, Security security)
    {
        this(security, outputStream, null);
    }

    /**
     * Encrypts the byte[] contents of the bytes written to stream with the
     * current key.  "salt" is ignored.  The v2 metadata array of bytes is
     * prefixed to the encrypted contents.  Client code must do any encoding
     * (e.g., base64) desired.
     */
    @Deprecated // due to salt param
    public EncryptedOutputStream(OutputStream os, Security security, String salt)
    {
        this(security, os, null);

        if (salt != null)
            throw new IllegalArgumentException("EncryptedOutputStream no longer supports non-null 'salt' values: " + salt);
    }

    public static EncryptedOutputStream encryptOutputStreamWithScope(Security security, OutputStream os, String scope)
    {
        return new EncryptedOutputStream(security, os, scope);
    }

    /**
     * Parameter types in different order--hack!
     */
    private EncryptedOutputStream(Security security, OutputStream os, String scope)
    {
        super();

        if (!security.useEncryption)
        {
            outputStream = os;
            return;
        }

        // keep same v1 runtime semantics of v1 by catching all exceptions and throwing RuntimeException
        try
        {
            Security.EncryptingData ed = security.prepareEncryption(scope);

            os.write(ed.metadata);

            outputStream = new CipherOutputStream(os, ed.encrypter);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void write(int b) throws IOException
    {
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        outputStream.write(b, off, len);
    }

    public void flush() throws IOException
    {
        outputStream.flush();
    }

    public void close() throws IOException
    {
        super.close();

        IOUtils.closeQuietly(outputStream);

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
