package com.apixio.sdk.metric;

import java.util.Map;

/**
 * Counter is a metric that represents a total number of things (elements, calls, etc.)
 */
public class Counter extends Metric
{

    private long count;

    /**
     * Factory methods to create and initialize the count.
     */
    public static Counter newCounter(String name)
    {
        return new Counter(name, 0);
    }

    public static Counter newCounter(String name, long count)
    {
        return new Counter(name, count);
    }

    /**
     * Hide constructor
     */
    private Counter(String name, long count)
    {
        super(Metric.Type.COUNTER, name);

        this.count = count;
    }

    /**
     * Increment the counter by one
     */
    public long increment()
    {
        return increment(1);
    }

    /**
     * Increment the counter by the given amount
     */
    public long increment(long by)
    {
        count += by;

        return count;
    }

    /**
     * Convert to logging format.
     */
    public void report(Map<String,Object> metrics)
    {
        metrics.put(name + ".count", count);
    }

}
