package com.apixio.restbase.util;

import com.apixio.datasource.cassandra.CqlTransactionCrud;
import com.apixio.datasource.cassandra.CqlException;

/**
 * A layer over Cql transactions for the purpose of being similar to RedisTransactions
 * and to make it so that client code looks similar
 */
public class CqlTransactions {

    private CqlTransactionCrud cqlCrud;

    public CqlTransactions(CqlTransactionCrud cqlCrud)
    {
        this.cqlCrud = cqlCrud;
    }

    public void begin()
    {
        cqlCrud.beginTransaction();
    }

    public void commit() throws CqlException
    {
        cqlCrud.commitTransaction();
    }

    public void abort()
    {
        cqlCrud.abortTransaction();
    }

    public boolean inTransaction()
    {
        return cqlCrud.inTransaction();
    }

}
