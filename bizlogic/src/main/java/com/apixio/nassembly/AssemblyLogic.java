package com.apixio.nassembly;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.apixio.dao.nassembly.AssemblyDAO.Meta;
import com.apixio.dao.nassembly.AssemblyDAO.Data;
import com.apixio.model.nassembly.AssemblySchema;

/**
 * AssemblyLogic contains business logic related to managing assembly things.
 * It contains all functionality for writing and reading final/merged/derived assembly data.
 */

public interface AssemblyLogic
{
    int    maxStringCols       = 3;
    int    maxLongCols         = 2;
    String dontCareStringValue = "444";
    Long   dontCareLongValue   = Long.MIN_VALUE;

    /**
     * Assembly Meta contains all the meta information for the assembly Logic to perform read and write operation.
     */

    public static class AssemblyMeta implements Serializable
    {
        /**
         * The logical schema information (this is NOT the physical schema).
         */
        private AssemblySchema assemblySchema;

        /**
         * "objDomain" is something like pdsID
         *
         */
        private String objDomain;

        /**
         * "dataTypeName" is something like "claims", "wraps" or "subtraction"
         */
        private String dataTypeName;

        public AssemblyMeta(AssemblySchema assemblySchema, String objDomain, String dataTypeName)
        {
            this.assemblySchema  = assemblySchema;
            this.objDomain       = objDomain;
            this.dataTypeName    = dataTypeName;
        }

        public Meta toDAOMeta(String dataTypeTable)
        {
            return new Meta(assemblySchema.getDatabase(), assemblySchema.getTableName(), dataTypeTable, objDomain, dataTypeName, assemblySchema.getTtlFromNowInSec());
        }

        public AssemblySchema getAssemblySchema()
        {
            return assemblySchema;
        }

        public String getObjDomain()
        {
            return objDomain;
        }

        public String getDataTypeName()
        {
            return dataTypeName;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
                return true;

            if (other == null || getClass() != other.getClass())
                return false;

            AssemblyMeta meta = (AssemblyMeta) other;

            return assemblySchema.getDatabase().equals(meta.assemblySchema.getDatabase()) && assemblySchema.getTableName().equals(meta.assemblySchema.getTableName()) &&
                    objDomain.equals(meta.objDomain) && dataTypeName.equals(meta.dataTypeName);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(assemblySchema.getTableName(), objDomain, dataTypeName);
        }

        @Override
        public String toString()
        {
            return "AssemblyMeta{" +
                    "database=" + assemblySchema.getDatabase() +
                    ", table=" + assemblySchema.getTableName() +
                    ", rowKeyField=" + assemblySchema.getRowKey() +
                    ", protobufField=" + assemblySchema.getProtobufFieldName() +
                    ", objDomain=" + objDomain +
                    ", dataTypeName=" + dataTypeName +
                    '}';
        }
    }

    /**
     * Assembly Data contains everything needed to persist or delete a persistent object.
     */
    public static class AssemblyData implements Serializable
    {
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
         * The table/location where the data is stored. Can't be null.
         */
        private byte[] protobuf;

        // used to persists or delete a column
        public AssemblyData(String assemblyID, Map<String, Object> clusterColFieldNamesToValues,
                            Map<String, Object> nonClusterColFieldNamesToValues,
                            byte[] protobuf)
        {
            this.assemblyID                      = assemblyID;
            this.clusterColFieldNamesToValues    = clusterColFieldNamesToValues;
            this.nonClusterColFieldNamesToValues = nonClusterColFieldNamesToValues;
            this.protobuf                        = protobuf;
        }

        // used to delete a row
        public AssemblyData(String assemblyID)
        {
            this.assemblyID = assemblyID;
        }

        public Data toDAOData(Marshalling marshalling, AssemblyMeta assemblyMeta)
        {
            AssemblySchema assemblySchema = assemblyMeta.getAssemblySchema();

            return new Data(assemblySchema.getRowKey(), assemblyID, fromLogicalToPhysicalDataForWrite(assemblySchema),
                    nonClusterColFieldNamesToValues, assemblySchema.getProtobufFieldName(),
                    marshalling.encrypt(protobuf, assemblyMeta.getObjDomain()));
        }

        public Data toDAOData(AssemblyMeta assemblyMeta)
        {
            AssemblySchema assemblySchema = assemblyMeta.getAssemblySchema();

            return new Data(assemblySchema.getRowKey(), assemblyID, fromLogicalToPhysicalDataForReadAndDelete(assemblySchema),
                    nonClusterColFieldNamesToValues, assemblySchema.getProtobufFieldName(), null);
        }

        private Map<String, Object> fromLogicalToPhysicalDataForWrite(AssemblySchema assemblySchema)
        {
            int strIndex  = 1;
            int longIndex = 1;

            Map<String, Object> physicalData = new HashMap<>();
            for (AssemblySchema.AssemblyCol entry: assemblySchema.getClusteringCols())
            {
                String k = entry.getName();
                Object v = clusterColFieldNamesToValues.get(k);
                if (v == null)
                    throw new RuntimeException("We don't allow null for a clustering column values");

                AssemblySchema.ColType ct = entry.getColType();

                if (ct.equals(AssemblySchema.ColType.StringType))
                {
                    physicalData.put("col" + strIndex++ + "s", v);

                }
                else if (ct.equals(AssemblySchema.ColType.LongType))
                {
                    physicalData.put("col" + longIndex++ + "l", v);
                }
            }

            // VK: So stupid but necessary changes for clustering keys in Cassandra
            for (; strIndex <= maxStringCols; strIndex++)
            {
                physicalData.put("col" + strIndex + "s", dontCareStringValue);
            }

            // VK: So stupid but necessary changes for clustering keys in Cassandra
            for (; longIndex <= maxLongCols; longIndex++)
            {
                physicalData.put("col" + longIndex + "l", dontCareLongValue);
            }

            return physicalData;
        }

        private Map<String, Object> fromLogicalToPhysicalDataForReadAndDelete(AssemblySchema assemblySchema)
        {
            int strIndex  = 1;
            int longIndex = 1;

            boolean queryIsTerminated = false;

            Map<String, Object> physicalData = new HashMap<>();
            if (clusterColFieldNamesToValues != null)
            {
                for (AssemblySchema.AssemblyCol entry: assemblySchema.getClusteringCols())
                {
                    String k = entry.getName();
                    Object v = clusterColFieldNamesToValues.get(k);
                    if (v == null)
                    {
                        queryIsTerminated = true; // will remain true with subsequent nulls
                        continue;
                    }
                    else if (queryIsTerminated)
                    {
                        throw new RuntimeException("PRIMARY KEY column "
                                + k
                                + "cannot be restricted as preceding column(s) not restricted");
                    }

                    AssemblySchema.ColType ct = entry.getColType();

                    if (ct.equals(AssemblySchema.ColType.StringType))
                    {
                        physicalData.put("col" + strIndex++ + "s", v);

                    }
                    else if (ct.equals(AssemblySchema.ColType.LongType))
                    {
                        physicalData.put("col" + longIndex++ + "l", v);
                    }
                }

                // Backfill "dontCare" string columns
                if (longIndex > 1)
                {
                    for(int i = strIndex; i <= maxStringCols; i++) {
                        String key = "col" + i + "s";
                        physicalData.put(key, dontCareStringValue);
                    }
                }
            }

            return physicalData;
        }

        private AssemblySchema.ColType getColType(String col, AssemblySchema assemblySchema)
        {
            return Arrays.stream(assemblySchema.getClusteringCols())
                    .filter(c -> c.getName().equals(col))
                    .map(c -> c.getColType())
                    .findFirst()
                    .get();
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

        public byte[] getProtobuf()
        {
            return protobuf;
        }

        @Override
        public String toString()
        {
            return "AssemblyData{" +
                    "assemblyID=" + assemblyID +
                    '}';
        }
    }

    /**
     * Assembly Query Result contains everything needed to return data queried.
     */
    public static class AssemblyQueryResult implements Serializable
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
        private Map<String, Object> physicalColFieldNamesToValues;


        /**
         * cluster column names and corresponding values.
         * It must have at least one value
         *
         */
        private Map<String, Object> colFieldNamesToValues;

        /**
         * The table/location where the data is stored. Can't be null.
         */
        private InputStream protobuf;

        /**
         * The write time in microseconds
         */
        long timeInMicroseconds;

        // used to read a row or part of a row
        public AssemblyQueryResult(String rowKeyValue, Map<String, Object> colFieldNamesToValues, InputStream protobuf, long timeInMicroseconds)
        {
            this.rowKeyValue                   = rowKeyValue;
            this.physicalColFieldNamesToValues = colFieldNamesToValues;
            this.protobuf                      = protobuf;
            this.timeInMicroseconds            = timeInMicroseconds;
        }

        public String getRowKeyValue()
        {
            return rowKeyValue;
        }

        public Map<String, Object> getColFieldNamesToValues()
        {
            return colFieldNamesToValues;
        }

        public InputStream getProtobuf()
        {
            return protobuf;
        }

        public long getTimeInMicroseconds()
        {
            return timeInMicroseconds;
        }

        public void fromPhysicalToLogicalData(AssemblySchema assemblySchema)
        {
            colFieldNamesToValues = new HashMap<>();

            for (Map.Entry<String, Object> entry : physicalColFieldNamesToValues.entrySet())
            {
                String k = entry.getKey();
                Object v = entry.getValue();

                if (k.endsWith("s"))
                {
                    if (v.toString().equals(dontCareStringValue))
                        continue;

                    String name = getLogicalColName(k, assemblySchema);
                    colFieldNamesToValues.put(name, v);

                }
                else if (k.endsWith("l"))
                {
                    if ((Long) v == dontCareLongValue)
                        continue;

                    String name = getLogicalColName(k, assemblySchema);
                    colFieldNamesToValues.put(name, v);
                }
            }
        }

        private String getLogicalColName(String col, AssemblySchema assemblySchema)
        {
            AssemblySchema.ColType colType = col.endsWith("s") ? AssemblySchema.ColType.StringType : AssemblySchema.ColType.LongType;
            int colNumber = Integer.valueOf(col.charAt(3));
            int number    = 1;

            for (AssemblySchema.AssemblyCol assemblyCol: assemblySchema.getClusteringCols())
            {
                if (assemblyCol.getColType().equals(colType))
                {
                    if (colNumber == number)
                    {
                        return assemblyCol.getName();
                    }
                    else
                    {
                        number++;
                    }
                }
            }

            return null;
        }

        @Override
        public String toString()
        {
            return "AssemblyQueryResult{" +
                   "rowKeyValue=" + rowKeyValue +
                   ", timeInMicroseconds=" + timeInMicroseconds +
                   '}';
        }
    }

    /**
     * Write an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     */
    void write(AssemblyMeta assemblyMeta, AssemblyData assemblyData);

    /**
     * Delete a certain parts (columns) of an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     */
    void deleteColumns(AssemblyMeta assemblyMeta, AssemblyData assemblyData);

    /**
     * Delete an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     */
    void deleteRow(AssemblyMeta assemblyMeta, AssemblyData assemblyData);

    /**
     * flush all in memory data to disk
     *
     */
    void flush();

    /**
     * Read an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     * @return List of Assembly Query Result
     */
    List<AssemblyQueryResult> read(AssemblyMeta assemblyMeta, AssemblyData assemblyData);

    /**
     * get the list of dataTypes given an assemblyMeta and assemblyID
     *
     * @param assemblyMeta
     * @param assemblyID
     * @return List of dataTypeNames
     */
    List<String> getDataTypes(AssemblyMeta assemblyMeta, String assemblyID);
}