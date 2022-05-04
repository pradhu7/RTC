package com.apixio.useracct.messager;

import com.amazonaws.auth.*;
import com.amazonaws.AmazonClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS SNS needs access key and secret key for credentials
 * Only some regions support direct messages to phone numbers
 */
public class AWSSNSMessenger extends Messenger
{
    public static final String AWSSNS = "AWSSNS";
    public static final String US_EAST_1 = "us_east_1";
    public static final String US_WEST_2 = "us_west_2";
    public static final String PROMOTIONAL = "Promotional";
    public static final String TRANSACTIONAL = "Transactional";
    private String messageType;
    private final String accessKey;
    private final String secretKey;
    private final Regions region;


    private AmazonSNS getAmazonSnSClient() {
        AWSCredentialsProvider credentialsProvider = null;

        try {
            if (accessKey != null && secretKey != null) {
                credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
            } else {
                credentialsProvider = WebIdentityTokenCredentialsProvider.create();
            }
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot setup aws client.", e
            );
        }

        return AmazonSNSClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();
    }
    public AWSSNSMessenger(String accessKey, String secretKey, String regionStr, String configMessageType)
    {
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        if (regionStr == null)
            throw new IllegalArgumentException("AWSSNS InvalidRegion specified " + regionStr);

        // US_WEST_2 will be the default as per dev ops
        if (regionStr.equals(US_EAST_1))
            this.region = Regions.US_EAST_1;
        else if (regionStr.equals(US_WEST_2))
            this.region = Regions.US_WEST_2;
        else
            this.region = Regions.US_WEST_2;

        if(configMessageType == null) {
            this.messageType = "Promotional";
        } else {
            if(checkMessageType(configMessageType)){
                this.messageType = configMessageType;
            }
        }

        this.setMessengertype(AWSSNS);
    }

    private boolean checkMessageType(String messageType){
        if((!messageType.equals(PROMOTIONAL)) && (!messageType.equals(TRANSACTIONAL))){
            throw new IllegalArgumentException("AWSSNS Invalid message type specified " + messageType);
        }
        return true;
    }

    private HashMap<String, MessageAttributeValue> setMessageAttributes(String messageType)
    {
        // Set Transactional for critical messages that support customer transactions,
        // such as one-time passcodes for multi-factor authentication.
        // Amazon SNS optimizes the message delivery to achieve the highest reliability.
        HashMap<String, MessageAttributeValue> smsAttributes =
                new HashMap<String, MessageAttributeValue>();
        smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue(messageType)
                .withDataType("String"));

        return smsAttributes;
    }

    /**
     * @param message     may need to add a small template later!
     * @param phoneNumber this has been been verified already
     *                    throws exceptions if the credentials are invalid
     */
    @Override
    public boolean sendMessage(String message, String phoneNumber, String perMessageType)
    {
        PublishResult publishResult = null;
        HashMap<String, MessageAttributeValue> smsAttributes = null;

        if(perMessageType == null){
            smsAttributes = setMessageAttributes(this.messageType);
        } else {
            if(checkMessageType(perMessageType)){
                smsAttributes = setMessageAttributes(perMessageType);
            }
        }

        publishResult = getAmazonSnSClient().publish(new PublishRequest().withMessage(message).withPhoneNumber(phoneNumber)
                .withMessageAttributes(smsAttributes));

        return (publishResult != null) && (publishResult.getSdkHttpMetadata().getHttpStatusCode() == 200);
    }
}
