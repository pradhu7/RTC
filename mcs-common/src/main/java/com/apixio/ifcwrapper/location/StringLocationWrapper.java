package com.apixio.ifcwrapper.location;

import com.apixio.ensemble.ifc.StringLocation;

public class StringLocationWrapper implements StringLocation {

    public static final StringLocationWrapper DEFAULT = new StringLocationWrapper();

    private final String descriptor;

    public StringLocationWrapper() {
        this.descriptor = "default";
    }

    public StringLocationWrapper(String descriptor) {
        this.descriptor = descriptor;
    }

    public StringLocationWrapper(com.apixio.ensemble.ifc.transport.Signals.StringLocation proto) {
        this.descriptor = proto.getLocDescriptor();
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    public com.apixio.ensemble.ifc.transport.Signals.StringLocation toProto() {
        return com.apixio.ensemble.ifc.transport.Signals.StringLocation.newBuilder()
            .setLocDescriptor(descriptor)
            .build();
    }

    public String toString() { return descriptor; }
}
