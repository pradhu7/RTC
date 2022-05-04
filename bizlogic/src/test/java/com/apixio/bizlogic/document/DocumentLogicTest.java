package com.apixio.bizlogic.document;

import com.apixio.model.ocr.page.Ocrextraction;
import com.apixio.model.ocr.page.Page;
import org.junit.Test;

import javax.xml.bind.JAXBException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentLogicTest {
    // stg_pdf_hocr.xml is from pds 1111, docId 8ba062bc-d655-4e91-a692-6b547e6f0fc2
    public final static UUID hocrText = UUID.fromString("8ba062bc-d655-4e91-a692-6b547e6f0fc2");
    // stg_pdf_text.xml is from pds 1128, docId c49203fc-e636-4e4a-9a60-f562db451309
    public final static UUID pdfText = UUID.fromString("c49203fc-e636-4e4a-9a60-f562db451309");
    // stg_rtf_text.xml is from pds  472, docId cbbc061c-5535-400d-9e5f-90ff9e0a0b69
    public final static UUID rtfText = UUID.fromString("cbbc061c-5535-400d-9e5f-90ff9e0a0b69");
    // stg_ehr_text.xml is from pds 1111, docId daaf36cb-962d-4f23-a2c6-942f6fda791b
    public final static UUID ehrText = UUID.fromString("daaf36cb-962d-4f23-a2c6-942f6fda791b");

    // stg_pdf_text is *also* a PDF, but not one where we have text via HOCR, but via Text Extract.

    @Test
    public void testGetPageTexts() throws Exception {
        Ocrextraction rtfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_rtf_text.xml"));
        Page rtfPage = DocumentLogic.getPage(rtfDoc, 1).orElse(null);
        Ocrextraction pdfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_text.xml"));
        Page pdfPage = DocumentLogic.getPage(pdfDoc, 1).orElse(null);
        Ocrextraction hocrDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_hocr.xml"));
        Page hocrPage = DocumentLogic.getPage(hocrDoc, 1).orElse(null);

        assertNotNull(rtfPage);
        assertNull(DocumentLogic.getExtractedPageText(rtfPage));
        assertNull(DocumentLogic.getPlainPagePdfText(rtfPage));
        assertNotNull(DocumentLogic.getPlainPageDocumentText(rtfPage));

        assertNotNull(pdfPage);
        assertNull(DocumentLogic.getExtractedPageText(pdfPage));
        assertNotNull(DocumentLogic.getPlainPagePdfText(pdfPage));
        assertNull(DocumentLogic.getPlainPageDocumentText(pdfPage));

        assertNotNull(hocrPage);
        assertNotNull(DocumentLogic.getExtractedPageText(hocrPage));
        assertNull(DocumentLogic.getPlainPagePdfText(hocrPage));
        assertNull(DocumentLogic.getPlainPageDocumentText(hocrPage));
    }

    @Test
    public void testGetPagesText() throws JAXBException {
        Ocrextraction rtfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_rtf_text.xml"));
        Page rtfPage = DocumentLogic.getPage(rtfDoc, 1).orElse(null);
        Ocrextraction pdfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_text.xml"));
        Page pdfPage = DocumentLogic.getPage(pdfDoc, 1).orElse(null);
        Ocrextraction hocrDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_hocr.xml"));
        Page hocrPage = DocumentLogic.getPage(hocrDoc, 1).orElse(null);
        Ocrextraction ehrDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_ehr_text.xml"));
        Page ehrPage = DocumentLogic.getPage(ehrDoc, 1).orElse(null);

        assertEquals(DocumentLogic.htmlToPageText(DocumentLogic.getPlainPageDocumentText(rtfPage)),
                     DocumentLogic.getPageText(rtfText, rtfPage));

        assertEquals(DocumentLogic.pdfTextExtractToPageText(pdfText, pdfPage.getPageNumber().intValue(), DocumentLogic.getPlainPagePdfText(pdfPage)),
                     DocumentLogic.getPageText(pdfText, pdfPage));

        assertEquals(DocumentLogic.hocrToPageText(hocrText, hocrPage.getPageNumber().intValue(), DocumentLogic.getExtractedPageText(hocrPage)),
                     DocumentLogic.getPageText(hocrText, hocrPage));

        assertEquals(DocumentLogic.htmlToPageText(DocumentLogic.getPlainPageDocumentText(ehrPage)),
                     DocumentLogic.getPageText(hocrText, ehrPage));

    }

    @Test
    public void testHocrExtract() throws JAXBException {
        Ocrextraction hocrDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_hocr.xml"));
        Page hocrPage = DocumentLogic.getPage(hocrDoc, 1).orElse(null);

        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(hocrText, hocrPage);
        assertTrue(pageText.text[0] instanceof PageTextWithCoords);

        List<PageTextWithCoords> threeWords = Arrays.stream((PageTextWithCoords[]) pageText.text).skip(6).limit(3).collect(Collectors.toList());

        assertEquals(
                Arrays.asList("Ms. Knowles is".split(" ")),
                threeWords.stream().map(t -> t.text).collect(Collectors.toList())
        );

        // These words *should* be in left-to-right order, so the "left" value for each word should also be in ascending order.
        List<Double> lefts = threeWords.stream().map(t -> t.left).collect(Collectors.toList());
        assertTrue(lefts.get(0) < lefts.get(1));
        assertTrue(lefts.get(1) < lefts.get(2));
    }

    @Test
    public void testPdfExtract() throws JAXBException {
        Ocrextraction pdfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_text.xml"));
        Page pdfPage = DocumentLogic.getPage(pdfDoc, 1).orElse(null);

        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(pdfText, pdfPage);
        assertTrue(pageText.text[0] instanceof PageTextWithCoords);

        List<PageTextWithCoords> firstThreeWords = Arrays.stream((PageTextWithCoords[]) pageText.text).limit(3).collect(Collectors.toList());

        assertEquals(
                Arrays.asList("Page 1 of".split(" ")),
                firstThreeWords.stream().map(t -> t.text).collect(Collectors.toList())
        );

        // These words *should* be in left-to-right order, so the "left" value for each word should also be in ascending order.
        List<Double> lefts = firstThreeWords.stream().map(t -> t.left).collect(Collectors.toList());
        assertTrue(lefts.get(0) < lefts.get(1));
        assertTrue(lefts.get(1) < lefts.get(2));
    }

    @Test
    public void testRtfExtract() throws JAXBException {
        Ocrextraction rtfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_rtf_text.xml"));
        Page rtfPage = DocumentLogic.getPage(rtfDoc, 1).orElse(null);

        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(rtfText, rtfPage);
        assertTrue(pageText.text[0] instanceof PageTextWithoutCoords);

        List<PageTextWithoutCoords> firstThreeWords = Arrays.stream((PageTextWithoutCoords[]) pageText.text).limit(3).collect(Collectors.toList());

        assertEquals(
                Arrays.asList("Joseph R Smith".split(" ")),
                firstThreeWords.stream().map(t -> Arrays.stream(t.text.split("\\W")).filter(s -> !s.isEmpty()).findFirst().get()).collect(Collectors.toList())
        );

        // These words *should* be in left-to-right order, so the "left" value for each word should also be in ascending order. But RTFs don't give us coordinates for the text!
    }

    @Test
    public void testHocrSearch() throws JAXBException {
        Ocrextraction hocrDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_hocr.xml"));
        int page = 1;
        Page hocrPage = DocumentLogic.getPage(hocrDoc, page).orElse(null);
        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(hocrText, hocrPage);

        String term = "Ms. Knowles is";
        term = Arrays.stream(term.split("\\W"))
                .filter(DocumentLogic.nonEmptyString)
                .collect(Collectors.joining(" "));

        assertEquals(
                Arrays.asList(new PageTextWithCoords(hocrText,
                                                     page,
                                                     term,
                                                     0.17,
                                                     0.1188235294117647,
                                                     0.010000000000000009,
                                                     0.11803921568627453)),
                pageText.searchHighlights(term));
    }

    @Test
    public void testPdfTextExtractSearch() throws JAXBException {
        Ocrextraction pdfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_text.xml"));
        int page = 1;
        Page pdfPage = DocumentLogic.getPage(pdfDoc, page).orElse(null);
        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(pdfText, pdfPage);

        String term = "Page 1 of";

        List<PageTextWithCoords> expected = Arrays.asList(new PageTextWithCoords(pdfText,
                                                                                 page,
                                                                                 term,
                                                                                 0.01853033405199618,
                                                                                 0.8234596672649388,
                                                                                 0.015147460528242437,
                                                                                 0.0756238839559118));

        assertEquals(
                expected,
                pageText.searchHighlights(term));

        assertEquals(expected,
                pageText.searchHighlights(Collections.singleton(term)));

        String someKey = "foo";
        assertEquals(expected,
                pageText.searchHighlights(Collections.singletonMap(someKey, Collections.singleton(term))).get(someKey));

        assertEquals(expected,
                DocumentLogic.searchPagesHighlights(
                        pdfText,
                        pdfDoc,
                        Collections.singletonMap(page,
                                Collections.singletonMap(someKey,
                                        Collections.singleton(term)))).get(page).get(someKey));

        assertEquals(
                expected,
                DocumentLogic.search(
                        pdfText,
                        pdfDoc,
                        Collections.singletonMap("1",
                                Collections.singleton(term))
                ).get(page).highlights
        );
    }

    @Test
    public void testNoCoordsForEhrSearch() throws JAXBException {
        Ocrextraction ehrDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_ehr_text.xml"));
        int page = 1;
        Page ehrPage = DocumentLogic.getPage(ehrDoc, page).orElse(null);
        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(ehrText, ehrPage);

        String term = "Patient";

        List<String> expected = Arrays.asList(
                " <span class=\"highlight\">patient</span> has"
        );

        assertEquals( // Note, no exceptions thrown here, just an empty list.
                Collections.emptyList(),
                pageText.searchHighlights(term));

        assertEquals(expected,
                     Arrays.asList(pageText.searchSnippets(term).split("\\.\\.\\."))
                     );
    }

    @Test
    public void testNoCoordsForRtfSearch() throws JAXBException {
        Ocrextraction rtfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_rtf_text.xml"));
        Page rtfPage = DocumentLogic.getPage(rtfDoc, 1).orElse(null);
        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(rtfText, rtfPage);

        String term = "Smith";

        assertEquals(
                Collections.emptyList(),
                pageText.searchHighlights(term));
    }

    @Test
    public void testSnippetSearch() throws JAXBException {
        Ocrextraction pdfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_text.xml"));
        int page = 1;
        Page pdfPage = DocumentLogic.getPage(pdfDoc, page).orElse(null);
        DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(pdfText, pdfPage);

        List<String> expected = Arrays.asList(
                "1 of 8 <span class=\"highlight\">Mary</span> 88 Y o1d Female, DOB;",
                "Guarantor: <span class=\"highlight\">Mary</span> lnsul\"ance: MEDICAR.13 Payer",
                "confused. j Patient: <span class=\"highlight\">Mary</span> DOB: 3/1/1929 Progress Note:"
        );

        assertEquals(expected,
                Arrays.asList(pageText.text[0].searchSnippets("mary", pageText.text).split("\\.\\.\\."))
        );

        assertEquals(expected,
                Arrays.asList(pageText.searchSnippets("mary").split("\\.\\.\\."))
        );

        assertEquals(expected,
                Arrays.asList(pageText.searchSnippets(Collections.singleton("mary")).get(0).split("\\.\\.\\."))
        );

        assertEquals(expected,
                Arrays.asList(pageText.searchSnippets(Collections.singletonMap("foo",
                        Collections.singleton("mary"))).get("foo").get(0).split("\\.\\.\\."))
        );

        assertEquals(expected,
                Arrays.asList(DocumentLogic.searchPagesSnippets(
                        pdfText,
                        pdfDoc,
                        Collections.singletonMap(page,
                                Collections.singletonMap("foo",
                                        Collections.singleton("mary")))).get(page).get("foo").get(0).split("\\.\\.\\."))
        );
    }

    @Test
    public void testWildcardSearch() throws JAXBException {
        Ocrextraction pdfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_text.xml"));
        int page = 1;

        String term = "Page";

        List<PageTextWithCoords> expected = Arrays.asList(new PageTextWithCoords(pdfText,
                                                                                 page,
                                                                                 term,
                                                                                 0.01853033405199618,
                                                                                 0.8234596672649388,
                                                                                 0.015147460528242437,
                                                                                 0.03780578423991421));

        assertEquals(
                expected,
                DocumentLogic.search(
                        pdfText,
                        pdfDoc,
                        Collections.singletonMap("*",
                                Collections.singleton(term))
                ).get(page).highlights
        );
    }

    @Test
    public void testBoundaryInsensitiveCoordSearchForPDF() throws JAXBException {
        Ocrextraction pdfDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_text.xml"));
        Page pdfPage = DocumentLogic.getPage(pdfDoc, 1).orElse(null);

        List<String> pdfWords = Arrays
                .asList(DocumentLogic.getPageText(pdfText, pdfPage))
                .stream()
                .flatMap(p -> Arrays.asList(p.text).stream())
                .map(w -> ((PageTextWithoutCoords) w).text)
                .collect(Collectors.toList())
                .subList(217, 222); // five words w/ a slash in the middle of the middle word

        testBoundaryInsensitiveTextCoordSearch(pdfText, pdfPage, pdfWords);
    }

    @Test
    public void testBoundaryInsensitiveCoordSearchForHOCR() throws JAXBException {
        Ocrextraction hocrDoc = DocumentLogic.getDocumentObject(this.getClass().getResourceAsStream("/stg_pdf_hocr.xml"));
        Page hocrPage = DocumentLogic.getPage(hocrDoc, 1).orElse(null);

        List<String> hocrWords = Arrays
                .asList(DocumentLogic.getPageText(hocrText, hocrPage))
                .stream()
                .flatMap(p -> Arrays.asList(p.text).stream())
                .map(w -> ((PageTextWithoutCoords)w).text)
                .collect(Collectors.toList())
                .subList(115, 120); // five words w/ a slash in the middle of the middle word

        testBoundaryInsensitiveTextCoordSearch(hocrText, hocrPage, hocrWords);
    }

    public void testBoundaryInsensitiveTextCoordSearch(UUID docUUID, Page page, List<String> words) {
        assertEquals(
                words.stream()
                        .flatMap(word -> Arrays.stream(word.split("\\W")))
                        .collect(Collectors.joining(" ")),
                DocumentLogic
                        .getPageText(docUUID, page)
                        .searchHighlights(words.stream().collect(Collectors.joining(" ")))
                        .get(0).text
        );

        // Now make the list 6 long
        List<String> wordsSplit = words
                .stream()
                .flatMap(w -> Arrays.stream(w.split("/")))
                .collect(Collectors.toList());
        assertEquals(
                6,
                wordsSplit.size()
        );

        assertEquals(
                words
                        .subList(0, 3)
                        .stream()
                        .flatMap(word -> Arrays.stream(word.split("\\W")))
                        .limit(3)
                        .collect(Collectors.joining(" ")),
                DocumentLogic
                        .getPageText(docUUID, page)
                        .searchHighlights(wordsSplit
                                .subList(0, 3)
                                .stream()
                                .collect(Collectors.joining(" ")))
                        .get(0).text
        );

        assertEquals(
                words
                        .subList(2, 5)
                        .stream()
                        .flatMap(word -> Arrays.stream(word.split("\\W")))
                        .skip(1)
                        .collect(Collectors.joining(" ")),
                DocumentLogic
                        .getPageText(docUUID, page)
                        .searchHighlights(wordsSplit
                                .subList(3, 6)
                                .stream()
                                .collect(Collectors.joining(" ")))
                        .get(0).text
        );
    }
}
