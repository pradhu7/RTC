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

public class DocumentSourceWrapper
    extends AbstractSourceWrapper<Signals.DocumentSource> implements DocumentSource {

    public DocumentSourceWrapper(Signals.DocumentSource proto) {
        super(proto);
    }

    @Override
    public int getNumPages() {
        return proto.getNumPages();
    }

    @Override
    public XUUID getDocumentId() {
        return Protobuf.XUUIDfromProtoXUUID(proto.getDocumentID());
    }

    @Override
    public XUUID getPatientId() {
        return Protobuf.XUUIDfromProtoXUUID(proto.getPatientID());
    }

    @Override
    public Boolean overlapsWith(Source that) {
        if (that instanceof DocumentSource) {
            return getDocumentId().equals(((DocumentSource) that).getDocumentId());
        }
        if (that instanceof PageSource) {
            return getDocumentId().equals(((PageSource) that).getDocumentId());
        }
        if (that instanceof PageWindowSource) {
            return getDocumentId().equals(((PageWindowSource) that).getDocumentId());
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
        return SourceType.DOCUMENT;
    }

    @Override
    public String toString() {
        return String.format(
            "[DocumentSource %s %s]", getDocumentId().toString(), getPatientId().toString());
    }
}
