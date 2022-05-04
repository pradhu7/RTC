package com.apixio.datasource.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.SetParams;

/**
 * RedisOps exposes the Redis operations needed by the rest of the system.  This
 * is a thin layer over the Jedis API because the Jedis API doesn't deal with
 * transactions in a way that lets us have a single class definition that works
 * with both transactional and non-transactional operations.
 *
 * This class works with the Transactions class in a way that makes transactions
 * (mostly) transparent to client code that just does simple Redis operations.
 * NOTE:  in order to do this, the Transaction itself is kept on the thread.
 *
 * The operational model is that if no Redis transaction is started, then a call
 * to one of these methods gets a Jedis connection from the pool, and then makes
 * the call to the method, and then returns the connection to the pool.  If a
 * call is made within the context of a transaction (meaning that there is a
 * Transaction on the thread), then the Transaction object is used (and the fact
 * that Transaction doesn't extend Jedis is the reason why we need this class).
 *
 * There are 3 categories of calls:
 *
 *  1.  pure write operations that return mostly meaningless values (such as zadd
 *      that returns the # of elements added).  These will always have a 'void'
 *      return declaration
 *
 *  2.  pure read operations.
 *
 *  3.  read/modify operations such as lpop that both modify and return something
 *      truly useful.
 *
 * Only methods in category 1 can be fully transactional.  Methods in category 2
 * can be supported by a direct read from redis (bypassing its transaction
 * method); there's no harm is doing this direct read while in a
 * transaction. Methods in category 3 can't be done in a transaction (using this
 * simple, thin wrapper).
 *
 * The above is formalized (mostly) via the Skinner wrapper and the Opclass enum.
 */
public class RedisOps
{
    /**
     * RetryConfig is a simple attempt to configure common retry parameters.  There
     * are 2 basic parameters:  the total number of attempts, and the time(s) to
     * wait between attempts.  Note that the timeout that's configured as part of
     * the JedisPool object that's passed to RedisOps() is what governs the main
     * timeout that occurs when trying to communicate with the redis server.  The
     * configuration in RetryConfig governs the behaviour outside JedisPool.
     *
     * The last value (in milliseconds) in the list of sleep times will be
     * repeated if the max # of retries is greater than the list of sleep values.
     */
    public static class RetryConfig
    {
        private int    maxRetries;
        private long[] sleepsMs;

        public int getMaxRetries()
        {
            return maxRetries;
        }

        public long getRetrySleep(int attempt)
        {
            if (attempt >= sleepsMs.length)
                return sleepsMs[sleepsMs.length - 1];
            else
                return sleepsMs[attempt];
        }

        /**
         * Convenience constructor.  Syntax:  maxRetries:ms1,ms2,...  For example:
         *
         *  5:250,500,1000
         *
         * would sleep 250ms before the first retry, 500ms before the second, then 1sec
         * between all subsequent.
         */
        public RetryConfig(String config)
        {
            if ((config == null) || ((config = config.trim()).length() == 0))
                throw new IllegalArgumentException("Bad RedisOp retry config:  missing/empty config");

            int      co = config.indexOf(':');
            String[] sleeps;
            int      max;
            long[]   mss;

            if (co == -1)
                throw new IllegalArgumentException("Bad RedisOps retry config:  '" + config + "' must be in the form of {count}:{sleepMs},...");

            max    = Integer.parseInt(config.substring(0, co));
            sleeps = config.substring(co + 1).split(",");
            mss    = new long[sleeps.length];

            for (int i = 0; i < sleeps.length; i++)
                mss[i] = Long.parseLong(sleeps[i]);

            validate(max, mss);

            this.maxRetries = max;
            this.sleepsMs   = mss;
        }

        public RetryConfig(int max, long[] sleepsMs)
        {
            validate(max, sleepsMs);

            this.maxRetries = max;
            this.sleepsMs   = sleepsMs;
        }

        private static void validate(int max, long[] sleeps)
        {
            if (max < 0)
                throw new IllegalArgumentException("Bad RedisOps retry config:  maxRetries must be > 0 but is " + max);
            else if ((sleeps == null) || (sleeps.length == 0))
                throw new IllegalArgumentException("Bad RedisOps retry config:  missing sleep config");

            for (long sleep : sleeps)
            {
                if (sleep < 0)
                    throw new IllegalArgumentException("Bad RedisOps retry config:  sleepMs must be >= 0 but is " + sleep);
            }
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();  // all because Arrays.asList(long[]) doesn't work as desired

            sb.append("(retry ").append(maxRetries).append(", sleeps=[");

            for (int i = 0; i < sleepsMs.length; i++)
            {
                if (i > 0)
                    sb.append(",");
                sb.append(sleepsMs[i]);
            }

            sb.append("])");

            return sb.toString();
        }
    }

    /**
     * Implementation note:  Redis sometimes delays a bit too much for Jedis and
     * we get SocketTimeouts, which are fatal to the system.  To try to avoid
     * this problem, each attempt to communicate with Redis/Jedis is done in a
     * loop so that we can re-attempt the operation.
     */

    /**
     * Retry info
     */
    private RetryConfig retryConfig;

    /**
     * Various types of Redis data, as returned by jedis.type():
     *
     *  string, list, set, zset and hash
     */
    public final static String TYPE_HASH   = "hash";
    public final static String TYPE_LIST   = "list";
    public final static String TYPE_SET    = "set";
    public final static String TYPE_STRING = "string";
    public final static String TYPE_ZSET   = "zset";

    /**
     * Dependencies
     */
    private static boolean      debug;          // true => log all redis commands
    private        Transactions transactions;
    private final  JedisPool    jedisPool;

    /**
     * Spring setters
     */
    public RedisOps(JedisPool pool, RetryConfig retryConfig)
    {
        this.jedisPool   = pool;
        this.retryConfig = retryConfig;
    }
    
    public RedisOps(JedisPool pool)
    {
        this.jedisPool = pool;
        setDefaultRetry();
    }
    
    public RedisOps(String host, int port) throws Exception {
        this.jedisPool    = new JedisPool(host, port);
        this.transactions = new Transactions(this.jedisPool);

        setDefaultRetry();
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void setRetryConfig(RetryConfig retryConfig)
    {
        this.retryConfig = retryConfig;
    }

    public void setTransactions(Transactions trans)
    {
        this.transactions = trans;
    }

    public Transactions getTransactions()
    {
        return transactions;
    }

    public JedisPool getJedisPool()
    {
        return jedisPool;
    }

    private void setDefaultRetry()
    {
        this.retryConfig = new RetryConfig(5, new long[] {250});
    }

    /**
     * Skinner (from skin wrapping guts) factors out the common code
     * structure/pattern for dealing with multiple attempts of a call to Jedis.
     * The use pattern is for each real method to do something like the
     * following:
     *
     *    public Long something(final String key)
     *    {
     *        return (new Skinner<Long>() {
     *                @Override
     *                Long directOp(Jedis jedis)
     *                {
     *                    return jedis.pttl(key);
     *                }
     *            }).wrap();
     *    }
     */

    private enum Opclass {
        /**
         * Always call directly to redis even if in a transaction; for methods
         * that don't modify redis data and that return values.  The method
         * transactionOp SHOULD NOT be overridden.
         */
        DIRECT,

        /**
         * Throws an exception if in a transaction; for methods that modify
         * redis data and that return values.  The method transactionOp SHOULD
         * NOT be overridden.
         */
        NO_TRANS,

        /**
         * Adds to transaction if in a transaction, otherwise call directly into
         * redis; for methods that modify redis data and that don't return
         * values.  The return value from directOp MUST be null.  The method
         * transactionOp MUST be overridden.
         */
        ALLOW_TRANS
    }

    private abstract class Skinner<T>
    {
        /**
         * The real method must call this method in order to provide the automatic
         * retry on failure, etc.
         */
        T wrap(final String operation, final Opclass handling)
        {
            Transaction tx    = transactions.getTransaction();
            boolean     inTx  = (tx != null);
            Exception   mx    = null;
            Jedis       jedis = null;

            if ((handling == Opclass.DIRECT) || !inTx)
            {
                // try operation the given number of times before abandoning.
                for (int attempt = 0, max = retryConfig.getMaxRetries(); attempt < max; attempt++)
                {
                    try
                    {
                        // if we're in a transaction, get the pre-reserved "readonly" connection
                        // otherwise we could deadlock on an exhausted pool
                        if (!inTx)
                            jedis = jedisPool.getResource();
                        else
                            jedis = transactions.getReadonlyJedis();

                        return directOp(jedis);
                    }
                    catch (Exception x)
                    {
                        mx = x;  // remember one of the exceptions for debugging purposes.
                        x.printStackTrace();
                        sleep(retryConfig.getRetrySleep(attempt));
                    }
                    finally
                    {
                        if (!inTx && (jedis != null))  // return resource only if we got it
                            jedis.close();
                    }
                }

                throw new IllegalStateException(operation + " failed after " + retryConfig.getMaxRetries() + " attempts", mx);
            }
            else if (handling == Opclass.NO_TRANS)
            {
                throw new IllegalStateException(operation + " attempted inside transaction but is not allowed inside transaction.");
            }
            else
            {
                transactionOp(tx);
                return null;
            }
        }

        /**
         * Put the real/final Jedis operation here
         */
        abstract T directOp(Jedis jedis);

        /**
         * Optionally overrideable (makes sense IFF Opclass==ALLOW_TRANS)
         */
        void transactionOp(Transaction trans)
        {
        }
    }

    /**
     * Bypass transactions.  Script may or may not modify.
     */
    public void eval(final String script, final List<String> keys, final List<String> args)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("eval", script, keys, args);

                jedis.eval(script,
                           ((keys != null) ? keys : new java.util.ArrayList<String>()),
                           ((args != null) ? args : new java.util.ArrayList<String>()));

                return null;
            }
        }).wrap("eval", Opclass.DIRECT);
    }

    /**
     * Passthrough to Redis' DUMP command
     */
    public byte[] dump(final String key)
    {
        return (new Skinner<byte[]>() {
                @Override
                byte[] directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("dump", key);

                    return jedis.dump(key);
                }
            }).wrap("dump", Opclass.DIRECT);
    }

    /**
     * Passthrough to Redis' PTTL command
     */
    public Long pttl(final String key)
    {
        return (new Skinner<Long>() {
                @Override
                Long directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("pttl", key);

                    return jedis.pttl(key);
                }
            }).wrap("pttl", Opclass.DIRECT);
    }

    /**
     * Passthrough to Redis' RESTORE command
     */
    public String restore(final String key, final int ttl, final byte[] dumped)
    {
        return (new Skinner<String>() {
                @Override
                String directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("restore", key, ttl, dumped);

                    return jedis.restore(key, ttl, dumped);
                }
            }).wrap("restore", Opclass.DIRECT);
    }

    /**
     * Begin a pipelined set of queries, where the client (this code) doesn't need to
     * wait for a response from the redis server before accepting a new query.  The
     * set of queries supported is given in RedisPipeline.
     */
    public RedisPipeline beginPipeline()
    {
        boolean  inTx  = transactions.inTransaction();
        Jedis    jedis = (!inTx) ? jedisPool.getResource() : transactions.getReadonlyJedis();

        return new RedisPipeline(jedis, !inTx);
    }

    /**
     * General design note: the handling of jedis "resource" is painful here as there are multiple
     * points where exceptions can occur and for each exception due to a broken resource, we need
     * to return the correct one.
     */

    /**
     * Test if the server is alive and responding as expected.
     */
    public boolean ping()
    {
        Jedis  jedis = null;

        try
        {
            jedis = jedisPool.getResource();

            return "PONG".equals(jedis.ping());
        }
        catch (Exception x)
        {
            return false;
        }
        finally
        {
            if (jedis != null)
                jedis.close();
        }
    }

    /**
     * This is a special-case method that does explicitly does *not* deal with
     * transactions.  This is a low-level 'set' operation that allows locking
     * of flag-like keys with a non-zero time-to-live.  This is intended to
     * be used to maintain exclusivity of access to things like queues.
     *
     * The model is that the first call to this method should pass 'true' for
     * the value of 'lock', and if true is returned then the caller owns that
     * key for the given number of milliseconds.  Within that TTL, another call
     * to this method with 'lock' set to 'false' should be made in order to
     * retain ownership of the lock.  If false is returned from that call, then
     * the lock is no longer owned.
     */
    public boolean lockDirect(final String key, final boolean lock, final long ttlMs)
    {
        final   String    val   = "locked: " + Long.toString(System.currentTimeMillis());
        final   Exception mx    = null;
        Boolean rv;

        rv = (new Skinner<Boolean>() {
                @Override
                Boolean directOp(Jedis jedis)
                {
                    SetParams sp = new SetParams();

                    if (debug)
                        logCmd("set", key, val, sp);

                    if (lock)
                        sp.nx();
                    else
                        sp.xx();

                    sp.px(ttlMs);

                    return "OK".equals(jedis.set(key, val, sp));
                }
            }).wrap("lockDirect", Opclass.DIRECT);

        return rv.booleanValue();
    }

    /**
     * This is a special-case method that does explicitly does *not* deal with
     * transactions.  This is a low-level 'set' operation that allows locking
     * of flag-like keys with a non-zero time-to-live.  This is intended to
     * be used to maintain exclusivity of access to things like objects.
     *
     * This locking model will return the randomly generated value used while
     * acquiring the lock, so that the lock owner might correctly refresh, unlock,
     * or check it.  If the lock was unable to be acquired, then null is returned.
     */
    public String lockRandom(final String key, final long ttlMs)
    {
        final String val = UUID.randomUUID().toString();
        Boolean rv;

        rv = (new Skinner<Boolean>() {
                @Override
                Boolean directOp(Jedis jedis)
                {
                    SetParams sp = new SetParams();

                    if (debug)
                        logCmd("set", key, val, sp);

                    sp.nx().px(ttlMs);

                    return "OK".equals(jedis.set(key, val, sp));
                }
            }).wrap("lockRandom", Opclass.DIRECT);

        return (rv.booleanValue()) ? val : null;
    }

    /**
     * This is a special-case method that does explicitly does *not* deal with
     * transactions. It is required where a creation of a key (e.g., for
     * purposes of storing an object via hmset) requires a unique ID.  If we
     * were to put the creation of the ID in the transaction, we'd never know
     * what it is when we need it because it isn't available until after the
     * exec() call is made.
     *
     * This method will always go directly to redis to get an ID for that case.
     * If the transaction fails, there will be a hole in the ID space.
     */
    public Long incrDirect(String key)
    {
        return incrDirect(key, 1);
    }

    public Long incrDirect(final String key, final int count)
    {
        return (new Skinner<Long>() {
                @Override
                Long directOp(Jedis jedis)
                {
                    Long rv;

                    if (debug)
                        logCmd("incrby", key, count);

                    if (count == 1)
                        rv = jedis.incr(key);
                    else
                        rv = jedis.incrBy(key, count);  //!! might need to conditionally use decrBy also...

                    return rv;
                }
            }).wrap("incrDirect", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' incr.  Can be in a transaction.
     */
    public void incrBy(final String key, final long amount)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("incrby", key, amount);

                jedis.incrBy(key, amount);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx incrby]", key, amount);

                tx.incrBy(key, amount);
            }
        }).wrap("incrBy", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' set.  Can be in a transaction.
     */
    public void set(final String key, final String value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("set", key, value);

                jedis.set(key, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx set]", key, value);

                tx.set(key, value);
            }
        }).wrap("set", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' del.  Can be in a transaction.
     */
    public void del(final String key)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("del", key);

                jedis.del(key);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx del]", key);

                tx.del(key);
            }
        }).wrap("del", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' expire.  Can be in a transaction.
     */
    public void expire(final String key, final int deltaSec)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("expire", key, deltaSec);

                jedis.expire(key, deltaSec);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx expire]", key, deltaSec);

                tx.expire(key, deltaSec);
            }
        }).wrap("expire", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' persist.  Can be in a transaction.
     */
    public void persist(final String key)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("persist", key);

                jedis.persist(key);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx persist]", key);

                tx.persist(key);
            }
        }).wrap("persist", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' rpush.  Can be in a transaction.
     */
    public void rpush(final String key, final String value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("rpush", key, value);

                jedis.rpush(key, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx rpush]", key, value);

                tx.rpush(key, value);
            }
        }).wrap("rpush", Opclass.ALLOW_TRANS);
    }
    @Deprecated
    public void rpush(final byte[] key, final byte[] value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("rpush", key, value);

                jedis.rpush(key, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx rpush]", key, value);

                tx.rpush(key, value);
            }
        }).wrap("rpush", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' sadd.  Can be in a transaction.
     */
    public void sadd(final String key, final String value)   // for backwards compatibility (< 4.x)
    {
        sadd(key, new String[] {value});
    }
    public void sadd(final String key, final String... values)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("sadd", key, logStrArray(values));

                jedis.sadd(key, values);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx sadd]", key, logStrArray(values));

                tx.sadd(key, values);
            }
        }).wrap("sadd", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' srem.  Can be in a transaction.
     */
    public void srem(final String key, final String value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("srem", key, value);

                jedis.srem(key, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx srem]", key, value);

                tx.srem(key, value);
            }
        }).wrap("srem", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' zadd.  Can be in a transaction.
     */
    public void zadd(final String key, final double score, final String value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("zadd", score, value);

                jedis.zadd(key, score, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx zadd]", score, value);

                tx.zadd(key, score, value);
            }
        }).wrap("zadd", Opclass.ALLOW_TRANS);
    }

    @Deprecated    // byte[] form is deprecated because we don't do a good job of ensuring UTF-8 (which
                   // is what the protocol uses)
    public void zadd(final byte[] key, final double score, final byte[] value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("zadd", score, value);

                jedis.zadd(key, score, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx zadd]", score, value);

                tx.zadd(key, score, value);
            }
        }).wrap("zadd", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' lrem to remove the head element.  Can be in a transaction.
     */
    public void lremHead(final String key)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("lpop", key);

                jedis.lpop(key);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx lpop]", key);

                tx.lpop(key);
            }
        }).wrap("lremHead", Opclass.ALLOW_TRANS);
    }
    @Deprecated
    public void lremHead(final byte[] key)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("lpop", key);

                jedis.lpop(key);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx lpop]", key);

                tx.lpop(key);
            }
        }).wrap("lremHead", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' lrem.  Can be in a transaction.
     */
    public void lrem(final String key, final long count, final String value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("lrem", count, value);

                jedis.lrem(key, count, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx lrem]", count, value);

                tx.lrem(key, count, value);
            }
        }).wrap("lrem", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' zrem, zremRangeByScore.  Can be in a transaction.
     */
    public void zrem(final String key, final String member)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("zrem", member);

                jedis.zrem(key, member);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx zrem]", member);

                tx.zrem(key, member);
            }
        }).wrap("zrem", Opclass.ALLOW_TRANS);
    }
    @Deprecated
    public void zrem(final byte[] key, final byte[] member)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("zrem", member);

                jedis.zrem(key, member);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx zrem]", member);

                tx.zrem(key, member);
            }
        }).wrap("zrem", Opclass.ALLOW_TRANS);
    }

    public void zremRangeByScore(final String key, final double start, final double end)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("zremrangebyscore", key, start, end);

                jedis.zremrangeByScore(key, start, end);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx zremrangebyscore]", key, start, end);

                tx.zremrangeByScore(key, start, end);
            }
        }).wrap("zremRangeByScore", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' hset.  Can be in a transaction.
     */
    public void hset(final String key, final String field, final String value)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("hset", key, field, value);

                jedis.hset(key, field, value);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx hset]", key, field, value);

                tx.hset(key, field, value);
            }
        }).wrap("hset", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' incr.  Can be in a transaction.
     */
    public void hincrBy(final String key, final String field, final long amount)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("hincrby", key, field, amount);

                jedis.hincrBy(key, field, amount);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx hincrby]", key, field, amount);

                tx.hincrBy(key, field, amount);
            }
        }).wrap("hincrBy", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' hdel.  Can be in a transaction.
     */
    public void hdel(final String key, final String... fields)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("hdel", key, logStrArray(fields));

                jedis.hdel(key, fields);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx hdel]", key, logStrArray(fields));

                tx.hdel(key, fields);
            }
        }).wrap("hdel", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' hmset.  Can be in a transaction.
     */
    public void hmset(final String key, final Map<String, String> values)
    {
        (new Skinner<Void>() {
            @Override
            Void directOp(Jedis jedis)
            {
                if (debug)
                    logCmd("hmset", key, values);

                jedis.hmset(key, values);
                return null;
            }

            @Override
            void transactionOp(Transaction tx)
            {
                if (debug)
                    logCmd("[tx hmset]", key, values);

                tx.hmset(key, values);
            }
        }).wrap("hmset", Opclass.ALLOW_TRANS);
    }

    /**
     * Pass-thru to jedis' lpop.  CANNOT be in a transaction.
     */
    public String lpop(final String key)
    {
        return (new Skinner<String>() {
                @Override
                String directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("lpop", key);

                    return jedis.lpop(key);
                }
            }).wrap("lpop", Opclass.NO_TRANS);
    }
    @Deprecated
    public byte[] lpop(final byte[] key)
    {
        return (new Skinner<byte[]>() {
                @Override
                byte[] directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("lpop", key);

                    return jedis.lpop(key);
                }
            }).wrap("lpop", Opclass.NO_TRANS);
    }

    /**
     * Pass-thru to jedis' llen.  CANNOT be in a transaction. TODO: ???
     */
    public Long llen(final String key)
    {
        return (new Skinner<Long>() {
                @Override
                Long directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("llen", key);

                    return jedis.llen(key);
                }
            }).wrap("llen", Opclass.DIRECT);
    }
    @Deprecated
    public Long llen(final byte[] key)
    {
        return (new Skinner<Long>() {
                @Override
                Long directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("llen", key);

                    return jedis.llen(key);
                }
            }).wrap("llen", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' type.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public String type(final String key)
    {
        return (new Skinner<String>() {
                @Override
                String directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("type", key);

                    return jedis.type(key);
                }
            }).wrap("type", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' get.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public String get(final String key)
    {
        return (new Skinner<String>() {
                @Override
                String directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("get", key);

                    return jedis.get(key);
                }
            }).wrap("get", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' lrange.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public List<String> lrange(final String key, final int from, final int to)
    {
        return (new Skinner<List<String>>() {
                @Override
                List<String> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("lrange", key, from, to);

                    return jedis.lrange(key, from, to);
                }
            }).wrap("lrange", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' lrange.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    @Deprecated
    public List<byte[]> lrange(final byte[] key, final int from, final int to)
    {
        return (new Skinner<List<byte[]>>() {
                @Override
                List<byte[]> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("lrange", key, from, to);

                    return jedis.lrange(key, from, to);
                }
            }).wrap("lrange", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' smembers.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public Set<String> smembers(final String key)
    {
        return (new Skinner<Set<String>>() {
                @Override
                Set<String> directOp(Jedis jedis)
                {
                    return jedis.smembers(key);
                }
            }).wrap("smembers", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' sinter.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public Set<String> sinter(final String... keys)
    {
        return (new Skinner<Set<String>>() {
                @Override
                Set<String> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("sinter", logStrArray(keys));

                    return jedis.sinter(keys);
                }
            }).wrap("sinter", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' zrangeByScoreWithScores.  Can be in a transaction but
     * doesn't reflect changes queued in transaction.  Note that jedis isn't nice
     * as it returns a Set<> rather than a List<> (redis returns the elements in
     * sorted order).
     */
    public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max, final int offset, final int count)
    {
        return (new Skinner<Set<Tuple>>() {
                @Override
                Set<Tuple> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("zrangebyscorewithscores", key, min, max, offset, count);

                    return (Set<Tuple>) jedis.zrangeByScoreWithScores(key, min, max, offset, count);
                }
            }).wrap("zrangeByScoreWithScores", Opclass.DIRECT);
    }
    @Deprecated
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max, final int offset, final int count)
    {
        return (new Skinner<Set<Tuple>>() {
                @Override
                Set<Tuple> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("zrangebyscorewithscores", key, min, max, offset, count);

                    return (Set<Tuple>) jedis.zrangeByScoreWithScores(key, min, max, offset, count);
                }
            }).wrap("zrangeByScoreWithScores", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' zcard.  Can be in a transaction but
     * doesn't reflect changes queued in transaction.
     */
    public int zrangeCard(final String key)
    {
        Long count;

        count = (new Skinner<Long>() {
                @Override
                Long directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("zrangecard", key);

                    return jedis.zcard(key);
                }
            }).wrap("zrangeCard", Opclass.DIRECT);

        return count.intValue();
    }
    @Deprecated
    public int zrangeCard(final byte[] key)
    {
        Long count;

        count = (new Skinner<Long>() {
                @Override
                Long directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("zrangecard", key);

                    return jedis.zcard(key);
                }
            }).wrap("zrangeCard", Opclass.DIRECT);

        return count.intValue();
    }

    /**
     * Pass-thru to jedis' hget.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public String hget(final String key, final String field)
    {
        return (new Skinner<String>() {
                @Override
                String directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("hget", key, field);

                    return jedis.hget(key, field);
                }
            }).wrap("hget", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' hgetAll.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public Map<String, String> hgetAll(final String key)
    {
        return (new Skinner<Map<String, String>>() {
                @Override
                Map<String, String> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("hgetall", key);

                    return jedis.hgetAll(key);
                }
            }).wrap("hgetAll", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' hvals.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public List<String> hvals(final String key)
    {
        return (new Skinner<List<String>>() {
                @Override
                List<String> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("hvals", key);

                    return jedis.hvals(key);
                }
            }).wrap("hvals", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' hmget.  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public List<String> hmget(final String key, final String... fieldNames)
    {
        return (new Skinner<List<String>>() {
                @Override
                List<String> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("hmget", key, fieldNames);

                    return jedis.hmget(key, fieldNames);
                }
            }).wrap("hmget", Opclass.DIRECT);
    }

    /**
     * Pass-thru to jedis' keys().  Can be in a transaction but doesn't reflect
     * changes queued in transaction.
     */
    public Set<String> keys(final String pattern)
    {
        return (new Skinner<Set<String>>() {
                @Override
                Set<String> directOp(Jedis jedis)
                {
                    if (debug)
                        logCmd("keys", pattern);

                    return jedis.keys(pattern);
                }
            }).wrap("keys", Opclass.DIRECT);
    }

    static void logCmd(String cmd, Object... args)
    {
        if (debug)
            System.out.println(" [tid=" + Thread.currentThread().getId() + " - " + System.currentTimeMillis() + " - redis] " + cmd + "(" + java.util.Arrays.asList(args) + ")");
    }

    static List<String> logStrArray(String[] strs)
    {
        if (strs == null)
            return new java.util.ArrayList<String>();
        else
            return java.util.Arrays.asList(strs);
    }

    /**
     * Uninterrupted sleep.
     */
    private final static void sleep(Long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ix)
        {
        }
    }
        
}
