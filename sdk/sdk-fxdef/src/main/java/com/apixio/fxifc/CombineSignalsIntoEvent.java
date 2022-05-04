package com.apixio.fxifc;

// manually generated

import java.util.List;

import com.apixio.model.event.EventType;
import com.apixio.ensemble.ifc.SignalCombinationParams;

public interface CombineSignalsIntoEvent
{
    public List<EventType> combineSignals(SignalCombinationParams p) throws Exception;
    public void setup() throws Exception;
}
