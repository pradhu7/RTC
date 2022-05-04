package com.apixio.datasource.cassandra.field;

public interface Field
{
    /**
     * Get Typed information about the field.
     *
     * e.g. col is type "String", and d is typed "ByteBuffer"
     *
     * @return String representation of typed field.
     */
    String getType();

    /**
     * Get the field name.
     * @return String name of field.
     */
    String getName();
}
