package com.apixio.dao.utility;

import java.util.Iterator;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import com.apixio.utility.DataSourceUtility;
import com.apixio.datasource.cassandra.ColumnAndValue;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;

public class OneToManyStore
{
    private CqlCache cqlCache;
    private CqlCrud  cqlCrud;

    public void setCqlCrud(CqlCrud cqlCrud)
    {
    	this.cqlCrud  = cqlCrud;
        this.cqlCache = cqlCrud.getCqlCache();
    }

    public void put(String id, String oneToManyIndexKey, String oneToManyPrefix, int numberOfBuckets, String columnFamily) throws Exception
    {
        put(id, "x".getBytes("UTF-8"), oneToManyIndexKey, oneToManyPrefix, numberOfBuckets, columnFamily);
    }

    public void put(String id, byte[] value, String oneToManyIndexKey, String oneToManyPrefix, int numberOfBuckets, String columnFamily) throws Exception
    {
        writeOneToManyIndex(id, oneToManyIndexKey, numberOfBuckets, columnFamily);
        writeOneToMany(id, value, oneToManyPrefix, numberOfBuckets, columnFamily);
    }

    // VK: We never delete the index (i.e., one to many index). Only one-to-many. That will be dangerous
    public void delete(String id, String oneToManyPrefix, int numberOfBuckets, String columnFamily) throws Exception
    {
        deleteOneToMany(id, oneToManyPrefix, numberOfBuckets, columnFamily);
    }

    public boolean checkIfKeyExists(String id, String oneToManyIndexKey, String oneToManyPrefix, int numberOfBuckets, String columnFamily) throws Exception
    {
        String indexKey    = prepareOneToManyIndexKey(oneToManyIndexKey);
        String indexColumn = prepareOneToManyIndexColumn(id, numberOfBuckets);

        if (DataSourceUtility.readColumnValue(cqlCrud, indexKey, indexColumn, columnFamily) == null) return false;

        String key = prepareOneToManyKey(id, oneToManyPrefix, numberOfBuckets);
        if (DataSourceUtility.readColumnValue(cqlCrud, key, id, columnFamily) == null) return false;

        return true;
    }

    public Iterator<String> getKeys(String oneToManyIndexPrefix, String oneToManyPrefix, String columnFamily) throws Exception
    {
        return new KeyIterator(oneToManyIndexPrefix, oneToManyPrefix, columnFamily);
    }

    private class KeyIterator implements Iterator<String>
    {
        private Iterator<KeyAndValue> keyAndValueIterator;

        /**
         * hasNext returns true if and only if there should be another column left to return.
         */
        public boolean hasNext()
        {
            return keyAndValueIterator.hasNext();
        }

        /**
         * next returns the next key
         */
        public String next()
        {
            KeyAndValue keyAndValue = keyAndValueIterator.next();
            return (keyAndValue != null ? keyAndValue.key : null);
        }

        /**
         * remove throws an exception since we don't allow keys to be removed via this interface.
         */
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        /**
         * Constructs a new KeyIterator by relying on keyAndValueIterator
         *
         * @throws Exception
         */
        private KeyIterator(String oneToManyIndexPrefix, String oneToManyPrefix, String columnFamily) throws Exception
        {
            keyAndValueIterator = getKeysAndValues(oneToManyIndexPrefix, oneToManyPrefix, columnFamily);
        }
    }

    public Iterator<KeyAndValue> getKeysAndValues(String oneToManyIndexPrefix, String oneToManyPrefix, String columnFamily) throws Exception
    {
        return new KeyAndValueIterator(oneToManyIndexPrefix, oneToManyPrefix, columnFamily);
    }

    private class KeyAndValueIterator implements Iterator<KeyAndValue>
    {
        private String columnFamily;
        private String oneToManyPrefix;
        private Iterator<ColumnAndValue> indexColumnAndValue;
        private Iterator<ColumnAndValue> keyColumnAndValue;

        /**
         * hasNext returns true if and only if there should be another column left to return.
         */
        public boolean hasNext()
        {
            if (keyColumnAndValue != null && keyColumnAndValue.hasNext()) return true;

            if (!indexColumnAndValue.hasNext()) return false;

            try
            {
                String column = prepareOneToManyKeyUsingIndexColumn(oneToManyPrefix, indexColumnAndValue.next().column);
                keyColumnAndValue = cqlCrud.getAllColumns(columnFamily, column, "", 10000);
            }
            catch (Exception e)
            {
                return false;
            }

            return keyColumnAndValue.hasNext();
        }

        /**
         * next returns the next key
         */
        public KeyAndValue next()
        {
            if (!hasNext())
                return null;

            ColumnAndValue columnAndValue = keyColumnAndValue.next();
            return new KeyAndValue(columnAndValue.column, DataSourceUtility.getBytes(columnAndValue.value));
        }

        /**
         * remove throws an exception since we don't allow keys to be removed via this interface.
         */
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        /**
         * Constructs a new KeyIterator by setting up the query parameters and relying on underlying
         * functionality of getAllColumns
         *
         * @throws Exception
         */
        private KeyAndValueIterator(String oneToManyIndexPrefix, String oneToManyPrefix, String columnFamily) throws Exception
        {
            this.columnFamily = columnFamily;
            this.oneToManyPrefix = oneToManyPrefix;
            indexColumnAndValue = cqlCrud.getAllColumns(columnFamily, prepareOneToManyIndexKey(oneToManyIndexPrefix), "", 10000);
        }
    }

    static public class KeyAndValue
    {
        public KeyAndValue(String key, byte[] value)
        {
            this.key = key;
            this.value = value;
        }

        public String  key;
        public byte[]  value;
    }

    private static CacheLoader<String, String> loader = new CacheLoader<String, String>()
        {
                @Override
                public String load(String key) {
                    return key;
        }
    };

    // Cache Size - 50000K
    private static LoadingCache<String, String> columnCache = CacheBuilder.newBuilder().maximumSize(50000L).build(loader);

    private void writeOneToManyIndex(String id, String oneToManyIndexKey, int numberOfBuckets, String columnFamily) throws Exception
    {
        String key      = prepareOneToManyIndexKey(oneToManyIndexKey);
        String column   = prepareOneToManyIndexColumn(id, numberOfBuckets);
        String cacheKey = "otmi" + columnFamily + key + column;

        // if it is in cache, nothing to do
        if (columnCache.getIfPresent(cacheKey) != null) return;

        // We don't want to keep writing the same value again and again!!!
        if (DataSourceUtility.readColumnValue(cqlCrud, key, column, columnFamily) == null)
        {
            DataSourceUtility.saveRawData(cqlCache, key, column, "x".getBytes("UTF-8"), columnFamily);
        }

        columnCache.put(cacheKey, "x");
    }

    private void writeOneToMany(String id, byte[] value, String oneToManyPrefix, int numberOfBuckets, String columnFamily) throws Exception
    {
        String key      = prepareOneToManyKey(id, oneToManyPrefix, numberOfBuckets);
        String cacheKey = "otm" + columnFamily + key + id;

        // if it is in cache, nothing to do
        if (columnCache.getIfPresent(cacheKey) != null) return;

        // We don't want to keep writing the same value again and again!!!
        if (DataSourceUtility.readColumnValue(cqlCrud, key, id, columnFamily) == null)
        {
            DataSourceUtility.saveRawData(cqlCache, key, id, value, columnFamily);
        }

        columnCache.put(cacheKey, "x");
    }

    private void deleteOneToMany(String id, String oneToManyPrefix, int numberOfBuckets, String columnFamily) throws Exception
    {
        String key = prepareOneToManyKey(id, oneToManyPrefix, numberOfBuckets);

        DataSourceUtility.deleteColumn(cqlCache, key, id, columnFamily);
    }

    private String prepareOneToManyIndexKey(String oneToManyIndexKey)
    {
        return oneToManyIndexKey;
    }

    private String prepareOneToManyIndexColumn(String id, int numberOfBuckets)
    {
        int hash = id.hashCode();
        if (hash < 0) hash = -hash;
         return String.valueOf(hash % numberOfBuckets);
    }

    private String prepareOneToManyKey(String id, String oneToManyPrefix, int numberOfBuckets)
    {
        int hash = id.hashCode();
        if (hash < 0) hash = -hash;
        return oneToManyPrefix + (hash % numberOfBuckets);
    }

    private String prepareOneToManyKeyUsingIndexColumn(String oneToManyPrefix, String column)
    {
        return oneToManyPrefix + column;
    }
}
