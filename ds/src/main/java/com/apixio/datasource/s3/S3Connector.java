package com.apixio.datasource.s3;

import java.util.Date;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import com.amazonaws.ClientConfiguration;

import com.amazonaws.auth.*;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;

import com.apixio.security.Security;
import com.apixio.security.exception.ApixioSecurityException;

/**
 * Connector holds the configured S3 connection.
 */

public class S3Connector {

    private final static Logger logger = LoggerFactory.getLogger(S3Connector.class);

    private AmazonS3 amazonS3;
    private int threadPoolSize = 10;
    private volatile TransferManager transferManager;

    private String accessKey;
    private String secretKey;
    private String region;
    private String defaultRegion = "us-west-2";
    private Date webIdentityTokenExpiration = null;
    private int connectionTimeoutMs = -1;
    private int socketTimeoutMs     = -1;

    private Security security;

    public S3Connector()
    {
        security = Security.getInstance();
    }

    public void close()
    {
        //        amazonS3.shutdown();
    }

    public void setThreadPoolSize(int threadPoolSize)
    {
        this.threadPoolSize = threadPoolSize;
    }

    public void setSecurity(Security sec)
    {
        this.security = sec;
    }

    public void setAccessKey(String accessKey)
    {
        try
        {
            this.accessKey = security.decrypt(accessKey);
        }
        catch (ApixioSecurityException e)
        {
            logger.error("Could not decrypt accesskey:" + accessKey, e);
        }
    }

    public void setUnencryptedAccessKey(String accessKey)
    {
        this.accessKey = accessKey;

    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setRegion(String region)
    {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }



    public void setSecretKey(String secretKey)
    {
        try
        {
            this.secretKey = security.decrypt(secretKey);
        }
        catch (ApixioSecurityException e)
        {
            logger.error("Could not decrypt secretKey:" + secretKey, e);
        }
    }

    public int getConnectionTimeoutMs()
    {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs)
    {
        if (connectionTimeoutMs >= 0)
        {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
    }

    public int getSocketTimeoutMs()
    {
        return socketTimeoutMs;
    }

    public void setSocketTimeoutMs(int socketTimeoutMs)
    {
        if (connectionTimeoutMs >= 0)
        {
            this.socketTimeoutMs = socketTimeoutMs;
        }
    }

    public void setUnencryptedSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getSecretKey()
    {
        return secretKey;
    }

    public AmazonS3 getAmazonS3()
    {
        if (needReloadWebIdentityToken()) {
            logger.info("Renew S3 client");
            amazonS3 = createS3Client();
        }
        return amazonS3;
    }

    public TransferManager getTransferManager()
    {
        if (transferManager == null)
        {
            synchronized (this)
            {
                if (transferManager == null)
                {
                    ThreadPoolExecutor executorService = createDefaultExecutorService(threadPoolSize);
                    executorService.setCorePoolSize((threadPoolSize + 1) / 2);
                    executorService.setMaximumPoolSize(threadPoolSize);
                    transferManager = new TransferManager(amazonS3, executorService);
                }
            }
        }

        return transferManager;
    }

    /**
     * init() sets up the actual Amazon s3 connection information.
     * The thread pool sizes are set from AWS advice.
     */

    private AWSCredentialsProvider getAwsCredentialsProvider() {
        AWSCredentialsProvider credentialsProvider;
        try {
            if (accessKey != null && secretKey != null) {
                credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
            } else {
                // https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts-technical-overview.html
                 BufferedReader reader = new BufferedReader(new FileReader(System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE")));
                 String token = reader.readLine();

                 String[] chunks = token.split("\\.");
                JSONObject payload = new JSONObject(new String(Base64.getUrlDecoder().decode(chunks[1])));
                this.webIdentityTokenExpiration = new Date(payload.getLong("exp"));

                credentialsProvider = WebIdentityTokenCredentialsProvider.create();
            }
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot setup aws provider.", e
            );
        }
        return credentialsProvider;
    }

    private AmazonS3 createS3Client() {
        if (region == null || region.isEmpty() || region.trim().isEmpty()) {
            region = defaultRegion;
        }

        if (connectionTimeoutMs >= 0 || socketTimeoutMs >= 0) {
            ClientConfiguration clientConfiguration = new ClientConfiguration();

            // connect timeout
            if (connectionTimeoutMs >= 0) {
                clientConfiguration.setConnectionTimeout(connectionTimeoutMs);
            }

            // connect timeout
            if (connectionTimeoutMs >= 0) {
                clientConfiguration.setConnectionTimeout(connectionTimeoutMs);
            }

            // socket timeout
            if (socketTimeoutMs >= 0) {
                clientConfiguration.setSocketTimeout(socketTimeoutMs);
            }

            // amazonS3 = new AmazonS3Client(credentials, clientConfiguration);
            return AmazonS3ClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(getAwsCredentialsProvider())
                    .withClientConfiguration(clientConfiguration).build();
        }
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(getAwsCredentialsProvider()).build();
    }

    private boolean needReloadWebIdentityToken() {
        if (webIdentityTokenExpiration != null) {
            logger.info("Check timer for renewing token");
            logger.info("webtoken: " + webIdentityTokenExpiration.getTime());
            logger.info("system time: " + System.currentTimeMillis());
            long timeRemaining = webIdentityTokenExpiration.getTime()*1000 - System.currentTimeMillis();
            return timeRemaining < 600000;
        }
        return false;
    }
    public void init()
    {
        amazonS3 = createS3Client();
    }

    /**
     * Returns a new thread pool configured with the default settings.
     *
     * @return A new thread pool configured with the default settings.
     */
    public static ThreadPoolExecutor createDefaultExecutorService(int threadPoolSize)
    {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadCount = 1;

            public Thread newThread(Runnable r)
            {
                Thread thread = new Thread(r);
                thread.setName("s3-transfer-manager-worker-" + threadCount++);
                return thread;
            }
        };
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    }


    private void checkConfig()
    {
        if (accessKey == null)
            throw new IllegalStateException("s3.accessKey not set; check system.properties");

        if (secretKey == null)
            throw new IllegalStateException("s3.secretKey not set; check system.properties");
    }
}
