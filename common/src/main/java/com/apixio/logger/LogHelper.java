package com.apixio.logger;

import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.apixio.logger.api.Event;
import com.apixio.logger.fluentd.FluentAppender;

public class LogHelper
{
    private static Logger logger = Logger.getLogger(LogHelper.class);
    
    public static FluentAppender getFluentAppender(String host, String tag, String label, Level level)
    {
        FluentAppender appender = new FluentAppender(host, tag, label);
        
        appender.setThreshold(level);
        appender.activateOptions();
        return appender;
    }
    
    public static EventLogger getEventLogger(Properties logProps, String appName, String loggerName) 
    {
        final String FLUENT_URL = "fluent.url";
        final String FLUENT_TAG = "fluent.tag";
        
        EventLogger eventLogger = null;
        eventLogger = new EventLogger(loggerName, Logger.getRootLogger());
        
        if (logProps.getProperty("fluent")==null || logProps.getProperty("fluent").equalsIgnoreCase("true")) 
        {
            String host = logProps.getProperty(FLUENT_URL, "localhost");
            String tag = logProps.getProperty(FLUENT_TAG, "production_test");
            logger.info("Fluent: " + host + ", tag:" + tag + ", label:" + appName);
            Appender fapp = getFluentAppender(host, tag, appName, Level.INFO);
            AsyncAppender async = new AsyncAppender();
            async.setName("fluent");
            async.addAppender(fapp);
            async.setBlocking(true);
            async.setBufferSize(1000);
            eventLogger.addAppender(async);
        }
        eventLogger.log(Event.INFO, "Create "+ loggerName + " logger");
        return eventLogger;
    }
}
