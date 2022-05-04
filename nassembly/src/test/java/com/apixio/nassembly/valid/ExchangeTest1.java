package com.apixio.nassembly.valid;

import com.apixio.model.nassembly.Exchange;
import com.google.protobuf.Descriptors;

import java.io.InputStream;

public class ExchangeTest1 implements Exchange
{
    @Override
    public String getDataTypeName()
    {
        return "impl1";
    }

    @Override
    public Descriptors.Descriptor getDescriptor()
    {
        return null;
    }

    @Override
    public String getCid()
    {
        return null;
    }

    @Override
    public void fromProto(Iterable<byte[]> protoBytes)
    {

    }

    @Override
    public void fromProtoStream(Iterable<InputStream> inputStreams)
    {

    }

    @Override
    public Iterable<ProtoEnvelop> getProtoEnvelops()
    {
        return null;
    }
}
