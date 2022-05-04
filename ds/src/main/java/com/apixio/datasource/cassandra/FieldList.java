package com.apixio.datasource.cassandra;

import com.google.common.base.Joiner;

import java.util.List;

/**
 * Created by dyee on 3/8/18.
 */
public class FieldList
{
    final protected List<String> fields;
    final static String deliminator = ",";

    public FieldList(List<String> fields)
    {
        this.fields = fields;
    }

    public String mkString()
    {
        if (fields == null || fields.size() == 0)
        {
            throw new RuntimeException("Error: There are no fields");
        }

        return Joiner.on(deliminator).join(fields);
    }

    public String mkPlaceHolders()
    {
        if (fields == null || fields.size() == 0)
        {
            throw new RuntimeException("Error: There are no fields");
        }

        StringBuffer placeholders = null;
        for (String field : fields)
        {
            if (placeholders == null)
            {
                placeholders = new StringBuffer("");
            } else
            {
                placeholders.append(deliminator);
            }
            placeholders.append("?");
        }
        return placeholders.toString();
    }
}