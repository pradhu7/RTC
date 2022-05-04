package com.apixio.restbase.config;

import java.util.Map;

/**
 * LoggincConfig holds the yaml-poked configuration for fluent/graphite logging.
 */
public class LoggingConfig {

    private String              appName;
    private String              loggerName;
    private Map<String, String> properties;

    public void setAppName(String name)
    {
        this.appName = name;
    }
    public String getAppName()
    {
        return appName;
    }

    public void setLoggerName(String name)
    {
        this.loggerName = name;
    }
    public String getLoggerName()
    {
        return loggerName;
    }

    public void setProperties(Map<String, String> props)
    {
        this.properties = props;
    }
    public Map<String, String> getProperties()
    {
        return properties;
    }

    public String toString()
    {
        return ("Logging: " +
                "appname=" + appName +
                "; loggername=" + loggerName +
                "; properties=" + properties
            );
    }

}
