package com.apixio.datasource.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

public class PubSub
{
    private static Logger logger = LoggerFactory.getLogger(PubSub.class);

    /**
     * How many times to retry operations before giving up.
     */
    public final static int  MAX_RETRIES = 5;
    public final static long RETRY_SLEEP = 250L;

    private RedisOps  redisOps;
    private String    prefix;

    private static ConcurrentMap<String, ChannelHandle> channelToChannelHandle = new ConcurrentHashMap<String, ChannelHandle>();

    public void setRedisOps(RedisOps ops)
    {
        this.redisOps = ops;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Publish a message to a Redis Pub/sub channel. Also, save the message in a persistent time ordered queue.
     *
     * @param channel
     * @param message
     */
    public void publishToChannel(String channel, String message)
    {
        JedisPool jedisPool = redisOps.getJedisPool();
        Jedis     jedis     = jedisPool.getResource();
        Exception mx        = null;

        String canonicalChannelName  = makeChannelNameCanonical(channel);
        String persistentChannelName = makeChannelPersistentNameCanonical(channel);

        // try operation the given number of times before abandoning.
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++)
        {
            try
            {
                redisOps.zadd(persistentChannelName.getBytes(), System.currentTimeMillis(), message.getBytes());

                jedis.publish(canonicalChannelName, message);

                jedis.close();

                return;
            }
            catch (Exception x)
            {
                jedis.close();
                jedis = jedisPool.getResource();

                mx = x;  // remember one of the exceptions for debugging purposes.
                x.printStackTrace();
                sleep(RETRY_SLEEP);
            }
        }

        jedis.close();

        throw new IllegalStateException("publish failed after " + MAX_RETRIES + " attempts", mx);
    }

    /**
     * Given a channel and time range, find all messages published during that time range.
     * If greater is zero, then start from the beginning
     * If less is zero, then find till the end
     *
     * @param channel
     * @param greater
     * @param less
     */
    public List<String> getChannelHistory(String channel, long greater, long less)
    {
        String persistentChannelName = makeChannelPersistentNameCanonical(channel);

        double min = (greater == 0L) ? Double.NEGATIVE_INFINITY : Double.valueOf(greater);
        double max = (less == 0L) ? Double.POSITIVE_INFINITY : Double.valueOf(less);

        Set<Tuple> tuples = redisOps.zrangeByScoreWithScores(persistentChannelName.getBytes(), min, max, 0, -1);

        List<String> messages = new ArrayList<String>();

        if (tuples == null || tuples.isEmpty())
            return messages;

        for (Tuple tuple : tuples)
        {
            messages.add(tuple.getElement());
        }

        return messages;
    }

    /**
     * Given a channel and a consumer, register to the channel. If you have already registered,
     * no operation is performed.
     *
     * @param channel
     * @param consumer - a method that runs each time a message is received
     */
    public void subscribeToChannel(String channel, Consumer consumer)
    {
        final JedisPool jedisPool = redisOps.getJedisPool();

        final String canonicalChannelName = makeChannelNameCanonical(channel);

        if (channelToChannelHandle.get(canonicalChannelName) != null)
        {
            logger.info("already subscribed to channel: " + canonicalChannelName);
        }

        final ChannelHandle channelHandle = new ChannelHandle(canonicalChannelName, consumer);

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                Jedis jedis = null;
                try
                {
                    logger.info("Subscribing to channel: " + canonicalChannelName);

                    jedis = jedisPool.getResource();

                    channelToChannelHandle.put(canonicalChannelName, channelHandle);

                    jedis.subscribe(channelHandle, canonicalChannelName);

                    jedis.close();
                }
                catch (Exception e)
                {
                    logger.error("Subscribing failed.", e);

                    if (jedis != null)
                        jedis.close();
                }
            }
        };

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Given a channel, un-subscribe to the channel.
     *
     * @param channel
     */
    public void unsubscribeFromChannel(String channel)
    {
        String canonicalChannelName = makeChannelNameCanonical(channel);

        ChannelHandle channelHandle = channelToChannelHandle.get(canonicalChannelName);

        if (channelHandle == null)
        {
            logger.info("Already unsubscribed to channel: " + canonicalChannelName);
        }

        channelHandle.unsubscribe(channelHandle.getChannel());
    }

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

    private String makeChannelNameCanonical(String channelName)
    {
        channelName = channelName.toLowerCase().trim();

        return (prefix + "channel_" + channelName);
    }

    private String makeChannelPersistentNameCanonical(String channelName)
    {
        channelName = channelName.toLowerCase().trim();

        return (prefix + "channel_persistence_" + channelName);
    }
}
