package com.apixio.logger.graphite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4j Appender which sends individual metrics to Graphite.
 * 
 * Special MetricLoggingEvent stores a custom set of props
 * that are sent to Graphite. We only send those on an append() call.
 * 
 * Send prefix.metric or prefix."groupField".metric
 * 
 * AsyncAppender passes down Discard messages. Forward
 * them to a different appender. Uses log4j's appender-ref
 * to set appenders in log4j.xml. For once, an elegant solution!
 * 
 * @author lance
 *
 */

public class GraphiteAppender extends AppenderSkeleton {
    Logger log = Logger.getLogger(GraphiteAppender.class);
    private final AppenderAttachableImpl appenders;

    final List<String> builtinNames = new ArrayList<String>() {{
        add("bytes");
        add("millis");
        add("count");
        add("probe");
    }};

    protected String host = "graphite.apixio.com";
    protected int port = 2003;
    private GraphiteClient client = null;
    // top prefix
    protected String prefix = null;
    // If set, this field supplies a middle value.
    // Allows segregating metrics by orgid
    private String groupField = null;

    public GraphiteAppender() {
        appenders = new AppenderAttachableImpl();
    }

    public GraphiteAppender(boolean isActive) {
        super(isActive);
        appenders = new AppenderAttachableImpl();
    }

    @Override
    public void close() {
        if (client != null) {
            client.flush();
            client.close();
        }
    }

    @Override
    public void activateOptions() {
        try {
            log.info("GraphiteAppender.activateOptions: " + host + ", " + prefix);
            String host = System.getProperty("GRAPHITE_HOST");
            String prefix = System.getProperty("GRAPHITE_PREFIX");
            client = new GraphiteClient(host == null ? this.host : host, 2003, prefix == null ? this.prefix : prefix);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    };

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(LoggingEvent event) { 
        Object message = event.getMessage();
        if (message instanceof String) {
            if (((String) message).startsWith("Discard")) {
                System.err.println("Graphite AsyncAppender discarded messages: " + message);
                Enumeration apps = appenders.getAllAppenders();
                while(apps.hasMoreElements()) {
                    Appender app = (Appender) apps.nextElement();
                    app.doAppend(event);
                }
            }
            return;
        }
        if (! (message instanceof Map))
            return;
        try
        {
            Map<String,Object> props = (Map<String,Object>) message;
            //                System.out.println("graphite post event");
            String group = null;
            if (groupField != null) {
                group = (String) props.get(groupField);
                if (group == null) 
                    group = "?";
            }
            for(String key:props.keySet()) {
                String value = props.get(key).toString();
                if (groupField != null)
                    key = group + "." + key;
                postEvent(key, value, event.getTimeStamp()/1000);
            }
        } catch (Exception e)
        {
            ;
        }
    }

    void postEvent(String name, String value, long secs) {
        try {
            for(String suffix: builtinNames) {
                if (name.endsWith(suffix)) {
                    //  log.info("Graphite post: " + name + "=" + value);
                    client.post(name, value, secs);
                    break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getGroupField() {
        return groupField;
    }

    public void setGroupField(String groupField) {
        this.groupField = groupField;
    }

    public void setClient(GraphiteClient client) {
        this.client = client;
    }
     
    /**
     * Log4j assembler checks for this method- no interface for it
     */
    public void addAppender(final Appender newAppender) {
      synchronized (appenders) {
        appenders.addAppender(newAppender);
      }
    }

}
