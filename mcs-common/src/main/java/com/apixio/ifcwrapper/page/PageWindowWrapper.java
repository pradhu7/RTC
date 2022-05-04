package com.apixio.ifcwrapper.page;

import static com.apixio.util.Protobuf.XUUIDfromProtoXUUID;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.DocCacheElement;
import com.apixio.ensemble.ifc.DocumentSource;
import com.apixio.ensemble.ifc.PageSource;
import com.apixio.ensemble.ifc.PageWindow;
import com.apixio.ensemble.ifc.PageWindowSource;
import com.apixio.ensemble.ifc.Source;
import com.apixio.ensemble.ifc.transport.Documents.PageWindowProto;
import com.apixio.ifcwrapper.source.DocumentSourceWrapper;
import com.apixio.ifcwrapper.source.PageSourceWrapper;
import com.apixio.ifcwrapper.source.PageWindowSourceWrapper;
import com.apixio.messages.MessageMetadata;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageWindowWrapper implements PageWindow {

    private static final Logger LOG = LoggerFactory.getLogger(PageWindowWrapper.class);

    private final PageWindowProto proto;

    public PageWindowWrapper(PageWindowProto proto) {
        this.proto = proto;
    }

    public PageWindowProto getProto() {
        return proto;
    }

    @Override
    public List<DocCacheElement> getPages() {
        return proto.getPagesList().stream()
            .map(DocCacheElementWrapper::new)
            .collect(Collectors.toList());
    }

    private List<DocCacheElementWrapper> getWrappedPages() {
        return proto.getPagesList().stream()
            .map(DocCacheElementWrapper::new)
            .collect(Collectors.toList());
    }

    @Override
    public Source getSource() {
        if (proto.hasDocumentSource()) {
            return new DocumentSourceWrapper(proto.getDocumentSource());
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
        throw new IllegalStateException("PageWindow has unsupported source");
    }

    /**
     * @return a sorted list of all page numbers in the page. in ascending order.
     */
    @Override
    public int[] getPageNumbers() {
        List<Integer> pageNumberList = proto.getPageNumbersList().stream()
            .sorted()
            .collect(Collectors.toList());
        int[] result = new int[pageNumberList.size()];
        for (int i = 0; i < pageNumberList.size(); i++) {
            result[i] = pageNumberList.get(i);
        }
        return result;
    }

    @Override
    public int getPageCentroid() {
        Source source = getSource();
        if (source instanceof PageWindowSource) {
            return ((PageWindowSource) source).getCentroid();
        }

        if (source instanceof PageSource) {
            return ((PageSource) source).getPage();
        }

        if (source instanceof DocumentSource) {
            return ((DocumentSource) source).getNumPages();
        }
        throw new IllegalStateException("Cannot determine centroid for source : " + source);
    }

    @Override
    public String getContent() {
        return proto.getPagesList().stream()
            .map(DocCacheElementWrapper::new)
            .sorted()
            .map(DocCacheElementWrapper::getContent)
            .collect(Collectors.joining("\n"));
    }

    @Override
    public String getTitle() {
        return proto.getTitle();
    }

    @Override
    public List<String> getTags() {
        return proto.getTagsList();
    }

    @Override
    public XUUID getDocumentId() {
        return XUUIDfromProtoXUUID(getDocumentIdAsProto());
    }

    public MessageMetadata.XUUID getDocumentIdAsProto() {
        switch (proto.getSourceCase()) {
            case DOCUMENTSOURCE:
                return proto.getDocumentSource().getDocumentID();
            case PAGESOURCE:
                return proto.getPageSource().getDocumentID();
            case PAGEWINDOWSOURCE:
                return proto.getPageWindowSource().getDocumentID();
            default:
                throw new IllegalStateException(
                    "Don't know how to get document from source type : " + proto.getSourceCase());
        }
    }

    @Override
    public XUUID getPatientId() {
        return XUUIDfromProtoXUUID(getPatientIdAsProto());
    }

    public MessageMetadata.XUUID getPatientIdAsProto() {
        switch (proto.getSourceCase()) {
            case DOCUMENTSOURCE:
                return proto.getDocumentSource().getPatientID();
            case PAGESOURCE:
                return proto.getPageSource().getPatientID();
            case PAGEWINDOWSOURCE:
                return proto.getPageWindowSource().getPatientID();
            default:
                throw new IllegalStateException(
                    "Don't know how to get patient from source type : " + proto.getSourceCase());
        }
    }

    @Override
    public int hashCode() {
        return proto.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PageWindowWrapper) {
            return proto.equals(((PageWindowWrapper) o).getProto());
        }
        if (o instanceof PageWindow) {
            throw new UnsupportedOperationException("Not implemented");
        }
        return false;
    }
}
