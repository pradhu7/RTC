package com.apixio.logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.log4j.Layout;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Unpack MDC into line of property-format maps. Dense, readable.
 * 
 * @author lance
 *
 */
public class MDCLayout extends Layout {

    private static final String MESSAGE = "message";
    private static final String ISO8601 = "yyyy-MM-dd' 'HH:mm:ss.SSS'Z'";
    // DateFormat classes are not thread-safe but are still ok to cache. 
    // (Inside Voice: what kind of idiot does this?)
    private static final ThreadLocal<DateFormat> dateFormatGmt = new ThreadLocal<DateFormat>();

    public MDCLayout() {
    }

    @Override
    public void activateOptions() {
        DateFormat format = new SimpleDateFormat(ISO8601);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        System.out.println("Activate MDCLayout");
    }

    @Override
    public String format(LoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append( getDateFormat().format(event.getTimeStamp()));
        sb.append(" (level=");
        String level = getMDC("level");
        if (level == null) {
            sb.append(event.getLevel().toString());
        } else {
            sb.append(level);
            MDC.put("level", "");
        }
        sb.append(", ");
        Object message = event.getMessage();
        // map contents have been copied out to MDC. But allow other structures.
        if (message != null && ! (message instanceof Map)) {
            sb.append("message=\"");
            sb.append(message.toString());
            sb.append("\", ");
        }
        Map<String, String> values = getMDCContext();
        if (values != null) {
            SortedSet<String> readable = new TreeSet<String>(values.keySet());
            for(String key: readable) {
                String value = values.get(key).toString();
                if (value.length() > 0) {
                    sb.append(key);
                    sb.append("=");
                    sb.append(value);
                    sb.append(", ");
                }
            }
        }
        sb.setLength(sb.length() - 2);
        sb.append(")\n");
        return sb.toString();
    }

    @Override
    public boolean ignoresThrowable() {
        return true;
    }
    
    DateFormat getDateFormat() {
        if (dateFormatGmt.get() == null) {
            DateFormat format = new SimpleDateFormat(ISO8601);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            dateFormatGmt.set(format);
        }
        return dateFormatGmt.get();
    }

    // depending on libraries included, log4j and slf4j might not be the same
    // in fact MDC might be logback's MDC. oh well.
    String getMDC(String key) {
        Object value = org.apache.log4j.MDC.get(key);
        if (value != null)
            return value.toString();
        return org.slf4j.MDC.get(key);
    }

    Map<String,String> getMDCContext() {
        Map<String, String> values = new HashMap<String, String>();
        Map<String,Object> mapObj = org.apache.log4j.MDC.getContext();
        if (mapObj != null) {
            for(String key: mapObj.keySet()) {
                values.put(key, mapObj.get(key).toString());
            }
        }
        
        // apps might include wrong one.
        Map<String,String> mapString = org.slf4j.MDC.getCopyOfContextMap();
        if (mapString != null) {
            for(String key: mapString.keySet()) {
                values.put(key, mapString.get(key));
            }
        }
        return values;
    }

}
