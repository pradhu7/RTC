package apixio.infraconfig.testhelpers;

import apixio.infraconfig.client.AWSClient;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AccessKey;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StsClient.class, Ec2Client.class, S3Client.class})
@PowerMockIgnore({"javax.crypto.*", "org.hamcrest.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.security.*", "org.w3c.*"})
public class LocalstackTestHelper {
    public static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.14.2");
    public static LocalStackContainer localstack;
    public static String localstackEndpointForVault = "https://localhost.localstack.cloud:4566";
    public static Network network;
    public static IamClient iamClient;
    public static StsClient stsClient;
    public static Ec2Client ec2Client;
    public static S3Client s3Client;
    public static String defaultRegion;
    public static String accessKey;
    public static String secretKey;
    public static List<String> TEST_SG_ID;
    public static AWSClient awsClient;
    public static String TEST_BUCKET;
    public static User testUserData;
    public static AccessKey testUserCreds;
    public static Role testRole;

    @BeforeClass
    public static void beforeClass() throws Exception {
        network = Network.newNetwork();
        localstack = new LocalStackContainer(localstackImage)
                .withServices(Service.S3, Service.EC2, Service.STS, Service.IAM)
                .withNetwork(network)
                .withNetworkAliases("localstack", "localhost.localstack.cloud");
        localstack.start();
    }

    @AfterClass
    public static void afterClass() {
        localstack.stop();
        network.close();
    }

    // have to re-init the mocks between each test run otherwise the default mocks won't work as intended
    @Before
    public void beforeEach() {
        initAwsMocks();
    }

    public static void initAwsMocks() {
        // currently does not work for all cases but keeping code in here
        // see https://github.com/localstack/localstack-java-utils/tree/master for annotations
        StsClientBuilder mockSts = StsClient.builder().endpointOverride(URI.create(localstack.getEndpointConfiguration(Service.STS).getServiceEndpoint()));
        PowerMockito.mockStatic(StsClient.class);
        when(StsClient.builder()).thenReturn(mockSts);
        Ec2ClientBuilder mockec2 = Ec2Client.builder().endpointOverride(URI.create(localstack.getEndpointConfiguration(Service.EC2).getServiceEndpoint()));
        PowerMockito.mockStatic(Ec2Client.class);
        when(Ec2Client.builder()).thenReturn(mockec2);
        S3ClientBuilder mocks3 = S3Client.builder().endpointOverride(URI.create(localstack.getEndpointConfiguration(Service.S3).getServiceEndpoint()));
        PowerMockito.mockStatic(S3Client.class);
        when(S3Client.builder()).thenReturn(mocks3);

        defaultRegion = localstack.getRegion();
        accessKey = localstack.getAccessKey();
        secretKey = localstack.getSecretKey();


        iamClient = IamClient.builder()
                .endpointOverride(URI.create(localstack.getEndpointConfiguration(Service.IAM).getServiceEndpoint()))
                .region(Region.of(defaultRegion))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        stsClient = StsClient.builder()
                .endpointOverride(URI.create(localstack.getEndpointConfiguration(Service.STS).getServiceEndpoint()))
                .region(Region.of(defaultRegion))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        ec2Client = Ec2Client.builder()
                .endpointOverride(URI.create(localstack.getEndpointConfiguration(Service.EC2).getServiceEndpoint()))
                .region(Region.of(defaultRegion))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(localstack.getEndpointConfiguration(Service.S3).getServiceEndpoint()))
                .region(Region.of(defaultRegion))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public static Pair<User, AccessKey> setupAwsUser() {
        String testUserName = UUID.randomUUID().toString();
        User userInfo = iamClient.createUser(CreateUserRequest.builder().userName(testUserName).build()).user();
        AccessKey accessKey = iamClient.createAccessKey(CreateAccessKeyRequest.builder().userName(testUserName).build()).accessKey();
        testUserData = userInfo;
        testUserCreds = accessKey;
        return Pair.of(userInfo, accessKey);
    }

    public static Role setupAwsRole() throws IOException {
        File assumeRoleDocFile = new File(ResourceHelpers.resourceFilePath("fixtures/assume-role-policy.json"));
        String assumeRoleDoc = new String(Files.readAllBytes(assumeRoleDocFile.toPath()));
        assumeRoleDoc = String.format(assumeRoleDoc, testUserData.arn());

        CreateRoleRequest createRoleRequest = CreateRoleRequest.builder()
                .assumeRolePolicyDocument(assumeRoleDoc)
                .roleName(UUID.randomUUID().toString())
                .build();
        Role role = iamClient.createRole(createRoleRequest).role();
        testRole = role;
        return role;
    }

    public static Pair<List<String>, String> setupTestBucketSg() {
        List<String> sgTestIds = new ArrayList<>();
        String testBucketName;
        sgTestIds.add(
                ec2Client.createSecurityGroup(
                        CreateSecurityGroupRequest.builder()
                                .groupName(UUID.randomUUID().toString())
                                .description("test")
                                .build()
                ).groupId()
        );
        sgTestIds.add(
                ec2Client.createSecurityGroup(
                        CreateSecurityGroupRequest.builder()
                                .groupName(UUID.randomUUID().toString())
                                .description("test2")
                                .build()
                ).groupId()
        );

        // create s3 bucket for testing
        testBucketName = UUID.randomUUID().toString();
        s3Client.createBucket(
                CreateBucketRequest.builder()
                        .bucket(testBucketName)
                        .build()
        );
        TEST_SG_ID = sgTestIds;
        TEST_BUCKET = testBucketName;
        return Pair.of(sgTestIds, testBucketName);
    }

    public static void prepAwsAccount() throws Exception {
        initAwsMocks();

        // create SG's for tests

        setupTestBucketSg();
        setupAwsUser();
        setupAwsRole();

        awsClient = new AWSClient(defaultRegion, testRole.arn(), accessKey, secretKey);
    }
}
