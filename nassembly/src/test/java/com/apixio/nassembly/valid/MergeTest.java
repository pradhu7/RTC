package com.apixio.nassembly.valid;

import com.apixio.model.nassembly.GarbageCollector;
import com.apixio.model.nassembly.SortedFieldOrder;

import java.util.Set;

public class MergeTest implements GarbageCollector {
    @Override
    public String getDataTypeName()
    {
        return "impl1";
    }

    @Override
    public Set<String> getGroupByFieldNames()
    {
        return null;
    }

    @Override
    public SortedFieldOrder[] getSortedFields()
    {
        return null;
    }
}

