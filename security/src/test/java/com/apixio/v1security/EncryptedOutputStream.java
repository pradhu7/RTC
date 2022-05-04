package com.apixio.v1security;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.io.CipherOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by dyee on 2/9/17.
 */
public class EncryptedOutputStream extends OutputStream
{
    private CipherOutputStream outputStream;
    private OutputStream originalOutputStream;

    boolean isClosed = false;

    public EncryptedOutputStream(OutputStream outputStream, Security security)
    {
        this(outputStream, security, null);
    }

    public EncryptedOutputStream(OutputStream outputStreamRaw, Security security, String salt)
    {
        super();
        this.originalOutputStream = outputStreamRaw;
        SecurityHelper helper = new SecurityHelper();

        OutputStream outputStream = outputStreamRaw;
        if(outputStreamRaw instanceof ApixioBase64OutputStream)
        {
            outputStream = ((ApixioBase64OutputStream)outputStreamRaw).getOriginalOutputStream();
        }

        String random = helper.getRandomStr();
        salt = (salt == null) ? random : random + salt;

        try
        {
            String keyVersion = security.getKeyManager().getKeyVersion();
            String key = security.getKeyManager().getRawKey();
            byte[] keyMetadata = helper.obfuscateRandomNumber(random, keyVersion).getBytes("UTF-8");
            outputStream.write(keyMetadata);

            if(outputStreamRaw instanceof ApixioBase64OutputStream)
            {
                outputStream = ((ApixioBase64OutputStream)outputStreamRaw).getBase64OutputStream();
            }

            BufferedBlockCipher cipher = helper.getCipher(key, salt, random.getBytes("UTF-8"), true);
            this.outputStream = new CipherOutputStream(outputStream, cipher);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException(ex.getMessage(), ex);
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

    public void flush() throws IOException {
        outputStream.flush();
    }

    public void close() throws IOException {
        super.close();
        IOUtils.closeQuietly(outputStream);
        IOUtils.closeQuietly(originalOutputStream);
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
