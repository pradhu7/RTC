package com.apixio.datasource.s3;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.fail;

/**
 * Created by dyee on 4/24/17.
 */
@Ignore("Integration")
public class TestWriteToCV2 {
        private S3Ops  s3Ops;
        private String bucket = "apixio-cv2-io-stg";
        @Before
        public void setUp()
        {
            S3Connector s3Connector = new S3Connector();
            //s3Connector.setAccessKey("241763147971321772288V01x/ym1ScyFe3MsbXM85JNTB3mR8Bsil161A+3em1N1JC8="); // new keys by Alex
            //s3Connector.setSecretKey("2518105169776125584576V01xDlabz9YIJ0TJs0h600lBd3hd0yofsPylNkP+ekddsmK3Dh8WmjsMePrw6KYqUfa2");

            s3Connector.setAccessKey("2518137795002469175296V01xVtG7bcOEGugqVuC1M+iRXVWKhjox2EhgQAe8yIDzB+Q=");
            s3Connector.setSecretKey("2518137795002469175296V01xeZkTH6Wcmt9kHMEXOII6ps0BlIHFCZO5wsqy7VK5vN74SfjtNQP5bGBr7KVYhlEB");

            s3Connector.init();

            s3Ops = new S3Ops();
            s3Ops.setS3Connector(s3Connector);
        }

        @Test
        public void read() {
            String bucket = "apixio-cassandra-migration-test";
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucket);
            ObjectListing objects = s3Ops.getS3Connector().getTransferManager().getAmazonS3Client().listObjects(listObjectsRequest);

            for (String commonPrefix : objects.getCommonPrefixes()) {
                System.out.println("cp: " + commonPrefix);
            }
            for (S3ObjectSummary summary: objects.getObjectSummaries()) {
                System.out.println("summary: " + summary.getKey());
            }
        }

        @Test
        public void writeAndReadHierarchy()
        {
            try
            {
                InputStream stream = new ByteArrayInputStream("ha".getBytes(StandardCharsets.UTF_8));
                String orgId1 = UUID.randomUUID().toString();

                List<String> inputKeys = new ArrayList<>();
                List<String> inputKeysOrg1 = new ArrayList<>();
                List<String> inputKeysOrg2 = new ArrayList<>();

                String key = "testwriting" + "/" + orgId1 + "/batchid101/node1/workrequest01/task_001";
                inputKeys.add(key);
                inputKeysOrg1.add(key);
                s3Ops.addObject(bucket, key, stream);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail();
            }
        }
}

