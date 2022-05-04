package com.apixio.datasource.cassandra;

import com.apixio.datasource.cassandra.field.ByteBufferField;
import com.apixio.datasource.cassandra.field.Field;
import com.apixio.datasource.cassandra.field.LongField;
import com.apixio.datasource.cassandra.field.StringField;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class FieldValueListBuilder
{
    private boolean assumePartitionKeyByOrder = true;

    private Pair<StringField, String> partitionKey = null;
    private Pair<ByteBufferField, ByteBuffer> data = null;
    private Map<Field, Object> columnValues = new LinkedHashMap<>();
    private long ttl;

    public FieldValueListBuilder addTtl(long ttl)
    {
        if (ttl < 0L)
            throw new RuntimeException("Error: ttl can't be negative: " + ttl);

        this.ttl = ttl;
        return this;
    }

    public FieldValueListBuilder addPartitionKey(StringField field, String value)
    {
        partitionKey = new ImmutablePair<>(field, value);
        return this;
    }

    public FieldValueListBuilder setAssumePartitionKeyByOrder(boolean value) {
        assumePartitionKeyByOrder = value;
        return this;
    }

    public FieldValueListBuilder addColumn(Field field, Object value)
    {
        if(!(field instanceof ByteBufferField))
        {
            columnValues.put(field, value);
        }
        else
        {
            throw new RuntimeException("Error: columns can't be of type ByteBuffer");
        }
        return this;
    }

    public FieldValueListBuilder addString(String field, String value)
    {
        if(assumePartitionKeyByOrder && partitionKey==null)
        {
            addPartitionKey(new StringField(field), value);
        }
        else
        {
            addColumn(new StringField(field), value);
        }
        return this;
    }

    public FieldValueListBuilder addLong(String field, Long value)
    {
        addColumn(new LongField(field), value);
        return this;
    }

    public FieldValueListBuilder addByteBuffer(String field, ByteBuffer value)
    {
        if(assumePartitionKeyByOrder && partitionKey==null) {
            throw new RuntimeException("Error: assumePartitionKeyByOrder is enabled, so first entry must be partition key");
        }

        if(data != null)
        {
            throw new RuntimeException("Error: Only one byte buffer allowed");
        }
        data = new ImmutablePair<>(new ByteBufferField(field), value);
        return this;
    }

    public FieldValueListBuilder addString(String field)
    {
        return addString(field, null);
    }

    public FieldValueListBuilder addByteBuffer(String field)
    {
        return addByteBuffer(field, null);
    }

    public FieldValueList build()
    {
        if(partitionKey == null)
        {
            throw new RuntimeException("Error: PartitionKey must be specified");
        }

        Map<Field, Object> fieldValue = new LinkedHashMap<>();

        fieldValue.put(partitionKey.getKey(), partitionKey.getValue());

        if(columnValues!=null && columnValues.size()>0)
        {
            fieldValue.putAll(columnValues);
        }

        if(data !=null)
        {
            fieldValue.put(data.getKey(), data.getValue());
        }

        return new FieldValueList(fieldValue, partitionKey==null ? null: partitionKey.getValue(),
                data==null ? null : data.getValue(), ttl);
    }
}