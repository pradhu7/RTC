package com.apixio.bizlogic.common;

import com.apixio.bizlogic.patient.logic.PatientLogic;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.restbase.config.ConfigSet;

import java.io.File;

public class TestBase {
    public ConfigSet config;
    public DaoServices daoServices;
    public PatientLogic patientLogic;

    public void setup() throws Exception {
        File configFile = new File(TestBase.class.getClassLoader().getResource("config.yaml").getFile());
        config = ConfigSet.fromYamlFile((configFile));
        daoServices = DaoServicesSet.createDaoServices(config);
        patientLogic = new PatientLogic(daoServices);
    }
}
