package com.apixio.security;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.apixio.security.exception.ApixioSecurityException;

/**
 * Interface into creating v1-compatible AES Cipher object via a dynamically loaded
 * JCE provider.
 */
class HelperV1
{
    /**
     * Using bcprovpath is the normal way to configure BC jar paths as it's far more intuitive
     * and less error-prone as it just requires the path to the .jar file.  Using bcprovname
     * is required ONLY if the path (absolute or relative) cannot be known; the only known
     * case for this AWS S3 Lambda functions as the deployment is under the control of AWS
     * and it's not defined (?) what the process' starting directory is.
     */
    public  final static String SYSPROP_NONFIPS_JARNAME = "bcprovname";   // must be findable as a resource in classpath
    public  final static String NONFIPS_JARPATH_CONFIG  = "bcprovpath";   // must be path to jar that's NOT in classpath
    private final static String NONFIPS_DESCRIPTION     = "BouncyCastle non-FIPS140 jar";

    /**
     * Including v2 security .jar in an uber jar and using a shader to rename BouncyCastle classes will/can change the
     * classnames within a String constant (at least sbt's ShadeRule.rename().inAll will do so).  Given that this code
     * explicitly loads BC classes from external jars (that we assume haven't been shaded) using a ClassLoader, doing
     * this rename on these strings here will break things so we protect here by breaking the string constant apart so
     * the rename can't happen.  Ugly.
     */
    private final static String[] BC_PROVIDER_CLASS_PARTS = new String[] { "org", "bouncycastle", "jce", "provider", "BouncyCastleProvider" };
    public  final static String   BC_PROVIDER_CLASS       = String.join(".", BC_PROVIDER_CLASS_PARTS);

    // algorithm names/params were reverse-engineered from old code SecurityHelper.getAlgorithm:
    private final static String V1_PBE_ALGORITHM    = "PBEWITHSHA256AND256BITAES-CBC-BC";  // CBC only because actual password is long
    private final static String V1_CIPHER_ALGORITHM = "AES/CBC/PKCS7PADDING";
    private final static int    V1_PBE_KEYSIZE      = 256;  // 256 for AES256
    private final static int    V1_PBE_ITERCOUNT    = 50;

    /**
     * Providers MUST be in different ClassLoader instances!
     */
    private static Provider bouncyCastleProvider;

    static void loadProvider()
    {
        String resourceName = System.getenv(SYSPROP_NONFIPS_JARNAME);

        // We support "loading by resource name" only via specifying the resource name via an environment
        // variable.  This is intentionally more limited than "loading by path" as the former is a special
        // case for the unusual case where the deployment of the code is controlled by another entity (e.g.,
        // AWS S3 Lambda).  The latter allows specifying the path to the jar by either an environment
        // variable or a Java system property

        if (resourceName != null)  // load by using its resource name (which must be findable in the classpath)
        {
            URL url = HelperV1.class.getClassLoader().getResource(resourceName);

            if (url == null)
                throw new ApixioSecurityException("Unable to load bcprov jar via ClassLoader.getResource(" + resourceName + ")." +
                                                  "Check value of the environment variable '" + SYSPROP_NONFIPS_JARNAME + "' to make sure it's findable by classpath.");

            bouncyCastleProvider = ProviderUtil.loadProviderByURL(url, BC_PROVIDER_CLASS, NONFIPS_DESCRIPTION);
        }
        else  // load by an explicit path to the .jar file
        {
            // this code requires that the PATH to the provider jar be specified by either
            // a Java system property or an environment variable.  The provider jar classes MUST
            // NOT BE available as part of the default system classloader/search.
            //
            // The names of the system property and the environment variable follow the same
            // convention for the other v2 security configuration:  the envvar name is prefixed
            // with APXSECV2_ and then uppercase()d.

            bouncyCastleProvider = ProviderUtil.loadProvider(NONFIPS_JARPATH_CONFIG,
                                                             BC_PROVIDER_CLASS, NONFIPS_DESCRIPTION);
        }
    }

    /**
     *
     */
    static Cipher getV1AESCipher(String key, String rand, byte[] ivData, boolean forEncrypt) throws ApixioSecurityException
    {
        try
        {
            // v1 code defined the "key" to be a long-ish password that was stretched into an AES256 key by using
            // PBE with 50 iterations; PBE algorithm specifics were reverse-engineered from old code in
            // SecurityHelper.getAlgorithm

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(V1_PBE_ALGORITHM, bouncyCastleProvider);
            PBEKeySpec       pbeKeySpec = new PBEKeySpec(key.toCharArray(), rand.getBytes(StandardCharsets.UTF_8), V1_PBE_ITERCOUNT, V1_PBE_KEYSIZE);
            SecretKeySpec    secretKey  = new SecretKeySpec(keyFactory.generateSecret(pbeKeySpec).getEncoded(), "AES");
            Cipher           cipher     = Cipher.getInstance(V1_CIPHER_ALGORITHM, bouncyCastleProvider);

            cipher.init(((forEncrypt) ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE), secretKey, new IvParameterSpec(ivData));

            return cipher;
        }
        catch (NoSuchAlgorithmException|
               InvalidKeySpecException|
               InvalidKeyException|
               NoSuchPaddingException|
               InvalidAlgorithmParameterException
               x)
        {
            throw new ApixioSecurityException("V1 AES cipher creation failure", x);
        }
    }

}
