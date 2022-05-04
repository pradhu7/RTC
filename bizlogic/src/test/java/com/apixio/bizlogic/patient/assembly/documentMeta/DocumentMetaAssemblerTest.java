package com.apixio.bizlogic.patient.assembly.documentMeta;

import com.apixio.bizlogic.patient.assembly.SummaryTestUtils;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.*;
import com.apixio.security.Security;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by dyee on 4/27/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Security.class)
public class DocumentMetaAssemblerTest {
    private DocumentMetaAssembler documentMetaAssembler;

    @Before
    public void init() throws Exception {
        PowerMockito.mockStatic(Security.class);
        when(Security.getInstance()).thenReturn(null);
        documentMetaAssembler = new DocumentMetaAssembler();
    }

    protected Patient generateTestPatient() {
        Patient p = new Patient();
        p.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(10)));
        return p;
    }

    @Test
    public void testSeparate() {
        Patient patient1 = generateTestPatient();

        Document document1 = new Document();
        document1.setDocumentDate(DateTime.now().minusYears(10));
        document1.setDocumentTitle("Test Title");
        document1.setStringContent("hello there");

        patient1.addDocument(document1);

        List<Part<Patient>> partList =  documentMetaAssembler.separate(patient1);

        Assert.assertEquals(1, partList.size());

        Iterator<Document> documentIterator = partList.get(0).getPart().getDocuments().iterator();
        Assert.assertEquals(document1.getDocumentTitle(), documentIterator.next().getDocumentTitle());
        Assert.assertFalse(documentIterator.hasNext());

        //
        // Now add another document
        //

        Document document = new Document();
        document.setDocumentDate(DateTime.now().minusYears(5));
        document.setDocumentTitle("Test Title -20");
        document.setStringContent("hello there -20");

        patient1.addDocument(document);

        partList =  documentMetaAssembler.separate(patient1);

        Assert.assertEquals(2, partList.size());

        documentIterator = partList.get(0).getPart().getDocuments().iterator();
        Assert.assertEquals(document1.getDocumentTitle(), documentIterator.next().getDocumentTitle());
        Assert.assertFalse(documentIterator.hasNext());

        documentIterator = partList.get(1).getPart().getDocuments().iterator();
        Assert.assertEquals(document.getDocumentTitle(), documentIterator.next().getDocumentTitle());
        Assert.assertFalse(documentIterator.hasNext());
    }

    @Test
    public void testMerge() {

        //
        // STEP PATIENT #1
        //
        Patient patient1 = generateTestPatient();

        Document document = new Document();
        document.setDocumentDate(DateTime.now().minusYears(10));
        document.setDocumentTitle("Test Title");
        document.setStringContent("hello there");

        patient1.addDocument(document);

        //
        // STEP PATIENT #2
        //

        Patient patient2 = generateTestPatient();

        document = new Document();
        document.setDocumentDate(DateTime.now().minusYears(5));
        document.setDocumentTitle("Test Title -20");
        document.setStringContent("hello there -20");

        patient2.addDocument(document);

        //
        // Merge patients.. order matters...
        //

        Patient mergedPatient = documentMetaAssembler.merge(null, new PartEnvelope<>(patient1, 0L), new PartEnvelope<>(patient2, 0L)).part;

        //
        // Verify....
        //
        Iterator<Document> iterator = mergedPatient.getDocuments().iterator();

        Document mergedDocument = iterator.next();

        Document patient1Doc = patient1.getDocuments().iterator().next();
        Document patient2Doc = patient2.getDocuments().iterator().next();

        Assert.assertEquals(mergedDocument.getDocumentTitle(), patient2Doc.getDocumentTitle());

        mergedDocument = iterator.next();

        Assert.assertEquals(mergedDocument.getDocumentTitle(), patient1Doc.getDocumentTitle());

        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testMergeReverseDate() {
        Patient patient2 = generateTestPatient();

        Document document = new Document();
        document.setDocumentDate(DateTime.now().minusYears(5));
        document.setDocumentTitle("Test Title -20");
        document.setStringContent("hello there -20");

        patient2.addDocument(document);
        Patient patient1 = generateTestPatient();
        // put in future to make sure its the newest. this test was running so fast it would sometimes fail
        patient1.addSource(SummaryTestUtils.createSource(DateTime.now().plusYears(1)));

        document = new Document();
        document.setDocumentDate(DateTime.now().minusYears(10));
        document.setDocumentTitle("Test Title");
        document.setStringContent("hello there");

        patient1.addDocument(document);

        Patient mergedPatient = documentMetaAssembler.merge(null, new PartEnvelope<>(patient1, 0L), new PartEnvelope<>(patient2, 0L)).part;

        Iterator<Document> iterator = mergedPatient.getDocuments().iterator();

        Document mergedDocument = iterator.next();

        Document patient1Doc = patient1.getDocuments().iterator().next();
        Document patient2Doc = patient2.getDocuments().iterator().next();

        Assert.assertEquals(mergedDocument.getDocumentTitle(), patient1Doc.getDocumentTitle());

        mergedDocument = iterator.next();

        Assert.assertEquals(mergedDocument.getDocumentTitle(), patient2Doc.getDocumentTitle());

        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testTotalPagesPatientAssembly()
    {
        //
        // STEP PATIENT #1
        //
        Patient existing = generateTestPatient();
        DateTime sameSourceDate = new DateTime();

        UUID docUuid = UUID.randomUUID();

        Document document = new Document();
        document.setInternalUUID(docUuid);
        document.setDocumentDate(DateTime.now().minusYears(10));
        document.setDocumentTitle("Test Title");
        document.setStringContent("hello there");

        document.setMetaTag("totalPages", "-1");

        Source source = new Source();
        source.setCreationDate(sameSourceDate);
        document.setSourceId(source.getInternalUUID());
        existing.setSources(Collections.singletonList(source));

        ParsingDetail p = new ParsingDetail();
        p.setParsingDateTime(DateTime.now().minusYears(10));
        p.setParser(ParserType.APO);
        List<ParsingDetail> parsingDetails = new ArrayList<>();
        parsingDetails.add(p);
        existing.setParsingDetails(parsingDetails);
        document.setParsingDetailsId(p.getParsingDetailsId());
        document.setSourceId(source.getSourceId());

        existing.addDocument(document);

        //
        // STEP PATIENT #2
        //

        Patient newP = generateTestPatient();

        source = new Source();
        source.setCreationDate(sameSourceDate);
        document.setSourceId(source.getInternalUUID());
        newP.setSources(Collections.singletonList(source));

        p = new ParsingDetail();
        p.setParsingDateTime(DateTime.now().minusYears(5));
        p.setParser(ParserType.APO);
        parsingDetails = new ArrayList<>();
        parsingDetails.add(p);
        newP.setParsingDetails(parsingDetails);

        document = new Document();
        document.setParsingDetailsId(p.getParsingDetailsId());
        document.setInternalUUID(docUuid);
        DateTime newDate = DateTime.now().minusYears(5);
        document.setDocumentDate(newDate);
        String newTitle = "Test Title (new)";
        document.setDocumentTitle(newTitle);
        document.setStringContent("hello there (new)"); // not sure why we'd update string content....
        String newMeta = "200";
        document.setMetaTag("totalPages", newMeta);
        document.setSourceId(source.getSourceId());

        newP.addDocument(document);


        // add test to confirm that what we think is the 'new' part is actually considered new
        // the base patient should be the newer one
        //        public Patient basePatient;          // new object
        //        public Patient additionalPatient;    // old object
        PatientSet test = new PatientSet(existing, newP);
        Assert.assertEquals(test.basePatient.getDocuments().iterator().next().getMetaTag("totalPages"), newMeta);

        // Now merge

        Patient mergedPatient = documentMetaAssembler.merge(null, new PartEnvelope<>(existing, 0L), new PartEnvelope<>(newP, 0L)).part;

        verifySourcesAndParsingDetails(mergedPatient, Lists.newArrayList(mergedPatient.getDocuments()));

        Iterator<Document> docit = mergedPatient.getDocuments().iterator();
        Document mergedDoc = docit.next();
        Assert.assertFalse(docit.hasNext());
        Assert.assertEquals(docUuid, mergedDoc.getInternalUUID());
        Assert.assertEquals(newMeta, mergedDoc.getMetaTag("totalPages"));
        Assert.assertEquals(newDate, mergedDoc.getDocumentDate());
        Assert.assertEquals(newTitle, mergedDoc.getDocumentTitle());

//        System.out.println(mergedPatient);
    }

    protected void verifySourcesAndParsingDetails(Patient mergedPatient, List<Document> documentList)
    {
        //
        // All sources, and parsing details pointed to by the Clinical Actors are present
        //
        for(Document document: documentList)
        {
            UUID referencedSource = document.getSourceId();
            Assert.assertNotNull(mergedPatient.getSourceById(referencedSource));

            UUID referencedParsingDetail = document.getParsingDetailsId();
            Assert.assertNotNull(mergedPatient.getParsingDetailById(referencedParsingDetail));
        }
    }
}
