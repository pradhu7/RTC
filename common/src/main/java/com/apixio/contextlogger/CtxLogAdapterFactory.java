package com.apixio.contextlogger;

import com.apixio.logger.LogHelper;
import com.apixio.logger.fluentd.FluentAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fluentd.logger.FluentLogger;

/**
 * Helper class to build and add a FluentAppender to a logger.
 */

public class CtxLogAdapterFactory {
    /**
     * The direct way to create log adapters. Requires that the Log4J config is present to work.
     * @param fluentHost    The fluentd host to post messages to.
     * @param tag           The tag used to post logging.
     * @param label         The label used to post logs.
     * @param level         The log level that is the threshold to decide if we need to post.
     * @return A fresh new LogAdapter to use in logging call.
     */
    public static ICtxLoggerAdapter buildStdAdapter(String fluentHost, String tag, String label, CtxLogLevel level) {
        return new CtxStdLoggerAdapter(CtxLogAdapterFactory.build(fluentHost, tag, label, level));
    }

    /**
     * Adapter for the Cerebro logging.
     * @param tag           The tag used to post logging.
     * @param logger        An slf4j Logger object (provided by Cerebro env).
     * @param fluentLogger  A FluentLogger object (provided by Cerebro env).
     * @return A fresh new LogAdapter to use in logging call.
     */
    public static ICtxLoggerAdapter buildSlf4jAdapter(String tag, org.slf4j.Logger logger, FluentLogger fluentLogger) {
        return new CtxLogAdapterSlf4jFluent(tag, logger, fluentLogger);
    }

    /**
     * Builds a logger and attaches a fluentD appender.
     * @param fluentHost    The fluentd host to post messages to.
     * @param tag           The tag used to post logs.
     * @param label         The label used to post logs.
     * @param level         The log level that is the threshold to decide if we need to post.
     * @return Logger       A fresh build logger configured.
     */
    private static Logger build(String fluentHost, String tag, String label, CtxLogLevel level) {
        String prefix = tag + "." + label;
        Logger result = Logger.getLogger(prefix);
        addTo(result, fluentHost, tag, label, level);
        return result;
    }

    /**
     * Add Fluent appender to the logger.
     *
     * @param logger        The event Logger to attach the FluentAppender to.
     * @param fluentHost    Host to post fluentd messages.
     * @param tag           Tag used for logging.
     * @param label         THe label used for logging.
     * @param level         The log level that is the threshold to decide if we need to post.
     *
     */
    private static void addTo(Logger logger, String fluentHost, String tag, String label, CtxLogLevel level) {
        Level internalLevel = toLog4jLevel(level);
        FluentAppender fluentAppender = LogHelper.getFluentAppender(fluentHost, tag, label, internalLevel);
        logger.addAppender(fluentAppender);
    }

    /**
     * Simple bridge between our new logging and the internal logging library.
     * Our goal is to avoid exposing any constants.
     */
    private static Level toLog4jLevel(CtxLogLevel level) {
        switch (level) {
            case LOG_INFO: return Level.INFO;
            case LOG_WARNING: return Level.WARN;
            case LOG_ERROR: return Level.ERROR;
            case LOG_DEBUG: return Level.DEBUG;
            default: return Level.FATAL;
        }
    }
}
