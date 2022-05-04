package apixio.infraconfig;

import apixio.infraconfig.resources.SftpV1RS;
import apixio.infraconfig.resources.VaultHealthCheck;
import com.apixio.dw.healthcheck.RedisHealthCheck;
import com.apixio.restbase.MicroserviceApplication;
import com.apixio.restbase.web.Microfilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.List;

public class InfraConfigApplication extends MicroserviceApplication<InfraConfigConfiguration> {

    public static void main(final String[] args) throws Exception {
        new InfraConfigApplication().run(args);
    }

    @Override
    public String getName() {
        return "infra-config";
    }

    @Override
    public void initialize(final Bootstrap<InfraConfigConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(final InfraConfigConfiguration configuration,
                    final Environment environment) throws Exception {
        InfraConfigSysServices sysServices = setupServices(configuration);
        JerseyEnvironment jersey = environment.jersey();
        List<Microfilter> filters = super.setupFilters(configuration, environment, sysServices);
        jersey.register(new SftpV1RS(configuration, sysServices));
        environment.healthChecks().register("Vault", new VaultHealthCheck(configuration));
        environment.healthChecks().register("Redis", new RedisHealthCheck(configuration, sysServices.getRedisOps("")));
        jersey.register(new JsonProcessingExceptionMapper(true));
        super.setupApiAcls(environment, sysServices, filters, configuration);
    }

    /**
     * setupServices sets up the internal services required by the RESTful service
     * code.  These services are collected in the TokenizerSysServices object, which serves
     * as the central location for any singleton object another object might need.
     */
    public InfraConfigSysServices setupServices(InfraConfigConfiguration configuration) throws Exception {
        return super.createSysServices(configuration,
                (seed, configuration1) -> new InfraConfigSysServices(seed, configuration1));
    }

}
