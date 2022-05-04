package com.apixio.fxifc;

// manually generated from extractFeature.idl

import java.util.List;
import java.util.Map;

import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.ifc.PageWindow;

public abstract class BaseExtractSignals extends BaseFxImplementation implements ExtractSignals
{

    protected Map<String,String> assets;

    @Override
    public List<Signal> extractSignals(PageWindow pw) throws Exception
    {
        throw new RuntimeException("Subclass must implement");
    }

    @Override
    public void setup() throws Exception  //!! TODO this signature must eventually include environment and other f(x)-specific setup (as defined by f(x) needs)
    {
        logger.debug("BaseExtractFeature.setup called");
    }

    @Override
    public void setAssets(Map<String,String> assets)
    {
        this.assets = assets;

        logger.debug("BaseExtractFeature.setAssets(" + assets + ")");
    }


}
