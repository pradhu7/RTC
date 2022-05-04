package com.apixio.restbase.web;

import java.util.Map;

import com.apixio.datasource.redis.Transactions;
import com.apixio.restbase.DataServices;

/**
 */
public class RedisTransaction extends Microfilter<DataServices>
{
    private Transactions redisTransaction;

    public RedisTransaction()
    {
    }

    public RedisTransaction(Map<String, Object> filterConfig, DataServices services)
    {
        configure(filterConfig, services);
    }

    /**
     */
    @Override
    public void configure(Map<String, Object> filterConfig, DataServices services)
    {
        super.configure(filterConfig, services);

        redisTransaction = services.getRedisTransactions();
    }

    /**
     * 
     */
    @Override
    public Action beforeDispatch(Context ctx)
    {
        redisTransaction.begin();

        return Action.NeedsUnwind;
    }

    /**
     *
     */
    @Override
    public Action afterDispatch(Context ctx)
    {
        if (ctx.error)
            redisTransaction.abort();
        else
            redisTransaction.commit();

        return Action.NextInChain;
    }

}
