package apixio.infraconfig.config;

import java.net.URI;
import java.util.Objects;

public class ApxVaultConfig {
    private URI vaultAddress;
    private String vaultRole;
    private Integer openTimeout;
    private Integer readTimeout;
    private String awsRole;
    private Integer secretsEngineVersion;
    private Integer healthCheckInterval = 60000;

    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public Integer getSecretsEngineVersion() {
        if (Objects.isNull(secretsEngineVersion)) {
            return 1;
        } else {
            return secretsEngineVersion;
        }
    }

    public void setSecretsEngineVersion(Integer secretsEngineVersion) {
        this.secretsEngineVersion = secretsEngineVersion;
    }

    public String getAwsRole() {
        return awsRole;
    }

    public void setAwsRole(String awsRole) {
        this.awsRole = awsRole;
    }

    public URI getVaultAddress() {
        return vaultAddress;
    }

    public void setVaultAddress(URI vaultAddress) {
        this.vaultAddress = vaultAddress;
    }

    public Integer getOpenTimeout() {
        return openTimeout;
    }

    public void setOpenTimeout(Integer openTimeout) {
        this.openTimeout = openTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getVaultRole() {
        return vaultRole;
    }

    public void setVaultRole(String vaultRole) {
        this.vaultRole = vaultRole;
    }

}
