package com.apixio.security;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.security.Security.EncryptionVersion;

/**
 * Performs decryption of the bytes of the source InputStream.  This process consists of
 * examining the metadata present at the beginning of the stream and fetching the key
 * for decryption.
 */
public class DecryptedInputStream extends InputStream
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DecryptedInputStream.class);

    private InputStream inputStream;
    private boolean     isClosed;

    /**
     * 
     */
    public DecryptedInputStream(InputStream inputStream, Security security)
    {
        this(inputStream, security, null, EncryptionVersion.UNKNOWN);
    }

    /**
     * Decrypts 
     */
    public DecryptedInputStream(InputStream inputStream, Security security, String salt)
    {
        this(inputStream, security, salt, EncryptionVersion.UNKNOWN);
    }

    /**
     * Most general form of decryption.  "salt" is ignored for data encrypted with v2.
     *
     * "version" param is required for a special case of v1 encryption: in v1 when
     * encrypting String to String the output was the base64 encoding of the encrypted
     * byte[] but the v1 metadata was NOT base64 encoded, so this flag is required to
     * switch from reading unencoded bytes (which are just UTF8 chars for the metadata) to
     * wrapping the the remaining bytes with a base64 decoder.
     *
     * Note that this code assumes that reading bytes for metadata construction will not
     * require any decoding operation in this code so if, for example, the entire byte
     * stream is base64 encoded (which would include the v2 metadata) then the
     * InputStream.read() operation must decode the data.
     */
    DecryptedInputStream(InputStream is, Security security, String salt, EncryptionVersion version)
    {
        super();

        if (!security.useEncryption)
        {
            inputStream = is;
            return;
        }

        if (salt != null)
            throw new IllegalArgumentException("DecryptedInputStream no longer supports non-null 'salt' values: " + salt);

        inputStream = is;

        // keep same v1 runtime semantics of v1 by catching all exceptions and throwing RuntimeException
        try
        {
            Cipher cipher = security.getDecryptionCipher(inputStream, salt, version);  // curpos of stream at now beginning of encrypted data

            if (version == EncryptionVersion.V1)
                inputStream = new Base64InputStream(inputStream, false);

            inputStream = new BufferedInputStream(new CipherInputStream(inputStream, cipher));
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
    public int read(byte[] b) throws IOException
    {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException
    {
        return inputStream.skip(n);
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
