package com.apixio.genericecc;

import java.util.HashMap;
import java.util.Map;

import com.apixio.dao.utility.DaoServices;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxExecEnvironment;
import com.apixio.sdk.FxExecutor;
import com.apixio.sdk.FxLogger;

public class EccEnvironment implements FxExecEnvironment
{

    private DaoServices        daoServices;
    private FxLogger           logger;
    private FxExecutor         fxExecutor;
    private Map<String,String> config = new HashMap<>();

    // add ecc-specific stuff

    public EccEnvironment(DaoServices daoServices, FxLogger logger) throws Exception
    {
        this.daoServices = daoServices;
        this.logger      = logger;
    }

    public void setExecutor(FxExecutor exec)
    {
        this.fxExecutor = exec;
    }

    @Override
    public FxLogger getLogger()
    {
        return logger;
    }

    @Override
    public DaoServices getDaoServices()
    {
        return daoServices;
    }

    @Override
    public FxExecutor getFxExecutor()
    {
        return fxExecutor;
    }

    @Override
    public String getAttribute(String id)
    {
        if (id.equals(FxEnvironment.DOMAIN))
            return "devel";
        else
            return null;
    }

    @Override
    public Map<String,String> getAttributes()
    {
        return null;
    }

}

   
