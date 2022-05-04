package com.apixio.util;

import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import com.apixio.restbase.util.ConversionUtil;

/**
 * Wraps InputStream to allow automatic calculation of MD5 hash of stream contents.
 * Client just needs to call wrapInputStream method and use the returned value as
 * a normal InputStream (e.g., as the source for data copied to S3) and when
 * the stream has been fully consumed, a call to getMd5AsHex will return the hexadecimal
 * representation of the MD5 hash value.
 */
public class Md5DigestInputStream extends DigestInputStream
{
    private MessageDigest md5Digest;

    /**
     * Hide to force factory method use
     */
    private Md5DigestInputStream(InputStream real, MessageDigest md5)
    {
        super(real, md5);

        this.md5Digest = md5;
    }

    /**
     * Client should call this after InputStream is exhausted
     */
    public String getMd5AsHex()
    {
        return ConversionUtil.toHex(md5Digest.digest()).toUpperCase();  // .toUpperCase to canonicalize hex string
    }

    /**
     * Wrap the input stream with the MD5 digester
     */
    public static Md5DigestInputStream wrapInputStream(InputStream real)
    {
        return new Md5DigestInputStream(real, DigestUtils.getDigest(MessageDigestAlgorithms.MD5));
    }

}
