package com.apixio.logger.metrics;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.MDC;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;

/**
 * Implement annotations from Coda Hale's Metrics library.
 * 
 * Send all metrics from call X to MDC at end of call X.
 *   
 * @author lance
 *
 */

public class MetricsProxy implements java.lang.reflect.InvocationHandler {
    private static ScheduledReporter reporter = null;
    // Only support Meter, ExceptionMeter and Timer. These are never set.
    private static SortedMap<String, Gauge> gauges = new TreeMap<String, Gauge>();
    private static SortedMap<String, Counter> counters = new TreeMap<String, Counter>();
    private static SortedMap<String, Histogram> histograms = new TreeMap<String, Histogram>();

    private final Object obj;
    private Metrics classAnno = null;

    // methodname -> (metric name, metric)
    private Map<String, Pair<Timer>> timers = new HashMap<String, Pair<Timer>>();
    private Map<String, Pair<Meter>> meters = new HashMap<String, Pair<Meter>>();
    private Map<String, Pair<Meter>> exceptions = new HashMap<String, Pair<Meter>>();
    private static final MetricRegistry privateRegistry = new MetricRegistry();
    private static MetricRegistry externalRegistry;
    
    /**
     * Set MetricRegistry object.
     * Default private registry for this dynamic object.
     * 
     * @param registry
     */

    public static void setRegistry(MetricRegistry registry) {
        // add any existing metrics - combats creation order problem
        MetricsProxy.externalRegistry = registry;
    }
    
    /** 
     * Set Metric ScheduledReporter object.
     * Otherwise save to MDC.
     * 
     * @param reporter
     */

    public static void setReporter(ScheduledReporter reporter) {
        MetricsProxy.reporter = reporter;
    }

    public static Object newInstance(Object obj) {
        return newInstance(obj, obj);
    }
    
    /**
     * Wrap given object with a MetricProxy object.
     */

    public static Object newInstance(Object obj, Object master) {
        // Proxy caches generated cache types so no need for us to.
        return java.lang.reflect.Proxy.newProxyInstance(obj.getClass().getClassLoader(), 
                        obj.getClass().getInterfaces(), new MetricsProxy(obj, master));
    }

    /**
     * Pick apart annotations in class. Create everything we need to process in invoke().
     * Allow wrapped proxies with proxy, original object
     * 
     * @param obj
     */
    private MetricsProxy(Object obj, Object original)  {
        this.obj = obj;
        Class cl = original.getClass();
        for(Annotation anno : cl.getDeclaredAnnotations()) {
            Class<? extends Annotation> type = anno.annotationType();
            if (type.equals(Metrics.class)) {
                classAnno = (Metrics) anno;
            }
        }
        if (classAnno == null)
            throw new IllegalArgumentException("MetricsProxy: Class must have @Metrics annotation: " + cl.getCanonicalName());

        // Find annotations and create metric objects
        MetricRegistry registry = classAnno.managed() ? MetricsProxy.privateRegistry : MetricsProxy.externalRegistry;
        for(Method m: original.getClass().getMethods()) {
            String methodKey = getMethodKey(m);
            for(Annotation ma: m.getDeclaredAnnotations()) {
                Class<? extends Annotation> annotationType = ma.annotationType();
                if (annotationType.equals(Timed.class)) {
                    Timed anno = (Timed) ma;
                    String name = chooseName(anno.name(), anno.absolute(), cl);
                    timers.put(methodKey, new Pair<Timer>(name, registry.timer(name)));
                }
                if (annotationType.equals(Metered.class)) {
                    Metered anno = (Metered) ma;
                    String name = chooseName(anno.name(), anno.absolute(), cl);
                    meters.put(methodKey, new Pair<Meter>(name, registry.meter(name)));
                }
                if (annotationType.equals(ExceptionMetered.class)) {
                    ExceptionMetered anno = (ExceptionMetered) ma;
                    String name = chooseName(anno.name(), anno.absolute(), cl);
                    exceptions.put(methodKey, new Pair<Meter>(name, registry.meter(name)));
                }
            }
        }
    }
    
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        long start = -1L;

        String methodKey = getMethodKey(m);
        Pair<Timer> timer = timers.get(methodKey);
        Pair<Meter> execMeter = exceptions.get(methodKey);
        if (timer != null) {
            start = System.currentTimeMillis();
        }
        try {
            return m.invoke(obj, args);
        } catch (InvocationTargetException e) {
            if (execMeter != null) {
                execMeter.metric.mark();
            }
            throw e.getTargetException();
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        } finally {
            if (timer != null) {
                timer.metric.update(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
            }
            Pair<Meter> meter = meters.get(methodKey);
            if (meter != null) {
                meter.metric.mark();
            }
            if (classAnno.managed()) {
                Map<String, Object> values;
                MetricPublisher publisher = new MetricPublisher();
                values = new HashMap<String, Object>();
                publisher.setCollector(values);                
                if (timer != null) {
                    publisher.logTimer(timer.name, timer.metric);
                }
                if (meter != null) {
                    publisher.logMeter(meter.name, meter.metric);
                }
                if (timer != null)
                    publisher.logTimer(timer.name, timer.metric);
                if (execMeter != null) {
                    publisher.logMeter(execMeter.name, execMeter.metric);
                }
                for(Entry<String,Object> entry: values.entrySet()) {
                    MDC.put(entry.getKey(), entry.getValue().toString());
                }
            } else if (classAnno.report()) {
                if (reporter == null) 
                    throw new IllegalStateException("MetricsReporter: ScheduledReporter is not set yet");
                reporter.report();
                SortedMap<String, Meter> meter2 = new TreeMap<String, Meter>();
                SortedMap<String, Timer> timer1 = new TreeMap<String, Timer>();
                if (execMeter != null) 
                    meter2.put(execMeter.name, execMeter.metric);
                if (meter != null) 
                    meter2.put(meter.name, meter.metric);
                if (timer != null)
                    timer1.put(timer.name, timer.metric);
                reporter.report(gauges, counters, histograms, meter2, timer1);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private String getMethodKey(Method m) {
        Class<?>[] parameterTypes = m.getParameterTypes();
        String methodKey = m.getName() + "_";
        for(Class cl: parameterTypes) {
            methodKey += cl.getCanonicalName() + ",";
        }
        methodKey = methodKey.substring(0, methodKey.length() - 1);
        return methodKey;
    }

    private static String chooseName(String explicitName, boolean absolute, Class cl) {
        if (absolute) {
            return explicitName;
        }
        return MetricRegistry.name(cl, explicitName);
    }

    class Pair<T extends Metric> {
        final String name;
        final T metric;
        Pair(String name, T metric) {
            this.name = name;
            this.metric = metric;
        }
    }

}

