package com.apixio.dw.healthcheck;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.logger.EventLogger;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.MicroserviceConfig;

/**
 * Redis health check. Sends ping to the server
 */
public final class RedisHealthCheck extends BaseHealthCheck
{

    private static final String REDIS_HEALTH_CHECK = "RedisHealthCheck";

    private RedisOps redisOps;

    public RedisHealthCheck(MicroserviceConfig config) throws Exception
    {
        super(config);

        ConfigSet cset = config.getPersistenceConfig();

        redisOps = new RedisOps(cset.getString(MicroserviceConfig.PST_REDIS_CONFIG_PREFIX + "." + MicroserviceConfig.PST_REDIS_HOST), cset.getInteger(MicroserviceConfig.PST_REDIS_CONFIG_PREFIX + "." + MicroserviceConfig.PST_REDIS_PORT));
    }

    // Need this to check your the redis ops instance is working
    public RedisHealthCheck(MicroserviceConfig config, RedisOps redisOps)
    {
        super(config);
        this.redisOps = redisOps;
    }

    @Override
    protected String getLoggerName()
    {
        return REDIS_HEALTH_CHECK;
    }

    @Override
    protected Result runHealthCheck(EventLogger logger) throws Exception
    {
        if (redisOps.ping())
            return Result.healthy();
        else
            return Result.unhealthy("Redis ping failed");
    }
}
