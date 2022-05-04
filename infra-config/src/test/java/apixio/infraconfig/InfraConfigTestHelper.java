package apixio.infraconfig;

import com.apixio.SysServices;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class InfraConfigTestHelper {
    private static String CONFIG_PATH = "config.yml";

    public static InfraConfigConfiguration getDropWizardConfig() throws IOException, ConfigurationException {
        return getDropWizardConfig(CONFIG_PATH);
    }

    public static InfraConfigConfiguration getDropWizardConfig(String path) throws IOException, ConfigurationException {
        return getDropWizardConfig(path, new HashMap<>());
    }

    public static InfraConfigConfiguration getDropWizardConfig(String path, Map<String, String> templateConfig) throws IOException, ConfigurationException {
        String resourcePath = ResourceHelpers.resourceFilePath(path);
        StringSubstitutor stringSubstitutor = new StringSubstitutor(templateConfig);
        String templateResult = stringSubstitutor.replace(new String(Files.readAllBytes(Paths.get(resourcePath))));
        File templateResultFile = File.createTempFile("unit-test-dw", ".yml");
        templateResultFile.deleteOnExit();
        Files.write(templateResultFile.toPath(), templateResult.getBytes(StandardCharsets.UTF_8));


        InfraConfigApplication infraConfigApplication = new InfraConfigApplication();
        Bootstrap bootstrap = new Bootstrap(infraConfigApplication);
        infraConfigApplication.initialize(bootstrap);
        ConfigurationFactory<InfraConfigConfiguration> configurationFactory = bootstrap.getConfigurationFactoryFactory()
                .create(InfraConfigConfiguration.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");
        return configurationFactory.build(bootstrap.getConfigurationSourceProvider(), templateResultFile.getPath());
    }

    public static SysServices getSysServices() throws Exception {
        return getSysServices(CONFIG_PATH);
    }

    public static SysServices getSysServices(String path) throws Exception {
        return new InfraConfigApplication().setupServices(getDropWizardConfig(path));
    }
}
