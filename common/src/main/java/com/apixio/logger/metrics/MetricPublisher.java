package com.apixio.logger.metrics;

/*
 * License- Same as Coda Hale's Metrics.
 */

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.Map;


/**
 * A publisher class for logging metrics values to a map.
 * Drastically reduced from Metrics MDCReporter.
 * Not sure about 'rate' stuff- it wants seconds v.s. milliseconds
 */

public class MetricPublisher {
    Map<String, Object> collector;

    public MetricPublisher() {
    }
    
    public Map<String, Object> getCollector() {
        return collector;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public void logTimer(String name, Timer timer) {
        final Snapshot snapshot = timer.getSnapshot();
        Writer writer = new Writer("TIMER", name);
        writer.write("count", timer.getCount());
        writer.write("min", snapshot.getMin());
        writer.write("max", snapshot.getMax());
        writer.write("mean", snapshot.getMean());
        writer.write("stddev", snapshot.getStdDev());
        writer.write("median", snapshot.getMedian());
        writer.write("p75", snapshot.get75thPercentile());
        writer.write("p95", snapshot.get95thPercentile());
        writer.write("p98", snapshot.get98thPercentile());
        writer.write("p99", snapshot.get99thPercentile());
        writer.write("p999", snapshot.get999thPercentile());
        writer.write("mean_rate", timer.getMeanRate());
        writer.write("m1", timer.getOneMinuteRate());
        writer.write("m5", timer.getFiveMinuteRate());
        writer.write("m15", timer.getFifteenMinuteRate());
    }

    public void logMeter(String name, Meter meter) {
        Writer writer = new Writer("METER", name);
        writer.write("count", meter.getCount());
        writer.write("mean_rate", meter.getMeanRate());
        writer.write("m", meter.getOneMinuteRate());
        writer.write("m5", meter.getFiveMinuteRate());
        writer.write("m15", meter.getFifteenMinuteRate());
    }

    public void logHistogram(String name, Histogram histogram) {
        final Snapshot snapshot = histogram.getSnapshot();
        Writer writer = new Writer("HISTOGRAM", name);
        writer.write("count", histogram.getCount());
        writer.write("min", snapshot.getMin());
        writer.write("max", snapshot.getMax());
        writer.write("mean", snapshot.getMean());
        writer.write("stddev", snapshot.getStdDev());
        writer.write("median", snapshot.getMedian());
        writer.write("p75", snapshot.get75thPercentile());
        writer.write("p95", snapshot.get95thPercentile());
        writer.write("p98", snapshot.get98thPercentile());
        writer.write("p99", snapshot.get99thPercentile());
        writer.write("p999", snapshot.get999thPercentile());
    }

    public void logCounter(String name, Counter counter) {
        Writer writer = new Writer("COUNTER", name);
        writer.write("count", counter.getCount());
    }

    public void logGauge(String name, Gauge<?> gauge) {
        Writer writer = new Writer("GAUGE", name);
        writer.write("value", gauge.getValue());
    }

    // actual policy for combining name and key
    class Writer {
         final String type;
        private final String name;

        public Writer(String type, String name) {
            this.type = type;
            this.name = name;
            collector.put(name + "." + "type", type);
        }

        public void write(String key, Object value) {
            collector.put(name + "." + key, value.toString());
        }
    }
}

