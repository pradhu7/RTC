package com.apixio.datasource.redis;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Queue defines a generic queue mechanism that has the following minimal feature
 * set:
 * 
 * 1. queues have unique names
 * 2. queue items are backed by a persistent mechanism to protect against data loss
 * 3. a queue can be either FIFO or time-ordered (but not both)
 * 4. queue items are arbitrary strings
 * 5. queue items have a "post at" time
 * 6. ability to "peek" at the head element
 * 7. ability to get/delete the head element
 * 
 * In the first version of Queues, a single queue consumer is all that is supported.
 * 
 * Instance of Queue objects are merely Java representations of a back-end queue
 * system that actually provides queue functionality. This is an important point of
 * the model as client code can do, for example,
 * 
 * q = new FifoQueue("highpriority");
 * 
 * to get a handle to the (externally maintained) "highpriority" FIFO queue. In
 * other words, Queue objects are really just an access mechanism to a real system
 * that supports queuing of items.
 * 
 * Times are in number of milliseconds since 1970.
 *
 * Queues can be exclusively locked.  To do this, the client must call takeOwnership
 * and then must periodically call keepLocked.  The locking mechanism is implemented
 * via a Redis key that has a short TTL.
 */
public abstract class Queue {

    /**
     * The type of the queue. Each type of queue has its own subclass of Queue to
     * implement the type-specific operations
     */
    public enum QueueType
    {
        FIFO, TIME_ORDERED
    };

    /**
     * QueueItem represents an item pulled from a queue and contains both the
     * "postAt" date (when the item was posted), "liveAt" (the earliest it can be retrieved)
     * and actual arbitrary data.
     * 
     * String contents assumed USASCII-7
     */
    public static class QueueItem {

        final private long postAt;
        final private long liveAt;
        final public String data;

        QueueItem(long postAt, long liveAt, String data)
        {
            this.postAt = postAt;
            this.liveAt = liveAt;
            this.data = data;
        }

        /**
         * Convert to JSON format
         */
        public String swizzle()
        {
            ObjectNode   node   = objectMapper.createObjectNode();

            node.put("postAt",  postAt);
            node.put("liveAt",  liveAt);
            node.put("data",    data);

            return node.toString();
        }

        /**
         * Convert from JSON format
         */
        public static QueueItem unswizzle(String json) throws IOException
        {
            JsonNode  root = objectMapper.readTree(json);

            return new QueueItem(root.get("postAt").asLong(),
                                 root.get("liveAt").asLong(),
                                 root.get("data").asText());
        }
    }

    /**
     * Reuse this (thread-safe once created).
     */
    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Access to all required Redis operations
     */
    protected RedisOps redisOps;

    /**
     * All queues have names.
     */
    final protected String name;
    final protected byte[] bName;

    /**
     * Exclusive lock (at most 1 reader) support
     */
    private String lockName;
    private long   exclusiveTtl;

    /**
     * Spring setters
     */
    public void setRedisOps(RedisOps ops)
    {
        this.redisOps = ops;
    }

    /**
     * Constructs a Java object representation of a real FIFO queue.
     */
    protected Queue(String name)
    {
        this.name  = name;
        this.bName = name.getBytes();
    }

    /**
     * Peek returns the next QueueItem without removing it from the queue. If the
     * queue is empty, null is returned.
     */
    public abstract QueueItem peek();

    /**
     * PeekAll returns the entire list of QueueItems without removing them from
     * the queue.
     */
    public abstract List<Queue.QueueItem> peekAll();

    /**
     * Pet returns the next QueueItem and removes it from the queue. If the
     * queue is empty, null is returned.
     */
    public abstract QueueItem get();

    /**
     * Remove deletes the head of the queue.
     */
    public abstract void remove();

    /**
     * Post data to queue
     */
    public abstract void post(String data);
    
    /**
     * Return length of queue
     */
    public abstract long getLength();

    /**
     * Return name of queue
     * @return
     */
    public abstract String getQueueName();

    /**
     * Client wants to own queue.
     */
    public boolean takeOwnership(long exclusiveTtl)
    {
        this.exclusiveTtl = exclusiveTtl;
        this.lockName     = name + ".lock";

        return redisOps.lockDirect(lockName, true, exclusiveTtl);
    }

    /**
     * keepLocked must be called prior to the TTL expiration of the exclusive
     * lock.  It is the responsibility of the client owner of the queue to do
     * this on schedule.
     */
    public void keepLocked()
    {
        if ((exclusiveTtl > 0) && !redisOps.lockDirect(lockName, false, exclusiveTtl))
            throw new IllegalStateException("Unable to maintain an exclusive lock on queue [" + name + "]");
    }
}
