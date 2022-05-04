package com.apixio.logger.metrics;

import com.codahale.metrics.MetricRegistry;

/**
 * Manage a common version of Metrics registry.
 * Allows advanced behaviors like polled publishing.
 * 
 * @author lance
 *
 */
public class MetricsUtil {
    private static MetricRegistry registry = new MetricRegistry();
    private static MetricPublisher publisher = null;
    
     public static MetricRegistry getRegistry() {
        return registry;
    }
    
     public static void setRegistry(MetricRegistry registry) {
         MetricsUtil.registry = registry;
     }

    public static MetricPublisher getPublisher() {
        return publisher;
    }

    public static void setPublisher(MetricPublisher publisher) {
        MetricsUtil.publisher = publisher;
    }
          
}
