package com.apixio.datasource.cassandra;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a cql cache to be used locally.
 */
public class LocalCqlCache extends CqlCache
{
    private List<CqlRowData> cqlCache = new ArrayList<>();

    @Override
    protected void addToCache(CqlRowData cqlRowData)
    {
        cqlCache.add(cqlRowData);
    }

    @Override
    protected List<CqlRowData> getCache()
    {
        return cqlCache;
    }

    @Override
    public int cacheSize()
    {
        return cqlCache.size();
    }

    @Override
    protected void cleanCache()
    {
        cqlCache.clear();
    }

    @Override
    public void flush()
    {
        List<CqlRowData> all = getCache();

        if (all != null && !all.isEmpty())
        {
            totalBufferWrites++;
            cqlCrud.insertOrDeleteOps(buildRawDataSet(all));
        }

        cleanCache();
    }

    public static void main(String[] args) throws Exception
    {
        LocalCqlCache cache = new LocalCqlCache();
        cache.test0();
        cache.test1();
        cache.test2();
        cache.test3();
        cache.test4();
        cache.test5();
        cache.test6();
    }

    private void test0() throws Exception
    {
        cleanCache();

        for (int i = 0; i < 10; i++)
        {
            int j = i % 2 == 0 ? 1 : 5;

            CqlRowData rd = new CqlRowData("t" + j, "r" + j, "c" + j, ByteBuffer.wrap(("" + i).getBytes("UTF-8")));
            if (i == 5)
                rd.setCqlOps(CqlRowData.CqlOps.deleteRow);
            else if (i == 9)
                rd.setCqlOps(CqlRowData.CqlOps.deleteColumn);
            else
                rd.setCqlOps(CqlRowData.CqlOps.insert);
            addToCache(rd);
        }

        System.out.println("#################### test1");
        List<CqlRowData> rds = getCache();
        System.out.println(rds);
        System.out.println(buildRawDataSet(rds));
        System.out.println("####################");
    }

    private void test1() throws Exception
    {
        cleanCache();

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t" + i, "r" + i, "c" + i, ByteBuffer.wrap(("" + i).getBytes("UTF-8")));
            rd.setCqlOps(CqlRowData.CqlOps.insert);
            addToCache(rd);
        }

        System.out.println("#################### test1");
        List<CqlRowData> rds = getCache();
        System.out.println(rds);
        System.out.println(buildRawDataSet(rds));
        System.out.println("####################");
    }

    private void test2() throws Exception
    {
        cleanCache();

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r", "c", ByteBuffer.wrap(("" + i).getBytes("UTF-8")));
            rd.setCqlOps(CqlRowData.CqlOps.insert);
            addToCache(rd);
        }

        System.out.println("#################### test2");
        List<CqlRowData> rds = getCache();
        System.out.println(rds);
        System.out.println(buildRawDataSet(rds));
        System.out.println("####################");
    }

    private void test3() throws Exception
    {
        cleanCache();

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r", "c", ByteBuffer.wrap(("" + i).getBytes("UTF-8")));
            rd.setCqlOps(CqlRowData.CqlOps.insert);
            addToCache(rd);
        }

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r");
            rd.setCqlOps(CqlRowData.CqlOps.deleteRow);
            addToCache(rd);
        }

        System.out.println("#################### test3");
        List<CqlRowData> rds = getCache();
        System.out.println(rds);
        System.out.println(buildRawDataSet(rds));
        System.out.println("####################");
    }

    private void test4() throws Exception
    {
        cleanCache();

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r");
            rd.setCqlOps(CqlRowData.CqlOps.deleteRow);
            addToCache(rd);
        }

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r", "c", ByteBuffer.wrap(("" + i).getBytes("UTF-8")));
            rd.setCqlOps(CqlRowData.CqlOps.insert);
            addToCache(rd);
        }

        System.out.println("#################### test4");
        List<CqlRowData> rds = getCache();
        System.out.println(rds);
        System.out.println(buildRawDataSet(rds));
        System.out.println("####################");
    }

    private void test5() throws Exception
    {
        cleanCache();

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r");
            rd.setCqlOps(CqlRowData.CqlOps.deleteRow);
            addToCache(rd);
        }

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r", "c");
            rd.setCqlOps(CqlRowData.CqlOps.deleteColumn);
            addToCache(rd);
        }

        System.out.println("#################### test5");
        List<CqlRowData> rds = getCache();
        System.out.println(rds);
        System.out.println(buildRawDataSet(rds));
        System.out.println("####################");
    }

    private void test6() throws Exception
    {
        cleanCache();

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r", "c", ByteBuffer.wrap(("" + i).getBytes("UTF-8")));
            if (i == 5)
                rd.setCqlOps(CqlRowData.CqlOps.insert);
            else
                rd.setCqlOps(CqlRowData.CqlOps.deleteColumn);
            addToCache(rd);
        }

        for (int i = 0; i < 10; i++)
        {
            CqlRowData rd = new CqlRowData("t", "r");
            rd.setCqlOps(CqlRowData.CqlOps.deleteRow);
            addToCache(rd);
        }

        System.out.println("#################### test6");
        List<CqlRowData> rds = getCache();
        System.out.println(rds);
        System.out.println(buildRawDataSet(rds));
        System.out.println("####################");
    }
}
