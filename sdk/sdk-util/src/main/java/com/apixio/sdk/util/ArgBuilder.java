package com.apixio.sdk.util;

import java.util.Arrays;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.util.JsonFormat;

import com.apixio.sdk.protos.EvalProtos.AccessorCall;
import com.apixio.sdk.protos.EvalProtos.Arg;
import com.apixio.sdk.protos.EvalProtos.ArgList;
import com.apixio.sdk.protos.EvalProtos.Value;

/**
 * Utility class to help build protobuf POJOs for (manually "parsed") Args that can be persisted and
 * restored.
 *
 * Use of this class can be confusing as it requires that the client code pretend it's parsing some f(x)
 * definition, building up the elements of it along the way.  It is also currently intentionally limited
 * to having non-accessor arg values be just scalars--no lists or maps (etc) can be passed as an arg
 */
public class ArgBuilder
{
    private JsonFormat.Printer formatter = JsonFormat.printer(); //.omittingInsignificantWhitespace();

    /**
     *
     */
    public static ArgList makeArgList(Arg... args)
    {
        return makeArgList(Arrays.asList(args));
    }
    public static ArgList makeArgList(List<Arg> args)
    {
        ArgList.Builder builder = ArgList.newBuilder();

        for (Arg arg : args)
            builder.addArgs(arg);

        return builder.build();
    }

    public static Arg makeAccessorCall(String name, ArgList args)
    {
        Arg.Builder          ab  = Arg.newBuilder();
        AccessorCall.Builder acb = AccessorCall.newBuilder();

        acb.setAccessorName(name);
        for (Arg arg : args.getArgsList())
            acb.addArgs(arg);

        ab.setIsConst(false);
        ab.setAccCall(acb.build());

        return ab.build();
    }

    /**
     * ConstValue factory
     */
    public static Arg makeIntValue(int value)
    {
        return makeConstArg(Value.newBuilder().setIntValue(value).build());
    }
    public static Arg makeLongValue(long value)
    {
        return makeConstArg(Value.newBuilder().setLongValue(value).build());
    }
    public static Arg makeFloatValue(float value)
    {
        return makeConstArg(Value.newBuilder().setFloatValue(value).build());
    }
    public static Arg makeDoubleValue(double value)
    {
        return makeConstArg(Value.newBuilder().setDoubleValue(value).build());
    }
    public static Arg makeBooleanValue(boolean value)
    {
        return makeConstArg(Value.newBuilder().setBooleanValue(value).build());
    }
    public static Arg makeStringValue(String value)
    {
        return makeConstArg(Value.newBuilder().setStringValue(value).build());
    }

    private static Arg makeConstArg(Value val)
    {
        Arg.Builder ab = Arg.newBuilder();

        ab.setConstValue(val);
        ab.setIsConst(true);

        return ab.build();
    }

}
