package com.apixio.sdk;

/**
 * A UmCreator is responsible for creating an instance of DataUriManager (the "um" part
 * means "UriManager") during bootup.  This indirection is required as the various
 * implementations of DataUriManager have to be initialized differently
 */
public interface UmCreator extends FxComponent
{
    /**
     * Create the DataUriManager to be used for saving values returned from
     * f(x).  The environment will have already been set via FxComponent.setEnvironment
     */
    public DataUriManager createUriManager() throws Exception;

}
