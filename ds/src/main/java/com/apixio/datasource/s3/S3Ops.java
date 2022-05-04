package com.apixio.datasource.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.apixio.logger.EventLogger;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class S3Ops {
    private ThreadLocal<List<Upload>> uploads = new ThreadLocal<>();

    private final static Logger logger = LoggerFactory.getLogger(S3Ops.class);

    private static EventLogger eventLogger;

    private final static int   maxRetries = 10;
    private final static long  retrySleep = 500L;
    private static final int[] FIBONACCI  = new int[]{2, 3, 5, 8, 13, 21, 34};
    private final static int   maxKeys    = 3000; // read 3000 max keys at once
    private final static int   partSize   = 5242880 * 6; // 30MB worth of bytes
    private final List<String> essentialField = new ArrayList<String>() {
        {
            add("ETag");
            add("Last-Modified");
            add("Content-Length");
        }
    };


    private S3Connector connector;

    public void setS3Connector(S3Connector connector)
    {
        this.connector = connector;
    }

    public void close()
    {
        connector.close();
    }

    public S3Connector getS3Connector()
    {
        return connector;
    }


    public static void setEventLogger(final EventLogger eventLogger) {
        S3Ops.eventLogger = eventLogger;
    }

    public static EventLogger getEventLogger() {
        return S3Ops.eventLogger;
    }


    /**
     * It checks whether an existing bucket exists
     *
     * @param bucketName
     * @return
     * @throws S3OpsException
     */
    public boolean bucketExists(String bucketName)
            throws S3OpsException
    {
        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3 = connector.getAmazonS3();
                return amazonS3.doesBucketExist(bucketName);

            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * create a new bucket
     *
     * @param bucketName
     * @return
     * @throws S3OpsException
     */
    public boolean createBucket(String bucketName)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3 = connector.getAmazonS3();
                logS3CreateEvent(map);
                CreateBucketRequest request = new CreateBucketRequest(bucketName);

                Bucket bucket = amazonS3.createBucket(request);

                return (bucket != null);
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It turns on versioning for an existing bucket
     *
     * @param bucketName
     * @throws S3OpsException
     */
    public void turnOnBucketVersioning(String bucketName)
            throws S3OpsException
    {
        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                BucketVersioningConfiguration           versionConfig = new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
                SetBucketVersioningConfigurationRequest reqConfig     = new SetBucketVersioningConfigurationRequest(bucketName, versionConfig);

                AmazonS3 amazonS3 = connector.getAmazonS3();

                amazonS3.setBucketVersioningConfiguration(reqConfig);

                return;
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It adds an object stored in a file to an existing bucket
     *
     * @param bucketName
     * @param key
     * @param file
     * @return versionId
     * @throws S3OpsException
     */
    public String addObject(String bucketName, String key, File file)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                TransferManager transferManager = connector.getTransferManager();
                logS3PutEvent(map);
                Upload          upload          = transferManager.upload(bucketName, key, file);
                UploadResult    result          = upload.waitForUploadResult();
                return result != null ? result.getVersionId() : null;

            } catch (InterruptedException ex)
            {
                logger.error("Woke up from wait: InterruptedException");
                mx = ex;
                sleep(retrySleep, attempt);
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                sleep(retrySleep, attempt);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonClientException " + ace);
                mx = ace;
                sleep(retrySleep, attempt);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It adds asynchronously an object stored in a file to an existing bucket
     *
     * @param bucketName
     * @param key
     * @param file
     * @return
     * @throws S3OpsException
     */
    public void addAsyncObject(String bucketName, String key, File file)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                TransferManager transferManager = connector.getTransferManager();

                PutObjectRequest req = new PutObjectRequest(bucketName, key, file);
                req.withCannedAcl(CannedAccessControlList.AuthenticatedRead);
                logS3PutEvent(map);
                addToCache(transferManager.upload(req));

                return;
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It adds an object to an existing bucket. Closes Input Stream.
     *
     * @param bucketName
     * @param key
     * @param data
     * @return versionId
     * @throws S3OpsException
     */
    public String addObject(String bucketName, String key, InputStream data)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        TransferManager transferManager = connector.getTransferManager();
        AmazonS3        amazonS3Client  = transferManager.getAmazonS3Client();

        InitiateMultipartUploadRequest initRequest  = null;
        InitiateMultipartUploadResult  initResponse = null;

        try
        {
            // Step 1: Initialize.
            initRequest = new InitiateMultipartUploadRequest(bucketName, key);
            initResponse = amazonS3Client.initiateMultipartUpload(initRequest);

            // Create a list of UploadPartResponse objects. You get one of these for
            // each part upload.
            List<PartETag> partETags = new ArrayList<>();

            byte[] part = new byte[partSize];

            long      uploaded = 0;
            Exception mx       = null;

            // As long as there are fewer than 50,000 parts we are fine
            boolean lastPart = false;
            for (int partNumber = 1; !lastPart && partNumber < 50000; partNumber++)
            {
                // make sure you read the data corresponding to the part as InputStream.read() may return with less data than asked for
                int bytesRead = IOUtils.read(data, part);

                lastPart = bytesRead < partSize;
                ByteArrayInputStream bais = new ByteArrayInputStream(part);

                // upload part
                boolean partUploadSuccess = false;
                for (int attempt = 0; !partUploadSuccess && attempt < maxRetries; attempt++)
                {
                    bais.reset();

                    try
                    {
                        UploadPartRequest uploadRequest = new UploadPartRequest()
                                .withBucketName(bucketName).withKey(key)
                                .withUploadId(initResponse.getUploadId()).withPartNumber(partNumber)
                                .withInputStream(bais)
                                .withPartSize(bytesRead)
                                .withLastPart(false);

                        logS3PutEvent(map);
                        UploadPartResult result = amazonS3Client.uploadPart(uploadRequest);
                        partETags.add(result.getPartETag());

                        uploaded += bytesRead;
                        partUploadSuccess = true;

                    } catch (AmazonServiceException ase)
                    {
                        logger.warn("AmazonServiceException: attempt: " + attempt + " : " + ase);
                        mx = ase;
                        sleep(retrySleep, attempt);
                    } catch (AmazonClientException ace)
                    {
                        logger.error("AmazonClientException: attempt: " + attempt + " : " + ace);
                        mx = ace;
                        sleep(retrySleep, attempt);
                    }

                } // for

                if (!partUploadSuccess)
                {
                    throw new S3OpsException("S3Ops: Unable to upload part: " + partNumber + " max retries " + maxRetries + " reached - " + mx.getMessage(), mx);
                }
            }

            // Step 3: Complete.
            CompleteMultipartUploadRequest compRequest = new
                    CompleteMultipartUploadRequest(bucketName, key, initResponse.getUploadId(), partETags);

            logS3PutEvent(map);
            CompleteMultipartUploadResult result = amazonS3Client.completeMultipartUpload(compRequest);

            return result != null ? result.getVersionId() : null;

        } catch (Exception ex)
        {
            logger.warn("Aborting upload request");
            try
            {
                if (initResponse != null)
                {
                    AbortMultipartUploadRequest req = new AbortMultipartUploadRequest(bucketName, key, initResponse.getUploadId());
                    amazonS3Client.abortMultipartUpload(req);
                }
            } catch (Exception ignored)
            {
            }
            // wrap and throw
            ex.printStackTrace();
            throw new S3OpsException("S3Ops: unable to upload data", ex);
        } finally
        {
            if (data != null)
            {
                IOUtils.closeQuietly(data);
            }
        }

    }


    /**
     * It adds an object to an existing bucket
     *
     * @param bucketName
     * @param key
     * @param data
     * @return
     * @throws S3OpsException
     */

    public void addAsyncObject(String bucketName, String key, InputStream data)
            throws S3OpsException
    {
        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {

            try
            {
                TransferManager transferManager = connector.getTransferManager();

                // Create a list of UploadPartResponse objects. You get one of these for
                // each part upload.
                List<PartETag> partETags = new ArrayList<PartETag>();

                // Step 1: Initialize.
                InitiateMultipartUploadRequest initRequest  = new InitiateMultipartUploadRequest(bucketName, key);
                InitiateMultipartUploadResult  initResponse = transferManager.getAmazonS3Client().initiateMultipartUpload(initRequest);

                byte[] part = new byte[partSize];

                long uploaded = 0;

                // As long as there are fewer than 10,000 parts we are fine
                for (int partNumber = 1; partNumber < 10000; partNumber++)
                {
                    // make sure you read the data corresponding to the part as InputStream.read() may return with less data than asked for
                    int bytesRead = IOUtils.read(data, part);

                    boolean              lastPart = bytesRead < partSize;
                    ByteArrayInputStream bais     = new ByteArrayInputStream(part);
                    // Create request to upload a part.
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName(bucketName).withKey(key)
                            .withUploadId(initResponse.getUploadId()).withPartNumber(partNumber)
                            .withInputStream(bais)
                            .withPartSize(bytesRead)
                            .withLastPart(false);


                    UploadPartResult result = transferManager.getAmazonS3Client().uploadPart(uploadRequest);
                    partETags.add(result.getPartETag());
                    uploaded += bytesRead;

                    if (lastPart)
                    {
                        break;
                    }
                }
                // Step 3: Complete.
                CompleteMultipartUploadRequest compRequest = new
                        CompleteMultipartUploadRequest(bucketName,
                        key,
                        initResponse.getUploadId(),
                        partETags);

                transferManager.getAmazonS3Client().completeMultipartUpload(compRequest);

                // How does cache work and how would it work here?
                // addToCache(transferManager.upload(req));

                return;
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
                if (data != null)
                {
                    try
                    {
                        data.close();
                    } catch (IOException e)
                    {
                        logger.error("IO Exception");
                        throw new S3OpsException();
                    }
                }
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It copies a file from an existing s3 location to a new s3 location
     *
     * @param sourceBucketName
     * @param sourceKey
     * @param destBucketName
     * @param destKey
     * @return versionId
     * @throws S3OpsException
     */
    public String copyObject(String sourceBucketName, String sourceKey, String destBucketName, String destKey) throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("sourceBucket", sourceBucketName);
        map.put("destBucket", destBucketName);

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3 = connector.getAmazonS3();

                CopyObjectRequest req = new CopyObjectRequest(sourceBucketName, sourceKey, destBucketName, destKey);
                req.withCannedAccessControlList(CannedAccessControlList.AuthenticatedRead);
                logS3CopyEvent(map);
                CopyObjectResult result = amazonS3.copyObject(req);

                return result != null ? result.getVersionId() : null;
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It gets an object given a bucket and a key
     *
     * @param bucketName
     * @param key
     * @return data (input stream)
     * @throws S3OpsException
     */
    public InputStream getObject(String bucketName, String key)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        Exception mx       = null;
        S3Object  s3Object = null;
        boolean   success  = false;
        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3 = connector.getAmazonS3();
                logS3GetEvent(map);
                GetObjectRequest req = new GetObjectRequest(bucketName, key);

                s3Object = amazonS3.getObject(req);

                if (s3Object != null)
                {
                    S3ObjectInputStream inputStream = s3Object.getObjectContent();
                    success = true;
                    return inputStream;
                }

                return null;
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);

                if (ase.getStatusCode() == 404) {
                    throw new KeyNotFoundException("Key not found in S3: " + key, ase);
                }

                mx = ase;
                sleep(retrySleep, attempt);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonClientException " + ace);
                mx = ace;
                sleep(retrySleep, attempt);
            } catch (RuntimeException ex)
            {
                String msg = ex.getMessage().toLowerCase();
                if (msg != null && msg.contains("read timed out"))
                {
                    logger.error("RuntimeException " + ex);
                    mx = ex;
                    sleep(retrySleep, attempt);
                }
            } finally
            {
                //
                // We **must** close the object we get back from the S3 objects, as long as we don't have
                // success.
                //
                if (!success)
                {
                    IOUtils.closeQuietly(s3Object);
                }
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * @param bucketName
     * @param key
     * @return -1 when key does not exist.
     */
    public long getObjectLength(String bucketName, String key)
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3                 amazonS3 = connector.getAmazonS3();
                GetObjectMetadataRequest req      = new GetObjectMetadataRequest(bucketName, key);
                logS3GetMetadataEvent(map);
                ObjectMetadata           metadata = amazonS3.getObjectMetadata(req);
                if (metadata == null)
                {
                    return -1;
                }
                return metadata.getContentLength();

            } catch (AmazonServiceException ex)
            {
                logger.error("AmazonServiceException " + ex);
                if (ex.getStatusCode() == 404) // key not found
                {
                    return -1;
                }
                sleep(retrySleep, attempt);
            } catch (AmazonClientException ex)
            {
                logger.error("AmazonServiceException " + ex);
                sleep(retrySleep, attempt);
            }
        }

        return -1;
    }


    /**
     * @param bucketName
     * @param key
     * @return map of s3 object metadata
     */
    public Map<String, Object> getObjectRawMetadata(String bucketName, String key) {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                AmazonS3 amazonS3 = connector.getAmazonS3();
                GetObjectMetadataRequest req = new GetObjectMetadataRequest(bucketName, key);
                logS3GetMetadataEvent(map);
                ObjectMetadata metadata = amazonS3.getObjectMetadata(req);
                if (metadata == null) {
                    return Collections.emptyMap();
                }
                return metadata.getRawMetadata();
            } catch (AmazonServiceException ex) {
                logger.error("AmazonServiceException " + ex);
                if (ex.getStatusCode() == 404) // key not found
                {
                    return Collections.emptyMap();
                }
                sleep(retrySleep, attempt);
            } catch (AmazonClientException ex) {
                logger.error("AmazonServiceException " + ex);
                sleep(retrySleep, attempt);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * @param bucketName
     * @param prefix
     * @return List of maps with s3 object metadata
     */
    public List<Map> getListObjectRawMetadata(String bucketName, String prefix) throws S3OpsException {
        List<String> keys = getKeys(bucketName, prefix);

        ArrayList<Map> metadataList = new ArrayList<>();
        for (String key : keys) {
            Map<String, Object> originalRawMetadata = getObjectRawMetadata(bucketName, key);
            Map<String, Object> finalRawMetadata = new HashMap<>();
            //add the path to the object for good measure
            finalRawMetadata.put("path", key);
            //keep only essential fields in the metadata object
            for (String field : essentialField) {
                finalRawMetadata.put(field, originalRawMetadata.get(field));
            }
            metadataList.add(finalRawMetadata);
        }
        return metadataList;
    }

    /**
     * @param bucketName
     * @param key
     * @return null when key does not exist.
     */
    public String getMd5(String bucketName, String key)
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3                 amazonS3 = connector.getAmazonS3();
                GetObjectMetadataRequest req      = new GetObjectMetadataRequest(bucketName, key);
                logS3GetMetadataEvent(map);
                ObjectMetadata           metadata = amazonS3.getObjectMetadata(req);

                metadata.getContentLength();
                metadata.getLastModified();
                // THIS IS A KNOWN HACK IN S3 - getContentMD5 always returns null and you need to get it from Etag
                // Be careful when updating the AWS S3 version as this might have changed!!!
                // return (metadata != null) ? metadata.getContentMD5() : null;
                return (metadata != null) ? metadata.getETag() : null;

            } catch (AmazonServiceException ex)
            {
                logger.error("AmazonServiceException " + ex);
                if (ex.getStatusCode() == 404) // key not found
                {
                    return null;
                }
                sleep(retrySleep, attempt);
            } catch (AmazonClientException ex)
            {
                logger.error("AmazonServiceException " + ex);
                sleep(retrySleep, attempt);
            }
        }

        return null;
    }

    /**
     * It checks whether an object exists given a bucket and a key
     *
     * @param bucketName
     * @param key
     * @throws S3OpsException
     */

    public boolean objectExists(String bucketName, String key)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3 = connector.getAmazonS3();
                GetObjectMetadataRequest req = new GetObjectMetadataRequest(bucketName, key);
                logS3GetMetadataEvent(map);
                ObjectMetadata objMetaData = amazonS3.getObjectMetadata(req);

                return (objMetaData != null && objMetaData.getContentLength() > 0);
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                if (ase.getStatusCode() == 404) {
                    throw new KeyNotFoundException(key, ase);
                }
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep, attempt);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep, attempt);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }


    /**
     * It returns the list of keys given a bucket and a prefix
     *
     * @param bucketName
     * @param prefix
     * @throws S3OpsException
     */
    public List<String> getKeys(String bucketName, String prefix)
            throws S3OpsException
    {
        KeysAndMarker keysAndMarker = getFirstKeys(bucketName, prefix);
        List<String>  allKeys       = keysAndMarker.keys;
        String        marker        = keysAndMarker.marker;

        while (marker != null)
        {
            keysAndMarker = getNextKeys(bucketName, prefix, marker);
            allKeys.addAll(keysAndMarker.keys);

            marker = keysAndMarker.marker;
        }

        return allKeys;
    }

    private KeysAndMarker getFirstKeys(String bucketName, String prefix)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("prefix", prefix);

        Exception    mx   = null;
        List<String> keys = new ArrayList<>();

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3   = connector.getAmazonS3();
                String   nextMarker = null;

                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().
                        withBucketName(bucketName).
                        withPrefix(prefix).
                        withMaxKeys(maxKeys);

                logS3ListEvent(map);
                ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);

                if (objectListing != null && objectListing.getObjectSummaries() != null)
                {
                    List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();
                    for (S3ObjectSummary summary : summaries)
                    {
                        keys.add(summary.getKey());
                    }

                    nextMarker = objectListing.isTruncated() ? objectListing.getNextMarker() : null;
                }

                return new KeysAndMarker(keys, nextMarker);
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    private KeysAndMarker getNextKeys(String bucketName, String prefix, String marker)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("prefix", prefix);

        Exception    mx   = null;
        List<String> keys = new ArrayList<>();

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3   = connector.getAmazonS3();
                String   nextMarker = null;

                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().
                        withBucketName(bucketName).
                        withPrefix(prefix).
                        withMarker(marker).
                        withMaxKeys(maxKeys);

                logS3ListEvent(map);
                ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);

                if (objectListing != null && objectListing.getObjectSummaries() != null)
                {
                    List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();
                    for (S3ObjectSummary summary : summaries)
                    {
                        keys.add(summary.getKey());
                    }

                    nextMarker = objectListing.isTruncated() ? objectListing.getNextMarker() : null;
                }

                return new KeysAndMarker(keys, nextMarker);
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    private static class KeysAndMarker {
        private KeysAndMarker(List<String> keys, String marker)
        {
            this.keys = keys;
            this.marker = marker;
        }

        private List<String> keys;
        private String       marker;
    }

    /**
     * It removes an existing version from a bucket
     *
     * @param bucketName
     * @param key
     * @param version
     * @throws S3OpsException
     */
    public void removeObject(String bucketName, String key, String version)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3 = connector.getAmazonS3();
                logS3DeleteEvent(map);
                amazonS3.deleteVersion(bucketName, key, version);

                return;
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It removes an existing "unversioned" object from a bucket
     *
     * @param bucketName
     * @param key
     * @throws S3OpsException
     */
    public void removeObject(String bucketName, String key)
            throws S3OpsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("bucket", bucketName);
        map.put("key", key);

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                AmazonS3 amazonS3 = connector.getAmazonS3();
                logS3DeleteEvent(map);
                amazonS3.deleteObject(bucketName, key);

                return;
            } catch (AmazonServiceException ase)
            {
                logger.error("AmazonServiceException " + ase);
                mx = ase;
                ase.printStackTrace();
                sleep(retrySleep);
            } catch (AmazonClientException ace)
            {
                logger.error("AmazonServiceException " + ace);
                mx = ace;
                ace.printStackTrace();
                sleep(retrySleep);
            }
        }

        throw new S3OpsException("S3Ops: max tries " + maxRetries + " reached - " + mx.getMessage(), mx);
    }

    /**
     * It waits for completion of all s3 uploads
     */
    public void waitForAllCompletion()
    {
        try
        {
            List<Upload> all = getCache();

            for (Upload one : all)
            {
                try
                {
                    one.waitForUploadResult();
                } catch (Exception e)
                {
                }
            }

            cleanCache();
        } catch (Exception e)
        {

        }
    }

    /**
     * It waits for completion of all s3 uploads if
     * number of uploads is greater than the upload
     * batch size
     */
    public void waitForBatchCompletion()
    {
        if (cacheSize() > 200)
        {
            waitForAllCompletion();
        }
    }

    private void addToCache(Upload upload)
    {
        List<Upload> all = getCache();

        all.add(upload);
    }

    private List<Upload> getCache()
    {
        List<Upload> all = uploads.get();

        if (all == null)
        {
            all = new ArrayList<>();
            uploads.set(all);
        }

        return all;
    }

    private int cacheSize()
    {
        List<Upload> all = uploads.get();

        return (all != null) ? all.size() : 0;
    }

    private void cleanCache()
    {
        uploads.set(null);
        uploads.remove();
    }

    private static void sleep(Long ms)
    {
        sleep(ms, 0);
    }

    // zero based attempt
    private static void sleep(Long ms, int attempt)
    {
        try
        {
            int index = 0;
            if (attempt >= 0)
            {
                index = attempt;
            }
            index = (index >= FIBONACCI.length) ? FIBONACCI.length - 1 : index;

            long sleepMs = FIBONACCI[index] * ms;
            sleepMs = Math.min(sleepMs, 1000 * 60 * 15); // 15 mins
            Thread.sleep(sleepMs);

        } catch (InterruptedException ix)
        {
            logger.info("woke up from sleep due to interruption!");
        }
    }

    private static void logS3CreateEvent(Map<String, Object> map) {
        map.put("operation", "create");
        logS3Event(map);
    }

    private static void logS3GetMetadataEvent(Map<String, Object> map) {
        map.put("operation", "getmd");
        logS3Event(map);
    }

    private static void logS3GetEvent(Map<String, Object> map) {
        map.put("operation", "get");
        logS3Event(map);
    }

    private static void logS3PutEvent(Map<String, Object> map) {
        map.put("operation", "put");
        logS3Event(map);
    }

    private static void logS3CopyEvent(Map<String, Object> map) {
        map.put("operation", "copy");
        logS3Event(map);
    }

    private static void logS3ListEvent(Map<String, Object> map) {
        map.put("operation", "list");
        logS3Event(map);
    }

    private static void logS3DeleteEvent(Map<String, Object> map) {
        map.put("operation", "delete");
        logS3Event(map);
    }

    private static void logS3Event(Map<String, Object> map) {
        if (eventLogger != null) {
            eventLogger.event(map);
        }
    }
}
