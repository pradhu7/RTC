package com.apixio.fxifc;

import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxImplementation;
import com.apixio.sdk.FxLogger;

public abstract class BaseFxImplementation implements FxImplementation
{

    protected ImplementationInfo implInfo;
    protected FxLogger           logger;

    @Override
    public void setEnvironment(FxEnvironment env)
    {
        logger = env.getLogger();
    }

    @Override
    public void setImplementationInfo(ImplementationInfo info)
    {
        this.implInfo = info;
    }

}
