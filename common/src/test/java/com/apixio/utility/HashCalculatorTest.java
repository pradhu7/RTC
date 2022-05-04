package com.apixio.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import junit.framework.Assert;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by dyee on 2/11/17.
 */
@Ignore("Uses custom file locations that are not available anywhere else")
public class HashCalculatorTest
{

    @Test
    public void sameHashTest() throws NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException
    {
        File file = new File("/Users/dyee/Downloads/with_claims.csv");
        File file2 = new File("/Users/dyee/Downloads/with_claims.csv");

        InputStream inputStream = new FileInputStream(file);
        InputStream inputStream2 = new FileInputStream(file2);

        final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

        final PipedInputStream pipedInputStream = new PipedInputStream();
        final PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

        final TeeInputStream tee = new TeeInputStream(inputStream, pipedOutputStream);

        final ByteArrayOutputStream dataOSThread = new ByteArrayOutputStream();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Callable<String> callable = new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

                try
                {
                    int read;
                    byte[] buf = new byte[4 * 1024];

                    while ((read = pipedInputStream.read(buf)) > 0)
                    {
                        //test #1 - write out to byte array stream
                        dataOSThread.write(buf,0,read);

                        //test #2 - write out incrementally to message digest
                        //          at the end, the bytestream and the incrementation
                        //          message digest should end up with the same hash value.
                        sha1.update(buf, 0, read);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                finally {
                    IOUtils.closeQuietly(pipedOutputStream);
                }
                return Hex.encodeHexString(sha1.digest());
            }
        };

        Future<String> future = executorService.submit(callable);

        System.out.println("processing btyes");

        byte[] buf = new byte[4 * 1024];

        ByteArrayOutputStream dataOS = new ByteArrayOutputStream();
        int read;
        while ((read = tee.read(buf)) > 0)
        {
            dataOS.write(buf,0,read);
        }

        tee.close();
        inputStream.close();

        System.out.println("waiting to get results of thread");
        String futureResult = future.get();

        pipedInputStream.close();
        pipedOutputStream.close();

        String hashCalculatedFromByteArray =  HashCalculator.getFileHash(IOUtils.toByteArray(inputStream2));

        Assert.assertEquals(HashCalculator.calculateHash(sha1, dataOS.toByteArray()),
                HashCalculator.calculateHash(sha1, dataOSThread.toByteArray()));

        Assert.assertEquals(HashCalculator.calculateHash(sha1, dataOS.toByteArray()), futureResult);

        Assert.assertEquals(HashCalculator.calculateHash(sha1, dataOS.toByteArray()), hashCalculatedFromByteArray);


        System.out.println("old way : [" + hashCalculatedFromByteArray + "]");

        System.out.println("orig    : [" + HashCalculator.calculateHash(sha1, dataOS.toByteArray()) + "]");
        System.out.println("teed    : [" + HashCalculator.calculateHash(sha1, dataOSThread.toByteArray()) + "]");
        System.out.println("teed(rs): [" + futureResult + "]");
    }


    @Ignore("Uses custom local file")
    @Test
    public void sameHashTest_WithDelay() throws NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException
    {
        //
        // Test hash generation with simulation of delay caused by concurrency
        // delays in executor servie thread pooling...
        //
        File file = new File("/Users/dyee/Downloads/with_claims.csv");
        File file2 = new File("/Users/dyee/Downloads/with_claims.csv");

        InputStream inputStream = new FileInputStream(file);
        InputStream inputStream2 = new FileInputStream(file2);

        final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

        final PipedInputStream pipedInputStream = new PipedInputStream();
        final PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

        final TeeInputStream tee = new TeeInputStream(inputStream, pipedOutputStream);

        final ByteArrayOutputStream dataOSThread = new ByteArrayOutputStream();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Callable<String> callable = new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

                try
                {
                    System.out.println("about to sleep");
                    Thread.sleep(20000);
                    System.out.println("about to process");
                    int read;
                    byte[] buf = new byte[4 * 1024];

                    while ((read = pipedInputStream.read(buf)) > 0)
                    {
                        dataOSThread.write(buf,0,read);
                        sha1.update(buf, 0, read);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                finally {
                    IOUtils.closeQuietly(pipedOutputStream);
                }
                return Hex.encodeHexString(sha1.digest());
            }
        };

        Future<String> future = executorService.submit(callable);

        System.out.println("processing btyes");

        byte[] buf = new byte[4 * 1024];

        ByteArrayOutputStream dataOS = new ByteArrayOutputStream();
        int read;
        while ((read = tee.read(buf)) > 0)
        {
            dataOS.write(buf,0,read);
        }

        tee.close();
        inputStream.close();

        System.out.println("waiting to get results of thread");
        String futureResult = future.get();

        pipedInputStream.close();
        pipedOutputStream.close();

        String hashCalculatedFromByteArray =  HashCalculator.getFileHash(IOUtils.toByteArray(inputStream2));

        Assert.assertEquals(HashCalculator.calculateHash(sha1, dataOS.toByteArray()),
                HashCalculator.calculateHash(sha1, dataOSThread.toByteArray()));

        Assert.assertEquals(HashCalculator.calculateHash(sha1, dataOS.toByteArray()), futureResult);

        Assert.assertEquals(HashCalculator.calculateHash(sha1, dataOS.toByteArray()), hashCalculatedFromByteArray);


        System.out.println("old way : [" + hashCalculatedFromByteArray + "]");

        System.out.println("orig    : [" + HashCalculator.calculateHash(sha1, dataOS.toByteArray()) + "]");
        System.out.println("teed    : [" + HashCalculator.calculateHash(sha1, dataOSThread.toByteArray()) + "]");
        System.out.println("teed(rs): [" + futureResult + "]");
    }

}
