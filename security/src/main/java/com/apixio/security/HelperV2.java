package com.apixio.security;

import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.apixio.security.exception.ApixioSecurityException;

/**
 * Interface into creating v2-compatible AES Cipher object via a dynamically loaded
 * JCE provider.
 */
class HelperV2
{
    /**
     * Using bcfipspath is the normal way to configure BC jar paths as it's far more intuitive
     * and less error-prone as it just requires the path to the .jar file.  Using bcfipsname
     * is required ONLY if the path (absolute or relative) cannot be known; the only known
     * case for this AWS S3 Lambda functions as the deployment is under the control of AWS
     * and it's not defined (?) what the process' starting directory is.
     */
    public  final static String SYSPROP_FIPS_JARNAME   = "bcfipsname";   // must be findable as a resource in classpath
    public  final static String FIPS_JARPATH_CONFIG    = "bcfipspath";   // must be path to jar that's NOT in classpath
    private final static String FIPS_DESCRIPTION       = "BouncyCastle FIPS140-certified jar";

    /**
     * Including v2 security .jar in an uber jar and using a shader to rename BouncyCastle classes will/can change the
     * classnames within a String constant (at least sbt's ShadeRule.rename().inAll will do so).  Given that this code
     * explicitly loads BC classes from external jars (that we assume haven't been shaded) using a ClassLoader, doing
     * this rename on these strings here will break things so we protect here by breaking the string constant apart so
     * the rename can't happen.  Ugly.
     */
    private final static String[] BC_FIPS_PROVIDER_CLASS_PARTS = new String[] { "org", "bouncycastle", "jcajce", "provider", "BouncyCastleFipsProvider" };
    public  final static String   BC_FIPS_PROVIDER_CLASS       = String.join(".", BC_FIPS_PROVIDER_CLASS_PARTS);

    /**
     * Providers MUST be in different ClassLoader instances!
     */
    static Provider bouncyCastleFipsProvider;

    static void loadProvider()
    {
        String resourceName = System.getenv(SYSPROP_FIPS_JARNAME);

        // We support "loading by resource name" only via specifying the resource name via an environment
        // variable.  This is intentionally more limited than "loading by path" as the former is a special
        // case for the unusual case where the deployment of the code is controlled by another entity (e.g.,
        // AWS S3 Lambda).  The latter allows specifying the path to the jar by either an environment
        // variable or a Java system property

        if (resourceName != null)  // load by using its resource name (which must be findable in the classpath)
        {
            URL url = HelperV2.class.getClassLoader().getResource(resourceName);

            if (url == null)
                throw new ApixioSecurityException("Unable to load bcfips jar via ClassLoader.getResource(" + resourceName + ")." +
                                                  "Check value of the environment variable '" + SYSPROP_FIPS_JARNAME + "' to make sure it's findable by classpath.");

            bouncyCastleFipsProvider = ProviderUtil.loadProviderByURL(url, BC_FIPS_PROVIDER_CLASS, FIPS_DESCRIPTION);
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

            bouncyCastleFipsProvider = ProviderUtil.loadProvider(FIPS_JARPATH_CONFIG,
                                                                 BC_FIPS_PROVIDER_CLASS, FIPS_DESCRIPTION);
        }
    }

    /**
     * Get a JCE Cipher for AES256 for and initialize in the given encrypt/decrypt mode.
     * cipherMode is something like "AES/CBC/PKCS7PADDING".
     */
    static Cipher getV2AESCipher(String cipherMode, SecretKey secretKey, IvParameterSpec iv, boolean forEncrypt) throws ApixioSecurityException
    {
        try
        {
            Cipher c = Cipher.getInstance(cipherMode, bouncyCastleFipsProvider);

            c.init(((forEncrypt) ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE), secretKey, iv);

            return c;
        }
        catch (NoSuchAlgorithmException|
               InvalidKeyException|
               NoSuchPaddingException|
               InvalidAlgorithmParameterException
               x)
        {
            throw new ApixioSecurityException("V2 AES cipher creation failure", x);
        }
    }

}
