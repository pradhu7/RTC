package com.apixio.ifcwrapper.errors;

public class SignalSerializationError extends IfcTransportSerializationError {

    public SignalSerializationError(String message) {
        super(message);
    }

    public SignalSerializationError(String message, Throwable t) {
        super(message, t);
    }
}
