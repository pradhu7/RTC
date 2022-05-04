package com.apixio.restbase.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Tuple;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.MessageBase;

/**
 * Provides a DAO layer for a time-ordered list of messages.  Operations supported:
 *
 *  * add message to list
 *  * get messages before/after point in time
 *  * delete messages before/after point in time
 *
 * As Redis' sorted set "score" type is a double and this class is presenting a time-
 * ordered list, a mapping between the two is needed.  The time/score of a message
 * is the epoch seconds, but kept to three decimal places (so, in the end millisecond
 * resolution is supported).  All queries are "exclusive" and the time is handled by
 * adding/subtracting .5 milliseconds.
 *
 * An optional behavior on a MessageList is to auto-expire the key using Redis' TTL
 * command.
 */
public class MessageList<T extends MessageBase> extends DaoBase
{
    private static final String LIST_PREFIX = "msglist-";

    private String   listKey;
    private int      ttl;       // seconds; if > 0 then we set TTL *only* if key doesn't exist or doesn't have a TTL

    public MessageList(DaoBase seed, String listID, int ttlSec)
    {
        super(seed);

        if ((listID == null) || (listID.trim().length() == 0))
            throw new IllegalArgumentException("ListID must not be null/empty");

        this.listKey = makeKey(LIST_PREFIX + listID);
        this.ttl     = ttlSec;
    }

    /**
     * Add the message to the message list, giving it the current time as the sorted
     * location in the list.
     */
    public void addItem(T message)
    {
        addToIndexes(message);
    }
    
    /**
     * Various kinds of queries
     */
    public List<T> getAllBefore(long epochMs)
    {
        Set<Tuple> all = redisOps.zrangeByScoreWithScores(listKey, Double.NEGATIVE_INFINITY, before(epochMs), 0, -1);

        return sortTuples(all);
    }
    
    public List<T> getAllAfter(long epochMs)
    {
        Set<Tuple> all = redisOps.zrangeByScoreWithScores(listKey, after(epochMs), Double.POSITIVE_INFINITY, 0, -1);

        return sortTuples(all);
    }
    
    public List<T> getAllBetween(long startMs, long endMs)
    {
        Set<Tuple> all = redisOps.zrangeByScoreWithScores(listKey, after(startMs), before(endMs), 0, -1);

        return sortTuples(all);
    }
    
    /**
     * Various kinds of deletes
     */
    public void deleteAllBefore(long epochMs)
    {
        redisOps.zremRangeByScore(listKey, Double.NEGATIVE_INFINITY, before(epochMs));
    }
    
    public void deleteAllAfter(long epochMs)
    {
        redisOps.zremRangeByScore(listKey, after(epochMs), Double.POSITIVE_INFINITY);
    }
    
    public void deleteAllBetween(long startMs, long endMs)
    {
        redisOps.zremRangeByScore(listKey, after(startMs), before(endMs));
    }

    /**
     * Adds the entity to the indexed lookups so we can find all entities.
     */
    private void addToIndexes(T message)
    {
        Long  pttl = (ttl > 0) ? redisOps.pttl(listKey) : Long.valueOf(0L);   // milliseconds; < 0 means key doesn't exist or doesn't have ttl

        redisOps.zadd(listKey, at(System.currentTimeMillis()), message.serialize());

        if (pttl < 0)
            redisOps.expire(listKey, ttl);
    }

    /**
     * Returns double value that represents the exact time as a double
     */
    private double at(long ms)
    {
        return ((double) ms) / 1000.;
    }

    /**
     * Returns double value that can be used as the 'max' of a query
     */
    private double before(long ms)
    {
        return at(ms) + .0005;
    }

    /**
     * Returns double value that can be used as the 'min' of a query
     */
    private double after(long ms)
    {
        return at(ms) - .0005;
    }

    /**
     * Since jedis returns a Set<> and not a List, we have to sort.  Boo
     */
    private List<T> sortTuples(Set<Tuple> all)
    {
        List<Tuple> sort = new ArrayList<>(all);
        List<T>     list = new ArrayList<>();

        Collections.sort(sort, new Comparator<Tuple>()
                         {
                             public int compare(Tuple o1, Tuple o2)
                             {
                                 double diff = o1.getScore() - o2.getScore();

                                 if (diff < 0.)
                                     return -1;
                                 else if (diff > 0.)
                                     return +1;
                                 else
                                     return 0;
                             }
                         });

        for (Tuple t : sort)
        {
            T m = (T) MessageBase.deserialize(new String(t.getBinaryElement()));
            
            list.add(m);
        }

        return list;
    }
}
