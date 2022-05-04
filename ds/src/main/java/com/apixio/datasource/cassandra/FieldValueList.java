package com.apixio.datasource.cassandra;

import com.apixio.datasource.cassandra.field.Field;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by dyee on 3/8/18.
 */
public class FieldValueList
{
    final private Map<Field, Object> fieldValue;
    final private String rowkey;
    final private ByteBuffer value;
    final private long ttl;

    FieldValueList(Map<Field, Object> fieldValue, String rowkey, ByteBuffer value)
    {
        this(fieldValue, rowkey, value, 0L);
    }

    FieldValueList(Map<Field, Object> fieldValue, String rowkey, ByteBuffer value, long ttl)
    {
        this.fieldValue = fieldValue;

        //cache for easy access, since these field must be there in our model...
        this.rowkey = rowkey;
        this.value = value;
        this.ttl = ttl;
    }

    public Map<Field, Object> getFieldValue()
    {
        return fieldValue;
    }

    public String getRowkey()
    {
        return rowkey;
    }

    public ByteBuffer getValue()
    {
        return value;
    }

    public long getTtl()
    {
        return ttl;
    }
}
