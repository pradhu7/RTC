package com.apixio.datasource.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPubSub;

public class ChannelHandle
        extends JedisPubSub
{
    private static Logger logger = LoggerFactory.getLogger(ChannelHandle.class);

    Consumer  consumer;
    String    channel;

    public ChannelHandle(String channel, Consumer consumer)
    {
        this.consumer = consumer;
        this.channel  = channel;
    }

    String getChannel()
    {
        return channel;
    }

    @Override
    public void onMessage(String channel, String message)
    {
        logger.info("Message " + message + " received on Channel " + channel);

        consumer.consumeMessage(channel, message);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels)
    {
        logger.info("onSubscribe() to channel: " + channel + "; number of subscribedChannels: " + subscribedChannels);
        System.out.println("onSubscribe() to channel: " + channel + "; number of subscribedChannels: " + subscribedChannels);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels)
    {
        logger.info("onUnsubscribe() from channel: " + channel + "; number of subscribedChannels left: " + subscribedChannels);
        System.out.println("onUnsubscribe() from channel: " + channel + "; number of subscribedChannels left: " + subscribedChannels);
    }

    @Override
    public void onPMessage(String pattern, String channel, String message)
    {
        throw new IllegalStateException();
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels)
    {
        throw new IllegalStateException();
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels)
    {
        throw new IllegalStateException();
    }
}
