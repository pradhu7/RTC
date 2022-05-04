package com.apixio.dao.storage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.apixio.dao.DAOTestUtils;
import com.apixio.datasource.s3.S3Ops;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.apixio.datasource.s3.S3OpsException;

@Ignore("Integration")
public class ApixioStorageTest
{
	private S3Ops s3Ops;
    private ApixioStorage apxStorage;
    private DAOTestUtils util;
    private String prefix = "";
    private String container;
	String pdsID = "8880888";

	@Before
	public void setUp() throws Exception
	{
        util = new DAOTestUtils();
		s3Ops = util.daoServices.getS3Ops();
        apxStorage = util.daoServices.getApixioStorage();
        container = util.config.getString("storageConfig.apixioFSConfig.mountPoint");
    }

    @Test
    public void testBucket() throws S3OpsException
    {
        try
        {
            boolean result = s3Ops.bucketExists("haha 11");
            System.out.println("bucket haha " + result);
            assertFalse(result);

            boolean result1 = s3Ops.bucketExists(container);
            System.out.println("bucket " + container + " " + result1);
            assertTrue(result1);
        }
        catch(Exception e)
        {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

	@Test
	public void testWriteAndRead1() throws S3OpsException
	{
		UUID testUUID = UUID.randomUUID();

		File testFile = new File("test.jpg");
		assertTrue(testFile.exists());

        assertTrue(s3Ops.bucketExists(container));

		try
		{
			assertNotNull(apxStorage.writeUsingPrefix(testUUID, testFile, prefix, false));

			InputStream is = apxStorage.readUsingPrefix(testUUID, prefix);
			assertNotNull(is);

    		assertTrue(apxStorage.fileExistsUsingPrefix(testUUID, prefix));
		}
		catch(Exception e)
		{
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testWriteAndRead2() throws S3OpsException
	{
		UUID testUUID = UUID.randomUUID();

		File testFile = new File("test.jpg");
		assertTrue(testFile.exists());

        assertTrue(s3Ops.bucketExists(container));

		try
		{
            assertNotNull(apxStorage.writeUsingPrefix(testUUID, testFile, prefix, true));

			sleep(1000 * 10L);

			InputStream is = apxStorage.readUsingPrefix(testUUID, prefix);
			assertNotNull(is);

    		assertTrue(apxStorage.fileExistsUsingPrefix(testUUID, prefix));
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testWriteAndRead3() throws S3OpsException
	{
		UUID testUUID = UUID.randomUUID();

		File testFile = new File("test.jpg");
		assertTrue(testFile.exists());

        assertTrue(s3Ops.bucketExists(container));

		try
		{
			assertNotNull(apxStorage.writeUsingPrefix(testUUID, new FileInputStream(testFile), prefix, false));

			InputStream is = apxStorage.readUsingPrefix(testUUID, prefix);
			assertNotNull(is);

    		assertTrue(apxStorage.fileExistsUsingPrefix(testUUID, prefix));
		}
		catch(Exception e)
		{
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testWriteAndRead4() throws S3OpsException
	{
		UUID testUUID = UUID.randomUUID();

		File testFile = new File("test.jpg");
		assertTrue(testFile.exists());

        assertTrue(s3Ops.bucketExists(container));

		try
		{
			assertNotNull(apxStorage.writeUsingPrefix(testUUID, new FileInputStream(testFile), prefix, true));

			sleep(1000 * 10L);

			InputStream is = apxStorage.readUsingPrefix(testUUID, prefix);
			assertNotNull(is);

    		assertTrue(apxStorage.fileExistsUsingPrefix(testUUID, prefix));
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	private void testReadKeysUsingPrefix(List<String> checkerDocList) throws Exception {
		String checkerID = "workNum" + RandomUtils.nextLong() + RandomUtils.nextLong();
		String bucketPrefix = "loadApoChecker";

		//
		// Step #1: write out data
		//

		InputStream stream = null;
		try
		{
			stream = new ByteArrayInputStream((checkerDocList.toString()).getBytes(StandardCharsets.UTF_8.name()));
			apxStorage.writeUsingPrefix(UUID.randomUUID(), stream, bucketPrefix + "/" +  pdsID + "/" +  checkerID, false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		finally
		{
			IOUtils.closeQuietly(stream);
		}

		//
		// Step #2: Read back the information
		//
		List<String> keys = apxStorage.getKeysUsingPrefix(null, bucketPrefix + "/" + pdsID + "/" +  checkerID);

		// Checker will not work unless there is one sequence file
		if (keys.size() != 1)
		{
			throw new Exception("should be exactly one sequence file - checkerID: " + checkerID + "; bucketPrefix: " + bucketPrefix);
		}

		stream     = apxStorage.readUsingPrefix(keys.get(0), bucketPrefix + "/" + pdsID + "/" +  checkerID);
		String      st         = stream != null ? IOUtils.toString(stream, StandardCharsets.UTF_8) : null;
		if(st!= null && st.startsWith("[") && st.endsWith("]")) {
			st = st.substring(1,st.length()-1);
		}

		List<String> uuidList  = st != null ? Arrays.asList(st.split(", ")) : null;

		if (uuidList == null)
		{
			fail("No input file for docuuids: " + checkerID + " and bucketPrefix: " + bucketPrefix);
		}

		//
		// Step #3, verify that the list we persisted is the one we get back..
		//

		Assert.assertEquals(checkerDocList.toString(), uuidList.toString());
	}


	@Test
	public void testReadKeysUsingPrefix() throws Exception {
		String checkerID = "workNum" + RandomUtils.nextLong() + RandomUtils.nextLong();
		String bucketPrefix = "loadApoChecker";

		//
		// Step #1: write out data
		//

		List<String> checkerDocList = new ArrayList<>();
		checkerDocList.add(UUID.randomUUID().toString());
		checkerDocList.add(UUID.randomUUID().toString());
		checkerDocList.add(UUID.randomUUID().toString());
		checkerDocList.add(UUID.randomUUID().toString());
		checkerDocList.add(UUID.randomUUID().toString());

		testReadKeysUsingPrefix(checkerDocList);
	}

	@Test
	public void testReadKeysUsingPrefix_OnekeyInList() throws Exception {
		String checkerID = "workNum" + RandomUtils.nextLong() + RandomUtils.nextLong();
		String bucketPrefix = "loadApoChecker";

		//
		// Step #1: write out data
		//

		List<String> checkerDocList = new ArrayList<>();
		checkerDocList.add(UUID.randomUUID().toString());

		testReadKeysUsingPrefix(checkerDocList);
	}

	@Test
	public void testReadKeysUsingPrefix_NokeyInList() throws Exception {
		String checkerID = "workNum" + RandomUtils.nextLong() + RandomUtils.nextLong();
		String bucketPrefix = "loadApoChecker";

		//
		// Step #1: write out data
		//

		List<String> checkerDocList = new ArrayList<>();

		testReadKeysUsingPrefix(checkerDocList);
	}

	private final static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ix) {
		}
	}
}
