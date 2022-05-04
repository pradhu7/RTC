package com.apixio.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import javax.crypto.CipherInputStream;
import org.apache.commons.io.IOUtils;

/**
 * This class might be better named EncryptingInputStream as it encrypts
 * the stream of bytes from the source InputStream with the current AES256
 * key.  It can be used when directly reading from an InputStream.
 * Metadata is prefixed to the encrypted stream of bytes.  No other
 * transformations are done here so if the output stream of bytes is to be
 * base64 encoded, for example, then the client must do that.
 */
public class EncryptedInputStream extends InputStream
{
    private InputStream inputStream;
    private boolean     isClosed;

    public EncryptedInputStream(InputStream inputStream, Security security)
    {
        this(security, inputStream, null);
    }

    /**
     * Encrypts the byte[] contents of the stream with the current v2 key.  "salt" is
     * ignored.  The v2 metadata array of bytes is prefixed to the encrypted contents.
     * Client code must do any encoding (e.g., base64) desired.
     */
    @Deprecated  // due to 'salt' param
    public EncryptedInputStream(InputStream is, Security security, String salt)
    {
        this(security, is, null);

        if (salt != null)
            throw new IllegalArgumentException("EncryptedInputStream no longer supports non-null 'salt' values: " + salt);
    }

    public static EncryptedInputStream encryptInputStreamWithScope(Security security, InputStream is, String scope)
    {
        return new EncryptedInputStream(security, is, scope);
    }

    /**
     * Parameter types in different order--hack!
     */
    private EncryptedInputStream(Security security, InputStream is, String scope)
    {
        super();

        if (!security.useEncryption)
        {
            inputStream = is;
            return;
        }

        // keep same v1 runtime semantics of v1 by catching all exceptions and throwing RuntimeException
        try
        {
            Security.EncryptingData ed = security.prepareEncryption(scope);

            inputStream = new SequenceInputStream(new ByteArrayInputStream(ed.metadata),
                                                  new CipherInputStream(is, ed.encrypter));
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public int read() throws IOException
    {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        return inputStream.read(b, off, len);
    }

    @Override
    public int available() throws IOException
    {
        return inputStream.available();
    }

    public void close() throws IOException
    {
        super.close();

        IOUtils.closeQuietly(inputStream);

        isClosed = true;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        return inputStream.read(b);
    }

    @Override
    public long skip(long n) throws IOException
    {
        return inputStream.skip(n);
    }

    @Override
    public void mark(int readlimit)
    {
        inputStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException
    {
        inputStream.reset();
    }

    @Override
    public boolean markSupported()
    {
        return inputStream.markSupported();
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
