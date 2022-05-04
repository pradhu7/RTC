package com.apixio.ensemblesdk.impl;

import java.util.List;

import com.apixio.ensemble.ifc.ManagedLifecycle;
import com.apixio.ensemble.ifc.PageWindow;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.impl.common.PageSignalGeneratorBase;

/**
 * This classname must be specified as the value for "meta.entry" in the .publish file.
 *
 * The f(x) IDL for it is:
 *
 *   'list<apixio.Signal> extractSignals(apixio.PageWindow)'
 *
 * and matches the params/return of PageSignalGeneratorBase.process()
 *
 * This implementation can be the bridge for any ensemble class that extends
 * PageSignalGeneratorBase.
 *
 */
public class PageSignalGeneratorWrapper extends FxWrapperBase
{
    // the actual f(x) as implemented w/o the SDK and dynamically located/instantiated
    private PageSignalGeneratorBase fx;

    /**
     * Check type and hang onto the implementation.
     */
    protected void setFx(ManagedLifecycle mlc) throws Exception
    {
        if (!(mlc instanceof PageSignalGeneratorBase))
            throw new IllegalArgumentException("PageSignalGeneratorWrapper wraps only PageSignalGeneratorBase-derived functions");

        fx = (PageSignalGeneratorBase) mlc;
    }

    /**
     * This is the method that will be discovered via reflection. It wraps
     * the call to the wanna-be-MLC-loaded .process()
     */
    public List<Signal> extractSignals(PageWindow pw) throws Exception
    {
        return fx.process(pw);
    }

}
