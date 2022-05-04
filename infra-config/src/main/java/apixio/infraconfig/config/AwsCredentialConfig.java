package apixio.infraconfig.config;

public class AwsCredentialConfig {
    private String secretKey;
    private String accessKey;
    private String awsRole;
    private Integer ec2Retries;

    public Integer getEc2Retries() {
        return ec2Retries;
    }

    public void setEc2Retries(Integer ec2Retries) {
        this.ec2Retries = ec2Retries;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getAwsRole() {
        return awsRole;
    }

    public void setAwsRole(String awsRole) {
        this.awsRole = awsRole;
    }
}
