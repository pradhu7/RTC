package com.apixio.datasource.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.datasource.cassandra.CqlRowData.CqlOps;
import com.apixio.datasource.cassandra.field.ByteBufferField;
import com.apixio.datasource.cassandra.field.Field;
import com.apixio.datasource.cassandra.field.LongField;
import com.apixio.datasource.cassandra.field.StringField;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This class caches Cassandra operations and supports caching of insert, delete row, and delete column operations.
 */
public abstract class CqlCache
{
    protected long     totalBufferWrites;
    protected CqlCrud  cqlCrud;
    private   int      bufferSize;
    private   boolean  dontFlush;

    public void setCqlCrud(CqlCrud crud)
    {
        cqlCrud = crud;
    }

    public CqlCrud getCqlCrud()
    {
        return cqlCrud;
    }

    public void setBufferSize(int size)
    {
        bufferSize = size;
    }

    public void setDontFlush(boolean dontFlush)
    {
        this.dontFlush = dontFlush;
    }

    public long getTotalBufferWrites()
    {
        return totalBufferWrites;
    }

    /**
     * Add an "insert" operation into the cache. If the cache is full, execute all the operations
     * in a batch and empty the cache
     *
     * @param cqlRowData
     * @throws CqlException
     */
    public void insert(CqlRowData cqlRowData)
    {
        cqlRowData.setCqlOps(CqlOps.insert);
        addToCache(cqlRowData);

        if (cacheSize() > bufferSize && !dontFlush)
        {
            flush();
        }
    }

    /**
     * Add an "insert" operation into the cache. If the cache is full, execute all the operations
     * in a batch and empty the cache
     *
     * @param cqlRowData
     * @throws CqlException
     */
    public void insertIfNotExists(CqlRowData cqlRowData)
    {
        cqlRowData.setCqlOps(CqlOps.insertIfNotExists);
        addToCache(cqlRowData);

        if (cacheSize() > bufferSize && !dontFlush)
        {
            flush();
        }
    }

    /**
     * Add a "delete row" operation into the cache. If the cache is full, execute all the operations
     * in a batch and empty the cache
     *
     * @param cqlRowData
     * @throws CqlException
     */
    public void deleteRow(CqlRowData cqlRowData)
    {
        cqlRowData.setCqlOps(CqlOps.deleteRow);
        addToCache(cqlRowData);

        if (cacheSize() > bufferSize && !dontFlush)
        {
            flush();
        }
    }

    /**
     * Add a "delete column" operation into the cache. If the cache is full, execute all the operations
     * in a batch and empty the cache
     *
     * @param cqlRowData
     * @throws CqlException
     */
    public void deleteColumn(CqlRowData cqlRowData)
    {
        cqlRowData.setCqlOps(CqlOps.deleteColumn);
        addToCache(cqlRowData);

        if (cacheSize() > bufferSize && !dontFlush)
        {
            flush();
        }
    }

    public void flushIfNeeded()
    {
        if (cacheSize() > bufferSize)
        {
            flush();
        }
    }

    protected List<CqlRowData> buildRawDataSet(List<CqlRowData> rowDataSet)
    {
        if (rowDataSet == null)
            return null;

        List<CqlRowData> newDataSet = new ArrayList<>();

        for (int i = 0; i < rowDataSet.size(); i++)
        {
            CqlRowData rd = rowDataSet.get(i);

            if (!hasNewerOverwriteOperation(rowDataSet, rd, i))
                newDataSet.add(rd);
        }

        return newDataSet;
    }

    private boolean hasNewerOverwriteOperation(List<CqlRowData> rowDataSet, CqlRowData rd, int pos)
    {
        Pair<String,String> rdRowkey = null;
        Map<String, String> rdColumns = new HashMap<>();

        for (Map.Entry<Field, Object> fieldValue : rd.getFieldValueList().getFieldValue().entrySet())
        {
            switch (fieldValue.getKey().getType())
            {
                case LongField.TYPE:
                    if(rdRowkey == null)
                        rdRowkey = new ImmutablePair<>(fieldValue.getKey().getName(), Long.toString((Long)fieldValue.getValue()));
                    else
                        rdColumns.put(fieldValue.getKey().getName(), Long.toString((Long)fieldValue.getValue()));
                    break;
                case StringField.TYPE:
                    if(rdRowkey == null)
                        rdRowkey = new ImmutablePair<>(fieldValue.getKey().getName(), (String)fieldValue.getValue());
                    else
                        rdColumns.put(fieldValue.getKey().getName(), (String)fieldValue.getValue());
                    break;
                case ByteBufferField.TYPE:
                    break;
                default:
                    throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
            }
        }

        for (int i = pos + 1; i < rowDataSet.size(); i++)
        {
            CqlRowData rd1 = rowDataSet.get(i);
            FieldValueList fieldValueList = rd1.getFieldValueList();

            Pair<String,String> rd1RowKey = null;
            Map<String, String> rd1Columns = new HashMap<>();

            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case LongField.TYPE:
                        if(rd1RowKey == null)
                            rd1RowKey = new ImmutablePair<>(fieldValue.getKey().getName(), Long.toString((Long)fieldValue.getValue()));
                        else
                            rd1Columns.put(fieldValue.getKey().getName(), Long.toString((Long)fieldValue.getValue()));
                        break;
                    case StringField.TYPE:
                        if(rd1RowKey == null)
                            rd1RowKey = new ImmutablePair<>(fieldValue.getKey().getName(), (String)fieldValue.getValue());
                        else
                            rd1Columns.put(fieldValue.getKey().getName(), (String)fieldValue.getValue());
                        break;
                    case ByteBufferField.TYPE:
                        break;
                    default:
                        throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                }
            }

            boolean    te  = rd.tableName.equals(rd1.tableName);
            boolean    re  = rdRowkey.getRight().equals(rd1RowKey.getRight());
            boolean    ce  = (rdColumns.size() == rd1Columns.size());

            // If column size is the same, we need to make sure that each named col
            // is of the same value.
            if(ce)
            {
                for (Map.Entry<String, String> entry : rd1Columns.entrySet())
                {
                    ce = entry.getValue().equals(rdColumns.get(entry.getKey()));
                    if(!ce) break;
                }
            }

            if (rd.ops == rd1.ops && te && re && ce)
                return true;

            if (rd1.ops == CqlOps.deleteRow && te && re)
                return true;

            if (rd.ops == CqlOps.insert && rd1.ops == CqlOps.deleteColumn && te && re && ce)
                return true;

            if (rd.ops == CqlOps.deleteColumn && rd1.ops == CqlOps.insert && te && re && ce)
                return true;
        }

        return false;
    }


    protected abstract void addToCache(CqlRowData cqlRowData);

    protected abstract List<CqlRowData> getCache();

    public abstract int cacheSize();

    protected abstract void cleanCache();



    /**
     * Flush both the insert cache and delete cache by first executing all the cached operations and then flushing the cache
     *
     * @throws CqlException
     */
    public abstract void flush();
}
