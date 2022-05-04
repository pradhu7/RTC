package com.apixio.sdk;

import java.util.List;

import com.apixio.sdk.protos.FxProtos.FxType;

/**
 * An accessor provides a way to get data (untyped at this level) from something at runtime, where
 * that something could be almost anything--kafka request info, configuration, an APO, a set of
 * signal data, etc.  Each accessor is identified by a unique name/ID and is implemented at most once
 * per execution environment, although there could be many instances of that implementation class
 * at runtime (as compared to f(x) implementations that have only 1 instance.  It should be
 * considered as a mechanism for centralizing reusable transformation code that can provide data to
 * f(x) implementations.  Certainly a client of some f(x) can manage the supplying of correctly
 * structured data to f(x), but using predefined and SDK-supported accessors is recommended.
 *
 * An accessor is functionally equivalent to an FxImpl but the implementation of the various
 * accessors is bound into the container's set of classes available.
 *
 * The invocation of an accessor is done by calling its eval() function (which is equivalent to
 * invoking via reflection a method that implements an f(x)).  The list of argument values for
 * evaluation is untyped so the implementations of accessors must obviously take care to check
 * correctness (arg count and types)
 *
 * Note that at execution container runtime, it's possible to have a tree of accessors that
 * describes how to fetch the data.  For example, the following can be declared as the way to get
 * data for an AutoAccept f(x) invocation:
 *
 *  list[signal] autoAccept(Request("patientXUUID"), list[AuditorEvent(Request("projectXUUID"), Request("logicalIDs"))])
 *
 * where both Request and AuditorEvent are accessors.  In this example, the output type of
 * AuditorEvent accessor must have been declared so that it has the same structure as what the
 * autoAccept f(x) accepts; this declaration would use structs, primitives, etc.
 */
public interface Accessor extends Plugin
{
    /**
     * Just in case the Accessor needs contextual info.
     *
     * FxType is set for all invocations for eval() and reflects the type of the
     * f(x,y,z) parameter based on the the actual positional parameter.  For example
     * if the function declaration is f(ObjType1, ObjType2) and the accessor is being
     * used during the evaluation of the argument for the ObjType1 parameter, then
     * FxType will be set to ObjTyp1, regardless of how deep the accessor nesting is
     * for evaluating that argument.  This allows an inner/nested accessor to make
     * use of the final parameter type is (which can be useful for restoring a set
     * of persisted data and then merging the lists).
     */
    public static class AccessorContext
    {
        private FxType    fxType;
        private FxRequest fxRequest;

        public FxType getFxType()
        {
            return fxType;
        }

        public FxRequest getFxRequest()
        {
            return fxRequest;
        }

        public AccessorContext(FxType fxType, FxRequest fxRequest)
        {
            this.fxType    = fxType;
            this.fxRequest = fxRequest;
        }
    }

    /**
     * Everything in params must have been (recursively) evaluated already.  Any use of FxEnvironment
     * by the implementer requires that it keep track of FxEnvironment from setEnvironment
     */
    public Object eval(AccessorContext context, List<Object> args) throws Exception;

}
