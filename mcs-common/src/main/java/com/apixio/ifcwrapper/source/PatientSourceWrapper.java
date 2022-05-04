package com.apixio.ifcwrapper.source;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.DocumentSource;
import com.apixio.ensemble.ifc.Location;
import com.apixio.ensemble.ifc.PageSource;
import com.apixio.ensemble.ifc.PageWindowSource;
import com.apixio.ensemble.ifc.PatientSource;
import com.apixio.ensemble.ifc.Source;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ifcwrapper.location.StringLocationWrapper;
import com.apixio.util.Protobuf;

public class PatientSourceWrapper
    extends AbstractSourceWrapper<Signals.PatientSource> implements PatientSource {

    public PatientSourceWrapper(Signals.PatientSource proto) {
        super(proto);
    }

    @Override
    public XUUID getPatientId() {
        return Protobuf.XUUIDfromProtoXUUID(proto.getPatientID());
    }

    @Override
    public Boolean overlapsWith(Source that) {
        if (that instanceof DocumentSource) {
            return getPatientId().equals(((DocumentSource) that).getPatientId());
        }
        if (that instanceof PageSource) {
            return getPatientId().equals(((PageSource) that).getPatientId());
        }
        if (that instanceof PageWindowSource) {
            return getPatientId().equals(((PageWindowSource) that).getPatientId());
        }
        if (that instanceof PatientSource) {
            return getPatientId().equals(((PatientSource) that).getPatientId());
        }
        return false;
    }

    @Override
    public Location getLocation() {
        if (proto.hasStringLocation()) {
            return new StringLocationWrapper(proto.getStringLocation());
        }
        return null;
    }

    public SourceType getSourceType() {
        return SourceType.PATIENT;
    }

    @Override
    public String toString() {
        return String.format("[PatientSource %s]", getPatientId().toString());
    }
}
