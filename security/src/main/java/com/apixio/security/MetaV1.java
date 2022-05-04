package com.apixio.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class formalizes the format/layout of what is now called "v1" of the bytes of metadata
 * that were prepended to the ciphertext.
 *
 * The layout of the bytes of metadata are as follows (shown as ordered in the actual byte[]):
 *
 *  * 2 bytes; these are interpreted as a UTF-8 encoded String that will be parsed into an int
 *             value; this int value gives the total number of bytes (/chars, since everything
 *             is UTF-8 encoded w/ nothing outside ASCII) in the full metadata.  This length
 *             INCLUDES these two bytes but does NOT include the terminating 'x' character (below)
 *
 *  * 2 bytes; these are interpreted as a UTF-8 encoded String that will be parsed into an int
 *             value; this int value gives the total number of bytes that are immediately following
 *             that are to be interpreted as the "random number"
 *
 *  * n bytes; n is taken from the previous 2 bytes; these n bytes are interpreted as the
 *             UTF-8 encoded String form of a long value ("random number").  This long value
 *             is to be rotated right by 4 bits and converted back to a String for the final
 *             value
 *
 *  * m bytes; m is a count of the rest of the bytes to get to the full declared length of the
 *             metadata.  These bytes are interpreted as the UTF-8 encoding of the key version
 *             string.
 *
 *  * 'x'; the lowercase X character (byte); probably exists so clients could scan for 'x' to
 *         get to end of metadata easily...
 *
 * Note that all bytes of the metadata are UTF-8 characters (for readability?).  Also note that
 * there is no easily tested signature to quickly determine that this is a v1 meta structure.
 * For that reason, v2 of security metadata includes byte markers at the beginning to help
 * check quickly.  This v1 class does no checking of data so if v2 metadata is passed to the
 * constructors then its behavior is undetermined.
 */
class MetaV1
{
    /**
     * Max size of persisted metadata.  This is required because we need to
     * be able to peek into an InputStream to see if it's v1/v2.  V1 metadata
     * is at most 99 bytes long due to the first 2 bytes encoding the total length.
     */
    public final static int MAX_MARKER_PEEK = 99;

    /**
     * Kind of hacky as MetaV1 is also creating values besides metadata ('random' is
     * also used to create Cipher).
     */
    static class V1EncryptingData
    {
        String random;
        byte[] metadata;
    }

    /**
     * For v1 encryption; copied verbatim from v1 SecurityHelper.java
     */
    private static SecureRandom random = null;
    static
    {
        try {
            // this is good and should be in every Java
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        } catch (GeneralSecurityException e) {
            random = new SecureRandom();
        }
    }

    /**
     * Extracted fields
     */
    private String randomNumber;
    private String keyVersion;

    /**
     * Construct from the bytes of a String
     */
    MetaV1(String data) throws IOException
    {
        this(new ByteArrayInputStream(data.getBytes(UTF_8)));
    }

    /**
     * Attempts to determine if the contents of the input stream are v1 metadata.  Since
     * there's no explicit marker, we determine this by reading the first two 2-byte lengths
     * and if those are both reasonable, we assume it's v1.
     */
    static boolean isPossiblyV1(InputStream is) throws IOException
    {
        int l1 = read2DigitNum(is);
        int l2 = read2DigitNum(is);

        // 99 is max 2-digit length
        return ((l1 > 0) && (l1 < 100) && (l2 > 0) && (l2 < 100));
    }

    /**
     * Reads the next 2 bytes/character (UTF-8), verifies they are digits 0-9
     * and returns the int value they represent.  -1 is returned if either
     * byte/char is not a digit.  Caller must manage stream repositioning as
     * necessary
     */
    private static int read2DigitNum(InputStream is) throws IOException
    {
        byte[] buf = new byte[2];
        int    c1;
        int    c2;

        is.read(buf);

        c1 = (int) buf[0];
        c2 = (int) buf[1];

        if ((c1 < '0') || (c1 > '9') || (c2 < '0') || (c2 > '9'))
            return -1;
        else
            return ((c1 - '0') * 10) + (c2 - '0');
    }

    /**
     * Construct from an InputStream. Stream is left at the byte immediately after
     * the terminating 'x' of v1 metadata.
     */
    public MetaV1(InputStream data) throws IOException
    {
        byte[]  buf = new byte[100];  // no v1 meta is more than 99 bytes by construction
        int     rlen;                 // remaining length
        int     nlen;                 // number len

        // first two bytes are total length, as a string.  total length includes these 2 bytes but does NOT include terminating 'x' character
        rlen = read2DigitNum(data) - 2;  // already read 2

        // next 2 bytes are # of bytes in random number
        nlen = read2DigitNum(data);
        rlen = rlen - 2;

        // read in actual random number
        if (nlen >= buf.length)
            throw new IllegalStateException("Attempt to read " + nlen +
                                            " bytes from MetaV1 data stream but buffer is " + buf.length +
                                            " bytes (v1 is limited to 2 bytes to indicate size)");

        data.read(buf, 0, nlen);
        randomNumber = Long.toString(Long.rotateRight(Long.parseLong(new String(buf, 0, nlen, UTF_8)), 4));  // initially encoded by rotating left 4 bytes

        // read in remaining to get key version
        rlen = rlen - nlen;

        if (rlen >= buf.length)
            throw new IllegalStateException("Attempt to read " + rlen +
                                            " bytes from MetaV1 data stream but buffer is " + buf.length + " bytes");

        data.read(buf, 0, rlen);
        keyVersion = asString(buf, rlen);

        // toss final 'x' char
        data.read(buf, 0, 1);
    }

    /**
     * Adapted from v1 EncryptedInputStream.java code
     */
    static V1EncryptingData createMetaV1(String keyVersion)
    {
        V1EncryptingData v1ed = new V1EncryptingData();

        v1ed.random   = getRandomStr();
        v1ed.metadata = obfuscateRandomNumber(v1ed.random, keyVersion).getBytes(UTF_8);

        return v1ed;
    }

    /**
     * Copied without modifications from v1 SecurityHelper.getRandomStr
     */
    private static String getRandomStr()
    {
        String randomString = null;
        randomString = (new StringBuilder(String.valueOf(random.nextInt(0x35a4e900) + 0x5f5e100))).append(random.nextInt(0x895440) + 0xf4240).toString();
        return randomString;
    }

    /**
     * Copied without modifications from v1 SecurityHelper.obfuscateRandomNumber
     */
    private static String obfuscateRandomNumber(String random, String keyVersion)
    {
        String obfuscatedRand = null;
        Long randomLong = Long.valueOf(Long.parseLong(random));
        randomLong = Long.valueOf(Long.rotateLeft(randomLong.longValue(), 4));
        String randSize = "";
        int randomNumberLength = (new StringBuilder()).append(randomLong).toString().length();
        if(randomNumberLength < 10)
        {
            randSize = (new StringBuilder("0")).append(randomNumberLength).toString();
        } else
        {
            randSize = (new StringBuilder()).append(randomNumberLength).toString();
        }
        String totalLength = "";
        int totalLengthInt = randomNumberLength + 2 + keyVersion.length() + 2;
        if(totalLengthInt < 10)
        {
            totalLength = (new StringBuilder("0")).append(totalLengthInt).toString();
        } else
        {
            totalLength = (new StringBuilder()).append(totalLengthInt).toString();
        }
        obfuscatedRand = (new StringBuilder(String.valueOf(totalLength))).append(randSize).append(randomLong).append(keyVersion).append('x').toString();
        return obfuscatedRand;
    }

    /**
     * Getters
     */
    public String getRandomNumber()
    {
        return randomNumber;
    }
    public String getKeyVersion()
    {
        return keyVersion;
    }

    /**
     * Convert the first n bytes of the buf to a string; encoding is UTF-8
     */
    private String asString(byte[] buf, int len)
    {
        return new String(buf, 0, len, UTF_8);
    }

}
