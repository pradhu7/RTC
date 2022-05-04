package com.apixio.datasource.cassandra;


import com.apixio.datasource.cassandra.field.ByteBufferField;
import com.apixio.datasource.cassandra.field.Field;
import com.apixio.datasource.cassandra.field.LongField;
import com.apixio.datasource.cassandra.field.StringArrayField;
import com.apixio.datasource.cassandra.field.StringField;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryFieldValueListBuilder
{
    private Map<Field, Object> columnValues = new LinkedHashMap<>();

    public QueryFieldValueListBuilder addColumn(Field field, Object value)
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

    public QueryFieldValueListBuilder addString(String field, String value)
    {
        addColumn(new StringField(field), value);
        return this;
    }

    public QueryFieldValueListBuilder addStringArray(String field, String [] value)
    {
        addColumn(new StringArrayField(field), value);
        return this;
    }


    public QueryFieldValueListBuilder addLong(String field, Long value)
    {
        addColumn(new LongField(field), value);
        return this;
    }

    public QueryFieldValueListBuilder addString(String field)
    {
        return addString(field, null);
    }

    public FieldValueList build()
    {
        Map<Field, Object> fieldValue = new LinkedHashMap<>();

        if(columnValues!=null && columnValues.size()>0)
        {
            fieldValue.putAll(columnValues);
        }


        return new FieldValueList(fieldValue, null, null);
    }
}