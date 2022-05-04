package com.apixio.sdk;

/**
 * Handles values returned from an f(x) invocation, where the handler could be called
 * multiple times for a single invocation due to automatic enumeration of a list of
 * elements, passing each element to f(x) (as compared to passing in the full list)
 */
public interface ReturnedValueHandler
{
    /**
     * Called when one iteration of invoking f(x) has completed
     */
    public void handleResults(Object fxReturned);
}
