package com.apixio.ensemblesdk.impl;

import java.util.List;

import com.apixio.ensemble.ifc.ManagedLifecycle;
import com.apixio.model.patient.Patient;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.impl.common.PatientSignalGeneratorBase;

/**
 * This classname must be specified as the value for "meta.entry" in the .publish file.
 *
 * The f(x) IDL for it is:
 *
 *   'list<apixio.Signal> extractSignals(apixio.Patient)'
 *
 * and matches the params/return of PatientSignalGeneratorBase.process()
 *
 * This implementation can be the bridge for any ensemble class that extends
 * PatientSignalGeneratorBase.
 *
 */
public class PatientSignalGeneratorWrapper extends FxWrapperBase
{
    // the actual f(x) as implemented w/o the SDK and dynamically located/instantiated
    private PatientSignalGeneratorBase fx;

    /**
     * Check type and hang onto the implementation.
     */
    protected void setFx(ManagedLifecycle mlc) throws Exception
    {
        if (!(mlc instanceof PatientSignalGeneratorBase))
            throw new IllegalArgumentException("PatientSignalGeneratorWrapper wraps only PatientSignalGeneratorBase-derived functions");

        fx = (PatientSignalGeneratorBase) mlc;
    }

    /**
     * This is the method that will be discovered via reflection. It wraps
     * the call to the wanna-be-MLC-loaded .process()
     */
    public List<Signal> extractSignals(Patient patient) throws Exception
    {
        return fx.process(patient);
    }

}
