package com.apixio.binding;

import java.util.Arrays;
import java.util.List;

import com.apixio.sdk.LanguageBinding;

/**
 * This is the class that will be loaded by EccSystem initialization unless overridden by the
 * ECC bootup.  It should contain the translations for all Apixio-defined symbolic data types
 * so whenever a new data type is created, it needs to be this list of bindings.
 */
public class DefaultJavaBindings implements LanguageBinding
{
    /**
     * Default bindings should include all Apixio-defined f(x) definitions
     */
    @Override
    public List<Binding> getJavaBindings()
    {
        return Arrays.asList(new Binding[] {
                // the symbolic names (IDs) can be mapped to interfaces or protobuf classes
                new Binding("apixio.Signal",      "com.apixio.ensemble.ifc.Signal"),
                new Binding("apixio.SignalGroups","com.apixio.ensemble.ifc.SignalCombinationParams"),
                new Binding("apixio.Event",       "com.apixio.model.event.EventType"),
                new Binding("apixio.Patient",     "com.apixio.model.patient.Patient"),
                new Binding("apixio.String",      "java.lang.String"),
                new Binding("apixio.PageWindow",  "com.apixio.ensemble.ifc.PageWindow")
            });
    }

}

