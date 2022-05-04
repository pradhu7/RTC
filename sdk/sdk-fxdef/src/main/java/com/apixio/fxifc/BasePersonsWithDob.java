package com.apixio.fxifc;

import java.util.List;
import java.util.Map;

import com.apixio.sdk.FxEnvironment;

public abstract class BasePersonsWithDob extends BaseFxImplementation implements PersonsWithDob
{

    protected Map<String,String> assets;

    @Override
    public List<Person> personsWithDob(Integer y, Integer m, Integer d) throws Exception
    {
        throw new RuntimeException("Subclass must implement");
    }

    @Override
    public void setEnvironment(FxEnvironment env)
    {
        super.setEnvironment(env);
    }

    @Override
    public void setAssets(Map<String,String> assets)
    {
        this.assets = assets;

        logger.info("BasePersonsWithDob.setAssets(%s)", assets);
    }

}
