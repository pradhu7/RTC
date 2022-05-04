package com.apixio.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;

import com.apixio.logger.api.Event;
import com.apixio.logger.fluentd.FluentAppender;

/**
 * 
 * Log4j Logger that writes to:
 * simple log4j,
 * our logs collector (Fluentd)
 * 
 * Every write can include a set of app-specific key-value pairs.
 * 
 * "Tag" and "Label" are, roughly, production/staging and pipeline/tagging/uploader/other application.
 * In Fluentd, these have specific meanings and are used for the routing key.
 * 
 * It turns out that Log4j does not like custom Logger classes, and so this is kind of hacky.
 * Also, Priority is old, use Level everywhere
 * 
 */

public class EventLogger extends Logger {
    private final static Logger log = Logger.getLogger(EventLogger.class);
    private final static Map<String,EventLogger> logmap = new HashMap<String, EventLogger>();
    private static boolean closed = false;
    private static FluentAppender fluentAppender = null;

    // environment.application
    private String prefix = null;

    public EventLogger(String name) {
        super(name);
        Logger root = Logger.getRootLogger();
        applyLogger(root, false);
    }

    public EventLogger(String name, Logger logger) {
        this(name);
        if (logger != null) {
            applyLogger(logger, true);
        } 
    }

    public static EventLogger getLogger(String name) {
        Logger l = LogManager.exists(name);
        if (l == null || l instanceof EventLogger) {
            EventLogger el = logmap.get(name);
            if (el == null) {
                el = new EventLogger(name);
                logmap.put(name, el);
            }
            return el;
        }
        throw new IllegalArgumentException("Logger already registered: " + name);
    }

    public static EventLogger getLogger(String name, Logger logger) {
        Logger l = LogManager.exists(name);
        if (l instanceof EventLogger) {
            EventLogger ev = (EventLogger) l;
            ev.addAppenders(logger);
            return ev;
        } else if (l == null) {
            return new EventLogger(name, logger);
        } else {
            throw new IllegalArgumentException("Logger already registered: " + name);
        }
    }

    public void event(Object message) {
        Map<String, Object> message2 = new HashMap<String, Object>();
        captureValues(message, message2);
        forcedLog(name, Event.EVENT, message2, null);
    }
    public void error(String message, Throwable t) {
        super.error(message + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(t).replace('\n', '/'));
    }

    void captureValues(Object message, Map<String, Object> message2) {
        if (message != null) { 
            if (message instanceof Map) {
                Map<String, Object> orig = (Map<String, Object>) message;
                for(String key: orig.keySet()) {
                    if (orig.get(key) != null)
                        message2.put(key, getCapture(orig.get(key)));
                    //					else
                    //						props.put(key, "null");
                }
            } else {
                if (!message2.containsKey("message"))
                    message2.put("message", getCapture(message));
            }
        }
    }

    public void setRepo(LoggerRepository repo) {
        repository = repo;
    }


    // load all generic logger stuff, and get all appenders
    // registers logger with Logger.getLogger
    private void applyLogger(Logger logger, boolean appenders) {
        setLevel(logger.getLevel());
        setRepo(logger.getLoggerRepository());
        if (appenders) {
            addAppenders(logger);
        }
    }

    public void addAppenders(Logger logger) {
        Enumeration<Appender> apps = logger.getAllAppenders();
        while(apps.hasMoreElements()) {
            Appender app = apps.nextElement();
            log.info("Adding appender " + app.getName() + " from logger " + logger.getName());
            if (app instanceof AsyncAppender) {
                Enumeration<Appender> applist = ((AsyncAppender) app).getAllAppenders();
                while(applist.hasMoreElements()) {
                    log.info("\tasync appender: " + applist.nextElement().getName());
                }
            }
            this.addAppender(app);
        }
    }

    @Override
    public synchronized void addAppender(Appender newAppender)
    {
        log.info("Adding appender: " + newAppender.getName());
        super.addAppender(newAppender);
    }

    // choke point for all log calls - turn Map<String,Object> into strings
    // support collections as 'Object', capture strings from members of collection
    // if called "message", make that the string message;
    // this could be a layout!
    @Override
    protected void forcedLog(String fqcn, Priority level, Object message, Throwable t) {
        String text = null;
        // some kind of concurrency problem! This is really expensive and i don't know why it's needed.
        Map<Object, Object> message2 = new ConcurrentHashMap<Object, Object>();
        if (message == null) {
            message2.put("message", "null");
        } else if (message instanceof Map) { 
            text = message.toString();
            Map<String, Object> msg = (Map<String, Object>) message;
            for(String key: msg.keySet()) {
                Object value = msg.get(key);
                if (value == null)
                    continue;
                message2.put(key, getCapture(value));
                if (key.equals("message")) {
                    text = value.toString();
                } else if (key.endsWith(".bytes")) {
                    String counter = key.substring(0, key.length() - 5) + "count";
                    if (! msg.containsKey(counter)) {
                        message2.put(counter, "1");
                    }
                } else if (key.endsWith(".millis")) {
                    String counter = key.substring(0, key.length() - 6) + "count";
                    if (! msg.containsKey(counter)) {
                        message2.put(counter, "1");
                    }
                }
            }
        } else {
            message2.put("message", message.toString());
        }
        // deliver to MDCLayout console appender
        for(Object key: message2.keySet()) {
            MDC.put(key.toString(), message2.get(key).toString());
        }
        LoggingEvent event = new LoggingEvent(getName(), this, level, message2, t); 
        callAppenders(event);
    }

    private Object getCapture(Object value)
    {
        if (value instanceof Collection<?>) {
            Collection<? extends Object> collection = (Collection<? extends Object>) value;
            Object[] arr = collection.toArray();
            List<String> capture = new ArrayList<String>();

            for(int i = 0; i < arr.length; i++) {
                if (arr[i] != null)
                    capture.add(arr[i].toString());
                else
                    capture.add("null");
            }
            return capture;
        } else if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            List<String> capture = new ArrayList<String>();

            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null)
                    capture.add(arr[i].toString());
                else
                    capture.add("null");
            }
            return capture;
        // TODO: } else if (value instanceof Throwable){ // obviously toString is not good enough. newlines need to be escaped or something to go to hive logs.
        } else {
            return value.toString();
        }
    }

    public String getPrefix() {
        return prefix;
    }

    // this damn thing just gets worse and worse
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        Enumeration<Appender> apps = getAllAppenders();
        while(apps.hasMoreElements()) {
            Appender app = apps.nextElement();
            if (app instanceof FluentAppender) {
                ((FluentAppender) app).setPrefix(prefix);
                fluentAppender = (FluentAppender) app;
            }
            else if (app instanceof AsyncAppender) {
                Enumeration<Appender> applist = ((AsyncAppender) app).getAllAppenders();
                while(applist.hasMoreElements()) {
                    Appender subapp = apps.nextElement();
                    if (subapp instanceof FluentAppender) {
                        ((FluentAppender) subapp).setPrefix(prefix);
                        fluentAppender = (FluentAppender) subapp;
                   }
                }
            }
        }
        FluentCheckup.initCheckup();
    }

    public void close() {
        this.closeAll();
    }
    
    // really only care about fluent. 
    public static boolean isUp() {
        if (fluentAppender != null)
            return fluentAppender.getFluentLogger().isUp();
        else 
            return true;
    }

    // async appender needs to push all
    // only call this once
    public synchronized static void closeAll() {
        if (closed)
            return;
        Enumeration<Appender> apps = log.getAllAppenders();
        while(apps.hasMoreElements()) {
            Appender app = apps.nextElement();
            app.close();
        }
        closed = true;

        // From testing it appears that EventLogger.close() doesn't flush all the way to fluentd
        // before returning so we sleep a bit here.  Truly truly ugly
        try {
            Thread.sleep(100L);
        } catch (InterruptedException x) {
        }
    }
    
    // make super-sure it gets closed- have to flush AsyncAppender
    public void finalize() {
        close();
    }

}
