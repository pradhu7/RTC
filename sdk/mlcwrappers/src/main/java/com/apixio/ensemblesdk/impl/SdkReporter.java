package com.apixio.ensemblesdk.impl;

import java.util.Map;

import com.apixio.ensemble.ifc.Reporter;
import com.apixio.sdk.FxLogger;

/**
 * Pass-thru to SDK implementation.
 */
public class SdkReporter implements Reporter
{

    private FxLogger logger;

    SdkReporter(FxLogger logger)
    {
        this.logger = logger;
    }

    @Override
    public void info(String message, Object context)
    {
        logger.info(message, context);  //!! not sure how context is used
    }

    @Override
    public void warn(String message, Object context)
    {
        logger.warn(message, context);
    }

    @Override
    public void error(String message, Object context)
    {
        logger.error(message, context);
    }

    @Override
    public void debug(String message, Object context)
    {
        logger.debug(message, context);
    }

    @Override
    public void event(Map<String,Object> packet)
    {
        logger.event(packet);
    }

}
