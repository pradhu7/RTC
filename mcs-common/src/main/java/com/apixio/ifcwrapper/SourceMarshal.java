package com.apixio.ifcwrapper;

import com.apixio.ensemble.ifc.DocumentSource;
import com.apixio.ensemble.ifc.PageSource;
import com.apixio.ensemble.ifc.PageWindowSource;
import com.apixio.ensemble.ifc.PatientSource;
import com.apixio.ensemble.ifc.Source;
import com.apixio.ensemble.ifc.StringLocation;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ifcwrapper.errors.IfcTransportSerializationError;
import com.apixio.ifcwrapper.source.AbstractSourceWrapper;
import com.apixio.ifcwrapper.source.DocumentSourceWrapper;
import com.apixio.ifcwrapper.source.PageSourceWrapper;
import com.apixio.ifcwrapper.source.PageWindowSourceWrapper;
import com.apixio.ifcwrapper.source.PatientSourceWrapper;
import com.apixio.ifcwrapper.source.SourceType;
import com.apixio.ifcwrapper.util.JsonUtil;
import com.apixio.util.Protobuf;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceMarshal {

    private static final Logger LOG = LoggerFactory.getLogger(SourceMarshal.class);

    public static AbstractSourceWrapper wrapSource(Source source)
        throws IfcTransportSerializationError {
        return wrapSource(source, false);
    }

    @VisibleForTesting
    static AbstractSourceWrapper wrapSource(Source source, boolean forcewrap)
        throws IfcTransportSerializationError {
        if (!forcewrap && (source instanceof AbstractSourceWrapper)) {
            return (AbstractSourceWrapper) source;
        }

        switch (getSourceType(source)) {
            case PAGE:
                return new PageSourceWrapper(fromIfcPageSource((PageSource) source));
            case PATIENT:
                return new PatientSourceWrapper(fromIfcPatientSource((PatientSource) source));
            case DOCUMENT:
                return new DocumentSourceWrapper(fromIfcDocumentSource((DocumentSource) source));
            case PAGE_WINDOW:
                return new PageWindowSourceWrapper(
                    fromIfcPageWindowSource((PageWindowSource) source));
            default:
                LOG.error("Error serializing ifc.Source : " + JsonUtil.writeForLog(source));
                throw new IfcTransportSerializationError("Could not determine source type");
        }
    }

    static SourceType getSourceType(Source source) {
        if (source instanceof PageSource) {
            return SourceType.PAGE;
        }
        if (source instanceof PageWindowSource) {
            return SourceType.PAGE_WINDOW;
        }
        if (source instanceof PatientSource) {
            return SourceType.PATIENT;
        }
        if (source instanceof DocumentSource) {
            return SourceType.DOCUMENT;
        }
        return null;
    }

    static Signals.PageSource fromIfcPageSource(PageSource source) {
        Signals.PageSource.Builder builder = Signals.PageSource.newBuilder()
            .setPage(source.getPage())
            .setDocumentID(Protobuf.XUUIDtoProtoXUUID(source.getDocumentId()))
            .setPatientID(Protobuf.XUUIDtoProtoXUUID(source.getPatientId()));

        Signals.StringLocation location = getLocationProto(source);
        if (location != null) {
            builder.setStringLocation(location);
        }
        return builder.build();
    }

    static Signals.PatientSource fromIfcPatientSource(PatientSource source) {
        Signals.PatientSource.Builder builder = Signals.PatientSource.newBuilder()
            .setPatientID(Protobuf.XUUIDtoProtoXUUID(source.getPatientId()));

        Signals.StringLocation location = getLocationProto(source);
        if (location != null) {
            builder.setStringLocation(location);
        }
        return builder.build();
    }

    static Signals.DocumentSource fromIfcDocumentSource(DocumentSource source) {
        Signals.DocumentSource.Builder builder = Signals.DocumentSource.newBuilder()
            .setPatientID(Protobuf.XUUIDtoProtoXUUID(source.getPatientId()))
            .setDocumentID(Protobuf.XUUIDtoProtoXUUID(source.getDocumentId()))
            .setNumPages(source.getNumPages());

        Signals.StringLocation location = getLocationProto(source);
        if (location != null) {
            builder.setStringLocation(location);
        }
        return builder.build();
    }

    static Signals.PageWindowSource fromIfcPageWindowSource(PageWindowSource source) {
        Signals.PageWindowSource.Builder builder = Signals.PageWindowSource.newBuilder()
            .setPatientID(Protobuf.XUUIDtoProtoXUUID(source.getPatientId()))
            .setDocumentID(Protobuf.XUUIDtoProtoXUUID(source.getDocumentId()))
            .setStartPage(source.getStartPage())
            .setEndPage(source.getEndPage())
            .setCentroid(source.getCentroid());

        Signals.StringLocation location = getLocationProto(source);
        if (location != null) {
            builder.setStringLocation(location);
        }
        return builder.build();
    }

    static Signals.StringLocation getLocationProto(Source source) {
        if (source.getLocation() != null && source.getLocation() instanceof StringLocation) {
            String descriptor = ((StringLocation) source.getLocation()).getDescriptor();
            return Signals.StringLocation.newBuilder().setLocDescriptor(descriptor).build();
        }
        return null;
    }
}
