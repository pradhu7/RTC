package com.apixio.datasource.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

/**
 * TimeOrderedQueue implements time-ordered queue, where the availability of items is
 * based on when they are declared to be "see-able": an item posted in the future
 * will not be retrievable until the current system time is past the "post at" time
 * of the item. Additionally, the items are ordered such that the earliest
 * retrievable item is the one returned via get() or peek().
 * 
 * It is not valid to post an item with a "post at" date prior to the current system
 * time.
 * 
 * TODO: This searches the entire set. Retain last returned "liveAt" to allow Redis to cut out of the
 * complete search, since there will be none earlier than the previous liveAt time.
 */
public class TimeOrderedQueue extends Queue {

    Logger logger = LoggerFactory.getLogger(TimeOrderedQueue.class);

    /**
     * Constructs a Java object representation of a real FIFO queue.
     */
    public TimeOrderedQueue(String name) throws Exception
    {
        super(name);
    }

    @Override
    public void post(String data)
    {
        // This really does not mix with posted dates, but helps in testing
        post(data, System.currentTimeMillis());
    }

    /**
     * Post data that is visible at given time (ms since 1970)
     * 
     * @param data
     * @param liveAt
     */
    public void post(String data, long liveAt)
    {
        QueueItem qi = new QueueItem(System.currentTimeMillis(), liveAt, data);

        redisOps.zadd(bName, (double) liveAt, qi.swizzle().getBytes());
    }

    @Override
    public Queue.QueueItem peek()
    {
        try
        {
            // get the earliest score
            Set<Tuple> items = (Set<Tuple>) redisOps.zrangeByScoreWithScores(bName, 0,
                                                                             System.currentTimeMillis(), 0, 1);
            if (items != null && items.size() > 0)
            {
                if (items.size() > 1)
                    throw new IllegalStateException("peek: # of items: " + items.size());

                byte[] blob = items.toArray(new Tuple[1])[0].getBinaryElement();

                return QueueItem.unswizzle(new String(blob));
            }
            else
            {
                return null;
            }
        }
        catch (IOException iox)
        {
            return null;
        }
    }

    @Override
    public List<Queue.QueueItem> peekAll()
    {
        List  all = new ArrayList<Queue.QueueItem>();

        try
        {
            int        count = redisOps.zrangeCard(bName);
            Set<Tuple> items = (Set<Tuple>) redisOps.zrangeByScoreWithScores(bName, 0, Double.POSITIVE_INFINITY,
                                                                             0, count);

            if (items != null)
            {
                for (Tuple tup : items)
                    all.add(QueueItem.unswizzle(new String(tup.getBinaryElement())));
            }
            else
            {
                all = null;
            }
        }
        catch (IOException iox)
        {
            all = null;
        }

        return all;
    }

    @Override
    public Queue.QueueItem get()
    {
        QueueItem qi = peek();

        if (qi != null)
        {
            redisOps.zrem(bName, qi.swizzle().getBytes());
            return qi;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void remove()
    {
        get();  // easy way since it's a set
    }

    @Override
    public long getLength()
    {
        // TODO Auto-generated method stub
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public String getQueueName()
    {
        return name;
    }

}
