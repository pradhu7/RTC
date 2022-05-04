package com.apixio.datasource.cassandra;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by dyee on 3/8/18.
 */
public class StatementGeneratorTest
{
    String[] cols = {"rowkey", "col", "d"};
    StatementGenerator statementGenerator;
    String tableName;

    @Before
    public void setup()
    {
        statementGenerator = new StatementGenerator(cols);
        tableName = "cf10000001";
    }

    @Test
    public void testInsert()
    {
        String insertStatement = statementGenerator.insert(tableName,10l);
        System.out.println(insertStatement);
        Assert.assertNotNull(insertStatement);
        Assert.assertTrue(insertStatement.startsWith("INSERT"));
        Assert.assertTrue(insertStatement.contains(tableName));

        for (String col : cols)
        {
            Assert.assertTrue(insertStatement.contains(col));
        }

        Assert.assertEquals("INSERT INTO cf10000001 (rowkey,col,d) VALUES (?,?,?) using ttl 10", insertStatement);
    }


    @Test
    public void testInsert1()
    {
        String insertStatement = statementGenerator.insert(tableName,0l);
        System.out.println(insertStatement);
        Assert.assertNotNull(insertStatement);
        Assert.assertTrue(insertStatement.startsWith("INSERT"));
        Assert.assertTrue(insertStatement.contains(tableName));

        for (String col : cols)
        {
            Assert.assertTrue(insertStatement.contains(col));
        }

        Assert.assertEquals("INSERT INTO cf10000001 (rowkey,col,d) VALUES (?,?,?)", insertStatement);
    }

    @Test
    public void testInsert2()
    {
        String insertStatement = statementGenerator.insert(tableName,true, 0l);
        System.out.println(insertStatement);
        Assert.assertNotNull(insertStatement);
        Assert.assertTrue(insertStatement.startsWith("INSERT"));
        Assert.assertTrue(insertStatement.contains(tableName));

        for (String col : cols)
        {
            Assert.assertTrue(insertStatement.contains(col));
        }

        Assert.assertEquals("INSERT INTO cf10000001 (rowkey,col,d) VALUES (?,?,?) if not exists", insertStatement);
    }

    @Test
    public void testInsert3()
    {
        String insertStatement = statementGenerator.insert(tableName,true, 10l);
        System.out.println(insertStatement);
        Assert.assertNotNull(insertStatement);
        Assert.assertTrue(insertStatement.startsWith("INSERT"));
        Assert.assertTrue(insertStatement.contains(tableName));

        for (String col : cols)
        {
            Assert.assertTrue(insertStatement.contains(col));
        }

        Assert.assertEquals("INSERT INTO cf10000001 (rowkey,col,d) VALUES (?,?,?) if not exists using ttl 10", insertStatement);
    }

    @Test
    public void testInsertInvalidTable()
    {
        testInsertInvalidTable(null);
        testInsertInvalidTable("");
        testInsertInvalidTable("     ");
    }

    @Test
    public void testSelect_WithFieldsValueList()
    {
        FieldValueList fieldValueList = new FieldValueListBuilder()
                .addString("a", "b")
                .addString("c", "d")
                .addByteBuffer("f", null).build();

        String selectStatement = statementGenerator.select(tableName, fieldValueList);
        System.out.println(selectStatement);
        Assert.assertNotNull(selectStatement);
        Assert.assertTrue(selectStatement.startsWith("SELECT"));
        Assert.assertTrue(selectStatement.contains(tableName));

        for (String col : cols)
        {
            Assert.assertTrue(selectStatement.contains(col));
        }

        Assert.assertEquals("SELECT rowkey,col,d FROM cf10000001 where a = ?  AND  c = ? ", selectStatement);
    }

    @Test
    public void testCreate()
    {
        FieldValueList fieldValueList = new FieldValueListBuilder()
                .addString("rowkey", null)
                .addString("col", null)
                .addByteBuffer("d", null).build();

        String partitionKey = "(rowkey)";
        String clusterKey = "col";

        String createStatement = statementGenerator.create(tableName, fieldValueList, partitionKey, clusterKey);
        System.out.println(createStatement);
        Assert.assertNotNull(createStatement);
        Assert.assertTrue(createStatement.trim().startsWith("CREATE"));
        Assert.assertTrue(createStatement.contains(tableName));

        for (String col : cols)
        {
            Assert.assertTrue(createStatement.contains(col));
        }

        Assert.assertTrue(createStatement.contains(partitionKey));
        Assert.assertTrue(createStatement.contains(clusterKey));
    }

    @Test
    public void testSelect()
    {
        String selectStatement = statementGenerator.select(tableName, null);
        System.out.println(selectStatement);

        Assert.assertNotNull(selectStatement);
        Assert.assertTrue(selectStatement.startsWith("SELECT"));
        Assert.assertTrue(selectStatement.contains(tableName));

        for (String col : cols)
        {
            Assert.assertTrue(selectStatement.contains(col));
        }

        Assert.assertEquals("SELECT rowkey,col,d FROM cf10000001", selectStatement);
    }

    @Test
    public void testSelectInvalidTable()
    {
        testSelectInvalidTable(null);
        testSelectInvalidTable("");
        testSelectInvalidTable("     ");
    }

    private void testSelectInvalidTable(String table)
    {
        try
        {
            statementGenerator.select(table, null);
            Assert.fail();
        } catch (Exception ex)
        {
            //ignore, this should happen!!
        }
    }

    private void testInsertInvalidTable(String table)
    {
        try
        {
            statementGenerator.insert(table, 0l);
            Assert.fail();
        } catch (Exception ex)
        {
            //ignore, this should happen!!
        }
    }
}
