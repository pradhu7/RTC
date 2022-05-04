package com.apixio.v1security;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.io.CipherInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by dyee on 2/9/17.
 */
public class EncryptedInputStream extends InputStream
{
    private SequenceInputStream inputStream;
    private InputStream originalInputStream;
    private InputStream cipherInputStream;
    private boolean isClosed;

    public EncryptedInputStream(InputStream inputStream, Security security)
    {
        this(inputStream, security, null);
    }

    public EncryptedInputStream(InputStream inputStreamRaw, Security security, String salt)
    {
        super();

        this.originalInputStream = inputStreamRaw;

        SecurityHelper helper = new SecurityHelper();
        String random = helper.getRandomStr();
        salt = (salt == null) ? random : random + salt;

        try
        {
            InputStream inputStream = inputStreamRaw;
            if(inputStreamRaw instanceof ApixioBase64InputStream) {
                inputStream = ((ApixioBase64InputStream)inputStreamRaw).getOriginalInputStream();
            }

            String keyVersion = security.getKeyManager().getKeyVersion();
            String key = security.getKeyManager().getRawKey();
            byte[] keyMetadata = helper.obfuscateRandomNumber(random, keyVersion).getBytes("UTF-8");
            BufferedBlockCipher cipher = helper.getCipher(key, salt, random.getBytes("UTF-8"), true);
            cipherInputStream = new CipherInputStream(inputStream, cipher);

            if(inputStreamRaw instanceof ApixioBase64InputStream) {
                cipherInputStream = new Base64InputStream(cipherInputStream,true);
            }

            List<InputStream> streams = Arrays.asList(new ByteArrayInputStream(keyMetadata), cipherInputStream);
            this.inputStream = new SequenceInputStream(Collections.enumeration(streams));
        }
        catch (Exception ex)
        {
            throw new IllegalStateException(ex.getMessage(), ex);
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

    public void close() throws IOException {
        super.close();

        IOUtils.closeQuietly(inputStream);

        //should be closed by wrapped inputstream, but just to be sure...
        IOUtils.closeQuietly(originalInputStream);
        IOUtils.closeQuietly(cipherInputStream);

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
    protected void finalize() throws IOException {
        if(!isClosed) close();
    }
}
