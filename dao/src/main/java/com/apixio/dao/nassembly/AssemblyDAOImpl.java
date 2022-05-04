package com.apixio.dao.nassembly;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.apixio.datasource.cassandra.CassandraResult;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.FieldValueList;
import com.apixio.datasource.cassandra.FieldValueListBuilder;
import com.apixio.datasource.cassandra.StatementGenerator;
import com.apixio.datasource.cassandra.field.StringField;
import com.apixio.utility.DataSourceUtility;

/**
 * AssemblyDAO contains all functionality for writing and reading fina;/merged/derived assembly data.  The assembly dao
 * is NOT an append only model but a (logical) overwrite model.
 *
 * The client code creates an instance of the implementation of this class and relies on the specific driver to
 * initialize the AssemblyDAO.
 */

public class AssemblyDAOImpl implements AssemblyDAO
{
    // Notes:
    //  "objDomain" is something like pdsID
    //  "assemblyID" is something like patientID or patientID/documentID
    //  "category" is something like "claims" or "subtraction"
    //
    // merged row rowkey
    //   * rowkey:   a_m/{objDomain}/{assemblyID}/{dataType}
    //   * colname:  groupingID ("merged" is a specialID when there is no grouping)
    //   * colval:   {persisted data}
    // dataTYpe row rowkey
    //   * rowkey:   a_t/{objDomain}/{assemblyID}
    //   * colname:  {dataType}
    //   * colval:   "x"

    private final static String mrkPrefix  = "a_m";
    private final static String trkPrefix  = "a_t";
    private final static byte[] existBytes = "x".getBytes(StandardCharsets.UTF_8);

    private Map<String, CqlCrud>  databaseToCqlCrud  = new HashMap<>();

    /**
     * This is a hook for the specific implementation of the driver
     *
     * @param databaseToDrivers
     */
    @Override
    public void setDrivers(Map<String, Object> databaseToDrivers)
    {
        databaseToDrivers.forEach((n, c) -> {
            CqlCrud crud =  (CqlCrud) c;
            this.databaseToCqlCrud.put(n, crud);
        });
    }

    /**
     * Write an assembly object given meta and data
     *
     * @param meta
     * @param data
     */
    @Override
    public void write(Meta meta, Data data)
    {
        CqlCrud  cqlCrud  = databaseToCqlCrud.get(meta.getDatabase());
        CqlCache cqlCache = cqlCrud.getCqlCache();

        StatementGenerator sg     = buildStatementGenerator(data);
        FieldValueList     fvList = buildFieldValueList(meta, data, true);

        DataSourceUtility.saveRawData(cqlCache, sg, fvList, meta.getTable());

        // list of dataTypeNames
        String dataTypeRowKey = buildDataTypeRowKey(meta, data.getAssemblyID());
        String dataTypeColumnName = buildDataTypeColumnName(meta.getDataTypeName());
        if (DataSourceUtility.readColumnValue(cqlCrud, dataTypeRowKey, dataTypeColumnName, meta.getDataTypeTable()) == null)
            DataSourceUtility.saveRawData(cqlCache, dataTypeRowKey, dataTypeColumnName, existBytes, meta.getDataTypeTable());
    }

    /**
     * Delete a certain parts (columns) of an assembly object given meta and data
     *
     * @param meta
     * @param data
     */
    @Override
    public void deleteColumns(Meta meta, Data data)
    {
        CqlCrud  cqlCrud  = databaseToCqlCrud.get(meta.getDatabase());
        CqlCache cqlCache = cqlCrud.getCqlCache();

        StatementGenerator sg     = buildStatementGenerator(data);
        FieldValueList     fvList = buildFieldValueList(meta, data, false);

        DataSourceUtility.deleteColumns(cqlCache, sg, fvList, meta.getTable());
    }

    /**
     * Delete an assembly object given meta and data
     *
     * @param meta
     * @param data
     */
    @Override
    public void deleteRow(Meta meta, Data data)
    {
        CqlCrud  cqlCrud  = databaseToCqlCrud.get(meta.getDatabase());
        CqlCache cqlCache = cqlCrud.getCqlCache();

        StatementGenerator sg     = buildStatementGenerator(data);
        FieldValueList     fvList = buildFieldValueList(meta, data, false);

        DataSourceUtility.deleteRow(cqlCache, sg, fvList, meta.getTable());
    }

    /**
     * Read an assembly object given meta and data
     *
     * @param meta
     * @param data
     * @return List of assembly Query Result
     */
    @Override
    public List<QueryResult> read(Meta meta, Data data)
    {
        CqlCrud  cqlCrud  = databaseToCqlCrud.get(meta.getDatabase());

        String writeTime = "writetime(" + data.getProtobufFieldName() + ")";
        Map<String, Object> newNonClustered = new HashMap<>();
        if (data.getNonClusterColFieldNamesToValues() != null)
            newNonClustered.putAll(data.getNonClusterColFieldNamesToValues());
        newNonClustered.put(writeTime, null);

        Data dataWithTS = new Data(data.getAssemblyIDFieldName(), data.getAssemblyID(), data.getClusterColFieldNamesToValues(),
                newNonClustered, data.getProtobufFieldName(), data.getProtobuf());
        List<QueryResult> queryResults = new ArrayList<>();

        StatementGenerator sg = buildStatementGenerator(dataWithTS);
        FieldValueList fvList = buildFieldValueList(meta, dataWithTS, false);
        String         query  = sg.select(meta.getTable(), fvList);

        CassandraResult cassandraResult = cqlCrud.query(query, fvList);
        CassandraResult.CassandraResultIterator  iterator = cassandraResult != null ? cassandraResult.iterator() : null;

        if (iterator == null) return queryResults;

        while (iterator.hasNext())
        {
            Map<String, Object> resultMap = cassandraResult.iterator().next();

            String     id     = (String) resultMap.get(dataWithTS.getAssemblyIDFieldName());
            ByteBuffer buffer = (ByteBuffer) resultMap.get(dataWithTS.getProtobufFieldName());
            Map<String, Object> colFieldNamesToValues = new HashMap<>();

            if (resultMap != null)
            {
                resultMap.forEach((k, v) ->
                {
                    if ( !(k.equals(dataWithTS.getAssemblyIDFieldName()) || k.equals(dataWithTS.getProtobufFieldName())) )
                    {
                        colFieldNamesToValues.put(k, v);
                    }
                });
            }

            if (id != null && buffer != null)
            {
                long ts = (Long) colFieldNamesToValues.get(writeTime);
                colFieldNamesToValues.remove(writeTime);
                queryResults.add(new QueryResult(id, colFieldNamesToValues, DataSourceUtility.getBytes(buffer), ts));
            }
        }

        return queryResults;
    }

    /**
     * flush all in memory data to disk
     *
     */
    @Override public void flush()
    {
        databaseToCqlCrud.forEach((n, c) -> c.getCqlCache().flush());
    }

    /**
     * get the list of dataTypes given meta and assemblyID
     *
     * @param meta
     * @param assemblyID
     * @return List of dataTypeNames
     */
    @Override
    public List<String> getDataTypes(Meta meta, String assemblyID)
    {
        CqlCrud  cqlCrud  = databaseToCqlCrud.get(meta.getDatabase());

        return DataSourceUtility.
                readColumns(cqlCrud, buildDataTypeRowKey(meta, assemblyID), null, null, meta.getDataTypeTable())
                .keySet()
                .stream()
                .collect(Collectors.toList());
    }

    private StatementGenerator buildStatementGenerator(Data data)
    {
        Stream<String> stream = Stream.of(data.getAssemblyIDFieldName());

        if (data.getProtobufFieldName() != null)
            stream = Stream.concat(stream, Stream.of(data.getProtobufFieldName()));

        if (data.getClusterColFieldNamesToValues() != null && !data.getClusterColFieldNamesToValues().isEmpty())
        {
            stream = Stream.concat(stream, data.getClusterColFieldNamesToValues().keySet().stream());
        }

        if (data.getNonClusterColFieldNamesToValues() != null && !data.getNonClusterColFieldNamesToValues().isEmpty())
        {
            stream = Stream.concat(stream, data.getNonClusterColFieldNamesToValues().keySet().stream());
        }

        String[] fieldNames = stream.collect(Collectors.toSet()).toArray(new String[0]);

        return new StatementGenerator(fieldNames);
    }

    private FieldValueList buildFieldValueList(Meta meta, Data data, boolean write)
    {
        FieldValueListBuilder builder = buildMergedRowKey(meta, data);

        if (data.getClusterColFieldNamesToValues() != null)
        {
            data.getClusterColFieldNamesToValues().forEach((k, v) ->
            {
                if (v instanceof String)
                {
                    builder.addString(k, (String) v);
                }
                else if (v instanceof Integer)
                {
                    Integer vInt = (Integer) v;
                    builder.addLong(k, vInt.longValue());
                }
                else if (v instanceof Long)
                {
                    builder.addLong(k, (Long) v);
                }
            });
        }

        // the boolean flag to hedge against somebody by mistake using non-clustering key value match for query/read or delete
        if (write && data.getNonClusterColFieldNamesToValues() != null)
        {
            data.getNonClusterColFieldNamesToValues().forEach((k, v) ->
            {
                if (v instanceof String)
                {
                    builder.addString(k, (String) v);
                }
                else if (v instanceof Integer)
                {
                    Integer vInt = (Integer) v;
                    builder.addLong(k, vInt.longValue());
                }
                else if (v instanceof Long)
                {
                    builder.addLong(k, (Long) v);
                }
            });
        }

        builder.addTtl(meta.getTtlFromNowInSec());

        return builder.build();
    }

    private FieldValueListBuilder buildMergedRowKey(Meta meta, Data data)
    {
        FieldValueListBuilder builder = new FieldValueListBuilder()
                .addPartitionKey(new StringField(data.getAssemblyIDFieldName()), buildMergedRowKey(meta, data.getAssemblyID()));

        if (data.getProtobuf() != null)
            builder = builder.addByteBuffer(data.getProtobufFieldName(), ByteBuffer.wrap(data.getProtobuf()));

        return builder;
    }

    // a_m/{objDomain}/{assemblyID}/{category}
    private static String buildMergedRowKey(Meta meta, String assemblyID)
    {
        NAssemblyUtils.validateNotEmpty(meta.getObjDomain());
        NAssemblyUtils.validateNotEmpty(meta.getDataTypeName());
        NAssemblyUtils.validateNotEmpty(assemblyID);

        return mrkPrefix + "/" +
                meta.getObjDomain() + "/" +
                assemblyID + "/" +
                meta.getDataTypeName();
    }

    // a_t/{objDomain}/{assemblyID}
    static String buildDataTypeRowKey(Meta meta, String assemblyID)
    {
        NAssemblyUtils.validateNotEmpty(meta.getObjDomain());
        NAssemblyUtils.validateNotEmpty(assemblyID);

        return trkPrefix + "/" +
                meta.getObjDomain() + "/" +
                assemblyID;
    }

    static String buildDataTypeColumnName(String dataTypeName)
    {
        NAssemblyUtils.validateNotEmpty(dataTypeName);

        return dataTypeName;
    }
}
