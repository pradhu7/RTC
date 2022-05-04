package com.apixio.logger.tracks;

/**
 * Any metric or data object. 
 * publishToMDC() captures current state of object to log4j MDC map.
 * This can have any number of names and entries.
 * 
 * toString() should print something but is not required to be complete.
 * 
 * @author lance
 *
 */
public interface Log {
    
    /**
     * Post object values to MDC.
     * Can do anything here.
     */
    public void publishToMDC();
}
