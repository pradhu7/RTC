package com.apixio.datasource.afs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.datasource.afs.Mount.Type;
import com.apixio.datasource.s3.S3Ops;

/**
 *
 * "Apixio File System", an abstraction for storing files on various media.
 */

public class ApixioFS
{
    private final static Logger logger = LoggerFactory.getLogger(ApixioFS.class);

    private S3Ops s3ops;

    public void setS3Ops(S3Ops s3ops)
    {
        this.s3ops = s3ops;
    }
    /**
     * Given a list of mount points, a uuid, and input stream, it creates the file synchronously or
     * asynchronously in all the mount systems (s3 or filesystem)
     *
     * @param mountPoints
     * @param uuid
     * @param data
     * @param async
     * @return key
     * @throws IOException
     *
     */
    public String create(List<Mount> mountPoints, String uuid, InputStream data, boolean async)
            throws IOException
    {
        String key = makeKey(uuid);

        for (Mount mountPoint : mountPoints)
        {
            if (mountPoint.type == Mount.Type.S3)
            {
                if (async)
                    s3ops.addAsyncObject(mountPoint.mount, mountPoint.prefix + "/" + key, data);
                else
                    s3ops.addObject(mountPoint.mount, mountPoint.prefix + "/" + key, data);
            }
            else
            {
                createFile(mountPoint.mount, mountPoint.prefix + "/" + key, data);
            }
        }

        return key;
    }

    /**
     * Given a list of mount points, a uuid, and an input file, it creates the file synchronously or
     * asynchronously in all the mount systems (s3 or filesystem)
     *
     * @param mountPoints
     * @param uuid
     * @param file
     * @param async
     * @return key
     * @throws IOException
     *
     */
    public String create(List<Mount> mountPoints, String uuid, File file, boolean async) throws IOException
    {
        String key = makeKey(uuid);

        for (Mount mountPoint : mountPoints)
        {
            if (mountPoint.type == Mount.Type.S3)
            {
                if (async)
                    s3ops.addAsyncObject(mountPoint.mount, mountPoint.prefix + "/" + key, file);
                else
                    s3ops.addObject(mountPoint.mount, mountPoint.prefix + "/" + key, file);
            }
            else
            {
                key = createFile(mountPoint.mount, mountPoint.prefix + "/" + key, file);
            }
        }

        return key;
    }

    /**
     * Given mount point and uuid, it checks whether the file exists.
     *
     * @param inMount
     * @param uuid
     * @return true if it exists; false otherwise
     * @throws IOException
     *
     */
    public boolean fileExists(Mount inMount, String uuid) throws IOException
    {
    	String key = makeKey(uuid);

    	if (inMount.type == Mount.Type.S3)
    	{
    		return s3ops.objectExists(inMount.mount, inMount.prefix + "/" + key);
    	}

    	//TODO for other Mount types.
    	return false;
    }

    public long fileSize(Mount inMount, String uuid) throws IOException
    {
        String key = makeKey(uuid);

        if (inMount.type == Type.S3)
        {
            return s3ops.getObjectLength(inMount.mount, inMount.prefix + "/" + key);
        }

        // TODO for other Mount types
        return -1;
    }

    // For migration program
    public String copy(Mount from, List<Mount> toMounts, String sourceKey, String uuid) throws IOException
    {
    	String key = makeKey(uuid);

        if (from.type == Mount.Type.S3)
        {
        	for (Mount mountPoint : toMounts)
            {
                if (mountPoint.type == Mount.Type.S3)
                	s3ops.copyObject(from.mount, sourceKey, mountPoint.mount, mountPoint.prefix + "/" + key);
            }
        }

        return key;
    }

    // not yet implemented
    public String move(Mount from, Mount to, String uuid)
    {
        return null;
    }

    /**
     * Given mount point and keyPrefix, it gets all the keys that match the prefix.
     *
     * @param inMount
     * @param keyPrefix
     * @return list of keys.
     * @throws IOException
     *
     */
    public List<String> getKeys(Mount inMount, String keyPrefix) throws IOException
    {
    	if (inMount.type == Mount.Type.S3)
    	{
    		String       newKey         = makeKey(keyPrefix);
            List<String> keysWithPrefix = (newKey != null && !newKey.isEmpty())  ?
                    s3ops.getKeys(inMount.mount, inMount.prefix + "/" + newKey) :
                    s3ops.getKeys(inMount.mount, inMount.prefix);

            int index = new String(inMount.prefix + "/").length();

            List<String> keys = new ArrayList<>();

            for (String key : keysWithPrefix)
            {
                String st = key.substring(index);
                keys.add(st.replace("/", ""));
            }

            return keys;
    	}

    	//TODO for other Mount types.
    	return null;
    }

    /**
     * Given mount point and uuid, it returns file stored in s3 or local file system.
     *
     * @param mountPoint
     * @param uuid
     * @return input stream - the file stored in s3 or local file system
     * @throws IOException
     *
     */
    public InputStream getInputStream(Mount mountPoint, String uuid) throws IOException
    {
        String key = makeKey(uuid);

        if (mountPoint.type == Mount.Type.S3)
            return s3ops.getObject(mountPoint.mount, mountPoint.prefix + "/" + key);
        else
            return getInputStream(mountPoint.mount, mountPoint.prefix + "/" + key);
    }

    /**
     * Given mount point and key, it returns the uri form of the key.
     *
     * @param mountPoint
     * @param uuid
     * @return uri
     *
     */
    public String getURI(Mount mountPoint, String uuid)
    {
        String key = makeKey(uuid);

        if (mountPoint.type == Type.S3)
        {
            return "s3://" + mountPoint.mount + "/" + mountPoint.prefix + "/" + key;
        }
        else if (mountPoint.type == Type.FS)
        {
            return "file://" + mountPoint.mount + "/" + mountPoint.prefix + "/" + key;
        }
        else
        {
            throw new IllegalStateException("File type not implemented");
        }
    }

    public void waitForAllCompletion()
    {
        s3ops.waitForAllCompletion();
    }

    public void waitForBatchCompletion()
    {
        s3ops.waitForBatchCompletion();
    }

    private String makeKey(String uuid)
    {
        StringBuffer key = new StringBuffer();

        if (uuid != null && !uuid.isEmpty())
        {
            int start = 0;

            for (int i = 0; i <= 7; i++)
            {
                start = i * 4;
                key.append(uuid.substring(start, start + 4)).append("/");
            }

            key.append(uuid.substring(start + 4));
        }

        return key.toString();
    }

    private String createFile(String mountPoint, String path, File file) throws ApixioFSException
    {
        FileInputStream data = null;

        try
        {
            data = new FileInputStream(file);

            createFile(mountPoint, path, data);
            return path;
  		}
        catch (IOException e)
        {
            logger.error("IO Exception");
            throw new ApixioFSException(e);
  		}
        finally
        {
            if (data != null)
            {
                try
                {
                    data.close();
                }
                catch (IOException e)
                {
                    logger.error("IO Exception");
                    throw new ApixioFSException(e);
                }
            }
        }
    }

    private void createFile(String mountPoint, String path, InputStream data) throws ApixioFSException
    {
        FileOutputStream outputStream = null;

        try
        {
  			File file = new File("/" + mountPoint + "/" + path);

  			File parent = new File(file.getParent());
  			parent.mkdirs();
            file.createNewFile();

            outputStream = new FileOutputStream(file);
            IOUtils.copy(data,outputStream);
            outputStream.getChannel().force(false);
  		}
        catch (IOException e)
        {
            logger.error("IO Exception");
            throw new ApixioFSException(e);
  		}
        finally
        {
            if (outputStream != null)
            {
                try
                {
                    outputStream.close();
                }
                catch (IOException e)
                {

                }
            }
        }
    }

    private InputStream getInputStream(String mountPoint, String path) throws ApixioFSException
    {
        InputStream inputStream = null;

        try
        {
            inputStream = new FileInputStream("/" + mountPoint + "/" + path);
  		}
        catch (FileNotFoundException e)
        {
            logger.error("File Not Found Exception");
            throw new ApixioFSException(e);
  		}

        return inputStream;
    }
}
