package com.apixio.dao.nassembly;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.fail;

import com.apixio.datasource.cassandra.FieldValueList;
import com.apixio.datasource.cassandra.FieldValueListBuilder;
import com.apixio.datasource.cassandra.StatementGenerator;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.apixio.dao.DAOTestUtils;
import com.apixio.datasource.cassandra.CqlCrud;

//TODO Make this test pass
@Ignore("Integration")
public class AssemblyDAOTest
{
    private DAOTestUtils util;
    private CqlCrud      cqlCrud;
    private String       pdsId;
    private String       database = "application";
    private String       table;
    private AssemblyDAO  assemblyDAO;

    @Before
    public void setUp() throws Exception
    {
        util = new DAOTestUtils();
        cqlCrud = util.daoServices.getApplicationCqlCrud();
        cqlCrud.getCqlCache().setBufferSize(0);

        pdsId = util.testCassandraTables.testOrg;
        table = util.testCassandraTables.nsummary;

        assemblyDAO = new AssemblyDAOImpl();
        assemblyDAO.setDrivers(new HashMap<String, Object>() {{
            put(database, cqlCrud);
        }});

        createTable();
    }

    private void createTable() throws Exception
    {
        String[] cols = {"rowkey", "col", "d"};
        StatementGenerator statementGenerator;

        statementGenerator = new StatementGenerator(cols);

        FieldValueList fieldValueList = new FieldValueListBuilder()
                .addString("rowkey", null)
                .addString("col", null)
                .addByteBuffer("d", null).build();

        cqlCrud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, table, fieldValueList, "rowkey", "col");
    }

    @Test
    public void TestWrite()
    {
        AssemblyDAO.Meta meta = new AssemblyDAO.Meta(database, table, table, pdsId, "demo", 0);
        String assemblyID = UUID.randomUUID().toString();
        byte[] serial = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        Map<String, Object> cluster = new HashMap() { { put("col", "merged"); } };
        AssemblyDAO.Data data = new AssemblyDAO.Data("rowkey", assemblyID, cluster, null, "d", serial);

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
        AssemblyDAO.Meta meta = new AssemblyDAO.Meta(database, table, table, pdsId, "demo", 0);
        String assemblyID = UUID.randomUUID().toString();
        byte[] serial = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        Map<String, Object> cluster = new HashMap() { { put("col", "merged"); } };
        AssemblyDAO.Data data = new AssemblyDAO.Data("rowkey", assemblyID, cluster, null, "d", serial);

        try
        {
            assemblyDAO.write(meta, data);
            Thread.sleep(2000);

            AssemblyDAO.Data data1 = new AssemblyDAO.Data("rowkey", assemblyID, cluster, null, "d", null);
            List<AssemblyDAO.QueryResult> result = assemblyDAO.read(meta, data1);
            Assert.assertEquals(serial.length, result.get(0).getProtobuf().length);
            Assert.assertEquals(new String(serial), new String(result.get(0).getProtobuf()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}