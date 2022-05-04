package com.apixio.ifcwrapper.page;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.DocCacheElement;
import com.apixio.ensemble.ifc.transport.Documents.DocCacheElementProto;
import com.apixio.ifcwrapper.DocumentMarshal;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

public class DocCacheElementWrapper implements DocCacheElement, Comparable<DocCacheElement> {

    private final DocCacheElementProto proto;

    public DocCacheElementWrapper(DocCacheElementProto proto) {
        this.proto = proto;
    }

    public DocCacheElementProto getProto() {
        return this.proto;
    }

    @Override
    public XUUID getPatientId() {
        String str = proto.getPatientUUID().startsWith("PAT_")
            ? proto.getPatientUUID() : "PAT_" + proto.getPatientUUID();
        return XUUID.fromString(str);
    }

    @Override
    public XUUID getDocumentId() {
        String str = proto.getDocumentUUID().startsWith("DOC_")
            ? proto.getDocumentUUID() : "DOC_" + proto.getDocumentUUID();
        return XUUID.fromString(str);
    }

    @Override
    public String getOriginalId() {
        return proto.getOriginalId();
    }

    @Override
    public String getEncounterId() {
        return proto.getEncId();
    }

    @Override
    public String getSourceId() {
        return proto.getSourceId();
    }

    @Override
    public DateTime getDocumentDate() {
        return new DateTime(proto.getDocumentDate());
    }

    @Override
    public String getTitle() {
        return proto.getTitle();
    }

    @Override
    public int getPageNumber() {
        return proto.getPageNumber();
    }

    @Override
    public String getExtractionType() {
        return proto.getExtractionType();
    }

    @Override
    public String getContent() {
        return proto.getContent();
    }

    @Override
    public String getOrganization() {
        return proto.getOrg();
    }

    @Override
    public List<String> getExternalIds() {
        return proto.getExternalIdsList();
    }

    @Override
    public int hashCode() {
        return proto.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DocCacheElementWrapper) {
            return proto.equals(((DocCacheElementWrapper) o).getProto());
        }
        if (o instanceof DocCacheElement) {
            return proto
                .equals(DocumentMarshal.fromIfcDocCacheElement((DocCacheElement) o).getProto());
        }
        return false;
    }

    @Override
    public int compareTo(@NotNull DocCacheElement o) {
        if (!getDocumentId().getUUID().equals(o.getDocumentId().getUUID())) {
            throw new IllegalArgumentException(
                "Cannot compare doc cache elements from different documents : "
                    + getDocumentId() + ", " + o.getDocumentId());
        }
        return Integer.compare(getPageNumber(), o.getPageNumber());
    }
}
