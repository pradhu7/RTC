package com.apixio.datasource.cassandra.field;

import java.util.Objects;

public class ByteBufferField implements Field
{
    static final public String TYPE = "ByteBuffer";
    private String name;

    public ByteBufferField(String name)
    {
        this.name = name;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof ByteBufferField))
            return false;
        ByteBufferField that = (ByteBufferField) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode()
    {

        return Objects.hash(getName());
    }
}
