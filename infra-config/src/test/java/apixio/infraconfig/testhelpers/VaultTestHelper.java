package apixio.infraconfig.testhelpers;

import apixio.infraconfig.InfraConfigConfiguration;
import apixio.infraconfig.InfraConfigTestHelper;
import apixio.infraconfig.client.VaultClient;
import apixio.infraconfig.config.AwsCredentialConfig;
import io.dropwizard.testing.ResourceHelpers;
import io.ianferguson.vault.Vault;
import io.ianferguson.vault.VaultConfig;
import io.ianferguson.vault.VaultException;
import io.ianferguson.vault.response.LogicalResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;
import software.amazon.awssdk.services.iam.model.Role;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VaultTestHelper extends LocalstackTestHelper {
    public VaultClient vaultClient;
    public static URI vaulturl;
    public static VaultContainer vaultContainer;
    public static Vault rootTokenVault;
    public static final DockerImageName vaultImage = DockerImageName.parse("vault:1.9.4");

    @BeforeClass
    public static void beforeClass() throws Exception {
        LocalstackTestHelper.beforeClass();
        String vaultRootToken = "testRootToken";
        vaultContainer = new VaultContainer<>(vaultImage)
                .withNetwork(network)
                .withVaultToken(vaultRootToken);
        vaultContainer.start();

        // a little hacky compared to the rest calls but easier to handle for enabling the v1 secrets engine
        Container.ExecResult result = vaultContainer.execInContainer("sh", "-c", String.format("vault login %s && vault secrets disable secret && vault secrets enable -version=1 -path=secret kv", vaultRootToken));
        if (result.getExitCode() != 0) {
            throw new VaultException(String.format("Failed to reconfigure vault %s", result.toString()));
        }

        // create root token vault client
        vaulturl = URI.create(String.format("http://%s:%d", vaultContainer.getHost(), vaultContainer.getFirstMappedPort()));
        VaultConfig vaultConfig = new VaultConfig().address(vaulturl.toString()).token(vaultRootToken).engineVersion(1).build();
        rootTokenVault = new Vault(vaultConfig);

        // enable aws auth backend
        Map<String, Object> enableMountPayload = new HashMap<>();
        enableMountPayload.put("type", "aws");
        LogicalResponse mountResponse = rootTokenVault.logical().write("sys/auth/aws", enableMountPayload);
        if (mountResponse.getRestResponse().getStatus() > 299) {
            throw new VaultException(String.format("Unable to configure aws mount for test %s", new String(mountResponse.getRestResponse().getBody())));
        }

        // write r/w policy for secrets backend
        Map<String, Object> secretsPolicyReq = new HashMap<>();
        File secretsPolicyFile = new File(ResourceHelpers.resourceFilePath("fixtures/sftp-vault-policy.hcl"));
        String secretsPolicy = new String(Files.readAllBytes(secretsPolicyFile.toPath()));
        secretsPolicyReq.put("policy", secretsPolicy);
        LogicalResponse response = rootTokenVault.logical().write("sys/policy/secretsrw", secretsPolicyReq);
        if (response.getRestResponse().getStatus() > 299) {
            throw new VaultException(String.format("Unable to configure policy for test %s", new String(response.getRestResponse().getBody())));
        }

        // configure aws auth backend
        Map<String, Object> configureAwsAuth = new HashMap<>();
        configureAwsAuth.put("access_key", localstack.getAccessKey());
        configureAwsAuth.put("secret_key", localstack.getSecretKey());
        configureAwsAuth.put("endpoint", localstackEndpointForVault);
        configureAwsAuth.put("iam_endpoint", localstackEndpointForVault);
        configureAwsAuth.put("sts_endpoint", localstackEndpointForVault);
        configureAwsAuth.put("sts_region", localstack.getRegion());
        LogicalResponse awsResponse = rootTokenVault.logical().write("auth/aws/config/client", configureAwsAuth);
        if (awsResponse.getRestResponse().getStatus() > 299) {
            throw new VaultException(String.format("Unable to configure aws auth for test %s", new String(awsResponse.getRestResponse().getBody())));
        }
    }

    @AfterClass
    public static void afterClass() {
        vaultContainer.stop();
        LocalstackTestHelper.afterClass();
    }

    public static String setupVaultAwsAuth(Role role) throws VaultException {
        String vaultRole = UUID.randomUUID().toString();
        Map<String, Object> vaultRoleConfig = new HashMap<>();
        vaultRoleConfig.put("auth_type", "iam");
        vaultRoleConfig.put("bound_iam_principal_arn", role.arn());
        vaultRoleConfig.put("policies", Arrays.asList("default", "secretsrw"));
        vaultRoleConfig.put("max_ttl", "500h");
        vaultRoleConfig.put("resolve_aws_unique_ids", "false"); // needed when testing with localstack
        LogicalResponse awsResponse = rootTokenVault.logical().write(String.format("auth/aws/role/%s", vaultRole), vaultRoleConfig);
        if (awsResponse.getRestResponse().getStatus() > 299) {
            throw new VaultException(String.format("Unable to configure aws auth for test %s", awsResponse.getRestResponse().toString()));
        }
        return vaultRole;
    }

    public void getVaultClient() throws Exception {
        setupAwsUser();
        setupAwsRole();

        String vaultRole = setupVaultAwsAuth(testRole);

        InfraConfigConfiguration config = InfraConfigTestHelper.getDropWizardConfig("config-unit.yml");
        AwsCredentialConfig awsCredentialConfig = new AwsCredentialConfig();
        awsCredentialConfig.setAwsRole(testRole.arn());
        awsCredentialConfig.setAccessKey(testUserCreds.accessKeyId());
        awsCredentialConfig.setSecretKey(testUserCreds.secretAccessKey());
        config.setAwsCredentialConfig(awsCredentialConfig);

        this.vaultClient = new VaultClient(
                vaulturl,
                vaultRole,
                testRole.arn(),
                1,
                config.getAwsCredentialConfig().getAccessKey(),
                config.getAwsCredentialConfig().getSecretKey(),
                URI.create(localstackEndpointForVault)
        );
        this.vaultClient.authenticate();
    }
}
