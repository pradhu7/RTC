package com.apixio.bms;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.XUUID;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DataStore.InitContext;
import com.apixio.datasource.s3.S3Ops;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.util.Md5DigestInputStream;
import com.apixio.util.S3BucketKey;

/**
 * General Blob use model is for a client/extended system to create a Blob by specifying
 * its metadata+extra, then using that Blob to actually store the blob data (as
 * a byte array).
 *
 * The actual S3 byte[] management is done by creating an s3 prefix/path within the
 * configured bucket name and storing the final FULL URL in the database (e.g.,
 *
 *  s3://apixio-blobbucket/models/blob1234
 *
 * The S3 object name/path is meant to be a useful one (rather than a, say, a UUID)
 * and it is formed by the client-supplied filename and appending the actual blob
 * XUUID for uniqueness (and ability to map back from S3 listing to object)
 *
 * This code also allows for a blob to have a parent blob.  The db schema allows
 * each blob (parent and part) to have both data and metadata, but this manager
 * currently forces the metadata to reside only on the parent and the data blobs
 * to be only on the parts; the more general API could have been done but the
 * current REST api doesn't need/want that.
 *
 *
 * Nomenclature:
 *
 *  "blob":  the top-level thing that's managed; also called "parent"
 *  "meta":  metadata (name=value set) of a blob
 *  "part":  named child of a blob that contains actual data bytes
 */
public class BlobManager
{
    private static final Logger LOG = LoggerFactory.getLogger(BlobManager.class);

    /**
     *
     */
    private MetaStorage   metastore;
    private Schema        schema;
    private S3Ops         s3Ops;
    private String        s3Prefix;  // full path like s3://{bucket}/{prefix}/

    /**
     * BlobManager requires a list of metadata definitions in order to make sure a new
     * Blob conforms to the "schema".
     */
    public BlobManager(DaoServices daos, ConfigSet config, Schema schema)  // metadata is pretty loose definition...
    {
        InitContext ctx = new InitContext(daos, config);

        if ((s3Prefix = config.getString("blobStorageUrl")) == null)
            throw new IllegalStateException("Configuration mcsConfig.blobStorageUrl is missing from main .yaml config");

        if (!s3Prefix.endsWith("/"))
            s3Prefix += "/";

        this.schema = schema;

        metastore = new MariaDbStorage();
        s3Ops     = daos.getS3Ops();

        ctx.initializeAll(metastore);
    }

    /**
     * Create an actual Blob by specifying its meta+extra.  This blob can be used as the
     * parent blob when actually uploading data (in uploadPart()).
     */
    public Blob createBlob(String typePrefix, String createdBy, Metadata md, JSONObject extra1, JSONObject extra2)
    {
        return createBlobWithMd5Hash(typePrefix, createdBy, md, extra1, extra2, null);
    }

    public Blob createBlobWithMd5Hash(String typePrefix, String createdBy, Metadata md, JSONObject extra1, JSONObject extra2, String md5Hash)
    {
        checkMetadataConformance(createdBy, md);

        Blob blob = Blob.newBlob(typePrefix, createdBy, md, extra1, extra2);

        blob.setMd5Digest(md5Hash);

        metastore.createMetadata(blob);

        return blob;
    }

    /**
     * Update metadata+extra on the Blob.  The Blob.metadata and Blob.extra fields are
     * ignored on input and are updated before returning.
     */
    public void updateBlobMeta(Blob blob, boolean replace, Metadata md, boolean lockOwnedIDs, JSONObject extra1, JSONObject extra2)
    {
        if (md != null)
            blob.setMetadata(md);

        if (extra1 != null)
            blob.setExtra1(extra1);

        if (extra2 != null)
            blob.setExtra2(extra2);

        if (lockOwnedIDs)
            metastore.beginTransaction();

        try
        {
            metastore.updateMetadata(blob, replace);

            if (lockOwnedIDs)
            {
                setLogicalIdLocked(getOwned(blob));
                metastore.commitTransaction();
            }
        }
        catch (Throwable t)
        {
            if (lockOwnedIDs)
                metastore.abortTransaction();

            throw new RuntimeException("Failed to update blob metadata", t);
        }
    }

    /**
     * Update extra on the Blob part.
     */
    public boolean updatePartMeta(Blob parent, String partName, JSONObject extra1, JSONObject extra2)
    {
        partName = checkPartName(partName);  // can throw exception

        if ((extra1 == null) && (extra2 == null))
            return false;

        Blob  part = getPart(parent, partName);

        if (part != null)
        {
            if (extra1 != null)
                part.setExtra1(extra1);

            if (extra2 != null)
                part.setExtra2(extra2);

            metastore.updateMetadata(part, false);

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Return the list of Blobs that have the given md5Hash (there *should* never be more than 1, but bugs do
     * happen).  The hash value string must represent the MD5 has in hex format that's compatible with
     * com.apixio.restbase.util.ConversionUtil.toHex()
     */
    public List<Blob> getBlobsWithMd5Hash(String md5Hash)
    {
        return metastore.getByMd5Hash(md5Hash);
    }

    /**
     * Create/update a part with the given name, using another part's blob data (as findable by the given md5 hash).
     * MimeType is also taken from the referenced blob.  If no existing part has the given MD5 hash then
     * null is returned.
     */
    private Blob putPartByMd5(Blob parent, String partName, String uploadedBy, String md5Hash) throws IOException
    {
        partName = checkPartName(partName);  // can throw exception

        checkEmpty(uploadedBy, "UploadedBy can't be empty");
        checkEmpty(md5Hash,    "md5Hash can't be empty");

        List<Blob>  srcs   = metastore.getByMd5Hash(md5Hash);
        Blob        part   = null;

        if (srcs.size() > 0)
        {
            Blob    src = srcs.get(0);
            boolean isNew;

            part = getPart(parent, partName);

            if (part == null)
            {
                part  = Blob.newPart(parent, uploadedBy, partName);
                isNew = true;
            }
            else
            {
                isNew = false;
            }

            part.setStoragePath(src.getStoragePath());
            part.setMimeType(src.getMimeType());
            part.setMd5Digest(src.getMd5Digest());

            if (isNew)
                metastore.createMetadata(part);
            else
                metastore.updateMetadata(part, true);
        }

        return part;  // null is returned if part put/add was valid
    }

    /**
     * Uploads the given actual blob data to S3.  This operation is really an upsert as
     * multiple uploads to the same part are supported (any such restrictions are
     * dealt with by the next level up).
     *
     * Created/updated part blob will be returned.  All failures to add/update a part
     * result in an exception.
     */
    public Blob uploadPart(Blob parent, String partName, String uploadedBy, String mimeType, InputStream data) throws IOException
    {
        partName = checkPartName(partName);   // can throw exception

        checkEmpty(uploadedBy, "UploadedBy can't be empty");

        Blob                 part     = getPart(parent, partName);
        String               fullPath = s3Prefix + parent.getUuid() + "/" + partName;  // format should help browsing/debugging
        S3BucketKey          bk       = new S3BucketKey(fullPath);
        Md5DigestInputStream dis      = Md5DigestInputStream.wrapInputStream(data);
        List<Blob>           md5Matches;
        String               md5Hex;
        boolean              isNew;

        if (part == null)
        {
            part  = Blob.newPart(parent, uploadedBy, partName);
            isNew = true;
        }
        else
        {
            isNew = false;
        }

        // we want to calculate md5 hash to facilitate download-only-if-modified
        // so we have to wrap the stream, etc.

        s3Ops.addObject(bk.getBucket(), bk.getKey(), dis);

        md5Hex = dis.getMd5AsHex();

        md5Matches = metastore.getByMd5Hash(md5Hex);

        if (md5Matches.size() > 0)
            fullPath = md5Matches.get(0).getStoragePath();

        if (!fullPath.equals(part.getStoragePath()))
            part.setStoragePath(fullPath);

        if (!md5Hex.equals(part.getMd5Digest()))
            part.setMd5Digest(md5Hex);

        if (mimeType != null)
            part.setMimeType(mimeType);

        if (isNew)
            metastore.createMetadata(part);
        else
            metastore.updateMetadata(part, true);

        return part;
    }

    /**
     * Generalized adding of a part:  uses PartSource as the type and source spec for the
     * contents of the part blob.  Two types of sources are supported:  matching by md5
     * hash of an existing blob, and reference to an S3 object.
     *
     * All exceptions are passed up so caller can know details of failure.  Null is returned
     * if there weren't any explicit error
     */
    public Blob uploadPart(Blob parent, PartSource partInfo, String uploadedBy) throws IOException
    {
        partInfo.name = checkPartName(partInfo.name);  // can throw exception

        checkEmpty(uploadedBy,      "UploadedBy can't be empty");
        checkEmpty(partInfo.source, "Part source can't be empty");

        if (partInfo.source.startsWith(PartSource.SOURCE_MD5))
        {
            return putPartByMd5(parent, partInfo.name, uploadedBy, partInfo.source.substring(PartSource.SOURCE_MD5.length()));
        }
        else if (partInfo.source.startsWith(PartSource.SOURCE_S3))
        {
            S3BucketKey bk = new S3BucketKey(partInfo.source);

            // important note: there is NO S3Ops method that allows quick checking on the
            // existence of an S3 object (including bucket)--all of them retry (sleeping
            // between) so we might as well just let it try to get the object.

            try (InputStream data = s3Ops.getObject(bk.getBucket(), bk.getKey()))
            {
                return uploadPart(parent, partInfo.name, uploadedBy, partInfo.mimeType, data);
            }
        }
        else
        {
            throw new IllegalArgumentException("Unknown Part source type:  " + partInfo.source);
        }
    }

    /**
     * Looks for the part with the given name within the given (parent) blob.
     * null is returned if there is no such part.  Blob should have been retrieved
     * via getBlobMeta()
     */
    public Blob getPart(Blob parent, String partName)
    {
        return metastore.getByPartName(schema, parent, partName);
    }

    public List<Blob> getParts(Blob parent)
    {
        return metastore.getParts(parent);
    }

    /**
     * An odd method, to be sure.  The need is to be able to search for blobs (which will be
     * parts, since only parts can have uploaded data) whose md5 hash value matches what's
     * passed in.  The resulting set of (part) blobs are then augmented with their parent
     * blobs and then returned.  The returned data is a map from the part to its parent as
     * it's possible for a single parent blob to have multiple children parts with the same
     * md5 hash (unlikely, but nothing disallows that).
     */
    public Map<Blob, Blob> getPartsParentsByMd5(String md5)
    {
        return metastore.getPartParents(metastore.getByMd5Hash(md5));
    }

    /**
     * Opens up a stream to the actual blob data from S3 and returns an InputStream to it
     * that the client can use to read the data.  If blob data has never been uploaded for
     * the Blob then a null is returned
     */
    public InputStream getPartData(Blob blob) throws IOException
    {
        String fullPath = blob.getStoragePath();

        if (fullPath != null)
        {
            S3BucketKey bk = new S3BucketKey(fullPath);

            return s3Ops.getObject(bk.getBucket(), bk.getKey());
        }
        else
        {
            return null;
        }
    }

    /**
     * Reads in the give blob
     */
    public Blob getBlobMeta(XUUID id)
    {
        return metastore.getByBlobID(schema, id);
    }

    /**
     *
     */
    public boolean deletePart(Blob parent, String name)
    {
        List<String> s3Paths = metastore.deletePart(parent, name);

        deleteS3Data(s3Paths);

        return (s3Paths != null);
    }
    
    public void deleteBlob(Blob blob)
    {
        List<String> s3Paths = metastore.deleteBlob(blob);

        deleteS3Data(s3Paths);
    }

    private void deleteS3Data(List<String> s3Paths)
    {
        for (String s3Obj : s3Paths)
        {
            try
            {
                S3BucketKey bk = new S3BucketKey(s3Obj);

                LOG.error("Removing S3 object " + s3Obj);

                s3Ops.removeObject(bk.getBucket(), bk.getKey());
            }
            catch (IOException x)
            {
                LOG.error("Failed to remove s3 object " + s3Obj, x);
            }
        }
    }

    /**
     * Performs a general query on all blob metadata and returns all matching Blobs.
     * See Query for what can be queried for/on.
     */
    public List<Blob> queryBlobMeta(Query q)
    {
        return metastore.queryMatching(schema, q);
    }

    /**
     * Inter-blob dependency management
     */
    public void setOwner(Blob blob, List<String> logicalIDs)
    {
        metastore.setOwner(blob, logicalIDs);
    }

    public Blob getOwner(String logicalID)
    {
        return metastore.getOwner(schema, logicalID);
    }

    public List<String> getOwned(Blob blob)
    {
        return metastore.getOwned(blob);
    }

    public void addDependencies(String leftID, List<String> rightIDs, boolean replace)
    {
        metastore.addDependencies(leftID, rightIDs, replace);
    }

    //?? temporary?  assumes the most useful convention is physical owns at most 1 logicalID
    public List<Blob> getPhysicalDependencies(String logicalID, Relationship rel)
    {
        return metastore.getPhysicalDependencies(schema, logicalID, rel);
    }

    public List<String> getLogicalDependencies(String logicalID, Relationship rel)
    {
        return metastore.getLogicalDependencies(logicalID, rel);
    }

    public void setLogicalIdLocked(List<String> leftIDs)
    {
        metastore.setLogicalIdLocked(leftIDs);
    }

    public boolean isLogicalIdLocked(String logicalID)
    {
        return metastore.isLogicalIdLocked(logicalID);
    }

    /**
     * Makes sure that the supplied (meta)data meets the required and type constraints
     * as declared as Metadata.  The algorithm is to make sure that all required metadata
     * exists and is of the right type, and to make sure that all optional are of the right
     * type if the values are actually present.
     */
    private void checkMetadataConformance(String createdBy, Metadata md)
    {
        if (schema != null)
        {
            for (MetadataDef mdd : schema.metadatas())
                mdd.checkValue(md.get(mdd));
        }
    }

    private String checkPartName(String name)
    {
        if ((name = name.trim()).indexOf('/') != -1)
            throw new IllegalArgumentException("Partname can't contain '/': " + name + " as it confuses S3 queries");

        return name;
    }

    private static void checkEmpty(String s, String error)
    {
        if (empty(s))
            throw new IllegalArgumentException(error);
    }

    private static boolean empty(String s)
    {
        return (s == null) || (s.length() == 0);
    }

}
