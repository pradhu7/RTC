package com.apixio.contextlogger;

import com.apixio.logger.api.Event;
import org.apache.log4j.Logger;


public class CtxStdLoggerAdapter implements ICtxLoggerAdapter  {
    private final Logger logger;

    public CtxStdLoggerAdapter(Logger logger) {
        this.logger = logger;
    }

    public void error(String msg) {
        logger.error(msg);
    }

    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void event(String msg) {
        logger.log(Event.EVENT, msg);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void fatal(String msg) {
        // TODO: SFL4J does not have fatal. Need to standardise this interface or remove it.
        logger.debug(msg);
    }
}
