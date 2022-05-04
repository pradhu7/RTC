package com.apixio.ifcwrapper.signal;

import com.apixio.ensemble.ifc.Generator;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.ifc.SignalType;
import com.apixio.ensemble.ifc.Source;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ifcwrapper.SignalMarshal;
import com.apixio.ifcwrapper.generator.GeneratorWrapper;
import com.apixio.ifcwrapper.source.DocumentSourceWrapper;
import com.apixio.ifcwrapper.source.PageSourceWrapper;
import com.apixio.ifcwrapper.source.PageWindowSourceWrapper;
import com.apixio.ifcwrapper.source.PatientSourceWrapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = LegacySignalDataDeserializer.class)
public class SignalDataWrapper implements Signal {

    private static final Logger LOG = LoggerFactory.getLogger(SignalDataWrapper.class);

    protected final Signals.Signal proto;

    public SignalDataWrapper(Signals.Signal proto) {
        this.proto = proto;
    }

    public Signals.Signal getProto() {
        return this.proto;
    }

    @JsonIgnore
    @Override
    public Generator getGenerator() {
        return new GeneratorWrapper(proto.getGenerator());
    }

    @JsonIgnore
    @Override
    public String getName() {
        return proto.getName();
    }

    @JsonIgnore
    @Override
    public Source getSource() {
        if (proto.hasDocumentSource()) {
            return new DocumentSourceWrapper(proto.getDocumentSource());
        }
        if (proto.hasPatientSource()) {
            return new PatientSourceWrapper(proto.getPatientSource());
        }
        if (proto.hasPageSource()) {
            return new PageSourceWrapper(proto.getPageSource());
        }
        if (proto.hasPageWindowSource()) {
            return new PageWindowSourceWrapper(proto.getPageWindowSource());
        }
        if (proto.hasMultiDocumentSource()) {
            throw new UnsupportedOperationException("Multidocument source not supported");
        }
        throw new IllegalStateException("Signal has unsupported source");
    }

    @JsonIgnore
    @Override
    public SignalType getType() {
        switch (proto.getSignalType()) {
            case NUMERIC:
                return SignalType.NUMERIC;
            case CATEGORY:
                return SignalType.CATEGORY;
        }
        throw new IllegalStateException(
            "Don't know how to handle signal type : " + proto.getSignalType());
    }

    @Override
    public Object getValue() {
        switch (proto.getSignalType()) {
            case NUMERIC:
                if (getType() == SignalType.NUMERIC && "NaN".equals(proto.getCategoryValue())) {
                    return Float.NaN;
                }
                return proto.getNumericValue();
            case CATEGORY:
                return proto.getCategoryValue();
        }
        throw new IllegalStateException(
            "Don't know how to get value for signal type : " + proto.getSignalType());
    }

    @Override
    public float getWeight() {
        return proto.getWeight();
    }

    @Override
    public int compareTo(Signal obj) {
        int c1 = this.hashCode();
        int c2 = obj.hashCode();
        if (c1 < c2) {
            return -1;
        }
        if (c1 > c2) {
            return 1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return proto.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SignalDataWrapper) {
            return proto.equals(((SignalDataWrapper) o).getProto());
        }
        if (o instanceof Signal) {
            try {
                return proto.equals(SignalMarshal.fromIfc((Signal) o).getProto());
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
