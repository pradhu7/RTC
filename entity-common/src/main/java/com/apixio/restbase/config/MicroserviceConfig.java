package com.apixio.restbase.config;

import java.io.IOException;
import java.util.Map;

import io.dropwizard.Configuration;

/**
 * MicroserviceConfig captures/defines all configuration needed to fully set up
 * a MicroserviceApplication instance.  This config includes, at a reasonable
 * level of granularity, the following:
 *
 *  * persistence:  Redis, Cassandra
 *  * logging
 *  * microfilters
 *  * API ACLs (requires logging and microfilters)
 *  * token
 *
 * Each of these 5 chunks of config are exposed separately so the code can be
 * modular.  Note that not all microservices will use all of the above
 */
public class MicroserviceConfig extends Configuration
{
    /**
     * Yaml config for the microservice application is under this key in the
     * .yaml file.  This MUST match the setter method (setMicroserviceConfig)
     * or System.getProperties() overrides won't work.
     */
    private static final String KEY_MICROSERVICE     = "microserviceConfig."; // Note the "." for code convenience
    private static final int    KEY_MICROSERVICE_LEN = KEY_MICROSERVICE.length();

    /**
     *
     */
    public enum ConfigArea
    {
        PERSISTENCE("persistenceConfig"),
        APIACL("apiaclConfig"),
        LOGGING("loggingConfig"),
        MICROFILTER("filterConfig"),
        TOKEN("tokenConfig");

        public String getYamlKey()
        {
            return yamlKey;
        }

        private ConfigArea(String yamlKey)
        {
            this.yamlKey = yamlKey;
        }

        private String yamlKey;
    }

    /**
     * The (sub-sub)keys for the actual configuration elements of each of the
     * config sets.  Note that the yaml key is relative to the subkey for the
     * particular chunk
     */
    // "PST_" is persistence

    // support prefixes for allowing redis configuration, for more than one
    // named instance of redis.
    //
    // The default prefix makes it backwards compatible; all other named
    // redis configuration will required a "_" + name convention
    //
    public static final String PST_JEDIS_PREFIX          = "jedisConfig";
    public static final String PST_JEDIS_TESTWHILEIDLE   = "testWhileIdle";                // boolean
    public static final String PST_JEDIS_TESTONBORROW    = "testOnBorrow";                 // boolean
    public static final String PST_JEDIS_MAXTOTAL        = "maxTotal";                     // int
    public static final String PST_JEDIS_MAXWAITMILLIS   = "maxWaitMillis";                // long; ms
    public static final String PST_JEDIS_BLOCKWHENNONE   = "blockWhenExhausted";           // boolean

    public static final String PST_POOL_PREFIX           = "jedisPool";
    public static final String PST_POOL_TIMEOUT          = "timeout";                        // int:  ms
    public static final String PST_POOL_PASSWORD         = "password";                       // string; not used currently

    public static final String PST_REDIS_CONFIG_PREFIX   = "redisConfig";
    public static final String PST_REDIS_HOST            = "host";                         // string
    public static final String PST_REDIS_PORT            = "port";                         // int
    public static final String PST_REDIS_PREFIX          = "keyPrefix";                    // string
    public static final String PST_REDIS_RETRIES         = "retries";                      // string:  max:ms1,ms2,...

    /**
     * Support for multiple sets of Cassandra config.  The first set (0-based) has "" for its ID and anything else
     * has the given ID
     */
    private static final String PST_CASS_PREFIX = "cassandraConfig_";

    // only if we need something besides Cassandra config to have multiple sets:
    private static final String pstConfigSet(String id)     { return PST_CASS_PREFIX + ((id == null) ? "" : id.trim()); }

    public static String pstCassHosts(String id)            { return pstConfigSet(id) + ".hosts";                 }  // string
    public static String pstCassLocalDC(String id)          { return pstConfigSet(id) + ".localDC";               }  // string
    public static String pstCassKeyspace(String id)         { return pstConfigSet(id) + ".keyspaceName";          }  // string
    public static String pstCassBinPort(String id)          { return pstConfigSet(id) + ".binaryPort";            }  // int
    public static String pstCassUsername(String id)         { return pstConfigSet(id) + ".username";              }  // string
    public static String pstCassPassword(String id)         { return pstConfigSet(id) + ".password";              }  // string
    public static String pstCassBaseDelay(String id)        { return pstConfigSet(id) + ".baseDelayMs";           }  // int: ms
    public static String pstCassMaxDelay(String id)         { return pstConfigSet(id) + ".maxDelayMs" ;           }  // int: ms
    public static String pstCassReadConsLevel(String id)    { return pstConfigSet(id) + ".readConsistencyLevel";  }  // string
    public static String pstCassWriteConsLevel(String id)   { return pstConfigSet(id) + ".writeConsistencyLevel"; }  // string
    public static String pstCassMinConn(String id)          { return pstConfigSet(id) + ".minConnections";        }  // int
    public static String pstCassMaxConn(String id)          { return pstConfigSet(id) + ".maxConnections";        }  // int
    public static String pstCassDowngrade(String id)        { return pstConfigSet(id) + ".downGrade";             }  // boolean
    public static String pstCassCqlMonitor(String id)       { return pstConfigSet(id) + ".cqlMonitor";            }  // boolean
    public static String pstCassBatchSyncEnabled(String id) { return pstConfigSet(id) + ".batchSyncEnabled";      }  // boolean
    public static String pstCassCqlTrace(String id)         { return pstConfigSet(id) + ".cqlTrace";              }  // boolean
    public static String pstCassCqlDetail(String id)        { return pstConfigSet(id) + ".cqlDetail";             }  // boolean

    private static final String pstSearchConfigSet(String id)       { return (id == null) ? "" : id.trim(); }
    public static String pstElasticHosts(String id)                 { return pstSearchConfigSet(id) + ".hosts";                   }  // string
    public static String pstElasticBinaryPort(String id)            { return pstSearchConfigSet(id) + ".binaryPort";              }  // int
    public static String pstElasticThreadCount(String id)           { return pstSearchConfigSet(id) + ".threadCount";             }  // int
    public static String pstElasticConnectionTimeoutMs(String id)   { return pstSearchConfigSet(id) + ".connectionTimeoutMs";     }  // int
    public static String pstElasticSocketTimeoutMs(String id)       { return pstSearchConfigSet(id) + ".socketTimeoutMs";         }  // int

    // "ACL_" is APi ACLs
    public static final String ACL_DEFFILE      = "apiAclDefs";            // string: filepath
    public static final String ACL_CFNAME       = "aclColumnFamilyName";   // string: column family name
    public static final String ACL_DEBUG        = "aclDebug";              // string: enum LogLevel
    public static final String ACL_CACHETIMEOUT = "aclHpCacheTimeout";     // int/long:  # of milliseconds

    // "LOG_" is Logging
    public static final String LOG_APPNAME       = "appName";            // string
    public static final String LOG_DEFLOGGERNAME = "defaultLoggerName";  // string
    public static final String LOG_PROPERTIES    = "properties";         // client must do getConfigSubtree(LOG_PROPERTIES) and convert resulting Map<String,Object>

    // "MF_" is microfilter
    public static final String MF_URLPATTERN      = "filterUrlPattern";   // list<string>:  string is something like "/*"
    public static final String MF_REQUESTFILTERS  = "requestFilters";     // list<string>:  string is classname
    // NOTE:  per-requestfilter config is retrieved via getConfigSubtree(classname)

    // "TOK_" is token
    public static final String TOK_INTERNALMAXTTL  = "internalMaxTTL";          // long: seconds; from time of token creation
    public static final String TOK_EXTERNALMAXTTL  = "externalMaxTTL";          // long: seconds; from time of token creation
    public static final String TOK_ACTIVITYTIMEOUT = "externalActivityTimeout"; // long: seconds; token deleted if no activity in this # of seconds
    public static final String TOK_AUTHCOOKIENAME  = "authCookieName";          // string:  optional cookie name that can contain token
    public static final String TOK_GATEWAYHEADER   = "gatewayHeader";           // string:  optional value to indicate request is from gateway; syntax:  "somename: somevalue"

    /**
     * The master set of all config
     */
    private ConfigSet            appConfig;
    private Map<String, String>  bootProps;

    /**
     * Indicates whether we are using consul for config
     */
    private boolean usingConsul;

    /**
     * Invoked by yaml parser during dropwizard bootup
     */
    @Deprecated
    public void setMicroserviceConfig(Map<String, Object> config) throws IOException
    {
        BootConfig bc = BootConfig.fromSystemProps(ConfigSet.fromMap(config));

        appConfig = bc.getBootConfig();
        bootProps = bc.getBootProps();
    }

    /**
     * To Make yaml file and dropwizard work with consul
     */
    public void setConsulConfig(Boolean usingConsul) throws IOException
    {
        if (usingConsul)
        {
            BootConfig bc = BootConfig.fromSystemProps(ConfigSet.fromConsul());

            this.usingConsul = usingConsul;
            appConfig        = bc.getBootConfig();
            bootProps        = bc.getBootProps();
        }
    }

    /**
     * Returns all application config
     */
    @Deprecated
    public ConfigSet getMicroserviceConfig()
    {
        return appConfig;
    }

    /**
     * Returns all application config
     */
    public ConfigSet getConsulConfig()
    {
        return appConfig;
    }

    public Map<String, String> getBootProps()
    {
        return bootProps;
    }

    public boolean isUsingConsul()
    {
        return usingConsul;
    }

    /**
     * Create and return a ConfigSet object that contains all config items that
     * are under the given subkey (i.e., under "application.{subkey}").  The value
     * of subkey should be one of CSET_* above.
     */
    public ConfigSet getConfigSubtree(ConfigArea area)
    {
        return appConfig.getConfigSubtree(area.getYamlKey());
    }
    public ConfigSet getPersistenceConfig()
    {
        return getConfigSubtree(ConfigArea.PERSISTENCE);
    }
    public ConfigSet getAclConfig()
    {
        return getConfigSubtree(ConfigArea.APIACL);
    }
    public ConfigSet getLoggingConfig()
    {
        return getConfigSubtree(ConfigArea.LOGGING);
    }
    public ConfigSet getMicrofilterConfig()
    {
        return getConfigSubtree(ConfigArea.MICROFILTER);
    }
    public ConfigSet getTokenConfig()
    {
        return getConfigSubtree(ConfigArea.TOKEN);
    }

    @Override
    public String toString()
    {
        return "config:" + appConfig.toString();
    }
}

