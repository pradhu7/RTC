package com.apixio.datasource.cassandra;

import com.apixio.datasource.cassandra.field.StringField;

import java.nio.ByteBuffer;

/**
 * Created by dyee on 2/16/18.
 */
public class CqlRowData
{
    public CqlRowData.CqlOps ops;
    public String     rowkey;
    public ByteBuffer value;

    public enum CqlOps
    {
        insert,
        insertIfNotExists, // careful uses paxos, can be slow.
        deleteRow,
        deleteColumn
    }

    public void setCqlOps(CqlRowData.CqlOps ops)
    {
        this.ops = ops;
    }

    public String tableName;
    public String column;
    public StatementGenerator statementGenerator;
    public FieldValueList fieldValueList;

    public CqlRowData(){
    }

    public CqlRowData(String tableName, String rowkey, String column, ByteBuffer value, long ttlFromNowInSec)
    {
        this(tableName, new StatementGenerator("rowkey", "col", "d"), new FieldValueListBuilder()
                .addPartitionKey(new StringField("rowkey"), rowkey)
                .addColumn(new StringField("col"), column)
                .addByteBuffer("d", value)
                .addTtl(ttlFromNowInSec)
                .build());

    }

    public CqlRowData(String tableName, String rowkey, String column, ByteBuffer value)
    {
        this(tableName, new StatementGenerator("rowkey", "col", "d"), new FieldValueListBuilder()
                .addPartitionKey(new StringField("rowkey"), rowkey)
                .addColumn(new StringField("col"), column)
                .addByteBuffer("d", value)
                .build());

    }

    public CqlRowData(String tableName, String rowkey, String column)
    {
        this(tableName, new StatementGenerator("rowkey", "col"), new FieldValueListBuilder()
                .addPartitionKey(new StringField("rowkey"), rowkey)
                .addColumn(new StringField("col"), column)
                .build());
    }

    public CqlRowData(String tableName, String rowkey)
    {
        this(tableName, new StatementGenerator("rowkey"), new FieldValueListBuilder()
                .addPartitionKey(new StringField("rowkey"), rowkey)
                .build());
    }

    public CqlRowData(String tableName, StatementGenerator statementGenerator, FieldValueList fieldValueList)
    {
        super();

        this.tableName = tableName;
        this.statementGenerator = statementGenerator;
        this.fieldValueList = fieldValueList;
        this.rowkey = fieldValueList.getRowkey();
        this.value = fieldValueList.getValue();
    }

    public String getTableName()
    {
        return tableName;
    }

    public StatementGenerator getStatementGenerator()
    {
        return statementGenerator;
    }

    public FieldValueList getFieldValueList()
    {
        return fieldValueList;
    }
}

