package com.apixio.ifcwrapper.errors;

public class DocumentSerializationError extends IfcTransportSerializationError {

    public DocumentSerializationError(String message) {
        super(message);
    }

    public DocumentSerializationError(String message, Throwable t) {
        super(message, t);
    }
}
