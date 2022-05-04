package com.apixio.converter;

import com.apixio.sdk.Converter;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.protos.BaseTypesProtos.FxString;

public class StringConverter implements Converter<String,FxString>
{
    @Override
    public Meta getMetadata()
    {
        return new Meta(String.class, FxString.class, false);
    }

    @Override
    public void setEnvironment(FxEnvironment env)
    {
    }

    @Override
    public String getID()
    {
        return "stringConverter";
    }

    /**
     * Convert from interface to protobuf.  No exceptions should be thrown.
     */
    @Override
    public FxString convertToProtobuf(String strval)
    {
        FxString fxs = FxString.newBuilder().setStringValue(strval).build();
        return fxs;
    }

    /**
     * Convert from protobuf to interface.  No exceptions should be thrown.
     */
    @Override
    public String convertToInterface(FxString fxs)
    {
        return new String(fxs.getStringValue());
    }

}
