package com.apixio.datasource.cassandra.field;

import java.util.Objects;

public class LongField implements Field
{
    static public final String TYPE = "Long";

    String name;

    public LongField(String name)
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
        if (!(o instanceof LongField))
            return false;
        LongField longField = (LongField) o;
        return Objects.equals(getName(), longField.getName());
    }

    @Override
    public int hashCode()
    {

        return Objects.hash(getName());
    }
}