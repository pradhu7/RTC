package apixio.infraconfig.client;

import apixio.infraconfig.core.StringContentStreamProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Charsets;
import io.ianferguson.vault.Vault;
import io.ianferguson.vault.VaultConfig;
import io.ianferguson.vault.VaultException;
import io.ianferguson.vault.json.JsonObject;
import io.ianferguson.vault.response.AuthResponse;
import io.ianferguson.vault.response.HealthResponse;
import io.ianferguson.vault.response.LogicalResponse;
import io.ianferguson.vault.response.LookupResponse;
import io.ianferguson.vault.response.VaultResponse;
import org.joda.time.DateTime;
import org.json.JSONObject;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class VaultClient {
    private URI vaultAddress;
    private String vaultRole;
    private VaultConfig vaultConfig;
    private AWSClient awsClient;
    private Long expireTime = 0L;
    private Boolean renewable = false;
    protected Vault vaultClient;
    private Logger log = Logger.getLogger(this.getClass().getName());
    private URI AWS_ENDPOINT;

    // sts/IAM only exist in us-east-1 so do not allow this to be changed
    private static final Region AWS_REGION = Region.US_EAST_1;
    private static final URI DEFAULT_AWS_ENDPOINT = URI.create(String.format("https://sts.%s.amazonaws.com", AWS_REGION.id()));
    private final String STS_REQUEST_BODY = "Action=GetCallerIdentity&Version=2011-06-15";
    private final String VAULT_AUTH_MOUNT = "aws";

    public VaultClient(URI vaultAddress, String vaultRole, String awsRole) {
        this(vaultAddress, vaultRole, awsRole, 1);
    }

    public VaultClient(URI vaultAddress, String vaultRole, String awsRole, Integer engineVersion) {
        this(vaultAddress, vaultRole, awsRole, engineVersion, null, null);

    }

    public VaultClient(URI vaultAddress, String vaultRole, String awsRole, Integer engineVersion, String awsAccessKey, String awsAccessSecretKey) {
        this(vaultAddress, vaultRole, awsRole, engineVersion, null, null, DEFAULT_AWS_ENDPOINT);
    }

    public VaultClient(URI vaultAddress, String vaultRole, String awsRole, Integer engineVersion, String awsAccessKey, String awsAccessSecretKey, URI awsStsEndpiont) {
        this.vaultAddress = vaultAddress;
        this.vaultRole = vaultRole;
        this.vaultConfig = new VaultConfig();
        this.vaultConfig.address(vaultAddress.toString()).engineVersion(engineVersion);
        this.awsClient = new AWSClient(AWS_REGION.id(), awsRole, awsAccessKey, awsAccessSecretKey);
        this.AWS_ENDPOINT = awsStsEndpiont;
    }

    /**
     * @return returns request headers for stsGetCallerIdentity as a base64 encoded string
     */
    String getBase64EncodedRequestHeaders() {

        SdkHttpFullRequest unsignedSdkHttpFullRequest = SdkHttpFullRequest.builder()
                .appendHeader("X-Vault-AWS-IAM-Server-ID", vaultAddress.getHost())
                .appendHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .method(SdkHttpMethod.POST)
                .uri(AWS_ENDPOINT)
                .contentStreamProvider(new StringContentStreamProvider(STS_REQUEST_BODY))
                .build();

        Aws4Signer aws4Signer = Aws4Signer.create();
        Aws4SignerParams aws4SignerParams = Aws4SignerParams.builder()
                .awsCredentials(awsClient.getSessionCredentials())
                .signingRegion(AWS_REGION)
                .signingName("sts")
                .build();

        SdkHttpFullRequest signedSdkHttpFullRequest = aws4Signer
                .sign(unsignedSdkHttpFullRequest, aws4SignerParams);

        JSONObject json = new JSONObject(signedSdkHttpFullRequest.headers());

        String signedHeaderString = json.toString();
        return Base64.getEncoder().encodeToString(signedHeaderString.getBytes(Charsets.UTF_8));
    }

    /**
     * renews a vault token if it is able to. true if success. false on failure
     */
    private synchronized Boolean renewToken() {
        if (Objects.isNull(vaultClient)) {
            return false;
        }
        try {
            Long currentTimeWithOffset = (DateTime.now().getMillis() + 60000);
            if (expireTime > currentTimeWithOffset) {
                return true;
            } else if (expireTime <= currentTimeWithOffset && renewable) {
                validateVaultRequest(vaultClient.auth().renewSelf(), "/renew");
                setExpiryTime();
                return true;
            }
        } catch (VaultException e) {
            return false;
        }
        return false;
    }

    /**
     * will set private variables tracking the expire time and renewability status
     *
     * @throws VaultException if there an issue looking up token information or other unexpected issue
     */
    private synchronized void setExpiryTime() throws VaultException {
        LookupResponse lookupResponse = vaultClient.auth().lookupSelf();
        validateVaultRequest(lookupResponse, "/self");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode responseMap = objectMapper.readTree(lookupResponse.getRestResponse().getBody());
            String expireTimeString = responseMap.get("data").get("expire_time").asText();
            if (Objects.isNull(expireTimeString) || expireTimeString.equals("null")) {
                /*
                null means it will never expire. so instead of having it try to renew every time set the time way into
                the future so a renewal is not done and un-necissary calls to vault are not done. this should not happen
                with AWS but it is a precaution to avoid any unexpected issues
                 */
                this.expireTime = DateTime.now().plusYears(100).getMillis();
                this.renewable = false;
            }
            DateTime expireTime = DateTime.parse(expireTimeString);
            this.expireTime = expireTime.getMillis();
            this.renewable = lookupResponse.isRenewable();
        } catch (IOException e) {
            e.printStackTrace();
            throw new VaultException(e.getMessage());
        }
    }

    /**
     * Authenticates a user using IAM authentication with an assumed AWS role
     *
     * @throws VaultException if the credentials do not have access to the role
     */
    public synchronized void authenticate() throws VaultException {
        // only authenticate if the token needs to be renewed, is expired, or not initialized
        if (!renewToken()) {
            Vault iamVaultClient = new Vault(vaultConfig.build());
            AuthResponse authResponse = iamVaultClient.auth().loginByAwsIam(
                    vaultRole,
                    Base64.getEncoder().encodeToString(AWS_ENDPOINT.toString().getBytes(Charsets.UTF_8)),
                    Base64.getEncoder().encodeToString(STS_REQUEST_BODY.getBytes(Charsets.UTF_8)),
                    getBase64EncodedRequestHeaders(),
                    VAULT_AUTH_MOUNT
            );
            validateVaultRequest(authResponse, VAULT_AUTH_MOUNT);
            String token = authResponse.getAuthClientToken();
            vaultClient = new Vault(vaultConfig.token(token).build());
            setExpiryTime();
        }
    }

    /**
     * @param response a LogicalResponse object that is returned from the vault client
     * @throws VaultException thrown if vault does not respond with a successful status code
     */
    private void validateVaultRequest(VaultResponse response, String path) throws VaultException {
        Integer responseCode = response.getRestResponse().getStatus();
        String responseBody = new String(response.getRestResponse().getBody());
        if (!Response.Status.Family.familyOf(responseCode).equals(Response.Status.Family.SUCCESSFUL)) {
            throw new VaultException(String.format("Unable to read/write data to vault %s. Status code: %d, Response: %s", path, responseCode, responseBody), responseCode);
        }
    }

    /**
     * @param path the path in vault to write to. include the mount in this
     * @param data A map representing the data to write into vault
     * @throws VaultException
     */
    public void writeObject(String path, Map<String, Object> data) throws VaultException {
        LogicalResponse response = vaultClient.logical().write(path, data);
        validateVaultRequest(response, path);
    }

    /**
     * @param path the path in vault to read. include the mount in this
     * @return the JSON representation of the object
     * @throws VaultException if there are permission or other issues reading from vault
     */
    public JsonObject getObject(String path) throws VaultException {
        LogicalResponse response = vaultClient.logical().read(path);
        validateVaultRequest(response, path);
        return response.getDataObject();
    }

    /**
     * @param path the path in vault to read. include the mount in this
     * @return the JSON representation of the object
     * @throws VaultException if there are permission or other issues reading from vault
     */
    public JsonObject listObjects(String path) throws VaultException {
        LogicalResponse response = vaultClient.logical().list(path);
        validateVaultRequest(response, path);
        return response.getDataObject();
    }

    /**
     * @param path the path in vault to delete. include the mount in this
     * @return the JSON representation of the object
     * @throws VaultException if there are permission or other issues reading from vault
     */
    public JsonObject deleteObject(String path) throws VaultException {
        LogicalResponse response = vaultClient.logical().delete(path);
        validateVaultRequest(response, path);
        return response.getDataObject();
    }

    /**
     * @param path the path in vault to read. include the mount in this
     * @return true if a 2xx is received from vault. false if a 404 is returned
     * @throws VaultException if a response other than 2xx or 404 is returned
     */
    public Boolean objectExists(String path) throws VaultException {
        LogicalResponse response = vaultClient.logical().read(path);
        if (response.getRestResponse().getStatus() == 404) {
            return false;
        }
        validateVaultRequest(response, path);
        return true;
    }

    public void setAwsEndpoint(URI endpoint) {
        this.AWS_ENDPOINT = endpoint;
    }

    public Boolean healthcheck() throws Exception {
        authenticate();
        HealthResponse healthResponse = vaultClient.debug().health();
        Integer responseCode = healthResponse.getRestResponse().getStatus();
        String responseBody = new String(healthResponse.getRestResponse().getBody());
        // health status will return 4xx for some healthy scenarios
        // https://www.vaultproject.io/api/system/health
        if (responseCode >= 500) {
            log.warning(String.format("Unable to read/write data to vault %s. Status code: %d, Response: %s", "/debug/health", responseCode, responseBody));
            return false;
        } else {
            return true;
        }
    }
}
