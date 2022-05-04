package com.apixio.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;   // temporary: for encrypting text to v1   
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.dao.vault.RestException;
import com.apixio.dao.vault.Vault.DataField;
import com.apixio.dao.vault.Vault;
import com.apixio.security.KeyCache.KeyCombo;
import com.apixio.security.exception.ApixioSecurityException;
import com.apixio.security.utility.SecurityUtil;

/**
 * "Version 2" of main security library functionality.  This is mostly a drop-in replacement
 * to v1 security and has not been refactored to be simplified.
 *
 * V2 requires the "vault" server from hashicorp to run and all key material is stored on
 * that server.  Access to the vault server requires a vault-issued token.
 *
 * V2 rollout will be in phases:
 *
 *  1) all Apixio code migrates to use v2 Security; this is anticipated to take a long time
 *     and the library is configured in this stage to read and write v1 security data (via
 *     Config.DEFAULT_V1_MODE)
 *  2) all Apixio code has migrated to v2 Security and the library is now configured to
 *     read v1&v2 and write v2.  This might or might not be done immediately when phase 1
 *     is complete
 *  3) all encrypted data has been reencrypted into v2 form; this is anticipated to take
 *     a long time.
 *  4) v1 support is removed, meaning this code can be simplified by getting rid of
 *     dynamic loadinv via ClassLoader, etc.; this might never be done...
 *
 * Development using a local vault server can be done with proper setup.
 */
public class Security
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Security.class);

    /**
     * Version of system used to encrypt the data
     */
    enum EncryptionVersion
    {
        V1,         // from beginning of time until phase 2 deployment
        V2,         // beginning 2020-mm-dd
        UNKNOWN     // not yet known by code, possibly unencrypted
    }

    /**
     * How many bytes to mark/reset a stream when trying to determine v1 vs v2 metadata
     */
    private final static int MAX_MARKER_PEEK = Math.max(MetaV1.MAX_MARKER_PEEK, MetaV2.MAX_MARKER_PEEK);

    /**
     * How frequently to remove expired keys from LRU caches
     */
    private static final long CLEANUP_SLEEP =  120 * 1000L;  // 2 minutes

    /**
     * Defined by JCE as the string needed to specify AES algorithm
     */
    private final static String SECRETKEY_AES_TAG = "AES";

    /**
     * Contains everything needed to actually encrypt data and persist it for later decryption.
     * Metadata is required to store (minimally) the initialization vector
     */
    static class EncryptingData
    {
        byte[]  metadata;
        Cipher  encrypter;

        EncryptingData(byte[] metadata, Cipher cipher)
        {
            this.metadata  = metadata;
            this.encrypter = cipher;
        }
    }

    private static volatile Security instance = null;

    /**
     * Instance fields
     */
    private static SecureRandom secureRandom = new SecureRandom();

    protected boolean  useEncryption;
    private   Config   config;
    private   Vault    vault;
    private   String   algorithm;

    /**
     * "Scope" is something like a PDSID.  There is always one global scope (id is "global")
     * and zero or more uniquely ID-ed scopes.  Each scope has its own cache of keys
     */
    private static class ScopeCache
    {
        KeyCache keys;
        long     renewMs;

        ScopeCache(int size, int ttlSec, int renewSec)
        {
            keys    = new KeyCache(size, ttlSec);
            renewMs = 1000L * renewSec;
        }
    }
    private ScopeCache              globalKeyCache;
    private Map<String, ScopeCache> scopeKeyCaches = new HashMap<>();

    /**
     * Legacy support
     */
    private boolean  v1Mode;
    private String   v1Password;
    private String   v1KeyVer;

    /**
     * Private as we want to force getInstance and because "getV1Info" is for testability
     * purposes.  If getV1Info is false, then this instance is being used for initial
     * setup of keys into Vault.
     */
    private Security(Config config, String vaultToken, boolean getV1Info)
    {
        useEncryption = config.isUseEncryption();

        if (!useEncryption)
            return;

        checkNoBouncyCastle();

        try
        {
            Map<String, String> v1Key;

            this.config = config;
            //            LOGGER.info("security config " + config);

            HelperV1.loadProvider();
            HelperV2.loadProvider();

            if ((vaultToken != null) && (vaultToken.trim().length() == 0))
                vaultToken = null;

            vault = new Vault(config.getVaultServer(),
                              vaultToken,
                              config.getTransitPath(),
                              config.getSecretsPath());

            if (vaultToken == null)
                vault.setupToken();

            globalKeyCache = new ScopeCache(config.getCacheSize(null),
                                            config.getCacheTtl(null),
                                            config.getRenewal(null));

            algorithm = config.getAlgorithm();

            v1Mode = Boolean.TRUE.equals(config.isV1Mode());

            if (getV1Info)
            {
                v1Key = vault.getSecret(config.getV1KeyinfoPath());

                // subfield names are defined by devops
                v1Password = v1Key.get(config.getV1PasswordField());
                v1KeyVer   = v1Key.get(config.getV1VersionField());

                if (v1Password == null)
                    throw new IllegalStateException("Missing v1 password information.  Check that the value " +
                                                    config.getV1PasswordField() + " is set at Vault's kv engine at " +
                                                    config.getSecretsPath() + config.getV1KeyinfoPath());
                else if (v1KeyVer == null)
                    throw new IllegalStateException("Missing v1 version information.  Check that the value " +
                                                    config.getV1VersionField() + " is set at Vault's kv engine at " +
                                                    config.getSecretsPath() + config.getV1KeyinfoPath());
            }

            if (true)  // TODO do we want to config this?
                setupCacheCleaner();
        }
        catch (Exception re)
        {
            throw new ApixioSecurityException("Security initialization failure", re);
        }
    }

    private void checkNoBouncyCastle()
    {
        try
        {
            Class.forName(HelperV1.BC_PROVIDER_CLASS);

            // if we get here then we have a problem because we won't be able to load
            // FIPS140 BouncyCastle classes without unpredictable failures
            throw new IllegalStateException("Unable to initialize V2 Security because non-FIPS140 BouncyCastle classes " +
                                            "are in the classpath.  Define Java cmdline system properties " +
                                            "-D" + HelperV1.NONFIPS_JARPATH_CONFIG + " (regular BC 'prov' .jar) and " +
                                            "-D" + HelperV2.FIPS_JARPATH_CONFIG + " (FIPS140 BC .jar) to fix this error." +
                                            "  You can also define environment vars " +
                                            Config.getEnvConfigName(HelperV1.NONFIPS_JARPATH_CONFIG) + " and " +
                                            Config.getEnvConfigName(HelperV2.FIPS_JARPATH_CONFIG));
        }
        catch (ClassNotFoundException x)
        {
            // this exception must be thrown for successful bootup
        }
    }

    private static Config loadConfig() throws ApixioSecurityException
    {
        try
        {
            return new Config();
        }
        catch (IOException x)
        {
            throw new ApixioSecurityException("Failure while loading security configuration", x);
        }
    }

    /**
     * Standard way to retrieve a Security instance.
     */
    synchronized
    public static Security getInstance()
    {
        if (instance == null)
            instance = new Security(loadConfig(), null, true);

        return instance;
    }

    /**
     * Supplying a Vault token bypasses some setup...
     */
    synchronized
    public static Security getInstance(String vaultToken)
    {
        if (instance == null)
            instance = new Security(loadConfig(), vaultToken, true);

        return instance;
    }

    /**
     * These methods allow client code to use a JCE security provider that must be registered with
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
     * The first method allows the client to specify the .jar to be used, while the second method allows
     * the client to tell the system to use a predefined Java system property whose value will be the
     * path to the jar (this method is more in line with the system properties for declaring the "bcprov"
     * and "bc-fips" external jar files).  The property name for this is "bcglobalprovpath"
     */
    public void addProviderUrl(String jarPath) throws Exception
    {
        // making this a non-static method just forces client to do getInstance() first

        HelperJce.addProviderUrl(jarPath);
    }
    public void addProviderUrlFromSysProp() throws Exception
    {
        HelperJce.addProviderUrl(null);
    }

    /**
     * Peek reads enough of the stream to determine if it contains v1 or v2 encrypted data,
     * or neither.  The current position of the stream is unaffected (uses .mark/.reset).  If
     * the underlying stream doesn't support InputStream.mark() then an ApixioSecurityException
     * is thrown.
     */
    static EncryptionVersion getEncryptionVersion(InputStream is) throws ApixioSecurityException, IOException
    {
        // TODO remove/simplify this after all data has been converted to v2
        if (!is.markSupported())
            throw new ApixioSecurityException("Unable to determine security metadata version as InputStream " + is + " doesn't support .markSupported/.reset");

        is.mark(MAX_MARKER_PEEK);

        try
        {
            if (MetaV2.verifyMarker(is, false))
                return EncryptionVersion.V2;
        }
        finally
        {
            is.reset();
        }

        is.mark(MAX_MARKER_PEEK);

        try
        {
            if (MetaV2.verifyMarker(is, true))
                return EncryptionVersion.V2;
        }
        finally
        {
            is.reset();
        }

        is.mark(MAX_MARKER_PEEK);
        
        try
        {
            if (MetaV1.isPossiblyV1(is))
                return EncryptionVersion.V1;
        }
        finally
        {
            is.reset();
        }

        return EncryptionVersion.UNKNOWN;
    }

    // NOTE: Char encryption isn't real encryption and doesn't use key service
    public String encrypt(String text, boolean isCharEncryption)
        throws ApixioSecurityException
    {
        if (isCharEncryption)
            return encryptChar(text);
        else
            return encryptWithScope(text, null);
    }

    private String encryptChar(String plainText)
    {
        String encryptedString = "";

        for (int i = 0; i < plainText.length(); i++)
            encryptedString = encryptedString + SecurityUtil.getOfC(plainText.charAt(i));

        return encryptedString;
    }

    public String encryptToHex(String text) throws ApixioSecurityException
    {
        String encryptedStr = encryptWithScope(text, null);

        try
        {
            return Utils.getHexString(encryptedStr.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage(), ex);
        }
    }

    public String encrypt(String text) throws ApixioSecurityException
    {
        return encryptWithScope(text, null);
    }

    /**
     * Encrypts the given plaintext with the current AES256 key and base64-encodes
     * the resulting byte[].  Salt parameter is ignored and if non-null a warning
     * will be logged (as client code should no longer think in terms of PBE w/ salt).
     *
     * The resulting string can be decrypted via decrypt(String).  If V2 metadata
     * needs to be retrieved from the string, then client must convert it first to
     * byte[] by doing a base64 decode on it
     */
    @Deprecated  // due to salt param
    public String encrypt(String text, String salt) throws ApixioSecurityException
    {
        // Scott: Isn't this a bug!!!
        if (salt != null)
            throw new IllegalArgumentException("Security.encrypt() no longer supports non-null 'salt' values: " + salt);

        return encryptWithScope(text, null);
    }
    public String encryptWithScope(String text, String scope) throws ApixioSecurityException
    {
        if (!useEncryption)
            return text;

        if (text == null)
            return null;

        EncryptedInputStream encryptedInputStream = null;

        try (ByteArrayInputStream  inputStream  = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            if (v1Mode)
            {
                // metadata is not base64 encoded, but ciphertext is.  this code is a
                // temporary hack (until v1mode isn't needed after migration is done) as
                // we need to duplicate what EncryptedInputStream does but handle the
                // different streams differently

                EncryptingData ed  = prepareEncryption(scope);
                InputStream    b64 = new Base64InputStream(new CipherInputStream(inputStream, ed.encrypter), true);
                InputStream    seq = new SequenceInputStream(new ByteArrayInputStream(ed.metadata), b64);

                IOUtils.copy(seq, outputStream);

                b64.close();  // forces cipher.doFinal
            }
            else
            {
                OutputStream base64Out = new Base64OutputStream(outputStream, true, -1, null);  // no linebreaks

                encryptedInputStream = EncryptedInputStream.encryptInputStreamWithScope(this, inputStream, scope);

                // encrypt entire output stream of bytes, including v2 metadata
                IOUtils.copy(encryptedInputStream, base64Out);

                base64Out.close();
            }

            return outputStream.toString("UTF-8");  // dumb that ByteArrayOutputStream.toString() only takes charset name
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage(), ex);
        }
        finally
        {
            IOUtils.closeQuietly(encryptedInputStream);
        }
    }

    public byte[] encryptBytesToBytes(byte[] bytes) throws ApixioSecurityException
    {
        return encryptBytesToBytesWithScope(bytes, null);
    }
    public byte[] encryptBytesToBytesWithScope(byte[] bytes, String scope) throws ApixioSecurityException
    {
        if (!useEncryption)
            return bytes;

        if (bytes == null)
            return null;

        EncryptedInputStream encryptedInputStream = null;

        try (ByteArrayInputStream  inputStream  = new ByteArrayInputStream(bytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            encryptedInputStream = EncryptedInputStream.encryptInputStreamWithScope(this, inputStream, scope);

            IOUtils.copy(encryptedInputStream, outputStream);

            return outputStream.toByteArray();
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage(), ex);
        }
        finally
        {
            IOUtils.closeQuietly(encryptedInputStream);
        }
    }

    public OutputStream encryptOutputStream(OutputStream outputStream)
    {
        return encryptOutputStreamWithScope(outputStream, null);
    }
    public OutputStream encryptOutputStreamWithScope(OutputStream outputStream, String scope)
    {
        return useEncryption ? EncryptedOutputStream.encryptOutputStreamWithScope(this, outputStream, scope) : outputStream;
    }

    public InputStream encryptInputStream(InputStream inputStream)
    {
        return useEncryption ? new EncryptedInputStream(inputStream, this) : inputStream;
    }

    public String decrypt(String text, boolean isCharEncryption) throws ApixioSecurityException
    {
        if (!useEncryption)
            return text;

        String decryptedString = "";

        if (isCharEncryption)
        {
            for (int i = 0; i < text.length(); i++)
                decryptedString = decryptedString + SecurityUtil.getDOfC(text.charAt(i));
        }
        else
        {
            decryptedString = decrypt(text);
        }

        return decryptedString;
    }

    public byte[] decryptBytesToBytes(byte[] bytes) throws ApixioSecurityException
    {
        return decryptBytesToBytes(bytes, false);
    }

    public InputStream decryptBytesToInputStream(byte[] bytes, boolean decompress) throws ApixioSecurityException
    {
        if (bytes == null)
            return null;

        InputStream decryptedInputStream = null;

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes))
        {
            decryptedInputStream = useEncryption ? new DecryptedInputStream(inputStream, this) : inputStream;

            if (decompress)
                decryptedInputStream = new DecompressedInputStream(decryptedInputStream, StandardCharsets.UTF_8);
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage(), ex);
        }

        return decryptedInputStream;
    }

    public byte[] decryptBytesToBytes(byte[] bytes, boolean decompress) throws ApixioSecurityException
    {
        if (bytes == null)
            return null;

        try (ByteArrayInputStream  inputStream  = new ByteArrayInputStream(bytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            InputStream decryptedInputStream = useEncryption ? new DecryptedInputStream(inputStream, this) : inputStream;

            if (decompress)
                decryptedInputStream = new DecompressedInputStream(decryptedInputStream, StandardCharsets.UTF_8);

            IOUtils.copy(decryptedInputStream, outputStream);

            decryptedInputStream.close();

            return outputStream.toByteArray();
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage(), ex);
        }
    }

    public String decryptFromHex(String hexText) throws ApixioSecurityException
    {
        byte[] bytes = Utils.getBinArray(hexText);
        String decrypted = null;

        String rawText = new String(bytes, StandardCharsets.UTF_8);
        decrypted = decrypt(rawText);

        return decrypted;
    }

    public String decrypt(String text) throws ApixioSecurityException
    {
        return decrypt(text, null);
    }

    /**
     * Decrypts the output of encrypt(String plaintext, String salt).  Input stream contains
     * one of the following:
     *
     *  1.  metaV1, base64(encrypt(string.bytes))
     *  2.  base64(metaV2, encrypt(string.bytes))
     *
     * The lack of consistent base64 encoding for the two forms complicates things as we
     * don't know if we need to decode or not in order to read metadata.  We know that for v1
     * metadata, the first 4 bytes/chars must be [0-9] and so we test those w/o decoding.  If
     * those look good, then we assume v1 and we don't wrap with a decoder.  Otherwise we
     * assume v2 and wrap with a decoder.
     *
     * Note that it was an explicit decision for v2 to do base64 encoding of the entire
     * byte array as it separates the encoding from the encrypting operations.
     */
    public String decrypt(String text, String salt) throws ApixioSecurityException
    {
        if (!useEncryption)
            return text;

        if (text == null)
            return null;

        InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            EncryptionVersion version         = getEncryptionVersion(inputStream);
            InputStream       decryptingInputStream;

            if (version == EncryptionVersion.V2)
                inputStream = new Base64InputStream(inputStream);

            decryptingInputStream = new DecryptedInputStream(inputStream, this, salt, version);

            IOUtils.copy(decryptingInputStream, outputStream);

            decryptingInputStream.close();

            return outputStream.toString("UTF-8");  // dumb that ByteArrayOutputStream.toString() only takes charset name
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage(), ex);
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Creates a new transit key in Vault that can/will be used to create datakeys.  The
     * Vault token must have sufficient privileges for both the creation of the key and
     * the subsequent storing of the key material in the secrets engine.  Note that this
     * copying behavior is consistent with the code in getCurrentEncryptingKey().
     */
    public void createScopeKey(String scope) throws Exception
    {
        Vault.DataField     df;
        Map<String, String> secret;

        vault.createTransitKey(config.getKeyname(scope), "aes256-gcm96");

        df = vault.createTransitDatakey(config.getKeyname(scope));

        secret = new HashMap<String,String>();
        secret.put(config.getCurrentDatakeyPlaintextFieldname(), df.plaintext);
        secret.put(config.getCurrentDatakeyCipherFieldname(),    df.ciphertext);

        vault.putSecret(config.getSecretNameForCurrentDatakey(scope), secret);
    }

    /**
     * Do all that's needed for encrypting data.  This includes creation of metadata
     * and creation/setup of JCE Cipher object.  Metadata creation includes getting the
     * latest SecretKey and creating IV.
     */
    EncryptingData prepareEncryption(String scope) throws ApixioSecurityException
    {
        if (v1Mode)
        {
            MetaV1.V1EncryptingData v1ed = MetaV1.createMetaV1(v1KeyVer);

            return new EncryptingData(v1ed.metadata,
                                      HelperV1.getV1AESCipher(v1Password, v1ed.random,
                                                              v1ed.random.getBytes(StandardCharsets.UTF_8), true));
        }
        else
        {
            KeyCombo       key  = getCurrentEncryptingKey(scope);
            SecretKey      sk   = new SecretKeySpec(key.rawKey, SECRETKEY_AES_TAG);
            byte[]         iv   = new byte[16];            // must be 16 because AES256 still uses 128-bit blocks
            Cipher         cipher;
            EncryptingData ed;

            // it appears that bcfips jar for AES256 requires an IV or decryption Cipher setup will fail
            secureRandom.nextBytes(iv);

            cipher = HelperV2.getV2AESCipher(algorithm, sk, new IvParameterSpec(iv), true);

            // metadata MUST have the cipher+mode+padding string saved as the restored copy
            // will be used to get the decryption cipher
            ed = new EncryptingData(MetaUtil.createMetaV2(key.encryptedKey, scope, algorithm, iv), cipher);

            Arrays.fill(iv, (byte) 0);  // reduce attack surface

            return ed;
        }
    }

    /**
     * Current position in data stream must be at beginning of v1 or v2 metadata.  ALL methods that
     * will decrypt data must call this method.
     *
     * This method gets version info (as best as possible) and creates the correct Cipher based
     * on that.  The InputStream position is left at the beginning of the encrypted content.
     */
    Cipher getDecryptionCipher(InputStream data, String v1Salt, EncryptionVersion version) throws IOException
    {
        version = (version != EncryptionVersion.UNKNOWN) ? version : getEncryptionVersion(data);

        if (version == EncryptionVersion.V2)
        {
            try
            {
                MetaV2   v2            = MetaUtil.readMetaV2(data);
                String   metaAlgorithm = v2.getAlgorithm();
                String   scope         = v2.getScope();
                KeyCache cache         = getCache(scope).keys;
                byte[]   rawKey        = cache.getRawKeyFromEncrypted(v2.getEncryptedKey());

                //!! for cache testing purposes, null out rawKey here

                try
                {
                    if (rawKey == null)
                    {
                        String encrypted = v2.getEncryptedKey();

                        // go to vault to ask it to decrypt it; that request could fail:
                        //   * no permission on datakey
                        //   * datakey has been trimmed
                        // call MUST return non-null OR throw an exception
                        rawKey = vault.decryptTransitValue(config.getKeyname(scope), encrypted);

                        cache.putRawKey(rawKey, encrypted);
                    }

                    return HelperV2.getV2AESCipher(metaAlgorithm,
                                                   new SecretKeySpec(rawKey, SECRETKEY_AES_TAG),
                                                   new IvParameterSpec(v2.getIv()),
                                                   false);
                }
                catch (ApixioSecurityException x)
                {
                    throw new IllegalStateException("Encryption algorithm " + metaAlgorithm + " is not supported", x);
                }
            }
            catch (IOException | RestException x)
            {
                throw new ApixioSecurityException("Failed to restore V2 metadata", x);
            }
        }
        else if (version == EncryptionVersion.V1)
        {
            try
            {
                MetaV1 v1     = new MetaV1(data);
                String random = v1.getRandomNumber();
                String salt   = (v1Salt != null) ? (random + v1Salt) : random;

                return HelperV1.getV1AESCipher(v1Password, salt, random.getBytes(StandardCharsets.UTF_8), false);
            }
            catch (Exception x)
            {
                throw new ApixioSecurityException("Unable to get v1 decryption Cipher", x);
            }
        }
        else
        {
            throw new ApixioSecurityException("Unable to get encryption version of InputStream data");
        }
    }

    /**
     * Gets latest cached key info from the cache and rotates to a new datakey if
     * it's older than the renew period.
     */
    private KeyCombo getCurrentEncryptingKey(String scope) throws ApixioSecurityException
    {
        ScopeCache  cache = getCache(scope);
        long        now   = System.currentTimeMillis();
        KeyCombo    key   = cache.keys.getLatestKey();

        if ((key == null) || (now > key.creationTime + cache.renewMs))
        {
            try
            {
                if (false)  // keeping code around "just in case"...
                {
                    // this block of code creates new datakeys on demand, which will
                    // likely causing poor cache hit ratio during decryption if there
                    // is a large number of JVMs that perform encryption.
                    DataField keyInfo = vault.createTransitDatakey(config.getKeyname(scope));

                    key = cache.keys.rotateCurrentKey(Utils.decodeBase64(keyInfo.plaintext), keyInfo.ciphertext);
                }
                else
                {
                    // this block of code fetches the current datakey from the KV secrets engine;
                    // this requires that dev-ops creates new datakeys (for each scope) and puts
                    // the values in the KV engine.  The value fetched is the same as what is
                    // returned when creating a new datakey and will therefore contain the
                    // needed meta+plaintext (for immediate AES256 encryption) fields
                    Map<String, String> keyInfo = vault.getSecret(config.getSecretNameForCurrentDatakey(scope));

                    key = cache.keys.rotateCurrentKey(Utils.decodeBase64(keyInfo.get(config.getCurrentDatakeyPlaintextFieldname())),
                                                      keyInfo.get(config.getCurrentDatakeyCipherFieldname()));
                }
            }
            catch (Exception x)
            {
                throw new ApixioSecurityException("Failed to get new Datakey", x);
            }
        }

        return key;
    }

    private ScopeCache getCache(String scope)
    {
        if (scope == null)
            return globalKeyCache;

        synchronized (scopeKeyCaches)
        {
            ScopeCache cache = scopeKeyCaches.get(scope);

            if (cache == null)
            {
                cache = new ScopeCache(config.getCacheSize(scope),
                                       config.getCacheTtl(scope),
                                       config.getRenewal(scope));
                scopeKeyCaches.put(scope, cache);
            }

            return cache;
        }
    }

    /**
     *
     */
    private void setupCacheCleaner()
    {
        Thread cleaner = new Thread() {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(CLEANUP_SLEEP);

                        globalKeyCache.keys.removeExpired();

                        synchronized(scopeKeyCaches)
                        {
                            for (Map.Entry<String,ScopeCache> entry : scopeKeyCaches.entrySet())
                                entry.getValue().keys.removeExpired();
                        }
                    }
                    catch (Exception x)
                    {
                        LOGGER.error("Error in KeyCache cleanup thread", x);
                    }
                }
            };

        cleaner.setDaemon(true);
        cleaner.start();
    }

    /**
     * THIS IS ONLY FOR TESTING!  See buildable/security/src/test/README.txt for more info
     */
    public static void main(String... args) throws Exception
    {
        boolean showUsage = false;

        if (args.length == 0)
        {
            showUsage = true;
        }
        else if (args[0].equals("init"))           // usage:  init v1password v1version
        {
            Security security = new Security(loadConfig(), null, false);

            if (args.length != 3)
            {
                System.err.println("Error:  init usage:  init v1pass v1vers");
                System.exit(1);
            }

            security.initVault(args[1], args[2]);
        }
        else if (args[0].equals("newscope"))  // usage:  Security newscope thescopename
        {
            Security security = new Security(loadConfig(), null, true);

            if (args.length != 2)
            {
                System.err.println("Error:  newscope usage:  newscope scopename");
                System.exit(1);
            }

            security.createScopeKey(args[1]);
        }
        else if (args[0].equals("encrypt"))   // usage:  Security thescopename stringtoencrypt
        {
            if (args.length != 3)
            {
                System.err.println("Error:  encrypt usage:  scope cleartext");
                System.exit(1);
            }

            testEnc(args[1], args[2]);
        }
        else if (args[0].equals("decrypt"))   // usage:  theencryptedstring
        {
            if (args.length != 2)
            {
                System.err.println("Error:  decrypt usage:  ciphertext");
                System.exit(1);
            }

            testDec(args[1]);
        }
        else
        {
            System.err.println("Error: unknown command '" + args[0] + "'");
            showUsage = true;
        }

        if (showUsage)
        {
            System.err.println("Usage:  com.apixio.security.Security {init|newscope|encrypt|decrypt} ...");
            System.exit(1);
        }
    }

    /**
     * To be used *after* src/test/scripts/local-dev-init.sh has been run
     */
    private void initVault(String v1Password, String v1Version) throws Exception
    {
        Vault.DataField     df;
        Map<String, String> secret;

        // This code is used to initialize a Vault server with the minimum necessary
        // keys/secrets to be able to test.  There is no reason to call this method
        // outside this testing scenario.

        // This code assumes only that the kvv1 secrets engine and the transit
        // engine have been mounted at the configured locations.
        //
        // Initialization includes creating the global transit key and its initial
        // datakey and copying that datakey over to the secrets engine.  this mimics
        // the setup done currently by local-dev-init.sh, mostly for testing purposes

        createScopeKey(null);
        createScopeKey("xyz");  // testing only

        // v1 password/version requires special setup
        secret = new HashMap<String,String>();
        secret.put(config.getV1PasswordField(), v1Password);
        secret.put(config.getV1VersionField(),  v1Version);
        vault.putSecret(config.getV1KeyinfoPath(), secret);
    }

    private static void testEnc(String pdsID, String str) throws Exception
    {
        Security security = new Security(loadConfig(), null, true);

        if ("".equals(pdsID))
            pdsID = null;

        System.out.println("'" + str + "' encrypted with pds key " + pdsID + ": " + security.encryptWithScope(str, pdsID));
    }

    private static void testDec(String enc) throws Exception
    {
        Security security = new Security(loadConfig(), null, true);

        System.out.println("decrypting..." + security.decrypt(enc));
    }

}
