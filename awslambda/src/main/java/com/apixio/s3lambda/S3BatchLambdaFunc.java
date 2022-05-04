package com.apixio.s3lambda;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.model.AmazonS3Exception;

/**
 * AWS Lambda function invoked by S3 batch operation.  Such a batch operation is
 * intended to work on a manifest list of S3 objects to perform some operation on
 * each of those S3 objects.  This class can perform the following operations on S3
 * objects:
 *
 *  * re-encrypt each and every object; all such objects are assumed to be encrypted
 *    with either v1 or v2 format.
 *
 *  * re-encrypt v2 format objects if and only if the internal encryption date is
 *    past a supplied cutoff date/time.  !! NOT YET DONE !!
 *
 * In all cases the newly encrypted object is put back at the same S3 location.
 *
 * Note that we can't use /tmp as a temporary store (see
 * https://docs.amazonaws.cn/en_us/lambda/latest/dg/limits.html) so encryption must
 * stream to the S3 replacement, which means that have to do a copy/remove to
 * the original S3 key.
 */
public class S3BatchLambdaFunc extends S3BaseLambdaFunc
    implements RequestHandler<Object, Object>
{
    static final Logger log = LoggerFactory.getLogger(S3BatchLambdaFunc.class);

    public S3BatchLambdaFunc() throws Exception
    {
        super();
    }

    /**
     * From https://docs.aws.amazon.com/AmazonS3/latest/dev/batch-ops-invoke-lambda.html the input
     * format is a map of maps, like so (dumped via event.toString()):
     *
     *   {
     *     invocationSchemaVersion=1.0,
     *     invocationId=AAAAAAAAAAGGkTSHu929b5u...d+,
     *     job={id=948f565c-f5fa-4dc9-8063-7156e6838e44},
     *     tasks=[
     *            {
     *              taskId=AAAAAAAAAAEnTBTu...9,
     *              s3BucketArn=arn:aws:s3:::apixio-security-library-testing,
     *              s3Key=002c/cccc/-372/5-42/b1-a/784-/df68/7bf1/af87,
     *              s3VersionId=null
     *            }
     *           ]
     *   }
     *
     * Output is map of maps like so:
     *
     *  {
     *     invocationSchemaVersion=1.0,
     *     treatMissingKeysAs=PermanentFailure,
     *     invocationId=YXNkbGZqYWRmaiBhc2RmdW9hZHNmZGpmaGFzbGtkaGZza2RmaAo,
     *     results=[
     *       {
     *         taskId=dGFza2lkZ29lc2hlcmUK,
     *         resultCode=Succeeded,
     *         resultString=["Mary Major", "John Stiles"]
     *       }
     *     ]
     *   }
     *
     */
    @Override
    public Object handleRequest(Object event, Context context)
    {
        Map<String,Object> wrap   = (Map<String,Object>) event;
        Map<String,String> task   = (Map<String,String>) ((List<Object>) wrap.get("tasks")).get(0);
        String             s3Buck = task.get("s3BucketArn").split(":::")[1];
        String             s3Key  = task.get("s3Key");
        String             code;
        String             msg;
        
        // https://docs.aws.amazon.com/AmazonS3/latest/dev/batch-ops-invoke-lambda.html
        try
        {
            encryptS3Object(s3Buck, decode(s3Key), LambdaUtil.getScope(s3Key));

            code = "Succeeded";
            msg = "";
        }
        catch (Exception x)
        {
            log.error("Failed processing S3 object", x);
            code = "TemporaryFailure";
            msg = x.getMessage();
        }

        return makeResponse(wrap, task, code, msg);
    }

    /**
     * Safe url decoder
     */
    private String decode(String urlEncoded)
    {
        try
        {
            return URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8.toString());
        }
        catch (UnsupportedEncodingException x)
        {
            return urlEncoded;  // should never happen as we're getting the charset from jre
        }
    }

    /**
     * Create a valid S3 Batch Operation response structure.
     */
    private Map<Object,Object> makeResponse(Map<String,Object> wrapper, Map<String,String> task, String code, String msg)
    {
        Map<Object,Object> resp    = new HashMap<>();
        List<Object>       results = new ArrayList<>();
        Map<Object,Object> result  = new HashMap<>();

        resp.put("invocationSchemaVersion", "1.0");
        resp.put("invocationId", wrapper.get("invocationId"));
        resp.put("results", results);

        results.add(result);

        result.put("taskId", task.get("taskId"));
        result.put("resultCode", code);
        result.put("resultString", msg);

        return resp;
    }

}
