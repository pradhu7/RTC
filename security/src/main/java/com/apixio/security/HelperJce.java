package com.apixio.security;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URL;
import java.net.URLClassLoader;

import com.apixio.security.exception.ApixioSecurityException;

/**
 */
class HelperJce
{
    private final static String SYSPROP_GLOBAL_JCE_JARPATH = "bcglobalprovpath";   // must be path to jar that's NOT in classpath

    /**
     * This method allows client code to use a JCE security provider that must be registered with
     * java.security.Security and whose classes must be available in the default classloader.
     * Note that this is a huge (well-documented) hack.
     *
     * In order for a client to use this, the following must be done:
     *
     *  1.  the provider jar can't be in the classpath (same/normal limitation there)
     *  2.  the client code must call com.apixio.security.Security.getInstance to pull in FIP/non-FIPS BC providers
     *  3.  the client must have a way of getting the file path to the provider.jar file; e.g., sys prop or config
     *  4.  the client must call addProviderUrl(filepath)
     *
     * This works by adding the URL to the JCE provider code to the system classloader (that's the hack).  The
     * standard way to locate this JCE provider URL is via the system property with the name given in
     * SYSPROP_GLOBAL_JCE_JARPATH but if a non-null jarPath is passed in, then that's used instead.  A jar path
     * must be specified in one of those two methods or this call will fail.
     */
    static void addProviderUrl(String jarPath) throws Exception
    {
        if (jarPath == null)
            jarPath = System.getProperty(SYSPROP_GLOBAL_JCE_JARPATH);

        if (jarPath == null)
            throw new ApixioSecurityException("Attempt to add a 'global' JCE provider jar to the system classloader requires either" +
                                              " the system property -D" + SYSPROP_GLOBAL_JCE_JARPATH + "=pathToJar or client code" +
                                              " must supply a file path");

        try
        {
            URL            url         = (new File(jarPath)).toURI().toURL();
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method         method      = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);

            method.setAccessible(true);
            method.invoke(classLoader, url);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

}
