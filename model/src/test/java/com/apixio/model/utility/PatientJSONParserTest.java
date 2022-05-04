package com.apixio.model.utility;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import com.apixio.model.patient.Document;
import com.apixio.model.patient.Coverage;
import com.apixio.model.patient.CoverageType;
import com.apixio.model.patient.Source;
import com.apixio.model.patient.DocumentContent;
import com.apixio.model.patient.ParserType;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.ExternalID;

public class PatientJSONParserTest
{
    PatientJSONParser testParser;

    @Before
    public void setUp()
    {
        testParser = new PatientJSONParser();
    }

    @Test
    public void testParsingDetailMetadata() throws IOException
    {
        ParsingDetail p = new ParsingDetail();
        System.out.println(p.toString());

        Patient testPatient = new Patient();
        testPatient.addParsingDetail(p);

        UUID pid = p.getParsingDetailsId();

        assertNull(p.getSourceFileHash());
        assertNull(p.getSourceFileArchiveUUID());
        assertNull(testParser.parsePatientData(testParser.toJSON(testPatient)).getParsingDetailById(pid).getSourceFileHash());
        assertNull(testParser.parsePatientData(testParser.toJSON(testPatient)).getParsingDetailById(pid).getSourceFileArchiveUUID());

        p.setSourceFileHash("something");
        p.setSourceFileArchiveUUID(UUID.randomUUID());
        System.out.println(p.toString());

        assert(p.getSourceFileHash().equals(testParser.parsePatientData(testParser.toJSON(testPatient)).getParsingDetailById(pid).getSourceFileHash()));
        assert(p.getSourceFileArchiveUUID().equals(testParser.parsePatientData(testParser.toJSON(testPatient)).getParsingDetailById(pid).getSourceFileArchiveUUID()));
    }

    /**
     * Makes sure that serializer ignores properties with empty string values from serialization.
     */
    @Test
    public void testEmptyStringsAreExcludedFromJson() throws Exception {
        Patient p = new Patient();
        ExternalID eid = new ExternalID();
        eid.setId(""); // 'id' key is not serialized because its an empty string.
        eid.setAssignAuthority("123");
        p.setPrimaryExternalID(eid);

        String expected = "{\"primaryExternalID\":{\"assignAuthority\":\"123\"}}";
        String actual = testParser.toJSON(p);
        assertEquals(expected, actual);
    }

    /**
     * Makes sure that serializer keeps empty objects in json.
     */
    @Test
    public void testEmptyObjectsArePreservedInJson() throws Exception {
        Patient p = new Patient();
        ExternalID eid = new ExternalID();
        eid.setId(""); // 'id' key is not serialized because its an empty string.
        eid.setAssignAuthority(""); // 'assignAuthority' key is not serialized because its an empty string.
        p.setPrimaryExternalID(eid);

        String expected = "{\"primaryExternalID\":{}}"; // empty objects is still perserved in JSON though.
        String actual = testParser.toJSON(p);
        assertEquals(expected, actual);
    }

    /**
     * Makes sure that serializer ignores empty lists.
     */
    @Test
    public void testEmptyListsAreIgnoredInJson() throws Exception {
        Patient p = new Patient();
        ExternalID eid = new ExternalID();
        eid.setAssignAuthority("123");
        p.setPrimaryExternalID(eid);
        p.setAlternateDemographics(new ArrayList<>()); // not serialized because empty list

        String expected = "{\"primaryExternalID\":{\"assignAuthority\":\"123\"}}";
        String actual = testParser.toJSON(p);
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testParsePatientData1()
    {
        Patient testPatient = new Patient();

        testPatient.setPatientId(UUID.randomUUID());

        ParsingDetail p = new ParsingDetail();
        p.setParsingDetailsId(UUID.randomUUID());

        Document testDoc = new Document();
        testDoc.setParsingDetailsId(p.getParsingDetailsId());

        testPatient.addParsingDetail(p);
        testPatient.addDocument(testDoc);

        try
        {
            String json = testParser.toJSON(testPatient);

            assertNotNull(json);

            long currentTime = System.currentTimeMillis();

            Patient patient = testParser.parsePatientData(json);

            System.out.println("Time taken:"+(System.currentTimeMillis() - currentTime));

            assertNotNull(patient);
            assertNotNull(patient.getPatientId());
            assertEquals(testPatient.getPatientId(), patient.getPatientId());

            ParsingDetail testParse = testPatient.getParsingDetailById(testDoc.getParsingDetailsId());

            ParsingDetail actualParse = patient.getParsingDetailById(testDoc.getParsingDetailsId());

            assertNotNull(testParse);
            assertNotNull(actualParse);
            assertEquals(testParse.getParsingDetailsId(), actualParse.getParsingDetailsId());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParsePatientData2()
    {
        Patient testPatient = new Patient();

        testPatient.setPatientId(UUID.randomUUID());
        int iterations = 1000;

        for (int i = 0 ; i < iterations ; i++)
        {
            ParsingDetail p = new ParsingDetail();
            p.setParsingDetailsId(UUID.randomUUID());

            Document testDoc = new Document();
            testDoc.setParsingDetailsId(p.getParsingDetailsId());

            testPatient.addParsingDetail(p);
            testPatient.addDocument(testDoc);
        }
        try
        {
            String json = testParser.toJSON(testPatient);

            assertNotNull(json);

            long currentTime = System.currentTimeMillis();

            Patient patient = testParser.parsePatientData(json);

            System.out.println("Time taken :"+(System.currentTimeMillis() - currentTime)+"ms");

            assertNotNull(patient);
            assertNotNull(patient.getPatientId());
            assertEquals(testPatient.getPatientId(), patient.getPatientId());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParsePatientData3()
    {
        Patient testPatient = new Patient();

        testPatient.setPatientId(UUID.randomUUID());
        int iterations = 1;

        for (int i = 0 ; i < iterations ; i++)
        {
            ParsingDetail p = new ParsingDetail();
            p.setParsingDetailsId(UUID.randomUUID());
            p.setParser(ParserType.CCD);

            Document testDoc = new Document();

            Map<String, String> metadata = new HashMap<String, String>();
            metadata.put("testkey", "testvalue");
            testDoc.setMetadata(metadata);

            DocumentContent docContent = new DocumentContent();
            docContent.setContent("nethra".getBytes());

            List<DocumentContent> docContents = new ArrayList<DocumentContent>();
            docContents.add(docContent);

            testDoc.setParsingDetailsId(p.getParsingDetailsId());
            testDoc.setDocumentContents(docContents);

            testPatient.addParsingDetail(p);
            testPatient.addDocument(testDoc);
        }
        try
        {
            String json = testParser.toJSON(testPatient);
            //TODO use Json reader to read json fields.
            assertFalse(json.contains("problems"));
            assertTrue(json.contains("documents"));

            assertNotNull(json);

            long currentTime = System.currentTimeMillis();

            Patient patient = testParser.parsePatientData(json);

            System.out.println("Time taken :"+(System.currentTimeMillis() - currentTime)+"ms");

            assertNotNull(patient);
            assertNotNull(patient.getPatientId());
            assertEquals(testPatient.getPatientId(), patient.getPatientId());

            assertNotNull(patient.getDocuments());
            assertNotNull(patient.getDocuments().iterator());
            assertNotNull(patient.getDocuments().iterator().next());

            //Check for parser Type.
            ParsingDetail testParsingDetail = patient.getParsingDetailById(patient.getDocuments().iterator().next().getParsingDetailsId());
            assertNotNull(testParsingDetail);
            assertEquals(testParsingDetail.getParser(), ParserType.CCD);

            assertNotNull(patient.getDocuments().iterator().next().getDocumentContents());
            assertNotNull(patient.getDocuments().iterator().next().getDocumentContents().get(0));
            assertNull(patient.getDocuments().iterator().next().getDocumentContents().get(0).getContent());

            assertNotNull(patient.getDocuments().iterator().next().getMetaTag("testkey"));
            assertEquals(patient.getDocuments().iterator().next().getMetaTag("testkey"), "testvalue");

            assertNotNull(patient.getProblems());
            assertFalse(patient.getProblems().iterator().hasNext());

            assertNull(patient.getPrimaryExternalID());
            assertNotNull(patient.getMetadata());
            assertTrue(patient.getMetadata().isEmpty());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_coverage_and_source_models() throws Exception
    {
        Patient originalPatient = new Patient();
        originalPatient.setPrimaryExternalID(createExternalId("123", "PAT_AA"));
        Source source = createSource("2016-01-01", "2015-01-01", "2015-12-31");
        Coverage coverage = createCoverage(source, "CMS", "123^^HICN", "2015-02-01", "2015-03-01");
        originalPatient.addSource(source);
        originalPatient.addCoverage(coverage);

        String json = testParser.toJSON(originalPatient);
        assertFalse(json.contains("problems"));
        assertTrue(json.contains("coverage"));
        assertNotNull(json);

        System.out.println(json);

        long currentTime = System.currentTimeMillis();

        Patient restoredPatient = testParser.parsePatientData(json);

        System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime) + "ms");

        assertEquals(coverage, restoredPatient.getCoverage().iterator().next());
        assertEquals(source, restoredPatient.getSources().iterator().next());
        assertEquals(originalPatient.getPrimaryExternalID(), restoredPatient.getPrimaryExternalID());
    }

//    @Test TODO
//    public void testParsePatientData_External() throws IOException
//    {
//        Patient patient = testParser.parsePatientData(new FileInputStream(new File("/tmp/data.json")));
//
//        System.out.println(patient.toString());
//    }

    @Test
    public void testParsePatientData4()
    {
        String testStr = "{\"metadata\":{\"testkey\":\"testvalue\"},\"problems\":[],\"procedures\":[],\"documents\":[{\"metadata\":{},\"sourceId\":null,\"parsingDetailsId\":\"02b74df5-de30-44ea-8e9c-c2c85cfb1958\",\"internalUUID\":\"17a2b606-1977-461e-9c6a-b393ff776d0b\",\"originalId\":null,\"otherOriginalIds\":[],\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2014-04-18T16:52:47.992-07:00\",\"primaryClinicalActorId\":null,\"supplementaryClinicalActorIds\":[],\"sourceEncounter\":null,\"code\":null,\"codeTranslations\":[],\"documentTitle\":null,\"stringContent\":null,\"documentContents\":[{\"metadata\":{},\"sourceId\":null,\"parsingDetailsId\":null,\"internalUUID\":\"47315053-75a6-45aa-a7df-791bc543f99f\",\"originalId\":null,\"otherOriginalIds\":[],\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2014-04-18T16:52:48.078-07:00\",\"length\":0,\"hash\":null,\"mimeType\":null,\"uri\":null,\"content\":\"bmV0aHJh\"}],\"documentDate\":null}],\"prescriptions\":[],\"administrations\":[],\"labs\":[],\"apixions\":[],\"biometricValues\":[],\"allergies\":[],\"patientId\":\"ac05d596-fa19-44bb-b20e-eb9f826a48a3\",\"primaryExternalID\":null,\"externalIDs\":[],\"primaryDemographics\":null,\"alternateDemographics\":[],\"primaryContactDetails\":null,\"alternateContactDetails\":[],\"familyHistories\":[],\"socialHistories\":[],\"clinicalEvents\":[],\"encounters\":[],\"sources\":[],\"parsingDetails\":[{\"metadata\":{},\"parsingDetailsId\":\"02b74df5-de30-44ea-8e9c-c2c85cfb1958\",\"contentSourceDocumentType\":null,\"contentSourceDocumentURI\":null,\"parser\":\"CCD\",\"parserVersion\":null,\"parsingDateTime\":null}],\"clinicalActors\":[],\"careSites\":[]}";
        try
        {
            Patient patient = testParser.parsePatientData(testStr);

            assertNotNull(patient);
            assertNotNull(patient.getPatientId());

            assertNotNull(patient.getDocuments());
            assertNotNull(patient.getDocuments().iterator());
            assertNotNull(patient.getDocuments().iterator().next());
            assertNotNull(patient.getDocuments().iterator().next().getDocumentContents());
            assertNotNull(patient.getDocuments().iterator().next().getDocumentContents().get(0));
            assertNull(patient.getDocuments().iterator().next().getDocumentContents().get(0).getContent());

            assertNotNull(patient.getMetadata());
            assertNotNull(patient.getMetaTag("testkey"));
            assertEquals(patient.getMetaTag("testkey"), "testvalue");

            assertNotNull(patient.getProblems());
            assertFalse(patient.getProblems().iterator().hasNext());

            assertNull(patient.getPrimaryExternalID());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParsePatientData5()
    {
        String testStr = "{\"metadata\":{\"testkey\":\"testvalue\"},\"problems\":[],\"procedures\":[],\"documents\":[{\"metadata\":{},\"sourceId\":null,\"parsingDetailsId\":\"02b74df5-de30-44ea-8e9c-c2c85cfb1958\",\"internalUUID\":\"17a2b606-1977-461e-9c6a-b393ff776d0b\",\"originalId\":null,\"otherOriginalIds\":[],\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2014-04-18T16:52:47.992-07:00\",\"primaryClinicalActorId\":null,\"supplementaryClinicalActorIds\":[],\"sourceEncounter\":null,\"code\":null,\"codeTranslations\":[],\"documentTitle\":null,\"stringContent\":null,\"documentContents\":[{\"metadata\":{},\"sourceId\":null,\"parsingDetailsId\":null,\"internalUUID\":\"47315053-75a6-45aa-a7df-791bc543f99f\",\"originalId\":null,\"otherOriginalIds\":[],\"editType\":\"ACTIVE\",\"lastEditDateTime\":\"2014-04-18T16:52:48.078-07:00\",\"length\":0,\"hash\":null,\"mimeType\":null,\"uri\":null,\"content\":\"bmV0aHJh\"}],\"documentDate\":null}],\"prescriptions\":[],\"administrations\":[],\"labs\":[],\"apixions\":[],\"biometricValues\":[],\"allergies\":[],\"patientId\":\"ac05d596-fa19-44bb-b20e-eb9f826a48a3\",\"primaryExternalID\":null,\"externalIDs\":[],\"primaryDemographics\":null,\"alternateDemographics\":[],\"primaryContactDetails\":null,\"alternateContactDetails\":[],\"familyHistories\":[],\"socialHistories\":[],\"clinicalEvents\":[],\"encounters\":[],\"sources\":[],\"parsingDetails\":[{\"metadata\":{},\"parsingDetailsId\":\"02b74df5-de30-44ea-8e9c-c2c85cfb1958\",\"contentSourceDocumentType\":null,\"contentSourceDocumentURI\":null,\"parser\":\"CCD\",\"parserVersion\":null,\"parsingDateTime\":null}],\"clinicalActors\":[],\"careSites\":[]}";
        try
        {
            Patient patient = testParser.parsePatientData(testStr);

            assertNotNull(patient);
            assertNotNull(patient.getPatientId());

            assertNotNull(patient.getDocuments());
            assertNotNull(patient.getDocuments().iterator());
            assertNotNull(patient.getDocuments().iterator().next());
            assertNotNull(patient.getDocuments().iterator().next().getMetadata());
            assertTrue(patient.getDocuments().iterator().next().getMetadata().isEmpty());

            assertNotNull(patient.getDocuments().iterator().next().getDocumentContents());
            assertNotNull(patient.getDocuments().iterator().next().getDocumentContents().get(0));
            assertNull(patient.getDocuments().iterator().next().getDocumentContents().get(0).getContent());

            assertNotNull(patient.getMetadata());
            assertNotNull(patient.getMetaTag("testkey"));
            assertEquals(patient.getMetaTag("testkey"), "testvalue");

            assertNotNull(patient.getProblems());
            assertFalse(patient.getProblems().iterator().hasNext());

            assertNull(patient.getPrimaryExternalID());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParsePatientData6()
    {
        String testStr = "{\"metadata\":{\"testkey\":\"testvalue\"},\"backwards\":[{\"compatible\":1}],\"coverage\":[{\"sequenceNumber\":1,\"coverageType\":\"HMO\"},{\"sequenceNumber\":1,\"coverageType\":\"HMO\"}],\"problems\":[],\"procedures\":[],\"documents\":[],\"prescriptions\":[],\"administrations\":[],\"labs\":[],\"apixions\":[],\"biometricValues\":[],\"allergies\":[],\"patientId\":\"ac05d596-fa19-44bb-b20e-eb9f826a48a3\",\"primaryExternalID\":null,\"externalIDs\":[],\"primaryDemographics\":null,\"alternateDemographics\":[],\"primaryContactDetails\":null,\"alternateContactDetails\":[],\"familyHistories\":[],\"socialHistories\":[],\"clinicalEvents\":[],\"encounters\":[],\"sources\":[],\"parsingDetails\":[{\"metadata\":{},\"parsingDetailsId\":\"02b74df5-de30-44ea-8e9c-c2c85cfb1958\",\"contentSourceDocumentType\":null,\"contentSourceDocumentURI\":null,\"parser\":\"CCD\",\"parserVersion\":null,\"parsingDateTime\":null}],\"clinicalActors\":[],\"careSites\":[]}";
        try
        {
            Patient patient = testParser.parsePatientData(testStr);

            assertNotNull(patient);
            assertNotNull(patient.getPatientId());

            assertNotNull(patient.getCoverage());
            assertNotNull(patient.getCoverage().iterator());
            assertNotNull(patient.getCoverage().iterator().next());
            assertEquals(patient.getCoverage().iterator().next().getSequenceNumber(), 1);

            assertNull(patient.getPrimaryExternalID());

        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    /**
     * Tests that PatientJSONParser intercepts Jackson parsing exceptions and strips PHI out of them.
     *
     * Normally Jackson will print part of the PHI string where the parse error happened.
     */
    @Test
    public void deserialization_exception_must_not_contain_phi() throws Exception
    {
        String testStr = "{\"coverage\":[{\"type\":\"unknownEnumValue\"}]}";
        try
        {
            Patient patient = testParser.parsePatientData(testStr);
        }
        catch (JsonProcessingException e)
        {
            assertTrue(e.toString().contains("com.apixio.model.patient.CoverageType"));
            assertTrue(e.toString().contains("unknownEnumValue")); // ideally we would be able to remove this part of the error message, but not sure how to do it reliably as the _value in the exception is protected & final
            assertTrue(e.toString().contains("not one of the values accepted for Enum class"));
        }
    }

    private Coverage createCoverage(Source source, String healthPlan, String subscriberId, String start, String end)
    {
        Coverage coverage = new Coverage();
        String[] parts = subscriberId.split("\\^\\^");
        coverage.setSubscriberID(createExternalId(parts[0], parts[1]));
        coverage.setStartDate(LocalDate.parse(start));
        coverage.setEndDate(LocalDate.parse(end));
        coverage.setHealthPlanName(healthPlan);
        coverage.setSourceId(source.getInternalUUID());
        coverage.setType(CoverageType.CMS);
        return coverage;
    }

    private Source createSource(String createdAt, String dciStart, String dciEnd)
    {
        Source source = new Source();
        source.setSourceId(source.getInternalUUID());
        source.setCreationDate(DateTime.parse(createdAt));
        source.setDciStart(LocalDate.parse(dciStart));
        source.setDciEnd(LocalDate.parse(dciEnd));
        return source;
    }

    private ExternalID createExternalId(String id, String assigningAuthority)
    {
        ExternalID externalId = new ExternalID();
        externalId.setId(id);
        externalId.setAssignAuthority(assigningAuthority);
        return externalId;
    }
}
