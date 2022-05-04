package com.apixio.converter;

import java.io.IOException;

import com.google.protobuf.Message;

import com.apixio.sdk.Converter;
import com.apixio.sdk.FxEnvironment;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.ifcwrapper.SignalMarshal;
import com.apixio.ifcwrapper.signal.SignalDataWrapper;    // implementation of ifc.Signal
import com.apixio.ensemble.ifc.transport.Signals;         // protobuf

public class SignalConverter implements Converter
{
    @Override
    public Meta getMetadata()
    {
        return new Meta(Signal.class, Signals.Signal.class, false);
    }

    @Override
    public void setEnvironment(FxEnvironment env)
    {
    }

    @Override
    public String getID()
    {
        return "signalConverter";
    }

    /**
     * Convert from interface to protobuf.  No exceptions should be thrown.
     */
    @Override
    public Message convertToProtobuf(Object o)
    {
        Signal signal = (Signal) o;

        try
        {
            return SignalMarshal.fromIfc(signal).getProto();
        }
        catch (IOException x)
        {
            throw new RuntimeException("Failed to convert ifc.Signal to protobuf", x);
        }
    }

    /**
     * Convert from protobuf to interface.  No exceptions should be thrown.
     */
    @Override
    public Object convertToInterface(Message signal)
    {
        return new SignalDataWrapper((Signals.Signal) signal);
    }

}
