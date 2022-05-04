package com.apixio.ensemblesdk.impl;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.apixio.ensemble.ifc.Reporter;
import com.apixio.ensemble.ifc.S3Services;
import com.apixio.sdk.FxLogger;
import com.apixio.datasource.s3.S3Ops;

/**
 * MLC-based function code can read S3 objects.
 */
public class SdkS3Services implements S3Services
{

    private S3Ops s3Ops;

    SdkS3Services(S3Ops s3Ops)
    {
        this.s3Ops = s3Ops;
    }

    @Override
    public boolean bucketExists(String bucketName)
    {
        try
        {
            return s3Ops.bucketExists(bucketName);
        }
        catch (Exception x)
        {
            throw new RuntimeException("bucketExists failure", x);
        }
    }

    @Override
    public boolean createBucket(String bucketName)
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public String addObject(String bucketName, String key, File file)
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public String addObject(String bucketName, String key, InputStream data)
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public String copyObject(String sourceBucketName, String sourceKey, String destBucketName, String destKey)
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public InputStream getObject(String bucketName, String key)
    {
        try
        {
            return s3Ops.getObject(bucketName, key);
        }
        catch (Exception x)
        {
            throw new RuntimeException("getObject failure", x);
        }
    }

    @Override
    public long getObjectLength(String bucketName, String key)
    {
        try
        {
            return s3Ops.getObjectLength(bucketName, key);
        }
        catch (Exception x)
        {
            throw new RuntimeException("getObjectLength failure", x);
        }
    }

    @Override
    public boolean objectExists(String bucketName, String key)
    {
        try
        {
            return s3Ops.objectExists(bucketName, key);
        }
        catch (Exception x)
        {
            throw new RuntimeException("objectExists failure", x);
        }
    }

    @Override
    public List<String> getKeys(String bucketName, String prefix)
    {
        try
        {
            return s3Ops.getKeys(bucketName, prefix);
        }
        catch (Exception x)
        {
            throw new RuntimeException("getKeys failure", x);
        }
    }

    @Override
    public void removeObject(String bucketName, String key, String version)
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public void removeObject(String bucketName, String key)
    {
        throw new RuntimeException("Unimplemented");
    }


}
