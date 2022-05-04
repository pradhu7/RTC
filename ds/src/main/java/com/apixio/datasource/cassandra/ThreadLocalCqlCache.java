package com.apixio.datasource.cassandra;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a cql cache based on thread local.
 */
public class ThreadLocalCqlCache extends CqlCache
{
    private ThreadLocal<List<CqlRowData>> cqlCache = new ThreadLocal<List<CqlRowData>>();

    @Override
    protected void addToCache(CqlRowData cqlRowData)
    {
        List<CqlRowData> all = getCache();

        all.add(cqlRowData);
    }

    @Override
    protected List<CqlRowData> getCache()
    {
        List<CqlRowData> all = cqlCache.get();

        if (all == null)
        {
            all = new ArrayList<CqlRowData>();
            cqlCache.set(all);
        }

        return all;
    }

    @Override
    public int cacheSize()
    {
        List<CqlRowData> all = cqlCache.get();

        return (all != null) ? all.size() : 0;
    }

    @Override
    protected void cleanCache()
    {
        cqlCache.set(null);
        cqlCache.remove();
    }

    @Override
    public void flush()
    {
        List<CqlRowData> all = getCache();

        if (all != null && !all.isEmpty())
        {
            totalBufferWrites++;
            List<CqlRowData> cqlRowDatas = buildRawDataSet(all);
            List<CqlRowData> chunks      = new ArrayList<>();

            int size = 0;
            for (CqlRowData cqlRowData: cqlRowDatas)
            {
                chunks.add(cqlRowData);
                size++;

                if (size >= 1000)
                {
                    cqlCrud.insertOrDeleteOps(chunks);
                    chunks.clear();
                    size = 0;
                }
            }

            if (size > 0)
            {
                cqlCrud.insertOrDeleteOps(chunks);
                chunks.clear();
            }
        }

        cleanCache();
    }
}
