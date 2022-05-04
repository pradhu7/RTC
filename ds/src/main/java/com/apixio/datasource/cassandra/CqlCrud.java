package com.apixio.datasource.cassandra;

import com.apixio.datasource.cassandra.CqlRowData.CqlOps;
import com.apixio.datasource.cassandra.StatementGenerator.CREATE_TYPE;
import com.apixio.datasource.cassandra.field.ByteBufferField;
import com.apixio.datasource.cassandra.field.Field;
import com.apixio.datasource.cassandra.field.LongField;
import com.apixio.datasource.cassandra.field.StringArrayField;
import com.apixio.datasource.cassandra.field.StringField;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.helpers.LogLog;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static com.apixio.datasource.cassandra.StatementGenerator.CREATE_TYPE.*;
import static com.apixio.datasource.cassandra.StatementGenerator.ROWKEY_STRING;


public class CqlCrud
{
    // Note: Leave blank space at the begin and end of each statement (table creation, insert and query)
    // 1. rowkey and columns are text/string
    // 2. value is a blob
    // 3. it uses the concept of cql composite keys to represent dynamic columns

    private final static int  maxRetries   = 10;
    private final static long retrySleep   = 250L;
    private final static long createSleep  = 30 * 1000L;
    private final static int  fetchSize     = 5000;

    // make sure to cache all prepared statements unless batch operation

    private static final String queryTableNames = " select keyspace_name, columnfamily_name from system.schema_columnfamilies ;  ";

    private static final String queryGivenColumns = "select col, d from %s where rowkey = ? and col in (%s) order by col ASC ";  // the "col in (%s)" is a bit of a hack

    private static final String queryBoundColumnsAsc = " select col, d, writetime(d) from %s where rowkey = ? and col >= ? and col <= ?  order by col ASC LIMIT %d ";
    private static final String queryAllColumnsAsc = " select col, d, writetime(d) from %s where rowkey = ?  order by col ASC LIMIT %d ";

    private static final String queryGivenRows = "select rowkey, col, d from %s where rowkey in ? ";

    // This to statements are used to iterate over keys and columns
    private static final String querySingleBoundColumnsAsc = " select col, d from %s where rowkey = ? and col > ? order by col ASC LIMIT %d ";
    private static final String querySingleBoundColumnsDesc = " select col, d from %s where rowkey = ? and col < ? order by col DESC LIMIT %d ";

    // THIS IS USEFUL AND SHOULD BE USED. Token() is a function that takes a key and returns a token
    private static final String queryRowScan1 = " select distinct rowkey from %s WHERE token(rowkey) > token(?) LIMIT %d ";

    // THIS IS ALMOST USELESS UNLESS YOU KNOW THE TOKEN
    private static final String queryRowScan2 = " select distinct rowkey from %s WHERE token(rowkey) >= ? and token(rowkey) <= ? LIMIT %d ";

    private CqlConnector cqlConnector;
    private CqlMonitor   cqlMonitor;
    private TraceCqlCrud traceCqlCrud;
    private CqlCache     cqlCache;
    private boolean      isTraceEnabled;
    private boolean      isStatementCacheEnabled = true;
    private boolean      isBatchSyncEnabled;

    private static volatile Cache<String, PreparedStatement> cqlStringToPreparedStatement = CacheBuilder.newBuilder().maximumSize(4096).build();

    public void setCqlConnector(CqlConnector c)
    {
        cqlConnector = c;
    }
    public void setCqlMonitor(CqlMonitor cqlMonitor)
    {
        this.cqlMonitor = cqlMonitor;
    }
    public void setTraceCqlCrud(TraceCqlCrud traceCqlCrud)
    {
        this.traceCqlCrud = traceCqlCrud;
    }
    public void setCqlCache(CqlCache cqlCache)
    {
        this.cqlCache = cqlCache;
    }
    public void setTraceEnabled(boolean t)
    {
        isTraceEnabled = t;
    }
    public void setStatementCacheEnabled(boolean c)
    {
        isStatementCacheEnabled = c;
    }
    public void setBatchSyncEnabled(boolean c)
    {
        isBatchSyncEnabled = c;
    }

    public CqlConnector getCqlConnector()
    {
        return cqlConnector;
    }
    public TraceCqlCrud getTraceCqlCrud()
    {
        return traceCqlCrud;
    }
    public CqlCache getCqlCache()
    {
        return cqlCache;
    }
    public CqlMonitor getCqlMonitor()
    {
        return cqlMonitor;
    }
    public boolean isBatchSyncEnabled()
    {
        return isBatchSyncEnabled;
    }
    private Session getSession()
    {
        return cqlConnector.getSession();
    }
    public ConsistencyLevel getReadCL()
    {
        return cqlConnector.getReadCL();
    }
    public ConsistencyLevel getWriteCL()
    {
        return cqlConnector.getWriteCL();
    }

    // Supports "legacy" static table definitons..
    private static final StatementGenerator legacyStatementGenerator = new StatementGenerator("rowkey", "col", "d");
    private static final FieldValueList legacyFieldValueList = new FieldValueListBuilder().addString("rowkey").addString("col").addByteBuffer("d").build();

    public void close()
    {
        cqlConnector.close();
    }

    /**
     *
     * @return Verify if schema is consistent
     */
    public boolean verifySchemaConsistency()
    {
        Session session = getSession();
        Cluster cluster = session.getCluster();

        return cluster.getMetadata().checkSchemaAgreement();
    }

    /**
     * Creates a table.
     *
     * @param createType
     * @param statementGenerator
     * @param tableName
     * @param fieldValueList
     * @param partitionKey
     * @param clusterKey
     * @return
     */
    public boolean createTable(CREATE_TYPE createType, StatementGenerator statementGenerator,
                               String tableName, FieldValueList fieldValueList, String partitionKey, String clusterKey)
    {
        List<String>clusterKeys = new ArrayList();
        if (StringUtils.isNotBlank(clusterKey)) clusterKeys.add(clusterKey);

        return createTable(createType, statementGenerator, tableName, fieldValueList, partitionKey, clusterKeys);
    }

    /**
     * Creates a table.
     *
     * @param createType
     * @param statementGenerator
     * @param tableName
     * @param fieldValueList
     * @param partitionKey
     * @param clusterKeys
     * @return
     */
    public boolean createTable(CREATE_TYPE createType, StatementGenerator statementGenerator,
                               String tableName, FieldValueList fieldValueList, String partitionKey, List<String> clusterKeys)
    {
        if(!isValidCreateTableRequest(fieldValueList, partitionKey))
            throw new RuntimeException("Error: Create table request is invalid, check syntax");

        if (tableExists(tableName))
            return false;

        String createStatement = statementGenerator.create(createType, tableName, fieldValueList, partitionKey, clusterKeys);

        if (createTable(createStatement) && tableExists(tableName))
            return true;

        throw new CqlException("VERY DANGEROUS ERROR - Can't create table: " + tableName);
    }


    /**
     * Given a table name, it creates a leveled compaction strategy table according to the
     * table definition
     * It returns false if table exists
     *
     * @param tableName
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    @Deprecated
    public boolean createTableLeveled(String tableName)
    {
        return createTable(LEVELED, legacyStatementGenerator, tableName, legacyFieldValueList, "rowkey", "col");
    }

    /**
     * Given a table name, it creates a size tiered compaction strategy table according to the
     * table definition
     * It returns false if table exists
     *
     * @param tableName
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    @Deprecated
    public boolean createSizeTieredTable(String tableName)
    {
        return createTable(SIZE_TIERED, legacyStatementGenerator, tableName, legacyFieldValueList, "rowkey", "col");
    }

    private boolean createTable(String createStatement)
    {
        Session session = getSession();
        ConsistencyLevel level = ConsistencyLevel.ALL;

        try
        {
            SimpleStatement simpleStatement = new SimpleStatement(createStatement);
            simpleStatement.setConsistencyLevel(level);

            ResultSet resultSet = session.execute(simpleStatement);
            if (resultSet != null)
            {
                sleep(createSleep);
                return true;
            }

            throw new CqlException("VERY DANGEROUS ERROR - No schema agreement - Can't create table: " + createStatement);

        }
        catch (RuntimeException e)
        {
            throw new CqlException(e);
        }
    }

    public boolean dropTable(String tableName)
    {
        if (!tableExists(tableName))
            return true;

        Session session = getSession();
        ConsistencyLevel level = ConsistencyLevel.ALL;

        try
        {
            String createStatement = String.format("drop table %s; ", tableName);

            SimpleStatement simpleStatement = new SimpleStatement(createStatement);
            simpleStatement.setConsistencyLevel(level);

            ResultSet resultSet = session.execute(simpleStatement);

            return(resultSet != null);
        }
        catch (RuntimeException e)
        {
            throw new CqlException(e);
        }
    }

    /**
     * Returns All table names including keyspace name
     *
     *
     * @return Map of table name and keyspace name
     *
     * @throws CqlException
     */
    public Map<String, String> describeTables()
    {
        Session session = getSession();
        ConsistencyLevel level = getReadCL();

        PreparedStatement preparedStatement = getPreparedStatement(session, queryTableNames);

        BoundStatement boundStatement = new BoundStatement(preparedStatement);
        boundStatement.setConsistencyLevel(level);

        if (isTraceEnabled)
            boundStatement.enableTracing();

        Map<String, String> tableToKeyspace = new HashMap<>();

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                for (Row row : resultSetFuture.getUninterruptibly())
                {
                    tableToKeyspace.put(row.getString("columnfamily_name"), row.getString("keyspace_name"));
                }

                return tableToKeyspace;
            }
            catch (RuntimeException e)
            {
                mx = e;
                e.printStackTrace();
                sleep(retrySleep);
            }
        }

        // The last exception for debugging purposes.
        throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
    }

    /**
     * Allows ad-hoc querying. By default this will use the default fetchSize.
     *
     * @param queryStatement
     * @return
     */
    public CassandraResult query(String queryStatement, FieldValueList fieldValueList)
    {
        return query(queryStatement, fieldValueList, fetchSize);
    }

    /**
     * Allows ad-hoc querying. Allows for user specified fetchSize.
     *
     * @param queryStatement
     * @param fetchSize
     * @return
     */
    public CassandraResult query(String queryStatement, FieldValueList fieldValueList, int fetchSize)
    {
        CqlMonitor.Timer  gTimer  = CqlMonitor.getTimer();
        ColumnValueResult result  = null;

        try
        {
            Session session = getSession();
            ConsistencyLevel level = getReadCL();

            PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

            BoundStatement boundStatement = new BoundStatement(preparedStatement);
            boundStatement.setFetchSize(fetchSize);
            boundStatement.setConsistencyLevel(level);

            int pos = 0;
            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case LongField.TYPE:
                        boundStatement.setLong(pos, (Long) fieldValue.getValue());
                        break;
                    case StringField.TYPE:
                        boundStatement.setString(pos, (String) fieldValue.getValue());
                        break;
                    case StringArrayField.TYPE:
                        String [] value = (String [])fieldValue.getValue();
                        boundStatement.setList(pos, Arrays.asList(value));
                        break;
                    case ByteBufferField.TYPE:
                        boundStatement.setBytes(pos, (ByteBuffer) fieldValue.getValue());
                        break;
                    default:
                        throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                }
                pos = pos+1;
            }

            if (isTraceEnabled)
                boundStatement.enableTracing();

            Exception mx = null;
            for (int attempt = 0; attempt < maxRetries; attempt++)
            {
                try
                {
                    ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                    UUID queryTraceID = isTraceEnabled ? resultSetFuture.getUninterruptibly().getAllExecutionInfo().get(0).getQueryTrace().getTraceId() : null;
                    return new CassandraResult(queryTraceID, resultSetFuture.getUninterruptibly());
                }
                catch (Exception e)
                {
                    mx = e;
                    e.printStackTrace();
                    sleep(retrySleep);
                }
            }

            // The last exception for debugging purposes.
            throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());

        }
        finally
        {
            CqlMonitor.recordGlobalTime(gTimer, result != null ? 1 : 0);
        }
    }

    /**
     * True if taleName exists; false otherwise
     *
     * @param tableName
     * @return
     * @throws CqlException
     */
    public boolean tableExists(String tableName)
    {
        Session session  = getSession();
        String  keyspace = session.getLoggedKeyspace();

        Map<String, String> tableToKeyspace = describeTables();

        if (tableToKeyspace == null)
            return false;

        for (Map.Entry<String, String> entry : tableToKeyspace.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(tableName) && entry.getValue().equalsIgnoreCase(keyspace))
                return true;
        }

        return false;
    }

    /**
     * Given a list of rows, it inserts or deletes them asynchronously in a single batch.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param cqlRowDataList
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    public boolean insertOrDeleteOps(List<CqlRowData> cqlRowDataList)
    {
        return insertOrDeleteOpsGuts(cqlRowDataList, false, false);
    }

    private boolean insertOrDeleteOpsGuts(List<CqlRowData> cqlRowDataList, boolean useAsyncVariable, boolean useAsync)
    {
        Session session = getSession();
        ConsistencyLevel level = getWriteCL();

        try
        {
            BatchStatement batch = new BatchStatement();
            batch.setConsistencyLevel(level);
            if (isTraceEnabled)
                batch.enableTracing();

            for (CqlRowData cqlRowData : cqlRowDataList)
            {
                BoundStatement boundStatement = null;

                StatementGenerator statementGenerator = cqlRowData.getStatementGenerator();
                FieldValueList fieldValueList = cqlRowData.getFieldValueList();

                if (cqlRowData.ops == CqlOps.insertIfNotExists)
                {
                    //
                    // There are performance issues using the native if not exists functionality because of internal
                    // paxos implementation in cassandra.
                    //
                    // We use a synchronious read, check method to try to avoid writes.
                    //
                    // This is not perfect, in that there can be race conditions if two clients do a read around the same time,
                    // and then both decide to do a write, you'll get one overwriting the other, but for our purposes
                    // this is good enough - since we're making an attempt to avoid tombstoning
                    //
                    // This synchronious read/write will likely outperform paxos, and when it's fix we have the framework
                    // to use the if not exists..
                    //
                    CassandraResult result = null;

                    try
                    {
                        String select = statementGenerator.select(cqlRowData.getTableName(), cqlRowData.fieldValueList);
                        result = this.query(select, statementGenerator.getQueryFieldValueList(cqlRowData.fieldValueList), 1);
                    }
                    catch(Exception ex)
                    {
                        ex.printStackTrace();
                        //ignore...
                    }

                    if (result != null && !result.iterator().hasNext())
                    {
                        String insertStatement = statementGenerator.insert(cqlRowData.getTableName(), true, cqlRowData.fieldValueList.getTtl());
                        PreparedStatement preparedStatement = getPreparedStatement(session, insertStatement);
                        boundStatement = new BoundStatement(preparedStatement);

                        for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
                        {
                            switch (fieldValue.getKey().getType())
                            {
                                case LongField.TYPE:
                                    boundStatement.setLong(fieldValue.getKey().getName(), (Long) fieldValue.getValue());
                                    break;
                                case StringArrayField.TYPE:
                                    String [] value = (String [])fieldValue.getValue();
                                    boundStatement.setList(fieldValue.getKey().getName(), Arrays.asList(value));
                                    break;
                                case StringField.TYPE:
                                    boundStatement.setString(fieldValue.getKey().getName(), (String) fieldValue.getValue());
                                    break;
                                case ByteBufferField.TYPE:
                                    boundStatement.setBytes(fieldValue.getKey().getName(), (ByteBuffer) fieldValue.getValue());
                                    break;
                                default:
                                    throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                            }
                        }
                    }
                }
                else if (cqlRowData.ops == CqlOps.insert)
                {
                    String insertStatement = statementGenerator.insert(cqlRowData.getTableName(), cqlRowData.fieldValueList.getTtl());
                    PreparedStatement preparedStatement = getPreparedStatement(session, insertStatement);
                    boundStatement = new BoundStatement(preparedStatement);

                    for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
                    {
                        switch (fieldValue.getKey().getType())
                        {
                            case LongField.TYPE:
                                boundStatement.setLong(fieldValue.getKey().getName(), (Long) fieldValue.getValue());
                                break;
                            case StringField.TYPE:
                                boundStatement.setString(fieldValue.getKey().getName(), (String) fieldValue.getValue());
                                break;
                            case StringArrayField.TYPE:
                                String [] value = (String [])fieldValue.getValue();
                                boundStatement.setList(fieldValue.getKey().getName(), Arrays.asList(value));
                                break;
                            case ByteBufferField.TYPE:
                                boundStatement.setBytes(fieldValue.getKey().getName(), (ByteBuffer) fieldValue.getValue());
                                break;
                            default:
                                throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                        }
                    }
                }
                else if (cqlRowData.ops == CqlOps.deleteRow)
                {
                    String deleteRowStatement = statementGenerator.deleteRow(cqlRowData.getTableName(), fieldValueList);
                    PreparedStatement preparedStatement = getPreparedStatement(session, deleteRowStatement);
                    boundStatement = new BoundStatement(preparedStatement);

                    for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
                    {
                        switch (fieldValue.getKey().getType())
                        {
                            case LongField.TYPE:
                                boundStatement.setLong(fieldValue.getKey().getName(), (Long) fieldValue.getValue());
                                break;
                            case StringField.TYPE:
                                if (fieldValue.getKey().getName().equals(ROWKEY_STRING))
                                {
                                    boundStatement.setString(fieldValue.getKey().getName(), (String) fieldValue.getValue());
                                }
                                break;
                            case StringArrayField.TYPE:
                                if (fieldValue.getKey().getName().equals(ROWKEY_STRING))
                                {
                                    String[] value = (String[]) fieldValue.getValue();
                                    boundStatement.setList(fieldValue.getKey().getName(), Arrays.asList(value));
                                }
                                break;
                            case ByteBufferField.TYPE:
                                //ignore, this is the actual value
                                break;
                            default:
                                throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                        }
                    }
                }
                else if (cqlRowData.ops == CqlOps.deleteColumn)
                {
                    String deleteRowStatement = statementGenerator.deleteColumn(cqlRowData.getTableName(), fieldValueList);
                    PreparedStatement preparedStatement = getPreparedStatement(session, deleteRowStatement);
                    boundStatement = new BoundStatement(preparedStatement);

                    for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
                    {
                        switch (fieldValue.getKey().getType())
                        {
                            case LongField.TYPE:
                                boundStatement.setLong(fieldValue.getKey().getName(), (Long) fieldValue.getValue());
                                break;
                            case StringField.TYPE:
                                boundStatement.setString(fieldValue.getKey().getName(), (String) fieldValue.getValue());
                                break;
                            case StringArrayField.TYPE:
                                String[] value = (String[]) fieldValue.getValue();
                                boundStatement.setList(fieldValue.getKey().getName(), Arrays.asList(value));
                                break;
                            case ByteBufferField.TYPE:
                                //ignore, this is the actual value
                                break;
                            default:
                                throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                        }
                    }
                }

                // Don't add to batch or trace if there's nothing to add.
                if (boundStatement != null)
                {
                    batch.add(boundStatement);

                    // VK: We only support tracing of inserts. No deletes
                    if (isTraceEnabled && (cqlRowData.ops == CqlOps.insert || cqlRowData.ops == CqlOps.insertIfNotExists))
                        traceCqlCrud.addToTrace(cqlRowData);

                }
            }

            if (isBatchSyncEnabled || (useAsyncVariable && !useAsync) )
            {
                ResultSet resultSet = session.execute(batch);

                return (resultSet != null);
            }
            else
            {
                ResultSetFuture resultSetFuture = session.executeAsync(batch);

                return resultSetFuture != null;
            }
        }
        catch (RuntimeException e)
        {
            throw new CqlException(e);
        }
    }

    /**
     * It inserts a dynamic row given a table name, rowkey, a column and its value. It could insert the column in
     * asynchronous mode if the useAsync is set to true.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param column
     * @param value
     * @param useAsync
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    @Deprecated
    public boolean insertRow(String tableName, String rowkey, String column, ByteBuffer value, boolean useAsync)
    {
        return insertRow(tableName, legacyStatementGenerator, new FieldValueListBuilder()
                .addString("rowkey", rowkey)
                .addString("col", column)
                .addByteBuffer("d", value)
                .build(), true);
    }

    public boolean insertRow(String tableName, StatementGenerator statementGenerator, FieldValueList fieldValueList, boolean useAsync)
    {
        CqlRowData cqlRowData = new CqlRowData(tableName, statementGenerator, fieldValueList);
        cqlRowData.setCqlOps(CqlOps.insert);
        return insertOrDeleteOpsGuts(Arrays.asList(cqlRowData), true, useAsync);
    }

    /**
     * Given a list of rows, it inserts them asynchronously in a single batch. Each row consists of a table name,
     * rowkey, a column and its value.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param cqlRowDataList
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */

    public boolean insertRows(List<CqlRowData> cqlRowDataList)
    {
        for (CqlRowData cqlRowData : cqlRowDataList)
        {
            cqlRowData.setCqlOps(CqlOps.insert);
        }

        return insertOrDeleteOps(cqlRowDataList);
    }

    /**
     * It deletes a row given a table name and a rowkey. It could delete the row in
     * asynchronous mode if the useAsync is set to true.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param useAsync
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    public boolean deleteRow(String tableName, String rowkey, boolean useAsync)
    {
        CqlRowData cqlRowData = new CqlRowData(tableName, rowkey, null, null);
        cqlRowData.setCqlOps(CqlOps.deleteRow);
        return insertOrDeleteOpsGuts(Arrays.asList(cqlRowData), true, useAsync);
    }

    /**
     * Given a list of rows, it deletes them asynchronously in a single batch. Each row consists of a table name and
     * a rowkey.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param cqlRowDataList
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    public boolean deleteRows(List<CqlRowData> cqlRowDataList)
    {
        for (CqlRowData cqlRowData : cqlRowDataList)
        {
            cqlRowData.setCqlOps(CqlOps.deleteRow);
        }

        return insertOrDeleteOps(cqlRowDataList);
    }

    /**
     * It deletes a column given a table name, a rowkey, and column name. It could delete the column in
     * asynchronous mode if the useAsync is set to true.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param column
     * @param useAsync
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    public boolean deleteColumn(String tableName, String rowkey, String column, boolean useAsync)
    {
        CqlRowData cqlRowData = new CqlRowData(tableName, rowkey, column, null);
        cqlRowData.setCqlOps(CqlOps.deleteColumn);
        return insertOrDeleteOpsGuts(Arrays.asList(cqlRowData), true, useAsync);
    }

    /**
     * Given a list of rows/columns, it deletes them asynchronously in a single batch. Each element of the list
     * consists of a table name, a rowkey, and a column name.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param cqlRowDataList
     *
     * @return true if successful false otherwise
     *
     * @throws CqlException
     */
    public boolean deleteColumns(List<CqlRowData> cqlRowDataList)
    {
        for (CqlRowData cqlRowData : cqlRowDataList)
        {
            cqlRowData.setCqlOps(CqlOps.deleteColumn);
        }

        return insertOrDeleteOps(cqlRowDataList);
    }

    /**
     * Given a table, a row key and a column name, it returns the value of the column
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param column
     *
     * @return  byte buffer or null
     *
     * @throws CqlException
     */
    public ByteBuffer getColumnValue(String tableName, String rowkey, String column)
    {
        CqlMonitor.Timer  timer   = null;
        CqlMonitor.Timer  gTimer  = CqlMonitor.getTimer();
        ColumnValueResult result  = null;
        boolean           isMonitored = (cqlMonitor != null);

        if (isMonitored)
            timer = cqlMonitor.getTimer("getColumnValue({}, {}, {})", tableName, rowkey, column);

        try
        {
            result = getColumnValueGuts(tableName, rowkey, column);

            return result.value;
        }
        finally
        {
            CqlMonitor.recordGlobalTime(gTimer, result != null ? 1 : 0);

            if (isMonitored)
                cqlMonitor.recordTime(timer, result != null ? result.queryTraceID : null);
        }
    }

    public ByteBuffer getColumnValue(String tableName, StatementGenerator statementGenerator, FieldValueList fieldValueList)
    {
        CqlMonitor.Timer  gTimer  = CqlMonitor.getTimer();
        ColumnValueResult result  = null;

        try
        {
            result = getColumnValueGuts(tableName, statementGenerator, fieldValueList);

            return result.value;
        }
        finally
        {
            CqlMonitor.recordGlobalTime(gTimer, result != null ? 1 : 0);
        }
    }


    private static class ColumnValueResult
    {
        ColumnValueResult(UUID queryTraceID, ByteBuffer value)
        {
            this.queryTraceID = queryTraceID;
            this.value        = value;
        }

        UUID       queryTraceID;
        ByteBuffer value;
    }

    private ColumnValueResult getColumnValueGuts(String tableName, StatementGenerator statementGenerator, FieldValueList fieldValueList)
    {
        Session session = getSession();
        ConsistencyLevel level = getReadCL();

        String queryStatement = statementGenerator.select(tableName, fieldValueList);

        PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

        BoundStatement boundStatement = new BoundStatement(preparedStatement);
        boundStatement.setConsistencyLevel(level);
        if (isTraceEnabled)
            boundStatement.enableTracing();

        for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
        {
            switch (fieldValue.getKey().getType())
            {
                case LongField.TYPE:
                    boundStatement.setLong(fieldValue.getKey().getName(), (Long) fieldValue.getValue());
                    break;
                case StringField.TYPE:
                    boundStatement.setString(fieldValue.getKey().getName(), (String) fieldValue.getValue());
                    break;
                case StringArrayField.TYPE:
                    String[] value = (String[]) fieldValue.getValue();
                    boundStatement.setList(fieldValue.getKey().getName(), Arrays.asList(value));
                    break;
                case ByteBufferField.TYPE:
                    //ignore, this is the actual value
                    break;
                default:
                    throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + "] is not a supported Type");
            }
        }

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                UUID queryTraceID = isTraceEnabled ? resultSetFuture.getUninterruptibly().getAllExecutionInfo().get(0).getQueryTrace().getTraceId() : null;

                for (Row row : resultSetFuture.getUninterruptibly())
                {
                    return new ColumnValueResult(queryTraceID, row.getBytes("d"));
                }

                return new ColumnValueResult(queryTraceID, null);
            }
            catch (Exception nhae)
            {
                mx = nhae;
                nhae.printStackTrace();
                sleep(retrySleep);
            }
        }

        // The last exception for debugging purposes.
        throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
    }

    private ColumnValueResult getColumnValueGuts(String tableName, String rowkey, String column)
    {
        return getColumnValueGuts(tableName, legacyStatementGenerator, new FieldValueListBuilder()
                .addPartitionKey(new StringField("rowkey"),rowkey)
                .addColumn(new StringField("col"), column).build());
    }

    /**
     * Given a table, a row key and a list of columns, it returns all the columns that match the query. The result returned
     * is a map of column name and column value.
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param columns
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */
    public SortedMap<String, ByteBuffer> getColumnValues(String tableName, String rowkey, List<String> columns)
    {
        Session session = getSession();
        ConsistencyLevel level = getReadCL();

        // Note that BoundStatement doesn't support setting the columns via .setList(x, List<String>)
        // because at runtime it causes:
        //
        //    com.datastax.driver.core.exceptions.InvalidTypeException: Column col is of type varchar, cannot set to a list
        //
        // This is expected due to the fact that all table schemas force 'col' column to be the same type.
        // Substitutin the quoted string list before making it a BoundStatement avoids the problem.  Ugly
        String queryStatement = String.format(queryGivenColumns, tableName, asQuotedStrList(columns));

        PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

        BoundStatement boundStatement = new BoundStatement(preparedStatement);
        boundStatement.setConsistencyLevel(level);
        if (isTraceEnabled)
            boundStatement.enableTracing();

        boundStatement.setString(0, rowkey);

        SortedMap<String, ByteBuffer> colToValue = new TreeMap<String, ByteBuffer>();

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                for (Row row : resultSetFuture.getUninterruptibly())
                {
                    colToValue.put(row.getString("col"), row.getBytes("d"));
                }

                return colToValue;
            }
            catch (RuntimeException e)
            {
                mx = e;
                e.printStackTrace();
                colToValue.clear();
                sleep(retrySleep);
            }
        }

        // The last exception for debugging purposes.
        throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
    }

    /**
     * This will work only for strings that don't have single-quotes in them...
     */
    private String asQuotedStrList(List<String> cols)
    {
        StringBuilder sb = new StringBuilder();

        for (String col : cols)
        {
            if (sb.length() > 0)
                sb.append(",");
            sb.append("'");
            sb.append(col);
            sb.append("'");
        }

        return sb.toString();
    }

    /**
     * Given a table, a row key and a pattern, it returns all the patterns that match the query (inclusive search).
     * The result returned is a map of column name and column value.
     *
     * Important: The pattern has to be a prefix to the column name. If it is the entire column name, then you should
     * use getColumnValue method
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param pattern
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */
    public SortedMap<String, ByteBuffer> getColumnsMap(String tableName, String rowkey, String pattern)
    {
        return getColumnsMap(tableName, rowkey, pattern, pattern, true, 10000);
    }

    /**
     * Given a table, a row key, a pattern, and limit it returns all the patterns that match the query (inclusive search).
     * The result returned is a map of column name and column value.
     *
     * Important: The pattern has to be a prefix to the column name. If it is the entire column name, then you should
     * use getColumnValue method
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param pattern
     * @param limit
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */
    public SortedMap<String, ByteBuffer> getColumnsMap(String tableName, String rowkey, String pattern, int limit)
    {
        return getColumnsMap(tableName, rowkey, pattern, pattern, true, limit);
    }


    /**
     * Given a table, a row key and a start and end column names, it returns all the column/values that match the query
     * (inclusive search). The result returned is a map of column name and column value.
     *
     * Note: If start and end is null, it returns all columns/values
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param start
     * @param end
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */
    public SortedMap<String, ByteBuffer> getColumnsMap(String tableName, String rowkey, String start, String end)
    {
        return getColumnsMap(tableName, rowkey, start, end, false, 10000);
    }

    /**
     * Given a table, a row key and a start and end column names, and limit, it returns all the column/values that
     * match the query (inclusive search). The result returned is a map of column name and column value.
     *
     * Note: If start and end is null, it returns all columns/values
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param start
     * @param end
     * @param limit
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */
    public SortedMap<String, ByteBuffer> getColumnsMapWithPattern(String tableName, String rowkey, String start, String end, int limit)
    {
        return getColumnsMap(tableName, rowkey, start, end, true, limit);
    }

    /**
     * Given a table, a row key and a start and end column names, it returns all the column/values that match the query
     * (inclusive search). The result returned is a map of column name and column value.
     *
     * Note: If start and end is null, it returns all columns/values
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param start
     * @param end
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */
    public SortedMap<String, ByteBuffer> getColumnsMapWithPattern(String tableName, String rowkey, String start, String end)
    {
        return getColumnsMap(tableName, rowkey, start, end, true, 10000);
    }

    /**
     * Given a table, a row key and a start and end column names, and limit, it returns all the column/values that
     * match the query (inclusive search). The result returned is a map of column name and column value.
     *
     * Note: If start and end is null, it returns all columns/values
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param start
     * @param end
     * @param limit
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */
    public SortedMap<String, ByteBuffer> getColumnsMap(String tableName, String rowkey, String start, String end, int limit)
    {
        return getColumnsMap(tableName, rowkey, start, end, false, limit);
    }


    /**
     * Given a table, a row key and a start and end column names, and limit, it returns all the column/values that
     * match the query (inclusive search). The result returned is a map of column name and column value.
     *
     * Note: If start and end is null, it returns all columns/values
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rowkey
     * @param start
     * @param end
     * @param isPattern
     * @param limit
     *
     * @return  a map of column name and column value or an empty map
     *
     * @throws CqlException
     */

    private SortedMap<String, ByteBuffer> getColumnsMap(String tableName, String rowkey, String start, String end, boolean isPattern, int limit)
    {
        CqlMonitor.Timer  timer       = null;
        CqlMonitor.Timer  gTimer      = CqlMonitor.getTimer();
        ColumnMapResult   result      = null;
        boolean           isMonitored = (cqlMonitor != null);

        if (isMonitored)
            timer = cqlMonitor.getTimer("getColumnsMap({}, {}, {}, {}, {}, {}, {})", tableName, rowkey, start, end, isPattern, limit);

        try
        {
            result = getColumnsMapGuts(tableName, rowkey, start, end, isPattern, limit);

            return result.value;
        }
        finally
        {
            CqlMonitor.recordGlobalTime(gTimer, (result != null && result.value != null) ? result.value.size() : 0);

            if (isMonitored)
                cqlMonitor.recordTime(timer, result != null ? result.queryTraceID : null);
        }
    }

    // for testing purposes
    public ColumnMapResult getColumnsMapWithWriteTime(String tableName, String rowkey)
    {
        return getColumnsMapGuts(tableName, rowkey, null, null, false, 10000);
    }

    public static class ColumnMapResult
    {
        public ColumnMapResult(UUID queryTraceID, SortedMap<String, ByteBuffer> value, SortedMap<String, Long> time)
        {
            this.queryTraceID = queryTraceID;
            this.value        = value;
            this.time         = time;
        }

        public UUID                          queryTraceID;
        public SortedMap<String, ByteBuffer> value;
        public SortedMap<String, Long>       time;
    }


    private ColumnMapResult getColumnsMapGuts(String tableName, String rowkey, String start, String end, boolean isPattern, int limit)
    {
        Session session        = getSession();
        ConsistencyLevel level = getReadCL();

        if (start == null)
            start = "";

        String            cql               = (end != null) ? queryBoundColumnsAsc : queryAllColumnsAsc;
        String            queryStatement    = String.format(cql, tableName, limit);
        PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

        BoundStatement boundStatement = new BoundStatement(preparedStatement);
        boundStatement.setConsistencyLevel(level);
        if (isTraceEnabled)
            boundStatement.enableTracing();

        boundStatement.setString("rowkey", rowkey);

        // VK: I have no idea why I can't specify col and I have to specify positions. CQL Sucks!!!
        if (end != null)
        {
            if (isPattern)
            {
                boundStatement.setString(1, start + "!");
                boundStatement.setString(2, end + "~");
            }
            else
            {
                boundStatement.setString(1, start);
                boundStatement.setString(2, end);
            }
        }

        SortedMap<String, ByteBuffer> colToValue = new TreeMap<>();
        SortedMap<String, Long>       colToTime  = new TreeMap<>();

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                UUID queryTraceID = isTraceEnabled ? resultSetFuture.getUninterruptibly().getAllExecutionInfo().get(0).getQueryTrace().getTraceId() : null;

                for (Row row : resultSetFuture.getUninterruptibly())
                {
                    String col = row.getString("col");
                    colToValue.put(col, row.getBytes("d"));
                    colToTime.put(col, row.getLong(2));
                }

                return new ColumnMapResult(queryTraceID, colToValue, colToTime);
            }
            catch (RuntimeException e)
            {
                mx = e;
                e.printStackTrace();
                colToValue.clear();
                sleep(retrySleep);
            }
        }

        // The last exception for debugging purposes.
        throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
    }

    /**
     * Given a table and a list of rows, it returns all the rows that match the query. The result returned is a map of
     * <rowkey, list> which each element of the list is a <column name, value>
     *
     * If trace is enabled, it writes data to trace tables.
     *
     * @param tableName
     * @param rows
     *
     * @return  a map of <rowkey, list> which each element of the list is a <column name, value> or an empty map
     *
     * @throws CqlException
     */
    // VK: DOES NOT WORK!!!
    public Map<String, List<Map<String, ByteBuffer>>> getRowsMap(String tableName, List<String> rows)
    {
        Session session = getSession();
        ConsistencyLevel level = getReadCL();

        String queryStatement = String.format(queryGivenRows, tableName);
        PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

        BoundStatement boundStatement = new BoundStatement(preparedStatement);
        boundStatement.setConsistencyLevel(level);
        if (isTraceEnabled)
            boundStatement.enableTracing();

        boundStatement.setList(0, rows);

        Map<String, List<Map<String, ByteBuffer>>> rowToCol = new HashMap<String, List<Map<String, ByteBuffer>>>();

        Exception mx = null;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                for (Row row : resultSetFuture.getUninterruptibly())
                {
                    String key = row.getString("rowkey");

                    Map<String, ByteBuffer> colToVal = new HashMap<String, ByteBuffer>();
                    colToVal.put(row.getString("col"), row.getBytes("d"));

                    List<Map<String, ByteBuffer>> columns = rowToCol.get(key);
                    if (columns == null)
                    {
                        columns = new ArrayList<Map<String, ByteBuffer>>();
                        rowToCol.put(key, columns);
                    }

                    columns.add(colToVal);
                }

                return rowToCol;

            }
            catch (RuntimeException e)
            {
                mx = e;
                e.printStackTrace();
                rowToCol.clear();
                sleep(retrySleep);
            }
        }

        // The last exception for debugging purposes.
        throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
    }

    public static abstract class ColumnFilter
    {
        public abstract boolean filter(String column);
    }

    /**
     * Given a table and a rowkey, get all the columns of a key
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey)
    {
        return getAllColumns(tableName, rowkey, null, null, 0);
    }

    /**
     * Given a table and a rowkey and bufferSize, get all the columns of a key
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey, int bufferSize)
    {
        return getAllColumns(tableName, rowkey, null, null, bufferSize);
    }

    /**
     * Given a table, a rowkey, and a starting column, get all the columns that are greater than the starting column
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey, String startingColumn)
    {
        return getAllColumns(tableName, rowkey, startingColumn, null, 0);
    }

    /**
     * Given a table, a rowkey and bufferSize, and a starting column, get all the columns that are greater than the starting column
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey, String startingColumn, int bufferSize)
    {
        return getAllColumns(tableName, rowkey, startingColumn, null, bufferSize);
    }


    /**
     *  Given a table, a rowkey, and a filter, get all the columns of the table that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey, ColumnFilter columnFilter)
    {
        return getAllColumns(tableName, rowkey, null, columnFilter, 0);
    }


    /**
     *  Given a table, a rowkey, a filter, and a bufferSize get all the columns of the table that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey, ColumnFilter columnFilter, int bufferSize)
    {
        return getAllColumns(tableName, rowkey, null, columnFilter, bufferSize);
    }


    /**
     * Given a table, a rowkey, and a starting column, and a filter, get all the columns that are greater than the starting columns
     * that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey, String startingColumn, ColumnFilter columnFilter)
    {
        return getAllColumns(tableName, rowkey, startingColumn, columnFilter, 0);
    }

    /**
     * Given a table, a rowkey, and a starting column, a filter, and bufferSize, get all the columns that are greater than the starting columns
     * that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumns(String tableName, String rowkey, String startingColumn, ColumnFilter columnFilter, int bufferSize)
    {
        try
        {
            return new ColumnIteratorAsc(tableName, rowkey, startingColumn, columnFilter, bufferSize);
        }
        catch (RuntimeException e)
        {
            throw new CqlException(e);
        }
    }

    private class ColumnIteratorAsc implements Iterator<ColumnAndValue>
    {
        private int columnBatchSize = 100;

        private String tableName;
        private String rowkey;
        private Session session = getSession();
        private ConsistencyLevel level = getReadCL();
        private boolean isMonitored = (cqlMonitor != null);

        /**
         * columnCache holds a list of <columns,values> yet to be returned to the caller;
         * elements are removed from the head of the list.  When the list is empty, a fetch is done to
         * fill it up if there are more to be retrieved.
         */
        private List<ColumnAndValue> columnAndValueCache;

        /**
         * The first column to fetch. If null, it indicates that there are no longer columns to fetch.
         */
        private String firstColumn;

        /**
         * Filters the columns based on a boolean criteria.
         */
        private ColumnFilter columnFilter;

        /**
         * hasNext returns true if and only if there should be another key left to return.
         */
        public boolean hasNext()
        {
            if (!columnAndValueCache.isEmpty())
                return true;

            try
            {
                fetchColumns();
            }
            catch (CqlException e)
            {
                System.err.println("Iterator.hasNext: " + e.getMessage());
                return false;
            }

            return (!columnAndValueCache.isEmpty());
        }

        /**
         * next returns the next <columns,values> in the column family
         */
        public ColumnAndValue next()
        {
            if (!columnAndValueCache.isEmpty() || hasNext())
            {
                return columnAndValueCache.remove(0);
            }
            else
            {
                throw new NoSuchElementException("No more keys in column family " + tableName);
            }
        }

        /**
         * remove throws an exception since we don't allow keys to be removed via this interface.
         */
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        /**
         * Constructs a new KeyIterator by setting up the query parameters and fetching the first
         * block of keys.
         * @throws CqlException
         */
        private ColumnIteratorAsc(String tableName, String rowkey, String firstColumn, ColumnFilter columnFilter, int bufferSize)
        {
            this.tableName = tableName;
            this.rowkey = rowkey;
            this.columnFilter = columnFilter;
            if (bufferSize > 0)
                columnBatchSize = bufferSize;

            columnAndValueCache = new ArrayList<ColumnAndValue>();

            this.firstColumn = (firstColumn != null) ? firstColumn : "";

            fetchColumns();
        }

        private void fetchColumns()
        {
            while (firstColumn != null && columnAndValueCache.isEmpty())
            {
                fetchNextBatch();
            }
        }

        /**
         * Monitoring wrapper around fetch next batch
         * @throws CqlException
         */
        private void fetchNextBatch()
        {
            CqlMonitor.Timer  timer        = null;
            CqlMonitor.Timer  gTimer       = CqlMonitor.getTimer();
            UUID              queryTraceID = null;

            if (isMonitored)
                timer = cqlMonitor.getTimer("ColumnIteratorAsc({}, {}, {})", tableName, rowkey, firstColumn);

            try
            {
                queryTraceID = fetchNextBatchGuts();
            }
            finally
            {
                CqlMonitor.recordGlobalTime(gTimer, (columnAndValueCache != null) ? columnAndValueCache.size() : 0);

                if (isMonitored)
                    cqlMonitor.recordTime(timer, queryTraceID);
            }
        }

        /**
         * fetchNextBatch fetches the next block of columns from the server.
         * - It adds the column/value to the cache list based on the column filter.
         * - It sets the first column for the next iteration.
         * @throws CqlException
         */
        private UUID fetchNextBatchGuts()
        {
            // You have reached the end. No more keys.
            if (firstColumn == null)
                return null;

            // calling by mistake!!!
            if (!columnAndValueCache.isEmpty())
                return null;

            String queryStatement = String.format(querySingleBoundColumnsAsc, tableName, columnBatchSize);

            PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

            BoundStatement boundStatement = new BoundStatement(preparedStatement);
            boundStatement.setConsistencyLevel(level);
            if (isTraceEnabled)
                boundStatement.enableTracing();

            boundStatement.setString("rowkey", rowkey);
            boundStatement.setString("col", firstColumn);

            Exception mx = null;

            for (int attempt = 0; attempt < maxRetries; attempt++)
            {
                try
                {
                    ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                    UUID queryTraceID = null;

                    boolean tryAgain = true;
                    firstColumn = null;

                    while (tryAgain)
                    {
                        tryAgain = false;

                        queryTraceID = isTraceEnabled ? resultSetFuture.getUninterruptibly().getAllExecutionInfo().get(0).getQueryTrace().getTraceId() : null;

                        for (Row row : resultSetFuture.getUninterruptibly())
                        {
                            ColumnAndValue colAndValue = new ColumnAndValue(row.getString("col"), row.getBytes("d"));

                            if (columnFilter == null || columnFilter.filter(colAndValue.column))
                            {
                                columnAndValueCache.add(colAndValue);
                            }

                            tryAgain = true;
                            firstColumn = colAndValue.column;
                        }

                        if (!columnAndValueCache.isEmpty())
                            break;
                    }

                    return queryTraceID;
                }
                catch (RuntimeException e)
                {
                    mx = e;
                    e.printStackTrace();
                    columnAndValueCache.clear();
                    sleep(retrySleep);
                }
            }

            // The last exception for debugging purposes.
            throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
        }
    }

    /**
     * Given a table and a rowkey, get all the columns of a key
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey)
    {
        return getAllColumnsDesc(tableName, rowkey, null, null, 0);
    }

    /**
     * Given a table and a rowkey and bufferSize, get all the columns of a key
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey, int bufferSize)
    {
        return getAllColumnsDesc(tableName, rowkey, null, null, bufferSize);
    }

    /**
     * Given a table, a rowkey, and a starting column, get all the columns that are greater than the starting column
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey, String startingColumn)
    {
        return getAllColumnsDesc(tableName, rowkey, startingColumn, null, 0);
    }

    /**
     * Given a table, a rowkey and bufferSize, and a starting column, get all the columns that are greater than the starting column
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey, String startingColumn, int bufferSize)
    {
        return getAllColumnsDesc(tableName, rowkey, startingColumn, null, bufferSize);
    }


    /**
     *  Given a table, a rowkey, and a filter, get all the columns of the table that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey, ColumnFilter columnFilter)
    {
        return getAllColumnsDesc(tableName, rowkey, null, columnFilter, 0);
    }


    /**
     *  Given a table, a rowkey, a filter, and a bufferSize get all the columns of the table that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey, ColumnFilter columnFilter, int bufferSize)
    {
        return getAllColumnsDesc(tableName, rowkey, null, columnFilter, bufferSize);
    }


    /**
     * Given a table, a rowkey, and a starting column, and a filter, get all the columns that are greater than the starting columns
     * that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey, String startingColumn, ColumnFilter columnFilter)
    {
        return getAllColumnsDesc(tableName, rowkey, startingColumn, columnFilter, 0);
    }

    /**
     * Given a table, a rowkey, and a starting column, a filter, and bufferSize, get all the columns that are greater than the starting columns
     * that match the filter
     */
    public Iterator<ColumnAndValue> getAllColumnsDesc(String tableName, String rowkey, String startingColumn, ColumnFilter columnFilter, int bufferSize)
    {
        try
        {
            return new ColumnIteratorDesc(tableName, rowkey, startingColumn, columnFilter, bufferSize);
        }
        catch (RuntimeException e)
        {
            throw new CqlException(e);
        }
    }

    private class ColumnIteratorDesc implements Iterator<ColumnAndValue>
    {
        private int columnBatchSize = 100;

        private String tableName;
        private String rowkey;
        private Session session = getSession();
        private ConsistencyLevel level = getReadCL();
        private boolean isMonitored = (cqlMonitor != null);

        /**
         * columnCache holds a list of <columns,values> yet to be returned to the caller;
         * elements are removed from the head of the list.  When the list is empty, a fetch is done to
         * fill it up if there are more to be retrieved.
         */
        private List<ColumnAndValue> columnAndValueCache;

        /**
         * The first column to fetch. If null, it indicates that there are no longer columns to fetch.
         */
        private String firstColumn;

        /**
         * Filters the columns based on a boolean criteria.
         */
        private ColumnFilter columnFilter;

        /**
         * hasNext returns true if and only if there should be another key left to return.
         */
        public boolean hasNext()
        {
            if (!columnAndValueCache.isEmpty())
                return true;

            try
            {
                fetchColumns();
            }
            catch (CqlException e)
            {
                System.err.println("Iterator.hasNext: " + e.getMessage());
                return false;
            }

            return (!columnAndValueCache.isEmpty());
        }

        /**
         * next returns the next <columns,values> in the column family
         */
        public ColumnAndValue next()
        {
            if (!columnAndValueCache.isEmpty() || hasNext())
            {
                return columnAndValueCache.remove(0);
            }
            else
            {
                throw new NoSuchElementException("No more keys in column family " + tableName);
            }
        }

        /**
         * remove throws an exception since we don't allow keys to be removed via this interface.
         */
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        /**
         * Constructs a new KeyIterator by setting up the query parameters and fetching the first
         * block of keys.
         * @throws CqlException
         */
        private ColumnIteratorDesc(String tableName, String rowkey, String firstColumn, ColumnFilter columnFilter, int bufferSize)
        {
            this.tableName = tableName;
            this.rowkey = rowkey;
            this.columnFilter = columnFilter;
            if (bufferSize > 0)
                columnBatchSize = bufferSize;

            columnAndValueCache = new ArrayList<ColumnAndValue>();

            this.firstColumn = (firstColumn != null) ? firstColumn + "~" : "~";

            fetchColumns();
        }

        private void fetchColumns()
        {
            while (firstColumn != null && columnAndValueCache.isEmpty())
            {
                fetchNextBatch();
            }
        }

        /**
         * Monitoring wrapper around fetch next batch
         * @throws CqlException
         */
        private void fetchNextBatch()
        {
            CqlMonitor.Timer  timer        = null;
            CqlMonitor.Timer  gTimer       = CqlMonitor.getTimer();
            UUID              queryTraceID = null;

            if (isMonitored)
                timer = cqlMonitor.getTimer("ColumnIteratorDesc({}, {}, {})", tableName, rowkey, firstColumn);

            try
            {
                queryTraceID = fetchNextBatchGuts();
            }
            finally
            {
                CqlMonitor.recordGlobalTime(gTimer, (columnAndValueCache != null) ? columnAndValueCache.size() : 0);

                if (isMonitored)
                    cqlMonitor.recordTime(timer, queryTraceID);
            }
        }

        /**
         * fetchNextBatchGuts fetches the next block of columns from the server.
         * - It adds the column/value to the cache list based on the column filter.
         * - It sets the first column for the next iteration.
         * @throws CqlException
         */
        private UUID fetchNextBatchGuts()
        {

            // You have reached the end. No more keys.
            if (firstColumn == null)
                return null;

            // calling by mistake!!!
            if (!columnAndValueCache.isEmpty())
                return null;

            String queryStatement = String.format(querySingleBoundColumnsDesc, tableName, columnBatchSize);

            PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

            BoundStatement boundStatement = new BoundStatement(preparedStatement);
            boundStatement.setConsistencyLevel(level);
            if (isTraceEnabled)
                boundStatement.enableTracing();

            boundStatement.setString("rowkey", rowkey);
            boundStatement.setString("col", firstColumn);

            Exception mx = null;

            for (int attempt = 0; attempt < maxRetries; attempt++)
            {
                try
                {
                    UUID queryTraceID = null;

                    ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                    boolean tryAgain = true;
                    firstColumn = null;

                    while (tryAgain)
                    {
                        tryAgain = false;

                        queryTraceID = isTraceEnabled ? resultSetFuture.getUninterruptibly().getAllExecutionInfo().get(0).getQueryTrace().getTraceId() : null;

                        for (Row row : resultSetFuture.getUninterruptibly())
                        {
                            ColumnAndValue colAndValue = new ColumnAndValue(row.getString("col"), row.getBytes("d"));

                            if (columnFilter == null || columnFilter.filter(colAndValue.column))
                            {
                                columnAndValueCache.add(colAndValue);
                            }

                            tryAgain = true;
                            firstColumn = colAndValue.column;
                        }

                        if (!columnAndValueCache.isEmpty())
                            break;
                    }

                    return queryTraceID;
                }
                catch (RuntimeException e)
                {
                    mx = e;
                    e.printStackTrace();
                    columnAndValueCache.clear();
                    sleep(retrySleep);
                }
            }

            // The last exception for debugging purposes.
            throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
        }
    }

    public static abstract class KeyFilter
    {
        public abstract boolean filter(String rowkey);
    }

    /**
     * Given a table, get all the keys of the table
     */
    public Iterator<String> getAllKeys(String tableName)
    {
        return getAllKeys(tableName, null, null);
    }

    /**
     * Given a table and a starting row key, get all the keys that are greater than the starting key
     */
    public Iterator<String> getAllKeys(String tableName, String startingKey)
    {
        return getAllKeys(tableName, startingKey, null);
    }

    /**
     * Given a table and a filter, get all the keys of the table that match the filter
     */
    public Iterator<String> getAllKeys(String tableName, KeyFilter keyFilter)
    {
        return getAllKeys(tableName, null, keyFilter);
    }

    /**
     * Given a table, and a starting row key and a filter, get all the keys that are greater than the starting key
     * that match the filter
     */
    public Iterator<String> getAllKeys(String tableName, String startingKey, KeyFilter keyFilter)
    {
        try
        {
            return new KeyIterator1(tableName, startingKey, keyFilter);
        }
        catch (RuntimeException e)
        {
            throw new CqlException(e);
        }
    }

    private class KeyIterator1 implements Iterator<String>
    {
        private final static int  rowBatchSize = 1000;

        private String tableName;
        private Session session = getSession();
        private ConsistencyLevel level = getReadCL();

        /**
         * keyCache holds a list of keys yet to be returned to the caller; keys are
         * removed from the head of the list.  When the list is empty, a fetch is done to
         * fill it up if there are more to be retrieved.
         */
        private List<String> keyCache;

        /**
         * The first key to fetch. If null, it indicates that there are no longer keys to fetch.
         */
        private String firstKey;

        /**
         * Filters the keys based on a boolean criteria.
         */
        private KeyFilter keyFilter;

        /**
         * hasNext returns true if and only if there should be another key left to return.
         */
        public boolean hasNext()
        {
            if (!keyCache.isEmpty())
                return true;

            try
            {
                fetchKeys();
            }
            catch (CqlException e)
            {
                System.err.println("Iterator.hasNext: " + e.getMessage());
                return false;
            }

            return (!keyCache.isEmpty());
        }

        /**
         * next returns the next key in the column family
         */
        public String next()
        {
            if (!keyCache.isEmpty() || hasNext())
            {
                return keyCache.remove(0);
            }
            else
            {
                throw new NoSuchElementException("No more keys in column family " + tableName);
            }
        }

        /**
         * remove throws an exception since we don't allow keys to be removed via this interface.
         */
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove keys");
        }

        /**
         * Constructs a new KeyIterator by setting up the query parameters and fetching the first
         * block of keys.
         * @throws CqlException
         */
        private KeyIterator1(String tableName, String firstKey, KeyFilter keyFilter)
        {
            this.tableName = tableName;
            this.keyFilter = keyFilter;

            keyCache = new ArrayList<String>();

            this.firstKey = (firstKey != null) ? firstKey : "";

            fetchKeys();
        }

        private void fetchKeys()
        {
            while (firstKey != null && keyCache.isEmpty())
            {
                fetchNextBatch();
            }
        }

        /**
         * fetchNextBatch fetches the next block of keys from the server.
         * - It adds the keys to the key list based on the key filter.
         * - It sets the first key for the next iteration.
         * @throws CqlException
         */
        private void fetchNextBatch()
        {
            // You have reached the end. No more keys.
            if (firstKey == null)
                return;

            // calling by mistake!!!
            if (!keyCache.isEmpty())
                return;

            String queryStatement = String.format(queryRowScan1, tableName, rowBatchSize);
            PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

            BoundStatement boundStatement = new BoundStatement(preparedStatement);
            boundStatement.setConsistencyLevel(level);
            if (isTraceEnabled)
                boundStatement.enableTracing();

            boundStatement.setString(0, firstKey);

            Exception mx = null;

            for (int attempt = 0; attempt < maxRetries; attempt++)
            {
                try
                {
                    ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                    boolean tryAgain = true;
                    firstKey = null;

                    while (tryAgain)
                    {
                        tryAgain = false;

                        for (Row row : resultSetFuture.getUninterruptibly())
                        {
                            String key = row.getString("rowkey");

                            if ( (!keyCache.contains(key)) &&  ((keyFilter == null) || keyFilter.filter(key)) )
                            {
                                keyCache.add(key);
                            }

                            tryAgain = true;
                            firstKey = key;
                        }

                        if (!keyCache.isEmpty())
                            break;
                    }

                    return;
                }
                catch (RuntimeException e)
                {
                    mx = e;
                    e.printStackTrace();
                    keyCache.clear();
                    sleep(retrySleep);
                }
            }

            // The last exception for debugging purposes.
            throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
        }
    }


    /**
     * Given a table, and a starting row token, an ending row token, and a filter, get all the keys that are greater than the start token
     * and less than the end token that match the filter
     */
    public Iterator<String> getAllKeys(String tableName, Long startToken, Long endToken, KeyFilter keyFilter) throws CqlException
    {
        try
        {
            return new KeyIterator2(tableName, startToken, endToken, keyFilter);
        }
        catch (RuntimeException e)
        {
            throw new CqlException(e);
        }
    }

    private class KeyIterator2 implements Iterator<String>
    {
        private final static int  rowBatchSize = 1000;

        private String tableName;
        private Session session = getSession();
        private ConsistencyLevel level = getReadCL();

        /**
         * keyCache holds a list of keys yet to be returned to the caller; keys are
         * removed from the head of the list.  When the list is empty, a fetch is done to
         * fill it up if there are more to be retrieved.
         */
        private List<String> keyCache;

        /**
         * The first token to fetch.
         */
        private long startToken;

        /**
         * The last token to fetch.
         */
        private long endToken;


        /**
         * Filters the keys based on a boolean criteria.
         */
        private KeyFilter keyFilter;

        /**
         * hasNext returns true if and only if there should be another key left to return.
         */
        public boolean hasNext()
        {
            if (!keyCache.isEmpty())
                return true;

            try
            {
                fetchKeys();
            }
            catch (CqlException e)
            {
                System.err.println("Iterator.hasNext: " + e.getMessage());
                return false;
            }

            return (!keyCache.isEmpty());
        }

        /**
         * next returns the next key in the column family
         */
        public String next()
        {
            if (!keyCache.isEmpty() || hasNext())
            {
                return keyCache.remove(0);
            }
            else
            {
                throw new NoSuchElementException("No more keys in column family " + tableName);
            }
        }

        /**
         * remove throws an exception since we don't allow keys to be removed via this interface.
         */
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove keys");
        }

        /**
         * Constructs a new KeyIterator by setting up the query parameters and fetching the first
         * block of keys.
         * @throws CqlException
         */
        private KeyIterator2(String tableName, Long startToken, Long endToken, KeyFilter keyFilter) throws CqlException
        {
            this.tableName = tableName;
            this.keyFilter = keyFilter;

            keyCache = new ArrayList<String>();

            this.startToken = (startToken != null) ? startToken : - ( (long) Math.pow(2, 63));
            this.endToken   = (endToken != null) ? endToken :( (long) Math.pow(2, 63) ) - 1;

            fetchKeys();
        }

        private void fetchKeys() throws CqlException
        {
            if (keyCache.isEmpty())
            {
                fetchNextBatch();
            }
        }

        /**
         * fetchNextBatch fetches the next block of keys from the server.
         * - It adds the keys to the key list based on the key filter.
         * - It sets the first key for the next iteration.
         * @throws CqlException
         */
        private void fetchNextBatch() throws CqlException
        {
            // calling by mistake!!!
            if (!keyCache.isEmpty())
                return;

            String queryStatement = String.format(queryRowScan2, tableName, rowBatchSize);
            PreparedStatement preparedStatement = getPreparedStatement(session, queryStatement);

            BoundStatement boundStatement = new BoundStatement(preparedStatement);
            boundStatement.setConsistencyLevel(level);
            if (isTraceEnabled)
                boundStatement.enableTracing();

            boundStatement.setLong(0, startToken);
            boundStatement.setLong(1, endToken);

            Exception mx = null;

            for (int attempt = 0; attempt < maxRetries; attempt++)
            {
                try
                {
                    ResultSetFuture resultSetFuture = session.executeAsync(boundStatement);

                    boolean tryAgain = true;

                    while (tryAgain)
                    {
                        tryAgain = false;

                        for (Row row : resultSetFuture.getUninterruptibly())
                        {
                            String key = row.getString("rowkey");

                            if ( (!keyCache.contains(key)) &&  ((keyFilter == null) || keyFilter.filter(key)) )
                            {
                                keyCache.add(key);
                            }

                            tryAgain = true;
                        }

                        if (!keyCache.isEmpty())
                            break;
                    }

                    return;
                }
                catch (RuntimeException e)
                {
                    mx = e;
                    e.printStackTrace();
                    keyCache.clear();
                    sleep(retrySleep);
                }
            }

            // The last exception for debugging purposes.
            throw new CqlException("CqlCrud: max tries " + maxRetries + " reached - " + mx.getMessage());
        }
    }

    private PreparedStatement getPreparedStatement(Session session, String queryStatement)
    {
        //logger.info(">>>>>> CqlCrud  isStatementCacheEnabled: " + isStatementCacheEnabled + " ;queryStatement: " + queryStatement);

        PreparedStatement preparedStatement;

        if (!isStatementCacheEnabled)
        {
            preparedStatement =  session.prepare(queryStatement);
        }
        else
        {
            String key = cqlConnector.getID() + queryStatement;

            preparedStatement = cqlStringToPreparedStatement.getIfPresent(key);
            if (preparedStatement == null)
            {
                synchronized(this)
                {
                    preparedStatement = cqlStringToPreparedStatement.getIfPresent(key);
                    if (preparedStatement == null)
                    {
                        preparedStatement = session.prepare(queryStatement);
                        cqlStringToPreparedStatement.put(key, preparedStatement);
                    }
                }
            }
        }

        return preparedStatement;
    }

    private static void sleep(Long ms)
    {

        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ix)
        {
        }
    }

    //
    // We only allow the following:
    //
    //    1) No composite rowkeys. The partition key must be a single text value in the legacyFieldValueList
    //    2) Only one byteBuffer that is considered the "data" value.
    //
    private boolean isValidCreateTableRequest(FieldValueList fieldValueList, String partitionKey)
    {
        boolean hasRowKey = false;
        int byteBufferCount = 0;

        for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
        {
            switch (fieldValue.getKey().getType())
            {
                case LongField.TYPE:
                    //This is an accepted type.
                    break;
                case StringField.TYPE:
                    if(fieldValue.getKey().getName().equals(partitionKey)) hasRowKey = true;
                    break;
                case StringArrayField.TYPE:
                    break;
                case ByteBufferField.TYPE:
                    byteBufferCount++;
                    break;
                default:
                    throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
            }
        }

        LogLog.debug("hasRowKey is [" + hasRowKey + "]");
        LogLog.debug("byteBufferCount is [" + byteBufferCount + "]");

        boolean isvalid = hasRowKey && byteBufferCount == 1;

        if(!isvalid)
        {
            LogLog.error("this is not a valid create table request, check syntax.");
        }

        return isvalid;
    }
}

