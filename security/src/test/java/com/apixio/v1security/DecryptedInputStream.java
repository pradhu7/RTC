package com.apixio.v1security;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.io.CipherInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by dyee on 2/9/17.
 */
public class DecryptedInputStream extends InputStream
{
    InputStream inputStream;
    InputStream originalInputStream;
    CipherInputStream cipherInputStream;

    boolean isClosed;

    public DecryptedInputStream(InputStream inputStream, Security security)
    {
        this(inputStream, security, null);
    }

    public DecryptedInputStream(InputStream inputStreamRaw, Security security, String salt)
    {
        super();
        this.originalInputStream = inputStreamRaw;
        try {
            SecurityHelper helper = new SecurityHelper();

            InputStream inputStream = inputStreamRaw;

            if(inputStreamRaw instanceof ApixioBase64InputStream) {
                inputStream = ((ApixioBase64InputStream)inputStreamRaw).getOriginalInputStream();
            }

            byte[] metadataLengthBuffer = new byte[2];
            inputStream.read(metadataLengthBuffer,0,2);
            // Subtract 2 for the metadatalength already read and add 1 for the terminating "x"
            int metaDataLength = Integer.parseInt(new String(metadataLengthBuffer,"UTF-8")) - 1;

            byte[] metadataBuffer = new byte[metaDataLength];
            inputStream.read(metadataBuffer,0,metaDataLength);
            int randomNumberSize = Integer.parseInt(new String(Arrays.copyOfRange(metadataBuffer,0, 2),"UTF-8"));

            String randomString = new String(Arrays.copyOfRange(metadataBuffer,2,randomNumberSize + 2),"UTF-8");
            String random = String.valueOf(Long.rotateRight(Long.parseLong(randomString), 4));
            // Read to the end with the exception of the final "x"
            String keyVersion = new String(Arrays.copyOfRange(metadataBuffer,randomNumberSize + 2, metaDataLength - 1),"UTF-8");

            if(inputStreamRaw instanceof ApixioBase64InputStream) {
                inputStream = ((ApixioBase64InputStream)inputStreamRaw).getBase64InputStream();
            }

            salt = (salt == null) ? random : random + salt;
            String key = security.getKeyManager().getDecryptKey(keyVersion);
            BufferedBlockCipher cipher = helper.getCipher(key, salt, random.getBytes(), false);
            cipherInputStream = new CipherInputStream(inputStream,cipher);

            // Let's buffer for improved performance
            this.inputStream =  new BufferedInputStream(cipherInputStream);

        } catch (Exception ex) {
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

    public void close() throws IOException {
        super.close();
        
        IOUtils.closeQuietly(inputStream);

        //these should be closed by the wrapped inputStream close, but just in case.
        IOUtils.closeQuietly(originalInputStream);
        IOUtils.closeQuietly(cipherInputStream);

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
    protected void finalize() throws IOException {
        if(!isClosed) close();
    }
}
