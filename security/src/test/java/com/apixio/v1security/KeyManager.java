package com.apixio.v1security;

import com.apixio.v1security.exception.ApixioSecurityException;
import org.apache.commons.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by alarocca on 11/9/16.
 *
 * January 2020:  totally deprecated for v2 of security, but is needed for creating
 * v1-encrypted data that can be decrypted by v2.  In order to do this the core
 * behavior of this class has been changed from fetching key info from a security
 * server (done in getKeyFromService) to returning fixed data.  This fixed data
 * is specified by the external system by setting Java system properties:
 *
 *  java -Dkeymanager_key=zyx
 */
public class KeyManager {

    private Configuration config;

    /**
     * encryptKeyWithVersion is the key to be used for encryption; it will
     * always be the most current version of the "default" key.
     */
    private String encryptKeyWithVersion;

    /**
     * decryptKeys contains all the decryption keys, with one entry for each
     * version.
     */
    private Map<String, String> decryptKeys = new HashMap<String, String>();


    /**
     * the list of decryption key versions known to be used; prefetched.
     */
    private static String[] knownDecryptVersions = new String[] {"V01"};


    public KeyManager(Configuration config) {
        this.config = config;

        /* January 2020 commented out as this class will just return configured data
        Map<String, String> key = getKey(config);
        encryptKeyWithVersion = (String) key.get("default");

        for (String ver : knownDecryptVersions)
            getDecryptKey(key, ver);
        */
    }

    /**
     * getDecryptKey looks up and returns the decryption key for the given key version.
     * If the key for the given version isn't cached, then a fetch is done
     */
    protected String getDecryptKey(String version)
    {
        return System.getProperty("keymanager_key");

        /* January 2020 commented out as this class will just return configured data
        String key = decryptKeys.get(version);

        if (key == null)
        {
            key = getKey(config).get(version);
            decryptKeys.put(version, key);
        }

        return key;
        */
    }

    protected String getKeyVersion() throws ApixioSecurityException
    {
        return getKeyVersion(encryptKeyWithVersion);
    }

    protected String getKeyVersion(String key) throws ApixioSecurityException
    {
        return knownDecryptVersions[0];

        /* January 2020 commented out as this class will just return configured data
        String version = null;
        if(key != null && key.contains("_"))
        {
            version = key.substring(0, key.indexOf("_"));
        }
        if(version == null)
        {
            throw new ApixioSecurityException("Missing key version number");
        } else
        {
            return version;
        }
        */
    }

    protected String getRawKey() throws ApixioSecurityException
    {
        return getRawKey(encryptKeyWithVersion);
    }

    protected String getRawKey(String key) throws ApixioSecurityException
    {
        return System.getProperty("keymanager_key");

        /* January 2020 commented out as this class will just return configured data
        String rawKey = null;
        if(key != null && key.contains("_"))
        {
            rawKey = key.substring(key.indexOf("_") + 1);
        }
        if(rawKey == null)
        {
            throw new ApixioSecurityException("Missing key version number");
        } else
        {
            return rawKey;
        }
        */
    }

    private Map<String, String> getKey(Configuration config) {
        return getKeyFromService(config);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getKeyFromService(Configuration config) {
        Map<String, String> key = (new KeyReceiver()).getKeyFromService(config);
        return key;
    }
    /**
     * getDecryptKey looks up and returns the decryption key for the given key version.
     */
    private String getDecryptKey(Map<String, String> keyMap, String version)
    {
        String key = decryptKeys.get(version);

        if (key == null)
        {
            key = (String) keyMap.get(version);
            decryptKeys.put(version, key);
        }

        return key;
    }
}
