package com.apixio.dao.apxdata;

import java.util.List;

import com.apixio.XUUID;
import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.dao.utility.DataStore;

/**
 * DAO for persistence, querying, etc., of blob metadata including query keys
 */
public interface MetadataStore extends DataStore
{
    /**
     * Convention, to allow other DAOs access to an instance of this in a controlled way
     */
    public static final String ID = "apxdata-rdb";

    /**
     * Upserts the metadata row and query key row(s).  Values replaced on existing row
     * for meta are count and totalSize.  Query key values are replaced on existing
     * query key row(s).
     */ 
    public void addOrUpdateMeta(ApxMeta meta, QueryKeys queryKeys);

    /**
     * Returns the list of ApxMetas whose groupingID matches the param.
     */
    public List<ApxMeta> getMetasForGrouping(String groupingID);

    /**
     * Return groupingIDs whose query keys match ALL the given name=value pairs given
     * in the queryKeys param.
     */
    public List<String> queryGroupingIDs(QueryKeys queryKeys);

    /**
     * Delete all meta rows and all query key rows that match the given groupingID.
     */
    public void deleteMetaByGrouping(String groupingID);
}
