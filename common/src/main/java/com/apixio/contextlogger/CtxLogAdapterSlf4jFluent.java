package com.apixio.contextlogger;

import org.fluentd.logger.FluentLogger;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CtxLogAdapterSlf4jFluent implements ICtxLoggerAdapter {
    // TODO: Review if need to forward the other log levels to fluent.
    //       Currently forwarding events like the current Cerebro logging.
    private final Logger logger;
    private final FluentLogger fluentLogger;
    private final String tag;

    public CtxLogAdapterSlf4jFluent(String tag, Logger logger, FluentLogger fluentLogger) {
        this.tag = tag;
        this.logger = logger;
        this.fluentLogger = fluentLogger;
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void event(String msg) {
        Map <String, Object> payload = new HashMap<>();
        payload.put("message", msg);
        payload.put("level", "EVENT");
        long theTime = System.currentTimeMillis() / 1000;
        this.fluentLogger.log(this.tag, payload, theTime);
        this.fluentLogger.flush();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void fatal(String msg) {
        // TODO: SFL4J does not have fatal. Need to standardise the interface.
        logger.debug(msg);
    }
}
