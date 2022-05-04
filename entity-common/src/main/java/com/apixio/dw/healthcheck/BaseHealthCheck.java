package com.apixio.dw.healthcheck;

import java.util.Map;
import java.util.Properties;

import com.codahale.metrics.health.HealthCheck;

import com.apixio.logger.EventLogger;
import com.apixio.logger.LogHelper;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.config.MicroserviceConfig;

/**
 * Base class to wrap logging.
 */
public abstract class BaseHealthCheck extends HealthCheck
{

    private final EventLogger logger;

    public BaseHealthCheck(MicroserviceConfig config)
    {
        ConfigSet  logConfig = config.getLoggingConfig();

        this.logger = LogHelper.getEventLogger(ConfigUtil.toProperties(logConfig.getConfigSubtree(MicroserviceConfig.LOG_PROPERTIES).getProperties()),
                                               logConfig.getString(MicroserviceConfig.LOG_APPNAME), this.getLoggerName());
    }

    protected abstract String getLoggerName();

    protected abstract com.codahale.metrics.health.HealthCheck.Result runHealthCheck(EventLogger logger) throws Exception;

    protected final com.codahale.metrics.health.HealthCheck.Result check() throws Exception
    {
        try
        {
            return this.runHealthCheck(this.logger);
        }
        catch (Exception ex)
        {
            String errorMessage = String.format("Error calling health check for logger %s", this.getLoggerName());

            logger.error(errorMessage, ex);

            return com.codahale.metrics.health.HealthCheck.Result.unhealthy(errorMessage);
        }
    }

}
