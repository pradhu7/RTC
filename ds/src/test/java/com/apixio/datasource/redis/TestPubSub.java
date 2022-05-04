package com.apixio.datasource.redis;

import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Integration")
public class TestPubSub
{
    private RedisOps redisOps;
    private PubSub   pubSub;

    @Before
    public void setUp() throws Exception
    {
        redisOps = new RedisOps("localhost", 6379);

        pubSub = new PubSub();
        pubSub.setRedisOps(redisOps);
        pubSub.setPrefix("test_");
    }

    @Test
    public void testPublishing()
    {

        Consumer consumer = new Consumer()
        {
            @Override
            public void consumeMessage(String channel, String message)
            {
                System.out.println("consumeMessage " + channel + " " + message);
            }
        };

        String channel = "channeltest" + Math.random();

        pubSub.subscribeToChannel(channel, consumer);
        sleep(100L);

        int i;
        for (i = 0; i < 10; i++)
            pubSub.publishToChannel(channel, "hi" + i);

        pubSub.unsubscribeFromChannel(channel);
        sleep(100L);

        for (; i < 20; i++)
            pubSub.publishToChannel(channel, "hi" + i);

        List<String> histories = pubSub.getChannelHistory(channel, 0, 0);
        i = 0;
        for (String history : histories)
        {
            Assert.assertEquals(history, "hi" + i++);
            System.out.println(history);
        }
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
}
