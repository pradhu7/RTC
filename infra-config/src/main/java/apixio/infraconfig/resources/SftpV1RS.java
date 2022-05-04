package apixio.infraconfig.resources;

import apixio.infraconfig.InfraConfigConfiguration;
import apixio.infraconfig.InfraConfigSysServices;
import apixio.infraconfig.api.SftpHomeDirectoryDetails;
import apixio.infraconfig.api.SftpUserModel;
import apixio.infraconfig.api.SftpUserQuery;
import apixio.infraconfig.client.AWSClient;
import apixio.infraconfig.client.VaultClient;
import apixio.infraconfig.config.AwsCredentialConfig;
import apixio.infraconfig.config.SftpServerConfig;
import apixio.infraconfig.core.IPFilterHelper;
import apixio.infraconfig.core.InvalidSftpUserException;
import apixio.infraconfig.core.PasswordHashGeneratorHelper;
import com.apixio.SysServices;
import com.apixio.restbase.web.BaseRS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.dropwizard.jersey.PATCH;
import io.dropwizard.jersey.params.BooleanParam;
import io.ianferguson.vault.VaultException;
import io.ianferguson.vault.json.JsonObject;
import io.ianferguson.vault.json.JsonValue;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ehcache.Cache;
import org.ehcache.spi.loaderwriter.CacheLoadingException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

@Path("/api/v1/sftp")
public class SftpV1RS extends BaseRS {
    private SysServices sysServices;
    private VaultClient vaultClient;
    private Map<String, SftpServerConfig> sftpServers;
    private AwsCredentialConfig awsConfig;
    private String LOGGER_ID = "infra-config-service";
    private final Set<String> homedirSkelFolders = new HashSet<>(Arrays.asList("to_apixio", "from_apixio"));
    private Logger LOG;
    private Cache<SftpUserQuery, SftpUserModel> sftpUserCache;

    public SftpV1RS(InfraConfigConfiguration configuration, InfraConfigSysServices sysServices) {
        super(configuration, sysServices, SftpV1RS.class.getName());
        LOG = Logger.getLogger(this.getClass().getName());
        this.sysServices = sysServices;
        this.sftpServers = configuration.getSftpServersConfig();
        this.awsConfig = configuration.getAwsCredentialConfig();
        this.vaultClient = sysServices.getVaultClient();
        this.sftpUserCache = sysServices.getSftpUserCache();
    }

    private String getAWSRole(SftpServerConfig sftpServer) {
        String awsRole;
        if (Objects.isNull(sftpServer.getAwsRole())) {
            awsRole = awsConfig.getAwsRole();
        } else {
            awsRole = sftpServer.getAwsRole();
        }
        return awsRole;
    }

    private AWSClient getAWSClient(SftpServerConfig sftpServer) {
        String awsRole = getAWSRole(sftpServer);
        AWSClient awsClient;
        if (
                Objects.isNull(awsConfig.getAccessKey())
                        && Objects.isNull(awsConfig.getSecretKey())
        ) {
            awsClient = new AWSClient(sftpServer.getRegion(), awsRole);
        } else {
            awsClient = new AWSClient(
                    sftpServer.getRegion(),
                    awsRole,
                    awsConfig.getAccessKey(),
                    awsConfig.getSecretKey()
            );
        }
        if (Objects.nonNull(awsConfig.getEc2Retries())) {
            awsClient.ec2Retries = awsConfig.getEc2Retries();
        }
        return awsClient;
    }

    private void finalizeSftpUser(SftpUserModel sftpUserInfo, String sftpUser, String sftpServer) throws InvalidSftpUserException {
        sftpUserInfo.setUsername(sftpUser);
        sftpUserInfo.setSftpServerId(sftpServer);
        // generate the home directory details
        // this also has the effect of not allowing this value to be overridden
        List<SftpHomeDirectoryDetails> homeDirectoryDetails = new ArrayList<>();
        homeDirectoryDetails.add(
                SftpHomeDirectoryDetails.SftpHomeDirectoryDetailsBuilder.builder()
                        .withEntry("/")
                        .withTarget(String.format("/%s/%s", sftpServers.get(sftpServer).getS3Bucket(), sftpUser))
                        .build()
        );
        sftpUserInfo.setHomeDirectoryDetails(homeDirectoryDetails);
        sftpUserInfo.setAwsRole(sftpServers.get(sftpServer).getUserAwsRole());
        // validate the group information
        sftpUserInfo.validateGroup(sysServices);
    }

    private void doWrite(SftpUserModel sftpUserInfo, ObjectMapper oMapper, String sftpUser, String sftpServer, String vaultPath) throws VaultException {
        AWSClient awsClient = getAWSClient(sftpServers.get(sftpServer));
        sftpUserCache.put(new SftpUserQuery(sftpServer, sftpUser), sftpUserInfo);
        for (String ipCidr : sftpUserInfo.acceptedIpNetwork) {
            if (IPFilterHelper.ipInList(sftpServers.get(sftpServer).getGlobalCidrWhitelist(), ipCidr)) {
                LOG.info(String.format("Skipping update for cidr %s as it is in the global whitelist", ipCidr));
                continue;
            }
            if (sftpUserInfo.getActive()) {
                awsClient.addIpRangeToGroup(ipCidr, sftpServers.get(sftpServer).getSecurityGroupId(), sftpUser);
            } else {
                awsClient.removeRuleFromGroup(ipCidr, sftpServers.get(sftpServer).getSecurityGroupId(), sftpUser);
            }
        }
        // remove cidrs that have been marked for removal
        for (String ipCidr : sftpUserInfo.removedCidrs) {
            awsClient.removeRuleFromGroup(ipCidr, sftpServers.get(sftpServer).getSecurityGroupId(), sftpUser);
        }
    }

    private Boolean writeS3Marker(String sftpUser, String sftpServer) throws IOException {
        AWSClient awsClient = getAWSClient(sftpServers.get(sftpServer));
        Boolean ret = false;
        for (String folder : homedirSkelFolders) {
            String s3Bucket = sftpServers.get(sftpServer).getS3Bucket();
            String markerKey = String.format("%s/%s/.initmarker", sftpUser, folder);
            if (!awsClient.s3PathExists(s3Bucket, markerKey)) {
                if (!awsClient.writeS3Object(s3Bucket, markerKey, new byte[0])) {
                    throw new IOException(String.format("Failed to write marker key %s to s3 bucket %s", markerKey, s3Bucket));
                } else {
                    ret = true;
                }
            }
        }
        return ret;
    }

    @GET
    @Path("/{sftpServer}/users/{sftpUser}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSftpUserConfig(@Context HttpServletRequest request,
                                      @PathParam("sftpServer") final String sftpServer,
                                      @PathParam("sftpUser") final String sftpUser,
                                      @QueryParam("cached") @DefaultValue("false") final BooleanParam cached) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}/{sftpUser}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        logger.addParameter("sftpUser", sftpUser);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    SftpUserQuery sftpUserQuery = new SftpUserQuery(sftpServer, sftpUser);
                    try {
                        if (cached.get()) {
                            try {
                                return ok(sftpUserCache.get(sftpUserQuery));
                            } catch (CacheLoadingException e) {
                                if (ExceptionUtils.hasCause(e.getCause(), VaultException.class)) {
                                    VaultException vaultException = (VaultException) e.getCause();
                                    if (vaultException.getHttpStatusCode() == 404) {
                                        return notFound();
                                    }
                                }
                                throw e;
                            }
                        }
                        if (!vaultClient.objectExists(sftpUserQuery.getVaultPath())) {
                            return notFound();
                        }
                        JsonObject response = vaultClient.getObject(sftpUserQuery.getVaultPath());
                        ObjectMapper objectMapper = new ObjectMapper();
                        SftpUserModel objResponse = objectMapper.readValue(response.toString(), SftpUserModel.class);
                        return ok(objResponse);
                    } catch (VaultException e) {
                        return badRequest(e.getMessage());
                    }
                } else {
                    return badRequest(String.format("No sftp server instance %s", sftpServer));
                }
            }
        });
    }

    @PUT
    @Path("/{sftpServer}/users/{sftpUser}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setSftpUserConfig(@Context HttpServletRequest request,
                                      @PathParam("sftpServer") final String sftpServer,
                                      @PathParam("sftpUser") final String sftpUser,
                                      @QueryParam("resetPassword") @DefaultValue("false") final BooleanParam resetPassword,
                                      @QueryParam("initializeS3") @DefaultValue("true") final BooleanParam initializeS3,
                                      final SftpUserModel sftpUserInfoReq) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}/{sftpUser}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        logger.addParameter("sftpUser", sftpUser);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    String vaultPath = new SftpUserQuery(sftpServer, sftpUser).getVaultPath();
                    ObjectMapper oMapper = new ObjectMapper();
                    SftpUserModel sftpUserInfo;
                    try {
                        if (vaultClient.objectExists(vaultPath)) {
                            SftpUserModel currentSftpUser = oMapper.readValue(vaultClient.getObject(vaultPath).toString(), SftpUserModel.class);
                            ObjectReader reader = oMapper.readerForUpdating(oMapper.convertValue(currentSftpUser, Map.class));
                            // serialize as string so it can be read by jackson for overriding
                            Map<String, Object> mergedUser = reader.readValue(oMapper.writeValueAsString(sftpUserInfoReq), Map.class);
                            sftpUserInfo = oMapper.convertValue(mergedUser, SftpUserModel.class);
                            // if the password should be reset take the raw values for password and keys and set them back into the object
                            // this is needed as these values are not serialized in the above conversions for merging
                            if (resetPassword.get()) {
                                // if there is a raw password then that means one was generated already with a hash and the current password value is the hash of passwordRaw
                                if (Objects.nonNull(sftpUserInfoReq.passwordRaw)) {
                                    sftpUserInfo.passwordRaw = sftpUserInfoReq.passwordRaw;
                                }
                                // if there is a public key in the request honor the value
                                if (Objects.nonNull(sftpUserInfoReq.publicKey)) {
                                    sftpUserInfo.publicKey = sftpUserInfoReq.publicKey;
                                }
                                // if a new ssh key was generated/set then include it in the request
                                // the set method will only set the raw field as to not expose it accidentally so this must be done to ensure it propagates
                                if (Objects.nonNull(sftpUserInfoReq.privateKeyRaw)) {
                                    sftpUserInfo.privateKeyRaw = sftpUserInfoReq.privateKeyRaw;
                                }
                            } else {
                                sftpUserInfo.setPassword(currentSftpUser.password);
                                sftpUserInfo.setPublicKey(currentSftpUser.publicKey);
                            }
                        } else {
                            sftpUserInfo = sftpUserInfoReq;
                        }
                        if (!sftpUserInfoReq.removedCidrs.isEmpty()) {
                            sftpUserInfo.removeAcceptedIpNetwork(sftpUserInfoReq.removedCidrs);
                        }
                        finalizeSftpUser(sftpUserInfo, sftpUser, sftpServer);

                        doWrite(sftpUserInfo, oMapper, sftpUser, sftpServer, vaultPath);
                        if (initializeS3.get()) {
                            writeS3Marker(sftpUser, sftpServer);
                            sftpUserInfo.setInitialized(true);
                        }
                        sftpUserInfo.password = sftpUserInfo.passwordRaw;
                        sftpUserInfo.privateKey = sftpUserInfo.privateKeyRaw;
                        return Response.created(URI.create(request.getRequestURI())).entity(sftpUserInfo).build();
                    } catch (VaultException e) {
                        return badRequest(e.getMessage());
                    }
                } else {
                    return badRequest(String.format("No sftp server instance %s", sftpServer));
                }
            }
        });
    }

    @POST
    @Path("/{sftpServer}/users/{sftpUser}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSftpUserConfig(@Context HttpServletRequest request,
                                         @PathParam("sftpServer") final String sftpServer,
                                         @PathParam("sftpUser") final String sftpUser,
                                         @QueryParam("initializeS3") @DefaultValue("true") final BooleanParam initializeS3,
                                         final SftpUserModel sftpUserInfoReq) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}/{sftpUser}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        logger.addParameter("sftpUser", sftpUser);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    String vaultPath = new SftpUserQuery(sftpServer, sftpUser).getVaultPath();
                    ObjectMapper oMapper = new ObjectMapper();
                    SftpUserModel sftpUserInfo;
                    try {
                        if (vaultClient.objectExists(vaultPath)) {
                            return Response.status(Status.CONFLICT).entity(String.format("{\"message\": \"User %s already exists on %s\"}", sftpUser, sftpServer)).build();
                        } else {
                            sftpUserInfo = sftpUserInfoReq;
                        }
                        finalizeSftpUser(sftpUserInfo, sftpUser, sftpServer);
                        doWrite(sftpUserInfo, oMapper, sftpUser, sftpServer, vaultPath);
                        if (initializeS3.get()) {
                            writeS3Marker(sftpUser, sftpServer);
                            sftpUserInfo.setInitialized(true);
                        }

                        sftpUserInfo.password = sftpUserInfo.passwordRaw;
                        sftpUserInfo.privateKey = sftpUserInfo.privateKeyRaw;
                        return Response.created(URI.create(request.getRequestURI())).entity(sftpUserInfo).build();
                    } catch (VaultException e) {
                        return badRequest(e.getMessage());
                    }
                } else {
                    return badRequest(String.format("No sftp server instance %s", sftpServer));
                }
            }
        });
    }

    @PATCH
    @Path("/{sftpServer}/users/{sftpUser}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifySftpUserConfig(@Context HttpServletRequest request,
                                         @PathParam("sftpServer") final String sftpServer,
                                         @PathParam("sftpUser") final String sftpUser,
                                         @QueryParam("generatePassword") @DefaultValue("false") final BooleanParam generatePassword,
                                         @QueryParam("initializeS3") @DefaultValue("false") final BooleanParam initializeS3,
                                         final Map<String, Object> sftpUserInfoReq) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}/{sftpUser}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        logger.addParameter("sftpUser", sftpUser);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    String vaultPath = new SftpUserQuery(sftpServer, sftpUser).getVaultPath();
                    ObjectMapper oMapper = new ObjectMapper();
                    SftpUserModel sftpUserInfo;
                    try {
                        if (vaultClient.objectExists(vaultPath)) {
                            Map<String, Object> currentSftpUser = oMapper.readValue(vaultClient.getObject(vaultPath).toString(), Map.class);
                            ObjectReader reader = oMapper.readerForUpdating(currentSftpUser);
                            // serialize as string so it can be read by jackson for overriding
                            Map<String, Object> mergedUser = reader.readValue(oMapper.writeValueAsString(sftpUserInfoReq), Map.class);
                            sftpUserInfo = oMapper.convertValue(mergedUser, SftpUserModel.class);
                            // generate a new password if told to
                            if (generatePassword.get()) {
                                sftpUserInfo.setPassword(null);
                            }
                        } else {
                            return Response.status(Status.NOT_FOUND).entity(String.format("Could not find User %s on %s", sftpUser, sftpServer)).build();
                        }
                        finalizeSftpUser(sftpUserInfo, sftpUser, sftpServer);

                        doWrite(sftpUserInfo, oMapper, sftpUser, sftpServer, vaultPath);
                        if (initializeS3.get()) {
                            writeS3Marker(sftpUser, sftpServer);
                            sftpUserInfo.setInitialized(true);
                        }
                        sftpUserInfo.password = sftpUserInfo.passwordRaw;
                        sftpUserInfo.privateKey = sftpUserInfo.privateKeyRaw;
                        return Response.created(URI.create(request.getRequestURI())).entity(sftpUserInfo).build();
                    } catch (VaultException e) {
                        return badRequest(e.getMessage());
                    }
                } else {
                    return badRequest(String.format("No sftp server instance %s", sftpServer));
                }
            }
        });
    }


    @DELETE
    @Path("/{sftpServer}/users/{sftpUser}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSftpUserConfig(@Context HttpServletRequest request,
                                         @PathParam("sftpServer") final String sftpServer,
                                         @PathParam("sftpUser") final String sftpUser) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}/{sftpUser}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        logger.addParameter("sftpUser", sftpUser);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    SftpUserQuery sftpUserQuery = new SftpUserQuery(sftpServer, sftpUser);
                    String vaultPath = sftpUserQuery.getVaultPath();
                    try {
                        if (!vaultClient.objectExists(vaultPath)) {
                            return notFound();
                        }
                        ObjectMapper oMapper = new ObjectMapper();
                        SftpUserModel sftpUserInfo = oMapper.readValue(vaultClient.getObject(vaultPath).toString(), SftpUserModel.class);
                        AWSClient awsClient = getAWSClient(sftpServers.get(sftpServer));
                        for (String ipCidr : sftpUserInfo.acceptedIpNetwork) {
                            awsClient.removeRuleFromGroup(ipCidr, sftpServers.get(sftpServer).getSecurityGroupId(), sftpUser);
                        }
                        sftpUserCache.remove(sftpUserQuery);
                        return Response.noContent().contentLocation(URI.create(request.getRequestURI())).build();
                    } catch (VaultException e) {
                        return badRequest(e.getMessage());
                    }
                } else {
                    return badRequest(String.format("No sftp server instance %s", sftpServer));
                }
            }
        });
    }

    @POST
    @Path("/{sftpServer}/users/{sftpUser}/validate/password")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateSftpUserPassword(@Context HttpServletRequest request,
                                             @PathParam("sftpServer") final String sftpServer,
                                             @PathParam("sftpUser") final String sftpUser,
                                             final String reqPassword) {
        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}/{sftpUser}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        logger.addParameter("sftpUser", sftpUser);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                Map<String, Object> objResponse = new HashMap<>();
                objResponse.put("result", false);
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    String vaultPath = new SftpUserQuery(sftpServer, sftpUser).getVaultPath();
                    try {
                        if (!vaultClient.objectExists(vaultPath)) {
                            return notFound();
                        }
                        JsonObject response = vaultClient.getObject(vaultPath);
                        ObjectMapper objectMapper = new ObjectMapper();
                        SftpUserModel sftpUserInfo = objectMapper.readValue(response.toString(), SftpUserModel.class);
                        Boolean result = PasswordHashGeneratorHelper.verify(reqPassword, sftpUserInfo.password);
                        objResponse.put("result", result);
                        objResponse.put("message", "");
                        return ok(objResponse);
                    } catch (VaultException e) {
                        objResponse.put("message", e.getMessage());
                        return Response.status(Status.BAD_REQUEST).entity(objResponse).build();
                    }
                } else {
                    objResponse.put("message", String.format("No sftp server instance %s", sftpServer));
                    return Response.status(Status.BAD_REQUEST).entity(objResponse).build();
                }
            }
        });
    }

    @POST
    @Path("/{sftpServer}/users/{sftpUser}/validate/sshkey")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateSftpUserSshKey(@Context HttpServletRequest request,
                                           @PathParam("sftpServer") final String sftpServer,
                                           @PathParam("sftpUser") final String sftpUser,
                                           final String reqPrivateKey) {
        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}/{sftpUser}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        logger.addParameter("sftpUser", sftpUser);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                Map<String, Object> objResponse = new HashMap<>();
                objResponse.put("result", false);
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    String vaultPath = new SftpUserQuery(sftpServer, sftpUser).getVaultPath();
                    try {
                        if (!vaultClient.objectExists(vaultPath)) {
                            return notFound();
                        }
                        JsonObject response = vaultClient.getObject(vaultPath);
                        ObjectMapper objectMapper = new ObjectMapper();

                        SftpUserModel sftpUserInfo = objectMapper.readValue(response.toString(), SftpUserModel.class);
                        sftpUserInfo.setPrivateKey(reqPrivateKey);
                        objResponse.put("result", true);
                        objResponse.put("message", "");
                        return ok(objResponse);
                    } catch (VaultException | InvalidSftpUserException e) {
                        objResponse.put("message", e.getMessage());
                        return Response.status(Status.BAD_REQUEST).entity(objResponse).build();
                    }
                } else {
                    objResponse.put("message", String.format("No sftp server instance %s", sftpServer));
                    return Response.status(Status.BAD_REQUEST).entity(objResponse).build();
                }
            }
        });
    }


    @GET
    @Path("/{sftpServer}/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSftpServerUsers(@Context HttpServletRequest request,
                                       @PathParam("sftpServer") final String sftpServer
    ) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp/{sftpServer}", LOGGER_ID);
        logger.addParameter("sftpServer", sftpServer);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                List<SftpUserModel> sftpUsers = new ArrayList<>();
                if (sftpServers.containsKey(sftpServer)) {
                    vaultClient.authenticate();
                    String vaultListPath = String.format("secret/sftp/%s", sftpServer);
                    try {
                        JsonObject response = vaultClient.listObjects(vaultListPath);
                        for (JsonValue sftpUserVal : response.get("keys").asArray()) {
                            String sftpUser = sftpUserVal.asString();
                            sftpUsers.add(sftpUserCache.get(new SftpUserQuery(sftpServer, sftpUser)));
                        }
                        return ok(sftpUsers);
                    } catch (VaultException e) {
                        e.printStackTrace();
                        return badRequest(e.getMessage());
                    }
                } else {
                    return badRequest(String.format("No sftp server instance %s", sftpServer));
                }
            }
        });
    }

    @GET
    @Produces("application/json")
    @Path("/{sftpServer}")
    public Response getSftpServer(
            @Context HttpServletRequest request,
            @PathParam("sftpServer") final String sftpServer
    ) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp", LOGGER_ID);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (sftpServers.containsKey(sftpServer)) {
                    return ok(sftpServers.get(sftpServer));
                } else {
                    return notFound();
                }
            }
        });
    }

    @GET
    @Produces("application/json")
    public Response getSftpServers(
            @Context HttpServletRequest request
    ) {

        final ApiLogger logger = super.createApiLogger(request, "/api/v1/sftp", LOGGER_ID);
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                return ok(sftpServers);
            }
        });
    }
}
