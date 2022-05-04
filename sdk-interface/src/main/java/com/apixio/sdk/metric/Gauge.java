package com.apixio.sdk.metric;

import java.util.Map;

/**
 */
public class Gauge extends Metric
{

    private long value;

    public static Gauge newGauge(String name)
    {
        return new Gauge(name, 0);
    }

    public static Gauge newGauge(String name, long value)
    {
        return new Gauge(name, value);
    }

    /**
     *
     */
    private Gauge(String name, long value)
    {
        super(Metric.Type.GAUGE, name);

        this.value = value;
    }

    /**
     *
     */
    public void set(long value)
    {
        this.value = value;
    }

    /**
     *
     */
    public void report(Map<String,Object> metrics)
    {
        metrics.put(name + ".value", value);
    }

}
