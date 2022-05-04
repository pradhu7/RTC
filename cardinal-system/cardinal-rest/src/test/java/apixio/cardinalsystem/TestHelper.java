package apixio.cardinalsystem;

import apixio.cardinalsystem.test.TestUtils;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.ResourceHelpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestHelper {
    private static String CONFIG_PATH = "config.yml";

    public static CardinalRestConfiguration getDropWizardConfig() throws IOException, ConfigurationException {
        return getDropWizardConfig(ResourceHelpers.resourceFilePath(CONFIG_PATH));
    }

    public static CardinalRestConfiguration getDropWizardConfig(String path) throws IOException, ConfigurationException {
        String resourcePath = ResourceHelpers.resourceFilePath("config.yml");
        CardinalRestApplication CardinalRestApplication = new CardinalRestApplication();
        Bootstrap bootstrap = new Bootstrap(CardinalRestApplication);
        CardinalRestApplication.initialize(bootstrap);
        ConfigurationFactory<CardinalRestConfiguration> configurationFactory = bootstrap.getConfigurationFactoryFactory()
                .create(CardinalRestConfiguration.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");
        return configurationFactory.build(bootstrap.getConfigurationSourceProvider(), resourcePath);
    }

    public static List<TrackingEventRecord> getTestEvents(Integer num) {
        List<TrackingEventRecord> events = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            events.add(TestUtils.generateRandomTrackingEvent());
        }
        return events;
    }
}
