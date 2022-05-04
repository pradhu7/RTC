package com.apixio.datasource.cassandra.field;

import java.util.Objects;

public class StringField implements Field
{
    static public final String TYPE = "String";

    String name;

    public StringField(String name)
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
        if (!(o instanceof StringField))
            return false;
        StringField that = (StringField) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode()
    {

        return Objects.hash(getName());
    }
}
