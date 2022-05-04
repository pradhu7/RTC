package com.apixio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Timer.class);

    private String name;
    private long   start;

    public Timer(String name)
    {
        this.name = "[TIMER] " + name;

        start();
    }

    public void start()
    {
        start = System.currentTimeMillis();
    }

    public void stop()
    {
        LOGGER.info(name + " took " + (System.currentTimeMillis() - start) + " ms");
    }

}
