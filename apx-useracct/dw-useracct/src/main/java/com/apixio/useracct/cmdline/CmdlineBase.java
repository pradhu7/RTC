package com.apixio.useracct.cmdline;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.config.MicroserviceConfig.ConfigArea;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dw.ServiceConfiguration;

/**
 */
public abstract class CmdlineBase
{
    // map from cmdline "-Dx=y" (the "x" name) to yaml key; for backward compatibility with scripts
    private static Map<String, String> cmdlineOverrides = new HashMap<>();
    static
    {
        cmdlineOverrides.put("redis.host",       "persistenceConfig.redisConfig.host");        // bit of a hack as we have to know full key
        cmdlineOverrides.put("redis.keyPrefix",  "persistenceConfig.redisConfig.keyPrefix");
        cmdlineOverrides.put("cassandra.hosts",  "persistenceConfig.cassandraConfig.hosts");
        cmdlineOverrides.put("acl.columnFamily", "apiaclConfig.aclColumnFamilyName");
    }

    protected static DaoBase daoBase;

    /**
     * setupServices sets up the internal services required by the RESTful service
     * code.  These services are collected in the SysServices object, which serves
     * as the central location for any singleton object another object might need.
     */
    protected static PrivSysServices setupServices(ConfigSet configuration) throws Exception
    {
        ConfigSet             psConfig  = configuration.getConfigSubtree(ConfigArea.PERSISTENCE.getYamlKey());
        ConfigSet             logConfig = configuration.getConfigSubtree(ConfigArea.LOGGING.getYamlKey());
        PersistenceServices   ps        = ConfigUtil.createPersistenceServices(psConfig, logConfig);
        ServiceConfiguration  svcConfig = new ServiceConfiguration();

        // a hack because cmdline shouldn't care about MicroserviceConfig...
        svcConfig.setMicroserviceConfig(configuration.getProperties());

        daoBase = new DaoBase(ps);

        return new PrivSysServices(daoBase, false, svcConfig);
    }

    /**
     * Read the (standard format) yaml configuration file and populate the Config object from it.
     */
    protected static ConfigSet readConfig(String filepath) throws Exception
    {
        ConfigSet            config = ConfigSet.fromYamlFile((new File(filepath)));
        Map<String, Object>  overs  = new HashMap<>();
        ConfigSet            perst;

        for (Map.Entry<String, String> entry : cmdlineOverrides.entrySet())
        {
            String key = entry.getKey();
            String val = System.getProperty(key, null);

            if (val != null)
                overs.put(entry.getValue(), val);
        }

        config.merge(overs);

        perst = config.getConfigSubtree(ConfigArea.PERSISTENCE.getYamlKey());

        // just getting these will test existence and throw exception if not there:
        perst.getString(MicroserviceConfig.PST_REDIS_CONFIG_PREFIX + "." + MicroserviceConfig.PST_REDIS_HOST);
        perst.getString(MicroserviceConfig.PST_REDIS_CONFIG_PREFIX + "." + MicroserviceConfig.PST_REDIS_PREFIX);

        // conditionally need Cassandra cluster
        if (perst.getString(MicroserviceConfig.pstCassHosts(PersistenceServices.CassandraCluster.INTERNAL.getID()), null) == null)
            System.out.println("WARNING:  no Cassandra configuration for INTERNAL cluster declared in yaml config");

        return config;
    }

}
