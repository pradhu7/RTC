package com.apixio.accessors;

import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxExecEnvironment;
import com.apixio.sdk.FxLogger;

/**
 * Simple base class usable by Accessor implementations that provides access to FxLogger.  Note
 * that because the ECC bootup does a Class.forName().newInstance() the actual classes must
 * have a default constructor so there's little use for a constructor here that captures the
 * ID of the accessor.
 */
public abstract class BaseAccessor implements Accessor
{
    protected FxLogger          logger;
    protected FxExecEnvironment fxEnv;

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        if (!(env instanceof FxExecEnvironment))
            throw new IllegalStateException("FxEnvironment argument must be of class FxExecEnvironment:  " + env.getClass());

        fxEnv  = (FxExecEnvironment) env;
        logger = env.getLogger();
    }

}
