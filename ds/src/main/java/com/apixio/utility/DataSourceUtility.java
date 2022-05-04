package com.apixio.utility;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.SortedMap;

import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlException;
import com.apixio.datasource.cassandra.CqlRowData;
import com.apixio.datasource.cassandra.FieldValueList;
import com.apixio.datasource.cassandra.StatementGenerator;

public class DataSourceUtility
{
    private static final Charset charset = Charset.forName("UTF-8");
    private static final CharsetEncoder encoder = charset.newEncoder();

    public static void saveRawData(CqlCache cqlCache, String key, String columnName, byte[] value, String columnFamily, long ttlFromNowInSec)
            throws CqlException
    {
        CqlRowData cqlRowData = new CqlRowData(columnFamily, key, columnName, ByteBuffer.wrap(value), ttlFromNowInSec);
        cqlCache.insert(cqlRowData);
    }

    public static void saveRawData(CqlCache cqlCache, String key, String columnName, byte[] value, String columnFamily)
            throws CqlException
    {
        CqlRowData cqlRowData = new CqlRowData(columnFamily, key, columnName, ByteBuffer.wrap(value));
        cqlCache.insert(cqlRowData);
    }

    public static void saveRawData(CqlCache cqlCache, StatementGenerator statementGenerator, FieldValueList fieldValueList, String columnFamily)
            throws CqlException
    {
        CqlRowData cqlRowData = new CqlRowData(columnFamily, statementGenerator, fieldValueList);
        cqlCache.insert(cqlRowData);
    }

    public static void deleteColumn(CqlCache cqlCache, String key, String columnName, String columnFamily)
            throws CqlException
    {
        CqlRowData cqlRowData = new CqlRowData(columnFamily, key, columnName);
        cqlCache.deleteColumn(cqlRowData);
    }

    public static void deleteColumns(CqlCache cqlCache, StatementGenerator statementGenerator, FieldValueList fieldValueList, String columnFamily)
            throws CqlException
    {
        CqlRowData cqlRowData = new CqlRowData(columnFamily, statementGenerator, fieldValueList);
        cqlCache.deleteColumn(cqlRowData);
    }

    public static void deleteRow(CqlCache cqlCache, String key, String columnFamily)
            throws CqlException
    {
        CqlRowData cqlRowData = new CqlRowData(columnFamily, key);
        cqlCache.deleteRow(cqlRowData);
    }

    public static void deleteRow(CqlCache cqlCache, StatementGenerator statementGenerator, FieldValueList fieldValueList, String columnFamily)
            throws CqlException
    {
        CqlRowData cqlRowData = new CqlRowData(columnFamily, statementGenerator, fieldValueList);
        cqlCache.deleteRow(cqlRowData);
    }

    public static SortedMap<String, ByteBuffer> readColumns(CqlCrud cqlCrud, String rowkey, String pattern, String columnFamily)
            throws CqlException
    {
        return cqlCrud.getColumnsMap(columnFamily, rowkey, pattern);
    }

    public static SortedMap<String, ByteBuffer> readColumns(CqlCrud cqlCrud, String rowkey, String pattern, String columnFamily, int limit)
            throws CqlException
    {
        return cqlCrud.getColumnsMap(columnFamily, rowkey, pattern, limit);
    }

    public static SortedMap<String, ByteBuffer> readColumns(CqlCrud cqlCrud, String rowkey, String start, String end, String columnFamily)
            throws CqlException
    {
        return cqlCrud.getColumnsMap(columnFamily, rowkey, start, end);
    }

    public static CqlCrud.ColumnMapResult readColumnsWithWriteTime(CqlCrud cqlCrud, String rowkey, String columnFamily)
            throws CqlException
    {
        return cqlCrud.getColumnsMapWithWriteTime(columnFamily, rowkey);
    }

    public static SortedMap<String, ByteBuffer> readColumnsWithPattern(CqlCrud cqlCrud, String rowkey, String start, String end, String columnFamily)
            throws CqlException
    {
        return cqlCrud.getColumnsMapWithPattern(columnFamily, rowkey, start, end);
    }

    public static ByteBuffer readColumnValue(CqlCrud cqlCrud, String rowkey, String columnName, String columnFamily)
            throws CqlException
    {
        return cqlCrud.getColumnValue(columnFamily, rowkey, columnName);
    }

    public static SortedMap<String, ByteBuffer> readColumnValues(CqlCrud cqlCrud, String rowkey, List<String> columnNames, String columnFamily)
            throws CqlException
    {
        return cqlCrud.getColumnValues(columnFamily, rowkey, columnNames);
    }

    public static String getString(ByteBuffer byteBuffer)
    {
        return (byteBuffer == null ? null : new String(getBytes(byteBuffer)));
    }

    public static String getString(byte[] bytes)
    {
        return (bytes == null ? null : new String(bytes));
    }

    public static byte[] getBytes(ByteBuffer byteBuffer)
    {
        if (byteBuffer == null)
            return null;

        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes, 0, bytes.length);

        return bytes;
    }

    public static ByteBuffer stringToByteBuffer(String st)
    {
        try
        {
            return encoder.encode(CharBuffer.wrap(st));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
