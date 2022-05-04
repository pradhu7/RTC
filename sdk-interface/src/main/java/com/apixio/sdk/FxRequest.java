package com.apixio.sdk;

/**
 * An FxRequest instance represents a request from some outside the runtime system to
 * execute the configured f(x) implementation.  A request contains execution-specific
 * information/parameters that are retrieved via accessors and transformed into the
 * actual f(x) arguments.
 */
public interface FxRequest extends FxAttributes
{
    /**
     * Defined attributes
     */
    public static final String REQUEST_ID = "apx.requestid";   // supplied by ECC for debug/tracking

    /**
     * !! TODO find a way to get rid of this.  Currently used to set algorithm string which
     * is used when creating UmCreator (to form a data URI that has, effectively, the
     * MCID/algoid in the URI for uniqueness)
     */
    public void setAttribute(String id, String val);
}
