package apixio.cardinalsystem;

import apixio.cardinalsystem.api.model.CardinalModelBase;
import apixio.cardinalsystem.core.CardinalSysServices;
import apixio.cardinalsystem.resources.v1.EventRS;
import apixio.cardinalsystem.tasks.CqlCreateStatments;
import apixio.cardinalsystem.tasks.CqlCreateTables;
import com.apixio.restbase.MicroserviceApplication;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class CardinalRestApplication extends MicroserviceApplication<CardinalRestConfiguration> {
    public static void main(final String[] args) throws Exception {
        new CardinalRestApplication().run(args);
    }

    @Override
    public String getName() {
        return "cardinalrest";
    }

    @Override
    public void initialize(final Bootstrap<CardinalRestConfiguration> bootstrap) {
        CardinalModelBase.configureObjectMapper(bootstrap.getObjectMapper());
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(final CardinalRestConfiguration configuration,
                    final Environment environment) throws Exception {
        JerseyEnvironment jersey = environment.jersey();
        CardinalSysServices sysServices = setupServices(configuration);
        jersey.register(new EventRS(configuration, sysServices));
        jersey.register(new JsonProcessingExceptionMapper(true));
        // register generic health check for now
        environment.healthChecks().register("pingcheck", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        });
        environment.admin().addTask(new CqlCreateStatments(sysServices));
        environment.admin().addTask(new CqlCreateTables(environment.getObjectMapper()));
    }

    /**
     * setupServices sets up the internal services required by the RESTful service
     * code.  These services are collected in the TokenizerSysServices object, which serves
     * as the central location for any singleton object another object might need.
     */
    public CardinalSysServices setupServices(CardinalRestConfiguration configuration) throws Exception {
        return super.createSysServices(configuration,
                (seed, configuration1) -> new CardinalSysServices(seed, configuration1));
    }

}
