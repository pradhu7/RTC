package com.apixio.useracct.dw.resources;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.MicroserviceApplication;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.google.common.io.Resources;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 *  Moving up common static fields required for all resource level tests created
 */
public class BaseResourceTest
{
    public static ServiceConfiguration serviceConfiguration = null;
    public static PrivSysServices sysServices = null;
    private static Logger LOGGER = LoggerFactory.getLogger(BaseResourceTest.class);

    protected static void mainSetup()
    {
        try
        {
            serviceConfiguration = (new YamlConfigurationFactory<ServiceConfiguration>(ServiceConfiguration.class,
                    Validators.newValidator(), Jackson.newObjectMapper(), "dw")).build(new File(Resources.getResource("user-account-stg.yaml").toURI()));
            MicroserviceApplication.SysServicesFactory<PrivSysServices, ServiceConfiguration> factory = new MicroserviceApplication.SysServicesFactory<PrivSysServices, ServiceConfiguration>() {
                public PrivSysServices createSysServices(DaoBase seed, ServiceConfiguration configuration)
                        throws Exception {
                    return new PrivSysServices(seed, true, configuration);
                }
            };

            PersistenceServices ps = ConfigUtil.createPersistenceServices(serviceConfiguration);
            DaoBase daoBase = new DaoBase(ps);
            sysServices = factory.createSysServices(daoBase, serviceConfiguration);
        } catch (IOException | ConfigurationException | URISyntaxException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
