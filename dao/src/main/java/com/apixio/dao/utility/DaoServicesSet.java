package com.apixio.dao.utility;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.config.MicroserviceConfig;

public class DaoServicesSet
{
    public static DaoServices createDaoServices(ConfigSet configuration)
    {
        ConfigSet           psConfig  = configuration.getConfigSubtree(MicroserviceConfig.ConfigArea.PERSISTENCE.getYamlKey());
        PersistenceServices ps        = ConfigUtil.createPersistenceServices(psConfig, psConfig.getConfigSubtree((MicroserviceConfig.ConfigArea.LOGGING.getYamlKey())));
        DaoBase             daoBase   = new DaoBase(ps); // doesn't this cause redis connection and breaks the on demand model

        return new DaoServices(daoBase, configuration);
    }
}
