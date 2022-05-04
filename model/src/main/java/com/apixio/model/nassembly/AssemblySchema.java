package com.apixio.model.nassembly;

public class AssemblySchema
{
    private final static String  DATABASE            = "application";
    private final static String  TABLE_NAME          = "apx_cfnassembly";
    private final static String  ROW_KEY             = "rowkey";
    private final static String  PROTOBUF_FIELD_NAME = "d";

    public enum ColType{StringType, LongType};

    public AssemblySchema(AssemblyCol[] clusteringCols)
    {
        this(DATABASE, TABLE_NAME, ROW_KEY, PROTOBUF_FIELD_NAME, clusteringCols, null, 0l);
    }

    public AssemblySchema(String database, AssemblyCol[] clusteringCols)
    {
        this(database, TABLE_NAME, ROW_KEY, PROTOBUF_FIELD_NAME, clusteringCols, null, 0l);
    }

    public AssemblySchema(String database, AssemblyCol[] clusteringCols, AssemblyCol[] nonClusteringCols, long ttlFromNowInSec)
    {
        this(database, TABLE_NAME, ROW_KEY, PROTOBUF_FIELD_NAME, clusteringCols, nonClusteringCols, ttlFromNowInSec);
    }

    public AssemblySchema(AssemblyCol[] clusteringCols, AssemblyCol[] nonClusteringCols, long ttlFromNowInSec)
    {
        this(DATABASE, TABLE_NAME, ROW_KEY, PROTOBUF_FIELD_NAME, clusteringCols, nonClusteringCols, ttlFromNowInSec);
    }

    public AssemblySchema(String database, String tableName, String rowKey, String protobufFieldName,
                          AssemblyCol[] clusteringCols, AssemblyCol[] nonClusteringCols, long ttlFromNowInSec)
    {
        this.database          = database;
        this.tableName         = tableName;
        this.rowKey            = rowKey;
        this.protobufFieldName = protobufFieldName;
        this.clusteringCols    = clusteringCols;
        this.nonClusteringCols = nonClusteringCols;
        this.ttlFromNowInSec   = ttlFromNowInSec;
    }

    public String getDatabase()
    {
        return database;
    }

    public String getTableName()
    {
        return tableName;
    }

    public String getRowKey()
    {
        return rowKey;
    }

    public String getProtobufFieldName()
    {
        return protobufFieldName;
    }

    public AssemblyCol[] getClusteringCols()
    {
        return clusteringCols;
    }

    public AssemblyCol[] getNonClusteringCols()
    {
        return nonClusteringCols;
    }

    public long getTtlFromNowInSec()
    {
        return ttlFromNowInSec;
    }

    public static class AssemblyCol
    {
        public AssemblyCol(String name, ColType colType)
        {
            this.name    = name;
            this.colType = colType;
        }

        public String getName()
        {
            return name;
        }

        public ColType getColType()
        {
            return colType;
        }

        private String  name;
        private ColType colType;
    }

    private String        database ;
    private String        tableName;
    private String        rowKey;
    private String        protobufFieldName;
    private AssemblyCol[] clusteringCols;
    private AssemblyCol[] nonClusteringCols;
    private long          ttlFromNowInSec;
}
