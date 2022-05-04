package apixio.infraconfig.client;

import org.antlr.v4.runtime.misc.OrderedHashSet;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Ec2Response;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.UpdateSecurityGroupRuleDescriptionsIngressRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Helper object to handle the calls to AWS. Requires use of assume role and credentials are assume to be provided
 * by environment variable or system properties
 */
public class AWSClient {
    private Region awsRegion;
    private String awsRole;
    private AwsCredentials configCredentials;
    private Credentials credentials = null;
    private final Integer DEFAULT_SFTP_PORT = 22;
    private Logger log = Logger.getLogger(this.getClass().getName());
    private Ec2Client ec2Client;
    private S3Client s3Client;
    public Integer ec2Retries = 10;

    private Boolean recreateOnDescriptionUpdate = false;
    protected static String REMOVE_ALL_TOKEN = "REMOVE_ALL";

    public AWSClient(String awsRegion, String awsRole, String accessKey, String accessKeySecret) {
        this(Region.of(awsRegion), awsRole, false);
        if (Objects.nonNull(accessKey) && Objects.nonNull(accessKeySecret)) {
            this.configCredentials = AwsBasicCredentials.create(accessKey, accessKeySecret);
        }
        refreshCredentials();
    }

    public AWSClient(String awsRegion, String awsRole) {
        this(Region.of(awsRegion), awsRole, true);
    }

    public AWSClient(Region awsRegion, String awsRole) {
        this(awsRegion, awsRole, true);
    }


    public AWSClient(Region awsRegion, String awsRole, Boolean initCredentials) {
        this.awsRegion = awsRegion;
        this.awsRole = awsRole;
        if (initCredentials) {
            refreshCredentials();
        }
    }

    public Boolean getRecreateOnDescriptionUpdate() {
        return recreateOnDescriptionUpdate;
    }

    public void setRecreateOnDescriptionUpdate(Boolean recreateOnDescriptionUpdate) {
        this.recreateOnDescriptionUpdate = recreateOnDescriptionUpdate;
    }

    public synchronized void refreshCredentials() {
        ec2Client = getEc2Client();
        s3Client = getS3Client();
    }

    public Boolean addIpRangeToGroup(String newRange, List<String> securityGroupIds, String descToken) throws Ec2Exception, InputMismatchException {
        String matchedSecurityGroupId = null;
        IpRange range = IpRange.builder().cidrIp(newRange).build();
        Map<String, Integer> sgRulesCounts = new HashMap<>();
        for (String securityGroupId : securityGroupIds) {
            if (isRuleInGroup(range, securityGroupId)) {
                matchedSecurityGroupId = securityGroupId;
                break;
            }
        }
        if (Objects.isNull(matchedSecurityGroupId)) {
            for (String securityGroupId : securityGroupIds) {
                sgRulesCounts.put(securityGroupId, getNumRules(securityGroupId));
            }
            Entry<String, Integer> sgWithLeastRules = null;
            for (Entry<String, Integer> entry : sgRulesCounts.entrySet()) {
                if (sgWithLeastRules == null || sgWithLeastRules.getValue() > entry.getValue()) {
                    sgWithLeastRules = entry;
                }
            }
            matchedSecurityGroupId = sgWithLeastRules.getKey();
        }
        return addIpRangeToGroup(IpRange.builder().cidrIp(newRange).build(), matchedSecurityGroupId, DEFAULT_SFTP_PORT, descToken);
    }

    public Boolean addIpRangeToGroup(String newRange, String securityGroupId, String descToken) throws Ec2Exception, InputMismatchException {
        return addIpRangeToGroup(IpRange.builder().cidrIp(newRange).build(), securityGroupId, DEFAULT_SFTP_PORT, descToken);
    }

    public Boolean addIpRangeToGroup(IpRange newRange, String securityGroupId, String descToken) throws Ec2Exception, InputMismatchException {
        return addIpRangeToGroup(newRange, securityGroupId, DEFAULT_SFTP_PORT, descToken);
    }

    public Boolean addIpRangeToGroup(String newRange, String securityGroupId, Integer sftpPort, String descToken) throws Ec2Exception, InputMismatchException {
        return addIpRangeToGroup(IpRange.builder().cidrIp(newRange).build(), securityGroupId, sftpPort, descToken);
    }

    public Boolean addIpRangeToGroup(IpRange newRange, String securityGroupId, Integer sftpPort, String descToken) throws Ec2Exception, InputMismatchException {
        if (descToken.contains(",")) {
            throw new InputMismatchException(String.format("Description token cannot contain ','. '%s'", descToken));
        }
        try {
            IpRange ingressIpRange = getCurrentRule(newRange, securityGroupId, sftpPort);
            if (Objects.isNull(ingressIpRange)) {
                log.info(String.format("Rule for %s - '%s' does not exist. Adding new rule", newRange.cidrIp(), sftpPort.toString()));
                newRange = newRange.toBuilder().description(addDescriptionItem("", descToken)).build();
            } else {
                newRange = newRange.toBuilder().description(addDescriptionItem(ingressIpRange.description(), descToken)).build();
            }

            IpPermission ingressRule = IpPermission.builder().ipProtocol("tcp").fromPort(sftpPort).toPort(sftpPort).ipRanges(newRange).build();

            if (Objects.nonNull(ingressIpRange) && ingressIpRange.equals(newRange)) {
                return true;
            }

            Ec2Response ingressRuleResponse;
            if (Objects.nonNull(ingressIpRange)) {
                // used mainly for unit testing as the mock api does not support description updates
                if (recreateOnDescriptionUpdate) {
                    forceRemoveRuleFromGroup(newRange, securityGroupId, sftpPort);
                    AuthorizeSecurityGroupIngressRequest ingressRuleRequest = AuthorizeSecurityGroupIngressRequest.builder().groupId(securityGroupId).ipPermissions(ingressRule).build();
                    ingressRuleResponse = ec2Client.authorizeSecurityGroupIngress(ingressRuleRequest);
                } else {
                    UpdateSecurityGroupRuleDescriptionsIngressRequest ingressRuleRequest = UpdateSecurityGroupRuleDescriptionsIngressRequest
                            .builder()
                            .groupId(securityGroupId)
                            .ipPermissions(ingressRule)
                            .build();
                    ingressRuleResponse = ec2Client.updateSecurityGroupRuleDescriptionsIngress(ingressRuleRequest);
                }
            } else {
                AuthorizeSecurityGroupIngressRequest ingressRuleRequest = AuthorizeSecurityGroupIngressRequest.builder().groupId(securityGroupId).ipPermissions(ingressRule).build();
                ingressRuleResponse = ec2Client.authorizeSecurityGroupIngress(ingressRuleRequest);
            }
            if (ingressRuleResponse.sdkHttpResponse().isSuccessful()) {
                log.info(String.format("Added rule %s to %s", newRange.toString(), securityGroupId));
                return true;
            } else {
                log.warning(String.format("Failed to add %s to %s: %s", newRange.toString(), securityGroupId, ingressRuleResponse.sdkHttpResponse().statusText()));
                return false;
            }
        } catch (Ec2Exception e) {
            log.severe("Error calling the EC2 to configure security group:" + e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public String addDescriptionItem(String currentDescription, String desc) throws InputMismatchException {
        return getNewDescription(currentDescription, desc, true);
    }

    public String removeDescriptionItem(String currentDescription, String desc) throws InputMismatchException {
        return getNewDescription(currentDescription, desc, false);
    }

    public String getNewDescription(String currentDescription, String desc, Boolean add) throws InputMismatchException {
        Set<String> descList = new LinkedHashSet<>();
        if (Objects.nonNull(currentDescription) && !currentDescription.isEmpty()) {
            descList.addAll(Arrays.asList(currentDescription.split(",")));
        }
        if (!descList.contains(desc) && add) {
            descList.add(desc);
        }

        if (descList.contains(desc) && !add) {
            descList.remove(desc);
        }
        return String.join(",", descList);
    }

    public IpRange getCurrentRule(String newRange, List<String> securityGroupIds, Integer sftpPort) throws Ec2Exception {
        IpRange matchedIpRange = null;
        IpRange range = IpRange.builder().cidrIp(newRange).build();
        for (String securityGroupId : securityGroupIds) {
            if (isRuleInGroup(range, securityGroupId, sftpPort)) {
                matchedIpRange = getCurrentRule(range, securityGroupId, sftpPort);
                break;
            }
        }
        return matchedIpRange;
    }

    public IpRange getCurrentRule(String newRange, String securityGroupId, Integer sftpPort) throws Ec2Exception {
        return getCurrentRule(IpRange.builder().cidrIp(newRange).build(), securityGroupId, sftpPort);
    }

    public IpRange getCurrentRule(IpRange newRange, String securityGroupId, Integer sftpPort) throws Ec2Exception {
        try {
            DescribeSecurityGroupsRequest describeRequest = DescribeSecurityGroupsRequest.builder().groupIds(securityGroupId).build();
            DescribeSecurityGroupsResponse describeResponse = ec2Client.describeSecurityGroups(describeRequest);
            // iterate though all the SG's returned. will only be a single group since our request specifies the groupId
            for (SecurityGroup group : describeResponse.securityGroups()) {
                if (group.hasIpPermissions()) {
                    // go though all the rules with IP address permissions
                    for (IpPermission ingressRule : group.ipPermissions()) {
                        if (ingressRule.hasIpRanges()) {
                            for (IpRange ingressIpRange : ingressRule.ipRanges()) {
                                if (ingressIpRange.cidrIp().equals(newRange.cidrIp()) && ingressRule.toPort().equals(sftpPort) && ingressRule.fromPort().equals(sftpPort)) {
                                    log.fine(String.format("Rule exists: %s", ingressRule.toString()));
                                    return ingressIpRange;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Ec2Exception e) {
            log.severe("Error calling EC2:" + e.awsErrorDetails().errorMessage());
            throw e;
        }
        return null;

    }

    public Boolean isRuleInGroup(String newRange, List<String> securityGroupIds) throws Ec2Exception {
        for (String securityGroupId : securityGroupIds) {
            if (isRuleInGroup(IpRange.builder().cidrIp(newRange).build(), securityGroupId, DEFAULT_SFTP_PORT)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isRuleInGroup(String newRange, String securityGroupId) throws Ec2Exception {
        return isRuleInGroup(IpRange.builder().cidrIp(newRange).build(), securityGroupId, DEFAULT_SFTP_PORT);
    }

    public Boolean isRuleInGroup(IpRange newRange, String securityGroupId) throws Ec2Exception {
        return isRuleInGroup(newRange, securityGroupId, DEFAULT_SFTP_PORT);
    }

    public Boolean isRuleInGroup(String newRange, String securityGroupId, Integer sftpPort) throws Ec2Exception {
        return isRuleInGroup(IpRange.builder().cidrIp(newRange).build(), securityGroupId, sftpPort);
    }

    public Boolean isRuleInGroup(IpRange newRange, String securityGroupId, Integer sftpPort) throws Ec2Exception {
        return Objects.nonNull(getCurrentRule(newRange, securityGroupId, sftpPort));
    }

    public Boolean removeRuleFromGroup(String newRange, List<String> securityGroupIds, String descToken) throws Ec2Exception {
        String matchedSecurityGroupId = null;
        IpRange range = IpRange.builder().cidrIp(newRange).build();
        for (String securityGroupId : securityGroupIds) {
            if (isRuleInGroup(range, securityGroupId)) {
                matchedSecurityGroupId = securityGroupId;
                break;
            }
        }
        // if there is no match then return true as it is already removed from all SG's
        if (Objects.isNull(matchedSecurityGroupId)) {
            return true;
        }
        return removeRuleFromGroup(IpRange.builder().cidrIp(newRange).build(), matchedSecurityGroupId, DEFAULT_SFTP_PORT, descToken);
    }

    public Boolean removeRuleFromGroup(String newRange, String securityGroupId, String descToken) throws Ec2Exception {
        return removeRuleFromGroup(IpRange.builder().cidrIp(newRange).build(), securityGroupId, DEFAULT_SFTP_PORT, descToken);
    }

    public Boolean removeRuleFromGroup(IpRange newRange, String securityGroupId, String descToken) throws Ec2Exception {
        return removeRuleFromGroup(newRange, securityGroupId, DEFAULT_SFTP_PORT, descToken);
    }

    public Boolean removeRuleFromGroup(String newRange, String securityGroupId, Integer sftpPort, String descToken) throws Ec2Exception {
        return removeRuleFromGroup(IpRange.builder().cidrIp(newRange).build(), securityGroupId, sftpPort, descToken);
    }


    /*
    remove rule forcefully. ignores any token or description information
     */
    public Boolean forceRemoveRuleFromGroup(IpRange newRange, String securityGroupId, Integer sftpPort) throws Ec2Exception {
        if (!isRuleInGroup(newRange, securityGroupId, sftpPort)) {
            return true;
        }
        IpRange ingressIpRange = getCurrentRule(newRange, securityGroupId, sftpPort);
        Ec2Response ingressRuleResponse;
        IpPermission ingressRule;
        ingressRule = IpPermission.builder().ipProtocol("tcp").fromPort(sftpPort).toPort(sftpPort).ipRanges(ingressIpRange).build();
        log.info(String.format("Rule for %s - '%s' exists. Removing rule", newRange.cidrIp(), sftpPort.toString()));
        RevokeSecurityGroupIngressRequest ingressRuleRequest = RevokeSecurityGroupIngressRequest.builder().groupId(securityGroupId).ipPermissions(ingressRule).build();
        ingressRuleResponse = ec2Client.revokeSecurityGroupIngress(ingressRuleRequest);
        if (ingressRuleResponse.sdkHttpResponse().isSuccessful()) {
            log.info(String.format("Removed rule %s from $s", newRange.toString(), securityGroupId));
            return true;
        } else {
            log.warning(String.format("Failed remove %s from %s: %s", newRange.toString(), securityGroupId, ingressRuleResponse.sdkHttpResponse().statusText()));
            return false;
        }
    }

    public Boolean removeRuleFromGroup(IpRange newRange, String securityGroupId, Integer sftpPort, String descToken) throws Ec2Exception {
        if (descToken.contains(",")) {
            throw new InputMismatchException(String.format("Description token cannot contain ','. '%s'", descToken));
        }
        try {
            //DescribeSecurityGroupsRequest describeRequest = DescribeSecurityGroupsRequest.builder().groupIds(securityGroupId).build();
            //DescribeSecurityGroupsResponse describeResponse = ec2Client.describeSecurityGroups(describeRequest);
            // iterate though all the SG's returned. will only be a single group since our request specifies the groupId
            if (!isRuleInGroup(newRange, securityGroupId, sftpPort)) {
                return true;
            }
            IpRange ingressIpRange = getCurrentRule(newRange, securityGroupId, sftpPort);
            if (!descToken.equals(REMOVE_ALL_TOKEN)) {
                newRange = newRange.toBuilder().description(removeDescriptionItem(ingressIpRange.description(), descToken)).build();
            }

            if (ingressIpRange.equals(newRange) && !newRange.description().isEmpty()) {
                return true;
            }

            Ec2Response ingressRuleResponse;
            IpPermission ingressRule;
            if (Objects.isNull(newRange.description()) || newRange.description().isEmpty()) {
                ingressRule = IpPermission.builder().ipProtocol("tcp").fromPort(sftpPort).toPort(sftpPort).ipRanges(ingressIpRange).build();
                log.info(String.format("Rule for %s - '%s' exists. Removing rule", newRange.cidrIp(), sftpPort.toString()));
                RevokeSecurityGroupIngressRequest ingressRuleRequest = RevokeSecurityGroupIngressRequest.builder().groupId(securityGroupId).ipPermissions(ingressRule).build();
                ingressRuleResponse = ec2Client.revokeSecurityGroupIngress(ingressRuleRequest);
            } else {
                if (recreateOnDescriptionUpdate) {
                    forceRemoveRuleFromGroup(newRange, securityGroupId, sftpPort);
                    ingressRule = IpPermission.builder().ipProtocol("tcp").fromPort(sftpPort).toPort(sftpPort).ipRanges(newRange).build();
                    AuthorizeSecurityGroupIngressRequest ingressRuleRequest = AuthorizeSecurityGroupIngressRequest.builder().groupId(securityGroupId).ipPermissions(ingressRule).build();
                    ingressRuleResponse = ec2Client.authorizeSecurityGroupIngress(ingressRuleRequest);
                } else {
                    ingressRule = IpPermission.builder().ipProtocol("tcp").fromPort(sftpPort).toPort(sftpPort).ipRanges(newRange).build();
                    log.fine(String.format("Rule for %s - '%s' exists but there is still a reference in the description. Updaing description", newRange.cidrIp(), sftpPort.toString()));
                    UpdateSecurityGroupRuleDescriptionsIngressRequest ingressRuleRequest = UpdateSecurityGroupRuleDescriptionsIngressRequest
                            .builder()
                            .groupId(securityGroupId)
                            .ipPermissions(ingressRule)
                            .build();
                    ingressRuleResponse = ec2Client.updateSecurityGroupRuleDescriptionsIngress(ingressRuleRequest);
                }
            }
            if (ingressRuleResponse.sdkHttpResponse().isSuccessful()) {
                log.info(String.format("Removed rule %s from $s", newRange.toString(), securityGroupId));
                return true;
            } else {
                log.warning(String.format("Failed remove %s from %s: %s", newRange.toString(), securityGroupId, ingressRuleResponse.sdkHttpResponse().statusText()));
                return false;
            }
        } catch (Ec2Exception e) {
            log.severe("Error calling the EC2 to configure security group:" + e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public Set<String> getAllowedRanges(String securityGroupId, Integer sftpPort) {
        return getAllowedRanges(securityGroupId, sftpPort, ".*");
    }

    public Set<String> getAllowedRanges(String securityGroupId, Integer sftpPort, String descriptionRegex) {
        Set<String> allowedRanges = new OrderedHashSet<>();
        try {
            DescribeSecurityGroupsRequest describeRequest = DescribeSecurityGroupsRequest.builder().groupIds(securityGroupId).build();
            DescribeSecurityGroupsResponse describeResponse = ec2Client.describeSecurityGroups(describeRequest);
            // iterate though all the SG's returned. will only be a single group since our request specifies the groupId
            for (SecurityGroup group : describeResponse.securityGroups()) {
                if (group.hasIpPermissions()) {
                    // go though all the rules with IP address permissions
                    for (IpPermission ingressRule : group.ipPermissions()) {
                        if (!(ingressRule.toPort().equals(sftpPort) && ingressRule.fromPort().equals(sftpPort))) {
                            continue;
                        }
                        if (ingressRule.hasIpRanges()) {
                            for (IpRange ingressIpRange : ingressRule.ipRanges()) {
                                if (ingressIpRange.description().matches(descriptionRegex)) {
                                    allowedRanges.add(ingressIpRange.cidrIp());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Ec2Exception e) {
            log.severe("Error calling EC2:" + e.awsErrorDetails().errorMessage());
            throw e;
        }
        return allowedRanges;
    }

    public Integer getNumRules(String securityGroupId) {
        Integer numRules = 0;
        try {
            DescribeSecurityGroupsRequest describeRequest = DescribeSecurityGroupsRequest.builder().groupIds(securityGroupId).build();
            DescribeSecurityGroupsResponse describeResponse = ec2Client.describeSecurityGroups(describeRequest);
            // iterate though all the SG's returned. will only be a single group since our request specifies the groupId
            for (SecurityGroup group : describeResponse.securityGroups()) {
                numRules += group.ipPermissions().size() + group.ipPermissionsEgress().size();
            }
        } catch (Ec2Exception e) {
            log.severe("Error calling EC2:" + e.awsErrorDetails().errorMessage());
            throw e;
        }
        return numRules;
    }

    protected Ec2Client getEc2Client() {
        AwsCredentials sessionCredentials = getSessionCredentials();
        RetryPolicy retryPolicy = RetryPolicy.builder(RetryMode.STANDARD).numRetries(ec2Retries).build();
        Ec2Client client = Ec2Client.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build())
                .build();
        return client;
    }

    protected S3Client getS3Client() {
        AwsCredentials sessionCredentials = getSessionCredentials();
        S3Client client = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                .build();
        return client;
    }

    public Credentials getStsCredentials() {
        if (Objects.isNull(credentials) ||
                Instant.now().minusSeconds(60).isAfter(credentials.expiration())
        ) {
            StsClient stsclient;
            if (Objects.nonNull(configCredentials)) {
                stsclient = StsClient.builder().region(awsRegion).credentialsProvider(StaticCredentialsProvider.create(configCredentials)).build();
            } else {
                stsclient = StsClient.builder().region(awsRegion).build();
            }
            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(awsRole)
                    .roleSessionName(UUID.randomUUID().toString())
                    .build();
            AssumeRoleResponse assumeRoleResponse = stsclient.assumeRole(assumeRoleRequest);
            SdkHttpResponse sdkHttpResponse = assumeRoleResponse.sdkHttpResponse();
            if (!sdkHttpResponse.isSuccessful()) {
                throw new RuntimeException(String.format("Unable to assume role %s: %s", awsRole, sdkHttpResponse.statusText()));
            }
            credentials = assumeRoleResponse.credentials();
        }
        return credentials;
    }

    public AwsCredentials getSessionCredentials() {
        Credentials ec2Credentials = getStsCredentials();
        return AwsSessionCredentials.create(
                ec2Credentials.accessKeyId(),
                ec2Credentials.secretAccessKey(),
                ec2Credentials.sessionToken());
    }

    public Boolean s3PathExists(String bucket, String key) {
        try {
            log.fine(String.format("Checking if key %s exists in %s", key, bucket));
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            if (response.sdkHttpResponse().isSuccessful()) {
                log.fine(String.format("Key %s exists in %s", key, bucket));
                return true;
            } else {
                log.fine(String.format("Key %s does not exists in %s", key, bucket));
                return false;
            }
        } catch (NoSuchKeyException e) {
            log.fine(String.format("Key %s does not exists in %s", key, bucket));
            return false;
        }
    }

    public Boolean writeS3Object(String bucket, String key, byte[] content) {
        log.fine(String.format("writing data to key %s in %s", key, bucket));
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).build();
        RequestBody body = RequestBody.fromBytes(content);
        PutObjectResponse response = s3Client.putObject(request, body);
        if (response.sdkHttpResponse().isSuccessful()) {
            log.info(String.format("Wrote %s to %s", key, bucket));
            return true;
        } else {
            log.warning(String.format("Failed to write %s to %s: %s", key, bucket, response.sdkHttpResponse().statusText()));
            return false;
        }
    }
}
