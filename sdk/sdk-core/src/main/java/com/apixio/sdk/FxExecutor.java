package com.apixio.sdk;

import java.net.URI;
import java.util.List;

/**
 * Definition of what it is to be an execution environment for f(x) implementations.
 */
public interface FxExecutor
{

    /**
     * Sets up the executor.  Loading more than one f(x)impl is allowed if they're all in the same
     * jar, and the full set of loaded f(x)impls is returned.
     */
    public List<FxInvokable> initialize(EccInit init) throws Exception;

    /**
     * Return the invokables that can be used to evaluate f(x) for a given request.  This is the
     * same list that is returned from initialize()
     */
    public List<FxInvokable> getFxs();

    /**
     * Invoke f(x) implementation with the given request.  The client must decide if the
     * output is to be persisted or not.
     */
    public Object  invokeFx(FxInvokable invokable, FxRequest request) throws Exception;
    public boolean invokeFx(FxInvokable invokable, FxRequest request, ReturnedValueHandler rvh) throws Exception;

    /**
     * Save/restore output of f(x).  Currently only lists of non-primitives can be persisted.
     */
    public URI persistOutput(FxInvokable invokable, FxRequest request, Object o) throws Exception;
    public List<Object> restoreOutput(URI dataURI, String datatype) throws Exception;
    
}
