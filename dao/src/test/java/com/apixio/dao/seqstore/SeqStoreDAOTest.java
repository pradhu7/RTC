package com.apixio.dao.seqstore;

import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.seqstore.utility.Criteria;
import com.apixio.dao.seqstore.utility.Range;
import com.apixio.dao.seqstore.utility.SeqStoreCFUtility;
import com.apixio.dao.seqstore.utility.SeqStoreCf;
import com.apixio.model.event.*;
import com.apixio.model.event.transformer.EventTypeJSONParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Iterator;
import java.util.Calendar;

import com.apixio.utility.StringUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.apixio.dao.DAOTestUtils;

import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.model.patient.Patient;

import static org.junit.Assert.*;

@Ignore("Broken")
public class SeqStoreDAOTest
{
    private SeqStoreDAO sdao;
    // private PatientDAO2 pdao;
    private CqlCache cache;

    private String org;

    private DAOTestUtils util;

    private EventTypeJSONParser jsonParser = new EventTypeJSONParser();

    private int repeats = 10;

    private SeqStoreCFUtilityOverride utility = new SeqStoreCFUtilityOverride();

    //Extend this class in the test, so we can use the same method to convert
    //eventType --> tag
    static class SeqStoreCFUtilityOverride extends SeqStoreCFUtility
    {
        public String getTagByEventType(EventType eventType)
        {
            return this.getTag(eventType);
        }
    }

    private void setAllPropertiesHaveDataFalse() throws Exception
    {
        Map<String, String> seqStoreMap = util.getAllColumnFamiliesGut(org);

        Map<String, String> falseMap = new HashMap<>();
        for(Map.Entry<String, String> entry : seqStoreMap.entrySet()) {
            falseMap.put(entry.getKey(), entry.getValue().replace("true", "false"));
        }

        util.setSequenceStoreColumnFamily(StringUtil.mapToString(falseMap));
    }

    private void setAllPropertiesHaveDataTrue() throws Exception
    {
        Map<String, String> seqStoreMap = util.getAllColumnFamiliesGut(org);

        Map<String, String> trueMap = new HashMap<>();
        for(Map.Entry<String, String> entry : seqStoreMap.entrySet()) {
            trueMap.put(entry.getKey(), entry.getValue().replace("false", "true"));
        }

        util.setSequenceStoreColumnFamily(StringUtil.mapToString(trueMap));
    }

    @Before
    public void setUp() throws Exception
    {
        util = new DAOTestUtils();
        cache = util.daoServices.getScienceCqlCrud().getCqlCache();
        // pdao = util.daoServices.getPatientDAO();
        sdao = util.daoServices.getSeqStoreDAO();

        org = util.testCassandraTables.testOrg;
    }

    @Test
    public void testAddAndGetEventsBySubject_CreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubject(false, true, false);
    }

    @Test
    public void testAddAndGetEventsBySubject_DoNotCreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubject(false,false, false);
    }

    @Test
    public void testAddAndGetEventsBySubject_CreateNewCF_DeleteCF() throws Exception
    {
        testAddAndGetEventsBySubject(false, true, true);
    }

    @Test
    public void testAddAndGetEventsBySubject_DoNotCreateNewCF_DeleteCF() throws Exception
    {
        testAddAndGetEventsBySubject(false,false, true);
    }

    @Test
    public void testAddAndGetEventsBySubject_InitTrue_CreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubject(true, true, false);
    }

    @Test
    public void testAddAndGetEventsBySubject_InitTrue_DoNotCreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubject(true,false, false);
    }

    @Test
    public void testAddAndGetEventsBySubject_InitTrue_CreateNewCF_DeleteCF() throws Exception
    {
        testAddAndGetEventsBySubject(true, true, true);
    }

    @Test
    public void testAddAndGetEventsBySubject_InitTrue_DoNotCreateNewCF_DeleteCF() throws Exception
    {
        testAddAndGetEventsBySubject(true,false, true);
    }

    private void testAddAndGetEventsBySubject(boolean propertiesSetToTrueInitially, boolean createNewInferredCF, boolean deleteCF) throws Exception
    {
        // Test it multiple times
        for (int i = 0; i < repeats; i++)
        {
            if (propertiesSetToTrueInitially)
            {
                setAllPropertiesHaveDataTrue();
            }
            else
            {
                setAllPropertiesHaveDataFalse();
            }

            try
            {
                String patientId = String.valueOf(Math.random());

                EventType event1 = createEventType(getDate(i - 5), patientId); Thread.sleep(100);
                EventType event2 = createEventType(getDate(i - 5), patientId); Thread.sleep(100);
                EventType event3 = createEventType(getDate(i - 5), patientId); Thread.sleep(100);
                EventType event4 = createEventType(getDate(i - 5), patientId); Thread.sleep(100);

                List<EventType> eventTypes = new ArrayList<>();

                eventTypes.add(event1);
                eventTypes.add(event2);
                eventTypes.add(event3);
                eventTypes.add(event4);


                sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);


                sdao.put(eventTypes, org);

                markSequenceStoresHaveData();

                cache.flush();

                Thread.sleep(5000);

                List<EventType> events = sdao.getSequence(event1.getSubject(), 0, 0, org); // todo: need to test this with a date range
                assertNotNull(events);
                assertEquals(events.size(), 4);

                Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(event1.getSubject())
                        .setTagType(SeqStoreDAO.TagType.all).setOrgId(org)
                        .setRange(new Range.RangeBuilder().setStart(0).setEnd(0).build()).build();

                List<Iterator<EventType>> iteratorList = sdao.getIteratorSequence(criteria);
                assertNotNull(iteratorList);

                int size = 0;
                for(Iterator<EventType> iter : iteratorList)
                {
                    while (iter.hasNext())
                    {
                        iter.next();
                        size++;
                    }
                }

                assertEquals(4, size);

                if(deleteCF)
                {
                    sdao.deleteSequenceStoreColumnFamily(utility.getTagByEventType(event1), org);
                }
                else
                {
                    setAllPropertiesHaveDataFalse();
                }

                events = sdao.getSequence(event1.getSubject(), 0, 0, org); // todo: need to test this with a date range
                assertNotNull(events);



                if(!createNewInferredCF && deleteCF)
                {
                    //it will be either 0 or 4, depending on whether we set the initial historical cf
                    //hasdata flag to true or false...
                    assertEquals(propertiesSetToTrueInitially ? 4 : 0, events.size());
                }
                else
                {
                    assertEquals(0, events.size());
                }

                //System.out.println(events);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testAddAndGetEventsBySubjectUsingIterator_CreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubjectUsingIterator(true);
    }

    @Test
    public void testAddAndGetEventsBySubjectUsingIterator_DoNotCreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubjectUsingIterator(false);
    }

    private void testAddAndGetEventsBySubjectUsingIterator(boolean createNewInferredCF) throws Exception
    {
        String patientId = String.valueOf(Math.random());

        // Test it multiple times
        for (int i = 0; i < repeats; i++)
        {
            try
            {
                //setAllPropertiesHaveDataTrue();

                EventType event1 = createEventType(getDate(i - 5), patientId);  Thread.sleep(100);
                EventType event2 = createEventType(getDate(i - 5), patientId);  Thread.sleep(100);
                EventType event3 = createEventType(getDate(i - 5), patientId);  Thread.sleep(100);
                EventType event4 = createEventType(getDate(i - 5), patientId);  Thread.sleep(100);

                List<EventType> eventTypes = new ArrayList<>();

                eventTypes.add(event1);
                eventTypes.add(event2);
                eventTypes.add(event3);
                eventTypes.add(event4);

                sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);

                sdao.put(eventTypes, org);

                markSequenceStoresHaveData();

                cache.flush();

                Thread.sleep(5000);

                Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(event1.getSubject())
                        .setTagType(SeqStoreDAO.TagType.all).setOrgId(org)
                        .setRange(new Range.RangeBuilder().setStart(0).setEnd(0).build()).build();


                List<EventType> events = sdao.getSequence(criteria);
                assertNotNull(events);
                assertEquals(4*(i+1), events.size());


                List<Iterator<EventType>> iteratorList = sdao.getIteratorSequence(criteria);
                assertNotNull(iteratorList);

                int size = 0;
                for(Iterator<EventType> iter : iteratorList)
                {
                    while (iter.hasNext())
                    {
                        iter.next();
                        size++;
                    }
                }

                assertEquals(4*(i+1), size);

                assertEquals(events.size(), size);


                setAllPropertiesHaveDataFalse();

                criteria = new Criteria.TagTypeCriteria.Builder().setSubject(event1.getSubject())
                        .setTagType(SeqStoreDAO.TagType.all).setOrgId(org)
                        .setRange(new Range.RangeBuilder().setStart(0).setEnd(0).build()).build();

                iteratorList = sdao.getIteratorSequence(criteria);
                assertNotNull(iteratorList);

                size = 0;
                for(Iterator<EventType> iter : iteratorList)
                {
                    while (iter.hasNext())
                    {
                        iter.next();
                        size++;
                    }
                }

                assertEquals(size, 0);


                events = sdao.getSequence(criteria);
                assertNotNull(events);
                assertEquals(events.size(), 0);


                assertEquals(events.size(), size);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testAddAndGetEventsBySubjectFromMultiplePeriods_CreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubjectFromMultiplePeriods(true);
    }

    @Test
    public void testAddAndGetEventsBySubjectFromMultiplePeriods_Do_Not_CreateNewCF() throws Exception
    {
        testAddAndGetEventsBySubjectFromMultiplePeriods(false);
    }

    private void testAddAndGetEventsBySubjectFromMultiplePeriods(boolean createNewInferredCF) throws Exception
    {
        // Test it in multiple periods with the same patientId

        String patientId = String.valueOf(Math.random());
        int yearsBack = 5;

        for (int year = -1*yearsBack; year < 0; year++)
        {
            try
            {
                setAllPropertiesHaveDataTrue();

                EventType event1 = createEventType(getDate(year), patientId); Thread.sleep(100);
                EventType event2 = createEventType(getDate(year), patientId); Thread.sleep(100);
                EventType event3 = createEventType(getDate(year), patientId); Thread.sleep(100);
                EventType event4 = createEventType(getDate(year), patientId); Thread.sleep(100);

                List<EventType> eventTypes = new ArrayList<>();

                eventTypes.add(event1);
                eventTypes.add(event2);
                eventTypes.add(event3);
                eventTypes.add(event4);

                sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);

                sdao.put(eventTypes, org);

                markSequenceStoresHaveData();

                cache.flush();

                Thread.sleep(5000);

                List<EventType> events = sdao.getSequence(event1.getSubject(), 0, 0, org);
                assertNotNull(events);
                assertEquals(events.size(), 4 * (year + yearsBack+1));

                setAllPropertiesHaveDataFalse();
                events = sdao.getSequence(event1.getSubject(), 0, 0, org);
                assertNotNull(events);
                assertEquals(events.size(), 0);


                //System.out.println(events);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testAddAndGetEventsBySubjectWithLink() throws Exception
    {
        UUID patientUUID = UUID.randomUUID();
        Patient apo = new Patient();
        String partKey = "testPutAndCheck-" + Math.random();

        String docHash = "testDocHash-" + Math.random();
        String docId = "testDocId-" + Math.random();
        UUID docUUID = UUID.randomUUID();
        List<UUID> docUUIDs = new ArrayList<>();
        docUUIDs.add(docUUID);

        try {
            // String partKeyHash = pdao.createPartialPatient(partKey, docId, docUUID, apo, org);
            // pdao.linkPartialPatient(patientUUID, docUUIDs, partKeyHash, org);
            cache.flush();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Date date = new Date();

        EventType event1 = createEventType(date, patientUUID.toString());
        EventType event2 = createEventType(date, patientUUID.toString());
        EventType event3 = createEventType(date, patientUUID.toString());
        EventType event4 = createEventType(date, patientUUID.toString());

        List<EventType> eventTypes = new ArrayList<>();

        eventTypes.add(event1);
        eventTypes.add(event2);
        eventTypes.add(event3);
        eventTypes.add(event4);

        try
        {
            boolean createNewInferredCF = true;

            sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);

            sdao.put(eventTypes, null);

            markSequenceStoresHaveData();

            cache.flush();

            Thread.sleep(5000);

            List<EventType> events = sdao.getSequence(event1.getSubject(), 0, 0, null);
            assertNotNull(events);
            assertEquals(events.size(), 4);

            sdao.deleteSequenceStoreColumnFamily(utility.getTagByEventType(event1), org);

            cache.flush();
            Thread.sleep(5000);

            events = sdao.getSequence(event1.getSubject(), 0, 0, null);
            assertNotNull(events);
            assertEquals(events.size(), 0);

            //System.out.println(events);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testAddAndGetEventsByAddress() throws Exception
    {
        // Test it multiple times
        for (int i = 0; i < repeats; i++)
        {
            Date date = new Date();
            String patientId = String.valueOf(Math.random());

            EventType event1 = createEventType(date, patientId);

            List<EventType> eventTypes = new ArrayList<>();

            eventTypes.add(event1);

            try
            {
                boolean createNewInferredCF = true;

                sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);
                sdao.put(eventTypes, org);

                markSequenceStoresHaveData();

                cache.flush();

                Thread.sleep(5000);

                EventType event1persisted = sdao.getAddressableSequenceElement(EventAddress.getEventAddress(event1), org);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testAddAndGetEventsBySubjectAndAttributeValue() throws Exception
    {
        // Test it multiple times
        for (int i = 0; i < repeats; i++)
        {
            setAllPropertiesHaveDataTrue();

            Date date = getDate(i - 5);
            String patientId = String.valueOf(Math.random());

            EventType event1 = createEventType(date, patientId);
            EventType event2 = createEventType(date, patientId);
            EventType event3 = createEventType(date, patientId);
            EventType event4 = createEventType(date, patientId);

            List<EventType> eventTypes = new ArrayList<>();

            eventTypes.add(event1);
            eventTypes.add(event2);
            eventTypes.add(event3);
            eventTypes.add(event4);

            try
            {
                boolean createNewInferredCF = true;

                sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);
                sdao.setPaths("attributes.a1");
                sdao.put(eventTypes, org);

                markSequenceStoresHaveData();
                cache.flush();

                Thread.sleep(5000);

                Criteria.TagCriteria criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                        .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1)))
                        .setPath("attributes.a1").setValue("v1").build();
                List<EventType> events = sdao.getSequence(criteria);

                assertNotNull(events);
                assertEquals(4, events.size());

                //
                // Negative test, with new syntax
                //
                criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                        .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1)))
                        .setPath("attributes.a1").setValue("v2").build();
                events = sdao.getSequence(criteria);
                assertEquals(0, events.size());

                setAllPropertiesHaveDataFalse();

                criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                        .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1)))
                        .setPath("attributes.a1").setValue("v1").build();
                events = sdao.getSequence(criteria);

                assertNotNull(events);
                assertEquals(0, events.size());

                //
                // Negative test, with new syntax
                //
                criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                        .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1)))
                        .setPath("attributes.a1").setValue("v2").build();
                events = sdao.getSequence(criteria);
                assertEquals(0, events.size());

            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testAddAndGetEvents_Iterator() throws Exception
    {
        setAllPropertiesHaveDataTrue();

        Date date = getDate(  5);
        String patientId = String.valueOf(Math.random());

        EventType event1 = createEventType(date, patientId);
        EventType event2 = createEventType(date, patientId);
        EventType event3 = createEventType(date, patientId);
        EventType event4 = createEventType(date, patientId);

        List<EventType> eventTypes = new ArrayList<>();

        eventTypes.add(event1);
        eventTypes.add(event2);
        eventTypes.add(event3);
        eventTypes.add(event4);

        try
        {
            boolean createNewInferredCF = true;

            sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);

            sdao.put(eventTypes, org);

            markSequenceStoresHaveData();
            cache.flush();

            Thread.sleep(5000);

            Criteria.TagCriteria criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                    .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1))).build();
            List<EventType> events = sdao.getSequence(criteria);

            assertNotNull(events);
            assertEquals(4, events.size());


            List<Iterator<EventType>> iteratorList = sdao.getIteratorSequence(criteria);
            assertNotNull(iteratorList);

            int size = 0;
            for(Iterator<EventType> iter : iteratorList)
            {
                while (iter.hasNext())
                {
                    iter.next();
                    size++;
                }
            }

            assertEquals(4, size);

            setAllPropertiesHaveDataFalse();


            criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                    .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1))).build();
            events = sdao.getSequence(criteria);

            assertNotNull(events);
            assertEquals(0, events.size());


            iteratorList = sdao.getIteratorSequence(criteria);
            assertNotNull(iteratorList);

            size = 0;
            for(Iterator<EventType> iter : iteratorList)
            {
                while (iter.hasNext())
                {
                    iter.next();
                    size++;
                }
            }

            assertEquals(0, size);

            assertEquals(size, events.size());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testAddAndGetEvents() throws Exception
    {
        // Test it multiple times
        for (int i = 0; i < repeats; i++)
        {
            setAllPropertiesHaveDataTrue();

            Date date = getDate(i - 5);
            String patientId = String.valueOf(Math.random());

            EventType event1 = createEventType(date, patientId);
            EventType event2 = createEventType(date, patientId);
            EventType event3 = createEventType(date, patientId);
            EventType event4 = createEventType(date, patientId);

            List<EventType> eventTypes = new ArrayList<>();

            eventTypes.add(event1);
            eventTypes.add(event2);
            eventTypes.add(event3);
            eventTypes.add(event4);

            try
            {
                boolean createNewInferredCF = true;

                sdao.createSequenceStoreColumnFamily(utility.getTagByEventType(event1), createNewInferredCF, org);

                sdao.put(eventTypes, org);
                markSequenceStoresHaveData();
                cache.flush();

                Thread.sleep(5000);

                Criteria.TagCriteria criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                        .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1))).build();
                List<EventType> events = sdao.getSequence(criteria);

                assertNotNull(events);
                assertEquals(4, events.size());

                setAllPropertiesHaveDataFalse();
                criteria = new Criteria.TagCriteria.Builder().setSubject(event1.getSubject()).setOrgId(org)
                        .setRange(Range.DEFAULT_RANGE).setTags(Collections.singletonList(utility.getTagByEventType(event1))).build();
                events = sdao.getSequence(criteria);

                assertNotNull(events);
                assertEquals(0, events.size());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testPutAndCheck() throws Exception
    {
        setAllPropertiesHaveDataTrue();

        UUID patientUUID = UUID.randomUUID();
        String annotationEventJSON = "{\"attributes\":{\"SOURCE_TYPE\":\"USER_ANNOTATION\",\"$patientUUID\":\""+patientUUID.toString()+"\",\"$documentUUID\":\"4064e598-cb8e-4632-9250-f37c8ebf834b\",\"sourceType\":\"USER_ANNOTATION\"},\"source\":{\"type\":\"document\",\"uri\":\"4064e598-cb8e-4632-9250-f37c8ebf834b\"},\"evidence\":{\"source\":{\"type\":\"user\",\"uri\":\"bfabros@apexcodemine.com\"},\"inferred\":false,\"attributes\":{\"presentedHcc\":\"96\",\"comment\":\"\",\"code\":\"\",\"physicianLasttName\":\"\",\"dateOfService\":\"06/07/2013\",\"encounterType\":\"\",\"physicianFirstName\":\"\",\"hccModelRun\":\"Final\",\"codeSystem\":\"\",\"hcc\":\"96\",\"pageNumber\":\"\",\"hccPaymentYear\":\"2014\",\"hccDisplayName\":\"Ischemic or Unspecified Stroke\",\"timestamp\":\"2015-01-26T00:22:15.918Z\",\"presentedHccDescription\":\"Ischemic or Unspecified Stroke\",\"presentedHccLabelSetVersion\":\"V12\",\"presentedHccMappingVersion\":\"2013 Final\",\"codeSystemVersion\":\"\",\"NPI\":\"\",\"physicianMiddleName\":\"\",\"hccModelYear\":\"2013\",\"project\":\"\",\"organization\":\"Hometown Health\"}},\"fact\":{\"code\":{\"codeSystemVersion\":\"2.0\",\"code\":\"V12_96\",\"displayName\":\"V12_96\",\"codeSystem\":\"APXCAT\"},\"values\":{\"rejectReason\":\"This specific HCC is not mentioned in the document\",\"result\":\"reject\"},\"time\":{\"endTime\":\"2013-06-07T07:00:00+0000\",\"startTime\":\"2013-06-07T07:00:00+0000\"}},\"subject\":{\"type\":\"patient\",\"uri\":\""+patientUUID.toString()+"\"}}";

        EventType event = null;
        try {
            event = jsonParser.parseEventTypeData(annotationEventJSON);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        sdao.setPaths("attributes.sourceType,source.type,attributes.bucketName");

        List<EventType> events = new ArrayList<>();
        events.add(event);
        try {
            sdao.putAndCheck(events, org);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        ReferenceType r = new ReferenceType();
        r.setUri(patientUUID.toString());
        r.setType("patient");

        assertEquals(sdao.getSequence(r, "attributes.sourceType", "USER_ANNOTATION", 0L, 0L, org).size(), 1);

        setAllPropertiesHaveDataFalse();
        assertEquals(sdao.getSequence(r, "attributes.sourceType", "USER_ANNOTATION", 0L, 0L, org).size(), 0);

    }

    // you shouldn't be testing in production, vram -lilith
//    @Test
//    public void testProduction() throws Exception
//    {
//        ReferenceType r = new ReferenceType();
//        r.setUri("000036ba-90a7-43d8-93a4-3c681dee47f7");
//        r.setType("patient");
//
//        List<EventType> l1 = sdao.getSequence(r, 0L, 0L, "10000263");
//        List<EventType> l2 = sdao.getSequence(r, 1404226800000L, 1435676400000L, "10000263");
//
//        System.out.println(l1.size());
//        System.out.println(l2.size());
//        // sdao.getSequence(r, 1404226800000L, 1435676400000L, null);
//    }

    @Test
    public void testPutAndCheckWithLink() throws Exception
    {
        UUID patientUUID = UUID.randomUUID();
        Patient apo = new Patient();
        String partKey = "testPutAndCheck-" + Math.random();

        String docHash = "testDocHash-" + Math.random();
        String docId = "testDocId-" + Math.random();
        UUID docUUID = UUID.randomUUID();
        List<UUID> docUUIDs = new ArrayList<>();
        docUUIDs.add(docUUID);

        try {
            // String partKeyHash = pdao.createPartialPatient(partKey, docId, docUUID, apo, org);
            // pdao.linkPartialPatient(patientUUID, docUUIDs, partKeyHash, org);
            cache.flush();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        String annotationEventJSON = "{\"attributes\":{\"SOURCE_TYPE\":\"USER_ANNOTATION\",\"$patientUUID\":\""+patientUUID.toString()+"\",\"$documentUUID\":\""+docUUID.toString()+"\",\"sourceType\":\"USER_ANNOTATION\"}," +
                "\"source\":{\"type\":\"document\",\"uri\":\""+docUUID.toString()+"\"},\"evidence\":{\"source\":{\"type\":\"user\",\"uri\":\"bfabros@apexcodemine.com\"},\"inferred\":false,\"attributes\":{\"presentedHcc\":\"96\",\"comment\":\"\",\"code\":\"\",\"physicianLasttName\":\"\",\"dateOfService\":\"06/07/2013\",\"encounterType\":\"\",\"physicianFirstName\":\"\",\"hccModelRun\":\"Final\",\"codeSystem\":\"\",\"hcc\":\"96\",\"pageNumber\":\"\",\"hccPaymentYear\":\"2014\",\"hccDisplayName\":\"Ischemic or Unspecified Stroke\",\"timestamp\":\"2015-01-26T00:22:15.918Z\",\"presentedHccDescription\":\"Ischemic or Unspecified Stroke\",\"presentedHccLabelSetVersion\":\"V12\",\"presentedHccMappingVersion\":\"2013 Final\",\"codeSystemVersion\":\"\",\"NPI\":\"\",\"physicianMiddleName\":\"\",\"hccModelYear\":\"2013\",\"project\":\"\",\"organization\":\"Hometown Health\"}},\"fact\":{\"code\":{\"codeSystemVersion\":\"2.0\",\"code\":\"V12_96\",\"displayName\":\"V12_96\",\"codeSystem\":\"APXCAT\"},\"values\":{\"rejectReason\":\"This specific HCC is not mentioned in the document\",\"result\":\"reject\"},\"time\":{\"endTime\":\"2013-06-07T07:00:00+0000\",\"startTime\":\"2013-06-07T07:00:00+0000\"}}," +
                "\"subject\":{\"type\":\"patient\",\"uri\":\""+patientUUID+"\"}}";

        EventType event = null;
        try {
            event = jsonParser.parseEventTypeData(annotationEventJSON);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        sdao.setPaths("attributes.sourceType,source.type,attributes.bucketName");

        List<EventType> events = new ArrayList<>();
        events.add(event);
        try {
            sdao.putAndCheck(events, org);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testDeleteCF() throws Exception
    {
        String testOrg = "10088";

        CustomerProperties customerProperties =  util.daoServices.getCustomerProperties();
        customerProperties.setNonPersistentMode();
        customerProperties.setColumnFamily2(util.testCassandraTables.patient, testOrg);
        customerProperties.setSequenceStoreColumnFamily(util.getSeqStoreProperty(), testOrg);
        customerProperties.setAFSFolder(util.testCassandraTables.testOrg, testOrg);

        String tag = "12345678910";
        sdao.createSequenceStoreColumnFamily(tag, true, testOrg);

        Map<String, String> allCF = util.getAllColumnFamiliesGut(testOrg);

        String cf = SeqStoreCf.newSeqStoreCF(tag, allCF.get(tag)).cf;

        int countOfAllInstancesOFCF = 0;

        for(String value : allCF.values()) {
            if(cf.equals(SeqStoreCf.newSeqStoreCF(tag, value).cf)) {
                countOfAllInstancesOFCF++;
            }
        }

        Assert.assertEquals(1, countOfAllInstancesOFCF);

        String tag2 = "12345678918";
        String tag3 = "12345678919";

        sdao.createSequenceStoreColumnFamily(tag2, false, testOrg);
        sdao.createSequenceStoreColumnFamily(tag3, false, testOrg);

        countOfAllInstancesOFCF = 0;

        sdao.refreshSequenceStore(testOrg);
        allCF = util.getAllColumnFamiliesGut(testOrg);
        for(String value : allCF.values()) {
            if(cf.equals(SeqStoreCf.newSeqStoreCF(tag, value).cf)) {
                countOfAllInstancesOFCF++;
            }
        }

        Assert.assertEquals(3, countOfAllInstancesOFCF);

        sdao.deleteSequenceStoreColumnFamily(tag3, testOrg);

        sdao.refreshSequenceStore(testOrg);
        allCF = util.getAllColumnFamiliesGut(testOrg);
        countOfAllInstancesOFCF = 0;
        for(String value : allCF.values()) {
            if(cf.equals(SeqStoreCf.newSeqStoreCF(tag, value).cf)) {
                countOfAllInstancesOFCF++;
            }
        }
        Assert.assertEquals(2, countOfAllInstancesOFCF);

        sdao.deleteSequenceStoreColumnFamily(tag2, testOrg);

        sdao.refreshSequenceStore(testOrg);
        allCF = util.getAllColumnFamiliesGut(testOrg);
        countOfAllInstancesOFCF = 0;

        for(String value : allCF.values()) {
            if(cf.equals(SeqStoreCf.newSeqStoreCF(tag, value).cf)) {
                countOfAllInstancesOFCF++;
            }
        }
        Assert.assertEquals(1, countOfAllInstancesOFCF);

        sdao.deleteSequenceStoreColumnFamily(tag, testOrg);

        sdao.refreshSequenceStore(testOrg);
        allCF = util.getAllColumnFamiliesGut(testOrg);
        countOfAllInstancesOFCF = 0;

        for(String value : allCF.values()) {
            if(cf.equals(SeqStoreCf.newSeqStoreCF(tag, value).cf)) {
                countOfAllInstancesOFCF++;
            }
        }
        Assert.assertEquals(0, countOfAllInstancesOFCF);
    }

    @After
    public void end() throws IOException
    {
        if(cache!=null) cache.flush();
    }

    private EventType createEventType(Date date, String patientId)
    {
        EventType event = null;

        String narrativeEvent =
                "{\"subject\":{\"uri\":\"" + patientId + "\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"83\",\"codeSystem\":\"HCC2013PYFinal\",\"codeSystemVersion\":\"2013Final\",\"displayName\":\"83\"},\"time\":{\"startTime\":\"2012-12-18T12:45:40-0800\",\"endTime\":\"2012-12-18T12:45:40-0800\"},\"values\":{\"face2face\":\"YES\",\"face2faceConfidence\":\"0.9709066893408104\",\"predictionEvidence\":\"cardiac angina\"}},\"source\":{\"uri\":\"a4302eda-3304-46f1-b679-0e3407faf9de\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"hccdict_bl20140106a.txt\",\"type\":\"File\"},\"attributes\":{\"predictionTs\":\"1389949359463\",\"classifierClass\":\"com.apixio.advdev.eventgen.extractors.tagging.TaggingService\",\"title\":\"Office Visit\",\"pageNumber\":\"1\",\"extractionType\":\"plainText\",\"snippet\":\"cardiac angina\"}},\"attributes\":{\"sweep\":\"Dec2013\",\"uploadingOrganizaiton\":\"Monarch\",\"cohort\":\"Monarch\",\"bucketType\":\"dictionaryModel.v0.0.1\",\"bucketName\":\"dictionaryModel.v0.0.1.83\"}}";

        jsonParser = new EventTypeJSONParser();
        try {
            event = jsonParser.parseEventTypeData(narrativeEvent);
        }
        catch (Exception e)
        {

        }

        CodeType codeType = new CodeType();
        codeType.setCode("100");
        codeType.setCodeSystem("ICD-9");

        TimeRangeType timeRangeType = new TimeRangeType();
        timeRangeType.setStartTime(date);
        timeRangeType.setEndTime(date);

        FactType factType = new FactType();
        factType.setCode(codeType);
        factType.setTime(timeRangeType);

        event.setFact(factType);

        AttributeType type1 = new AttributeType();
        type1.setName("a1");
        type1.setValue("v1");

        AttributeType type2 = new AttributeType();
        type2.setName("a2");
        type2.setValue("v2");

        AttributeType type3 = new AttributeType();
        type3.setName("a3");
        type3.setValue("v3");

        AttributeType type4 = new AttributeType();
        type4.setName("a4");
        type4.setValue("v4");

        AttributeType modelType = new AttributeType();
        modelType.setName("$modelVersion");
        modelType.setValue(patientId);

        AttributesType attributesType = new AttributesType();
        List<AttributeType> types = attributesType.getAttribute();
        types.add(type1);
        types.add(type2);
        types.add(type3);
        types.add(type4);
        types.add(modelType);

        event.setAttributes(attributesType);

        return event;
    }

    @Test
    public void testAnnotation() throws Exception
    {
        setAllPropertiesHaveDataTrue();

        UUID patientUUID = UUID.randomUUID();
        String annotationEventJSON = "{\"attributes\":{\"SOURCE_TYPE\":\"USER_ANNOTATION\",\"$patientUUID\":\""+patientUUID.toString()+"\",\"$documentUUID\":\"4064e598-cb8e-4632-9250-f37c8ebf834b\",\"sourceType\":\"USER_ANNOTATION\"},\"source\":{\"type\":\"document\",\"uri\":\"4064e598-cb8e-4632-9250-f37c8ebf834b\"},\"evidence\":{\"source\":{\"type\":\"user\",\"uri\":\"bfabros@apexcodemine.com\"},\"inferred\":false,\"attributes\":{\"presentedHcc\":\"96\",\"comment\":\"\",\"code\":\"\",\"physicianLasttName\":\"\",\"dateOfService\":\"06/07/2013\",\"encounterType\":\"\",\"physicianFirstName\":\"\",\"hccModelRun\":\"Final\",\"codeSystem\":\"\",\"hcc\":\"96\",\"pageNumber\":\"\",\"hccPaymentYear\":\"2014\",\"hccDisplayName\":\"Ischemic or Unspecified Stroke\",\"timestamp\":\"2015-01-26T00:22:15.918Z\",\"presentedHccDescription\":\"Ischemic or Unspecified Stroke\",\"presentedHccLabelSetVersion\":\"V12\",\"presentedHccMappingVersion\":\"2013 Final\",\"codeSystemVersion\":\"\",\"NPI\":\"\",\"physicianMiddleName\":\"\",\"hccModelYear\":\"2013\",\"project\":\"\",\"organization\":\"Hometown Health\"}},\"fact\":{\"code\":{\"codeSystemVersion\":\"2.0\",\"code\":\"V12_96\",\"displayName\":\"V12_96\",\"codeSystem\":\"APXCAT\"},\"values\":{\"rejectReason\":\"This specific HCC is not mentioned in the document\",\"result\":\"reject\"},\"time\":{\"endTime\":\"2013-06-07T07:00:00+0000\",\"startTime\":\"2013-06-07T07:00:00+0000\"}},\"subject\":{\"type\":\"patient\",\"uri\":\""+patientUUID.toString()+"\"}}";

        EventType event = null;
        try {
            event = jsonParser.parseEventTypeData(annotationEventJSON);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        sdao.setPaths("attributes.sourceType,source.type,attributes.bucketName");

        List<EventType> events = new ArrayList<>();
        events.add(event);
        try {
            sdao.putAndCheck(events, org);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        ReferenceType r = new ReferenceType();
        r.setUri(patientUUID.toString());
        r.setType("patient");

        assertEquals(sdao.getSequence(r, "attributes.sourceType", "USER_ANNOTATION", 0L, 0L, org).size(), 1);

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(r)
                .setPath("attributes.sourceType").setValue("USER_ANNOTATION")
                .setTagType(SeqStoreDAO.TagType.Annotation).setOrgId(org)
                .setRange(new Range.RangeBuilder().setStart(0).setEnd(0).build()).build();

        assertEquals(1, sdao.getSequenceValues(criteria).size());

        criteria = new Criteria.TagTypeCriteria.Builder().setSubject(r)
                .setPath("attributes.sourceType").setValue("USER_ANNOTATION")
                .setTagType(SeqStoreDAO.TagType.NonInferred).setOrgId(org)
                .setRange(new Range.RangeBuilder().setStart(0).setEnd(0).build()).build();
        assertEquals(0, sdao.getSequenceValues(criteria).size());


        criteria = new Criteria.TagTypeCriteria.Builder().setSubject(r)
                .setPath("attributes.sourceType").setValue("USER_ANNOTATION")
                .setTagType(SeqStoreDAO.TagType.Inferred).setOrgId(org)
                .setRange(new Range.RangeBuilder().setStart(0).setEnd(0).build()).build();
        assertEquals(0, sdao.getSequenceValues(criteria).size());

        criteria = new Criteria.TagTypeCriteria.Builder().setSubject(r)
                .setPath("attributes.sourceType").setValue("USER_ANNOTATION")
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(org)
                .setRange(new Range.RangeBuilder().setStart(0).setEnd(0).build()).build();
        assertEquals(1, sdao.getSequenceValues(criteria).size());
    }

    private Date getDate(int year)
    {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, year);

        return cal.getTime();
    }

    //simulate pipeline behavior
    private void markSequenceStoresHaveData()
            throws Exception
    {
        sdao.markSequenceStoresHaveData();
    }
}
