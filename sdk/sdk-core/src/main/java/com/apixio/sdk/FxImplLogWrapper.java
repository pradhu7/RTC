package com.apixio.sdk;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Wraps an actual FxImplementation in order to change the logging component name for each
 * method call made to the implementation.
 */
public class FxImplLogWrapper implements FxImplementation
{

    private FxImplementation real;
    private String           compName;
    private FxLogger         logger;

    /**
     * Wrap impl so that componentName is logged for all calls from impl code to the logger.
     */
    public FxImplLogWrapper(FxImplementation impl, String componentName)
    {
        this.real     = impl;
        this.compName = componentName;
    }

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        this.logger = env.getLogger();

        wrapVoidCall(e -> {
                try
                {
                    real.setEnvironment(e);
                }
                catch (Exception x)
                {
                    throw new RuntimeException("Failure to set f(x) impl environment", x);
                }
            },
            env);
    }

    @Override
    public void setAssets(Map<String,String> assets) throws Exception
    {
        wrapVoidCall(a -> {
                try
                {
                    real.setAssets(a);
                }
                catch (Exception x)
                {
                    throw new RuntimeException("Failure to set f(x) impl assets", x);
                }
            },
            assets);
    }

    @Override
    public void setImplementationInfo(ImplementationInfo info)
    {
        wrapVoidCall(i -> real.setImplementationInfo(i), info);
    }

    /**
     * Template/wrapper for temporarily swapping logger component name
     */
    private <T> void wrapVoidCall(Consumer<T> fn, T arg)
    {
        String curComponent = logger.getCurrentComponentName();

        try
        {
            logger.setCurrentComponentName(compName);
            fn.accept(arg);
        }
        catch (Exception x)
        {
            throw new RuntimeException("Failed in log-wrapped call", x);
        }
        finally
        {
            logger.setCurrentComponentName(curComponent);
        }

    }

}
