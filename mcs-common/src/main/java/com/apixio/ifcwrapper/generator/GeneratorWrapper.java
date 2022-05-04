package com.apixio.ifcwrapper.generator;

import com.apixio.ensemble.ifc.Generator;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ifcwrapper.SignalMarshal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = GeneratorWrapperDeserializer.class)
public class GeneratorWrapper implements Generator, Comparable {

    private final Signals.Generator proto;

    public GeneratorWrapper(Signals.Generator generatorProto) {
        this.proto = generatorProto;
    }

    @JsonIgnore
    public Signals.Generator getProto() {
        return proto;
    }

    @Override
    @JsonProperty("name")
    public String getName() {
        return proto.getName();
    }

    @Override
    @JsonProperty("version")
    public String getVersion() {
        return proto.getVersion();
    }

    @Override
    @JsonProperty("className")
    @Nullable
    public String className() {
        return proto.getClassName();
    }

    @Override
    @Nullable
    @JsonProperty("jarVersion")
    public String jarVersion() {
        return proto.getJarVersion();
    }

    @Override
    @Nullable
    @JsonProperty("modelVersion")
    public String modelVersion() {
        return proto.getModelVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GeneratorWrapper) {
            return proto.equals(((GeneratorWrapper) o).getProto());
        }
        if (o instanceof Generator) {
            return proto.equals(SignalMarshal.wrapGenerator((Generator) o).getProto());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getProto().hashCode();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof GeneratorWrapper) {
            GeneratorWrapper other = (GeneratorWrapper) o;
            if (!getName().equals(other.getName())) {
                return getName().compareTo(other.getName());
            }
            if (className() != null
                && other.className() != null
                && !className().equals(other.className())) {
                return className().compareTo(other.className());
            }
            if (jarVersion() != null
                && other.jarVersion() != null
                && !jarVersion().equals(other.jarVersion())) {
                return jarVersion().compareTo(other.jarVersion());
            }
            if (getVersion() != null
                && other.getVersion() != null
                && !getVersion().equals(other.getVersion())) {
                return getVersion().compareTo(other.getVersion());
            }
            if (modelVersion() != null
                && other.modelVersion() != null
                && !modelVersion().equals(other.modelVersion())) {
                return modelVersion().compareTo(other.modelVersion());
            }
            return 0;
        } else {
            return -1;
        }
    }
}
