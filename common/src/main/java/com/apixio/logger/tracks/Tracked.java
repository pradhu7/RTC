package com.apixio.logger.tracks;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class annotation for the Tracked regime.
 * Add this annotation to a class and transform an instance with LoggedProxy.
 * All method calls to that instance can log a set of key-value pairs. These
 * pairs are declared in method annotations and in objects inside the methods.
 * 
 * @author lance
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Tracked {

    /**
     * This class is a base "frame" of log4j MDC key-value pairs.
     * Every key-value pair added to MDC in these methods and called methods 
     * will be removed when this method returns.
     */
    boolean isFrame() default true;
    /**
     * Clear MDC when returning from a method
     */
    boolean clearMDC() default false;
    /**
     * Call EventLogger log.event() with all key-value pairs on method exit
     */
    boolean doLog() default true;
    /**
     * Add standard Apixio fields
     */
    boolean standard() default true;
}
