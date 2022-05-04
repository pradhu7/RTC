package com.apixio.restbase.config;

/**
 * GlobalConfig holds the yaml-poked configuration that is global in nature (i.e.,
 * all RESTful services use it).
 */
public class GlobalConfig {
    private DwRedisConfig   redisConfig;
    private DwJedisConfig   jedisConfig;
    private DwJedisPool     jedisPool;
    private CassandraConfig cassandraConfig;
    private TokenConfig     tokenConfig;
    private LoggingConfig   loggingConfig;

    public void setRedisConfig(DwRedisConfig redisConfig)
    {
        this.redisConfig = redisConfig;
    }
    public DwRedisConfig getRedisConfig()
    {
        return redisConfig;
    }

    public void setJedisConfig(DwJedisConfig jedisConfig)
    {
        this.jedisConfig = jedisConfig;
    }
    public DwJedisConfig getJedisConfig()
    {
        return jedisConfig;
    }

    public void setJedisPool(DwJedisPool jedisPool)
    {
        this.jedisPool = jedisPool;
    }
    public DwJedisPool getJedisPool()
    {
        return jedisPool;
    }

    public void setCassandraConfig(CassandraConfig cassConfig)
    {
        this.cassandraConfig = cassConfig;
    }
    public CassandraConfig getCassandraConfig()
    {
        return cassandraConfig;
    }

    public void setTokenConfig(TokenConfig tokenConfig)
    {
        this.tokenConfig = tokenConfig;
    }
    public TokenConfig getTokenConfig()
    {
        return tokenConfig;
    }

    public void setLoggingConfig(LoggingConfig loggingConfig)
    {
        this.loggingConfig = loggingConfig;
    }
    public LoggingConfig getLoggingConfig()
    {
        return loggingConfig;
    }

    public String toString()
    {
        return ("Global: " + redisConfig + "; " + jedisConfig + "; " + jedisPool +
                "; " + tokenConfig + "; " + cassandraConfig + "; " + loggingConfig);
    }

}
