package com.apixio.datasource.redis;

import redis.clients.jedis.Transaction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis Transaction support.  This class provides per-thread Redis-defined transactions
 * that begin with multi() and end with either exec() or discard().
 *
 * The runtime model is that the Jedis "Transaction" object is put on the thread in the
 * begin() and is used as necessary after that.  It is cleared from the thread at both
 * the commit() and abort() times.
 *
 * Note that the use of this class (typically in a web microfilter) introduces a
 * deadlock possibility that is avoided by the use of JedisPair: as the whole purpose of
 * this class is to provide a sort of transaction capability via Redis' MULTI/EXEC which
 * queues even "readonly" commands (in contrast to how SQL transactions work), we need
 * to allow a readonly operation that isn't queued in the MULTI connection.  So, this
 * class actually needs to grab *two* Jedis connections in an atomic way, otherwise we
 * end up with the dining philosopher's problem, which ends in deadlock.
 *
 * The above model requires cooperation with RedisOps so that it makes use of this
 * pre-reserved readonly Jedis connection instead of attempting to get another resource
 * (which could also end in deadlock).  That's what the method getReadonlyJedis is for
 */
public class Transactions
{
    /**
     * Thread locals are used to hold on to Jedis connections and the result of
     * Jedis.multi()
     */
    private static final ThreadLocal<Transaction> tlTransaction = new ThreadLocal<Transaction>();
    private static final ThreadLocal<Jedis>       tlTxJedis     = new ThreadLocal<Jedis>();  // for cmds that modify
    private static final ThreadLocal<Jedis>       tlRoJedis     = new ThreadLocal<Jedis>();  // for cmds that only read

    /**
     * Gets access to Jedis/Redis connections.  A connection is held as long as a transaction
     * is open.  This means that client code MUST do a commit/abort/clear after beginning a
     * transaction or a leak will occur!
     */
    final private JedisPool jedisPool;

    /**
     * Constructs a Java object representation of a real FIFO queue.
     */
    public Transactions(JedisPool pool)
    {
        this.jedisPool = pool;
    }

    /**
     * Begins a transaction and grabs a Jedis connection
     */
    public void begin()
    {
        if (inTransaction())
            throw new IllegalStateException("Nested redis transactions not supported");

        JedisPair jp = JedisPair.getPair(jedisPool);

        tlTxJedis.set(jp.jedis1);
        tlRoJedis.set(jp.jedis2);

        tlTransaction.set(jp.jedis1.multi());

        RedisOps.logCmd("begin tx");
    }

    /**
     * Aborts a transaction and releases the Jedis connection.
     */
    public void abort()
    {
        if (!inTransaction())
            throw new IllegalStateException("Attempt to abort a transaction but system is not in a transaction");

        try
        {
            RedisOps.logCmd("abort tx");

            tlTransaction.get().discard();
        }
        finally
        {
            clean();
        }
    }

    /**
     * Commits a transaction and releases the Jedis connection.
     */
    public void commit()
    {
        if (!inTransaction())
            throw new IllegalStateException("Attempt to commit a transaction but system is not in a transaction");

        // note that it makes NO SENSE to retry the commit on an exception
        // because the transaction is a server-side construct that doesn't
        // allow retries.
        try
        {
            RedisOps.logCmd("commit tx");

            tlTransaction.get().exec();
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
        finally
        {
            clean();
        }
    }

    /**
     * Gets the current Jedis "Transaction" object for the calling thread, if a begin() was
     * done.
     */
    public Transaction getTransaction()
    {
        return tlTransaction.get();
    }

    /**
     * Gets the current Jedis read-only Jedis connection that must be used for all non-modify
     * operations.
     */
    public Jedis getReadonlyJedis()
    {
        return tlRoJedis.get();
    }

    /**
     * Returns true if the current thread has opened a transaction.
     */
    public boolean inTransaction()
    {
        return getTransaction() != null;
    }

    /**
     * Release both Jedis connections and clears all thread locals.
     */
    public void clean()
    {
        (new JedisPair(tlRoJedis.get(), tlTxJedis.get())).releasePair();

        tlRoJedis.set(null);
        tlTxJedis.set(null);
        tlTransaction.set(null);
    }

}
