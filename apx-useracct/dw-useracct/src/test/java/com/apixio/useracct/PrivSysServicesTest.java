package com.apixio.useracct;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.messager.AWSSNSMessenger;
import com.google.common.io.Resources;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;


public class PrivSysServicesTest
{
    private static ServiceConfiguration configuration;
    private static PrivSysServices privSysServices;
    private static boolean isSetupDone;             // run setup once


    @Before
    public void setUp() throws Exception
    {
        if (!isSetupDone)
        {
            configuration = (new YamlConfigurationFactory<ServiceConfiguration>(ServiceConfiguration.class,
                    Validators.newValidator(), Jackson.newObjectMapper(), "dw")).build(new File(Resources.getResource("user-account-stg.yaml").toURI()));

            PersistenceServices ps = ConfigUtil.createPersistenceServices(configuration);
            DaoBase daoBase = new DaoBase(ps);
            privSysServices = new PrivSysServices(daoBase, false, configuration);

            isSetupDone = true;
        }
    }

    /**
     * Only AWSSNS is supported for now
     * Verify configuration
     */
    @Test
    public void testMessengerConfiguration()
    {
        if(configuration.getMessengerConfig()!=null) {
            Assert.assertEquals(configuration.getMessengerConfig().getRegion(), AWSSNSMessenger.US_WEST_2);
            Assert.assertEquals(configuration.getMessengerConfig().getServiceType(), AWSSNSMessenger.AWSSNS);
            Assert.assertNotNull(configuration.getMessengerConfig().getEncryptedAccessKey());
            Assert.assertNotNull(configuration.getMessengerConfig().getEncryptedSecretAccessKey());
        }
    }

    @Test
    public void testVerifyMessengerType()
    {
        Assert.assertEquals(privSysServices.getMessenger().getMessengertype(), AWSSNSMessenger.AWSSNS);
    }

}
