package com.apixio.bizlogic.document;

import com.apixio.converters.text.extractor.TextExtractionUtils;
import com.apixio.dao.blobdao.BlobDAO;
import com.apixio.model.blob.BlobType;
import com.apixio.model.ocr.page.ExtractedText;
import com.apixio.model.ocr.page.Ocrextraction;
import com.apixio.model.ocr.page.Page;
import com.aspose.pdf.HtmlLoadOptions;
import org.apache.commons.io.IOUtils;
import com.aspose.pdf.License;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.apixio.dao.utility.PageUtility.STRING_CONTENT;

public class DocumentLogic {
    public final static Predicate<String> nonEmptyString = ((Predicate<String>) String::isEmpty).negate();
    private final BlobDAO blobDao;

    private static final Logger logger = LoggerFactory.getLogger(DocumentLogic.class);

    public DocumentLogic(BlobDAO blobDao) {
        this.blobDao = blobDao;
        try {
            License license = new License();
            license.setLicense(DocumentLogic.class.getClassLoader().getResourceAsStream("Aspose.PDF.Java-17.8.lic"));
        } catch (Exception e) {
            logger.info("cannot find espose license %s".format(e.getMessage()));
        }
    }

    public static Map<Integer, DocTermResults> search(UUID documentUuid,
                                                      Ocrextraction ocrDocument,
                                                      Map<String, Collection<String>> pageTerms) {
        Collection<String> wildcard = pageTerms.getOrDefault("*", null);

        Map<Integer, Collection<String>> pageIntTerms;
        if (wildcard != null) {
            pageIntTerms = IntStream.range(1, getPageCount(ocrDocument).intValue() + 1)
                    .boxed()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            i -> wildcard
                    ));
        } else {
            pageIntTerms = pageTerms.entrySet().stream().collect(Collectors.toMap(
                    kv -> Integer.parseInt(kv.getKey()),
                    Map.Entry::getValue
            ));
        }

        Map<BigInteger, Page> documentPages = getPages(ocrDocument, pageIntTerms.keySet());

        return pageIntTerms
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        es -> {
                            Page documentPage = documentPages.get(BigInteger.valueOf(es.getKey()));
                            DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(documentUuid, documentPage);
                            if (pageText == null)
                                return new DocTermResults(Collections.emptyList(), Collections.emptyList());
                            List<PageTextWithCoords> highlights = pageText.searchHighlights(es.getValue());
                            List<String> snippets = pageText.searchSnippets(es.getValue());
                            DocTermResults ret = new DocTermResults(highlights, snippets);
                            return ret;
                        }
                ));
    }

    public static <Category> Map<Integer, Map<Category, List<PageTextWithCoords>>> searchPagesHighlights(UUID documentUuid,
                                                                                                         Ocrextraction ocrDocument,
                                                                                                         Map<Integer, Map<Category, Collection<String>>> pageCategoryTerms) {
        Map<BigInteger, Page> documentPages = getPages(ocrDocument, pageCategoryTerms.keySet());
        return pageCategoryTerms
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        es -> {
                            Page documentPage = documentPages.get(BigInteger.valueOf(es.getKey()));
                            DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(documentUuid, documentPage);
                            return pageText.searchHighlights(es.getValue());
                        }
                ));
    }

    public static <Category> Map<Integer, Map<Category, List<String>>> searchPagesSnippets(UUID documentUuid,
                                                                                           Ocrextraction ocrDocument,
                                                                                           Map<Integer, Map<Category, Collection<String>>> pageCategoryTerms) {
        Map<BigInteger, Page> documentPages = getPages(ocrDocument, pageCategoryTerms.keySet());
        return pageCategoryTerms
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        es -> {
                            Page documentPage = documentPages.get(BigInteger.valueOf(es.getKey()));
                            DocPageText<? extends PageText> pageText = DocumentLogic.getPageText(documentUuid, documentPage);
                            return pageText.searchSnippets(es.getValue());
                        }
                ));
    }

    // Existing logic from document search returns the first content in this hierarchy:
    // - HOCR
    // - Text Extract
    // - Non-PDF plaintext
    public static DocPageText<? extends PageText> getPageText(UUID documentUuid, Page ocrPage) {
        int documentPage = ocrPage.getPageNumber().intValue();
        Stream<Function<Page, DocPageText<? extends PageText>>> pageExtractors = Stream.of(
                ((Function<Page, String>) DocumentLogic::getExtractedPageText).andThen(hocr -> hocrToPageText(documentUuid, documentPage, hocr)),
                ((Function<Page, String>) DocumentLogic::getPlainPagePdfText).andThen(text -> pdfTextExtractToPageText(documentUuid, documentPage, text)),
                ((Function<Page, String>) DocumentLogic::getPlainPageDocumentText).andThen(DocumentLogic::htmlToPageText)
        );

        return pageExtractors
                .map(pe -> pe.apply(ocrPage))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        // Do I throw, instead? We don't know how to handle after this
        // point. Like, do we even have docs like this? What's a
        // non-insane way to search for a few to see if they even have
        // anything, how should that be transformed if non-empty/null
        // content exists or what should be returned here?
    }

    public static String getExtractedPageText(Page page) {
        return Optional.ofNullable(page)
            .map(DocumentLogic::getDocumentPageHocr)
            .filter(nonEmptyString)
            .orElse(null);
    }

    public static DocPageText<PageTextWithCoords> hocrToPageText(UUID documentUuid, int documentPage, String hocr) {
        if (hocr == null)
            return null;
        Document pageRoot = Jsoup.parse(hocr.replace("&?",""));

        String[] bbox = pageRoot.select("div.ocr_page")
                .stream()
                .findFirst()
                .map(DocumentLogic::getTitle)
                .get()
                .get("bbox")
                .split(" ");
        if (bbox.length != 4) {
            throw new RuntimeException("Could not extract HOCR page bounding box information.");
        }
        int pageHeight = Integer.parseInt(bbox[3]);
        int pageWidth = Integer.parseInt(bbox[2]);

        PageTextWithCoords[] words = pageRoot
                .select("span.ocr_line")
                .stream()
                .flatMap(line -> {
                    Map<String, String> lineDetails = getTitle(line);

                    return line
                            .select("span.ocrx_word")
                            .stream()
                            .map(word -> {
                                String text = word.text().trim();

                                // ex: title="bbox 1497 1135 2187 1180; textangle 180; x_size 46.388493; x_descenders 9.3611107; x_ascenders 9.4523811"
                                Map<String, String> wordDetails = getTitle(word);
                                Double[] coordinates = Arrays.stream(wordDetails.get("bbox").split(" "))
                                        .map(Double::parseDouble)
                                        .toArray(Double[]::new);

                                // Flip the coordinates if textangle is 180

                                // Handle other transforms
                                // baseline 0 -1;
                                //  The two numbers for the baseline are the slope (1st number) and constant term (2nd number) of a
                                // linear equation describing the baseline relative to the bottom left corner of the bounding box (red).
                                // The baseline crosses the y-axis at -18 and its slope angle is arctan(0.015) = 0.86°.
                                //
                                // x_size 52.388062; x_descenders 7.3880601; x_ascenders 12

                                // TODO: what other textangle values could we see and how should they be handled?
                                double top;
                                double left;
                                if ("180".equals(lineDetails.getOrDefault("textangle", null))) {
                                    left = 1 - (coordinates[2] / pageWidth);
                                    top = 1 - (coordinates[3] / pageHeight);
                                } else {
                                    left = coordinates[0] / pageWidth;
                                    top = coordinates[1] / pageHeight;
                                }
                                return new PageTextWithCoords(
                                        documentUuid,
                                        documentPage,
                                        text,
                                        top,
                                        left,
                                        ((coordinates[3] - coordinates[1]) / pageHeight),
                                        ((coordinates[2] - coordinates[0]) / pageWidth)
                                        );
                            });
                })
                .toArray(PageTextWithCoords[]::new);
        return new DocPageText<>(words);
    }

    private static Map<String, String> getTitle(Element element) {
        return Arrays.stream(element.attr("title")
                .split(";"))
                .map(String::trim)
                .collect(Collectors.toMap(
                        kvs -> kvs.substring(0, kvs.indexOf(" ")),
                        kvs -> kvs.substring(kvs.indexOf(" ")+1)
                ));
    }

    public static String getPlainPagePdfText(Page page) {
        return Optional.ofNullable(page)
            .filter(p -> p.getImgType().equals("PDF"))
            .map(Page::getPlainText)
            .map(JAXBElement::getValue)
            .filter(nonEmptyString)
            .orElse(null);
    }

    public static DocPageText<PageTextWithCoords> pdfTextExtractToPageText(UUID documentUuid, int documentPage, String text) {
        if (text == null)
            return null;

        // fix all unclosed image tags (Note, this may need to be extended to other types
        String validXmlPlainText = text.replaceAll("<img([^/>]*)>", "<img$1></img>");

        Document pageRoot = Jsoup.parse(validXmlPlainText);

        Element pageStyleElement = pageRoot
                .select("div.page[style]")
                .stream()
                .findFirst()
                .get();

        Map<String, String> pageStyle = getStyle(pageStyleElement);

        double pageHeight = Double.parseDouble(pageStyle.get("height").replace("pt", ""));
        double pageWidth = Double.parseDouble(pageStyle.get("width").replace("pt", ""));

        return new DocPageText<>(pageRoot
                .select("div.p[style]")
                .stream()
                .map(word -> handleTextExtractWord(documentUuid, documentPage, word, pageHeight, pageWidth))
                .toArray(PageTextWithCoords[]::new)
        );
    }

    private static Map<String, String> getStyle(Element element) {
        return Arrays.stream(element
                .attr("style")
                .split(";"))
                .map(kv -> kv.split(":"))
                .filter(pair -> pair.length > 1)
                .collect(Collectors.toMap(
                        kv -> kv[0],
                        kv -> kv[1]));
    }

    private static PageTextWithCoords handleTextExtractWord(UUID documentUuid, int documentPage, Element wordElement, double pageHeight, double pageWidth) {
        String text = wordElement.text().trim();
        Map<String, String> wordStyle = getStyle(wordElement);
        double fontSize = Double.parseDouble(wordStyle.get("font-size").replace("pt",""));
        // If we have width, use it. Otherwise, calculate the word width
        Double width = Optional.ofNullable(wordStyle.getOrDefault("width", null))
                .filter(Objects::nonNull)
                .map(str -> Double.parseDouble(str.replace("pt", "")))
                .orElse(null);
        if (width == null) {
            String fontFamily = wordStyle.getOrDefault("font-family", "Helvetica");
            int fontWeight = "bold".equals(wordStyle.getOrDefault("font-weight", null)) ? Font.BOLD : Font.PLAIN;
            Font font = new Font(fontFamily, fontWeight, (int) fontSize);

            // Make these global to optimize font transforms
            AffineTransform affineTransform = new AffineTransform();
            FontRenderContext frc = new FontRenderContext(affineTransform, true, true);

            width = font.getStringBounds(text, frc).getWidth();
        }
        PageTextWithCoords ret = new PageTextWithCoords(
                documentUuid,
                documentPage,
                text,
                Double.parseDouble(wordStyle.get("top").replace("pt", "")) / pageHeight,
                Double.parseDouble(wordStyle.get("left").replace("pt", "")) / pageWidth,
                fontSize / pageHeight,
                width/pageWidth
        );
        return ret;
    }

    public static String getPlainPageDocumentText(Page page) {
        return Optional.ofNullable(page)
            // This shouldn't be a hard-coded exception list, but a
            // subset from an enum, right?
            .filter(p -> !p.getImgType().equals("PDF"))
            .map(Page::getPlainText)
            .map(JAXBElement::getValue)
            .filter(nonEmptyString)
            .orElse(null);
    }

    public static DocPageText<PageTextWithoutCoords> htmlToPageText(String text) {
        if (text == null)
            return null;

        InputStream is = stringToInputStream(text);

        String html;

        try {
            html = TextExtractionUtils.getTextFromHtml(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        PageTextWithoutCoords[] words = Arrays.stream(html.split(" "))
                .map(PageTextWithoutCoords::new)
                .toArray(PageTextWithoutCoords[]::new);

        return new DocPageText<>(words);
    }

    public static InputStream stringToInputStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    public static String getDocumentPageHocr(Ocrextraction document, int page) {
        BigInteger pageCount = getPageCount(document);
        if (pageCount.compareTo(BigInteger.valueOf(page)) == -1)
            throw new IllegalArgumentException(String.format("Document only has %d pages (1-indexed). Requested page was %d (0-indexed)", pageCount, page));
        Page ocrPage = getPage(document, page + 1)
                .orElse(null);
        if (ocrPage == null)
            throw new IndexOutOfBoundsException(String.format("Could not find page %d (0-indexed). Document has %d pages (1-indexed).", pageCount, page));
        return getDocumentPageHocr(ocrPage);
    }

    public static BigInteger getPageCount(Ocrextraction document) {
        return document.getPages().getNumPages();
    }

    public static Optional<Page> getPage(Ocrextraction document, int page) {
        BigInteger biPage = BigInteger.valueOf(page);
        return Optional.ofNullable(getPages(document, Collections.singleton(page)).getOrDefault(biPage, null));
    }

    public static Map<BigInteger, Page> getPages(Ocrextraction document, Collection<Integer> pages) {
        Set<BigInteger> bintPages = pages.stream().map(BigInteger::valueOf).collect(Collectors.toSet());

        return document
                .getPages()
                .getPage()
                .stream()
                .filter(ocrPage -> bintPages.stream().anyMatch(p -> ocrPage.getPageNumber().equals(p)))
                .collect(Collectors.toMap(Page::getPageNumber,
                        Function.identity()));
    }

    public static String getDocumentPageHocr(Page page) {
        return Optional.ofNullable(page)
                .map(Page::getExtractedText)
                .map(ExtractedText::getContent)
                .orElse(null);
    }

    public Ocrextraction getDocumentObject(String documentPdsId, String documentUuid) throws Exception {
        InputStream documentStream = this.getDocumentStream(documentPdsId, documentUuid);
        return getDocumentObject(documentStream);
    }

    public static Ocrextraction getDocumentObject(String documentString) throws JAXBException {
        InputStream is = stringToInputStream(documentString);
        return getDocumentObject(is);
    }

    public static Ocrextraction getDocumentObject(InputStream documentStream) throws JAXBException {
        JAXBContext contextObj = JAXBContext.newInstance(Ocrextraction.class);
        Unmarshaller um = contextObj.createUnmarshaller();
        Ocrextraction ocrextraction = (Ocrextraction) um.unmarshal(documentStream);
        return ocrextraction;
    }

    public String getDocumentString(String documentPdsId, String documentUuid) throws Exception {
        return IOUtils.toString(getDocumentStream(documentPdsId, documentUuid), StandardCharsets.UTF_8);

    }

    public InputStream getDocumentStream(String documentPdsId, String documentUuid) throws Exception {
        BlobType stringContentBlobType = new BlobType.Builder(documentUuid, STRING_CONTENT).build();
        InputStream stringContent = blobDao.read(stringContentBlobType, documentPdsId);
        return stringContent;
    }

    // TODO: move the template and simple content rendering in DocumentLogic as well
    public ByteArrayOutputStream getPdfFromSimpleContent(String simpleContent, UUID documentUuid) throws Exception {
        if (simpleContent.isEmpty()) {
            logger.error("simple content cannot be empty %s".format(documentUuid.toString()));
            throw new IllegalArgumentException("simple content cannot be empty %s".format(documentUuid.toString()));
        }
        ByteArrayOutputStream pdfStream = generatePdfStream(simpleContent);
        return pdfStream;
    }

    private ByteArrayOutputStream generatePdfStream(String content) {
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        HtmlLoadOptions options = new HtmlLoadOptions();
        com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(contentInputStream, options);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        pdfDocument.save(output);
        return output;
    }


}
