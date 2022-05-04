package com.apixio.sdk.util;

import com.google.protobuf.util.JsonFormat;

import com.apixio.sdk.protos.EvalProtos.ArgList;

/**
 * Utility class to load serialized objects in EvalProtos
 */
public class EvalLoader
{
    private JsonFormat.Printer formatter = JsonFormat.printer(); //.omittingInsignificantWhitespace();

    /**
     *
     */
    public static ArgList loadArgList(byte[] serialized) throws Exception
    {
        return ArgList.parseFrom(serialized);
    }

}
