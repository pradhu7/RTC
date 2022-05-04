package com.apixio.ifcwrapper;

import com.apixio.ensemble.ifc.DocCacheElement;
import com.apixio.ensemble.ifc.PageWindow;
import com.apixio.ensemble.ifc.transport.Documents.DocCacheElementProto;
import com.apixio.ensemble.ifc.transport.Documents.PageWindowProto;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ensemble.ifc.transport.Signals.PageWindowSource;
import com.apixio.ensemble.ifc.transport.Signals.StringLocation;
import com.apixio.ifcwrapper.errors.DocumentSerializationError;
import com.apixio.ifcwrapper.errors.IfcTransportSerializationError;
import com.apixio.ifcwrapper.page.DocCacheElementWrapper;
import com.apixio.ifcwrapper.page.PageWindowWrapper;
import com.apixio.ifcwrapper.source.AbstractSourceWrapper;
import com.apixio.ifcwrapper.util.JsonUtil;
import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentMarshal {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentMarshal.class);

    private static final int WINDOW_LOW_LIMIT = 2;
    private static final int WINDOW_HIGH_LIMIT = 2;

    /**
     * Given any DocCacheElement, wrap it in a protocol buffer serializable DocCacheElement
     *
     * @return wrapped protocol buffer backed instance of DocCacheElement
     */
    public static DocCacheElementWrapper fromIfcDocCacheElement(DocCacheElement element) {
        return fromIfcDocCacheElement(element, false);
    }

    /**
     * Given any IFC page window, wrap it in a protocol buffer serializable PageWindowWrapper.
     *
     * @return wrapped protocol buffer backed instance of PageWindow
     */
    public static PageWindowWrapper fromIfcPageWindow(PageWindow pageWindow)
        throws IfcTransportSerializationError {
        return fromIfcPageWindow(pageWindow, false);
    }

    /**
     * Given a patient, document and the associated doc cache elements, demux all page windows:
     *
     * <ol>
     * <li>1. The full page window (with DocumentSource)</li>
     * <li>2. Page windows for each individual page (with PageSource)</li>
     * <li>3. Page windows with 2 page context before and after centroid (with PageWindowSource)</li>
     * </ol>
     *
     * @param patient the patient object
     * @param document the document object
     * @param docCacheElements all doc cache elements to be considered
     * @return a list of all page windows that can be constructed from the inputs
     */
    public static List<PageWindow> demuxAllPageWindows(
        Patient patient, Document document, List<DocCacheElement> docCacheElements) {

        return demuxAllPageWindows(
            patient.getPatientId(),
            document.getInternalUUID(),
            document.getDocumentTitle(),
            docCacheElements);
    }

    /**
     * This method allows creating this method using only patId, docId, docTitle and
     * docCacheElements - this improves using this method in unit testing
     */
    public static List<PageWindow> demuxAllPageWindows(
        UUID patId, UUID docId, String docTitle, List<DocCacheElement> docCacheElements) {
        ImmutableList.Builder<PageWindow> builder = new Builder();

        final PageWindowWrapper fullDocWindow = createFullDocPageWindow(
            patId, docId, docTitle, docCacheElements);

        // Full Page window
        builder.add(fullDocWindow);
        // All single page windows
        builder.addAll(createPerPagePageWindows(fullDocWindow));
        // All lo-hi context page windows
        builder.addAll(createLoHiPageWindows(fullDocWindow));

        return builder.build();
    }

    public static PageWindowWrapper createFullDocPageWindow(
        UUID patId, UUID docId, String docTitle, List<DocCacheElement> docCacheElements) {
        List<Integer> pageNumbers = getPageNumbers(docCacheElements);
        PageWindowProto proto = PageWindowProto.newBuilder()
            .setTitle(docTitle)
            .setDocumentSource(createDocumentSource(patId, docId, pageNumbers.size()))
            .addAllPageNumbers(pageNumbers)
            .addAllPages(docCacheElements.stream()
                .map(elem -> DocumentMarshal.fromIfcDocCacheElement(elem).getProto())
                .collect(Collectors.toList()))
            .build();
        return new PageWindowWrapper(proto);
    }

    /**
     * Create a single, whole document (DocumentSource) page window containing every page of the
     * document.
     */
    public static PageWindowWrapper createFullDocPageWindow(
        Patient patient, Document document, List<DocCacheElement> docCacheElements) {
        return createFullDocPageWindow(
            patient.getPatientId(),
            document.getInternalUUID(),
            document.getDocumentTitle(),
            docCacheElements);
    }

    /**
     * Given a PageWindow representing the whole document, create a list of single page PageWindows
     * (with PageSource), one for each page
     */
    public static List<PageWindowWrapper> createPerPagePageWindows(PageWindowWrapper fullPw) {
        return fullPw.getProto().getPagesList().stream()
            .map(elementProto ->
                new PageWindowWrapper(PageWindowProto.newBuilder()
                    .setTitle(fullPw.getTitle())
                    .setPageSource(createPageSource(fullPw, elementProto))
                    .addPageNumbers(elementProto.getPageNumber())
                    .addPages(elementProto)
                    .build())
            )
            .collect(Collectors.toList());
    }

    /**
     * Given a PageWindow representing the whole document, create a list of lo-hi range PageWindows
     * (with PageWindowSource), one for each page, but including context of up to 2 pages before and
     * 2 pages after the centroid.
     */
    public static List<PageWindowWrapper> createLoHiPageWindows(PageWindowWrapper fullPw) {
        int[] pageNumbers = fullPw.getPageNumbers();

        final int minPage = pageNumbers[0];
        final int maxPage = pageNumbers[pageNumbers.length - 1];

        final List<DocCacheElementProto> pagesList = fullPw
            .getProto()
            .getPagesList();

        final Map<Integer, List<DocCacheElementProto>> pageToElementMap = pagesList
            .stream()
            .collect(Collectors.groupingBy(DocCacheElementProto::getPageNumber));

        ImmutableList.Builder<PageWindowWrapper> resultBuilder = new ImmutableList.Builder<>();

        for (DocCacheElementProto element : pagesList) {
            int centroid = element.getPageNumber();
            int startPage = Math.max(centroid - WINDOW_LOW_LIMIT, minPage);
            int endPage = Math.min(centroid + WINDOW_HIGH_LIMIT, maxPage);

            PageWindowProto.Builder builder = fullPw.getProto().toBuilder();
            builder.clearPages();
            builder.clearPageNumbers();
            builder.clearSource();

            // note that the start and end range is inclusive
            for (int j = startPage; j <= endPage; j++) {
                if (pageToElementMap.containsKey(j)) {
                    builder.addAllPages(pageToElementMap.get(j));
                    builder.addPageNumbers(j);
                }
            }

            PageWindowSource pageWindowSource = PageWindowSource.newBuilder()
                .setDocumentID(fullPw.getDocumentIdAsProto())
                .setPatientID(fullPw.getPatientIdAsProto())
                .setCentroid(centroid)
                .setStartPage(startPage)
                .setEndPage(endPage)
                .build();

            builder.setPageWindowSource(pageWindowSource);
            builder.setPageCentroid(centroid);

            resultBuilder.add(new PageWindowWrapper(builder.build()));
        }
        return resultBuilder.build();
    }

    @VisibleForTesting
    static DocCacheElementWrapper fromIfcDocCacheElement(
        DocCacheElement element, boolean forceWrap) {
        if (!forceWrap && (element instanceof DocCacheElementWrapper)) {
            return (DocCacheElementWrapper) element;
        }
        DocCacheElementProto.Builder builder = DocCacheElementProto.newBuilder();

        builder.setDocumentUUID(element.getDocumentId().getUUID().toString());
        builder.setContent(element.getContent());
        builder.setTitle(element.getTitle());
        builder.setPageNumber(element.getPageNumber());

        if (element.getPatientId() != null) {
            builder.setPatientUUID(element.getPatientId().getUUID().toString());
        }
        if (element.getOriginalId() != null) {
            builder.setOriginalId(element.getOriginalId());
        }
        if (element.getEncounterId() != null) {
            builder.setEncId(element.getEncounterId());
        }
        if (element.getSourceId() != null) {
            builder.setSourceId(element.getSourceId());
        }
        if (element.getDocumentDate() != null) {
            builder.setDocumentDate(element.getDocumentDate().getMillis());
        }
        if (element.getExtractionType() != null) {
            builder.setExtractionType(element.getExtractionType());
        }
        if (element.getOrganization() != null) {
            builder.setOrg(element.getOrganization());
        }
        if (element.getExternalIds() != null) {
            builder.addAllExternalIds(element.getExternalIds());
        }
        return new DocCacheElementWrapper(builder.build());
    }

    @VisibleForTesting
    static PageWindowWrapper fromIfcPageWindow(PageWindow pageWindow, boolean forceWrap)
        throws IfcTransportSerializationError {

        if (!forceWrap && (pageWindow instanceof PageWindowWrapper)) {
            return (PageWindowWrapper) pageWindow;
        }
        PageWindowProto.Builder builder = PageWindowProto.newBuilder();

        builder.addAllPages(pageWindow.getPages().stream()
            .map(elem -> fromIfcDocCacheElement(elem))
            .map(elem -> elem.getProto())
            .collect(Collectors.toList()));

        AbstractSourceWrapper source = SourceMarshal.wrapSource(pageWindow.getSource());
        switch (SourceMarshal.getSourceType(source)) {
            case PAGE:
                builder.setPageSource((Signals.PageSource) source.getProto());
                break;
            case DOCUMENT:
                builder.setDocumentSource((Signals.DocumentSource) source.getProto());
                break;
            case PAGE_WINDOW:
                builder.setPageWindowSource((Signals.PageWindowSource) source.getProto());
                break;
            default:
                LOG.error("Error serializing ifc.Source : " +
                    JsonUtil.writeProtoForLog(source.getProto()));
                throw new DocumentSerializationError(
                    "Could not determine source type of signal");
        }

        builder.addAllPageNumbers(Arrays.asList(ArrayUtils.toObject(pageWindow.getPageNumbers())));

        builder.setPageCentroid(pageWindow.getPageCentroid());
        builder.setTitle(pageWindow.getTitle());
        builder.addAllTags(pageWindow.getTags());

        return new PageWindowWrapper(builder.build());
    }

    static Signals.DocumentSource createDocumentSource(
        UUID patId, UUID docId, int numPages) {
        Signals.DocumentSource sourceProto = Signals.DocumentSource.newBuilder()
            .setDocumentID(XUUID.newBuilder()
                .setUuid(docId.toString())
                .setType("DOC")
                .build())
            .setPatientID(XUUID.newBuilder()
                .setUuid(patId.toString())
                .setType("PAT")
                .build())
            .setNumPages(numPages)
            .setStringLocation(StringLocation.getDefaultInstance())
            .build();
        return sourceProto;
    }

    /*
    static Signals.DocumentSource createDocumentSource(
        Patient patient, Document document, int numPages) {
        return createDocumentSource(patient.getPatientId(), document.getInternalUUID(), numPages);
    }
     */

    static Signals.PageSource createPageSource(
        PageWindowWrapper fullPw, DocCacheElementProto elementProto) {

        Signals.PageSource sourceProto = Signals.PageSource.newBuilder()
            .setDocumentID(fullPw.getProto().getDocumentSource().getDocumentID())
            .setPatientID(fullPw.getProto().getDocumentSource().getPatientID())
            .setStringLocation(StringLocation.getDefaultInstance())
            .setPage(elementProto.getPageNumber())
            .build();

        return sourceProto;
    }

    /**
     * @return Page numbers in sorted order
     */
    static List<Integer> getPageNumbers(List<DocCacheElement> docCacheElements) {
        return docCacheElements.stream()
            .map(DocCacheElement::getPageNumber)
            .collect(Collectors.toSet())
            .stream()
            .sorted()
            .collect(Collectors.toList());
    }
}
