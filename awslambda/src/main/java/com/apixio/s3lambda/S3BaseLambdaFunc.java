package com.apixio.s3lambda;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.datasource.s3.S3Connector;
import com.apixio.datasource.s3.S3Ops;
import com.apixio.security.DecryptedInputStream;
import com.apixio.security.EncryptedInputStream;
import com.apixio.security.Security;

/**
 * Base code for S3-oriented AWS Lambda functions.  "S3-oriented" means that
 * the AWS function will access S3 objects.  It appears that it's not enough
 * for an IAM role to have S3 access, and it's necessary to provide the standard
 * S3 access key and secret keys, so this class gets those from environment
 * variables set by the AWS LAmbda Function configuration.
 */
public class S3BaseLambdaFunc
{
    static final Logger log = LoggerFactory.getLogger(S3BaseLambdaFunc.class);

    /**
     * What to do during request handling
     */
    public enum Mode
    {
        DECRYPT_ONLY,    // only read S3 objects
        ENCRYPT_ONLY,    // read (decrypt) and encrypt and write to {key}.v2encryption
        ENCRYPT_REPLACE  // full operation
    }

    /**
     * Define some useful messages that will show up in AWS CloudWatch logs so the
     * person creating the function might know what's needed if there's missing
     * info.
     */
    static private final String ENVVARS_ERROR = ("Missing AWS Lambda Function environment variable(s)!\n" +
                                                 "Apixio S3 Lambda Functions require the following environment variables" +
                                                 " to be set on the AWS Lambda Function (under Configuration tab):\n" +
                                                 " S3ACCESSKEY: encrypted access key for bucket of objects being processed\n" +
                                                 " S3SECRETKEY: encrypted secret key for bucket of objects being processed\n" +
                                                 " bcfipsname: name of resource findable by ClassLoader; like bc-fips-1.0.1.jar\n" +
                                                 " bcprovname: name of resource findable by ClassLoader; like bcprov-jdk15on-1.47.jar\n"
        );
    
    static private final String AUTH_ERROR = ("Missing AWS Lambda Function environment variable(s)!\n" +
                                              "Apixio S3 Lambda Functions require the following environment variables" +
                                              " to be set on the AWS Lambda Function (under Configuration tab):\n" +
                                              " one of VAULT_TOKEN, APX_VAULT_TOKEN or both" +
                                              " APX_VAULT_ROLE_ID and APX_VAULT_SECRET_ID\n"
        );


    /**
     * Fields
     */
    protected Mode     mode = Mode.DECRYPT_ONLY;
    protected Security security;
    protected S3Ops    s3Ops;

    /**
     * Sets up the Security object and the S3Ops DAO to access S3 objects
     */
    protected S3BaseLambdaFunc() throws Exception
    {
        String      strMode = System.getenv("RUN_MODE");
        S3Connector connector;

        checkEnvVars();

        security = Security.getInstance();

        connector = new S3Connector();
        connector.setAccessKey(System.getenv("S3ACCESSKEY"));
        connector.setSecretKey(System.getenv("S3SECRETKEY"));
        connector.init();

        s3Ops = new S3Ops();
        s3Ops.setS3Connector(connector);

        if (strMode != null)
            mode = Mode.valueOf(strMode.toUpperCase());

        log.info("S3LambdaFunc running in mode " + mode);
    }

    /**
     * Perform decryption of S3 object at bucket+key by reading it, and optionally encrypt it by writing it back out.
     * Scope parameter can be null, and if it is and encryption is done, then the default datakey will be used.
     */
    protected void encryptS3Object(String bucket, String key, String scope) throws Exception
    {
        log.info("S3LambdaFunc bucket=" + bucket + " key=" + key);

        try (InputStream is = new DecryptedInputStream(new BufferedInputStream(s3Ops.getObject(bucket, key)), security))
        {
            switch (mode) {
                case DECRYPT_ONLY:
                    // do nothing
                    break;
                case ENCRYPT_ONLY:
                    /**
                     * create new V2 encrypted file with suffix .v2encryption
                     */
                    String tmpKey = key + ".v2encryption";
                    s3Ops.addObject(bucket, tmpKey, EncryptedInputStream.encryptInputStreamWithScope(security, is, scope));
                    break;
                case ENCRYPT_REPLACE:
                    /**
                     * because the S3 Bucket Versioning is enabled. Should write directly instead of doing
                     * new -> copy override -> removed (increase 3x storage cost)
                     */
                    s3Ops.addObject(bucket, key, EncryptedInputStream.encryptInputStreamWithScope(security, is, scope));
                    break;
            }
        }
    }

    /**
     * Make sure the required environment variables have been defined for things
     * to work and throw an exception with useful info if not.
     */
    private void checkEnvVars()
    {
        boolean haveToken;
        boolean haveAuth;

        if ((System.getenv("S3ACCESSKEY") == null) ||
            (System.getenv("S3SECRETKEY") == null) ||
            (System.getenv("bcfipsname") == null) ||
            (System.getenv("bcprovname") == null))
            throw new IllegalStateException(ENVVARS_ERROR);

        haveToken = (System.getenv("VAULT_TOKEN")       != null) || (System.getenv("APX_VAULT_TOKEN")     != null);
        haveAuth  = (System.getenv("APX_VAULT_ROLE_ID") != null) && (System.getenv("APX_VAULT_SECRET_ID") != null);

        if (!haveToken && !haveAuth)
            throw new IllegalStateException(AUTH_ERROR);

        if (haveToken)
            log.info("Vault token " + System.getenv("VAULT_TOKEN") + "||" + System.getenv("APX_VAULT_TOKEN") +
                     " will be used");
        else
            log.info("Vault role/secret ids " + System.getenv("APX_VAULT_ROLE_ID") + "||" + System.getenv("APX_VAULT_SECRET_ID") +
                     " will be used");
    }

}
