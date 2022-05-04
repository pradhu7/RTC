package com.apixio.datasource.cassandra;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.datastax.driver.core.ConsistencyLevel;

import com.apixio.utility.DataSourceUtility;
import com.apixio.datasource.cassandra.CqlCrud.ColumnFilter;
import com.apixio.datasource.cassandra.CqlCrud.KeyFilter;

/**
 * @author lschneider
 */
@Ignore("Integration")
public class CqlCrudCacheTest
{
    private CqlCrud crud;
    private CqlCache cache;

    private int bufferSize;
    private String tableName;
    private String tableNameWorkArea;

    private boolean localCassandra = true;
    private int repeats = 20;

    boolean inClause = false;

    @Before
    public void setUp() throws IOException
    {
        CqlConnector connector = new CqlConnector();
        connector.setBinaryPort(9042);
        connector.setKeyspaceName("apixio");
        if (localCassandra)
        {
            connector.setHosts("localhost");
            connector.setReadConsistencyLevel(ConsistencyLevel.ONE.name());
            connector.setWriteConsistencyLevel(ConsistencyLevel.ONE.name());
        }
        else
        {
            connector.setHosts("ec2-54-219-24-211.us-west-1.compute.amazonaws.com,ec2-54-215-114-82.us-west-1.compute.amazonaws.com,ec2-54-215-21-23.us-west-1.compute.amazonaws.com,"
                    + "ec2-184-169-217-7.us-west-1.compute.amazonaws.com,ec2-184-169-222-174.us-west-1.compute.amazonaws.com,ec2-50-18-75-99.us-west-1.compute.amazonaws.com");
            connector.setReadConsistencyLevel(ConsistencyLevel.QUORUM.name());
            connector.setWriteConsistencyLevel(ConsistencyLevel.QUORUM.name());
        }

        connector.setMinConnections(1);
        connector.setMaxConnections(2);
        connector.setBaseDelayMs(50);
        connector.setMaxDelayMs(250);
        connector.setLocalDC("");
        connector.init();

        crud = new CqlCrud();
        crud.setCqlConnector(connector);
        crud.setStatementCacheEnabled(true);

        bufferSize = 20; // must be >= 10 for filled buffer test

        cache = new ThreadLocalCqlCache();
        cache.setBufferSize(bufferSize);
        cache.setCqlCrud(crud);

        tableName = "test_cf";
        // tableName = "vram_test_crud";

        tableNameWorkArea = "test_work_cf";

        System.out.println("Number of repeats: " + repeats);
        System.out.println("Using inClause: " + inClause);
        System.out.println();
    }

////////////////////////////////////////////////////
//////////////////// Crud Tests ////////////////////
////////////////////////////////////////////////////

    @Test
    public void describeTables()
    {
        try
        {
            System.out.println(crud.describeTables());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void testHappyPath_NewWay()
    {
        String transientTable = "transient_" + System.currentTimeMillis() + RandomUtils.nextLong();

        String rowkey = "testInsertAndDeleteRow_" + Math.random();
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        try
        {
            // test insert
            String [] LEGACY_FIELDS = {"rowkey", "col", "d"};
            StatementGenerator statementGenerator = new StatementGenerator(LEGACY_FIELDS);

            FieldValueList fieldValueList = new FieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column)
                    .addByteBuffer("d", value)
                    .build();


            //create new table
            boolean tableCreated = crud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, transientTable, fieldValueList, "rowkey", "col");

            Assert.assertTrue(tableCreated);

            //INSERT NEW STYLE
            crud.insertRow(transientTable, statementGenerator, fieldValueList,true);

            Thread.sleep(200);

            //QUERY NEW STYLE
            ByteBuffer valResult = crud.getColumnValue(transientTable, statementGenerator, fieldValueList);
            assertNotNull(valResult);

            assertEquals(valResult, value);

            // test delete

            crud.deleteRow(transientTable, rowkey, true);

            Thread.sleep(200);
            Iterator<ColumnAndValue> resultIt = crud.getAllColumns(transientTable, rowkey);

            assertFalse(resultIt.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
        finally
        {
            //
            // Clean up
            //
            assertTrue(crud.dropTable(transientTable));
        }
    }

    @Test
    public void testHappyPath_CacheGenericQuery_WithLong1()
    {
        String transientTable = "transient_" + System.currentTimeMillis() + RandomUtils.nextLong();

        String rowkey = "testInsertAndDeleteRow_" + Math.random();
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        try
        {
            // test insert
            StatementGenerator statementGenerator = new StatementGenerator("rowkey", "col", "testLong", "d");

            FieldValueList fieldValueList = new FieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column)
                    .addLong("testLong", Long.MIN_VALUE)
                    .addByteBuffer("d", value)
                    .build();

            //create new table
            boolean tableCreated = crud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, transientTable, fieldValueList, "rowkey", "col");

            Assert.assertTrue(tableCreated);

            CqlRowData cqlRowData = new CqlRowData(transientTable, statementGenerator, fieldValueList);

            //INSERT NEW STYLE
            cache.insert(cqlRowData);
            cache.flush();

            Thread.sleep(200);

            String query = "select rowkey, col, testlong, d from " + transientTable + " where rowkey in ( ? ) and col = ?";

            FieldValueList fvl = new QueryFieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column).build();
            CassandraResult cassandraResult = crud.query(query, fvl);

//            CassandraResult cassandraResult = crud.query("select rowkey, col, testlong, d from " + transientTable
//                    + " where rowkey in ('" + rowkey +"') and col = '" + column + "'");

            assertNotNull(cassandraResult);

            CassandraResult.CassandraResultIterator iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertTrue(iterator.hasNext());

            Map<String, Object> result = iterator.next();

            assertNotNull(result);

            assertTrue(result.keySet().contains("rowkey"));
            assertTrue(result.keySet().contains("col"));
            assertTrue(result.keySet().contains("d"));
            assertTrue(result.keySet().contains("testlong"));

            assertTrue(result.get("rowkey") instanceof String);
            assertTrue(result.get("col") instanceof String);
            assertTrue(result.get("d") instanceof ByteBuffer);
            assertTrue(result.get("testlong") instanceof Long);

            assertEquals(((String) result.get("rowkey")), rowkey);
            assertEquals(((String) result.get("col")), column);
            assertEquals(((ByteBuffer) result.get("d")), value);
            assertEquals(result.get("testlong"), Long.MIN_VALUE);

            assertFalse(iterator.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
        finally
        {
            //
            // Clean up
            //
            assertTrue(crud.dropTable(transientTable));
        }
    }

    @Test
    public void testHappyPath_CacheGenericQuery_WithLong()
    {
        String transientTable = "transient_" + System.currentTimeMillis() + RandomUtils.nextLong();

        String rowkey = "testInsertAndDeleteRow_" + Math.random();
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        try
        {
            // test insert
            StatementGenerator statementGenerator = new StatementGenerator("rowkey", "col", "testLong", "d");

            FieldValueList fieldValueList = new FieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column)
                    .addLong("testLong", Long.MIN_VALUE)
                    .addByteBuffer("d", value)
                    .build();

            //create new table
            boolean tableCreated = crud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, transientTable, fieldValueList, "rowkey", "col");

            Assert.assertTrue(tableCreated);

            CqlRowData cqlRowData = new CqlRowData(transientTable, statementGenerator, fieldValueList);

            //INSERT NEW STYLE
            cache.insert(cqlRowData);
            cache.flush();

            Thread.sleep(200);

            String query = "select rowkey, col, testlong, d from " + transientTable + " where rowkey in ( ? ) and col = ?";

            FieldValueList fvl = new QueryFieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column).build();
            CassandraResult cassandraResult = crud.query(query, fvl);

            assertNotNull(cassandraResult);

            CassandraResult.CassandraResultIterator iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertTrue(iterator.hasNext());

            Map<String, Object> result = iterator.next();

            assertNotNull(result);

            assertTrue(result.keySet().contains("rowkey"));
            assertTrue(result.keySet().contains("col"));
            assertTrue(result.keySet().contains("d"));
            assertTrue(result.keySet().contains("testlong"));

            assertTrue(result.get("rowkey") instanceof String);
            assertTrue(result.get("col") instanceof String);
            assertTrue(result.get("d") instanceof ByteBuffer);
            assertTrue(result.get("testlong") instanceof Long);

            assertEquals(((String)result.get("rowkey")), rowkey);
            assertEquals(((String)result.get("col")), column);
            assertEquals(((ByteBuffer)result.get("d")), value);
            assertEquals(result.get("testlong"), Long.MIN_VALUE);

            assertFalse(iterator.hasNext());

            crud.deleteRow(transientTable, rowkey, true);

            query = "select rowkey, col, d from " + transientTable + " where rowkey in ( ? ) and col = ?";

            fvl = new QueryFieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column).build();
            cassandraResult = crud.query(query, fvl);

            assertNotNull(cassandraResult);

            iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertFalse(iterator.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
        finally
        {
            //
            // Clean up
            //
            assertTrue(crud.dropTable(transientTable));
        }
    }

    @Test
    public void testHappyPath_CacheGenericQuery()
    {
        String transientTable = "transient_" + System.currentTimeMillis() + RandomUtils.nextLong();

        String rowkey = "testInsertAndDeleteRow_" + Math.random();
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        try
        {
            // test insert
            String [] LEGACY_FIELDS = {"rowkey", "col", "d"};
            StatementGenerator statementGenerator = new StatementGenerator(LEGACY_FIELDS);

            FieldValueList fieldValueList = new FieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column)
                    .addByteBuffer("d", value)
                    .build();

            //create new table
            boolean tableCreated = crud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, transientTable, fieldValueList, "rowkey", "col");

            Assert.assertTrue(tableCreated);

            CqlRowData cqlRowData = new CqlRowData(transientTable, statementGenerator, fieldValueList);

            //INSERT NEW STYLE
            cache.insert(cqlRowData);
            cache.flush();

            Thread.sleep(200);

            String query = "select rowkey, col, d from " + transientTable + " where rowkey in ( ? ) and col = ?";

            FieldValueList fvl = new QueryFieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column).build();
            CassandraResult cassandraResult = crud.query(query, fvl);

            assertNotNull(cassandraResult);

            CassandraResult.CassandraResultIterator iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertTrue(iterator.hasNext());

            Map<String, Object> result = iterator.next();

            assertNotNull(result);

            assertTrue(result.keySet().contains("rowkey"));
            assertTrue(result.keySet().contains("col"));
            assertTrue(result.keySet().contains("d"));

            assertTrue(result.get("rowkey") instanceof String);
            assertTrue(result.get("col") instanceof String);
            assertTrue(result.get("d") instanceof ByteBuffer);

            assertEquals(((String)result.get("rowkey")), rowkey);
            assertEquals(((String)result.get("col")), column);
            assertEquals(((ByteBuffer)result.get("d")), value);

            assertFalse(iterator.hasNext());

            crud.deleteRow(transientTable, rowkey, true);

            fvl = new QueryFieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column).build();
            cassandraResult = crud.query(query, fvl);

            cassandraResult = crud.query(query, fvl);

            assertNotNull(cassandraResult);

            iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertFalse(iterator.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
        finally
        {
            //
            // Clean up
            //
            assertTrue(crud.dropTable(transientTable));
        }
    }


    @Test
    public void testHappyPath_CacheGenericQueryDeleteRow()
    {
        String transientTable = "transient_" + System.currentTimeMillis() + RandomUtils.nextLong();

        String rowkey = "testInsertAndDeleteRow_" + Math.random();
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        try
        {
            // test insert
            String [] LEGACY_FIELDS = {"rowkey", "col", "d"};
            StatementGenerator statementGenerator = new StatementGenerator(LEGACY_FIELDS);

            FieldValueList fieldValueList = new FieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column)
                    .addByteBuffer("d", value)
                    .build();


            //create new table
            boolean tableCreated = crud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, transientTable, fieldValueList, "rowkey", "col");

            Assert.assertTrue(tableCreated);

            CqlRowData cqlRowData = new CqlRowData(transientTable, statementGenerator, fieldValueList);

            //INSERT NEW STYLE
            cache.insert(cqlRowData);
            cache.flush();

            Thread.sleep(200);

            String query = "select rowkey, col, d from " + transientTable + " where rowkey in ( ? ) and col = ?";

            FieldValueList fvl = new QueryFieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column).build();
            CassandraResult cassandraResult = crud.query(query, fvl);

            assertNotNull(cassandraResult);

            CassandraResult.CassandraResultIterator iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertTrue(iterator.hasNext());

            Map<String, Object> result = iterator.next();

            assertNotNull(result);

            assertTrue(result.keySet().contains("rowkey"));
            assertTrue(result.keySet().contains("col"));
            assertTrue(result.keySet().contains("d"));

            assertTrue(result.get("rowkey") instanceof String);
            assertTrue(result.get("col") instanceof String);
            assertTrue(result.get("d") instanceof ByteBuffer);

            assertEquals(((String)result.get("rowkey")), rowkey);
            assertEquals(((String)result.get("col")), column);
            assertEquals(((ByteBuffer)result.get("d")), value);

            assertFalse(iterator.hasNext());


            cache.deleteRow(cqlRowData);
            cache.flush();

            cassandraResult = crud.query(query, fvl);

            assertNotNull(cassandraResult);

            iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertFalse(iterator.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
        finally
        {
            //
            // Clean up
            //
            assertTrue(crud.dropTable(transientTable));
        }
    }

    @Test
    public void testHappyPath_GenericQuery()
    {
        String transientTable = "transient_" + System.currentTimeMillis() + RandomUtils.nextLong();

        String rowkey = "testInsertAndDeleteRow_" + Math.random();
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        try
        {
            // test insert
            String [] LEGACY_FIELDS = {"rowkey", "col", "d"};
            StatementGenerator statementGenerator = new StatementGenerator(LEGACY_FIELDS);

            FieldValueList fieldValueList = new FieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column)
                    .addByteBuffer("d", value)
                    .build();


            //create new table
            boolean tableCreated = crud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, transientTable, fieldValueList, "rowkey", "col");

            Assert.assertTrue(tableCreated);

            //INSERT NEW STYLE
            crud.insertRow(transientTable, statementGenerator, fieldValueList,true);

            Thread.sleep(200);

            String query = "select rowkey, col, d from " + transientTable + " where rowkey in ( ? ) and col = ?";

            FieldValueList fvl = new QueryFieldValueListBuilder()
                    .addString("rowkey", rowkey)
                    .addString("col", column).build();
            CassandraResult cassandraResult = crud.query(query, fvl);

            assertNotNull(cassandraResult);

            CassandraResult.CassandraResultIterator iterator = cassandraResult.iterator();

            assertNotNull(iterator);

            assertTrue(iterator.hasNext());

            Map<String, Object> result = iterator.next();

            assertNotNull(result);

            assertTrue(result.keySet().contains("rowkey"));
            assertTrue(result.keySet().contains("col"));
            assertTrue(result.keySet().contains("d"));

            assertTrue(result.get("rowkey") instanceof String);
            assertTrue(result.get("col") instanceof String);
            assertTrue(result.get("d") instanceof ByteBuffer);

            assertEquals(((String)result.get("rowkey")), rowkey);
            assertEquals(((String)result.get("col")), column);
            assertEquals(((ByteBuffer)result.get("d")), value);

            assertFalse(iterator.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
        finally
        {
            //
            // Clean up
            //
            assertTrue(crud.dropTable(transientTable));
        }
    }

    @Test
    public void testInsertAndDeleteRow_NewWay()
    {
        for (int i = 0; i < repeats; i++)
        {
            String rowkey = "testInsertAndDeleteRow_" + Math.random();
            String column = "col1";
            ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

            try
            {
                // test insert
                StatementGenerator statementGenerator = new StatementGenerator("rowkey", "col", "d");

                FieldValueList fieldValueList = new FieldValueListBuilder()
                        .addString("rowkey", rowkey)
                        .addString("col", column)
                        .addByteBuffer("d", value)
                        .build();

                //INSERT NEW STYLE
                crud.insertRow(tableName, statementGenerator, fieldValueList,true);

                Thread.sleep(200);

                //QUERY NEW STYLE
                ByteBuffer valResult = crud.getColumnValue(tableName, statementGenerator, fieldValueList);
                assertNotNull(valResult);

                assertEquals(valResult, value);

                // test delete

                crud.deleteRow(tableName, rowkey, true);

                Thread.sleep(200);
                Iterator<ColumnAndValue> resultIt = crud.getAllColumns(tableName, rowkey);

                assertFalse(resultIt.hasNext());
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }

    @Test
    public void testInsertAndDeleteRow()
    {
        for (int i = 0; i < repeats; i++)
        {
            String rowkey = "testInsertAndDeleteRow_" + Math.random();
            String column = "col1";
            ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

            try
            {
                // test insert

                crud.insertRow(tableName, rowkey, column, value, true);

                Thread.sleep(200);
                Iterator<ColumnAndValue> resultIt = crud.getAllColumns(tableName, rowkey);

                assertTrue(resultIt.hasNext());

                ColumnAndValue result = resultIt.next();

                assertFalse(resultIt.hasNext());

                String colResult = result.column;
                assertEquals(colResult, column);

                ByteBuffer valResult = result.value;
                assertEquals(valResult, value);

                // test delete

                crud.deleteRow(tableName, rowkey, true);

                Thread.sleep(200);
                resultIt = crud.getAllColumns(tableName, rowkey);

                assertFalse(resultIt.hasNext());
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }

    @Test
    public void testInsertAndDeleteCols()
    {
        for (int i = 0; i < repeats; i++)
        {
            String rowkey = "testInsertAndDeleteCols_" + Math.random();
            String column1 = "col1";
            String column2 = "col2";
            String column3 = "thirdCol";
            ByteBuffer value1 = DataSourceUtility.stringToByteBuffer("testColumnValue");
            ByteBuffer value2 = DataSourceUtility.stringToByteBuffer("anotherTestColumnValue");

            try
            {
                // test with two columns

                crud.insertRow(tableName, rowkey, column1, value1, true);
                crud.insertRow(tableName, rowkey, column2, value2, true);
                crud.insertRow(tableName, rowkey, column3, value2, true);
                Thread.sleep(200);

                ColumnFilter filter = new ColumnFilter()
                    {
                        @Override
                        public boolean filter(String column)
                        {
                            return column.startsWith("col");
                        }
                    };

                // test column filter and starting column

                Iterator<ColumnAndValue> resultIt1 = crud.getAllColumns(tableName, rowkey);
                Iterator<ColumnAndValue> resultIt2 = crud.getAllColumns(tableName, rowkey, filter);
                Iterator<ColumnAndValue> resultIt3 = crud.getAllColumns(tableName, rowkey, column1);
                Iterator<ColumnAndValue> resultIt4 = crud.getAllColumns(tableName, rowkey, column1, filter);

                assertTrue(resultIt1.hasNext());
                assertTrue(resultIt2.hasNext());
                assertTrue(resultIt3.hasNext());
                assertTrue(resultIt4.hasNext());

                ColumnAndValue result1_1 = resultIt1.next();
                ColumnAndValue result1_2 = resultIt1.next();
                ColumnAndValue result1_3 = resultIt1.next();
                ColumnAndValue result2_1 = resultIt2.next();
                ColumnAndValue result2_2 = resultIt2.next();
                ColumnAndValue result3_1 = resultIt3.next();
                ColumnAndValue result3_2 = resultIt3.next();
                ColumnAndValue result4 = resultIt4.next();

                assertFalse(resultIt1.hasNext());
                assertFalse(resultIt2.hasNext());
                assertFalse(resultIt3.hasNext());
                assertFalse(resultIt4.hasNext());

                assertEquals(result1_1.value, value1);
                assertEquals(result1_2.value, value2);
                assertEquals(result1_3.value, value2);
                assertEquals(result2_1.value, value1);
                assertEquals(result2_2.value, value2);
                assertEquals(result3_1.value, value2);
                assertEquals(result3_2.value, value2);
                assertEquals(result4.value, value2);

                // test delete

                crud.deleteColumn(tableName, rowkey, column1, false);

                Thread.sleep(200);
                Iterator<ColumnAndValue> resultIt = crud.getAllColumns(tableName, rowkey);
                resultIt.next();
                resultIt.next();
                assertFalse(resultIt.hasNext());

                List<CqlRowData> cols = new ArrayList<CqlRowData>();
                cols.add(new CqlRowData(tableName, rowkey, column2, value2));
                cols.add(new CqlRowData(tableName, rowkey, column3, value2));
                crud.deleteColumns(cols);

                Thread.sleep(200);
                resultIt = crud.getAllColumns(tableName, rowkey);
                assertFalse(resultIt.hasNext());
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }

    @Test
    public void testInsertAndDeleteRows()
    {
        for (int j = 0; j < repeats; j++)
        {
            final String rowkey = "testInsertAndDeleteRows_" + Math.random();
            String column = "col1";
            ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

            List<CqlRowData> rows = new ArrayList<CqlRowData>();

            int numRows = 10;
            for (int i = 0; i < numRows; i++)
            {
                rows.add(new CqlRowData(tableName, rowkey + i, column, value));
            }

            try
            {
                crud.insertRows(rows);
                Thread.sleep(500);

                KeyFilter filter = new KeyFilter()
                    {
                        @Override
                        public boolean filter(String id)
                        {
                            return id.startsWith(rowkey);
                        }
                    };

                Iterator<String> resultIt = crud.getAllKeys(tableName, filter);
                assertTrue(resultIt.hasNext());

                int countRows = 0;
                while (resultIt.hasNext())
                {
                    resultIt.next();
                    countRows++;
                }

                assertEquals(numRows, countRows);

                crud.deleteRows(rows);
                Thread.sleep(500);

                resultIt = crud.getAllKeys(tableName, filter);
                assertFalse(resultIt.hasNext());
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }

    @Test
    public void testMixedBatch()
    {
        final String rowkey = "testMixedBatch_" + Math.random() + "_";
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        List<CqlRowData> rows = new ArrayList<CqlRowData>();

        int numRows = 10;
        for (int i = 0; i < numRows; i++)
        {
            rows.add(new CqlRowData(tableName, rowkey + i, column, value));
        }

        try
        {
            crud.insertRow(tableName, rowkey + "before", column, value, true);
            crud.insertRows(rows);
            crud.insertRow(tableName, rowkey + "after", column, value, true);
            Thread.sleep(500);

            KeyFilter filter = new KeyFilter()
                {
                    @Override
                    public boolean filter(String id)
                    {
                        return id.startsWith(rowkey);
                    }
                };

            Iterator<String> resultIt = crud.getAllKeys(tableName, filter);
            assertTrue(resultIt.hasNext());

            int countRows = 0;
            while (resultIt.hasNext())
            {
                resultIt.next();
                countRows++;
            }

            assertEquals(numRows + 2, countRows);
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
    }

    @Test
    public void testGetAllKeys()
    {
        final String rowkey = "testGetKeys_" + Math.random() + "_";
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        List<CqlRowData> rows = new ArrayList<CqlRowData>();

        int numRows = 20;
        for (int i = 0; i < numRows; i++)
            rows.add(new CqlRowData(tableName, rowkey + i, column, value));

        try
        {
            crud.insertRows(rows);
            Thread.sleep(500);

            KeyFilter filter = new KeyFilter()
                {
                    @Override
                    public boolean filter(String id)
                    {
                        return id.startsWith(rowkey);
                    }
                };

            String startKey;

            Iterator<String> unfiltered1 = crud.getAllKeys(tableName);
            assertTrue(unfiltered1.hasNext());
            for (int i = 0; i < numRows/2; i++)
                unfiltered1.next();
            startKey = unfiltered1.next();

            Iterator<String> unfiltered2 = crud.getAllKeys(tableName, startKey);
            assertTrue(unfiltered2.hasNext());
            for (int i = numRows/2; i < numRows; i++)
                assertTrue(unfiltered1.next().equals(unfiltered2.next()));

            if (unfiltered1.hasNext())
                assertTrue(unfiltered2.hasNext());
            else
                assertFalse(unfiltered2.hasNext());

            Iterator<String> filtered1 = crud.getAllKeys(tableName, filter);
            assertTrue(filtered1.hasNext());
            for (int i = 0; i < numRows/4; i++)
                filtered1.next();
            startKey = filtered1.next();

            Iterator<String> filtered2 = crud.getAllKeys(tableName, startKey, filter);
            assertTrue(filtered2.hasNext());
            for (int i = (numRows/4)+1; i < numRows; i++)
                assertTrue(filtered1.next().equals(filtered2.next()));

            assertFalse(filtered1.hasNext());
            assertFalse(filtered2.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }

    }

    @Test
    public void testGetAllColumns()
    {
        String rowkey = "testGetAllColumns_" + Math.random();
        String column = "col";
        String otherColumn = "filterOut";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        int limit = 5;
        int numCols = limit*4;
        String startCol = "";

        List<CqlRowData> cols = new ArrayList<CqlRowData>();

        try
        {
            for (int i = 0; i < numCols; i++)
            {
                cols.add(new CqlRowData(tableName, rowkey, column + i, value));
                crud.insertRow(tableName, rowkey, otherColumn + i, value, true);
            }
            crud.insertRows(cols);
            Thread.sleep(200);

            ColumnFilter filter = new ColumnFilter()
                {
                    @Override
                    public boolean filter(String column)
                    {
                        return column.startsWith("col");
                    }
                };

            // Test unfiltered with limit

            Iterator<ColumnAndValue> unfiltered1 = crud.getAllColumns(tableName, rowkey, 2*limit);
            assertTrue(unfiltered1.hasNext());

            for (int i = 0; i < limit; i++)
                startCol = unfiltered1.next().column;

            Iterator<ColumnAndValue> unfiltered2 = crud.getAllColumns(tableName, rowkey, startCol, limit);
            assertTrue(unfiltered2.hasNext());

            for (int i = limit; i < limit*2; i++)
                assertTrue((unfiltered1.next().column).equals(unfiltered2.next().column));

            if (unfiltered1.hasNext())
                assertTrue(unfiltered2.hasNext());
            else
                assertFalse(unfiltered2.hasNext());

            // Test filtered with small limit while hasNext

            Iterator<ColumnAndValue> filtered1 = crud.getAllColumns(tableName, rowkey, filter, limit);
            assertTrue(filtered1.hasNext());

            for (int i = 0; i < limit; i++)
                startCol = filtered1.next().column;

            Iterator<ColumnAndValue> filtered2 = crud.getAllColumns(tableName, rowkey, startCol, filter, limit);
            assertTrue(filtered2.hasNext());

            while (filtered1.hasNext() && filtered2.hasNext())
                assertTrue((filtered1.next().column).equals(filtered2.next().column));

            assertFalse(filtered1.hasNext());
            assertFalse(filtered2.hasNext());

            // Test filtered with small limit without hasNext

            filtered1 = crud.getAllColumns(tableName, rowkey, filter, limit);
            assertTrue(filtered1.hasNext());

            for (int i = 0; i < limit; i++)
                startCol = filtered1.next().column;

            filtered2 = crud.getAllColumns(tableName, rowkey, startCol, filter, limit);
            assertTrue(filtered2.hasNext());

            for (int i = limit; i < numCols; i++)
                assertTrue((filtered1.next().column).equals(filtered2.next().column));

            assertFalse(filtered1.hasNext());
            assertFalse(filtered2.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
    }

    @Test
    public void testGetAllColumnsDesc()
    {
        String rowkey = "testGetAllColumnsDesc_" + Math.random();
        String column = "col";
        String otherColumn = "filterOut";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        int limit = 5;
        int numCols = limit*4;
        String startCol = "";

        List<CqlRowData> cols = new ArrayList<CqlRowData>();

        try
        {
            for (int i = 0; i < numCols; i++)
            {
                cols.add(new CqlRowData(tableName, rowkey, column + i, value));
                crud.insertRow(tableName, rowkey, otherColumn + i, value, true);
            }
            crud.insertRows(cols);
            Thread.sleep(200);

            ColumnFilter filter = new ColumnFilter()
                {
                    @Override
                    public boolean filter(String column)
                    {
                        return column.startsWith("col");
                    }
                };

            // Test unfiltered with limit

            Iterator<ColumnAndValue> unfiltered1 = crud.getAllColumns(tableName, rowkey, 2*limit);
            assertTrue(unfiltered1.hasNext());

            for (int i = 0; i < limit; i++)
                startCol = unfiltered1.next().column;

            Iterator<ColumnAndValue> unfiltered2 = crud.getAllColumns(tableName, rowkey, startCol, limit);
            assertTrue(unfiltered2.hasNext());

            for (int i = limit; i < limit*2; i++)
                assertTrue((unfiltered1.next().column).equals(unfiltered2.next().column));

            if (unfiltered1.hasNext())
                assertTrue(unfiltered2.hasNext());
            else
                assertFalse(unfiltered2.hasNext());

            // Test filtered with small limit while hasNext

            Iterator<ColumnAndValue> filtered1 = crud.getAllColumns(tableName, rowkey, filter, limit);
            assertTrue(filtered1.hasNext());

            for (int i = 0; i < limit; i++)
                startCol = filtered1.next().column;

            Iterator<ColumnAndValue> filtered2 = crud.getAllColumns(tableName, rowkey, startCol, filter, limit);
            assertTrue(filtered2.hasNext());

            while (filtered1.hasNext() && filtered2.hasNext())
                assertTrue((filtered1.next().column).equals(filtered2.next().column));

            assertFalse(filtered1.hasNext());
            assertFalse(filtered2.hasNext());

            // Test filtered with small limit without hasNext

            filtered1 = crud.getAllColumns(tableName, rowkey, filter, limit);
            assertTrue(filtered1.hasNext());

            for (int i = 0; i < limit; i++)
                startCol = filtered1.next().column;

            filtered2 = crud.getAllColumns(tableName, rowkey, startCol, filter, limit);
            assertTrue(filtered2.hasNext());

            for (int i = limit; i < numCols; i++)
                assertTrue((filtered1.next().column).equals(filtered2.next().column));

            assertFalse(filtered1.hasNext());
            assertFalse(filtered2.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
    }

    @Test
    public void testDeleteAllColumns()
    {
        for (int j = 0; j < repeats; j++)
        {
            String rowkey = "testDeleteColumns_" + Math.random();
            String column = "col";
            ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

            int numCols = 10;

            List<CqlRowData> cols = new ArrayList<CqlRowData>();

            try
            {
                for (int i = 0; i < numCols; i++)
                    cols.add(new CqlRowData(tableName, rowkey, column + i, value));

                crud.insertRows(cols);
                Thread.sleep(200);

                Iterator<ColumnAndValue> it = crud.getAllColumns(tableName, rowkey);
                assertTrue(it.hasNext());
                int count = 0;
                while (it.hasNext())
                {
                    it.next();
                    count++;
                }
                assertTrue(count == numCols);

                crud.deleteColumns(cols);
                Thread.sleep(200);

                it = crud.getAllColumns(tableName, rowkey);
                assertFalse(it.hasNext());
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }

    @Test
    public void testDeleteSomeColumns()
    {
        String rowkey = "testDeleteColumns_" + Math.random();
        String column = "col";
        String otherColumn = "filterOut";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        int numCols = 10;

        List<CqlRowData> cols = new ArrayList<CqlRowData>();

        try
        {
            for (int i = 0; i < numCols; i++)
            {
                cols.add(new CqlRowData(tableName, rowkey, column + i, value));
                crud.insertRow(tableName, rowkey, otherColumn + i, value, true);
            }
            crud.insertRows(cols);
            Thread.sleep(200);

            ColumnFilter filter = new ColumnFilter()
                {
                    @Override
                    public boolean filter(String column)
                    {
                        return column.startsWith("col");
                    }
                };

            Iterator<ColumnAndValue> it = crud.getAllColumns(tableName, rowkey, filter);
            assertTrue(it.hasNext());
            int count = 0;
            while (it.hasNext())
            {
                it.next();
                count++;
            }
            assertTrue(count == numCols);

            crud.deleteColumns(cols);
            Thread.sleep(200);

            it = crud.getAllColumns(tableName, rowkey, filter);
            assertFalse(it.hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
    }

    @Test
    public void testGetColumnValues()
    {
        for (int j = 0; j < repeats; j++)
        {
            String rowkey = "testGetColValues_" + Math.random();
            String column = "col";
            String anotherColumn = "getThisCol";
            ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");
            ByteBuffer anotherValue = DataSourceUtility.stringToByteBuffer("anotherColumnValue");

            int numCols = 10;
            List<CqlRowData> cols = new ArrayList<CqlRowData>();

            try
            {
                crud.insertRow(tableName, rowkey, anotherColumn, anotherValue, true);
                Thread.sleep(200);

                ByteBuffer singleValue = crud.getColumnValue(tableName, rowkey, anotherColumn);
                assertNotNull(singleValue);
                assertTrue(singleValue.equals(anotherValue));

                for (int i = 0; i < numCols; i++)
                    cols.add(new CqlRowData(tableName, rowkey, column + i, value));

                crud.insertRows(cols);
                Thread.sleep(200);

                singleValue = crud.getColumnValue(tableName, rowkey, anotherColumn);
                assertNotNull(singleValue);
                assertTrue(singleValue.equals(anotherValue));

                //TODO, test getColumnValues

                if (inClause) fail("not implemented");
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }

    @Test
    public void testGetColumnsMap()
    {
        String rowkey = "testGetColMap_" + Math.random();
        String column = "col_";
        String otherCol = "noise";

        int numCols = 20;
        List<CqlRowData> cols = new ArrayList<CqlRowData>();

        try
        {
            for (int i = 0; i < numCols; i++)
            {
                cols.add(new CqlRowData(tableName, rowkey, column + i, DataSourceUtility.stringToByteBuffer("" + i)));
                crud.insertRow(tableName, rowkey, otherCol, DataSourceUtility.stringToByteBuffer("someValue"), true);
            }
            crud.insertRows(cols);
            Thread.sleep(200);

            String start = column + 1;
            String end = column + 5;
            int limit = 5;
            String pattern = column;

            SortedMap<String, ByteBuffer> map1 = crud.getColumnsMap(tableName, rowkey, pattern);
            SortedMap<String, ByteBuffer> map2 = crud.getColumnsMap(tableName, rowkey, pattern, limit);
            SortedMap<String, ByteBuffer> map3 = crud.getColumnsMap(tableName, rowkey, start, end);
            SortedMap<String, ByteBuffer> map4 = crud.getColumnsMap(tableName, rowkey, start, end, limit);

            assertFalse(map1.isEmpty());
            assertFalse(map2.isEmpty());
            assertFalse(map3.isEmpty());
            assertFalse(map4.isEmpty());

            int count = 0;
            for (String colName: map1.keySet())
            {
                count++;
                assertTrue( (colName.split("_")[1]).equals( DataSourceUtility.getString(map1.get(colName)) ) );
            }
            assertTrue(count == numCols);

            count = 0;
            for (String colName: map2.keySet())
            {
                count++;
                assertTrue( (colName.split("_")[1]).equals( DataSourceUtility.getString(map2.get(colName)) ) );
            }
            assertTrue(count == limit);

            count = 0;
            String last = "";
            for (String colName: map3.keySet())
            {
                if (count == 0)
                    assertTrue(colName.equals(start));
                count++;
                last = colName;
            }
            assertTrue(last.equals(end));
            assertTrue(count == 15);

            count = 0;
            for (String colName: map4.keySet())
            {
                count++;
                assertTrue( (colName.split("_")[1]).equals( DataSourceUtility.getString(map4.get(colName)) ) );
            }
            assertTrue(count == limit);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
    }

    @Test
    public void testGetRowsMap()
    {
        //TODO, needs cassandra 2
        if (inClause) fail("not implemented");
    }

    ////////////////////////////////////////////////////
    /////////////////// Cache Tests ////////////////////
    ////////////////////////////////////////////////////

    @Test
    public void testCacheInsertAndDelete()
    {
        for (int i = 0; i < repeats; i++)
        {
            String rowkey = "testCacheInsertAndDelete_" + Math.random();
            String column = "col1";
            ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");
            CqlRowData row = new CqlRowData(tableName, rowkey, column, value);

            try
            {
                cache.insert(row);
                cache.flush();
                Thread.sleep(500);

                Iterator<ColumnAndValue> resultIt = crud.getAllColumns(tableName, rowkey);

                assertTrue(resultIt.hasNext());

                ColumnAndValue result = resultIt.next();

                String colResult = result.column;
                assertEquals(colResult, column);

                ByteBuffer valResult = result.value;
                assertEquals(valResult, value);

                cache.deleteRow(row);
                cache.flush();
                Thread.sleep(500);

                resultIt = crud.getAllColumns(tableName, rowkey);

                assertFalse(resultIt.hasNext());
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }

    @Test
    public void testWrongTable() throws Exception
    {
        try
        {
            List<CqlRowData> rows = new ArrayList<CqlRowData>();

            int numRows = bufferSize + 5; // any number greater than bufferSize
            for (int i = 0; i < numRows; i++)
            {
                String tn        = (i % 2 == 0) ? tableName : "bizarre";
                String rowkey    = "testWrongTable" + Math.random();
                String column    = "col1";
                ByteBuffer value = DataSourceUtility.stringToByteBuffer("testWrongTable");

                CqlRowData row = new CqlRowData(tn, rowkey + i, column, value);
                cache.insert(row);
            }

            cache.flush();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }


    @Test
    public void testCacheInsertAndDeleteColumn()
    {
        for (int i = 0; i < repeats; i++)
        {
            String rowkey = "testCacheInsertAndDeleteColumn_" + Math.random();

            String column1 = "col1";
            String column2 = "col2";
            ByteBuffer value1 = DataSourceUtility.stringToByteBuffer("testColumnValue");
            ByteBuffer value2 = DataSourceUtility.stringToByteBuffer("anotherTestColumnValue");

            CqlRowData rowCol1 = new CqlRowData(tableName, rowkey, column1, value1);
            CqlRowData rowCol2 = new CqlRowData(tableName, rowkey, column2, value2);

            try
            {
                // insert 2 columns
                cache.insert(rowCol1);
                cache.insert(rowCol2);
                cache.flush();
                Thread.sleep(500);

                Iterator<ColumnAndValue> allColumns = crud.getAllColumns(tableName, rowkey);

                // check that there are 2 columns
                assertTrue(allColumns.hasNext());
                ColumnAndValue col = allColumns.next();
                assertTrue(allColumns.hasNext());
                col = allColumns.next();
                assertFalse(allColumns.hasNext());

                // delete 1 column
                CqlRowData delColRowKey = new CqlRowData(tableName, rowkey, column1);
                cache.deleteColumn(delColRowKey);
                cache.flush();
                Thread.sleep(500);

                allColumns = crud.getAllColumns(tableName, rowkey);

                // check that there is only 1 column left
                assertTrue(allColumns.hasNext());
                col = allColumns.next();
                assertFalse(allColumns.hasNext());

                // confirm it is the correct column
                String colResult = col.column;
                assertEquals(colResult, column2);
                assertEquals(col.value, value2);
            }
            catch (CqlException e)
            {
                e.printStackTrace();
                fail("CqlException");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                fail("InterruptedException");
            }
        }
    }




    @Test
    public void testFilledBuffer()
    {
        String rowkey = "testFilledBuffer_" + Math.random() + "_";
        String column = "col1";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("testColumnValue");

        List<CqlRowData> rows = new ArrayList<CqlRowData>();

        int numRows = bufferSize + 10; // buffer must be >= 10
        for (int i = 0; i < numRows; i++)
        {
            rows.add(new CqlRowData(tableName, rowkey + i, column, value));
        }

        try
        {
            // test insert
            cache.flush();
            for (int j = 0; j < numRows; j++)
                cache.insert(rows.get(j));
            Thread.sleep(2000);

            for (int k = 0; k <= bufferSize; k++)
                assertTrue(crud.getAllColumns(tableName, rowkey + k).hasNext());

            // second cache should not have been inserted yet
            for (int l = bufferSize + 1; l < numRows; l++)
                assertFalse(crud.getAllColumns(tableName, rowkey + l).hasNext());

            cache.flush();
            Thread.sleep(2000);

            for (int m = bufferSize + 1; m < numRows; m++)
                assertTrue(crud.getAllColumns(tableName, rowkey + m).hasNext());

            cache.flush();

            // test delete
            for (int n = 0; n < numRows; n++)
                cache.deleteRow(rows.get(n));
            Thread.sleep(2000);

            for (int o = 0; o <= bufferSize; o++)
                assertFalse(crud.getAllColumns(tableName, rowkey + o).hasNext());

            // second cache should not have been deleted yet
            for (int p = bufferSize + 1; p < numRows; p++)
                assertTrue(crud.getAllColumns(tableName, rowkey + p).hasNext());

            cache.flush();
            Thread.sleep(2000);

            for (int q = bufferSize + 1; q < numRows; q++)
                assertFalse(crud.getAllColumns(tableName, rowkey + q).hasNext());
        }
        catch (CqlException e)
        {
            e.printStackTrace();
            fail("CqlException");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("InterruptedException");
        }
    }
}
