package com.apixio.datasource.cassandra.field;

import java.util.Objects;

public class StringArrayField implements Field
{
    static public final String TYPE = "StringArray";

    String name;

    public StringArrayField(String name)
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
        if (!(o instanceof StringArrayField))
            return false;
        StringArrayField that = (StringArrayField) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode()
    {

        return Objects.hash(getName());
    }
}
