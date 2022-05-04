package com.apixio.sdk;

/**
 * A Component is something that interacts with the runtime SDK environment.
 */
public interface FxComponent
{
    /**
     * As a component interacts with the environment we need to set the environment
     * for it to interact with.
     */
    public void setEnvironment(FxEnvironment env) throws Exception;

}
