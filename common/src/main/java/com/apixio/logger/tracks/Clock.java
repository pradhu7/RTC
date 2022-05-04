package com.apixio.logger.tracks;

import org.apache.log4j.MDC;

/**
 * Add timer to secret stack of metrics.
 * Include ability to get and reset current time delta.
 * 
 * @author lance
 *
 */
public class Clock implements Log {
    private final String name;
    private Long start;
    
    public Clock(String name) {
        this.name = name;
        this.start = System.currentTimeMillis();
        TrackMDC.put(name, this);
    }
    
    /**
     * 
     * Return delta since <b>new</b> or last call to <b>tick</b>().
     */
    public long tick() {
        long delta = System.currentTimeMillis() - start;
        start = System.currentTimeMillis();
        return delta;
    }
    
    @Override
    public String toString() {
        // don't call tick()
        return Long.toString(System.currentTimeMillis() - start);
    }
    
    /**
     * Create and register a Timer.
     * 
     * @param name
     * @return
     */
    public static Clock getTimer(String name) {
        return new Clock(name);
    }

    @Override
    public void publishToMDC() {
        MDC.put(name, this.toString());
    }

}
