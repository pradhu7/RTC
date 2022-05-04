package com.apixio.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.datasource.s3.S3Ops;
import com.apixio.ensemble.ifc.S3Services;

/**
 * Provides caching of S3 objects in the form of Ensemble's S3Services interface.  This interface
 * was chosen only to avoid a translation layer for the initial/intended use of this caching
 * feature.
 * <p>
 * This code delegates caching operations to the more general UriCacheManager.
 */
public class S3CacheManager implements S3Services
{

    private static final Logger LOG = LoggerFactory.getLogger(S3CacheManager.class);

    private S3Ops           s3Ops;
    private UriCacheManager uriCacher;

    /**
     *
     */
    public S3CacheManager(S3Ops s3Ops, Path tmpDir, UriCacheManager.Option... options) throws IOException
    {
        UriFetcher fetcher;

        this.s3Ops = s3Ops;

        fetcher = new UriFetcher() {
                public InputStream fetchUri(URI uri) throws IOException
                {
                    S3BucketKey bk = new S3BucketKey(uri);

                    return s3Ops.getObject(bk.getBucket(), bk.getKey());
                }
            };

        uriCacher = new UriCacheManager(tmpDir, fetcher, options);
    }

    /**
     * Return a java.nio.file.Path object to a local file that contains the contents of the given
     * S3 bucket+key, caching as necessary.  This is NOT a method in S3Service from ensemble
     */
    public Path getLocalObject(String bucketName, String key)
    {
        return uriCacher.getLocalObjectInfo(s3ToUri(bucketName, key)).getPath();
    }

    /**
     * S3Service-defined methods
     */
    @Override
    public boolean bucketExists(String bucketName)
    {
        try
        {
            return s3Ops.bucketExists(bucketName);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean createBucket(String bucketName)
    {
        try
        {
            return s3Ops.createBucket(bucketName);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String addObject(String bucketName, String key, File file)
    {
        try
        {
            return s3Ops.addObject(bucketName, key, file);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String addObject(String bucketName, String key, InputStream data)
    {
        try
        {
            return s3Ops.addObject(bucketName, key, data);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String copyObject(String sourceBucketName, String sourceKey,
                             String destBucketName, String destKey)
    {
        try
        {
            return s3Ops.copyObject(sourceBucketName, sourceKey, destBucketName, destKey);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String key)
    {
        return uriCacher.getObject(s3ToUri(bucketName, key));
    }

    @Override
    public long getObjectLength(String bucketName, String key)
    {
        try
        {
            return s3Ops.getObjectLength(bucketName, key);
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    @Override
    public boolean objectExists(String bucketName, String key)
    {
        try
        {
            return s3Ops.objectExists(bucketName, key);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<String> getKeys(String bucketName, String prefix)
    {
        try
        {
            return s3Ops.getKeys(bucketName, prefix);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void removeObject(String bucketName, String key, String version)
    {
        try
        {
            s3Ops.removeObject(bucketName, key, version);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void removeObject(String bucketName, String key)
    {
        try
        {
            s3Ops.removeObject(bucketName, key);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Convert the S3 bucket and key to a correctly-formed S3 URI, checking for data errors
     * that will cause problems later.
     */
    private URI s3ToUri(String bucketName, String key)
    {
        if ((bucketName == null) || ((bucketName = bucketName.trim()).length() == 0))
            throw new IllegalArgumentException("S3 bucket name can't be empty/null");
        else if (bucketName.indexOf('/') != -1)
            throw new IllegalArgumentException("S3 bucket name can't have '/' in it:  " + bucketName);
        else if ((key == null) || ((key = key.trim()).length() == 0))
            throw new IllegalArgumentException("S3 key name can't be empty/null");

        // keys should NOT start with / as it confuses S3 to have two /s like:  s3://mybucket//thekey
        if (key.startsWith("/"))
            throw new IllegalArgumentException("S3 keys should NOT start with '/': " + key);

        try
        {
            return new URI(S3BucketKey.S3_SCHEME + bucketName + "/" + key);
        }
        catch (URISyntaxException x)
        {
            throw new RuntimeException("Unable to create URI from bucket/key of " + bucketName + ", " + key, x);
        }
    }

}
