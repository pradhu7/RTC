package apixio.infraconfig.core;

import apixio.infraconfig.InfraConfigSysServices;
import apixio.infraconfig.api.SftpUserModel;
import apixio.infraconfig.api.SftpUserQuery;
import apixio.infraconfig.client.VaultClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;

import java.util.Map;

public class SftpUserCacheLoader implements CacheLoaderWriter<SftpUserQuery, SftpUserModel> {
    private InfraConfigSysServices sysServices;
    private VaultClient vaultClient;
    private ObjectMapper objectMapper;

    public SftpUserCacheLoader(InfraConfigSysServices sysServices) {
        this.sysServices = sysServices;
        this.vaultClient = sysServices.getVaultClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SftpUserModel load(SftpUserQuery key) throws Exception {
        return objectMapper.readValue(vaultClient.getObject(key.getVaultPath()).toString(), SftpUserModel.class);
    }

    @Override
    public void write(SftpUserQuery key, SftpUserModel value) throws Exception {
        Map<String, Object> vaultData = objectMapper.convertValue(value, Map.class);
        vaultClient.writeObject(key.getVaultPath(), vaultData);
    }

    @Override
    public void delete(SftpUserQuery key) throws Exception {
        vaultClient.deleteObject(key.getVaultPath());
    }
}
