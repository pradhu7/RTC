package com.apixio.dao.indexsearch;

import com.apixio.dao.indexsearch.DemographicData.DemographicType;

public class QueryAndType
{
    public QueryAndType(String query, DemographicType type)
    {
        this.query = query;
        this.type = type;
    }

    public String          query;
    public DemographicType type;
}
