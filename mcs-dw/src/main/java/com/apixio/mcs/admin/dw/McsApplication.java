package com.apixio.mcs.admin.dw;

import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.apixio.dw.healthcheck.FluentHealthCheck;
import com.apixio.dw.healthcheck.RedisHealthCheck;
import com.apixio.mcs.admin.McsSysServices;
import com.apixio.mcs.admin.dw.resources.*;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.MicroserviceApplication;

/**
 * McsApplication is a drop-wizard service that provides RESTful APIs for coord v2
 */
public class McsApplication extends MicroserviceApplication<McsConfig> {

    public static void main(String[] args) throws Exception
    {
        new McsApplication().run(args);
    }

    @Override
    public String getName()
    {
        return "Apixio Model Catalog Service";
    }

    @Override
    public void initialize(Bootstrap<McsConfig> bootstrap)
    {
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
            )
        );
    }

    @Override
    public void run(McsConfig configuration, Environment environment) throws Exception {
        McsSysServices     sysServices = setupServices(configuration);
        JerseyEnvironment  jersey      = environment.jersey();

        jersey.register(new ModelRS(configuration, sysServices));
        jersey.register(new PartRS(configuration, sysServices));
        jersey.register(new LogicalIDsRS(configuration, sysServices));
        jersey.register(new FxDefRS(configuration, sysServices));

        environment.healthChecks().register("Fluent", new FluentHealthCheck(configuration,"fluent"));
        environment.healthChecks().register("Redis",  new RedisHealthCheck(configuration));

        super.setupApiAcls(environment, sysServices,
                           super.setupFilters(configuration, environment, sysServices),
                           configuration);

        (new DraftReaper(sysServices.getModelCatalogService(), configuration.getMcsConfig())).start();
    }

    /**
     * setupServices sets up the internal services required by the RESTful service
     * code.  These services are collected in the SysServices object, which serves
     * as the central location for any singleton object another object might need.
     */
    private McsSysServices setupServices(McsConfig configuration) throws Exception
    {
        McsSysServices    sysServices;

        sysServices = super.createSysServices(configuration,
                                              (DaoBase seed, McsConfig cfg) -> new McsSysServices(seed, cfg));

        return sysServices;
    }

}
