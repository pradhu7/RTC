package com.apixio.sdk.metric;

import java.util.Map;

/**
 * A Metric is a named single bit of data that is numeric and quantifiable in nature.
 * As metrics are intended to be queried and analyzed in aggregate, it's useful to have
 * a known and standard naming scheme for these data.  It's also useful to have a known
 * set of metric types.
 *
 * The supported types of metrics are:
 *
 *  * timer, for duration of some operation
 *  * counter, for a total count of some operation
 *  * gauge, for a point-in-time value (e.g., heap memory usage)
 *
 * Client code that wishes to record metrics must create an instance of the desired
 * metric type, and interact with it as needed.
 */
public abstract class Metric
{
    public enum Type { TIMER, COUNTER, GAUGE }

    /**
     * Name is the client-code-specific identifier to be used when recording/reporting
     * the metric.  Examples:  "fx_duration", "error_count"
     */
    private   Type   type;
    protected String name;

    protected Metric(Type type, String name)
    {
        this.type = type;
        this.name = name;
    }

    /**
     * report converts the single metric into a form that is compatible with FxLogger.
     * Each conversion SHOULD append meaningful-to-the-type string to the name that
     * indicates how the value should be interpreted.
     */
    public abstract void report(Map<String,Object> metrics);

}
