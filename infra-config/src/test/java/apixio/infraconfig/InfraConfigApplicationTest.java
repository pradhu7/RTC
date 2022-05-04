package apixio.infraconfig;

import apixio.infraconfig.api.SftpUserModel;
import apixio.infraconfig.config.ApxVaultConfig;
import apixio.infraconfig.config.AwsCredentialConfig;
import apixio.infraconfig.config.SftpServerConfig;
import apixio.infraconfig.core.InvalidSftpUserException;
import apixio.infraconfig.testhelpers.VaultTestHelper;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.ianferguson.vault.VaultException;
import io.ianferguson.vault.response.LogicalResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.iam.model.AccessKey;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.User;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InfraConfigApplicationTest extends VaultTestHelper {
    public static DropwizardAppRule<InfraConfigConfiguration> RULE;
    public static Client client;
    public static String sftpServerId;
    public static String sftpServerApiEndpiont;
    public static GenericContainer redisContainer = new GenericContainer(DockerImageName.parse("redis:6.2"))
            .withExposedPorts(6379);

    @BeforeClass
    public static void beforeClass() throws Exception {
        VaultTestHelper.beforeClass();
        redisContainer.start();
        Map<String, String> templateValues = new HashMap<>();
        templateValues.put("redisHost", redisContainer.getHost());
        templateValues.put("redisPort", redisContainer.getFirstMappedPort().toString());
        InfraConfigConfiguration localConfig = InfraConfigTestHelper.getDropWizardConfig("config-unit.yml", templateValues);
        initAwsMocks();
        Pair<User, AccessKey> userAccessKeyPair = setupAwsUser();
        Role role = setupAwsRole();
        String vaultRole = setupVaultAwsAuth(role);
        Pair<List<String>, String> bucketAndSg = setupTestBucketSg();

        AwsCredentialConfig awsCredentialConfig = new AwsCredentialConfig();
        awsCredentialConfig.setSecretKey(userAccessKeyPair.getRight().secretAccessKey());
        awsCredentialConfig.setAccessKey(userAccessKeyPair.getRight().accessKeyId());
        awsCredentialConfig.setAwsRole(role.arn());

        ApxVaultConfig vaultConfig = new ApxVaultConfig();
        vaultConfig.setAwsRole(role.arn());
        vaultConfig.setVaultAddress(vaulturl);
        vaultConfig.setVaultRole(vaultRole);

        Map<String, SftpServerConfig> sftpServerConfigMap = new HashMap<>();
        SftpServerConfig sftpServerConfig = new SftpServerConfig();
        sftpServerConfig.setRegion(defaultRegion);
        sftpServerConfig.setSecurityGroupId(bucketAndSg.getLeft());
        sftpServerConfig.setS3Bucket(bucketAndSg.getRight());
        sftpServerId = UUID.randomUUID().toString();
        sftpServerApiEndpiont = String.format("api/v1/sftp/%s", sftpServerId);
        sftpServerConfigMap.put(sftpServerId, sftpServerConfig);

        localConfig.setAwsCredentialConfig(awsCredentialConfig);
        localConfig.setVaultConfig(vaultConfig);
        localConfig.setSftpServersConfig(sftpServerConfigMap);

        RULE = new DropwizardAppRule<>(InfraConfigApplication.class, localConfig);
        RULE.getTestSupport().before();
        client = RULE.client();
    }

    @AfterClass
    public static void afterClass() {
        redisContainer.stop();
        RULE.getTestSupport().after();
        VaultTestHelper.afterClass();
    }

    private URI getUri(String path) {
        return URI.create(String.format("http://localhost:%d/%s", RULE.getLocalPort(), path));
    }

    private String createTestUser() throws InvalidSftpUserException {
        String sftpUser = UUID.randomUUID().toString();
        SftpUserModel newUser = SftpUserModel.SftpUserModelBuilder.builder()
                .withAcceptedIpNetwork(Arrays.asList("127.0.0.1"))
                .withGroup("INTERNAL")
                .build();
        Response response = client.target(getUri(String.format("%s/users/%s", sftpServerApiEndpiont, sftpUser)))
                .request()
                .post(Entity.json(newUser));
        assertEquals(201, response.getStatus());
        return sftpUser;
    }

    private LogicalResponse getUserFromVault(String sftpUser) throws VaultException {
        return rootTokenVault.logical().read(String.format("secret/sftp/%s/%s", sftpServerId, sftpUser));
    }

    @Test
    public void testCreateUser() throws Exception {
        String sftpUser = UUID.randomUUID().toString();
        SftpUserModel newUser = SftpUserModel.SftpUserModelBuilder.builder()
                .withAcceptedIpNetwork(Arrays.asList("127.0.0.1"))
                .withGroup("INTERNAL")
                .build();
        Response response = client.target(getUri(String.format("%s/users/%s", sftpServerApiEndpiont, sftpUser)))
                .request()
                .post(Entity.json(newUser));
        assertTrue(response.getStatus() == 201);
        LogicalResponse vaultResponse = getUserFromVault(sftpUser);
        assertTrue(vaultResponse.getRestResponse().getStatus() == 200);
        System.out.println(vaultResponse.getData());
        assertEquals(sftpUser, vaultResponse.getData().get("Username"));
        assertEquals(sftpServerId, vaultResponse.getData().get("SFTPServerID"));
    }

    @Test
    public void testReadUser() throws Exception {
        String sftpUser1 = createTestUser();
        String sftpUser2 = createTestUser();

        Response response = client.target(getUri(String.format("%s/users/%s", sftpServerApiEndpiont, sftpUser1)))
                .request()
                .get();
        assertEquals(200, response.getStatus());

        response = client.target(getUri(String.format("%s/users/%s", sftpServerApiEndpiont, sftpUser2)))
                .request()
                .get();
        assertEquals(200, response.getStatus());

        // bypass the cache and logic and ensure that a cached read happens if it is deleted from underneath the cache
        rootTokenVault.logical().delete(String.format("secret/sftp/%s/%s", sftpServerId, sftpUser2));
        response = client.target(getUri(String.format("%s/users/%s?cached=true", sftpServerApiEndpiont, sftpUser2)))
                .request()
                .get();
        assertEquals(200, response.getStatus());
        response = client.target(getUri(String.format("%s/users/%s?cached=false", sftpServerApiEndpiont, sftpUser2)))
                .request()
                .get();
        assertEquals(404, response.getStatus());

        // a cache miss with the user not existing should still return a 404
        response = client.target(getUri(String.format("%s/users/%s?cached=true", sftpServerApiEndpiont, UUID.randomUUID())))
                .request()
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteUser() throws Exception {
        String sftpUser1 = createTestUser();
        Response response = client.target(getUri(String.format("%s/users/%s", sftpServerApiEndpiont, sftpUser1)))
                .request()
                .get();
        assertEquals(200, response.getStatus());
        response = client.target(getUri(String.format("%s/users/%s", sftpServerApiEndpiont, sftpUser1)))
                .request()
                .delete();
        assertEquals(204, response.getStatus());

        response = client.target(getUri(String.format("%s/users/%s?cached=false", sftpServerApiEndpiont, sftpUser1)))
                .request()
                .get();
        assertEquals(404, response.getStatus());

        response = client.target(getUri(String.format("%s/users/%s?cached=true", sftpServerApiEndpiont, sftpUser1)))
                .request()
                .get();
        assertEquals(404, response.getStatus());
    }
}