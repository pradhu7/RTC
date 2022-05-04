package com.apixio.dao.indexsearch;

import java.util.List;

public class DemographicData
{
    /**
     * Types of indexed demographic data.
     * each type corresponds to a raw type in Cassandra
     * "nt" is the no type or universal type - all the fields are indexed to the same row.
     */
    public enum DemographicType
    {
        uuid, pid, fn, ln, bd, aid, nt
    }

    public boolean indexUuid;

    /**
     * Fields indexed in Cassandra
     */
    public String         uuid;
    public String         primaryId;
    public List<String>   firstNames;
    public List<String>   lastNames;
    public List<String>   birthDates;
    public List<String>   alternateIds;
}
