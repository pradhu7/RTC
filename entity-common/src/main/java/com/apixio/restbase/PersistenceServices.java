package com.apixio.restbase;

import java.util.HashMap;
import java.util.Map;

import com.apixio.datasource.elasticSearch.ElasticConnector;
import com.apixio.datasource.elasticSearch.ElasticCrud;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;

import com.apixio.datasource.cassandra.CqlConnector;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlTransactionCrud;
import com.apixio.datasource.cassandra.MonitoredCqlCrud;
import com.apixio.datasource.cassandra.TraceCqlCrud;
import com.apixio.datasource.kafka.KafkaDS;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.datasource.springjdbc.JdbcDS;
import com.apixio.datasource.springjdbc.JdbcTransaction;
import com.apixio.logger.EventLogger;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.util.CqlTransactions;

import static com.apixio.restbase.config.MicroserviceConfig.PST_JEDIS_PREFIX;
import static com.apixio.restbase.config.MicroserviceConfig.PST_POOL_PREFIX;
import static com.apixio.restbase.config.MicroserviceConfig.PST_REDIS_CONFIG_PREFIX;

/**
 */
public class PersistenceServices
{
    /**
     * Support for multiple JDBC datasources is done (for now) by the client specifying
     * a string that identifies the config section to use.  This is a different, looser
     * model than Cassandra datasource definition (which uses the above enum).
     */
    private Map<String, JdbcDS>          jdbcDss = new HashMap<>();
    private Map<String, JdbcTransaction> jdbcTxs = new HashMap<>();

    /**
     * Support for multiple Kafka datasources is done (for now) by the client specifying
     * a string that identifies the config section to use.
     */
    private Map<String, KafkaDS>  kafkaSources = new HashMap<>();

    /**
     * Support for multiple redis datasources is done (for now) by the client specifying
     * a string that identifies the config section to use.
     */
    private Map<String, RedisOps>  redisOps = new HashMap<>();


    /**
     * The core things needed for persistence.  Note that the fields that hold the
     * lazy/on-demand values are private in order to force all clients/subclasses
     * to go through the getter methods.
     *
     * To support multiple redis configuration, we allow named components (jedisPool, key prefixes, etc)
     */
    private   Map<String, JedisPool>       jedisPools = new HashMap<>();
    private   Map<String, Transactions>    redisTrans = new HashMap<>();
    private   Map<String, String>          redisKeyPrefixes = new HashMap<>();


    /**
     * Hang on to the root of persistence config
     */
    private   ConfigSet       rootConfig;

    private Map<CassandraCluster, CqlCrud>         cqlCruds        = new HashMap<>();
    private Map<CassandraCluster, CqlTransactions> cqlTransactions = new HashMap<>();
    private Map<ElasticCluster, ElasticCrud>       elasticCruds    = new HashMap<>();

    /**
     * No better place than here for setting up centralized logging
     */
    private ConfigSet loggingConfig;

    /**
     * Support for multiple CassandraConfig sets (so far only Cassandra Config--this is intentional
     * and if there are more things that need multiple set support (e.g., talking to two redis
     * servers) then it's probably better to have 2 PersistenceServices instances.  That potentially forces
     * a change to all yaml files, though (so as to keep some consistency in them).  This was deemed
     * the easier way at this point in time
     */
    public static class CassandraCluster
    {
        public static CassandraCluster INTERNAL    = new CassandraCluster("internal");
        public static CassandraCluster SCIENCE     = new CassandraCluster("science");
        public static CassandraCluster APPLICATION = new CassandraCluster("application");

        /**
         * Factory method for general use
         */
        public static CassandraCluster newCluster(String id)
        {
            if ((id == null) || ((id = id.trim()).length() == 0))
                throw new IllegalArgumentException("CassandraCluster id can't be empty");
            else if (id.equals(INTERNAL.getID()) ||
                    id.equals(SCIENCE.getID()) ||
                    id.equals(APPLICATION.getID()))
                throw new IllegalArgumentException("CassandraCluster id can't a reserved one [" +
                        INTERNAL.getID() + "," + SCIENCE.getID() + "," + APPLICATION.getID() +
                        "]");

            return new CassandraCluster(id);
        }

        /**
         *
         */
        public String getID()
        {
            return clusterID;
        }

        /**
         * implementation
         */
        private CassandraCluster(String id)
        {
            this.clusterID = id;
        }

        private String clusterID;

        static public CassandraCluster valueOf(String clusterID)
        {
            if(clusterID!=null)
            {
                if(clusterID.equals(INTERNAL.getID())) return INTERNAL;
                else if(clusterID.equals(SCIENCE.getID())) return SCIENCE;
                else if(clusterID.equals(APPLICATION.getID())) return APPLICATION;
            }
            return null;
        }
    }

    /**
     * Support for multiple Elastic Search config sets (so far only chart space Config
     */
    public static class ElasticCluster
    {
        public static ElasticCluster CHART_SPACE    = new ElasticCluster("searchChartSpace");

        /**
         * Factory method for general use
         */
        public static ElasticCluster newCluster(String id)
        {
            if ((id == null) || ((id = id.trim()).length() == 0))
                throw new IllegalArgumentException("ElasticCluster id can't be empty");
            else if (id.equals(CHART_SPACE.getID()))
                throw new IllegalArgumentException("ElasticCluster id can't a reserved one [" +
                        CHART_SPACE.getID() + "]");

            return new ElasticCluster(id);
        }

        /**
         *
         */
        public String getID()
        {
            return clusterID;
        }

        /**
         * implementation
         */
        private ElasticCluster(String id)
        {
            this.clusterID = id;
        }

        private String clusterID;

        static public ElasticCluster valueOf(String clusterID)
        {
            if(clusterID!=null)
            {
                if(clusterID.equals(CHART_SPACE.getID())) return CHART_SPACE;
            }
            return null;
        }
    }

    /**
     * Constructor that just records the core objects.
     */
    public PersistenceServices(ConfigSet config, ConfigSet loggingConfig)
    {
        this.rootConfig    = config;
        this.loggingConfig = loggingConfig;
    }

    protected PersistenceServices(DaoBase seed)
    {
        PersistenceServices ps = seed.getPersistenceServices();

        this.rootConfig      = ps.rootConfig;
        this.redisOps        = ps.redisOps;
        this.redisTrans      = ps.redisTrans;
        this.redisKeyPrefixes = ps.redisKeyPrefixes;
        this.cqlCruds        = ps.cqlCruds;
        this.cqlTransactions = ps.cqlTransactions;
        this.loggingConfig   = ps.loggingConfig;
        this.jdbcDss         = ps.jdbcDss;
        this.jdbcTxs         = ps.jdbcTxs;
        this.kafkaSources    = ps.kafkaSources;
    }

    private synchronized void addRedis(String id)
    {
        if (rootConfig != null)
        {
            ConfigSet redisConfigSet;
            ConfigSet jedisConfigSet;
            ConfigSet jedisPoolConfigSet;

            String redisClusterId = id.trim();

            String rootKey = PST_REDIS_CONFIG_PREFIX + (StringUtils.isEmpty(redisClusterId) ? "" : "_" + redisClusterId);
            if ((redisConfigSet = rootConfig.getConfigSubtree(rootKey)) == null)
                throw new IllegalStateException("PersistenceServices:  attempt to add redis  for id '" + redisClusterId + "' " +
                        "but no yaml configuration was found.  Expected yaml key " + rootKey);


            String jedisConfigRootKey = PST_JEDIS_PREFIX + (StringUtils.isEmpty(redisClusterId) ? "" : "_" + redisClusterId);
            if ((jedisConfigSet = rootConfig.getConfigSubtree(jedisConfigRootKey)) == null)
                throw new IllegalStateException("PersistenceServices:  attempt to add jedisConfig for id '" + redisClusterId + "' " +
                        "but no yaml configuration was found.  Expected yaml key " + jedisConfigRootKey);


            String jedisPoolRootKey = PST_POOL_PREFIX + (StringUtils.isEmpty(redisClusterId) ? "" : "_" + redisClusterId);
            if ((jedisPoolConfigSet = rootConfig.getConfigSubtree(jedisPoolRootKey)) == null)
                throw new IllegalStateException("PersistenceServices:  attempt to add jedisPoolRootKey for id '" + redisClusterId + "' " +
                        "but no yaml configuration was found.  Expected yaml key " + jedisPoolRootKey);


            if (redisOps.get(redisClusterId) == null)
            {
                GenericObjectPoolConfig jedisConfig   = new GenericObjectPoolConfig();
                String                  retries       = redisConfigSet.getString(MicroserviceConfig.PST_REDIS_RETRIES, null);

                jedisConfig.setTestWhileIdle(jedisConfigSet.getBoolean(MicroserviceConfig.PST_JEDIS_TESTWHILEIDLE, true));
                jedisConfig.setTestOnBorrow(jedisConfigSet.getBoolean(MicroserviceConfig.PST_JEDIS_TESTONBORROW, true));
                jedisConfig.setMaxTotal(jedisConfigSet.getInteger(MicroserviceConfig.PST_JEDIS_MAXTOTAL));
                jedisConfig.setMaxWaitMillis(jedisConfigSet.getLong(MicroserviceConfig.PST_JEDIS_MAXWAITMILLIS, 10000L));
                jedisConfig.setBlockWhenExhausted(jedisConfigSet.getBoolean(MicroserviceConfig.PST_JEDIS_BLOCKWHENNONE, true));

                JedisPool jedisPool = new JedisPool(jedisConfig,
                        redisConfigSet.getString(MicroserviceConfig.PST_REDIS_HOST),
                        redisConfigSet.getInteger(MicroserviceConfig.PST_REDIS_PORT),
                        jedisPoolConfigSet.getInteger(MicroserviceConfig.PST_POOL_TIMEOUT));
                jedisPools.put(redisClusterId, jedisPool);

                Transactions redisTansaction = new Transactions(jedisPool);
                redisTrans.put(redisClusterId, redisTansaction);

                RedisOps redisOp = new RedisOps(jedisPool);

                if (retries != null)
                    redisOp.setRetryConfig(new RedisOps.RetryConfig(retries));

                redisOp.setTransactions(redisTansaction);

                String redisKeyPrefix = redisConfigSet.getString(MicroserviceConfig.PST_REDIS_PREFIX);
                redisKeyPrefixes.put(redisClusterId, redisKeyPrefix);

                redisOps.put(redisClusterId, redisOp);
            }
        }
    }

    /**
     * "id" contains the client-managed string that is used as the yaml config key root
     * for that instance of JdbcDS configuration
     */
    private void addJdbc(String id)
    {
        if (rootConfig != null)
        {
            ConfigSet jdbcConfig;
            String    rootKey;

            id      = id.trim();
            rootKey = "jdbc_" + id;

            // If caller is asking for JDBC but we don't have config, tell them
            if ((jdbcConfig = rootConfig.getConfigSubtree(rootKey)) == null)
                throw new IllegalStateException("PersistenceServices:  attempt to add Jdbc for connection '" + id + "' " +
                                                "but no yaml configuration was found.  Expected yaml key " + rootKey);
                                                

            if (jdbcDss.get(id) == null)
            {
                JdbcDS jdbcDS = new JdbcDS(jdbcConfig.getProperties());

                jdbcDss.put(id, jdbcDS);
                jdbcTxs.put(id, jdbcDS.getTransactionManager());
            }
        }
    }

    /**
     * "id" contains the client-managed string that is used as the yaml config key root
     * for that instance of KafkaDS configuration
     */
    private void addKafkaSource(String id)
    {
        if (rootConfig != null)
        {
            ConfigSet kafkaConfig;
            String    rootKey;

            id      = id.trim();
            rootKey = "kafka_" + id;

            // If caller is asking for Kafka but we don't have config, tell them
            if ((kafkaConfig = rootConfig.getConfigSubtree(rootKey)) == null)
                throw new IllegalStateException("PersistenceServices:  attempt to add Kafka datasource for id '" + id + "' " +
                                                "but no yaml configuration was found.  Expected yaml key " + rootKey);
                                                

            if (kafkaSources.get(id) == null)
            {
                KafkaDS kafkaConsumer = new KafkaDS(kafkaConfig.getProperties());

                kafkaSources.put(id, kafkaConsumer);
            }
        }
    }

    private void addCql(CassandraCluster cass)
    {
        if (rootConfig != null)
        {
            String id = cass.getID();

            // If caller is asking for CQL but we don't have config, tell them
            if (rootConfig.getString(MicroserviceConfig.pstCassHosts(id), null) == null)
                throw new IllegalStateException("PersistenceServices:  attempt to add CQL for cluster '" + id + "' " +
                                                "but no yaml configuration was found.  Expected yaml key " +
                                                MicroserviceConfig.pstCassHosts(id));

            if (cqlCruds.get(cass) == null)
            {
                boolean            monitored = rootConfig.getBoolean(MicroserviceConfig.pstCassCqlMonitor(id), false);
                CqlTransactionCrud crud;

                if (monitored)
                    crud = new MonitoredCqlCrud(rootConfig.getBoolean(MicroserviceConfig.pstCassCqlDetail(id), false),
                                                rootConfig.getBoolean(MicroserviceConfig.pstCassCqlTrace(id),  false));
                else
                    crud = new CqlTransactionCrud();

                cqlCruds.put(cass, crud);
                cqlTransactions.put(cass, new CqlTransactions(crud));

                crud.setBatchSyncEnabled(rootConfig.getBoolean(MicroserviceConfig.pstCassBatchSyncEnabled(id), true));
                crud.setCqlConnector(toCqlConnector(rootConfig, true, cass));
                crud.setStatementCacheEnabled(true);
                crud.setTraceCqlCrud(new TraceCqlCrud());
            }
        }
    }

    public static CqlConnector toCqlConnector(ConfigSet config, boolean init, CassandraCluster cass)
    {
        String       id   = cass.getID();
        CqlConnector conn = new CqlConnector();

        conn.setHosts(config.getString(MicroserviceConfig.pstCassHosts(id)));
        conn.setLocalDC(config.getString(MicroserviceConfig.pstCassLocalDC(id)));
        conn.setBinaryPort(config.getInteger(MicroserviceConfig.pstCassBinPort(id)));
        conn.setUsername(config.getString(MicroserviceConfig.pstCassUsername(id), null));
        conn.setPassword(config.getString(MicroserviceConfig.pstCassPassword(id), null));
        conn.setReadConsistencyLevel(config.getString(MicroserviceConfig.pstCassReadConsLevel(id)));
        conn.setWriteConsistencyLevel(config.getString(MicroserviceConfig.pstCassWriteConsLevel(id)));
        conn.setKeyspaceName(config.getString(MicroserviceConfig.pstCassKeyspace(id)));
        conn.setMinConnections(config.getInteger(MicroserviceConfig.pstCassMinConn(id), 1));
        conn.setMaxConnections(config.getInteger(MicroserviceConfig.pstCassMaxConn(id), 2));
        conn.setDowngrade(config.getBoolean(MicroserviceConfig.pstCassDowngrade(id), false));

        if (init)
            conn.init();

        return conn;
    }

    private void addElasticSearch(ElasticCluster elasticCluster) throws Exception
    {
        if (rootConfig != null)
        {
            String id = elasticCluster.getID();

            // If caller is asking for CQL but we don't have config, tell them
            if (rootConfig.getString(MicroserviceConfig.pstElasticHosts(id), null) == null)
                throw new IllegalStateException("PersistenceServices:  attempt to add Elastic for cluster '" + id + "' " +
                        "but no yaml configuration was found.  Expected yaml key " +
                        MicroserviceConfig.pstElasticHosts(id));

            if (elasticCruds.get(elasticCluster) == null)
            {
                ElasticCrud crud = new ElasticCrud();

                elasticCruds.put(elasticCluster, crud);

                crud.setElasticConnector(toElasticConnector(rootConfig, true, elasticCluster));
            }
        }
    }

    public static ElasticConnector toElasticConnector(ConfigSet config, boolean init, ElasticCluster elasticCluster) throws Exception
    {
        String           id   = elasticCluster.getID();
        ElasticConnector conn = new ElasticConnector();

        conn.setHosts(config.getString(MicroserviceConfig.pstElasticHosts(id)));
        conn.setBinaryPort(config.getInteger(MicroserviceConfig.pstElasticBinaryPort(id)));
        conn.setThreadCount(config.getInteger(MicroserviceConfig.pstElasticThreadCount(id), 1));
        conn.setConnectionTimeoutMs(config.getInteger(MicroserviceConfig.pstElasticConnectionTimeoutMs(id), null));
        conn.setSocketTimeoutMs(config.getInteger(MicroserviceConfig.pstElasticSocketTimeoutMs(id), null));
        if (init) {
            conn.init();
        }
        return conn;
    }

    /**
     * Lazy Getters
     */

    /**
     * Backwards compatibility
     */
    @Deprecated
    public RedisOps getRedisOps()
    {
        return getRedisOps("");
    }

    @Deprecated
    public Transactions getRedisTransactions()
    {
        return getRedisTransactions("");
    }

    @Deprecated
    public String getRedisKeyPrefix()
    {
        return getRedisKeyPrefix("");
    }


    public RedisOps getRedisOps(String id)
    {
        if (redisOps.get(id) == null)
            addRedis(id);

        return redisOps.get(id);
    }

    public Transactions getRedisTransactions(String id)
    {
        if (redisOps.get(id) == null)
            addRedis(id);

        return redisTrans.get(id);
    }

    public String getRedisKeyPrefix(String id)
    {
        if (redisOps.get(id) == null)
            addRedis(id);

        return redisKeyPrefixes.get(id);
    }

    public JdbcDS getJdbc(String id)
    {
        synchronized (jdbcDss)
        {
            addJdbc(id);

            return jdbcDss.get(id);
        }
    }

    public JdbcTransaction getJdbcTransaction(String id)
    {
        synchronized (jdbcDss)
        {
            addJdbc(id);

            return jdbcTxs.get(id);
        }
    }

    public KafkaDS getKafkaDS(String id)
    {
        synchronized (kafkaSources)
        {
            addKafkaSource(id);

            return kafkaSources.get(id);
        }
    }

    public CqlCrud getCqlCrud(CassandraCluster cass)
    {
        synchronized (cass)
        {
            addCql(cass);

            return cqlCruds.get(cass);
        }
    }
    public CqlTransactions getCqlTransactions(CassandraCluster cass)
    {
        synchronized (cass)
        {
            addCql(cass);

            return cqlTransactions.get(cass);
        }
    }

    public ElasticCrud getElasticCrud(ElasticCluster elasticCluster) throws Exception
    {
        synchronized (elasticCluster)
        {
            addElasticSearch(elasticCluster);

            return elasticCruds.get(elasticCluster);
        }
    }

    /**
     * Backwards compatibility
     */
    @Deprecated
    public CqlCrud getCqlCrud()
    {
        return getCqlCrud(CassandraCluster.SCIENCE);
    }

    @Deprecated
    public CqlCrud getCqlCrud1()
    {
        return getCqlCrud(CassandraCluster.INTERNAL);
    }

    @Deprecated
    public CqlCrud getCqlCrud2()
    {
        return getCqlCrud(CassandraCluster.APPLICATION);
    }

    @Deprecated
    public CqlTransactions getCqlTransactions()
    {
        if (getCqlCrud(CassandraCluster.INTERNAL) != null)
            return cqlTransactions.get(CassandraCluster.INTERNAL);
        else
            return null;
    }

    /**
     *
     */
    public EventLogger getEventLogger(String loggerName)
    {
        if (loggingConfig != null)
            return ConfigUtil.getEventLogger(loggingConfig, loggerName);
        else
            return null;
    }

    public void close()
    {
        synchronized (cqlCruds)
        {
            for (CqlCrud crud : cqlCruds.values())
            {
                crud.close();
            }
        }

        for(JedisPool jedisPool: jedisPools.values())
        {
            if (jedisPool != null)
                jedisPool.close();
        }
    }

    /**
    *  Use logging config to know what service uses the library.
    *  Our current systems either use apxLogging (most of new services) or loggingConfig (ex. useraccount) to set up logging
    *  They have their own ways to get the microservice name
    *
    *  This method is only used by versioning feature for project properties change right now (08/12/2020)
    */
    public String getMicroServiceName()
    {
        String serviceName = "unknownFromConfig";
        try {
            // For apxLogging
            serviceName = getMicroServiceNameFromApxLogging();
        } catch (Exception e1) {
            // For loggingConfig
            try {
                serviceName = getMicroServiceNameFromLoggingConfig();
            } catch (Exception e2) {
            }
        }
        return serviceName;
    }

    private String getMicroServiceNameFromApxLogging()
    {
        return loggingConfig.getString("fluent.url");
    }

    private String getMicroServiceNameFromLoggingConfig()
    {
        String env = loggingConfig.getString("properties.fluent.tag");
        String appName = loggingConfig.getString("appName");
        return env + "." + appName;
    }

}
