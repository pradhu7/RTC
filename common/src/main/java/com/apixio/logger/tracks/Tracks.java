package com.apixio.logger.tracks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method annotation for the Tracks regime.
 * All calls to this method are done through the TrackedProxy filter.
 * <br/>
 * prefix: each log in this method starts with this prefix
 * <br/>
 * message: the EventLogger.event() call includes this as the 'message' parameter
 * <br/>
 * status: a status is logged with this name. The value is 'success' or 'error'
 * based on whether the method throws an exception (Throwable). The exception
 * is rethrown.
 * <br/>
 * timer: a log is made of the # of milliseconds taken by this method.
 * 
 * @author lance
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Tracks {
    String[] value() default {};
    String prefix() default "";
    String message() default "";
    String status() default "";
    String timer() default "";
}
