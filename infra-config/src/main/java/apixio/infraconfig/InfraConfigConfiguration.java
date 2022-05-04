package apixio.infraconfig;

import apixio.infraconfig.config.ApxVaultConfig;
import apixio.infraconfig.config.AwsCredentialConfig;
import apixio.infraconfig.config.CacheConfig;
import apixio.infraconfig.config.SftpServerConfig;
import com.apixio.restbase.config.MicroserviceConfig;

import java.util.HashMap;
import java.util.Map;

public class InfraConfigConfiguration extends MicroserviceConfig {
    private AwsCredentialConfig awsCredentialConfig;
    private Map<String, SftpServerConfig> sftpServersConfig;
    private ApxVaultConfig vaultConfig;
    private Map<String, CacheConfig> cacheConfigs = new HashMap<>();

    public ApxVaultConfig getVaultConfig() {
        return vaultConfig;
    }

    public void setVaultConfig(ApxVaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
    }

    public AwsCredentialConfig getAwsCredentialConfig() {
        return awsCredentialConfig;
    }

    public void setAwsCredentialConfig(AwsCredentialConfig awsCredentialConfig) {
        this.awsCredentialConfig = awsCredentialConfig;
    }

    public Map<String, SftpServerConfig> getSftpServersConfig() {
        return sftpServersConfig;
    }

    public void setSftpServersConfig(Map<String, SftpServerConfig> sftpServersConfig) {
        this.sftpServersConfig = sftpServersConfig;
    }

    public Map<String, CacheConfig> getCacheConfigs() {
        return cacheConfigs;
    }

    public CacheConfig getCacheConfig(String cacheName) {
        return cacheConfigs.getOrDefault(cacheName, new CacheConfig());
    }

    public void setCacheConfigs(Map<String, CacheConfig> cacheConfigs) {
        this.cacheConfigs = cacheConfigs;
    }
}
