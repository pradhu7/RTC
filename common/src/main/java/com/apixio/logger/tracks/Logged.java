package com.apixio.logger.tracks;

import org.apache.log4j.MDC;

/**
 * Generic logged object.
 * Also use to access customized Log objects.
 * 
 * @author lance
 *
 */
public class Logged implements Log {
    private final String name;
    private Object obj;
    
    /**
     * Add an object with a name.
     * Return a settable Log object with that name.
     * Don't make new Logged object for re-set of same name.
     * 
     * Object could be this class, a Clock, or other implementation of Log.
     * It just needs a toString() method.
     * 
     * Shorthand for 
     * 
     * @param name
     * @param obj
     * @return
     */
    
    public static Logged put(String name, Object obj) {
        Object it = TrackMDC.get(name);
        if (obj instanceof Log && obj == it) {
            return (Logged) obj;
        }
        if (it instanceof Log) {
            Logged l = (Logged) it;
            // yes, reference comparison
            if (l.obj == obj) 
                return l;
        }
        return new Logged(name, obj);
    }
    
    public Logged(String name, Object obj) {
        this.name = name;
        this.obj = obj;
        TrackMDC.put(name, obj instanceof Log ? obj : this);
    }
    
    public Logged(String name) {
        this.name = name;
        TrackMDC.put(name, null);
    }
    
    public Object get() {
        return obj;
    }
    
    public void set(Object obj) {
        this.obj = obj;
    }
    
    @Override
    public String toString() {
        return obj.toString();
    }

    @Override
    public void publishToMDC() {
        if (obj == null)
            MDC.remove(name);
        else
            MDC.put(name, obj.toString());
    }
    
}
