package com.apixio.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Simple class to parse an S3 url into bucket,key components
 */
public class S3BucketKey
{
    /**
     * S3 URLs are like s3://bucket/key and key SHOULD NOT start with "/"!
     */
    public  static final String S3_URI_SCHEME = "s3";
    public  static final String S3_SCHEME     = S3_URI_SCHEME + "://";
    private static final int    S3_SCHEME_LEN = S3_SCHEME.length();

    /**
     *
     */
    private String bucket;
    private String key;

    /**
     * true if keyOrPath can be passed to ctor
     */
    public static boolean isS3Path(String keyOrPath)
    {
        return keyOrPath.startsWith(S3_SCHEME);
    }

    /**
     * Parse it
     */
    public S3BucketKey(String fullPath)
    {
        if (!fullPath.startsWith(S3_SCHEME) && !fullPath.toLowerCase().startsWith(S3_SCHEME))
            throw new IllegalArgumentException("Not a well-formed s3 url (must start with " + S3_SCHEME + "):  " + fullPath);

        String  rest   = fullPath.substring(S3_SCHEME_LEN);
        int     slash;

        // required syntax of S3-based URI:  s3://{bucket}/path/to/resource.  The access/secret keys for
        // {bucket} must have been correctly configured

        if ((slash = rest.indexOf('/')) == -1)
            throw new IllegalArgumentException("Malformed S3 URI " + fullPath + ":  no key after bucket");

        bucket = rest.substring(0, slash);
        key    = rest.substring(slash + 1);
    }

    /**
     * If we already have them separated
     */
    public S3BucketKey(String bucket, String key)
    {
        this.bucket = bucket;
        this.key    = key;
    }

    /**
     * Inverse of .toURI
     */
    public S3BucketKey(URI s3Uri)
    {
        String scheme = s3Uri.getScheme();

        if ((scheme == null) || !scheme.equalsIgnoreCase(S3_URI_SCHEME))
            throw new IllegalArgumentException("S3 URIs must start with '" + S3_SCHEME + "':  " + s3Uri);

        bucket = s3Uri.getHost();
        if ((bucket == null) || (bucket.length() == 0))
            throw new IllegalArgumentException("S3 bucket names can't be empty/null:  " + s3Uri);
        
        key = s3Uri.getPath().substring(1);    // paths always start with '/'
    }

    /**
     *
     */
    public String getBucket()
    {
        return bucket;
    }

    public String getKey()
    {
        return key;
    }

    /**
     * URI support
     */
    public URI toURI()
    {
        return toURI(bucket, key);
    }

    public static URI toURI(String bucketName, String key)
    {
        if (key.startsWith("/"))
            throw new IllegalArgumentException("S3 key must not start with '/'");

        try
        {
            return new URI(S3_SCHEME + bucketName + "/" + key);
        }
        catch (URISyntaxException x)
        {
            throw new IllegalArgumentException("Unable to convert s3 bucket + key to URI: " + bucketName + " " + key, x);
        }
    }

    @Override
    public String toString()
    {
        return S3_SCHEME + bucket + "/" + key;
    }

    /**
     * Test only
     */
    public static void main(String... args) throws Exception
    {
        if (args[0].equals("s3"))
        {
            S3BucketKey bk = new S3BucketKey(args[1]);

            System.out.println("Converted via fullpath: " + bk);
            System.out.println("and toURI:  " + bk.toURI());
        }
        else if (args[0].equals("uri"))
        {
            S3BucketKey bk = new S3BucketKey(new URI(args[1]));

            System.out.println("Converted via URI: " + bk);
            System.out.println("and toURI:  " + bk.toURI());
        }
    }

}
