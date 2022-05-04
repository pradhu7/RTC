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

public class PageSourceWrapper
    extends AbstractSourceWrapper<Signals.PageSource> implements PageSource {

    public PageSourceWrapper(Signals.PageSource proto) {
        super(proto);
    }

    @Override
    public int getPage() {
        return proto.getPage();
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
        if (that instanceof PageSource) {
            return getDocumentId().equals(((PageSource) that).getDocumentId())
                && getPage() == ((PageSource) that).getPage();
        }
        if (that instanceof PageWindowSource) {
            return getDocumentId().equals(((PageWindowSource) that).getDocumentId());
        }
        if (that instanceof DocumentSource) {
            return getDocumentId().equals(((DocumentSource) that).getDocumentId());
        }
        if (that instanceof PatientSource) {
            return getPatientId().equals(((PatientSource) that).getPatientId());
        }
        /*
        if (that instanceof PageWindowSource) {
            return getDocumentId().equals(((PageWindowSource) that).getDocumentId())
                && getPage() <= ((PageWindowSource) that).getEndPage()
                && getPage() >= ((PageWindowSource) that).getStartPage();
        }
        */
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
        return SourceType.PAGE;
    }

    @Override
    public String toString() {
        return String.format("[PageSource %s %s %d]", getDocumentId(), getPatientId(), getPage());
    }
}
