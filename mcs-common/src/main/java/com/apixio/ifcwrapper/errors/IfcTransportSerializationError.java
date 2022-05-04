package com.apixio.ifcwrapper.errors;

import java.io.IOException;

public class IfcTransportSerializationError extends IOException {

    public IfcTransportSerializationError(String message) {
        super(message);
    }

    public IfcTransportSerializationError(String message, Throwable t) {
        super(message, t);
    }
}
