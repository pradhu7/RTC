package com.apixio.datasource.s3;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author lschneider
 * 12/1/15.
 */
@Ignore("Integration")
public class S3OpsTest
{
    private S3Ops  s3Ops;
    private String bucket = "vram-testing"; // asked Alex to create a special test bucket for me

    @Before
    public void setUp()
    {
        S3Connector s3Connector = new S3Connector();
        s3Connector.setUnencryptedAccessKey("AKIAJQ5UHDWHMWZ73ANA"); // new keys by Alex
        s3Connector.setUnencryptedSecretKey("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");
        s3Connector.init();

        s3Ops = new S3Ops();
        s3Ops.setS3Connector(s3Connector);
    }

    @Test
    public void writeAndRead1Key()
    {
        writeAndReadKeys(1);
    }

    @Test
    public void writeAndReadOneLessThanMaxDefaultKeys()
    {
        writeAndReadKeys(999);
    }

    @Test
    public void writeAndReadMaxDefaultKeys()
    {
        writeAndReadKeys(1000);
    }

    @Test
    public void writeAndReadOneMoreThanMaxDefaultKeys()
    {
        writeAndReadKeys(1001);
    }

    @Test
    public void writeAndReadOneLessThanMaxKeys()
    {
        writeAndReadKeys(2999);
    }

    @Test
    public void writeAndReadMaxKeys()
    {
        writeAndReadKeys(3000);
    }

    @Test
    public void writeAndReadOneMoreThanMaxKeys()
    {
        writeAndReadKeys(3001);
    }

    @Test
    public void writeAndReadOneLessThanDoubleMaxKeys()
    {
        writeAndReadKeys(5999);
    }

    @Test
    public void writeAndReadDoubleMaxKeys()
    {
        writeAndReadKeys(6000);
    }

    @Test
    public void writeAndReadOneMoreThanDoubleMaxKeys()
    {
        writeAndReadKeys(6001);
    }

    @Test
    public void writeAndReadLargeNumberOfKeys()
    {
        writeAndReadKeys(10001);
    }

    private void writeAndReadKeys(int repeat)
    {
        try
        {
            InputStream stream = new ByteArrayInputStream("ha".getBytes(StandardCharsets.UTF_8));
            String prefix = UUID.randomUUID().toString();

            List<String> inputKeys = new ArrayList<>();

            for (int i = 0; i < repeat; i++)
            {
                System.out.println("Loop " + i);

                String key = prefix + i;
                inputKeys.add(key);

                s3Ops.addObject(bucket, key, stream);
            }

            System.out.println("checking if keys are in S3");

            List<String> keys = s3Ops.getKeys(bucket, prefix);

            assertTrue(keys.containsAll(inputKeys));
            assertTrue(inputKeys.containsAll(keys));
            assertEquals(keys.size(), repeat); // redundant check!!!
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }
}
