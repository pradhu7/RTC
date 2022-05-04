package com.apixio.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.apixio.security.Utils;

/**
 * This class formalizes the format/layout of version 2 of the bytes of metadata
 * that are prepended to the ciphertext.  The requirements for this metadata are:
 *
 *  * must start with a unique marker to enable quick checking if a byte[] is actually
 *    v2 of security metadata
 *
 *  * must be extensible over time by allowing addition of new key=value tuples
 *
 *  * must be able to be restored via String, byte[], or InputStream
 *
 *  * must initially contain the following tuples:
 *
 *    * version=1             # determines what other tuples should be here
 *    * dt=epochSec           # date of encryption; unix epoch seconds as a string
 *    * key=blah              # ENCRYPTED datakey from Vault as as string; must be submitted to Vault for decryption
 *    * scope=blah            # pdsId or ???; must be submitted to Vault
 *    * algo=AES              # standard encryption algorithm name; v1 is AES
 *
 *  * interface for tuple is Map<String,String>; means integer values are converted to
 *    string representation
 *
 *  * must be encoded as UTF-8
 *
 * Note that key is used by other security code to select the right decryption
 * key in Vault to decrypt the actual key.
 *
 * There is no requirement to obfuscate or secure this metadata in any way.
 *
 * For actual byte-level format of metadata:
 *
 *  * all integer values are written as string representation (UTF8) of the value
 *    followed by ";".  For example:  "12;" represents the value of twelve.  this
 *    format means that a scan for ";" is required when fetching/parsing the
 *    next integer value
 *  
 *  * all strings are written as <length>;<chars>; (UTF8), where <length> is the
 *    length of the string, and <chars> are the UTF8 encoding of the characters.
 *    the final ";" is for readability only.  For example:  "5;hello;" is the
 *    encoding of "hello".  Empty strings have a length of 0, and null strings
 *    have a length of -1.  Both null and empty strings will write 0 chars (but
 *    will still write the final ";");
 *
 * The actual layout of the bytes of the metadata are as follows:
 *
 *  * 9 byte marker to identify v2 of metadata; consists of "ApixioSM2"
 *
 *  * "<numberOfKeyPairs>"; integer count of number of key=value pairs; e.g., "7;"
 *
 *  * "<key>=<value>;"; repeated numberOfKeyPairs times; key=value pair; e.g.,,
 *     "7;version;=4;blah;;"  (note the double ";;")
 *
 * It is the explicit intent of this class to contain all code needed to read and write
 * the V2 metadata (to reduce chance of breakage from code changes elsewhere); this
 * intent is the reason com.fasterxml.jackson JSON parsing isn't used.
 */
class MetaV2
{
    public final static String FILE_SIGNATURE        = "ApixioSM2";      // MUST be multiple of 3 chars/bytes to be nicely base64 encoded!
    public final static String FILE_SIGNATURE_BASE64 = Utils.encodeBase64(FILE_SIGNATURE.getBytes(UTF_8));

    static
    {
        // base64 encoding must not end with any "=" chars as that would make a quick
        // signature-based comparison on base64-encoded metadata not work
        if (FILE_SIGNATURE_BASE64.endsWith("="))
            throw new IllegalStateException("Base64 encoding of file signature MUST NOT END WITH padding char of '='");
    }

    /**
     * Max size of persisted metadata marker.  This is required because we need to
     * be able to peek into an InputStream to see if it's v2.  Because we
     * have the file marker, the actual number of bytes that we need to peek
     * is just the length of that marker.
     */
    final static int MAX_MARKER_PEEK = FILE_SIGNATURE.length() + 10;  // 10 just to allow something unanticipated

    /**
     * Metadata field names.  Do NOT change these strings or all existing metadata will be unreadable
     */
    private final static String META_VERSION  = "ver";       // int; 1, 2, ...
    private final static String META_DATETIME = "dt";        // long; epoch ms
    private final static String META_ENCKEY   = "enckey";    // string; encrypted key
    private final static String META_ALGO     = "algo";      // string; as defined externally
    private final static String META_IV       = "iv";        // base64(byte[]); initialization vector
    private final static String META_SCOPE    = "scope";     // string; as defined externally

    private final static char[] HEXCHARS = "0123456789abcdef".toCharArray();

    /**
     * Version refers to the set of metadata fields that's being stored.
     */
    final static int VERSION = 1;

    /**
     * Fields are stored as strings only
     */
    private Map<String, String> fields;

    /**
     * Reconstruct from the bytes
     */
    MetaV2(String data) throws IOException
    {
        this(data.getBytes(UTF_8));
    }
    MetaV2(byte[] data) throws IOException
    {
        this(new ByteArrayInputStream(data));
    }
    MetaV2(InputStream data) throws IOException
    {
        if (!verifyMarker(data, false))
            throw new IllegalStateException("Data bytes are not MetaV2:  file marker " + FILE_SIGNATURE + " not present");

        fields = readMap(data);
    }

    /**
     * New construction
     */
    MetaV2(String encryptedKey, String algorithm, byte[] iv)
    {
        notNull(encryptedKey, "Encrypted Key");
        notNull(algorithm,    "Encryption algorithm");
        notNull(iv,           "Initialization vector");

        fields = new HashMap<>();

        // the only things this class has control over
        fields.put(META_VERSION,  Integer.toString(VERSION));
        fields.put(META_DATETIME, Long.toString(System.currentTimeMillis()));
        fields.put(META_ENCKEY,   encryptedKey);
        fields.put(META_ALGO,     algorithm);
        fields.put(META_IV,       toHex(iv));
    }

    /**
     * Optional values
     */
    void setScope(String scope)
    {
        fields.put(META_SCOPE, scope);
    }

    /**
     *
     */
    String asString()
    {
        return FILE_SIGNATURE + writeMap(fields);
    }

    /**
     * Caller must manage stream position
     */
    static boolean verifyMarker(InputStream data, boolean testBase64) throws IOException
    {
        String sig = (testBase64) ? FILE_SIGNATURE_BASE64 : FILE_SIGNATURE;

        for (int i = 0, m = sig.length(); i < m; i++)
        {
            if (sig.charAt(i) != data.read())
                return false;
        }

        return true;
    }

    /**
     *
     */
    String getAlgorithm()
    {
        return fields.get(META_ALGO);
    }
    String getEncryptedKey()
    {
        return fields.get(META_ENCKEY);
    }
    byte[] getIv()
    {
        return fromHex(fields.get(META_IV));
    }
    String getScope()
    {
        return fields.get(META_SCOPE);
    }

    /**
     * Private implementation
     */

    /**
     * Stolen shamelessly from stackoverflow because I didn't want to pull in another
     * dependency.
     */
    private static String toHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];

        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xff;

            hexChars[j * 2]     = HEXCHARS[v >>> 4];
            hexChars[j * 2 + 1] = HEXCHARS[v & 0x0f];
        }

        return new String(hexChars);
    }

    private static byte[] fromHex(String s)
    {
        int    len  = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) (
                (Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16)
                );
        }

        return data;
    }

    /**
     * Encodes the given integer value to be written to metadata.  Returned
     * format will be
     *
     *  ###;
     *
     * like "12;" for the value of twelve
     */
    private static String writeInteger(int n)
    {
        return Integer.toString(n) + ";";
    }
    
    /**
     * Encodes string to be written to metadata.  Returned format will be
     *
     *  <len>;<chars>;
     */
    private static String writeString(String s)
    {
        if (s == null)
            return "-1;;";
        else
            return writeInteger(s.length()) + s + ";";  // final ; for readability only
    }

    private static String writeMap(Map<String, String> meta)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(writeInteger(meta.size()));

        for (Map.Entry<String,String> entry : meta.entrySet())
        {
            sb.append(writeString(entry.getKey()));
            sb.append("=");
            sb.append(writeString(entry.getValue()));
        }

        return sb.toString();
    }

    /**
     * Read the integer presumably at the current position in the streams
     */
    private static int readInteger(InputStream is) throws IOException
    {
        StringBuilder sb   = new StringBuilder();
        boolean       term = false;
        int           ch;

        while ((ch = is.read()) != -1)
        {
            if (ch == ';')
            {
                term = true;
                break;
            }
            else
            {
                sb.append((char) ch);
            }
        }

        if (!term)
            throw new IllegalStateException("Unexpected hit EOF while reading/parsing number; got " + sb.toString());
        else
            return Integer.parseInt(sb.toString());
    }

    /**
     *
     */
    private static String readString(InputStream is) throws IOException
    {
        int     len = readInteger(is);
        String  s;

        if (len > -1)
        {
            if (len > 0)
            {
                StringBuilder sb  = new StringBuilder(len + 2);
                int           ch;

                while ((len-- > 0) && ((ch = is.read()) != -1))
                    sb.append((char) ch);

                s = sb.toString();
            }
            else
            {
                s = "";
            }
        }
        else
        {
            s = null;
        }

        // read trailing ";"
        if (is.read() != ';')
            throw new IllegalStateException("Failed to read expected ';' at end of string:  " + s);

        return s;
    }

    /**
     *
     */
    private static Map<String, String> readMap(InputStream is) throws IOException
    {
        int                 size = readInteger(is);
        Map<String, String> map  = new HashMap<>(size + 2);
        int                 ch;

        while (size-- > 0)
        {
            String key = readString(is);

            if ((ch = is.read()) != '=')
                throw new IllegalStateException("Expected '=' while reading map but got " + ((char) ch));

            map.put(key, readString(is));
        }

        return map;
    }

    /**
     *
     */
    private static void notNull(Object v, String param)
    {
        if (v == null)
            throw new IllegalArgumentException("Security metadata for " + param + " can't be null");
    }

    /**
     * Test
    public static void main(String[] args) throws IOException
    {
        if (true)
        {
            System.out.println(FILE_SIGNATURE_BASE64);
            System.exit(0);
        }

        MetaV2 smv2 = new MetaV2("the encrypted key", "cbc", new byte[10]);
        String enc1;
        String enc2;

        smv2.setScope("somepds");

        enc1 = smv2.asString();

        System.out.println("Original      encoded: " + enc1);

        smv2 = new MetaV2(enc1);

        enc2 = smv2.asString();

        System.out.println("Reconstructed encoded: " + enc2);

        if (!enc1.equals(enc2))
            System.out.println("FAILED:  encodings are different");
    }
     */

    /**
     * Test
    public static void main2(String[] args) throws IOException
    {
        //        System.out.println(writeString(null));
        String encoded;

        encoded = writeInteger(22) + writeInteger(1025);
        ByteArrayInputStream is = new ByteArrayInputStream(encoded.getBytes(UTF_8));
        System.out.println("int roundtrip: " + readInteger(is) + " and " + readInteger(is));

        if (args.length == 1)
        {
            encoded = writeString(args[0]);

            System.out.println(encoded);

            System.out.println(readString(new ByteArrayInputStream(encoded.getBytes(UTF_8))));
        }
        else if (args.length == 2)
        {
            Map<String,String> m = new HashMap<>();
            m.put(args[0], args[1]);
            m.put(args[1], args[0]);

            encoded = writeMap(m);

            System.out.println(encoded);

            m = readMap(new ByteArrayInputStream(encoded.getBytes(UTF_8)));

            System.out.println("restored map: " + m);
        }
    }
     */
}
