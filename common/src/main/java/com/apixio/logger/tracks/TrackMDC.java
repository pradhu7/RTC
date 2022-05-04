package com.apixio.logger.tracks;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.MDC;

/**
 * maintain stack of key-value pairs <String,Object>
 * Tracks version of MDC, with one difference: it can be a stack frame for Tracked classes.
 * TrackMDC.frame() dictates whether a Tracked class maintains a separate disjoint TrackMDC frame for 
 * a method call in that class.
 * 
 * @author lance
 *
 */

public class TrackMDC {
    private static final ThreadLocal<Map<String, Object>> mdc = new ThreadLocal<Map<String,Object>>();

    /**
     * Return value for key, or null if not set.
     * 
     * @param key
     * @return
     */
    public static Object get(String key) {
        return checkmdc().get(key);
    }

    /**
     * Set key, value pair
     * 
     * @param key
     * @param value
     */
    public static void put(String key, Object value) {
        checkmdc().put(key, value);
    }

    /**
     * Clear frame
     * 
     */
    public static void clear() {
        checkmdc().clear();
    }
    
    /**
     * Get copy of all key, value pairs
     * (maybe should be internal to package)
     * 
     * @return
     */
    static public Map<String, Object> getCopyOfContext() {
        Map<String, Object> copy = new HashMap<String, Object>();
        copy.putAll(checkmdc());
        return copy;
    }

    static void publishToMDC() {
        Map<String, Object> map = checkmdc();
        for(String key: map.keySet()) {
            Object obj = map.get(key);
            if (obj instanceof Log)
                ((Log) obj).publishToMDC();
            else if (obj != null)
                MDC.put(key, obj.toString());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void putMap(Map map) {
        checkmdc().putAll(map);
    }

    private static Map<String, Object> checkmdc() {
        Map<String, Object> map = mdc.get();
        if (map == null) {
            map = new HashMap<String, Object>();
            mdc.set(map);
        }
        return map;
    }


}


