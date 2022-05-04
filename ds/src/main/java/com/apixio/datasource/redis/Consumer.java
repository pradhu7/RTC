package com.apixio.datasource.redis;

public abstract class Consumer
{
    public abstract void consumeMessage(String channel, String message);
}
