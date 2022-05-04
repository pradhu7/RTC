package com.apixio.datasource.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FifoQueue implements a strict FIFO queue. A posted item is immediately available
 * for consumption via the peek() or get() methods.
 * 
 * This class uses RedisOps to actually perform persisted object operations as the
 * operations might need to take place within the context of a transaction and
 * RedisOps deals with transactions.
 */
public class FifoQueue extends Queue {

    Logger logger = LoggerFactory.getLogger(FifoQueue.class);

    /**
     * Constructs a Java object representation of a real FIFO queue.
     */
    public FifoQueue(String name)
    {
        super(name);
    }

    /**
     * Posts the given data as a new QueueItem in the queue. Once posted the item
     * can be immediately seen via call to get() or peek().
     */
    @Override
    public void post(String data)
    {
        QueueItem qi = new QueueItem(System.currentTimeMillis(), 0L, data);

        redisOps.rpush(bName, qi.swizzle().getBytes());
    }

    @Override
    public Queue.QueueItem peek()
    {
        try
        {
            // get the end with list range
            List<byte[]> items = (List<byte[]>) redisOps.lrange(bName, 0, 0);

            if (items != null && items.size() > 0)
            {
                if (items.size() > 1)
                    throw new IllegalStateException("peek: # of items: " + items.size());

                return QueueItem.unswizzle(new String(items.get(0)));
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
            // get the end with list range
            List<byte[]> items = (List<byte[]>) redisOps.lrange(bName, 0, -1);

            if (items != null)
            {
                for (byte[] item : items)
                    all.add(QueueItem.unswizzle(new String(item)));
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
        try
        {
            // get the end with list range
            byte[] blob = redisOps.lpop(bName);

            if (blob != null)
                return QueueItem.unswizzle(new String(blob));
            else
                return null;
        }
        catch (IOException iox)
        {
            return null;
        }
    }

    @Override
    public void remove()
    {
        redisOps.lremHead(bName);
    }

    @Override
    public long getLength()
    {
        return redisOps.llen(bName);
    }

    @Override
    public String getQueueName()
    {
        return name;
    }

}
