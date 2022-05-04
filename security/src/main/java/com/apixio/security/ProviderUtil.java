package com.apixio.security;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;

/**
 * Dynamically loads a JCE java.security.Provider into a separate ClassLoader instance
 * given the classname
 */
public class ProviderUtil
{
    /**
     * Load provider into its own ClassLoader to separate non-FIPS and FIPS code (as required
     * to use bc-fips:
     *
     * Referring to: https://www.bouncycastle.org/fips-java/BCFipsIn100.pdf:
     *  "The provider jar itself has no external dependencies, but it cannot be used in the same
     *   JVM as the regular Bouncy Castle provider. The classes in the two jar files do not get along"
     *
     * sysPropName contains the path to the external-to-the-jvm .jar file that contains the JCE
     * Provider code to be loaded.
     */
    public static Provider loadProvider(String configName, String className, String description) throws RuntimeException
    {
        String jarPath = getFromEnvOrSysProp(configName);
        File   jarFile;

        if (jarPath == null)
            throw new IllegalArgumentException("No system property or environment variable for " + description +
                                               "!  Use -D" + configName + "=pathToJar or set env var " +
                                               Config.getEnvConfigName(configName) + "=pathToJar" +
                                               " to declare");
        else if (((jarFile = new File(jarPath)) == null) || !jarFile.exists())   // first condition never false but makes for nicer formatted code...
            throw new IllegalArgumentException("BouncyCastle jar file " + jarFile + " doesn't exist => can't load java.security.Provider class " + className);

        try
        {
            return loadProviderByURL(jarFile.toURI().toURL(), className, description);
        }
        catch (MalformedURLException x)
        {
            throw new RuntimeException("Unexpected MalformedURLException loading security Provider", x);
        }
    }

    public static Provider loadProviderByURL(URL jarUrl, String className, String description) throws RuntimeException
    {
        try
        {
            URL[]          jarUrls = { jarUrl };
            URLClassLoader loader  = new URLClassLoader(jarUrls, Thread.currentThread().getContextClassLoader());
            Class<?>       clz     = loader.loadClass(className);

            return (Provider) clz.newInstance();
        }
        catch (Exception x)
        {
            throw new RuntimeException("Failure during load of java.security.Provider", x);
        }
    }

    /**
     * Look up config value first as a system property and then as an environment var.  This does
     * NOT use the maybe-expected Config.getSysPropConfigName() as doing so would break existing
     * scripts (12/4/2020)
     */
    private static String getFromEnvOrSysProp(String name)
    {
        String val = System.getProperty(name);

        if (val != null)
            return val;
        else
            return System.getenv(Config.getEnvConfigName(name));
    }

}
