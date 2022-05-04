package com.apixio.logger.tracks;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.apixio.logger.EventLogger;
import com.apixio.logger.PollableAsyncAppender;
import com.apixio.logger.StandardMetricsLayout;
import com.apixio.logger.metrics.Metrics;
import com.apixio.logger.metrics.MetricsProxy;

/**
 * Implements features of Tracks logging annotations:
 * <br/>
 *   1) @Tracked Class annotation includes global prefix (like Jersey top-level http path)
 * <br/>
 *   2) @Tracks Method annotation includes sub-prefix (like Jersey method-level paths) 
 * <br/>
 *   3) @Tracks Method annotation supplies 'status' field that is success or error if method throws Exception
 * <br/>
 *   4) @Tracks Method annotation supplies list of constant MDC key/value pairs
 * <br/>
 *   5) @Tracks Method annotation optionally logs all constant, parameter and local variables
 * <br/>
 *   6) @Tracks Method annotation includes "message" for call to logger
 * <br/>
 *   7) @Track Method parameter annotation creates MDC key-value pair for the incoming value
 * <br/>
 *   8) @Metrics - if class has @Metrics create & wrap its MetricsProxy as convenience. The glue for this is tricky.
 * <br/><br/>
 *   
 * @author lance
 * 
 * TODO: do MetricsProxy wrapper also as courtesy
 *
 */

public class TrackedProxy implements java.lang.reflect.InvocationHandler {
    // triggers sequences of EventLogger->Log4j creation - has to be here
    private static final Logger logger = Logger.getLogger(TrackedProxy.class);
    private EventLogger classLogger = null;
    private Tracked baseClassAnno = null;
    private Map<String, Tracks> baseMethodAnnos = new HashMap<String, Tracks>();
    private Map<String, String[]> paramNames = new HashMap<String, String[]>();

    // base object OR MetricsProxy object for base object
    private Object obj;
    private Object baseObj;

    /**
     * Wrap given object with a dynamic TrackedProxy
     * 
     * @param obj
     * @return
     */
    public static Object newInstance(Object obj) {
        return java.lang.reflect.Proxy.newProxyInstance(obj.getClass().getClassLoader(), 
                        obj.getClass().getInterfaces(), new TrackedProxy(obj));
    }

    /**
     * Pick apart annotations in class. Cache everything we need to process in invoke().
     * 
     * @param obj
     */
    private TrackedProxy(Object obj)  {
        this.obj = obj;
        this.baseObj = obj;
        Class<? extends Object> baseClass = obj.getClass();
        for(Annotation anno : baseClass.getDeclaredAnnotations()) {
            Class<? extends Annotation> type = anno.annotationType();
            if (type.equals(Tracked.class)) {
                baseClassAnno = (Tracked) anno;
            }
        }
        if (baseClassAnno == null)
            return;
        if (baseClass.getAnnotation(Metrics.class) != null) {
            Object metricsProxy = MetricsProxy.newInstance(obj);
            this.obj = metricsProxy;
        }
        if (baseClassAnno.doLog())
            classLogger = EventLogger.getLogger(baseClass.getCanonicalName(), Logger.getRootLogger());
        // cache all annotations
        for(Method m: baseClass.getMethods()) {
            String methodKey = getMethodKey(m);
            for(Annotation ma: m.getDeclaredAnnotations()) {
                if (ma.annotationType().equals(Tracks.class)) {
                    Tracks anno = (Tracks) ma;
                    baseMethodAnnos.put(methodKey, (Tracks) anno);
                    if (anno.value().length % 2 == 1)
                        throw new IllegalArgumentException("Tracks value field must have an even number of strings: method " + 
                                        baseClass.getCanonicalName() + "." + methodKey);
                }
            }
            int param = 0;
            String[] args = new String[m.getParameterAnnotations().length];
            boolean found = false;
            for(Annotation[] params: m.getParameterAnnotations()) {
                for(Annotation anno: params) {
                    if (anno.annotationType().equals(TrackParam.class)) {
                        TrackParam t = (TrackParam) anno;
                        String name = ((TrackParam) anno).value();
                        args[param] = name;
                        found = true;
                    }
                }
                param++;
            }
            if (found) {
                paramNames.put(methodKey, args);
            }
        }
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        Logged statusLog = null;
        long start = -1L;

        String methodKey = getMethodKey(m);
        if (baseClassAnno == null || !baseMethodAnnos.containsKey(methodKey)) {
            try {
                return m.invoke(obj, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        Map<String, Object> mymdc = null;
        if (baseClassAnno.isFrame()) {
            mymdc = TrackMDC.getCopyOfContext();
            TrackMDC.clear();
        }
        Map<String, String> oldmdc = null;
        if (! baseClassAnno.clearMDC()) {
            oldmdc = MDC.getContext();
        }
        MDC.clear();

        // async appender tracking
        pollLoggerQueues();

        // first, add all data from annotations. 
        Tracks anno = baseMethodAnnos.get(methodKey);
        String[] values = anno.value();
        for(int i = 0; i < values.length; i += 2) {
            TrackMDC.put(values[i], values[i+1].toString());
        }
        String status = anno.status();
        if (status.length() > 0) {
            statusLog = new Logged(status, "success");
            TrackMDC.put(status, statusLog);
        }
        String timer = anno.timer();
        if (timer.length() > 0) {
            start = System.currentTimeMillis();
        }
        if (paramNames.containsKey(methodKey)) {
            String[] params = paramNames.get(methodKey);
            for(int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    Object value = args[i];
                    if (value != null)
                        TrackMDC.put(params[i], value.toString());
                }
            }
        }
        try {
            Object result = m.invoke(obj, args);
            return result;
        } catch (InvocationTargetException e) {
            // get nice stack trace printer
            Throwable t = e.getTargetException();
            String message = t.getMessage() + ": " + printStackTrace(t);
            if (statusLog != null) {
                statusLog.set("error");
                if (baseClassAnno.doLog())
                    this.classLogger.error(message);
            }
            if (baseClassAnno.standard()) {
                // strip off debug details
                // maybe go to the first number then back off to a punctuation?
                if (message.contains(":"))
                    message = message.substring(0, message.indexOf(':'));
                TrackMDC.put("error.message", message);
            }
            throw t;
        } catch (Throwable t) {
            t.hashCode();
            throw t;
        } finally { 
            if (timer.length() > 0) {
                String delta = Long.toString(System.currentTimeMillis() - start);
                TrackMDC.put(timer, delta);
            }
            if (baseClassAnno.doLog())
            {
                // All data from annotations is now saved, and can be overwritten by dynamic stack frame.
                // Publish Log and other objects from the stack frame.
                TrackMDC.publishToMDC();
                Map<String,Object> message = new TreeMap<String, Object>();
                if (baseClassAnno.standard()) {
                    message.put("loggerName", classLogger.getName());
                    StandardMetricsLayout.addJVMMetrics(message);
                    for(String key: message.keySet())
                        message.put(key, message.get(key).toString());
                }
                Hashtable<String, Object> mdcmap = MDC.getContext();
                if (mdcmap != null)
                    for(String key: mdcmap.keySet())
                        message.put(key, mdcmap.get(key).toString());
                if (anno.message().length() > 0)
                    message.put("message", anno.message());
                this.classLogger.event(message);
            }
            if (baseClassAnno.isFrame()) {
                TrackMDC.clear();
                TrackMDC.putMap(mymdc);
            }
            MDC.clear();
            if (!baseClassAnno.clearMDC() && oldmdc != null) {
                for(String key: oldmdc.keySet()) {
                    Object val = oldmdc.get(key);
                    MDC.put(key, val.toString());
                }
            }
        }
    }

    private void pollLoggerQueues() {
        Enumeration apps = classLogger.getAllAppenders();
        int max = 0;
        while (apps.hasMoreElements()) {
            Object next = apps.nextElement();
            if (next instanceof PollableAsyncAppender) {
                max = Math.max(max, ((PollableAsyncAppender)next).getQueueSize());
            }
        }
        MDC.put("tracks.max.count", Integer.toString(max));
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

    private String printStackTrace(Throwable base) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        base.printStackTrace(writer);
        return baos.toString();
    }

    private Throwable getBaseException(Throwable t) {
        Throwable base = t;
        int count = 5;
        // sometimes base self-points
        while (count > 0 &&base.getCause() != null) {
            base = t.getCause();
            count--;
        }
        return base;
    }

}

