package com.apixio.converters.ccda.parser;

import com.apixio.converters.html.creator.HTMLUtils;
import com.apixio.converters.text.extractor.TextExtractionUtils;
import com.apixio.converters.xml.creator.XMLUtils;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.patient.*;
import com.apixio.model.utility.PatientJSONParser;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by alarocca on 7/10/20.
 */
public class CCDParserTest {

    private InputStream getInputStreamFromDocId(String docId, String pdsId) {
        InputStream is = null;
        try {

            String prodToken = "{PUT TOKEN HERE}";
            String urlPath = "https://accounts.apixio.com/api/do-pdf/document/" + docId + "/file";
            if (pdsId != null) {
                urlPath += "?pdsId=" + pdsId;
            }
            URL url = new URL(urlPath);
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestProperty("Authorization", "Apixio " + prodToken);
            urlc.setAllowUserInteraction(false);
            is = urlc.getInputStream();
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return is;
    }

    @Ignore
    @Test
    public void testInputStream()
    {
        try {
            System.out.println(IOUtils.toString(new ByteArrayInputStream(null)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testParseCCD()
    {
        try {
//            String fileName = "/Users/alarocca/Downloads/healthfirst/test.xml";
//            String fileName = "/Users/alarocca/Downloads/coverage_extraction_test.xml";
//            String fileName = "/Users/alarocca/Downloads/text.xml";
//            String fileName = "src/test/java/com/apixio/converters/ccda/parser/1up_ccda_1.xml";
//            String fileName = "src/test/java/com/apixio/converters/ccda/parser/athena_ccda_2.xml";
//            InputStream ccdDocument = new FileInputStream(fileName);
//            String prodDocId = "2a704ad0-a7b5-431f-98bf-fa1921961a97";
//            String prodDocId = "255fce5b-813d-4afb-bd73-c9cb98dec249";
//            String prodDocId = "55dcafae-57bd-427e-aa0b-a230c364d86e";
            String prodDocId = "69ad4143-2bf2-4ea3-86e0-370071c2b364";
            String pdsId = "10001102";
            byte[] documentBytes = IOUtils.toByteArray(getInputStreamFromDocId(prodDocId, pdsId));
            CCDAParser parser = new CCDAParser();
//            String fileName = "/Users/alarocca/Documents/document_load_test/test_b64_txt_ccda.xml";
//            InputStream ccdDocument = new FileInputStream(fileName);
//            byte[] documentBytes = IOUtils.toByteArray(ccdDocument);
            String paa = "PAT_ID";
            parser.setPrimaryAssignAuthority(paa);
            ApxCatalog.CatalogEntry catalogEntry = new ApxCatalog.CatalogEntry();
            catalogEntry.setMimeType("application/x-ccd");
            catalogEntry.setDocumentUUID(UUID.randomUUID().toString());
            ApxCatalog.CatalogEntry.Patient patient = new ApxCatalog.CatalogEntry.Patient();
            ApxCatalog.CatalogEntry.Patient.PatientId patientId = new ApxCatalog.CatalogEntry.Patient.PatientId();
            patientId.setId("12345");
            patientId.setAssignAuthority(paa);
            patient.getPatientId().add(patientId);
            catalogEntry.setPatient(patient);
            parser.parse(new ByteArrayInputStream(documentBytes), catalogEntry);

            Patient patientApo = parser.getPatient();

//
//            for (Document doc : patientApo.getDocuments()) {
//                System.out.println(doc.getDocumentDate());
//            }
//
//            for (Prescription med : patientApo.getPrescriptions()) {
//                System.out.println(med);
//            }
            // Hack the protected getIdentity method to test the fix. REMOVED due to migration to mono making bizlogic unavailable
//            for (LabResult labResult : patientApo.getLabs()) {
//                LabResultMerge labResultMerge = new LabResultMerge();
//                Method m = labResultMerge.getClass().getDeclaredMethod("getIdentity", BaseObject.class);
//                m.setAccessible(true);
//                System.out.println(m.invoke(labResultMerge, labResult));
//            }
            for (Coverage coverage : patientApo.getCoverage()) {
                System.out.println(coverage);
            }
//            for (SocialHistory socialHistory : patientApo.getSocialHistories()) {
//                System.out.println(socialHistory);
//            }

//            patientApo.setPatientId(UUID.randomUUID());

// TODO: We no longer populate the document contents in the parser. This happens in the Parser Manager
//  We should move that to DocumentLogic so it is centrally located. Additionally we need to handle the case
//  that a source may contain multiple "Document" objects.
// Mimic the extraction of text to test the clean text
//
//            String computedFileType = ParserUtil.getFileTypeFromDocumentOrCatalog(catalogEntry, patientApo);
//            System.out.println(computedFileType);
//            documentBytes = ParserUtil.getDocumentContent(patientApo);
////            System.out.println(IOUtils.toString(documentBytes));
//            Iterable<Document> documents = patientApo.getDocuments();
//            if (documents != null)
//            {
//                Iterator<Document> it = documents.iterator();
//                if (it.hasNext())
//                {   // There should be only one file to process. So getting the 0th element.
//                    Document document = it.next();
//                    String xmlContent = XMLUtils.getXMLRepresentation(new ByteArrayInputStream(documentBytes), computedFileType);
//                    System.out.println(xmlContent);
//                    if (xmlContent != null && !xmlContent.trim().equals(""))
//                    {
//                        document.setStringContent((document.getStringContent() == null ? "" : document.getStringContent()) + xmlContent);
//
//                        IOUtils.write(xmlContent,new FileOutputStream("/Users/alarocca/Downloads/stringContent.txt"),"UTF8");
//                    }
//                    //Aspose text extraction..
//                    Map<String, String> metadata = document.getMetadata();
//                    if (metadata == null)
//                        metadata = new HashMap<String, String>();
//
//                    String extractedBytes = TextExtractionUtils.extractText(new ByteArrayInputStream(documentBytes), computedFileType);
////                    System.out.println(extractedBytes);
//                    metadata.put("textextracted", extractedBytes);
//                    document.setMetadata(metadata);
//
//                    // TODO: do we want "totalPages" for text documents? right now we consider them always as 1 page
//                }
//            }

//            System.out.println(DocCacheElemUtil.generateCleanTextAndUpdateApo(patientApo,"fakePds"));

            PatientJSONParser p = new PatientJSONParser();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            System.out.println(patientApo.getCoverage());
            p.serialize(bos,patientApo);
            System.out.println(bos.toString());
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testCDAHtml()
    {
        try {
//            String fileName = "/Users/alarocca/Downloads/coverage_extraction_test.xml";
//            InputStream ccdDocument = new FileInputStream(fileName);
            String prodDocId = "7136fd5b-b302-4cb9-a230-f907cf74cb37";
            InputStream ccdDocument = getInputStreamFromDocId(prodDocId,null);
            String htmlRepresentation = HTMLUtils.getHtmlFromCDA(ccdDocument);
            System.out.println(htmlRepresentation);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testCDAOCRPages()
    {
        try {
//            String fileName = "src/test/java/com/apixio/converters/ccda/parser/athena_ccda_2.xml";
            String fileName = "/Users/alarocca/Downloads/coverage_extraction_test.xml";
            InputStream ccdDocument = new FileInputStream(fileName);
            System.out.println(XMLUtils.getXMLRepresentation(ccdDocument, "CCD"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testCDAText()
    {
        try {
//            String fileName = "src/test/java/com/apixio/converters/ccda/parser/athena_ccda_2.xml";
//            InputStream ccdDocument = new FileInputStream(fileName);
            String prodDocId = "1dc91464-e93f-4166-9564-f5279d82796a";
            InputStream ccdDocument = getInputStreamFromDocId(prodDocId, null);
            System.out.println(TextExtractionUtils.extractText(ccdDocument, "CCD"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
