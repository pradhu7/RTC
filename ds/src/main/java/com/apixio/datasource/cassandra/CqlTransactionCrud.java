package com.apixio.datasource.cassandra;

import java.nio.ByteBuffer;
import java.util.List;

import com.apixio.datasource.cassandra.CqlRowData.CqlOps;

/**
 * This class supports a client-side transaction capbility by delaying the
 * actual operations against the Cassandra cluster and keeping them in a thread
 * local cache that's applied when the final commit is done.
 *
 * Client-side "transaction" support.  Clients MUST use a catch/finally block
 * to not screw up later operations due to a thread local not being cleaned up:
 *
 *  cqlCrud.beginTransaction();
 *
 *  try {
 *    ... cqlCrud stuff ...
 *    cqlCrud.commitTransaction();
 *  } catch (Exception x) {
 *    cqlCrud.abortTransaction();
 *  }
 */
public class CqlTransactionCrud extends CqlCrud
{
    private static ThreadLocal<Boolean> tlInTransaction = new ThreadLocal<Boolean>();

    private int bufferSize;

    public void setBufferSize(int size)
    {
        bufferSize = size;
    }

    /**
     * Support for client-side transaction-like functionality is done by a thread local
     * (kept by ThreadLocalCqlCache).  If there is such a thread local then we're
     * in a transaction and calls to insert/delete rows/columns are just added to the
     * local cache; the commit will just do an insertOrDeleteOps on it and abort will
     * discard.
     */
    private ThreadLocalCqlCache transaction;

    /*
     * Implementation WARNING:  in order to not use yet another ThreadLocal, this code MUST
     * NEVER CALL transaction.getCache() UNLESS we want to mark the thread as being in a
     * transaction!
     */

    public CqlTransactionCrud()
    {
        super();

        transaction = new ThreadLocalCqlCache();

        transaction.setCqlCrud(this);
    }

    /**
     * Return true IFF beginTransaction has been called since the latest commit or abort.
     * This is a bit of a hack in that we're relying on two things:  that no code in here
     * calls transaction.getCache() prior to beginTransaction(), and that ThreadLocalCqlCache
     * code doesn't set the actual thread local unless ThreadLocalCqlCache.getCache is called.
     */
    public boolean inTransaction()
    {
        Boolean in = tlInTransaction.get();

        return (in != null) && in.booleanValue();
    }

    public void beginTransaction()
    {
        if (inTransaction())
            throw new IllegalStateException("Attempt to begin a Cql transaction but thread is already in one and subtransactions are not supported");

        transaction.cleanCache();
        transaction.setDontFlush(true);

        tlInTransaction.set(Boolean.TRUE);
    }

    public void commitTransaction()
    {
        if (!inTransaction())
            throw new IllegalStateException("Attempt to commit a Cql transaction but the thread is not in a transaction");

        List<CqlRowData> mods = transaction.getCache();

        tlInTransaction.set(null);

        transaction.cleanCache();

        if (mods.size() > 0)
            super.insertOrDeleteOps(mods);
    }

    public void abortTransaction()
    {
        if (!inTransaction())
            throw new IllegalStateException("Attempt to abort a Cql transaction but the thread is not in a transaction");

        tlInTransaction.set(null);

        transaction.cleanCache();
    }

    // ################################################################
    // Actual overrides of methods that modify Cassandra.  If the thread is
    // in a transaction, these overridden methods accumulate the modifications
    // in the thread local, otherwise they just delegate to CqlCrud.

    @Override
    public boolean insertOrDeleteOps(List<CqlRowData> cqlRowDataList)
    {
        if (inTransaction())
        {
            for (CqlRowData crd : cqlRowDataList)
                transaction.addToCache(crd);

            return true;
        }
        else
        {
            return super.insertOrDeleteOps(cqlRowDataList);
        }
    }

    @Override
    public boolean insertRow(String tableName, String rowkey, String column, ByteBuffer value, boolean useAsync)
    {
        if (inTransaction())
            return addToTransaction(new CqlRowData(tableName, rowkey, column, value), CqlOps.insert);
        else
            return super.insertRow(tableName, rowkey, column, value, useAsync);
    }

    @Override
    public boolean insertRows(List<CqlRowData> cqlRowDataList)
    {
        if (inTransaction())
            return addToTransaction(cqlRowDataList, CqlOps.insert);
        else
            return super.insertRows(cqlRowDataList);
    }

    @Override
    public boolean deleteRow(String tableName, String rowkey, boolean useAsync)
    {
        if (inTransaction())
            return addToTransaction(new CqlRowData(tableName, rowkey), CqlOps.deleteRow);
        else
            return super.deleteRow(tableName, rowkey, useAsync);
    }

    @Override
    public boolean deleteRows(List<CqlRowData> cqlRowDataList)
    {
        if (inTransaction())
            return addToTransaction(cqlRowDataList, CqlOps.deleteRow);
        else
            return super.deleteRows(cqlRowDataList);
    }

    @Override
    public boolean deleteColumn(String tableName, String rowkey, String column, boolean useAsync)
    {
        if (inTransaction())
            return addToTransaction(new CqlRowData(tableName, rowkey, column), CqlOps.deleteColumn);
        else
            return super.deleteColumn(tableName, rowkey, column, useAsync);
    }

    @Override
    public boolean deleteColumns(List<CqlRowData> cqlRowDataList)
    {
        if (inTransaction())
            return addToTransaction(cqlRowDataList, CqlOps.deleteColumn);
        else
            return super.deleteColumns(cqlRowDataList);
    }

    /**
     * Internal transaction support.
     */
    private boolean addToTransaction(CqlRowData crd, CqlOps ops)
    {
        // force ops; needed since no guarantees that client set prior to calling the method whose name
        // makes such setting unnecessary (e.g., a client that calls deleteRows won't assume that it
        // also needs to set ops itself)
        crd.setCqlOps(ops);
        transaction.addToCache(crd);

        return true;
    }

    private boolean addToTransaction(List<CqlRowData> crds, CqlOps ops)
    {
        for (CqlRowData crd : crds)
            addToTransaction(crd, ops);

        return true;
    }
}
