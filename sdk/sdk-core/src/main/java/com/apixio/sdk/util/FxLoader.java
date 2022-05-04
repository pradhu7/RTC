package com.apixio.sdk.util;

import com.google.protobuf.util.JsonFormat;

import com.apixio.sdk.protos.FxProtos.FxDef;
import com.apixio.sdk.protos.FxProtos.FxImpl;

/**
 * Utility class to load serialized FxDef and FxImpl info.
 */
public class FxLoader
{
    private JsonFormat.Printer formatter = JsonFormat.printer(); //.omittingInsignificantWhitespace();

    /**
     *
     */
    public static FxDef loadFxDef(byte[] serialized) throws Exception
    {
        return FxDef.parseFrom(serialized);
    }

    /**
     *
     */
    public static FxImpl loadFxImpl(byte[] serialized) throws Exception
    {
        return FxImpl.parseFrom(serialized);
    }

}
