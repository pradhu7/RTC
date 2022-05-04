package com.apixio.dao.nassembly;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AssemblyDAO contains all functionality for writing and reading fina;/merged/derived assembly data.  The assembly dao
 * is NOT an append only model but a (logical) overwrite model.
 *
 * The client code creates an instance of the implementation of this class and relies on the specific driver to
 * initialize the AssemblyDAO.
 */

public interface AssemblyDAO
{
    /**
     * Meta contains all the meta information for the assembly DOA to perform read and write operation.
     */

    public static class Meta implements Serializable
    {
        /**
         * The table/location where the data is stored.
         */
        private String database;

        /**
         * The table/location where the data is stored.
         */
        private String table;

        /**
         * The table/location where the datatype data is stored.
         *
         * Important: It must be a default table type table
         */
        private String dataTypeTable;

        /**
         * "objDomain" is something like pdsID
         *
         */
        private String objDomain;

        /**
         * "dataTypeName" is something like "claims", "wraps" or "subtraction"
         */
        private String dataTypeName;

        /**
         *  0 - indicates no tll
         *  The value is in seconds. It is calculated from now by Cassandra
         */
        private long ttlFromNowInSec;

        public Meta(String database, String table, String dataTypeTable, String objDomain, String dataTypeName, long ttlFromNowInSec)
        {
            this.database        = database;
            this.table           = table;
            this.dataTypeTable   = dataTypeTable;
            this.objDomain       = objDomain;
            this.dataTypeName    = dataTypeName;
            this.ttlFromNowInSec = ttlFromNowInSec;;
        }

        public String getDatabase()
        {
            return database;
        }

        public String getTable()
        {
            return table;
        }

        public String getDataTypeTable()
        {
            return dataTypeTable;
        }

        public String getObjDomain()
        {
            return objDomain;
        }

        public String getDataTypeName()
        {
            return dataTypeName;
        }

        public long getTtlFromNowInSec()
        {
            return ttlFromNowInSec;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
                return true;

            if (other == null || getClass() != other.getClass())
                return false;

            Meta meta = (Meta) other;

            return database.equals(meta.database) && table.equals(meta.table) && dataTypeTable.equals(meta.dataTypeName) &&
                    objDomain.equals(meta.objDomain) && dataTypeName.equals(meta.dataTypeName) && ttlFromNowInSec == meta.ttlFromNowInSec;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(database, table, dataTypeTable, objDomain, dataTypeName, ttlFromNowInSec);
        }

        @Override
        public String toString()
        {
            return "Meta{" +
                    "database=" + database +
                    ", table=" + table +
                    ", dataTypeTable=" + dataTypeTable +
                    ", objDomain=" + objDomain +
                    ", dataTypeName=" + dataTypeName +
                    ", ttlFromNowInSec=" + ttlFromNowInSec +
                    '}';
        }
    }

    /**
     * Data contains everything needed to persist or delete a persistent object.
     */
    public static class Data implements Serializable
    {
        /**
         * assemblyIdFieldName - this the name of assembly rowkey
         */
        private String assemblyIDFieldName;  // rowKey

        /**
         * The rowkey value
         */
        private String assemblyID;

        /**
         * cluster column names and corresponding values.
         * It must have at least one value
         *
         */
        private Map<String, Object> clusterColFieldNamesToValues;   // {"code" -> "123", "dos", 123}

        /**
         * non cluster column names and corresponding values.
         * Might be mull or empty.
        */
        private Map<String, Object> nonClusterColFieldNamesToValues;   // {"code" -> "123", "dos", 123}

        /**
         * The name of the protobuf column. Can't be null.
         */
        private String protobufFieldName; // d -> protobuf

        /**
         * The table/location where the data is stored. Can't be null.
         */
        private byte[] protobuf;

        // used to persist. read, or delete a column
        public Data(String assemblyIDFieldName, String assemblyID,
                    Map<String, Object> clusterColFieldNamesToValues,  Map<String, Object> nonClusterColFieldNamesToValues,
                    String protobufFieldName, byte[] protobuf)
        {
            this.assemblyIDFieldName             = assemblyIDFieldName;
            this.assemblyID                      = assemblyID;
            this.clusterColFieldNamesToValues    = clusterColFieldNamesToValues;
            this.nonClusterColFieldNamesToValues = nonClusterColFieldNamesToValues;
            this.protobufFieldName               = protobufFieldName;
            this.protobuf                        = protobuf;
        }

        // used to delete a row
        public Data(String assemblyIDFieldName, String assemblyIDList)
        {
            this.assemblyIDFieldName = assemblyIDFieldName;
            this.assemblyID          = assemblyIDList;
        }

        public String getAssemblyIDFieldName()
        {
            return assemblyIDFieldName;
        }

        public String getAssemblyID()
        {
            return assemblyID;
        }

        public Map<String, Object> getClusterColFieldNamesToValues()
        {
            return clusterColFieldNamesToValues;
        }

        public Map<String, Object> getNonClusterColFieldNamesToValues()
        {
            return nonClusterColFieldNamesToValues;
        }

        public String getProtobufFieldName()
        {
            return protobufFieldName;
        }

        public byte[] getProtobuf()
        {
            return protobuf;
        }

        @Override
        public String toString()
        {
            return "Data{" +
                    "assemblyID=" + assemblyID +
                    '}';
        }
    }

    /**
     * QueryResult contains everything needed to return data queried.
     */
    public static class QueryResult implements Serializable
    {
        /**
         * The rowkey value
         */
        private String rowKeyValue;

        /**
         * cluster column names and corresponding values.
         * It must have at least one value
         *
         */
        private Map<String, Object> colFieldNamesToValues;

        /**
         * The table/location where the data is stored. Can't be null.
         */
        private byte[] protobuf;

        /**
         * The write time in microseconds
         */
        long timeInMicroseconds;

        // used to read a row or part of a row
        public QueryResult(String rowKeyValue, Map<String, Object> colFieldNamesToValues, byte[] protobuf, long timeInMicroseconds)
        {
            this.rowKeyValue           = rowKeyValue;
            this.colFieldNamesToValues = colFieldNamesToValues;
            this.protobuf              = protobuf;
            this.timeInMicroseconds    = timeInMicroseconds;
        }

        public String getRowKeyValue()
        {
            return rowKeyValue;
        }

        public Map<String, Object> getColFieldNamesToValues()
        {
            return colFieldNamesToValues;
        }

        public byte[] getProtobuf()
        {
            return protobuf;
        }

        public long getTimeInMicroseconds()
        {
            return timeInMicroseconds;
        }

        @Override
        public String toString()
        {
            return "QueryResult{" +
                    "rowKeyValue=" + rowKeyValue +
                    ", timeInMicroseconds=" + timeInMicroseconds +
                    '}';
        }
    }

    /**
     * This is a hook for the specific implementation of the driver
     *
     * @param databaseToDrivers
     */
    default void setDrivers(Map<String, Object> databaseToDrivers)
    {
    }

    /**
     * Write an assembly object given meta and data
     *
     * @param meta
     * @param data
     */
    void write(Meta meta, Data data);

    /**
     * Delete a certain parts (columns) of an assembly object given meta and data
     *
     * @param meta
     * @param data
     */
    void deleteColumns(Meta meta, Data data);

    /**
     * Delete an assembly object given meta and data
     *
     * @param meta
     * @param data
     */
    void deleteRow(Meta meta, Data data);

    /**
     * flush all in memory data to disk
     *
     */
    void flush();

    /**
     * Read an assembly object given meta and data
     *
     * @param meta
     * @param data
     * @return List of assembly Query Result
     */
    List<QueryResult> read(Meta meta, Data data);

    /**
     * get the list of dataTypes given meta and assemblyID
     *
     * @param meta
     * @param assemblyID
     * @return List of dataTypeNames
     */
    List<String> getDataTypes(Meta meta, String assemblyID);
}