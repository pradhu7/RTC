package com.apixio.ifcwrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.DocCacheElement;
import com.apixio.ensemble.ifc.DocumentSource;
import com.apixio.ensemble.ifc.PageSource;
import com.apixio.ensemble.ifc.PageWindow;
import com.apixio.ensemble.ifc.PageWindowSource;
import com.apixio.ensemble.ifc.transport.Documents.DocCacheElementProto;
import com.apixio.ifcwrapper.page.DocCacheElementWrapper;
import com.apixio.ifcwrapper.page.PageWindowWrapper;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DocumentMarshalTest {

    public static final XUUID ID_1 = XUUID.fromString("DOC_3b5eda26-58f6-4d3b-87d7-b3edf156dc19");
    public static final XUUID ID_2 = XUUID.fromString("DOC_fa870a42-4692-46e9-8d23-1f9b5cda599e");

    private static final XUUID DOC_XUUID =
        XUUID.fromString("DOC_8b46098b-5806-4557-b581-b0e61d824002");

    private static final XUUID PAT_XUUID =
        XUUID.fromString("PAT_ef83c8cf-0d93-47d8-b26e-8f7c19578552");

    private static final String DOC_TITLE = "HAR2_TEST_DOCUMENT";
    @Mock
    Patient patient;

    @Mock
    Document document;

    @Mock
    DocCacheElement docCacheElement;

    @Mock
    DocCacheElement page2Element;

    List<DocCacheElement> singlePageElements;

    List<DocCacheElement> doublePageElements;

    String documentContent;

    @BeforeEach
    public void setupTests() throws Exception {
        initMocks(this);

        documentContent =
            IOUtils.toString(this.getClass().getResourceAsStream("/mock_document.txt"));

        when(patient.getPatientId()).thenReturn(PAT_XUUID.getUUID());
        when(document.getInternalUUID()).thenReturn(DOC_XUUID.getUUID());
        when(document.getDocumentTitle()).thenReturn(DOC_TITLE);

        when(docCacheElement.getPageNumber()).thenReturn(1);
        when(docCacheElement.getContent()).thenReturn(documentContent);
        when(docCacheElement.getDocumentId()).thenReturn(DOC_XUUID);
        when(docCacheElement.getPatientId()).thenReturn(PAT_XUUID);
        when(docCacheElement.getTitle()).thenReturn(DOC_TITLE);

        when(page2Element.getPageNumber()).thenReturn(2);
        when(page2Element.getContent()).thenReturn("PAGE 2 TEXT");
        when(page2Element.getDocumentId()).thenReturn(DOC_XUUID);
        when(page2Element.getPatientId()).thenReturn(PAT_XUUID);
        when(page2Element.getTitle()).thenReturn(DOC_TITLE);

        singlePageElements = ImmutableList.of(docCacheElement);
        doublePageElements = ImmutableList.of(docCacheElement, page2Element);
    }

    @Test
    public void docCacheComparisonTest() throws Exception {
        DocCacheElementWrapper element1 = new DocCacheElementWrapper(
            DocCacheElementProto.newBuilder()
                .setDocumentUUID(ID_1.getUUID().toString())
                .setPageNumber(1)
                .build());

        DocCacheElementWrapper element2 = new DocCacheElementWrapper(
            DocCacheElementProto.newBuilder()
                .setDocumentUUID(ID_1.getUUID().toString())
                .setPageNumber(2)
                .build());

        assertThat(element2).isGreaterThan(element1);
        assertThat(element1).isLessThan(element2);

        DocCacheElementWrapper outsideDocument = new DocCacheElementWrapper(
            DocCacheElementProto.newBuilder()
                .setDocumentUUID(ID_2.getUUID().toString())
                .setPageNumber(2)
                .build());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            element1.compareTo(outsideDocument);
        });
    }

    @Test
    public void utilsShouldCreateWholeDocumentPageWindow() {
        PageWindow wholeDocPw = DocumentMarshal.createFullDocPageWindow(
            patient, document, singlePageElements);

        assertThat(wholeDocPw.getContent()).isEqualTo(documentContent);
        assertThat(wholeDocPw.getTitle()).isEqualTo(DOC_TITLE);
        assertThat(wholeDocPw.getDocumentId()).isEqualTo(DOC_XUUID);
        assertThat(wholeDocPw.getPageNumbers()).containsExactly(1);
        assertThat(wholeDocPw.getPatientId()).isEqualTo(PAT_XUUID);

        assertThat(wholeDocPw.getSource() instanceof DocumentSource).isTrue();

        DocumentSource documentSource = (DocumentSource) wholeDocPw.getSource();

        assertThat(documentSource.getNumPages()).isEqualTo(1);
        assertThat(documentSource.getDocumentId()).isEqualTo(DOC_XUUID);
        assertThat(documentSource.getPatientId()).isEqualTo(PAT_XUUID);
    }

    @Test
    public void utilsShouldCreateSinglePageWindows() {
        PageWindowWrapper wholeDocPw = DocumentMarshal.createFullDocPageWindow(
            patient, document, doublePageElements);

        assertThat(wholeDocPw.getTitle()).isEqualTo(DOC_TITLE);
        assertThat(wholeDocPw.getDocumentId()).isEqualTo(DOC_XUUID);
        assertThat(wholeDocPw.getPageNumbers()).containsExactly(1, 2);
        assertThat(wholeDocPw.getPatientId()).isEqualTo(PAT_XUUID);

        List<PageWindowWrapper> pages = DocumentMarshal.createPerPagePageWindows(wholeDocPw);

        assertThat(pages.size()).isEqualTo(2);
        assertThat(pages.get(0).getContent()).isEqualTo(documentContent);
        assertThat(pages.get(0).getPages().size()).isEqualTo(1);
        assertThat(pages.get(0).getPages().get(0).getContent()).isEqualTo(documentContent);

        assertThat(pages.get(0).getPageNumbers()).containsExactly(1);
        assertThat(pages.get(1).getContent()).isEqualTo("PAGE 2 TEXT");
        assertThat(pages.get(1).getPageNumbers()).containsExactly(2);
        assertThat(pages.get(1).getPages().size()).isEqualTo(1);
        assertThat(pages.get(1).getPages().get(0).getContent()).isEqualTo("PAGE 2 TEXT");
    }


    @Test
    public void utilsShouldCreateLoHiPageWindows() {
        List<DocCacheElement> mockCacheElements = IntStream.range(1, 11)
            .mapToObj(i -> DocCacheElementProto.newBuilder()
                .setPageNumber(i)
                .setContent("TEXT FOR PAGE " + i)
                .setDocumentUUID(DOC_XUUID.getUUID().toString())
                .setPatientUUID(PAT_XUUID.getUUID().toString())
                .setTitle(DOC_TITLE)
                .build())
            .map(proto -> new DocCacheElementWrapper(proto))
            .collect(Collectors.toList());
        PageWindowWrapper wholeDocPw = DocumentMarshal.createFullDocPageWindow(
            patient, document, mockCacheElements);

        List<PageWindowWrapper> loHiPageWindows = DocumentMarshal.createLoHiPageWindows(wholeDocPw);

        assertThat(loHiPageWindows.size()).isEqualTo(10);

        // First page window should be 3 pages: 1 - 3, centroid = 1

        PageWindowWrapper startPageWindow = loHiPageWindows.get(0);
        assertThat(startPageWindow.getSource() instanceof PageWindowSource).isTrue();
        PageWindowSource firstSource = (PageWindowSource) startPageWindow.getSource();

        assertThat(startPageWindow.getPageCentroid()).isEqualTo(1);
        assertThat(firstSource.getCentroid()).isEqualTo(1);
        assertThat(firstSource.getStartPage()).isEqualTo(1);
        assertThat(startPageWindow.getContent().startsWith("TEXT FOR PAGE 1")).isTrue();
        assertThat(firstSource.getEndPage()).isEqualTo(3);
        assertThat(startPageWindow.getPages().size()).isEqualTo(3);

        // Middle page window should be 5 pages: 3 - 7, centroid = 5
        PageWindowWrapper midPageWindow = loHiPageWindows.get(4);
        assertThat(midPageWindow.getSource() instanceof PageWindowSource).isTrue();
        PageWindowSource midSource = (PageWindowSource) midPageWindow.getSource();

        assertThat(midPageWindow.getPageCentroid()).isEqualTo(5);
        assertThat(midSource.getCentroid()).isEqualTo(5);
        assertThat(midSource.getStartPage()).isEqualTo(3);
        assertThat(midPageWindow.getContent().startsWith("TEXT FOR PAGE 3")).isTrue();
        assertThat(midSource.getEndPage()).isEqualTo(7);
        assertThat(midPageWindow.getPages().size()).isEqualTo(5);

        // End page window should be 3 pages, 8 - 10, centroid = 10
        PageWindowWrapper endPageWindow = loHiPageWindows.get(9);

        assertThat(endPageWindow.getSource() instanceof PageWindowSource).isTrue();
        PageWindowSource lastSource = (PageWindowSource) endPageWindow.getSource();

        assertThat(endPageWindow.getPageCentroid()).isEqualTo(10);
        assertThat(lastSource.getCentroid()).isEqualTo(10);
        assertThat(lastSource.getStartPage()).isEqualTo(8);
        assertThat(endPageWindow.getContent().startsWith("TEXT FOR PAGE 8")).isTrue();
        assertThat(lastSource.getEndPage()).isEqualTo(10);
        assertThat(endPageWindow.getPages().size()).isEqualTo(3);
    }

    @Test
    public void utilDemuxShouldCreateAllPossiblePageWindows() {
        List<DocCacheElement> mockCacheElements = IntStream.range(1, 11)
            .mapToObj(i -> DocCacheElementProto.newBuilder()
                .setPageNumber(i)
                .setContent("TEXT FOR PAGE " + i)
                .setDocumentUUID(DOC_XUUID.getUUID().toString())
                .setPatientUUID(PAT_XUUID.getUUID().toString())
                .setTitle(DOC_TITLE)
                .build())
            .map(proto -> new DocCacheElementWrapper(proto))
            .collect(Collectors.toList());

        List<PageWindow> demuxedPageWindows =
            DocumentMarshal.demuxAllPageWindows(patient, document, mockCacheElements);

        // 1 whole doc + 10 single page + 10 range page windows
        assertThat(demuxedPageWindows.size()).isEqualTo(21);

        // Should create exactly one whole doc page window
        assertThat(demuxedPageWindows.stream()
            .filter(pw -> pw.getSource() instanceof DocumentSource)
            .collect(Collectors.toList())).hasSize(1);

        // Should create exactly one single page window per page (10 total)
        assertThat(demuxedPageWindows.stream()
            .filter(pw -> pw.getSource() instanceof PageSource)
            .collect(Collectors.toList())).hasSize(10);

        // Should create exactly one lo-hi page window doc window per page (10 total)
        assertThat(demuxedPageWindows.stream()
            .filter(pw -> pw.getSource() instanceof PageWindowSource)
            .collect(Collectors.toList())).hasSize(10);
    }
}
