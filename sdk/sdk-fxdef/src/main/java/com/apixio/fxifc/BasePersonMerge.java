package com.apixio.fxifc;

import java.util.List;
import java.util.Map;

public abstract class BasePersonMerge extends BaseFxImplementation implements PersonMerge
{

    @Override
    public List<Person> merge(List<Person> persons) throws Exception
    {
        throw new RuntimeException("Subclass must implement");
    }

    @Override
    public void setAssets(Map<String,String> assets)
    {
    }

}
