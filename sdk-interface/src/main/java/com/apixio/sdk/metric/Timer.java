package com.apixio.sdk.metric;

import java.util.Map;

/**
 */
public class Timer extends Metric
{
    private Long startMs;
    private Long endMs;

    public static Timer newTimer(String name)
    {
        return new Timer(name);
    }

    public static Timer startTimer(String name)
    {
        Timer timer = new Timer(name);

        timer.start();

        return timer;
    }

    /**
     *
     */
    private Timer(String name)
    {
        super(Metric.Type.TIMER, name);
    }

    /**
     * Reusable timer model
     */
    public void start()
    {
        startMs = Long.valueOf(System.currentTimeMillis());
        endMs   = null;
    }

    public long stop()
    {
        if (startMs == null)
            throw new IllegalStateException("Timer " + name + " has not been started");

        endMs = Long.valueOf(System.currentTimeMillis());

        return duration();
    }

    public long duration()
    {
        if ((startMs == null) || (endMs == null))
            throw new IllegalStateException("Timer " + name + " has not been started or stopped");

        return endMs - startMs;
    }

    /**
     *
     */
    public void report(Map<String,Object> metrics)
    {
        metrics.put(name + ".duration.ms", duration());
    }

}
