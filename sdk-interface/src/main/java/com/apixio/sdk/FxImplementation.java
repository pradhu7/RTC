package com.apixio.sdk;

import java.util.Map;

/**
 * This is the base interface for implementations of some arbitrary f(x).  All implementations
 * are components within the runtime system and allow some set of assets to be used.
 */
public interface FxImplementation extends FxComponent
{
    /**
     * Useful information about the FxImpl that the implementation code
     * can use.
     */
    public static class ImplementationInfo
    {
        String packageId;   // mcid
    }

    /**
     * Assets are set after system initialization is done but before the first
     * execution of an implementation
     */
    public void setAssets(Map<String,String> assets) throws Exception;

    public void setImplementationInfo(ImplementationInfo info);

}
