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

public class PageWindowSourceWrapper
    extends AbstractSourceWrapper<Signals.PageWindowSource> implements PageWindowSource {

    public PageWindowSourceWrapper(Signals.PageWindowSource proto) {
        super(proto);
    }

    @Override
    public int getCentroid() {
        return proto.getCentroid();
    }

    @Override
    public int getStartPage() {
        return proto.getStartPage();
    }

    @Override
    public int getEndPage() {
        return proto.getEndPage();
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
        if (that instanceof PatientSource) {
            return getPatientId().equals(((PatientSource) that).getPatientId());
        }
        if (that instanceof PageSource) {
            if (!getDocumentId().equals(((PageSource) that).getDocumentId())) {
                return false;
            }
            /*
            final int thatPage = ((PageSource) that).getPage();
            if (thatPage < getStartPage() || thatPage > getEndPage()) {
                return false;
            }
            */
            return true;
        }
        if (that instanceof PageWindowSource) {
            if (!getDocumentId().equals(((PageSource) that).getDocumentId())) {
                return false;
            }
            final int thatEndPage = ((PageWindowSource) that).getEndPage();
            final int thatStartPage = ((PageWindowSource) that).getStartPage();
            // If the start or end page of either source is inside the page window for the other,
            // they overlap.
            if (thatEndPage <= getEndPage() && thatEndPage >= getStartPage()) {
                return true;
            }
            if (thatStartPage <= getEndPage() && thatStartPage >= getStartPage()) {
                return true;
            }
            if (getEndPage() <= thatEndPage && getEndPage() >= thatStartPage) {
                return true;
            }
            if (getStartPage() <= thatEndPage && getStartPage() >= thatStartPage) {
                return true;
            }
            return true;
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
        return SourceType.PAGE_WINDOW;
    }

    @Override
    public String toString() {
        return String.format(
            "[PageWindow %s %s %d %d %d]",
            getDocumentId().toString(), getPatientId().toString(), getStartPage(), getCentroid(),
            getEndPage());
    }
}
