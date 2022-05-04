package apixio.infraconfig.resources;

import apixio.infraconfig.InfraConfigConfiguration;
import apixio.infraconfig.client.VaultClient;
import apixio.infraconfig.config.AwsCredentialConfig;
import com.apixio.dw.healthcheck.BaseHealthCheck;
import com.apixio.logger.EventLogger;
import org.joda.time.DateTime;

import java.util.Objects;

public class VaultHealthCheck extends BaseHealthCheck {
    private String LOGGER_ID = "infra-config-service";
    private Boolean lasthHealthStatus = false;
    private DateTime healthTime;
    private Long checkInterval;
    private VaultClient vaultClient;

    public VaultHealthCheck(InfraConfigConfiguration configuration) {
        super(configuration);
        InfraConfigConfiguration config = configuration;
        AwsCredentialConfig awsConfig = config.getAwsCredentialConfig();
        String vaultAWSRole;
        if (Objects.nonNull(config.getVaultConfig().getAwsRole())) {
            vaultAWSRole = config.getVaultConfig().getAwsRole();
        } else {
            vaultAWSRole = awsConfig.getAwsRole();
        }
        this.vaultClient = new VaultClient(
                config.getVaultConfig().getVaultAddress(),
                config.getVaultConfig().getVaultRole(),
                vaultAWSRole,
                config.getVaultConfig().getSecretsEngineVersion(),
                awsConfig.getAccessKey(),
                awsConfig.getSecretKey()
        );
        this.healthTime = DateTime.now().minusYears(10);
        this.checkInterval = config.getVaultConfig().getHealthCheckInterval().longValue();

    }

    @Override
    protected String getLoggerName() {
        return "InfraConfig";
    }

    @Override
    public Result runHealthCheck(EventLogger logger) {
        // use an interval to avoid too many calls to vault for healthchecks
        if (healthTime.isBefore(DateTime.now().minus(checkInterval)) && lasthHealthStatus) {
            return Result.healthy();
        }
        try {
            if (vaultClient.healthcheck()) {
                return Result.healthy();
            } else {
                return Result.unhealthy("Error checking vault");
            }
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
}
