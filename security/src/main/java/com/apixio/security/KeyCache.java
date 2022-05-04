package com.apixio.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.util.LruCache;

/**
 * Handles the caching of encryption and decryption keys according to the given
 * configuration.  Cache key is the base64(encrypted) string as returned from
 * Vault.createTransitDatakey() and the cache value is the raw decrypted byte[32] AES256
 * key along with a timestamp.
 *
 * The cache is populated by two mechanisms: the rotation "in" of a new encryption key,
 * and the normal fetch/put caching of decryption keys.  The rotating in of new
 * encryption keys also maintains the idea of a current encryption key so that clients
 * can get the current key easily.
 *
 * A TTL on cache entries is merely to remove unused AES keys just in case there's
 * some attack on the JVM heap (unlikely).
 */
class KeyCache  // purposely not public
{
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyCache.class);

    /**
     * Specific to datakey/transit model of Vault in that we get back from Vault a
     * "raw" AES key (as byte[32]) along with an encrypted form of it.  The raw key is
     * to be used directly for actual encryption and the encrypted form of the key is
     * to be kept as metadata.  Decryption of data consists of asking Vault to decrypt
     * this encrypted form that's stored as metadata to get back the raw AES key.
     */
    static class KeyCombo
    {
        byte[] rawKey;
        String encryptedKey;  // as returned from Vault REST call; contains meta+encrypted key
        long   creationTime;  // of when it was added to cache

        KeyCombo(byte[] raw, String encrypted, long time)
        {
            rawKey       = raw;
            encryptedKey = encrypted;
            creationTime = time;
        }
    }

    /**
     * Map from encryptedKey to keycombo.  Holds keys needed for decryption
     * along with the single current key used for encryption
     */
    private LruCache<String, KeyCombo> keyCache;

    /**
     * Current key is used for all encryption
     */
    private KeyCombo currentKey;

    /**
     *
     */
    KeyCache(final int cacheSize, final int cacheTtl)
    {
        keyCache = new LruCache<String, KeyCombo>(cacheSize, cacheTtl);
    }

    /**
     * Given the encrypted form of a Vault datakey, look up/return the cached decrypted
     * byte[32] form of it directly usable as an AES256 key.
     */
    byte[] getRawKeyFromEncrypted(String encryptedKey)
    {
        KeyCombo kc = keyCache.get(encryptedKey);

        if (kc != null)
            return kc.rawKey;
        else
            return null;
    }

    /**
     * Return latest encryption key
     */
    KeyCombo getLatestKey()
    {
        return currentKey;
    }

    /**
     * Record and cache a new encryption key.
     */
    KeyCombo rotateCurrentKey(byte[] rawKey, String encryptedKey)
    {
        currentKey = putRawKey(rawKey, encryptedKey);

        LOGGER.info("Rotating in new current encryption key [" + encryptedKey + "]");

        return currentKey;
    }

    /**
     * Record a new decryption key
     */
    KeyCombo putRawKey(byte[] rawKey, String encryptedKey)
    {
        KeyCombo kc = new KeyCombo(rawKey, encryptedKey, System.currentTimeMillis());

        LOGGER.info("Caching byte[32] for encryption key [" + encryptedKey + "]");

        keyCache.put(kc.encryptedKey, kc);

        return kc;
    }

    /**
     * Delegates request to remove old (by TTL) entries in cache.
     */
    void removeExpired()
    {
        keyCache.removeExpired();
    }
}
