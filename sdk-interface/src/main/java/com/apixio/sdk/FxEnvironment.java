package com.apixio.sdk;

/**
 * The FxEnvironment is the main concept for containing all the execution-related
 * entities in a single location.  There is exactly one instance of FxEnvironment
 * during runtime execution of an f(x) implementation.
 */
public interface FxEnvironment extends FxAttributes
{

    /**
     * Defined attributes.  All implementations of FxEnvironment MUST return a non-null
     * value from .getAttribute() with any of these defined attributes
     */
    public static final String DOMAIN = "apx.domain";

    /**
     * Main interface into logging, for both diagnostic/debug and operational/metrics
     * purposes.
     */
    public FxLogger getLogger();

}
