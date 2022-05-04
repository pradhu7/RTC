package com.apixio.model.nassembly;


/**
 * A class that describes whether a field is using for sorting and the type of sort (desc or asc)
 */
public class SortedFieldOrder
{
    public SortedFieldOrder(String fieldName, boolean desc)
    {
        this.fieldName = fieldName;
        this.desc      = desc;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public boolean isDesc()
    {
        return desc;
    }

    private final String  fieldName;
    private final boolean desc;
}