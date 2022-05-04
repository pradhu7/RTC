package com.apixio.dao.nassembly;

import com.apixio.dao.DAOTestUtils;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.FieldValueList;
import com.apixio.datasource.cassandra.FieldValueListBuilder;
import com.apixio.datasource.cassandra.StatementGenerator;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.fail;

// @Ignore("Integration")
public class AssemblyDAOTest2
{
    private String assemblyTable = "cfvram2";
    private String partitionKey = "row";
    private String clusterKey1 = "dataBucket";
    private String clusterKey2 = "code";
    private String nonClusterKey = "name";
    private String buffer = "buf";

    private String       dataTypeTable;
    private DAOTestUtils util;
    private CqlCrud      cqlCrud;
    private String       pdsId;
    private String       database = "application";
    private AssemblyDAO  assemblyDAO;


    @Before
    public void setUp() throws Exception
    {
        util = new DAOTestUtils();
        cqlCrud = util.daoServices.getApplicationCqlCrud();
        cqlCrud.getCqlCache().setBufferSize(0);

        pdsId = util.testCassandraTables.testOrg;
        dataTypeTable = util.testCassandraTables.nsummary;

        assemblyDAO = new AssemblyDAOImpl();
        assemblyDAO.setDrivers(new HashMap<String, Object>() {{
            put(database, cqlCrud);
        }});

        createDataTypeTable();
        createTable();
    }

    @After
    public void setAfter() throws Exception
    {
    }

    private void createDataTypeTable() throws Exception
    {
        String[] cols = {"rowkey", "col", "d"};
        StatementGenerator statementGenerator;

        statementGenerator = new StatementGenerator(cols);

        FieldValueList fieldValueList = new FieldValueListBuilder()
                .addString("rowkey", null)
                .addString("col", null)
                .addByteBuffer("d", null).build();

        cqlCrud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, dataTypeTable, fieldValueList, "rowkey", "col");
    }

    private void createTable() throws Exception
    {
        String[] cols = {partitionKey, clusterKey1, clusterKey2, nonClusterKey, buffer};
        StatementGenerator statementGenerator;

        statementGenerator = new StatementGenerator(cols);

        FieldValueList fieldValueList = new FieldValueListBuilder()
                .addString(partitionKey, null)
                .addLong(clusterKey1, null)
                .addString(clusterKey2, null)
                .addString(nonClusterKey, null)
                .addByteBuffer(buffer, null).build();

        cqlCrud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, assemblyTable, fieldValueList, partitionKey, clusterKey1 + "," + clusterKey2);
    }

    @Test
    public void TestWrite()
    {
        AssemblyDAO.Meta meta = new AssemblyDAO.Meta(database, assemblyTable, dataTypeTable, pdsId, "demo", 0);

        String assemblyID = UUID.randomUUID().toString();
        byte[] serial = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        Map<String, Object> cluster = new HashMap() { {put(clusterKey1, 25L); put(clusterKey2, "someCode");} };
        Map<String, Object> nonCluster = new HashMap() { {put(nonClusterKey, "someName");} };
        AssemblyDAO.Data data = new AssemblyDAO.Data(partitionKey, assemblyID, cluster, nonCluster, buffer, serial);

        try
        {
            assemblyDAO.write(meta, data);
            Thread.sleep(2000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void TestRead()
    {
        AssemblyDAO.Meta meta = new AssemblyDAO.Meta(database, assemblyTable, dataTypeTable, pdsId, "demo", 0);

        String assemblyID = UUID.randomUUID().toString();
        byte[] serial = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        Map<String, Object> cluster = new HashMap() { {put(clusterKey1, 25L); put(clusterKey2, "someCode");} };
        Map<String, Object> nonCluster = new HashMap() { {put(nonClusterKey, "someName");} };
        AssemblyDAO.Data data = new AssemblyDAO.Data(partitionKey, assemblyID, cluster, nonCluster, buffer, serial);

        try
        {
            assemblyDAO.write(meta, data);
            Thread.sleep(2000);

            Map<String, Object> nonCluster1 = new HashMap() { {put(nonClusterKey, null);} };
            AssemblyDAO.Data data1 = new AssemblyDAO.Data(partitionKey, assemblyID, cluster, nonCluster1, buffer, null);
            List<AssemblyDAO.QueryResult> result = assemblyDAO.read(meta, data1);
            Assert.assertEquals(serial.length, result.get(0).getProtobuf().length);
            Assert.assertNotSame(0, result.get(0).timeInMicroseconds);
            Assert.assertEquals(new String(serial), new String(result.get(0).getProtobuf()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}