package com.apixio.logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import com.apixio.utility.HostAddressUtil;

/**
 * Add standard metric values to event sent to Fluent and Graphite
 * 
 * @author lance
 *
 */
public class StandardMetricsLayout {

    /**
     * Add JVM metric variables. Anything you want.
     * @param map
     * @return
     */
    public static Map<String,Object> addJVMMetrics(Map<String, Object> map) {
        map.put("jvm.memory.total.bytes", new Object() {
            @Override
            public String toString() {
                return Long.toString(Runtime.getRuntime().totalMemory());
            }
        });
        map.put("jvm.memory.free.bytes", new Object() {
            @Override
            public String toString() {
                return Long.toString(Runtime.getRuntime().freeMemory());
            }
        });
        map.put("jvm.memory.max.bytes", new Object() {
            @Override
            public String toString() {
                return Long.toString(Runtime.getRuntime().maxMemory());
            }
        });
        map.put("jvm.processors.count", new Object() {
            @Override
            public String toString() {
                return Long.toString( Runtime.getRuntime().availableProcessors());
            }
        });
        map.put("jvm.thread", new Object() {
            @Override
            public String toString() {
                return Long.toString(Thread.currentThread().getId());
            }
        });
        map.put("jvm.threads.count", new Object() {
            @Override
            public String toString() {
                return Long.toString(Thread.activeCount());
            }
        });

        final String hostname = "ip-" + HostAddressUtil.getLocalHostLANAddress().replace('.','-');
        map.put("hostname", new Object() {
            @Override
            public String toString() {
                    return hostname;
                }
        });

        final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        map.put("time", new Object() {
            @Override
            public String toString() {
                return Long.toString(System.currentTimeMillis());
            }
        });
        map.put("datestamp", new Object() {
            @Override
            public String toString() {
                return dateFormatGmt.format(new Date());
            }
        });

        return map;
    }

    /**
     * Add Hadoop variables
     */


}
