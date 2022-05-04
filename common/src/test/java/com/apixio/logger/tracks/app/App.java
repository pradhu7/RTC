package com.apixio.logger.tracks.app;

/**
 * Simple interface/class pair to test Tracks features.
 * All annotations are on class, not interface.
 * @author lance
 *
 */
public interface App {
    
    void methodTimer();
    
    void methodConstants();

    void methodLogged(String name, String value);

    void methodMDC(String name, String value);

    void methodStatus(boolean fail) throws Exception;

    void methodParams(String abc, String def, String ghi);

    void methodMetered();

    void methodTimed();
    
    void methodExceptionMetered(boolean throwIt) throws Exception;
    
    void methodName();

}
