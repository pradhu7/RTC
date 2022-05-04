package com.apixio.s3lambda;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.event.S3EventNotification;

/**
 * AWS Lambda function with S3-ish trigger.  This is used mostly for debugging
 * and testing purposes as it's easier and faster to trigger a test from the
 * Lambda GUI/console than it is to create the S3 Batch Operation and wade
 * through the log messages.
 *
 * The only real difference between these two modes is the type of object passed
 * to the handleRequest method
 */
public class S3DebugLambdaFunc extends S3BaseLambdaFunc
    implements RequestHandler<S3EventNotification, String>
{
    static final Logger log = LoggerFactory.getLogger(S3EventNotification.class);

    /**
     * Testing on just this one object; pulled from env vars
     */
    private String s3Bucket;
    private String s3Key;

    public S3DebugLambdaFunc() throws Exception
    {
        super();

        s3Bucket = System.getenv("S3BUCKET");
        s3Key    = System.getenv("S3KEY");
    }

    @Override
    public String handleRequest(S3EventNotification s3EventNotification, Context context)
    {
        //        String s3buck = "apixio-security-library-testing";
        //        String s3key  = "002e/fd8a/-f0f/a-42/bc-b/e39-/5316/f69c/f8fb";

        try
        {
            // scope is null for debug as we can't guarantee that the S3 url has the pds id in it
            encryptS3Object(s3Bucket, s3Key, null);
        }
        catch (Exception x)
        {
            log.error("failed to read/decrypt/encrypt/put S3 object", x);
        }

        return null;
    }

}
