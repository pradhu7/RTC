package com.apixio.dao.apxdata;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.protobuf.Message;

/**
 * DataType is a light manager-like wrapper over serialization and deserialization,
 * both of which are performed by Google Protobuf-generated code (Message is
 * the base protobuf-generated class that implements (de)serialize).
 *
 * In general this class shouldn't need to be extended as most of the common
 * info that must be per-type can be specified in the constructor.
 */
public class DataType<T extends Message>
{
    /**
     * dataClass is the leaf-level protoc-created Java class that is used to restore
     * from a protobuf byte[].
     */
    private Class<T> dataClass;

    /**
     * dataName is a useful/unique ID used for logging
     */
    private String dataName;

    /**
     * storageID must be unique across datatypes; it should be a short string that
     * is used to prevent Cassandra rowkey conflicts across datatypes for a given
     * groupingID.
     */
    private String storageID;

    /**
     * This is the first byte of the packed byte array and identifies the format
     * and compression/encryption state of the data.  This tag info is per-DataType,
     * meaning that format/type info bits don't need to be unique across data types.
     * It must NEVER change (or already existing data will be inaccessible).
     */
    private int tagInfo;

    /**
     * If true then encryption is forced, regardless of what the client wants.
     */
    private boolean canContainPHI;

    /**
     * Restoring from byte[] requires a call to the "parseFrom" method on the dataClass
     * class (static method); this Method instance is retrieved via runtime reflection.
     */
    private Method parseFrom;

    /**
     * Constructs new DataType with the given attributes
     */
    public DataType(Class<T> dataClass, String dataName, String storageID, int tagInfo)
    {
        notNull(dataClass, "Class<T> dataclass");
        notNull(dataName,  "String dataname");
        notNull(storageID, "String storageID");

        this.dataClass = dataClass;
        this.dataName  = dataName;
        this.storageID = storageID;
        this.tagInfo   = tagInfo;

        getParseFromMethod();
    }

    /**
     *
     */
    public DataType<T> setCanContainPHI(boolean canContain)
    {
        this.canContainPHI = canContain;

        return this;
    }

    /**
     *
     */
    public Class<T> getDataClass()
    {
        return dataClass;
    }

    public String getName()
    {
        return dataName;
    }

    /**
     * Returns unique and never-changing short-ish storage ID.  This will form part of the
     * Cassandra rowkey.
     */
    public String  getStorageID()
    {
        return storageID;
    }

    /**
     * 
     */
    public boolean canContainPHI()
    {
        return canContainPHI;
    }

    /**
     * Return base tagging byte for the datatype.  
     */
    public int getTagInfo()
    {
        // note that the FLAG_ZIPPED here is dealt with differently than it is
        // with signal data because prediction JSON strings are large enough
        // already to have effective zip compression (whereas signal data
        // might not be, so in that case we pack the byte arrays and then
        // zip)

        return tagInfo | ChunkUtil.TAGDATA_FLAG_ZIPPED;
    }

    /**
     * These are generic methods that use the normal (de)serialization methods on
     * protoc-generated classes.
     */
    public byte[] serialize(T data) throws IOException
    {
        return data.toByteArray();
    }

    public T deserialize(byte[] serialized) throws IOException
    {
        try
        {
            return (T) parseFrom.invoke(null, serialized);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x)
        {
            throw new IOException("Failed to invoke 'parseFrom' method", x);
        }
    }

    /**
     *
     */
    private void getParseFromMethod() throws UnsupportedOperationException
    {
        // all protobuf classes /should/ have a parseFrom(byte[]) that we can use

        try
        {
            parseFrom = dataClass.getMethod("parseFrom", byte[].class);
        }
        catch (NoSuchMethodException | SecurityException x)
        {
            throw new UnsupportedOperationException("Failed to get 'parseFrom' method", x);
        }
    }

    private void notNull(Object o, String name)
    {
        if (o == null)
            throw new IllegalArgumentException("Parameter " + name + " can't be null");
    }

}
