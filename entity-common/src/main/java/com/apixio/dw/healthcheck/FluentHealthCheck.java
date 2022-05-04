package com.apixio.dw.healthcheck;

import org.apache.log4j.Appender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

import com.apixio.logger.EventLogger;
import com.apixio.restbase.config.MicroserviceConfig;

public class FluentHealthCheck extends BaseHealthCheck
{
    private static final String FLUENT_HEALTH_CHECK = "FluentHealthCheck";
    private final String appenderName;

    public FluentHealthCheck(MicroserviceConfig config, String appenderName)
    {
        super(config);

        this.appenderName = appenderName;
    }

    @Override
    protected String getLoggerName()
    {
        return FLUENT_HEALTH_CHECK;
    }

    @Override
    protected Result runHealthCheck(EventLogger logger) throws Exception
    {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger", logger, Priority.INFO, this.getLoggerName(), null);
        Appender fluentAppender = logger.getAppender(this.appenderName);

        if(fluentAppender == null)
            return Result.unhealthy(String.format("Failed to find fluent appender with name %s",this.appenderName));

        fluentAppender.doAppend(event);

        return Result.healthy();
    }
}
