package com.apixio.logger.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Implement minimal support for Coda Hale's Metrics library annotations.
 * Annotations are implemented via dynamic proxies managed by MetricsProxy.
 * 
 * @author lance
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Metrics {
    /**
     * Publish Metric values to MDC upon method return.
     * 
     * @return
     */
    boolean managed() default true;
    /**
     * Publish Metric values to external ScheduledReporter upon method return.
     * 
     * @return
     */
    boolean report() default true;
}
