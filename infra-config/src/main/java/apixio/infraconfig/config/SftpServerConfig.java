package apixio.infraconfig.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SftpServerConfig {
    private String region;
    private String hostname;
    private Integer port;
    @JsonFormat(with = Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> securityGroupId;
    private String awsRole;
    private String s3Bucket;
    private String userAwsRole;
    // cidrs in this list are not managed by the service
    private Set<String> globalCidrWhitelist = new LinkedHashSet<>();

    public String getUserAwsRole() {
        return userAwsRole;
    }

    public void setUserAwsRole(String userAwsRole) {
        this.userAwsRole = userAwsRole;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getAwsRole() {
        return awsRole;
    }

    public void setAwsRole(String awsRole) {
        this.awsRole = awsRole;
    }

    public List<String> getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(List<String> securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Set<String> getGlobalCidrWhitelist() {
        return globalCidrWhitelist;
    }

    public void setGlobalCidrWhitelist(Set<String> globalCidrWhitelist) {
        this.globalCidrWhitelist = globalCidrWhitelist;
    }
}
