package apixio.infraconfig.client;

import apixio.infraconfig.testhelpers.LocalstackTestHelper;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AWSClientTest extends LocalstackTestHelper {

    private final String AWS_REGION = "us-west-2";
    private List<String> TEST_SG_ID;
    private AWSClient awsClient;
    private String TEST_BUCKET;
    private final String TEST_IP_RANGE = "192.168.100.1/32";
    private final String DEFAULT_DESCRIPTION = "unit-test";
    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.14.2");

    private static LocalStackContainer localstack;

    @Before
    public void doBeforeEachTest() {
        Logger.getGlobal().setLevel(Level.INFO);
    }

    @Before
    public void init() throws Exception {
        initAwsMocks();

        // create SG's for tests
        TEST_SG_ID = new ArrayList<>();
        TEST_SG_ID.add(
                ec2Client.createSecurityGroup(
                        CreateSecurityGroupRequest.builder()
                                .groupName(UUID.randomUUID().toString())
                                .description("test")
                                .build()
                ).groupId()
        );
        TEST_SG_ID.add(
                ec2Client.createSecurityGroup(
                        CreateSecurityGroupRequest.builder()
                                .groupName(UUID.randomUUID().toString())
                                .description("test2")
                                .build()
                ).groupId()
        );

        // create s3 bucket for testing
        TEST_BUCKET = UUID.randomUUID().toString();
        s3Client.createBucket(
                CreateBucketRequest.builder()
                        .bucket(TEST_BUCKET)
                        .build()
        );


        GetCallerIdentityResponse getCallerIdentityResponse = stsClient.getCallerIdentity();

        File assumeRoleDocFile = new File(ResourceHelpers.resourceFilePath("fixtures/assume-role-policy.json"));
        Scanner scanner = new Scanner(assumeRoleDocFile);
        String assumeRoleDoc = new String(Files.readAllBytes(assumeRoleDocFile.toPath()));
        assumeRoleDoc = String.format(assumeRoleDoc, getCallerIdentityResponse.arn());

        CreateRoleRequest createRoleRequest = CreateRoleRequest.builder()
                .assumeRolePolicyDocument(assumeRoleDoc)
                .roleName(UUID.randomUUID().toString())
                .build();
        Role role = iamClient.createRole(createRoleRequest).role();

        awsClient = new AWSClient(defaultRegion, role.arn(), accessKey, secretKey);
    }

    @Test
    public void getStsCredentials() {
        assertNotNull(awsClient.getStsCredentials());
    }

    @Test
    public void getSessionCredentials() {
        assertNotNull(awsClient.getSessionCredentials());
    }

    @Test
    public void isRuleInGroup() {
        String testRange = "192.168.50.0/24";
        awsClient.addIpRangeToGroup(TEST_IP_RANGE, TEST_SG_ID, DEFAULT_DESCRIPTION);
        awsClient.addIpRangeToGroup(testRange, TEST_SG_ID, DEFAULT_DESCRIPTION);
        assertTrue(awsClient.isRuleInGroup(TEST_IP_RANGE, TEST_SG_ID));
        assertTrue(awsClient.isRuleInGroup(testRange, TEST_SG_ID));
        // by default a single egress rule is created. so make sure there is more than 1 rule
        assertTrue(awsClient.getNumRules(TEST_SG_ID.get(0)) > 1);
        assertTrue(awsClient.getNumRules(TEST_SG_ID.get(1)) > 1);

        // test that multiple values for SG work for both ranges
        assertTrue(awsClient.isRuleInGroup(TEST_IP_RANGE, TEST_SG_ID));
        assertTrue(awsClient.isRuleInGroup(testRange, TEST_SG_ID));
        assertFalse(awsClient.isRuleInGroup("10.10.10.0/24", TEST_SG_ID));

        awsClient.removeRuleFromGroup(TEST_IP_RANGE, TEST_SG_ID, AWSClient.REMOVE_ALL_TOKEN);
        awsClient.removeRuleFromGroup(testRange, TEST_SG_ID, AWSClient.REMOVE_ALL_TOKEN);
        // ensure rules were removed from the SGs
        assertFalse(awsClient.isRuleInGroup(TEST_IP_RANGE, TEST_SG_ID));
        assertFalse(awsClient.isRuleInGroup(testRange, TEST_SG_ID));
        // by default a single egress rule is created. so make sure there that is the only rule left
        assertTrue(awsClient.getNumRules(TEST_SG_ID.get(0)) == 1);
        assertTrue(awsClient.getNumRules(TEST_SG_ID.get(1)) == 1);
        // make sure the rule is removed from both SG's
        assertFalse(awsClient.isRuleInGroup(TEST_IP_RANGE, TEST_SG_ID));
        assertFalse(awsClient.isRuleInGroup(testRange, TEST_SG_ID));
    }

    @Test
    public void removeRuleFromGroup() {
        String testRange = "192.168.50.0/24";
        // clear existing rule if exists
        assertTrue(awsClient.removeRuleFromGroup(testRange, TEST_SG_ID, AWSClient.REMOVE_ALL_TOKEN));
        // add range with single token and make sure it is removed fully
        assertTrue(awsClient.addIpRangeToGroup(testRange, TEST_SG_ID, DEFAULT_DESCRIPTION));
        assertTrue(awsClient.removeRuleFromGroup(testRange, TEST_SG_ID, DEFAULT_DESCRIPTION));
        assertFalse(awsClient.isRuleInGroup(testRange, TEST_SG_ID));
        // make sure running remove a second time does not throw any errors and returns true
        assertTrue(awsClient.removeRuleFromGroup(testRange, TEST_SG_ID, DEFAULT_DESCRIPTION));
        // add range with multiple tokens
        assertTrue(awsClient.addIpRangeToGroup(testRange, TEST_SG_ID, DEFAULT_DESCRIPTION));
        assertTrue(awsClient.addIpRangeToGroup(testRange, TEST_SG_ID, "unit-test2"));
        // remove single token and make sure the rule is still there without the removed token
        assertTrue(awsClient.removeRuleFromGroup(testRange, TEST_SG_ID, "unit-test2"));
        assertTrue(awsClient.isRuleInGroup(testRange, TEST_SG_ID));
        IpRange currentRange = awsClient.getCurrentRule(testRange, TEST_SG_ID, 22);
        assertEquals(currentRange.description(), DEFAULT_DESCRIPTION);
        // cleanup the rule
        assertTrue(awsClient.removeRuleFromGroup(testRange, TEST_SG_ID, AWSClient.REMOVE_ALL_TOKEN));
        assertFalse(awsClient.isRuleInGroup(testRange, TEST_SG_ID));
    }

    @Test
    public void addIpRangeToGroup() {
        awsClient.removeRuleFromGroup(TEST_IP_RANGE, TEST_SG_ID, AWSClient.REMOVE_ALL_TOKEN);
        assertFalse(awsClient.isRuleInGroup(TEST_IP_RANGE, TEST_SG_ID.get(0)));

        assertTrue(awsClient.addIpRangeToGroup(TEST_IP_RANGE, TEST_SG_ID, DEFAULT_DESCRIPTION));
        assertTrue(awsClient.addIpRangeToGroup(TEST_IP_RANGE, TEST_SG_ID, DEFAULT_DESCRIPTION));
        assertTrue(awsClient.addIpRangeToGroup(TEST_IP_RANGE, TEST_SG_ID, "unit-test2"));
        IpRange currentRange = awsClient.getCurrentRule(TEST_IP_RANGE, TEST_SG_ID, 22);
        assertTrue(currentRange.description().equals(String.format("%s,unit-test2", DEFAULT_DESCRIPTION)));
        awsClient.removeRuleFromGroup(TEST_IP_RANGE, TEST_SG_ID, AWSClient.REMOVE_ALL_TOKEN);

        assertThrows(Ec2Exception.class, () -> {
            awsClient.addIpRangeToGroup("thisisnotarealrange", TEST_SG_ID, DEFAULT_DESCRIPTION);

        });
    }

    @Test
    public void s3PathExists() {
        assertFalse(awsClient.s3PathExists(TEST_BUCKET, "thiskeydoesnotexist"));
        awsClient.writeS3Object(TEST_BUCKET, "thiskeyexists", new byte[0]);
        assertTrue(awsClient.s3PathExists(TEST_BUCKET, "thiskeyexists"));
    }

    @Test
    public void writeS3Object() {
        String testString = UUID.randomUUID().toString();
        assertTrue(awsClient.writeS3Object(TEST_BUCKET, testString, testString.getBytes()));
        ResponseBytes<GetObjectResponse> objectBytes = awsClient.getS3Client().getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(TEST_BUCKET)
                        .key(testString)
                        .build()
        );
        byte[] responseData = objectBytes.asByteArray();
        assertEquals(new String(responseData), testString);
    }
}