package com.apixio.sdk.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.apixio.logger.EventLogger;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.metric.Metric;
import com.apixio.sdk.util.ExceptionUtil;

/**
 * EccEventLogger delegates logging info to com.apixio.logging.EventLogger which sends
 * to fluentd (with correct configuration).
 */
public class EccEventLogger extends BaseLogger
{

    /**
     * A fallback logger just in case a component name isn't set
     */
    private static final org.apache.log4j.Logger defaultLogger = org.apache.log4j.Logger.getLogger(EccEventLogger.class);

    /**
     * Hold on to config to build new EventLoggers as needed
     */
    private ConfigSet baseLogConfig;

    /**
     * Map from component name to Logger
     */
    private Map<String, Logger> loggers = Collections.synchronizedMap(new HashMap<>());

    /**
     * Configure logger
     */
    public void configure(ConfigSet config)
    {
        System.out.println("EccEventLogger configuration: " + config);

        baseLogConfig = config;
    }

    /**
     * Current component info is managed by the infrastructure code and is thread-specific
     */
    @Override
    public void setCurrentComponentName(String component)
    {
        loggers.computeIfAbsent(component, k -> ConfigUtil.getEventLogger(baseLogConfig, component));
        super.setCurrentComponentName(component);
    }

    /**
     * Standard logging levels as methods
     */
    @Override
    public void info(String format, Object... args)
    {
        Logger logger = findLogger();

        if (logger.isInfoEnabled())
        {
            String fmt     = String.format(format, args);
            String context = getContext();

            if (context != null)
                logger.info("[ctx:" + context + "] " + fmt);
            else
                logger.info(fmt);
        }
    }

    @Override
    public void warn(String format, Object... args)
    {
        Logger logger = findLogger();

        if (logger.isEnabledFor(Priority.WARN))
        {
            String fmt     = String.format(format, args);
            String context = getContext();

            if (context != null)
                logger.warn("[ctx:" + context + "] " + fmt);
            else
                logger.warn(fmt);
        }
    }

    @Override
    public void debug(String format, Object... args)
    {
        Logger logger = findLogger();

        if (logger.isDebugEnabled())
        {
            String fmt     = String.format(format, args);
            String context = getContext();

            if (context != null)
                logger.debug("[ctx:" + context + "] " + fmt);
            else
                logger.debug(fmt);
        }
    }

    /**
     * "Error" level logging; if throwable form is called then stack trace is
     * included automatically 
     */
    @Override
    public void error(String format, Object... args)
    {
        Logger logger = findLogger();

        if (logger.isEnabledFor(Priority.ERROR))
        {
            String fmt     = String.format(format, args);
            String context = getContext();

            if (context != null)
                logger.error("[ctx:" + context + "] " + fmt);
            else
                logger.error(fmt);
        }
    }

    @Override
    public void error(String format, Throwable t, Object... args)
    {
        Logger logger = findLogger();

        if (logger.isEnabledFor(Priority.ERROR))
        {
            String fmt     = String.format(format, args);
            String context = getContext();
            String trace   = ExceptionUtil.oneLineStackTrace(t);

            if (context != null)
                logger.error("[ctx:" + context + "] " + fmt + "; " + trace);
            else
                logger.error(fmt);
        }
    }

    /**
     * This is mostly a pass-through to EventLogger.event().  It adds context information...
     * TODO: finish this...
     */
    @Override
    public void event(Map<String,Object> packet)
    {
        EventLogger logger = (EventLogger) findLogger();

        logger.event(packet);
    }

    /**
     * Returns the SLF4J-based Logger instance for the current component name (for this thread)
     * returning the default Logger if there isn't one (that case shouldn't happen, however).
     */
    private Logger findLogger()
    {
        Logger logger = loggers.get(getCurrentComponentName());

        return (logger != null) ? logger : defaultLogger;
    }

}
