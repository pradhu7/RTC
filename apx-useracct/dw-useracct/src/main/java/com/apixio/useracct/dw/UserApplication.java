package com.apixio.useracct.dw;

import java.util.List;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;

import com.apixio.SysServices;
import com.apixio.dw.healthcheck.FluentHealthCheck;
import com.apixio.dw.healthcheck.RedisHealthCheck;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.MicroserviceApplication;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.web.Microfilter;
import com.apixio.useracct.PrivSysServices;

import com.apixio.useracct.dw.resources.*;

public class UserApplication extends MicroserviceApplication<ServiceConfiguration>
{
    public static void main(String[] args) throws Exception
    {
        new UserApplication().run(args);
    }

    @Override
    public String getName()
    {
        return "Apixio User Account Service";
    }

    @Override
    public void initialize(Bootstrap<ServiceConfiguration> bootstrap)
    {
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
            )
        );
    }

    @Override
    public void run(ServiceConfiguration configuration, Environment environment) throws Exception
    {
        PrivSysServices   sysServices = setupServices(configuration);
        List<Microfilter> filters     = super.setupFilters(configuration, environment, sysServices);

        checkSysState(sysServices);

        //            PdsTester.test(sysServices); if (true) System.exit(1);
        //            UserProjectsTester.test(sysServices); if (true) System.exit(1);
        //            RbacTester.test(sysServices); if (true) System.exit(1);

        environment.jersey().register(new AccessTypeRS(configuration, sysServices));
        environment.jersey().register(new AuthRS(configuration, sysServices));
        environment.jersey().register(new BatchRS(configuration, sysServices));
        environment.jersey().register(new DebugRS(configuration, sysServices));
        environment.jersey().register(new GrantRS(configuration, sysServices));
        environment.jersey().register(new OperationRS(configuration, sysServices));
        environment.jersey().register(new OrganizationRS(configuration, sysServices));
        environment.jersey().register(new PasswordPolicyRS(configuration, sysServices));
        environment.jersey().register(new PatientDataSetRS(configuration, sysServices));
        environment.jersey().register(new PermissionRS(configuration, sysServices));
        environment.jersey().register(new ProjectRS(configuration, sysServices));
        environment.jersey().register(new RoleSetsRS(configuration, sysServices));
        environment.jersey().register(new TextBlobRS(configuration, sysServices));
        environment.jersey().register(new UserRS(configuration, sysServices));
        environment.jersey().register(new UtilRS(configuration, sysServices));
        environment.jersey().register(new VerifyRS(configuration, sysServices));

        environment.healthChecks().register("Fluent",    new FluentHealthCheck(configuration, "fluent"));
        environment.healthChecks().register("Redis",     new RedisHealthCheck(configuration, sysServices.getRedisOps()));

        environment.admin().addServlet("help-server", new HelpRS()).addMapping("/help");

        super.setupApiAcls(environment, sysServices, filters, configuration);
    }

    /**
     * Check the system state by (currently only) peforming any needed schema updates.
     */
    private void checkSysState(PrivSysServices sysServices) throws Exception
    {
        sysServices.getSysStateLogic().performSchemaUpgrades();
    }

    /**
     * setupServices sets up the internal services required by the RESTful service
     * code.  These services are collected in the SysServices object, which serves
     * as the central location for any singleton object another object might need.
     */
    private PrivSysServices setupServices(ServiceConfiguration configuration) throws Exception
    {
        PrivSysServices sysServices;

        sysServices = super.createSysServices(configuration,
                                              new MicroserviceApplication.SysServicesFactory<PrivSysServices, ServiceConfiguration>() {
                                                  public PrivSysServices createSysServices(
                                                      DaoBase seed, ServiceConfiguration configuration
                                                      ) throws Exception
                                                  {
                                                      return new PrivSysServices(seed, true, configuration);
                                                  }
                                              });

        return sysServices;
    }

}
