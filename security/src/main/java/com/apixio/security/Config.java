package com.apixio.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config is the central class for ALL v2 security configuration for the system (note, however,
 * that configuration needed for *communicating* with Vault server is dealt with in
 * com.apixio.dao.vault.Vault as that class is intended to be separate/reusable).  This class
 * collects all definitions of, and defaults for, this configuration.  The sources for this
 * configuration are from apixio-security.properties file(s), JVM System properties, and
 * environment variables, with the precedence of these sources determined by the how easily
 * modified they are.  The exact sources and precedence, from low to high, are:
 *
 *  1) classloader-based resource with name "apixio-security.properties"
 *  2) $(pwd)/apixio-security.properties OR $HOME/apixio-security.properties ("pwd" is where JVM started)
 *  3) JVM system properties; these are prefixed with "apx-security-" string
 *  4) OS process environment variables; var names are UPPERCASE config names with "_" replacing "."s and "-"s
 *
 * Initialization will always log info about where it finds configuration.
 *
 * The initialization of Config will squash the config values from the above sources into
 * a single final set of values, which is kept in a Map<String,Object> and is used
 * when returning values.  All values except for scope-based ones will reside in that Map.
 *
 * Note that many of the enum-based constants below are the PROPERTY NAMES within a
 * properties file (i.e., not the value).  Here are the contents of an example .properties
 * file demonstrating some of these configurations:
 *
 *   vault-server=http://localhost:8200
 *   v1-encryption-mode=false
 *   algorithm=AES/GCM/NoPadding
 *   transit-path=transit/
 *   secrets-path=apixio_encryption/
 *   v1-keyinfo-path=v1
 *   v1-password-field=password                      # i.e., json field name is "password"
 *   v1-version-field=version                        # i.e., json field name is "version"
 *   scope.global-keyname=local-phi-key              # i.e., subpath within vault namespace starts with "local-phi-key"
 *   scope.non-global-datakey-prefix=local-pds-      # i.e., subpath within vault namespace starts with "local-pds-"
 *   scope.global.renew=1h
 *   scope.global.cache.size=1024
 *   scope.global.cache.ttl=2h
 *   scope.xyz.renew=123
 *   scope.xyz.cache.size=1234
 *   scope.xyz.cache.ttl=12345
 *   curdatakey.global-secretname=curkeys/local-phi-key        # subpath in vault
 *   curdatakey.non-global-secret-prefix=curkeys/local-pds-    # subpath in vault
 *   curdatakey.ciphertext-fieldname=ciphertext                # json field name is "ciphertext"
 *   curdatakey.plaintext-fieldname=plaintext                  # json field name is "plaintext"
 *
 * Values taken from environment variables requires that the name of the config not contain
 * "-", so the canonical name is changed by replacing "-" with "_" and then doing toUpperCase
 * to follow env var conventions.
 *
 * Note that setting the enviroment variable APXSECV2_LOGCONFIG to any value will turn on bootup logging that
 * will give exact source of config and keynames within that source.
 *
 * The concepts relevant to configuration are:
 *
 *  1) "v1" mode; version 1 of Security.java didn't use Vault or FIPS-compliant algorithms which is
 *     why v2 security was created
 *
 *  2) Vault "datakey"; a Vault-managed logical name that contains a versioned list of AES-256 keys
 *     that are used to create actual AES2565-bit key material via encrypting secure-random byte[32].
 *     Dev-ops manages these datakeys.  The overall v2 security architecture is that dev-ops will
 *     also copy/store AES256-bit key material to a Vault "secret"; this means that this security
 *     code will *not* directly fetch encrypting datakey info, but will instead fetch a secret.
 *     This fetched secret will be the expected datakey information (meta+plaintext, where plaintext
 *     is in-memory-only AES256 key material).
 *
 *  3) scope; a unique identifier that maps to a datakey; null scope means global datakey (not specific
 *     to any scope)
 *
 *  4) cache; each scope (including global/default) has settings to control TTL and number of entires
 *     in the cache, along with a renew duration, which tells the code to fetch a new datakey for
 *     the scope.
 *
 * Implementation notes
 *
 *  - the characters "scope." occur in several places that are related in that if ever this string
 *    changes, all the other places in the code that have "scope." will need to be reviewed to see
 *    if they also need to change, as some logic is dependent on testing this string.
 */
public class Config
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    /**
     * Resource (file) name used to get resource from ClassLoader.
     */
    public final static String CONFIG_FILENAME = "apixio-security.properties";

    /**
     * This is the string that must be prefixed to config names that are used on the
     * Java cmdline via -Dx=y.  For example:
     *
     *  java -Dapx-security-vault-server=xyz ...
     *
     * Note that this means that a key name that's in the .properties file will NOT
     * have this prefix.
     */
    public final static String SYSPROP_PREFIX  = "apx-security-";

    /**
     * This is the string that must be prefixed to environment variable names that
     * are used to override configuration.  Note that ALL such environment variables
     * MUST be all uppercase and that ALL occurrences of "-" and "." in the ConfigKey
     * enum strings WILL be replaced by "_" in the environment variable name.  For
     * example, the CONFIG_V1_MODE (with the base string of "v1-encryption-mode") will
     * be controlled by the environment variable APXSECV2_V1_ENCRYPTION_MODE
     *
     *  APXSECV2_VAULT_SERVER="https://vault.apixio.com:8200" java -cp classpath some.class.name ...
     *
     */
    public final static String ENVVAR_PREFIX  = "APXSECV2_";

    /**
     * Default values for the configuration.  These final fields are intentionally not public.
     */
    private final static String  DEFAULT_VAULT_SERVER           = "https://vault-stg.apixio.com:8200";  // defined by dev-ops
    private final static Boolean DEFAULT_USE_ENCRYPTION         = Boolean.TRUE;                         // To turn encryption on/off

    private final static Boolean DEFAULT_V1_MODE                = Boolean.TRUE;                         // only until all components are upgraded
    private final static String  DEFAULT_V1_KEYINFO_PATH        = "v1";                                 // defined by devops
    private final static String  DEFAULT_V1_FIELD_PASSWORD      = "password";                           // defined by devops
    private final static String  DEFAULT_V1_FIELD_VERSION       = "version";                            // defined by devops

    // v2 config
    private final static String  DEFAULT_ALGORITHM              = "AES/CBC/PKCS7PADDING";               // defined by engineering
    private final static String  DEFAULT_TRANSIT_PATH           = "transit";                            // defined by devops; "mount point" on Vault URL for datakey endpoints
    private final static String  DEFAULT_SECRETS_PATH           = "apixio_encryption";                  // defined by devops; "mount point" on Vault URL for secrets engine endpoints

    private final static String  DEFAULT_GLOBAL_KEYNAME         = "default";                            // defined by devops;
    private final static String  DEFAULT_CURRDK_GLOBAL_NAME     = "curkeys/default";                    // defined by devops; global/default datakey name; appended to secrets_path
    private final static int     DEFAULT_GLOBAL_RENEW           = convertDuration("ignored", "6h");     // i.e., check for new key every 6 hours
    private final static int     DEFAULT_GLOBAL_CACHE_SIZE      = 10000;                                // TODO:  need better estimate
    private final static int     DEFAULT_GLOBAL_CACHE_TTL       = convertDuration("ignored", "1d");     // TODO: likely need better value

    private final static String  DEFAULT_KEYNAME_PREFIX         = "pds-";                               // defined by devops;
    private final static String  DEFAULT_CURRDK_SECRET_PREFIX   = "curkeys/pds-";                       // defined by devops; non-null scope-specific datakey name prefix; appended to secrets_path
    private final static String  DEFAULT_CURRDK_CIPHERFLD       = "ciphertext";                         // defined by devops; JSON field name that contains meta to be put on final encrypted data
    private final static String  DEFAULT_CURRDK_PLAINFLD        = "plaintext";                          // defined by devops; JSON field name that contains in-mem only AES256 key material

    /**
     * The following are the string names of "configuration points".  These are the
     * EXACT STRINGS that must be used in the .properties file (or after the above
     * prefix if specified via Java cmdline) and are also the EXACT map keys in the
     * squashed/aggregated config, with the exception of the scope-based ones.
     *
     * The values of each config point have a type (indicated by the Class<?> param)
     * and conversion to the required type is done during initialization.  The supported
     * types are String, Integer, and Boolean.
     */
    public enum ConfigKey
    {
        /**
         * Support for phased rollout.  v1-encryption-mode causes encryption with v2
         * code to encrypt using v1 code (with no salt value).
         *
         * Configured type is string true|false, runtime type is Boolean
         */
        CONFIG_V1_MODE("v1-encryption-mode", Boolean.class),

        /**
         * To turn encryption on/off
         */
        CONFIG_USE_ENCRYPTION("use-encryption", Boolean.class),

        /**
         * The JCE cipher+mode+padding string used to select the actual Cipher from the JCE
         * provider.  If this is an invalid string or refers to an unknown cipher
         * the initialization will fail.  This value is also stored in the metadata of
         * the encrypted data and is used during decryption to select the Cipher.
         *
         * Configured type is string, runtime type is String
         */
        CONFIG_ALGORITHM("algorithm", String.class),

        /**
         * Vault server is the full protocol://host:port specification of what Vault
         * server should be used.  Protocol should be https for production.  Example:
         *
         *  https://vault.apixio.com:8200
         *
         * Configured type is String, runtime type is String
         */
        CONFIG_VAULT_SERVER("vault-server", String.class),

        /**
         * Transit path is the "mount point" of the transit secret engine.  It's used only
         * when forming the URL endpoint to create/decrypt a datakey
         *
         * Configured type is String, runtime type is String
         */
        CONFIG_TRANSIT_PATH("transit-path", String.class),

        /**
         * Secrets path is the "mount point" of the version-1 kv secrets engine.
         *
         * Configured type is String, runtime type is String
         */
        CONFIG_SECRETS_PATH("secrets-path", String.class),

        /**
         * V1 key path is the path "under" the secrets-path to the old v1 master keyinfo
         * that's used by v1 security code to decrypt data encrypted with v1 code.  It's
         * used to help form the vault REST endpoint URL.  v1 keyinfo includes both the
         * PBE (password based encryption) password and the keyVersion string.
         *
         * This path is used to fetch a single secret from Vault's "secrets" engine
         * where that secret has two fields, one for the password and one for the key
         * version.  These are configurable only so that dev-ops can modify this info in
         * Vault without needing to change code.  The only constraint is that fetching
         * the keyinfo secret from Vault results in two fields at the top level.
         *
         * Configured types are String, runtime types are String
         */
        CONFIG_V1_KEYINFO_PATH("v1-keyinfo-path",     String.class),   // used in forming URL
        CONFIG_V1_FIELD_PASSWORD("v1-password-field", String.class),   // value is fieldname of returned value
        CONFIG_V1_FIELD_VERSION("v1-version-field",   String.class),   // value is fieldname of returned value

        /**
         * To support per-scope keys and caching of keys, the system has the idea of a
         * global config and optional per-scope config.  If config for a specific scope is
         * requested but there is no (override) config for it, then global config values are
         * returned.
         *
         * Per-scope config is identified by the scope name/id (the "<id>" below) and
         * global/default config is identified by using "global" as the <id> below.
         * ("global" was chosen as the better word as compared to "default" because we would
         * otherwise have to specify the default values for the default cache TTL, etc.,
         * which was somewhat confusing.)  The term "non-global" just means that a non-null
         * scope is being used.
         *
         * Keyname is the name of the transit key that's used to create a new datakey and to
         * decrypt the ciphertext form of a datakey.  Its value is used to form part of the
         * REST endpoint URL.
         *
         *   Configured type of keyname is String, runtime type is String.
         *
         * Cache size is the number of decrypted datakeys to keep in the cache before older
         * ones are removed.  The cache is a map from the ciphertext form of a datakey to
         * the "raw" byte[32] form that can be used to create an in-memory AES key.
         *
         *   Configured type of cache.size is String, runtime type is integer.
         *
         * Cache TTL is a secondary way of clearing cache entries and represents the max age
         * of a cache entry (as measured by last access of the key).  The runtime time unit
         * is seconds, but the actual configured value includes the time unit for clarity
         * and convenience.  If no duration unit is given, seconds is used.
         *
         *   Configured type of cache.ttl is String, runtime type is integer; unit is seconds.
         *
         * Datakey renewal defines the max time that a datakey will be used by the JVM--if
         * the current datakey was created more than this many seconds ago and an encryption
         * operation is requested, a new datakey is created.  The runtime time unit is
         * seconds, but the actual configured value includes the time unit for clarity and
         * convenience.  If no duration unit is given, seconds is used.  There can be
         * per-PDS datakey renewals times, which is why this is just a default.
         *
         * Configured type for renewal period is String, runtime type is integer; unit is
         * seconds.
         */

        // global scope has a different name of datakey than other scopes and the non-global scopes
        // have a prefix before the scope id instead
        CONFIG_SCOPE_GLOBAL_KEYNAME("scope.global-keyname",            String.class), // value is the datakey name for global/default scope
        CONFIG_SCOPE_KEYNAME_PREFIX("scope.non-global-datakey-prefix", String.class), // value is prefixed to scope id to get actual datakey name

        /**
         * In order to reduce the number of distinct datakeys used for encryption, the system
         * fetches the current datakey (ciphertext+plaintext) from the KV secrets engine store.
         * There is one global current datakey (used for when either scope isn't known or there
         * isn't logically a scope for the encryption), and /n/ datakeys, one for each scope.
         *
         * The system allows for different paths to these two types (though both must obviously
         * be "under" the mounted path for the KVV1 secrets engine), where the non-global path
         * configured is actually a prefix that will have the scope ID appended to it.
         *
         * Finally, the value of the secret must contain the ciphertext and the plaintext that
         * are returned from the datakey creation operation.  The actual names of these fields
         * is configurable (which is probably overkill).
         */
        // these define where the current datakey to use is in the secrets engine URL path:
        CONFIG_CURRDATAKEY_GLOBAL_KEYNAME("curdatakey.global-secretname",       String.class),  // value is the secret name for global scope's current datakey
        CONFIG_CURRDATAKEY_SECRET_PREFIX("curdatakey.non-global-secret-prefix", String.class),  // value is prefixed to scope id to get actual secret name

        // the value retrieved from the above-located secret(s) must have fields fetchable via the names from this config:
        CONFIG_CURRDATAKEY_CIPHERTEXT_FIELDNAME("curdatakey.ciphertext-fieldname", String.class),  // value is the json keyname whose value contains the ciphertext
        CONFIG_CURRDATAKEY_PLAINTEXT_FIELDNAME("curdatakey.plaintext-fieldname",   String.class),  // value is the json keyname whose value contains the plaintext (AES256 byte[32] base64-encoded)

        // all scopes (global and others) have these configs.  note that these CANNOT be overridden by env vars
        // as we don't really want to enum all env vars to try to match against these patterns...
        CONFIG_SCOPE_RENEW("scope.<id>.renew",           Integer.class),
        CONFIG_SCOPE_CACHE_SIZE("scope.<id>.cache.size", Integer.class),
        CONFIG_SCOPE_CACHE_TTL("scope.<id>.cache.ttl",   Integer.class);

        public String   getKey()  { return key;       }
        public Class<?> getType() { return typeClass; }

        private ConfigKey(String key, Class<?> typeClass)
        {
            // so we can reverse from envvars:
            if (!key.equals(key.toLowerCase()))
                throw new IllegalArgumentException("Config key names must be all lowercase:  " + key);
            else if (key.contains("_"))
                throw new IllegalArgumentException("Config key names cannot contain _ character(s):  " + key);

            this.key       = key;
            this.typeClass = typeClass;
        }

        private String   key;
        private Class<?> typeClass;
    }

    // the <id> that's used for global scope; no other scope can use this <id>:
    private final static String SCOPE_GLOBALNAME = "global";

    private final static String  SCOPE_IDSTR  = "<id>";
    private final static Pattern ID_PATTERN = Pattern.compile(SCOPE_IDSTR);

    static String replaceID(String template, String id)
    {
        return ID_PATTERN.matcher(template).replaceAll(id);
    }

    /**
     * String form of the different time duration units.  Matching is case-insensitive.
     */
    private final static char DUR_SECONDS = 's';
    private final static char DUR_MINUTES = 'm';
    private final static char DUR_HOURS   = 'h';
    private final static char DUR_DAYS    = 'd';

    /**
     * Final/aggregated configuration values are kept here
     */
    private Map<String,Object> aggregatedConfig = new HashMap<>();

    /**
     * Set env var "APXSECV2_LOGCONFIG=anything" to get info about what config is loaded
     */
    private static final boolean LOG_CONFIG;
    static
    {
        LOG_CONFIG = System.getenv(ENVVAR_PREFIX + "LOGCONFIG") != null;
    }

    /**
     * Converts the basename of a configuration point to its environment variable
     * form of it.
     */
    public static String getEnvConfigName(String base)
    {
        return ENVVAR_PREFIX + base.replaceAll("-|\\.", "_").toUpperCase();
    }

    /**
     * Converts the basename of a configuration point to its Java system property
     * form of it.
     */
    public static String getSysPropConfigName(String base)
    {
        return SYSPROP_PREFIX + base;
    }

    /**
     * Configuration can be specified in actual file, a classloader resource, from
     * system properties, and from environment variables.
     *
     * The precedence of config source is the inverse of how hard is it to change, with
     * a classloader resource being the hardest to change and is therefore the lowest
     * precedence.  Environment variables are the easiest to change and are therefore
     * the highest precedence.
     *
     * Config is loaded from lowest to highest precedence, starting with the default
     * config settings.
     */
    public Config() throws IOException
    {
        // order of precedence:  code, classloader, file, sysprop, envvar
        setupDefaults();

        // stream-based sources, from low-to-high precedence
        fromClassLoaderResource();
        fromFile();    // this actually looks in user.dir (i.e., cur dir) and user.home

        // "environment"-based sources
        fromSysProps();
        fromEnvVars();
    }

    /**
     * Set up all values in aggregatedConfig to be overridden by other sources.
     */
    private void setupDefaults()
    {
        putConfig(ConfigKey.CONFIG_V1_MODE,                          DEFAULT_V1_MODE);
        putConfig(ConfigKey.CONFIG_V1_KEYINFO_PATH,                  DEFAULT_V1_KEYINFO_PATH);
        putConfig(ConfigKey.CONFIG_V1_FIELD_PASSWORD,                DEFAULT_V1_FIELD_PASSWORD);
        putConfig(ConfigKey.CONFIG_V1_FIELD_VERSION,                 DEFAULT_V1_FIELD_VERSION);

        putConfig(ConfigKey.CONFIG_USE_ENCRYPTION,                   DEFAULT_USE_ENCRYPTION);
        putConfig(ConfigKey.CONFIG_ALGORITHM,                        DEFAULT_ALGORITHM);
        putConfig(ConfigKey.CONFIG_VAULT_SERVER,                     DEFAULT_VAULT_SERVER);
        putConfig(ConfigKey.CONFIG_TRANSIT_PATH,                     DEFAULT_TRANSIT_PATH);
        putConfig(ConfigKey.CONFIG_SECRETS_PATH,                     DEFAULT_SECRETS_PATH);
        putConfig(ConfigKey.CONFIG_SCOPE_GLOBAL_KEYNAME,             DEFAULT_GLOBAL_KEYNAME);
        putConfig(ConfigKey.CONFIG_SCOPE_KEYNAME_PREFIX,             DEFAULT_KEYNAME_PREFIX);
        putConfig(ConfigKey.CONFIG_CURRDATAKEY_GLOBAL_KEYNAME,       DEFAULT_CURRDK_GLOBAL_NAME);
        putConfig(ConfigKey.CONFIG_CURRDATAKEY_SECRET_PREFIX,        DEFAULT_CURRDK_SECRET_PREFIX);
        putConfig(ConfigKey.CONFIG_CURRDATAKEY_CIPHERTEXT_FIELDNAME, DEFAULT_CURRDK_CIPHERFLD);
        putConfig(ConfigKey.CONFIG_CURRDATAKEY_PLAINTEXT_FIELDNAME,  DEFAULT_CURRDK_PLAINFLD);

        // defaults for global scope
        aggregatedConfig.put(replaceID(ConfigKey.CONFIG_SCOPE_RENEW.getKey(),      SCOPE_GLOBALNAME), DEFAULT_GLOBAL_RENEW);
        aggregatedConfig.put(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_SIZE.getKey(), SCOPE_GLOBALNAME), DEFAULT_GLOBAL_CACHE_SIZE);
        aggregatedConfig.put(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_TTL.getKey(),  SCOPE_GLOBALNAME), DEFAULT_GLOBAL_CACHE_TTL);
    }

    private void putConfig(ConfigKey key, Object value)
    {
        Class<?> type = key.getType();

        if (!type.isAssignableFrom(value.getClass()))
        {
            if (type == Boolean.class)
            {
                value = Boolean.valueOf(value.toString());
            }
            else if (type == String.class)
            {
                value = value.toString();
            }
            else if (type == Integer.class)
            {
                value = Integer.valueOf(value.toString());
            }
            else
            {
                throw new IllegalStateException("Config values of type " + type + " are not supported");
            }
        }

        aggregatedConfig.put(key.getKey(), value);
    }

    /**
     * InputStream-oriented loading methods; this covers classloader resources and files.
     */
    private void fromClassLoaderResource() throws IOException
    {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILENAME))
        {
            if (is != null)
                fromInputStream(is, "ClassLoader resource '" + CONFIG_FILENAME + "'");
        }
    }

    private void fromFile() throws IOException
    {
        final String file = System.getProperty("file.separator") + CONFIG_FILENAME;

        try
        {
            fromFilepath(System.getProperty("user.dir") + file);
        }
        catch (IOException iox)
        {
            try
            {
                fromFilepath(System.getProperty("user.home") + file);
            }
            catch (IOException iox2)
            {
            }
        }
    }

    private void fromFilepath(String filepath) throws IOException
    {
        try (InputStream is = new FileInputStream(filepath))
        {
            fromInputStream(is, "file " + filepath);
        }
    }

    private void fromInputStream(InputStream is, String sourceMsg) throws IOException
    {
        Properties props = new Properties();

        LOGGER.info("v2 security config found in " + sourceMsg);

        props.load(is);

        mergeConfig(sourceMsg, propsToMap(props, ""), "");
    }

    /**
     * System prop names are only prefixed.  Most of this method is just for
     * the purpose of logging what's loaded.
     */
    private void fromSysProps()
    {
        Map<String,Object> config = propsToMap(System.getProperties(), SYSPROP_PREFIX);

        if (config.size() > 0)
        {
            LOGGER.info("v2 security config found in system properties");
            mergeConfig("System.getProperties", config, SYSPROP_PREFIX);
        }
    }

    private Map<String,Object> propsToMap(Properties props, String prefix)
    {
        Map<String,Object> map = new HashMap<>();

        for (Map.Entry<Object,Object> entry : props.entrySet())
        {
            String key = (String) entry.getKey();

            if (key.startsWith(prefix))
                map.put(((String) entry.getKey()), entry.getValue());
        }

        return map;
    }

    /**
     * Environment var names are modified to have _ instead of "." and "-" and are
     * upper-cased.  Because we map both . and - to _ in envvar names, when going
     * back to config point name, we add both . and _ keys to aggregated map
     */
    private void fromEnvVars()
    {
        Map<String,Object> config = new HashMap<>();

        for (Map.Entry<String,String> entry : System.getenv().entrySet())
        {
            String name = entry.getKey();

            if (name.startsWith(ENVVAR_PREFIX))
            {
                String val = entry.getValue();

                // big hack as ConfigKey.getKey can contain either "." or "-"
                name = name.substring(ENVVAR_PREFIX.length()).toLowerCase();

                config.put(name.replaceAll("_", "."), val);
                config.put(name.replaceAll("_", "-"), val);
            }
        }

        if (config.size() > 0)
        {
            LOGGER.info("v2 security config found in environment variables");
            mergeConfig("System.getenv", config, "");
        }
    }

    /**
     *
     */
    private void mergeConfig(String source, Map<String,Object> config, String prefix)
    {
        for (ConfigKey key : ConfigKey.values())
        {
            String name = key.getKey();

            // pull in only those entries from props that we know about (ignoring for a moment the scope-based ones)
            if (!name.contains(SCOPE_IDSTR))  // hack for now
            {
                Object value = config.get(prefix + name);

                if (value != null)
                {
                    if (LOG_CONFIG)
                        LOGGER.info(" Config " + name + " overridden by " + source);

                    putConfig(key, value);
                }
            }
        }

        // now look for props that have keys of the form "scope." in order to get per-scope overrides;
        // slightly unfortunately this will redundantly pull over a couple of other ones that also
        // start with "scope."

        for (Map.Entry<String,Object> entry : config.entrySet())
        {
            String name = entry.getKey();

            if (name.startsWith(prefix))
            {
                name = name.substring(prefix.length());

                // since some non-scope-based config keys start with "scope." we need to skip them...
                if (name.startsWith("scope.")                                    &&
                        !name.equals(ConfigKey.CONFIG_SCOPE_GLOBAL_KEYNAME.getKey()) &&
                        !name.equals(ConfigKey.CONFIG_SCOPE_KEYNAME_PREFIX.getKey()))
                {
                    if (LOG_CONFIG)
                        LOGGER.info(" Config " + name + " overridden by " + source);

                    // all true scope-based ones define a duration
                    aggregatedConfig.put(name, convertDuration(name, (String) entry.getValue()));
                }
            }
        }
    }

    /**
     * Converts from ###type form to the equivalent number of seconds; e.g., "1d" means 1 day,
     * "2h" means 2 hours, etc.  It's an error to pass an empty/null string.
     */
    private static int convertDuration(String name, String dur)
    {
        dur = trimAndCheck(name, dur);

        int  chop = dur.length() - 1;  // presumed location of unit char
        char unit = Character.toLowerCase(dur.charAt(chop));
        int  mult = 1;

        if (unit == DUR_SECONDS)
            ;
        else if (unit == DUR_MINUTES)
            mult = 60;
        else if (unit == DUR_HOURS)
            mult = 60 * 60;
        else if (unit == DUR_DAYS)
            mult = 60 * 60 * 24;
        else
            chop++;

        dur = dur.substring(0, chop);

        return mult * Integer.parseInt(dur);
    }

    /**
     * If a config value is specified, complain if it's actually empty
     */
    private static String trimAndCheck(String prop, String s)
    {
        if ((s == null) || ((s = s.trim()).length() == 0))
            throw new IllegalArgumentException("Property '" + prop + "' is present but empty");

        return s;
    }

    /**
     * Getters
     */
    public Boolean isV1Mode()
    {
        return (Boolean) aggregatedConfig.get(ConfigKey.CONFIG_V1_MODE.getKey());
    }
    public Boolean isUseEncryption()
    {
        return (Boolean) aggregatedConfig.get(ConfigKey.CONFIG_USE_ENCRYPTION.getKey());
    }
    public String getAlgorithm()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_ALGORITHM.getKey());
    }
    public String getVaultServer()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_VAULT_SERVER.getKey());
    }
    public String getTransitPath()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_TRANSIT_PATH.getKey());
    }
    public String getSecretsPath()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_SECRETS_PATH.getKey());
    }
    public String getV1KeyinfoPath()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_V1_KEYINFO_PATH.getKey());
    }
    public String getV1PasswordField()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_V1_FIELD_PASSWORD.getKey());
    }
    public String getV1VersionField()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_V1_FIELD_VERSION.getKey());
    }
    public String getKeyname(String scope)
    {
        if (scope == null)
            return (String) aggregatedConfig.get(ConfigKey.CONFIG_SCOPE_GLOBAL_KEYNAME.getKey());
        else
            return (String) aggregatedConfig.get(ConfigKey.CONFIG_SCOPE_KEYNAME_PREFIX.getKey()) + scope;
    }
    public int getRenewal(String scope)
    {
        Integer val = getOverride(ConfigKey.CONFIG_SCOPE_RENEW, scope);

        if (val == null)
            val = getOverride(ConfigKey.CONFIG_SCOPE_RENEW, null);

        return val;
    }
    public int getCacheSize(String scope)
    {
        Integer val = getOverride(ConfigKey.CONFIG_SCOPE_CACHE_SIZE, scope);

        if (val == null)
            val = getOverride(ConfigKey.CONFIG_SCOPE_CACHE_SIZE, null);

        return val;
    }
    public int getCacheTtl(String scope)
    {
        Integer val = getOverride(ConfigKey.CONFIG_SCOPE_CACHE_TTL, scope);

        if (val == null)
            val = getOverride(ConfigKey.CONFIG_SCOPE_CACHE_TTL, null);

        return val;
    }

    public String getSecretNameForCurrentDatakey(String scope)
    {
        if (scope == null)
            return (String) aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_GLOBAL_KEYNAME.getKey());
        else
            return ((String) aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_SECRET_PREFIX.getKey())) + scope;
    }
    public String getCurrentDatakeyCipherFieldname()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_CIPHERTEXT_FIELDNAME.getKey());
    }
    public String getCurrentDatakeyPlaintextFieldname()
    {
        return (String) aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_PLAINTEXT_FIELDNAME.getKey());
    }

    /**
     * Form the real key for the aggregated config and return its value
     */
    private Integer getOverride(ConfigKey config, String scope)
    {
        return (Integer) aggregatedConfig.get(
                replaceID(config.getKey(), (scope != null) ? scope : SCOPE_GLOBALNAME)
        );
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        String[]      it = new String[aggregatedConfig.size()];
        int           ix = 0;

        sb.append("SecurityConfig:");

        for (Map.Entry<String,Object> entry : aggregatedConfig.entrySet())
            it[ix++] = "\n -- " + entry.getKey() + " = " + entry.getValue();

        java.util.Arrays.sort(it);

        for (String li : it)
            sb.append(li);

        sb.append("\n");

        return sb.toString();
    }

    public static void main(String... args) throws Exception
    {
        Config config = new Config();

        System.out.println("\n\n========> Security config:  " + config.toString());

        verify(config);
    }

    private static void verify(Config config)
    {
        // verify that what's in the aggregatedConfig matches what is returned from the methods

        // simple ones:
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_V1_MODE.getKey()), config.isV1Mode());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_ALGORITHM.getKey()), config.getAlgorithm());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_VAULT_SERVER.getKey()), config.getVaultServer());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_TRANSIT_PATH.getKey()), config.getTransitPath());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_SECRETS_PATH.getKey()), config.getSecretsPath());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_V1_KEYINFO_PATH.getKey()), config.getV1KeyinfoPath());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_V1_FIELD_PASSWORD.getKey()), config.getV1PasswordField());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_V1_FIELD_VERSION.getKey()), config.getV1VersionField());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_CIPHERTEXT_FIELDNAME.getKey()), config.getCurrentDatakeyCipherFieldname());
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_PLAINTEXT_FIELDNAME.getKey()), config.getCurrentDatakeyPlaintextFieldname());

        // global cache
        verifyMethod(true, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_RENEW.getKey(),      SCOPE_GLOBALNAME)), config.getRenewal(null));
        verifyMethod(true, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_SIZE.getKey(), SCOPE_GLOBALNAME)), config.getCacheSize(null));
        verifyMethod(true, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_TTL.getKey(),  SCOPE_GLOBALNAME)), config.getCacheTtl(null));

        // per-pds cache; since overrides for "abc" aren't configured, we expect nulls in aggregatedConfig
        verifyMethod(false, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_RENEW.getKey(),      "abc")), config.getRenewal("abc"));
        verifyMethod(false, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_SIZE.getKey(), "abc")), config.getCacheSize("abc"));
        verifyMethod(false, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_TTL.getKey(),  "abc")), config.getCacheTtl("abc"));

        // requires that config includes cache overrides for "xyz" scope
        verifyMethod(true, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_RENEW.getKey(),      "xyz")), config.getRenewal("xyz"));
        verifyMethod(true, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_SIZE.getKey(), "xyz")), config.getCacheSize("xyz"));
        verifyMethod(true, config.aggregatedConfig.get(replaceID(ConfigKey.CONFIG_SCOPE_CACHE_TTL.getKey(),  "xyz")), config.getCacheTtl("xyz"));

        // parameterized, but use null (for global)
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_SCOPE_GLOBAL_KEYNAME.getKey()), config.getKeyname(null));
        verifyMethod(true, config.aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_GLOBAL_KEYNAME.getKey()), config.getSecretNameForCurrentDatakey(null));

        // parameterized; use non-null
        verifyMethod(false, config.aggregatedConfig.get(ConfigKey.CONFIG_SCOPE_KEYNAME_PREFIX.getKey()), config.getKeyname("hello"));
        verifyMethod(false, config.aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_SECRET_PREFIX.getKey()), config.getSecretNameForCurrentDatakey("hello"));

        verifyMethod(false, config.aggregatedConfig.get(ConfigKey.CONFIG_SCOPE_KEYNAME_PREFIX.getKey()), config.getKeyname("something1"));
        verifyMethod(false,config.aggregatedConfig.get(ConfigKey.CONFIG_CURRDATAKEY_SECRET_PREFIX.getKey()), config.getSecretNameForCurrentDatakey("something2"));
    }

    private static void verifyMethod(boolean mustMatch, Object fromAggr, Object fromMethod)
    {
        System.out.println(((mustMatch) ? "" : "[match not expected] ") + fromAggr + " :: " + fromMethod);

        if (mustMatch && !fromAggr.equals(fromMethod))
            throw new IllegalStateException("method != map value:  " + fromMethod + " :: " + fromAggr);
    }

}
