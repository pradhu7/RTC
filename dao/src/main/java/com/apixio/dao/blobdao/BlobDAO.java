package com.apixio.dao.blobdao;

import java.io.BufferedInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.apixio.model.blob.BlobType;
import com.apixio.security.DecryptedInputStream;
import com.apixio.security.EncryptedInputStream;
import com.apixio.dao.storage.ApixioStorage;
import com.apixio.security.Security;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import org.apache.commons.io.IOUtils;


public class BlobDAO
{
    private Security      security;
    private ApixioStorage apixioStorage;

    public BlobDAO()
    {
        this.security            = Security.getInstance();
    }

    public void setApixioStorage(ApixioStorage apixioStorage)
    {
        this.apixioStorage = apixioStorage;
    }

    /**
     * Given a blobType, blob & its length, and org that the blob belongs to,
     * it saves the blob to persistent storage
     *
     * @param blobType
     * @param data
     * @param orgId
     *
     * @return
     * @throws Exception
     */
    public String write(BlobType blobType, InputStream data, String orgId, boolean async) throws Exception
    {
        String id = blobType.getID();

        InputStream encryptedData = null;

        try
        {
            encryptedData = EncryptedInputStream.encryptInputStreamWithScope(security, data, PatientDataSetLogic.computeScope(orgId));
            apixioStorage.write(id, encryptedData, orgId, async);
        }
        finally
        {
            IOUtils.closeQuietly(encryptedData);
        }

        return id;
    }

    /**
     * Given a blobType and pds that the blob belongs to,
     * returns the length of the blob from persistent storage
     *
     * @param blobType
     * @param pdsId
     *
     * @return blob length (aka byte size)
     * @throws Exception
     */
    public long getBlobSize(BlobType blobType, String pdsId) throws Exception
    {
        String id = blobType.getID();
        return apixioStorage.readSize(id, pdsId);
    }

    /**
     * Given a blobType and org that the blob belongs to,
     * it reads the blob from persistent storage
     *
     * @param blobType
     * @param orgId
     *
     * @return input stream
     * @throws Exception
     */
    public InputStream read(BlobType blobType, String orgId) throws java.lang.Exception
    {
        String id = blobType.getID();
        boolean success = false;

        InputStream in = null;
        InputStream inputStream = null;

        //Careful! apixioStorage.read returns a S3 object from the object pool, if not released it will
        //         cause pool exhaustion, we must clean up even in error conditions...
        try
        {
            in = apixioStorage.read(id, orgId);
            if (in != null)
            {
                inputStream = new DecryptedInputStream(new BufferedInputStream(in), security);
            }

            success = true;
            return inputStream;
        }
        finally
        {
            if (!success)
            {
                IOUtils.closeQuietly(in);  //closing inputStream will close in, but just to be sure :-)
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    /**
     * Given a blobType/model and org, it returns all the blob types that match the model
     *
     * @param model
     * @param orgId
     *
     * @return blob types
     * @throws Exception
     */
    public List<BlobType> getBlobTypes(BlobType model, String orgId) throws Exception
    {
        return getBlobTypesGuts(model.getModelID(), orgId);
    }

    /**
     * Given a docUUID and org, it returns all the blob types for the blobs that belong
     * to the docUUID.
     *
     * @param docUUID
     * @param orgId
     *
     * @return blob types
     * @throws Exception
     */
    public List<BlobType> getBlobTypes(UUID docUUID, String orgId) throws Exception
    {
        return getBlobTypesGuts(BlobType.makeModelDocUUID(docUUID.toString()), orgId);
    }

    private List<BlobType> getBlobTypesGuts(String keyPrefix, String orgId) throws Exception
    {
        List<String> keys = apixioStorage.getKeys(keyPrefix, orgId);

        List<BlobType> types = new ArrayList<>();

        for (String key : keys)
        {
            types.add(BlobType.getBlobData(key));
        }

        return types;
    }

    public void waitForAllCompletion()
    {
        apixioStorage.waitForAllCompletion();
    }

    public void waitForBatchCompletion()
    {
        apixioStorage.waitForBatchCompletion();
    }
}
