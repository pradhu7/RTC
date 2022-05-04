package com.apixio.bizlogic.document;

import com.apixio.bizlogic.BizLogicTestCategories;
import com.apixio.bizlogic.patient.DAOTestUtils;
import com.apixio.dao.blobdao.BlobDAO;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(BizLogicTestCategories.IntegrationTest.class)
public class DocumentLogicIntegrationTest {
    private DocumentLogic documentLogic;
    String doc_id = "8ba062bc-d655-4e91-a692-6b547e6f0fc2";
    String doc_pds_id = "1111";

    public BlobDAO getBlobDao() throws Exception {
        DAOTestUtils daoTestUtils = new DAOTestUtils();
        return daoTestUtils.daoServices.getBlobDAO();
    }

    @Test
    public void testConstructor() throws Exception {
        try {
            BlobDAO blobDAO = getBlobDao();
            documentLogic = new DocumentLogic(blobDAO);
        } catch (Throwable t) {
            fail("Creating an instance of DocumentLogic should not throw an exception.");
        }
        // This test merely checks for no exceptions to be thrown on
        // construction of the class under test.
    }

    @Test
    public void testGetDocString() throws Exception {
        BlobDAO blobDAO = getBlobDao();
        documentLogic = new DocumentLogic(blobDAO);
        String doc_text = documentLogic.getDocumentString(doc_pds_id, doc_id);
        assertNotNull(doc_text);
        assert(doc_text.length() > 0);
    }

    @Test
    public void testGetOcrDoc() throws Exception {
        BlobDAO blobDAO = getBlobDao();
        documentLogic = new DocumentLogic(blobDAO);
        com.apixio.model.ocr.page.Ocrextraction ocrextraction = documentLogic.getDocumentObject(doc_pds_id, doc_id);
        assertNotNull(ocrextraction);
        assertEquals(ocrextraction.getPages().getPage().size(), 2);
    }

    @Test
    public void testGetOcrDocHocr() throws Exception {
        BlobDAO blobDAO = getBlobDao();
        documentLogic = new DocumentLogic(blobDAO);
        com.apixio.model.ocr.page.Ocrextraction ocrextraction = documentLogic.getDocumentObject(doc_pds_id, doc_id);
        int pageIdx = 0;
        String hocr = DocumentLogic.getDocumentPageHocr(ocrextraction, pageIdx);
        assertNotNull(hocr);
        assertNotEquals(hocr, "");
        assertTrue(hocr.startsWith("<!DOCTYPE html"));
    }
}
