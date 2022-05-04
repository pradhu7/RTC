package com.apixio.datasource.cassandra;

import java.nio.ByteBuffer;

public class ColumnAndValue
{
    ColumnAndValue(String column, ByteBuffer value)
    {
        this.column = column;
        this.value = value;
    }

    public String column;
    public ByteBuffer value;

    @Override
    public String toString()
    {
        return ("[ColumnAndValue: "+
                "; column=" + column +
                "; value=" + value +
                "]");
    }
}
