package com.apixio.logger.tracks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use on method parameter in a Tracked class.
 * 
 * At call time, save name/value pair of the name from this
 * and the value when called. This example creates a logged name/value pair
 * "funky.monkey", "name of a monkey" when you call it.
 * <br/>
 * void funky(@Track("funky.animal") String monkey)
 * <br/>
 * 
 * @author lance
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface TrackParam {
    String value();
}
