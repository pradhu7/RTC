package com.apixio.sdk;


import com.apixio.sdk.protos.FxProtos.FxDef;
import com.apixio.sdk.protos.FxProtos.FxImpl;
import com.apixio.sdk.protos.FxProtos.FxType;

import java.util.List;

import com.apixio.sdk.metric.Timer;

/**
 * FxInvokable captures all the reflection-level details needed to actually invoke the
 * dynamically discovered f(x)impl method.
 */
public abstract class FxInvokable<T>
{
    protected FxLogger logger;
    protected FxImpl   fxImpl;       // just to keep it around
    protected FxDef   fxDef;       

    public FxInvokable(FxLogger logger, FxImpl impl)
    {
        this.logger       = logger;
        this.fxImpl       = impl;
        this.fxDef        = impl.getFxDef();
    }

    public FxImpl getFxImpl()
    {
        return fxImpl;
    }
    public FxDef getFxDef()
    {
        return fxDef;
    }
    /**
     *
     */
    public abstract void callSetup(FxEnvironment env) throws Exception;

    /**
     *
     */
    public Object invoke(Object[] args) throws Exception
    {
        String curComponent   = logger.getCurrentComponentName();
        String id             = fxImpl.getEntryName();
        List<FxType> argTypes = fxImpl.getFxDef().getParametersList();
        Timer  dur            = Timer.newTimer("invoke");
        long   start;
        Object rv;

        logger.debug("Calling invoke() on %s", id);

        logger.setCurrentComponentName(id);

        start = System.currentTimeMillis();

        try
        {
            dur.start();

            rv = invokeInner(argTypes, args);

            // log timer with fximpl component name as it's likely people will want to query
            // on that rather than on infrastructure-named metrics
            dur.stop();
            logger.metric(dur);

            logger.setCurrentComponentName(curComponent);
            logger.debug("Success on execution of %s", id);
        }
        catch (Throwable t)
        {
            logger.setCurrentComponentName(curComponent);
            logger.error("Error during execution of %s", t, id);
            throw new RuntimeException("Error during execution", t);
        }
        finally
        {
            //!! finish & log stats
            logger.info("Duration for invoke() of %s: %d millis", id, (System.currentTimeMillis() - start));
        }

        return rv;
    }

    public abstract Object invokeInner(List<FxType> argTypes, Object[] args) throws Exception;

    public abstract Class<?>[] getParameterTypes();

}
