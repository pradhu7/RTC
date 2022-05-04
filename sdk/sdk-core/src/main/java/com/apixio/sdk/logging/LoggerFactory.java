package com.apixio.sdk.logging;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.FxLogger;

/**
 * LoggerFactory creates an instance of the FxLogger specified by the configuration and
 * configures it for use.
 */
public class LoggerFactory
{

    /**
     * Expected structure of config keys:
     *
     *  classname:       # must extend com.apixio.sdk.logging.BaseLogger
     *  config:          # subtree will be passed to BaseLogger.configure()
     */
    public static FxLogger createLogger(ConfigSet config) throws Exception
    {
        Class<? extends BaseLogger> logClass = (Class<? extends BaseLogger>) Class.forName(config.getString("classname"));
        BaseLogger                  logger   = logClass.newInstance();

        logger.configure(config.getConfigSubtree("config"));
        logger.setDefaultComponentName();

        return logger;
    }

}
