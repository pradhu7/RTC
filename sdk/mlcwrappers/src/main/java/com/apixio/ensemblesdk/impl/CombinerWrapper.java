package com.apixio.ensemblesdk.impl;

import java.util.List;

import com.apixio.model.event.EventType;
import com.apixio.ensemble.ifc.ManagedLifecycle;
import com.apixio.ensemble.ifc.SignalCombinationParams;
import com.apixio.ensemble.impl.common.CombinerBase;

/**
 * This classname must be specified as the value for "meta.entry" in the .publish file.
 *
 * The f(x) IDL for it is:
 *
 *   'list<apixio.Event>  combineSignals(apixio.SignalGroups)'
 *
 * and matches the params/return of CombinerBase
 *
 * This implementation can be the bridge for any ensemble class that extends
 * CombinerBase.
 *
 */
public class CombinerWrapper extends FxWrapperBase
{
    // the actual f(x) as implemented w/o the SDK and dynamically located/instantiated
    private CombinerBase fx;

    /**
     * Check type and hang onto the implementation.
     */
    protected void setFx(ManagedLifecycle mlc) throws Exception
    {
        if (!(mlc instanceof CombinerBase))
            throw new IllegalArgumentException("CombinerBase wraps only CombinerBase-derived functions");

        fx = (CombinerBase) mlc;
    }

    /**
     * This is the method that will be discovered via reflection. It wraps
     * the call to the wanna-be-MLC-loaded .process()
     */
    public List<EventType> combineSignals(SignalCombinationParams signals) throws Exception
    {
        return fx.process(signals);
    }

}
