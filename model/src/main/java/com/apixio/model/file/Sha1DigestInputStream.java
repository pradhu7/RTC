package com.apixio.model.file;

import java.io.InputStream;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

/**
 * Wraps InputStream to allow automatic calculation of SHA1 hash of stream contents.
 * Client just needs to call wrapInputStream method and use the returned value as
 * a normal InputStream (e.g., as the source for data copied to S3) and when
 * the stream has been fully consumed, a call to getSha1AsHex will return the hexadecimal
 * representation of the SHA1 hash value.
 */
public class Sha1DigestInputStream extends DigestInputStream
{
    private MessageDigest sha1Digest;

    /**
     * Hide to force factory method use
     */
    private Sha1DigestInputStream(InputStream real, MessageDigest sha1)
    {
        super(real, sha1);

        this.sha1Digest = sha1;
    }

    /**
     * Client should call this after InputStream is exhausted
     */
    public String getSha1AsHex()
    {
        return Hex.encodeHexString(sha1Digest.digest());
    }

    /**
     * Wrap the input stream with the SHA1 digester
     */
    public static Sha1DigestInputStream wrapInputStream(InputStream real)
    {
        return new Sha1DigestInputStream(real, DigestUtils.getDigest(MessageDigestAlgorithms.SHA_1));
    }
}
