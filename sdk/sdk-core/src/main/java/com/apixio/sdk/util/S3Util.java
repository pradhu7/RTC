package com.apixio.sdk.util;

import com.apixio.datasource.s3.S3Connector;
import com.apixio.datasource.s3.S3Ops;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.security.Security;

/**
 * Centralized utility for S3 access setup
 */
public class S3Util
{
    /**
     * "Standard" config points via ConfigSet via .yaml.  Expected structure of yaml is
     *
     *  s3:
     *    accessKey:             ""      # encrypted
     *    secretKey:             ""      # encrypted
     *    accessKeyUnencrypted:  ""
     *    secretKeyUnencrypted:  ""
     *
     * where one of the set of encrypted/unencrypted must be configured
     */
    public static final String S3_ACCESSKEY_ENCRYPTED   = "s3.accessKey";
    public static final String S3_SECRETKEY_ENCRYPTED   = "s3.secretKey";
    public static final String S3_ACCESSKEY_UNENCRYPTED = "s3.accessKeyUnencrypted";
    public static final String S3_SECRETKEY_UNENCRYPTED = "s3.secretKeyUnencrypted";
    
    /**
     * Bind early and often.
     */
    private static Security security = Security.getInstance();

    /**
     * S3 setup info
     */
    public static class S3Info
    {
        public String accessKey;      // always unencrypted
        public String secretKey;

        /**
         * Create from non-standard config
         */
        public static S3Info fromUnencryptedKeys(String accessKey, String secretKey)
        {
            S3Info s3Info = new S3Info();

            s3Info.accessKey = accessKey;
            s3Info.secretKey = secretKey;

            return s3Info;
        }

        public static S3Info fromEncryptedKeys(String accessKey, String secretKey)
        {
            return fromUnencryptedKeys(security.decrypt(accessKey), security.decrypt(secretKey));
        }

        /**
         * Create from standard config.  The "root" of the config set must be at the parent of the "s3"
         * key so that a config.getConfigString("s3") would return a non-null ConfigSet
         */
        public static S3Info fromConfig(ConfigSet config)
        {
            return fromUnencryptedKeys(
                getOrDecrypt(config, S3_ACCESSKEY_UNENCRYPTED, S3_ACCESSKEY_ENCRYPTED),
                getOrDecrypt(config, S3_SECRETKEY_UNENCRYPTED, S3_SECRETKEY_ENCRYPTED));
        }

        private static String getOrDecrypt(ConfigSet config, String unencryptedKeyname, String encryptedKeyname)
        {
            String  value = config.getString(unencryptedKeyname, null);

            if (value == null)
            {
                if ((value = config.getString(encryptedKeyname, null)) == null)
                    throw new IllegalArgumentException("Expected config key for " + unencryptedKeyname + " or " + encryptedKeyname);

                value = security.decrypt(value);
            }

            return value;
        }

    }

    /**
     *
     */
    public static S3Ops makeS3Ops(S3Info s3Info) throws Exception
    {
        S3Connector connector = new S3Connector();
        S3Ops       s3Ops     = new S3Ops();

        connector.setUnencryptedAccessKey(s3Info.accessKey);
        connector.setUnencryptedSecretKey(s3Info.secretKey);
        connector.init();

        s3Ops.setS3Connector(connector);

        return s3Ops;
    }

}
