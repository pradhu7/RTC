package apixio.infraconfig;

import apixio.infraconfig.api.SftpUserModel;
import apixio.infraconfig.api.SftpUserQuery;
import apixio.infraconfig.client.VaultClient;
import apixio.infraconfig.config.AwsCredentialConfig;
import apixio.infraconfig.core.SftpUserCacheLoader;
import com.apixio.SysServices;
import com.apixio.restbase.DaoBase;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.time.Duration;
import java.util.Objects;

public class InfraConfigSysServices extends SysServices {
    private CacheManager cacheManager;
    private VaultClient vaultClient;
    private AwsCredentialConfig awsConfig;
    private Cache<SftpUserQuery, SftpUserModel> sftpUserCache;
    private String sftpUserCacheAlias = "sftpUserCache";

    public InfraConfigSysServices(DaoBase seed, InfraConfigConfiguration config) {
        super(seed, config);
        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
        this.awsConfig = config.getAwsCredentialConfig();
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
        this.sftpUserCache = createUserCache(config);
    }

    public void close() {
        super.close();
        this.cacheManager.close();
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public VaultClient getVaultClient() {
        return vaultClient;
    }

    public Cache<SftpUserQuery, SftpUserModel> createUserCache(InfraConfigConfiguration configuration) {
        return getCacheManager().createCache(
                sftpUserCacheAlias,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                SftpUserQuery.class,
                                SftpUserModel.class,
                                ResourcePoolsBuilder.heap(configuration.getCacheConfig(sftpUserCacheAlias).getCacheEntries())
                                        .offheap(configuration.getCacheConfig(sftpUserCacheAlias).getCacheSizeInBytes(), MemoryUnit.B)
                                        .build()
                        )
                        .withExpiry(
                                ExpiryPolicyBuilder.timeToLiveExpiration(
                                        Duration.ofSeconds(configuration.getCacheConfig(sftpUserCacheAlias).getCacheTtlSeconds())
                                )
                        )
                        .withLoaderWriter(new SftpUserCacheLoader(this))
                        .build()
        );
    }

    public Cache getSftpUserCache() {
        return sftpUserCache;
    }
}
