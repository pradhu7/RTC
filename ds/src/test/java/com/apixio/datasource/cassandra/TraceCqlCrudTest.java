package com.apixio.datasource.cassandra;

import com.apixio.datasource.cassandra.field.ByteBufferField;
import com.apixio.datasource.cassandra.field.StringField;
import com.apixio.utility.DataSourceUtility;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by dyee on 3/8/18.
 */
public class TraceCqlCrudTest
{
    @Test
    public void testTraceOpsOff()
    {
        TraceCqlCrud traceCqlCrud = new TraceCqlCrud();
        traceCqlCrud.setAddTraceOps(false);

        String rowKey = "r" + Math.random();
        String col = "c" + Math.random();
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("v" + Math.random());

        CqlRowData cqlRowData = new CqlRowData("cf1001", rowKey, col, value);

        traceCqlCrud.addToTrace(cqlRowData);

        Map<String, ByteBuffer> serializedTraces = traceCqlCrud.serializeTraces();

        Assert.assertEquals(0, serializedTraces.size());
    }

    @Ignore("Integration")
    @Test
    public void testCqlRowDataSerializeDeserialize_Conversion()
    {
        TraceCqlCrud traceCqlCrud = new TraceCqlCrud();
        traceCqlCrud.setAddTraceOps(true);

        String rowKey = "r" + Math.random();
        String col = "c" + Math.random();
        String tableName = "cf1001";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("v" + Math.random());

        CqlRowData cqlRowData = new CqlRowData(tableName, rowKey, col, value);
        cqlRowData.setCqlOps(CqlRowData.CqlOps.insert);

        traceCqlCrud.addToTrace(cqlRowData);

        Map<String, ByteBuffer> serializedTraces = traceCqlCrud.serializeTraces();

        Assert.assertEquals(1, serializedTraces.size());

        //
        // Get the serialized trace output
        //
        String traceKey = serializedTraces.keySet().iterator().next();
        ByteBuffer traceValue = serializedTraces.get(traceKey);
        Assert.assertNotNull(traceKey);
        Assert.assertNotNull(traceValue);

        TraceCqlCrud.TraceIdAndCqlRowData traceIdAndCqlRowData = traceCqlCrud.deserializeTrace(traceKey, traceValue);

        //
        // Verify that the trace output is correct...
        //
        CqlRowData traceCqlRowData = traceIdAndCqlRowData.cqlRowData;
        assertCqlGeneratorRowData(tableName, rowKey, col, value, traceCqlRowData);
    }

    @Ignore("Integration")
    @Test
    public void testCassandralRowDataSerializeDeserialize()
    {
        TraceCqlCrud traceCqlCrud = new TraceCqlCrud();
        traceCqlCrud.setAddTraceOps(true);

        String rowKey = "r" + Math.random();
        String col = "c" + Math.random();
        String tableName = "cf1001";
        ByteBuffer value = DataSourceUtility.stringToByteBuffer("v" + Math.random());

        CqlRowData cqlGeneratorRowData = new CqlRowData(tableName, new StatementGenerator("rowkey", "col", "d"), new FieldValueListBuilder()
                .addPartitionKey(new StringField("rowkey"), rowKey)
                .addColumn(new StringField("col"), col)
                .addByteBuffer("d", value)
                .build());

        cqlGeneratorRowData.setCqlOps(CqlRowData.CqlOps.insert);

        traceCqlCrud.addToTrace(cqlGeneratorRowData);

        Map<String, ByteBuffer> serializedTraces = traceCqlCrud.serializeTraces();

        Assert.assertEquals(1, serializedTraces.size());

        //
        // Get the serialized trace output
        //
        String traceKey = serializedTraces.keySet().iterator().next();
        ByteBuffer traceValue = serializedTraces.get(traceKey);
        Assert.assertNotNull(traceKey);
        Assert.assertNotNull(traceValue);

        TraceCqlCrud.TraceIdAndCqlRowData traceIdAndCqlRowData = traceCqlCrud.deserializeTrace(traceKey, traceValue);

        //
        // Verify that the trace output is correct...
        //
        CqlRowData traceCqlRowData = traceIdAndCqlRowData.cqlRowData;
        Assert.assertTrue(traceCqlRowData instanceof CqlRowData);

        CqlRowData traceCqlGeneratorRowData = (CqlRowData) traceCqlRowData;
        assertCqlGeneratorRowData(tableName, rowKey, col, value, traceCqlGeneratorRowData);
    }


    private void assertCqlGeneratorRowData(String tableName, String rowKey, String col, ByteBuffer value, CqlRowData traceCqlGeneratorRowData)
    {
        Assert.assertEquals(tableName, traceCqlGeneratorRowData.tableName);
        Assert.assertEquals(CqlRowData.CqlOps.insert, traceCqlGeneratorRowData.ops);

        Assert.assertNotNull(traceCqlGeneratorRowData.statementGenerator);
        FieldList fieldList = traceCqlGeneratorRowData.statementGenerator.fieldList;
        Assert.assertEquals(4, fieldList.fields.size());

        // make sure field list from statement generator is correct
        Assert.assertTrue(fieldList.fields.contains("rowkey"));
        Assert.assertTrue(fieldList.fields.contains("col"));
        Assert.assertTrue(fieldList.fields.contains("d"));

        FieldValueList fieldValueList = traceCqlGeneratorRowData.getFieldValueList();
        Assert.assertNotNull(fieldValueList);

        // Assert only four values
        Assert.assertEquals(4, fieldValueList.getFieldValue().size());

        //make sure we have rowkey, col, and d
        String key = "d";

        ByteBuffer testByteBufferValue = (ByteBuffer) fieldValueList.getFieldValue().get(new ByteBufferField(key));
        Assert.assertNotNull(testByteBufferValue);
        Assert.assertEquals(testByteBufferValue.array(), value.array());

        key = "rowkey";
        String testStringValue = (String) fieldValueList.getFieldValue().get(new StringField(key));
        Assert.assertNotNull(testStringValue);
        Assert.assertEquals(testStringValue, rowKey);

        key = "col";
        testStringValue = (String) fieldValueList.getFieldValue().get(new StringField(key));
        Assert.assertNotNull(testStringValue);
        Assert.assertEquals(testStringValue, col);

        key = "column"; //backwards compt
        testStringValue = (String) fieldValueList.getFieldValue().get(new StringField(key));
        Assert.assertNotNull(testStringValue);
        Assert.assertEquals(testStringValue, "");
    }
}