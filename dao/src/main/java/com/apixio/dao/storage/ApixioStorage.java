package com.apixio.dao.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.datasource.afs.ApixioFS;
import com.apixio.datasource.afs.Mount;

/**
 *
 * Perform ops on Apixio File System.
 */

public class ApixioStorage
{
    private final static Logger logger = LoggerFactory.getLogger(ApixioStorage.class);

    private ApixioFS apixioFS;

    private String mountPoint;
    private String fromStorageType;
    private String[] toStorageTypes;  // a comma separated list of storage types

    private CustomerProperties customerProperties;

    public void setApixioFS(ApixioFS apixioFS)
    {
        this.apixioFS = apixioFS;
    }

    public void setMountPoint(String mountPoint)
    {
        this.mountPoint = mountPoint;
    }

    public void setFromStorageType(String fromStorageType)
    {
        this.fromStorageType = fromStorageType;
    }

    public void setToStorageTypes(String toStorageTypes)
    {
        this.toStorageTypes = toStorageTypes.split(",");
    }

    public void setCustomerProperties(CustomerProperties customerProperties)
    {
    	this.customerProperties = customerProperties;
    }

    /**
     * Given an object uuid, stream, and org that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes.
     *
     * @param uuid
     * @param data
     * @param orgId
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String write(UUID uuid, InputStream data, String orgId, boolean async) throws Exception
    {
        return write(uuid.toString(), data, orgId, async);
    }

    /**
     * Given an object uuid, stream, and org that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes.
     *
     * @param uuid
     * @param data
     * @param orgId
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String write(String uuid, InputStream data, String orgId, boolean async) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        return writeGuts(uuid, data, prefix, async);
    }

    /**
     * Given an object uuid, stream, and predefined prefix (s3 bucket etc.) that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes
     *
     * @param uuid
     * @param data
     * @param prefix
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String writeUsingPrefix(UUID uuid, InputStream data, String prefix, boolean async) throws Exception
    {
        return writeGuts(uuid.toString(), data, prefix, async);
    }


    /**
     * Given an object uuid, stream, and predefined prefix (s3 bucket etc.) that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes
     *
     * @param uuid
     * @param data
     * @param prefix
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String writeUsingPrefix(String uuid, InputStream data, String prefix, boolean async) throws Exception
    {
        return writeGuts(uuid, data, prefix, async);
    }

    private String writeGuts(String uuid, InputStream data, String prefix, boolean async) throws Exception
    {
        List<Mount> mounts = new ArrayList<>();

        for (int i = 0; i < toStorageTypes.length; i++)
        {
            Mount mount = new Mount(mountPoint, prefix, Mount.Type.valueOf(toStorageTypes[i]));
            mounts.add(mount);
        }

        String key = apixioFS.create(mounts, uuid, data, async);

        return key;
    }
    /**
     * Given an object uuid, file, and org that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes
     *
     * @param uuid
     * @param file
     * @param orgId
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String write(UUID uuid, File file, String orgId, boolean async) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        return writeGuts(uuid.toString(), file, prefix, async);
    }

    /**
     * Given an object uuid, file, and org that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes
     *
     * @param uuid
     * @param file
     * @param orgId
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String write(String uuid, File file, String orgId, boolean async) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        return writeGuts(uuid, file, prefix, async);
    }

    /**
     * Given an object uuid, file, and prefix (s3 bucket) that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes
     *
     * @param uuid
     * @param file
     * @param prefix
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String writeUsingPrefix(UUID uuid, File file, String prefix, boolean async) throws Exception
    {
        return writeGuts(uuid.toString(), file, prefix, async);
    }

    /**
     * Given an object uuid, file, and prefix (s3 bucket) that the data belongs to,
     * it writes the data to all the storage types defined toStorageTypes
     *
     * @param uuid
     * @param file
     * @param prefix
     * @param async
     *
     * @return key of the written object
     * @throws IOException
     */
    public String writeUsingPrefix(String uuid, File file, String prefix, boolean async) throws IOException
    {
        return writeGuts(uuid, file, prefix, async);
    }

    private String writeGuts(String uuid, File file, String prefix, boolean async) throws IOException
    {
        List<Mount> mounts = new ArrayList<>();

        for (int i = 0; i < toStorageTypes.length; i++)
        {
            Mount mount = new Mount(mountPoint, prefix, Mount.Type.valueOf(toStorageTypes[i]));
            mounts.add(mount);
        }

        String key = apixioFS.create(mounts, uuid, file, async);

        return key;
    }

    /**
     * Given an object uuid and orgId, it returns a list of uris
     *
     * @param uuid
     * @param orgId
     *
     * @return list of uris
     *
     * @throws Exception
     */
    List<String> getURIs(UUID uuid, String orgId) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        return getURIsGuts(uuid.toString(), prefix);
    }

    /**
     * Given an object uuid and orgId, it returns a list of uris
     *
     * @param uuid
     * @param orgId
     *
     * @return list of uris
     *
     * @throws Exception
     */
    List<String> getURIs(String uuid, String orgId) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        return getURIsGuts(uuid, prefix);
    }

    /**
     * Given an object uuid and orgId, it returns a list of uris
     *
     * @param uuid
     * @param prefix
     *
     * @return list of uris
     *
     * @throws Exception
     */
    List<String> getURIsUsingPrefix(UUID uuid, String prefix) throws Exception
    {
        return getURIsGuts(uuid.toString(), prefix);
    }

    /**
     * Given an object uuid and orgId, it returns a list of uris
     *
     * @param uuid
     * @param prefix
     *
     * @return list of uris
     *
     * @throws Exception
     */
    List<String> getURIsUsingPrefix(String uuid, String prefix) throws Exception
    {
        return getURIsGuts(uuid, prefix);
    }

    List<String> getURIsGuts(String uuid, String prefix) throws Exception
    {
        List<String> uris = new ArrayList<>();

        for (int i = 0; i < toStorageTypes.length; i++)
        {
            Mount mount = new Mount(mountPoint, prefix, Mount.Type.valueOf(toStorageTypes[i]));

            uris.add(apixioFS.getURI(mount, uuid));
        }

        return uris;
    }


    /**
     * Given an object uuid, and org that the data belongs to,
     * it checks whether the object corresponding to the uuid exists in
     * the storage system
     *
     * @param uuid
     * @param orgId
     *
     * @return boolean
     * @throws IOException
     */
    public boolean fileExists(UUID uuid, String orgId) throws Exception
    {
    	String prefix = customerProperties.getAFSFolder(orgId);

        return fileExistsGuts(uuid.toString(), prefix);
    }

    /**
     * Given an object uuid, and org that the data belongs to,
     * it checks whether the object corresponding to the uuid exists in
     * the storage system
     *
     * @param uuid
     * @param orgId
     *
     * @return boolean
     * @throws IOException
     */
    public boolean fileExists(String uuid, String orgId) throws Exception
    {
    	String prefix = customerProperties.getAFSFolder(orgId);

        return fileExistsGuts(uuid, prefix);
    }

    /**
     * Given an object uuid, and prefix (s3 bucket) that the data belongs to,
     * it checks whether the object corresponding to the uuid exists in
     * the storage system
     *
     * @param uuid
     * @param prefix
     *
     * @return boolean
     * @throws IOException
     */
    public boolean fileExistsUsingPrefix(UUID uuid, String prefix) throws Exception
    {
        return fileExistsGuts(uuid.toString(), prefix);
    }

    /**
     * Given an object uuid, and prefix (s3 bucket) that the data belongs to,
     * it checks whether the object corresponding to the uuid exists in
     * the storage system
     *
     * @param uuid
     * @param prefix
     *
     * @return boolean
     * @throws IOException
     */
    public boolean fileExistsUsingPrefix(String uuid, String prefix) throws Exception
    {
        return fileExistsGuts(uuid, prefix);
    }

    private boolean fileExistsGuts(String uuid, String prefix) throws Exception
    {
    	Mount inMount = new Mount(mountPoint, prefix, Mount.Type.valueOf(fromStorageType));

    	return apixioFS.fileExists(inMount, uuid);
    }

    /**
     * Given a key prefix, and org that the key prefix belongs to,
     * it returns all the keys that match the key prefix
     *
     * @param keyPrefix
     * @param orgId
     *
     * @return list of keys
     * @throws IOException
     */
    public List<String> getKeys(String keyPrefix, String orgId) throws Exception
    {
    	String prefix = customerProperties.getAFSFolder(orgId);

        return getKeysUsingPrefix(keyPrefix, prefix);
    }

    /**
     * Given a key prefix, and prefix (s3 bucket) that the key prefix belongs to,
     * it returns all the keys that match the key prefix
     *
     * @param keyPrefix
     * @param prefix
     *
     * @return list of keys
     * @throws IOException
     */
    public List<String> getKeysUsingPrefix(String keyPrefix, String prefix) throws Exception
    {
    	Mount inMount = new Mount(mountPoint, prefix, Mount.Type.valueOf(fromStorageType));

    	return apixioFS.getKeys(inMount, keyPrefix);
    }

    //TODO rework on this. This method is needed for Migration
    public String copy(String fromKey, UUID uuid, String orgId) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        List<Mount> toMounts = new ArrayList<Mount>();

        Mount fromMount = new Mount(mountPoint, prefix, Mount.Type.valueOf(fromStorageType));

        for (int i = 0; i < toStorageTypes.length; i++)
        {
            Mount mount = new Mount(mountPoint, prefix, Mount.Type.valueOf(toStorageTypes[i]));
            toMounts.add(mount);
        }

        return apixioFS.copy(fromMount, toMounts, fromKey, uuid.toString());
    }

    /**
     * Given an object uuid and pds that belongs to,
     * it fetches size from storage type defined in fromStorageType
     *
     * @param uuid
     * @parm pdsId
     *
     * @return object length
     */
    public long readSize(String uuid, String pdsId) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(pdsId);
        return readSizeGuts(uuid, prefix);
    }


    /**
     * Given an object uuid and org that the data belongs to,
     * it fetches the data from the storage type defined in fromStorageType
     *
     * @param uuid
     * @param orgId
     *
     * @return key of the written object
     * @throws IOException
     */
    public InputStream read(UUID uuid, String orgId) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        return readGuts(uuid.toString(), prefix);
    }

    /**
     * Given an object uuid and org that the data belongs to,
     * it fetches the data from the storage type defined in fromStorageType
     *
     * @param uuid
     * @param orgId
     *
     * @return key of the written object
     * @throws IOException
     */
    public InputStream read(String uuid, String orgId) throws Exception
    {
        String prefix = customerProperties.getAFSFolder(orgId);

        return readGuts(uuid, prefix);
    }

    /**
     * Given an object uuid and prefix (s3 bucket) that the data belongs to,
     * it fetches the data from the storage type defined in fromStorageType
     *
     * @param uuid
     * @param prefix
     *
     * @return key of the written object
     * @throws IOException
     */
    public InputStream readUsingPrefix(UUID uuid, String prefix) throws IOException
    {
        return readGuts(uuid.toString(), prefix);
    }

    /**
     * Given an object uuid and prefix (s3 bucket) that the data belongs to,
     * it fetches the data from the storage type defined in fromStorageType
     *
     * @param uuid
     * @param prefix
     *
     * @return key of the written object
     * @throws IOException
     */
    public InputStream readUsingPrefix(String uuid, String prefix) throws IOException
    {
        return readGuts(uuid, prefix);
    }

    private InputStream readGuts(String uuid, String prefix) throws IOException
    {
        Mount mount = new Mount(mountPoint, prefix, Mount.Type.valueOf(fromStorageType));

        return apixioFS.getInputStream(mount, uuid);
    }

    private long readSizeGuts(String uuid, String prefix) throws IOException
    {
        Mount mount = new Mount(mountPoint, prefix, Mount.Type.valueOf(fromStorageType));

        return apixioFS.fileSize(mount, uuid);
    }

    public void waitForAllCompletion()
    {
        apixioFS.waitForAllCompletion();
    }

    public void waitForBatchCompletion()
    {
        apixioFS.waitForBatchCompletion();
    }

    private void setURIs(String key, List<Mount> mounts, List<String> uris)
    {
        for(Mount mount: mounts)
        {
            uris.add(apixioFS.getURI(mount, key));
        }
    }
}
