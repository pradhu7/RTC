package com.apixio.bms;

import java.util.Date;
import java.util.EnumSet;

import org.json.JSONObject;

import com.apixio.XUUID;

/**
 * Blobs are basically metadata and (oddly enough) an *optional* byte array of data.
 * The metadata is stored in an RDBMS while the actual blob data is stored as an S3
 * object.
 *
 * Blobs are intended to be wrapped (in a loose sense) by an application-meaningful
 * way where the system doing this wrapping/extending defines allowed metadata to
 * be stored along with the blob data.  All blobs within that system context have to
 * have the (required) metadata.
 *
 * The client of the system (that extends/wraps blobs) can also define client-specific
 * data, expressed as a JSON object.
 */
public class Blob
{
    public static int NOT_PERSISTED = -1;

    /**
     * Modifiable fields that need "dirty bit" tracking for purposes of
     * minimizing SQL update statements (to avoid race conditions of SQL updates).
     *
     * The model requires that the POJO (this class) define an enum that
     * includes the modifiable fields and keep track of modifications to field
     * values and to be able to return the set of modified fields in an
     * EnumSet.
     *
     * Note that metadata is not a simple field (it has its own table) so it
     * not a "field" in this narrow use here.
     */
    public enum Fields {
        // for easier updates (i.e., allows auto creation of WHERE clause)
        ID,

        // for insert only (these fields are not updateable):
        UUID, CREATEDAT, CREATEDBY, PARENT, PARTNAME,

        // for insert and update:
        SOFTDELETED, EXTRA1, EXTRA2, STORAGEPATH, MD5DIGEST, MIMETYPE,

        // for modifications tracking only (i.e., the DbTable methods won't deal with these):
        METADATA
    }

    private EnumSet<Fields> modified = EnumSet.noneOf(Fields.class);

    /**
     * Data managed by blob management system
     */
    private int        dbID;          // as assigned by RDBMS
    private Date       createdAt;
    private String     createdBy;
    private boolean    softDeleted;

    // non-null for parent:
    private XUUID      id;

    // non-null for parts:
    private int        parentID;
    private Blob       parent;
    private String     partName;

    // only for parts with actual data uploaded:
    private String     storagePath;   // full s3://bucket/path URL
    private String     md5Digest;
    private String     mimeType;

    /**
     * Data managed by either a "wrapper" system (i.e., the system that uses
     * blob management and adds its own semantics) or a client of the wrapper
     * system.
     *
     * "metadata" contents are defined by the wrapper systems
     * "extra" is defined by the client of wrapper system
     */
    private Metadata   metadata;
    private JSONObject extra1;
    private JSONObject extra2;

    private Blob(int dbID, Date createdAt, String createdBy, boolean softDeleted,
                 XUUID id, Integer parentID, Blob parent, String partName,
                 String stgPath, String md5Digest, String mimeType,
                 Metadata metadata, JSONObject extra1, JSONObject extra2)
    {
        this.dbID        = dbID;
        this.createdAt   = createdAt;
        this.createdBy   = createdBy;
        this.softDeleted = softDeleted;

        this.id          = id;

        this.parentID    = (parentID != null) ? parentID : 0;
        this.parent      = parent;
        this.partName    = partName;

        this.storagePath = stgPath;
        this.md5Digest   = md5Digest;
        this.mimeType    = mimeType;

        this.metadata    = metadata;
        this.extra1      = extra1;
        this.extra2      = extra2;
    }

    /**
     * For reconstruction from persisted form
     */
    static Blob fromPersisted(int dbID, Date createdAt, String createdBy, boolean softDeleted,
                              XUUID id, Integer parentID, Blob parent, String partName,
                              String stgPath, String md5Digest, String mimeType,
                              JSONObject extra1, JSONObject extra2)
    {
        return new Blob(dbID, createdAt, createdBy, softDeleted,
                        id, parentID, parent, partName,
                        stgPath, md5Digest, mimeType,
                        null, extra1, extra2);
    }

    /**
     * for client to really create a new Blob instance
     */
    public static Blob newBlob(String typePrefix, String createdBy, Metadata metadata, JSONObject extra1, JSONObject extra2)
    {
        return new Blob(NOT_PERSISTED, new Date(), createdBy, false,
                        XUUID.create(typePrefix), null, null, null,
                        null, null, null,
                        metadata, extra1, extra2);
    }

    public static Blob newPart(Blob parent, String createdBy, String partName)
    {
        return new Blob(NOT_PERSISTED, new Date(), createdBy, false,
                        null, parent.getDbID(), parent, partName,
                        null, null, null,
                        null, null, null);
    }

    /**
     * Setters
     */
    void setDbID(int id)  // NOT public as only MariaDbStorage should set
    {
        this.dbID = id;
    }

    int getRawParentID()
    {
        return parentID;
    }

    /**
     * Special form used during reading that doesn't set modified bits
     */
    static void setMetadata(Blob blob, Metadata md)
    {
        blob.metadata = md;
    }

    public void setMetadata(Metadata md)
    {
        modified.add(Fields.METADATA);
        metadata = md;
    }

    public void setSoftDeleted(boolean deleted)
    {
        modified.add(Fields.SOFTDELETED);
        softDeleted = deleted;
    }

    public void setStoragePath(String path)
    {
        modified.add(Fields.STORAGEPATH);
        storagePath = path;
    }

    public void setMd5Digest(String md5)
    {
        modified.add(Fields.MD5DIGEST);
        md5Digest = md5;
    }

    public void setMimeType(String type)
    {
        modified.add(Fields.MIMETYPE);
        mimeType = type;
    }

    public void setExtra1(JSONObject jo)
    {
        modified.add(Fields.EXTRA1);
        extra1 = jo;
    }

    public void setExtra2(JSONObject jo)
    {
        modified.add(Fields.EXTRA2);
        extra2 = jo;
    }

    /**
     * Getters
     */
    public int getDbID()
    {
        return dbID;
    }

    public boolean isPersisted()
    {
        return NOT_PERSISTED != dbID;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public String getCreatedBy()
    {
        return createdBy;
    }

    public XUUID getUuid()
    {
        return id;
    }

    public Integer getParentDbID()
    {
        return (parent != null) ? parent.getDbID() : null;
    }

    public String getPartName()
    {
        return partName;
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    public Boolean getSoftDeleted()
    {
        return Boolean.valueOf(softDeleted);
    }

    public String getStoragePath()
    {
        return storagePath;
    }

    public String getMd5Digest()
    {
        return md5Digest;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public JSONObject getExtra1()
    {
        return extra1;
    }

    public JSONObject getExtra2()
    {
        return extra2;
    }

    /**
     *
     */
    public EnumSet<Fields> getModified()
    {
        return modified;
    }

    // debug
    @Override
    public String toString()
    {
        return "[Blob: dbid=" + dbID + "; uuid=" + id + "]";
    }

}
