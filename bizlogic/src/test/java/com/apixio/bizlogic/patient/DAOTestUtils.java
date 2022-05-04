package com.apixio.bizlogic.patient;

import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.restbase.config.ConfigSet;

import java.io.File;

public class DAOTestUtils
{
    public ConfigSet config;
    public DaoServices daoServices;

    public DAOTestUtils() throws Exception
    {
        File configFile = new File(DAOTestUtils.class.getClassLoader().getResource("config.yaml").getFile());
        config = ConfigSet.fromYamlFile((configFile));
        daoServices = DaoServicesSet.createDaoServices(config);
    }
}


