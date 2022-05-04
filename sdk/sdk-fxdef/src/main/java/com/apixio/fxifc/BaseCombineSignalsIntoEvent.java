package com.apixio.fxifc;

import com.apixio.ensemble.ifc.SignalCombinationParams;
import java.util.List;
import java.util.Map;

import com.apixio.model.event.EventType;

public abstract class BaseCombineSignalsIntoEvent extends BaseFxImplementation implements CombineSignalsIntoEvent
{

    protected Map<String,String> assets;

    @Override
    public List<EventType> combineSignals(SignalCombinationParams p) throws Exception
    {
        throw new RuntimeException("Subclass must implement");
    }

    @Override
    public void setup() throws Exception  //!! TODO this signature must eventually include environment and other f(x)-specific setup (as defined by f(x) needs)
    {
        logger.debug("Combinesignals.setup called");
    }

    @Override
    public void setAssets(Map<String,String> assets)
    {
        this.assets = assets;

        logger.debug("BaseExtractSignals.setAssets(" + assets + ")");
    }

}
