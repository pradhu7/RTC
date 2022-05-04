package com.apixio.s3lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class S3RunLocal extends S3BaseLambdaFunc
{
    static final Logger log = LoggerFactory.getLogger(S3RunLocal.class);

    public S3RunLocal() throws Exception
    {
        super();
    }

    // args: bucket key
    public static void main(String[] args) throws Exception
    {
        S3RunLocal test = new S3RunLocal();

        if (args.length < 4)
        {
            System.err.println("Missing args.  Usage:  S3RunLocal <bucket> <key> <scope> [true/false]  # true-> grab scope from key");
        }
        else
        {
            test.run(args[0], args[1], args[2], args[3]);
        }
    }

    public void run(String bucket, String key, String scope, String grabScope) throws Exception
    {
        if (Boolean.valueOf(grabScope))
            scope = LambdaUtil.getScope(key);
        else if ("".equals(scope))
            scope = null;

        try
        {
            encryptS3Object(bucket, key, scope);
        }
        catch (Exception x)
        {
            log.error("Failed processing S3 object", x);
        }
    }
}
