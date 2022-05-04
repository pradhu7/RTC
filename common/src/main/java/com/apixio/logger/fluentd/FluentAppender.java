package com.apixio.logger.fluentd;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.apixio.logger.fluentd.client.FluentClient;

/**
 * Log4j appender for Fluent log forwarder.
 * Started as https://github.com/tanaka-takayoshi/fluentd-log4j-appender.git
 * which had problems.
 * 
 * Priority of params: constructor values are highest, then system properties, then
 * Spring properties. 
 * 
 * TODO: make Spring app properties app.
 * 
 * @author lance
 * 
 * TODO: use logging event timestamp instead of fetching system timestamp ourselves
 * TODO: migrate custom fields out into StandardMetrics.java
 */

public class FluentAppender extends AppenderSkeleton {
    Logger log = Logger.getLogger(FluentAppender.class);

    public static String JOBNAME = "apixio_job_name";
    private static SimpleDateFormat dateFormatGmt = null;

    private FluentClient fluentClient;
    private long timer = 0L;
    private String session = null;
    private String thread = null;
    private String jobname = null;
    private String ipSequenceFileName = null;
    private String ipSequenceFilePath = null;
    private Appender consoleAppender = null;

    protected String level = "INFO";
    protected String host = "localhost";
    protected int port = 24224;
    protected String tag;
    protected String label;
    // hard left-hand prefix
    protected String prefix;
    // soft middle - used differently in fluent and graphite
    protected String group;

    public FluentAppender() {
        log.info("Constructor");
        init();
        host = System.getProperty("FLUENT_HOST", this.host);
        tag = System.getProperty("FLUENT_TAG", this.tag);
        label = System.getProperty("FLUENT_LABEL", this.label);
        prefix = System.getProperty("FLUENT_PREFIX", this.prefix);
    }

    public FluentAppender(String host, String tag, String label) {
        init();
        this.host = host;
        this.tag = tag;
        this.label = label;
        this.prefix = tag + "." + label;
    }

    private void init() {
        if (dateFormatGmt == null) {
            // Use Solr format to make indexing easier
            dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            jobname = System.getProperty(JOBNAME);
        }
    }

    @Override
    public void activateOptions() {
        log.info("activateOptions");
        super.activateOptions();
        if (prefix != null) {
            int dot = prefix.indexOf('.');
            if (dot == -1 || dot > prefix.length() -2) {
                tag = prefix;
                label = "label";
            } else {
                tag = prefix.substring(0, dot);
                label = prefix.substring(dot + 1);
            }
        } else {
            prefix = tag + "." + label;
        }
        try {
            fluentClient = FluentClient.getClient(tag, host, port);
        } catch (Exception e) {
            log.error("Cannot create FluentClient " + host + ", " + tag + ", " + label);
        }
        log.info("FluentAppender: " + host + ", " + prefix);
    }

    private String substitute(String template) {
        InetAddress ip = null;
        try {
            ip = InetAddress.getLocalHost();

        } catch (UnknownHostException e) {
            // Highly unlikely, leave original
            return template;
        }
        // hack - ip-1-1-1-1 is the amazon internal IP format
        // Don't want period in key
        String hostname = "ip-" + ip.getHostAddress().replace('.','-');
        String subst = template.replaceAll("%I", hostname);
        if (jobname != null) {
            subst = subst.replaceAll("%J", jobname);
        }
        return subst;
    }

    @Override
    public void close() {
        fluentClient.flush();
        fluentClient.close();
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    // add all kinds of variables.
    // allow Map<String,Object> as message- it's like a Log4j Formatter object
    @Override
    protected void append(LoggingEvent event) {
        String evLevel = event.getLevel().toString();
        if (host == null || tag == null || label == null || prefix == null) {
            throw new IllegalStateException("FluentAppender not initialized: host/label/tag = " + host + "/" + tag + "/" + label);
        }
        
        if (level != null && level.equals("EVENT") && 
                        ( evLevel.equals("INFO") || evLevel.equals("DEBUG")))
            return;
        Map<String, Object> messages = new HashMap<String, Object>();
        messages.put("level", evLevel);
        messages.put("loggerName", event.getLoggerName());
        //		String ip = substitute("%I");
        //		messages.put("hostname", ip);
        if (jobname != null)
            messages.put("jobname", jobname);
        if (ipSequenceFileName != null)
            messages.put("inputSeqFileName", ipSequenceFileName);
        if (ipSequenceFilePath != null)
            messages.put("inputSeqFilePath", ipSequenceFilePath);
        messages.put("source", tag + "." + substitute(label));
        Date now = new Date();
        setTimeFields(messages, now);
        if (session != null)
            messages.put("session", session);
        if (thread != null)
            messages.put("thread", thread);
        if (group != null)
            messages.put("group", group);
        if (event.getMessage() instanceof Map) {
            try {
                Map<String, Object> fields = (Map<String, Object>) event.getMessage();
                // should call toString right here!
                for(String key: fields.keySet())
                    messages.put(key, fields.get(key));
            } catch (Exception e) {

            }
        } else if (event.getMessage() != null){
            String msg = (String) event.getMessage().toString();
            messages.put("message", msg);
        }
        String msg = (String) messages.get("message");
        if (msg != null && msg.startsWith("Discarded")) {
            // HACK: Asyncappender sends us a message when it discards, and we have to make an error out of it.
            // AsyncAppender "Discarded N messages" notification
            messages.put("error.message", "AsyncAppender Discarded messages");
            messages.put("level", "ERROR");
        }

        send(event, messages);
    }

    private void send(LoggingEvent event, Map<String, Object> messages) {
        if (!requireClient()) {
            if (consoleAppender != null && (System.currentTimeMillis() - timer) > 1000)
                consoleAppender.doAppend(new LoggingEvent(getName(), event.getLogger(), Level.ERROR, 
                                "Fluent client not available, no messages dropped", null));
                timer = System.currentTimeMillis();
            return;
        }
        requireClient();
        try {
            if (fluentClient == null) 
                try {
                    fluentClient = FluentClient.getClient(tag, host, port);
                    Map<String,Object> notification = new HashMap<>();
                    notification.put("message", "Creating new fluent client");
                    setTimeFields(notification, new Date());
                    packTree(substitute(label), notification, System.currentTimeMillis()/1000);
                } catch (Throwable t) {
                    return;
                }
            packTree(substitute(label), messages, event.getTimeStamp()/1000);
        } catch (IOException e) {
            if (consoleAppender != null) {
                consoleAppender.doAppend(new LoggingEvent(getName(), event.getLogger(), Level.ERROR, "Fluent send failed, " + messages.toString(), e));
            };
            try {
                
            } catch(Throwable t) {
                fluentClient.close();
                fluentClient = null;
                try {
                    fluentClient = FluentClient.getClient(tag, host, port);
                    packTree(substitute(label), messages, event.getTimeStamp()/1000);
                } catch (Exception e1) {
                    ;
                }
            }
        }
    }

    private boolean requireClient() {
        try {
            if (fluentClient == null) 
                    fluentClient = FluentClient.getClient(tag, host, port);
        } catch (Exception e) {
            ;
        }
        return fluentClient != null;
    }

    private void setTimeFields(Map<String, Object> messages, Date now) {
        synchronized(dateFormatGmt) {
            String utc = dateFormatGmt.format(now);
            messages.put("time", Long.toString(now.getTime()));
            messages.put("datestamp", utc);
        }
    }

    // hack for json in hive - really annoying!
    private void packTree(String substitute, Map<String, Object> messages, long time) throws IOException {
        Map<String,Object> mapped = new HashMap<String,Object>();
        for(String key: messages.keySet()) {
            Object value = messages.get(key);
            if (value instanceof String || value instanceof List)
                addToTree(mapped, key, value);
            else
                log.error("Message field must be string or string[], not: " + value.getClass().getCanonicalName());
        }
        fluentClient.log(substitute, mapped, time);
    }

    // fabricate tree of maps so that Fluentd serializer sends as struct
    // leaf of this tree is either a String or an array of Strings
    public void addToTree(Map<String, Object> mapped, String key, Object value) {
        String[] parts = key.split("\\.");
        // walk 
        for (int i = 0; i < parts.length - 1; i++) {
            String branch = parts[i];
            Object node = mapped.get(branch);
            if (node == null) {
                Map<String, Object> sub = new HashMap<String,Object>();
                mapped.put(branch, sub);
                mapped = sub;
            } else if (!(node instanceof Map<?,?>)) {

            } else {
                mapped = (Map<String,Object>) node;
            } 
        }
        mapped.put(parts[parts.length - 1], value);
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

   public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getJobname() {
        return jobname;
    }

    public void setJobname(String jobName) {
        this.jobname = jobName;
    }

    public FluentClient getFluentLogger() {
        return fluentClient;
    }

    public void setFluentLogger(FluentClient fluentLogger) {
        this.fluentClient = fluentLogger;
    }

    public String getIpSequenceFileName() {
        return ipSequenceFileName;
    }

    public void setIpSequenceFileName(String ipSequenceFileName) {
        this.ipSequenceFileName = ipSequenceFileName;
    }

    public String getIpSequenceFilePath() {
        return ipSequenceFilePath;
    }

    public void setIpSequenceFilePath(String ipSequenceFilePath) {
        this.ipSequenceFilePath = ipSequenceFilePath;
    }

    public Appender getConsoleAppender() {
        return consoleAppender;
    }

    public void setConsoleAppender(Appender consoleAppender) {
        this.consoleAppender = consoleAppender;
    }

}
