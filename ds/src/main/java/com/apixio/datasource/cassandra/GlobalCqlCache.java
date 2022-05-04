package com.apixio.datasource.cassandra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements a cql cache based on a synchronized list.
 */
public class GlobalCqlCache extends CqlCache
{
    private List<CqlRowData> cqlCache = Collections.synchronizedList(new ArrayList<CqlRowData>());

    @Override
    protected void addToCache(CqlRowData cqlRowData)
    {
        cqlCache.add(cqlRowData);
    }

    @Override
    protected List<CqlRowData> getCache()
    {
        synchronized(cqlCache)
        {
            List<CqlRowData> copyCache = new ArrayList<>();

            Iterator<CqlRowData> iterator = cqlCache.iterator();
            while (iterator.hasNext())
            {
                copyCache.add(iterator.next());
            }

            cleanCache();

            return copyCache;
        }
    }

    @Override
    public int cacheSize()
    {
        return cqlCache.size();
    }

    @Override
    protected void cleanCache()
    {
        cqlCache.clear();
    }

    @Override
    public void flush()
    {
        List<CqlRowData> all = getCache();

        if (all != null && !all.isEmpty())
        {
            totalBufferWrites++;
            cqlCrud.insertOrDeleteOps(buildRawDataSet(all));
        }
    }
}
