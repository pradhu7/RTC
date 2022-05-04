package com.apixio.restbase.web;

import java.util.Map;

import com.apixio.restbase.DataServices;
import com.apixio.datasource.cassandra.CqlException;
import com.apixio.restbase.util.CqlTransactions;

/**
 */
public class CqlTransaction extends Microfilter<DataServices>
{
    private CqlTransactions cqlTrans;

    public CqlTransaction()
    {
    }

    /**
     */
    @Override
    public void configure(Map<String, Object> filterConfig, DataServices services)
    {
        super.configure(filterConfig, services);

        cqlTrans = services.getCqlTransactions();

        if (cqlTrans == null)
            throw new IllegalStateException("System is configured to provide Cql transactions but SysServices CqlCrud object was created with CqlCrud rather than CqlTransactionCrud");
    }

    /**
     * 
     */
    @Override
    public Action beforeDispatch(Context ctx)
    {
        cqlTrans.begin();

        return Action.NeedsUnwind;
    }

    /**
     *
     */
    @Override
    public Action afterDispatch(Context ctx)
    {
        try
        {
            if (ctx.error)
                cqlTrans.abort();
            else
                cqlTrans.commit();
        }
        catch (CqlException cx)
        {
            cx.printStackTrace();
        }

        return Action.NextInChain;
    }

}
