package com.apixio.dao.apxdata;

import java.util.Date;
import java.util.EnumSet;

/**
 * Entity that contains metadata for a single groupingID+partitionID row in blob storage.
 */
class ApxMeta
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
     */
    public enum Fields
    {
        // for easier updates (i.e., allows auto creation of WHERE clause)
        ID,

        // for insert only (these fields are not updateable):
        PROTOCLASS, GROUPING_ID, PARTITION_ID, CREATED_AT,

        // for insert and update:
        COUNT, TOTAL_SIZE
    }

    private EnumSet<Fields> modified = EnumSet.noneOf(Fields.class);

    /**
     * Persisted fields
     */
    private int    dbID;
    private String protoClass;
    private String groupingID;
    private String partitionID;
    private Date   createdAt;
    private int    count;
    private int    totalSize;

    private ApxMeta(int dbID, String protoClass, String groupingID, String partitionID, Date createdAt,
                        Integer count, Integer totalSize)
    {
        this.dbID        = dbID;
        this.protoClass  = protoClass;
        this.groupingID  = groupingID;
        this.partitionID = partitionID;;
        this.createdAt   = createdAt;

        this.count      = (count != null) ? count : 0;
        this.totalSize  = (totalSize != null) ? totalSize : 0;
    }

    /**
     * For reconstruction from persisted form
     */
    static ApxMeta fromPersisted(int dbID, String protoClass, String groupingID, String partitionID, Date createdAt,
                                 Integer count, Integer totalSize)
    {
        return new ApxMeta(dbID, protoClass, groupingID, partitionID, createdAt, count, totalSize);
    }

    /**
     * for client to really create a new Meta instance
     */
    static ApxMeta newBlobMeta(String protoClass, String groupingID, String partitionID, Integer count, Integer totalSize)
    {
        return new ApxMeta(NOT_PERSISTED, protoClass, groupingID, partitionID, new Date(), count, totalSize);
    }

    /**
     * Setters
     */
    void setDbID(int id)  // NOT public as only MariaDbStorage should set
    {
        this.dbID = id;
    }

    public void setCount(int count)
    {
        modified.add(Fields.COUNT);
        this.count = count;
    }

    public void setTotalSize(int totalSize)
    {
        modified.add(Fields.TOTAL_SIZE);
        this.totalSize = totalSize;
    }

    /**
     * Getters
     */
    public int getDbID()
    {
        return dbID;
    }

    public String getProtoClass()
    {
        return protoClass;
    }

    public boolean isPersisted()
    {
        return NOT_PERSISTED != dbID;
    }

    public String getGroupingID()
    {
        return groupingID;
    }

    public String getPartitionID()
    {
        return partitionID;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public int getCount()
    {
        return count;
    }

    public int getTotalSize()
    {
        return totalSize;
    }

    /**
     *
     */
    public EnumSet<Fields> getModified()
    {
        return modified;
    }

}
