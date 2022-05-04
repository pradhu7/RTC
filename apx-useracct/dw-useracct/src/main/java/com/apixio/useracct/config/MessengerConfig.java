package com.apixio.useracct.config;

/**
 * Contains the yaml-poked messenger-related configuration:
 *
 *  * serviceType is the service used for sending messages (Only AWS is supported noe)
 *
 * Below are reuired only for AWS
 *
 *  * encryptedAccessKey is the access key for AWS which need to be decrypted
 *
 *  * encryptedSecretAccessKey is the secret key for AWS which needs to be decrypted
 *
 *  * region is the AWS regions
 *
 *  * messageType is either promotional or Transactional
 */
public class MessengerConfig
{
    private String serviceType;
    private String encryptedAccessKey;          // for AWS SNS
    private String encryptedSecretAccessKey;    // for AWS SNS
    private String region;                      // Only US_EAST_1
    private String messageType;

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getEncryptedAccessKey() { return encryptedAccessKey; }
    public void setEncryptedAccessKey(String encryptedAccessKey)
    {
        this.encryptedAccessKey = encryptedAccessKey;
    }

    public String getEncryptedSecretAccessKey() { return encryptedSecretAccessKey; }
    public void setEncryptedSecretAccessKey(String encryptedSecretAccessKey)
    {
        this.encryptedSecretAccessKey = encryptedSecretAccessKey;
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getMessageType() {
        return messageType;
    }
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

}
