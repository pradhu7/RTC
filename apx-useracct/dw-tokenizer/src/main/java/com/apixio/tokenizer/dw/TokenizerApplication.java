package com.apixio.tokenizer.dw;

import com.apixio.tokenizer.dw.resources.HelpRS;
import com.apixio.tokenizer.dw.resources.UtilRS;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;

import com.apixio.SysServices;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.MicroserviceApplication;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.tokenizer.dw.resources.*;

/**
 * TokenizerService is the root-level dropwizard class that is the RESTful tokenization
 * service.
 */
public class TokenizerApplication extends MicroserviceApplication<MicroserviceConfig> {

    public static void main(String[] args) throws Exception
    {
        new TokenizerApplication().run(args);
    }

    @Override
    public String getName()
    {
        return "Apixio Tokenizer Edge Server Security Service (ATESSS?)";
    }

    @Override
    public void initialize(Bootstrap<MicroserviceConfig> bootstrap)
    {
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
            )
        );
    }

    @Override
    public void run(MicroserviceConfig configuration, Environment environment) throws Exception
    {
        SysServices          sysServices = setupServices(configuration);
        JerseyEnvironment    jersey      = environment.jersey();

        super.setupFilters(configuration, environment, sysServices);

        jersey.register(new SessionRS(configuration, sysServices));
        jersey.register(new TokenRS(configuration,   sysServices));
        jersey.register(new UtilRS(configuration,    sysServices));
        
        environment.admin().addServlet("help-server", new HelpRS()).addMapping("/help");

        //        environment.addHealthCheck(new TemplateHealthCheck(template));
    }

    /**
     * setupServices sets up the internal services required by the RESTful service
     * code.  These services are collected in the TokenizerSysServices object, which serves
     * as the central location for any singleton object another object might need.
     */
    private SysServices setupServices(MicroserviceConfig configuration) throws Exception
    {
        return super.createSysServices(configuration,
                                       new MicroserviceApplication.SysServicesFactory<SysServices, MicroserviceConfig>() {
                                           public SysServices createSysServices(
                                               DaoBase seed, MicroserviceConfig configuration
                                               ) throws Exception
                                           {
                                               return new SysServices(seed, configuration);
                                           }
                                       });
    }

}
