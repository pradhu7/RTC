package com.apixio.restbase.config;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.dropwizard.setup.Environment;

import com.apixio.SysServices;
import com.apixio.logger.EventLogger;
import com.apixio.logger.LogHelper;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.apiacl.ApiAcls.LogLevel;
import com.apixio.restbase.apiacl.ApiAcls;
import com.apixio.restbase.apiacl.dwutil.ApiReaderInterceptor;
import com.apixio.restbase.web.AclChecker;
import com.apixio.restbase.web.AclContainerFilter;
import com.apixio.restbase.web.Microfilter;

/**
 *
 */
public class ConfigUtil
{
    /**
     *
     */
    public static PersistenceServices createPersistenceServices(MicroserviceConfig config)
    {
        return createPersistenceServices(config.getPersistenceConfig(), config.getLoggingConfig());
    }
    public static PersistenceServices createPersistenceServices(ConfigSet config, ConfigSet loggingConfig)
    {
        return new PersistenceServices(config, loggingConfig);
    }

    /* ################################################################
       5/13/2016 TEMPORARY ONLY (?) Necessary because some scala code
       can't extend MicroserviceApplication but still needs to set up
       API ACls using this central code.  When that code has been
       changed then this code should be moved back to MicroserviceApplication!
    */

    /**
     * Set up API ACL checking by initializing that subsystem and modifying the
     * AclChecker filter (if it's been installed) and registering the ReaderInterceptor
     * with the JAX-RS container (only dropwizard 0.8.1+ supports this).
     */
    public static void setupApiAcls(Environment environment, SysServices sysServices, List<Microfilter> filters, MicroserviceConfig config) throws FileNotFoundException, IOException
    {
        ConfigSet aclConfig = config.getAclConfig();

        if (aclConfig != null)
        {
            String jsonFile     = aclConfig.getString(MicroserviceConfig.ACL_DEFFILE);
            Long   cacheTimeout = aclConfig.getLong(MicroserviceConfig.ACL_CACHETIMEOUT);

            if (cacheTimeout != null)
                sysServices.getAclLogic().setHasPermissionCacheTimeout(cacheTimeout.longValue());

            if (jsonFile != null)
            {
                ApiAcls                acls        = ApiAcls.fromJsonFile(sysServices, jsonFile);
                ApiReaderInterceptor   interceptor = new ApiReaderInterceptor(acls.getPermissionEnforcer());
                String                 aclDebug    = aclConfig.getString(MicroserviceConfig.ACL_DEBUG, null);

                if (aclDebug != null)
                {
                    ConfigSet lc = config.getLoggingConfig();

                    if (lc != null)
                        acls.setLogLevel(LogLevel.valueOf(aclDebug.toUpperCase()), getEventLogger(lc, null));  // use default logger name
                }

                environment.jersey().register(interceptor);
                environment.jersey().register(new AclContainerFilter());  // because messing with ResponseWrappers isn't sufficient...

                for (Microfilter mf : filters)
                {
                    if (mf instanceof AclChecker)
                        ((AclChecker) mf).setApiAcls(acls);
                }
            }
        }
    }

    /**
     * Central method to create an event logger given logger configuration.
     */
    public static EventLogger getEventLogger(ConfigSet config, String loggerName)
    {
        ConfigSet logConfig = config.getConfigSubtree(MicroserviceConfig.LOG_PROPERTIES);

        if (logConfig != null)
        {
            if (loggerName == null)
                loggerName = config.getString(MicroserviceConfig.LOG_DEFLOGGERNAME);


            // What a hack. To fix Lance's perfect code!!!
            String cleanLoggerName = loggerName != null ? loggerName.replaceAll("[^A-Za-z0-9]", "_") : null;
            String appName         = config.getString(MicroserviceConfig.LOG_APPNAME);
            String cleanAppName    = appName != null ? appName.replaceAll("[^A-Za-z0-9]", "_") : null;

            return LogHelper.getEventLogger(toProperties(logConfig.getProperties()),
                                            cleanAppName,
                                            cleanLoggerName);
        }
        else
        {
            return null;
        }
    }

    public static EventLogger getEventLogger(ConfigSet config, String loggerName, String appName)
    {
        ConfigSet logConfig = config.getConfigSubtree(MicroserviceConfig.LOG_PROPERTIES);

        if (logConfig != null)
        {
            if (loggerName == null)
                loggerName = config.getString(MicroserviceConfig.LOG_DEFLOGGERNAME);

            if (appName == null)
                appName = config.getString(MicroserviceConfig.LOG_APPNAME);

            // What a hack. To fix Lance's perfect code!!!
            String cleanLoggerName = loggerName != null ? loggerName.replaceAll("[^A-Za-z0-9]", "_") : null;
            String cleanAppName    = appName != null ? appName.replaceAll("[^A-Za-z0-9]", "_") : null;
 
            return LogHelper.getEventLogger(toProperties(logConfig.getProperties()),
                                            cleanAppName,
                                            cleanLoggerName);
        }
        else
        {
            return null;
        }
    }

    /**
     * Convert Map<String, String> to its equivalent Properties object.
     */
    public static Properties toProperties(Map<String, Object> map)
    {
        Properties props = new Properties();

        for (Map.Entry<String, Object> entry : map.entrySet())
            props.setProperty(entry.getKey(), entry.getValue().toString());

        return props;
    }

}
