package com.apixio.ifcwrapper.source;

import com.apixio.ensemble.ifc.Source;
import com.apixio.ifcwrapper.SourceMarshal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.protobuf.MessageOrBuilder;
import org.jetbrains.annotations.NotNull;

@JsonDeserialize(using = AbstractSourceDeserializer.class)
public abstract class AbstractSourceWrapper<T extends MessageOrBuilder> implements Source {

    protected final T proto;

    protected AbstractSourceWrapper(T proto) {
        this.proto = proto;
    }

    @JsonIgnore
    public T getProto() {
        return proto;
    }

    @Override
    public int compareTo(@NotNull Source o) {
        int code = this.hashCode();
        int code2 = o.hashCode();
        if (code == code2) {
            return 0;
        }
        if (code > code2) {
            return 1;
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return proto.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AbstractSourceWrapper) {
            return proto.equals(((AbstractSourceWrapper) o).getProto());
        }
        if (o instanceof Source) {
            try {
                return proto.equals(SourceMarshal.wrapSource((Source) o).getProto());
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public abstract SourceType getSourceType();
}
