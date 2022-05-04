package com.apixio.dao.seqstore;

import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.seqstore.store.*;
import com.apixio.dao.seqstore.utility.Criteria;
import com.apixio.dao.seqstore.utility.Range;
import com.apixio.dao.seqstore.utility.SeqStoreCFUtility;
import com.apixio.dao.seqstore.utility.SeqStoreCf;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.model.event.EventType;
import com.apixio.model.event.ReferenceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by dyee on 12/5/16.
 *
 * A lot of these test are invalidate by making
 *
 * private List<String> getAllColumnFamilies(TagTypeCriteria criteria)
 *
 * private, instead of protected. These tests are mock level test, that are tested indirectly by test
 * cases in SeqStoreDaoTests....
 *
 */
@Ignore("Broken")
@RunWith(MockitoJUnitRunner.class)
public class SeqStoreDAOMockTest {
    @InjectMocks
    SeqStoreDAO seqStoreDAO;

    @Mock
    CqlCrud cqlCrud;

    @Mock
    SeqStoreCFUtility seqStoreCFUtility;

    @Mock
    SubjectStore subjectStore;

    @Mock
    PathValueStore pathValueStore;

    @Mock
    SubjectPathValueStore subjectPathValueStore;

    @Mock
    SubjectPathStore subjectPathStore;

    @Mock
    AddressStore addressStore;

    @Mock
    QueryStore queryStore;

    @Before
    public void setup() {
    }

    @Test
    public void testRange_NoRangeCheck() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = mock(Range.class);

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(Collections.EMPTY_LIST);

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).build();

//        List<String> result =  seqStoreDAO.getAllColumnFamilies(criteria);

//        Assert.assertNotNull(result);
//        Mockito.verify(range, times(0)).isIncludeLower();
//        Mockito.verify(range, times(0)).isIncludeUpper();
//        Mockito.verify(range, times(0)).getEnd();
//        Mockito.verify(range, times(0)).getStart();
    }

    @Test
    public void testRange_RangeCheck() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = mock(Range.class);

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf("", "", 1, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).build();

//        List<String> result =  seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//        Mockito.verify(range, times(1)).isIncludeLower();
//        Mockito.verify(range, times(1)).isIncludeUpper();
//        Mockito.verify(range, times(1)).getEnd();
//        Mockito.verify(range, times(1)).getStart();
    }

    @Test
    public void testRange_RangeCheck_CheckResultValid() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(100).setEnd(200).setIncludeLower(true).setIncludeLower(true).build();

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf("", "", 150, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).build();

//        List<String> result =  seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testRange_RangeCheck_CheckResultInvalid() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(100).setEnd(200).setIncludeLower(true).setIncludeLower(true).build();

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf("", "", 88, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).build();

//        List<String> result =  seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testRange_RangeCheck_CheckResultValid_MixedResult() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(100).setEnd(200).setIncludeLower(true).setIncludeLower(true).build();

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf("", "", 88, false, false)); // should not be included..
        SeqStoreCfResult.add(new SeqStoreCf("", "", 100, false, false));
        SeqStoreCfResult.add(new SeqStoreCf("", "", 200, false, false));
        SeqStoreCfResult.add(new SeqStoreCf("", "", 150, false, false));

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).build();

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

//        List<String> result =  seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//        Assert.assertEquals(SeqStoreCfResult.size()-1, result.size());
    }

    @Test
    public void testCorrectQueryTypeResolution_Default() throws Exception{
        ArgumentCaptor<QueryType> argumentCaptor = ArgumentCaptor.forClass(QueryType.class);

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf("", "", 88, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(0).setEnd(Long.MAX_VALUE).setIncludeLower(true).setIncludeLower(true).build();

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).build();
        seqStoreDAO.getSequence(criteria);

        verify(queryStore).getSequence(argumentCaptor.capture(),
                (ReferenceType)any(), anyString(), anyString(), anyLong(), anyLong(), anyString());


        QueryType queryType = argumentCaptor.getValue();

        Assert.assertEquals(QueryType.QuerySubjectStore, queryType);
    }

    @Test
    public void testCorrectQueryTypeResolution_QuerySubjectPathStore() throws Exception{
        ArgumentCaptor<QueryType> argumentCaptor = ArgumentCaptor.forClass(QueryType.class);

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf("", "", 88, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(0).setEnd(Long.MAX_VALUE).setIncludeLower(true).setIncludeLower(true).build();

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).setPath("dfsadfsadf").build();
        seqStoreDAO.getSequence(criteria);

        verify(queryStore).getSequence(argumentCaptor.capture(),
                (ReferenceType)any(), anyString(), anyString(), anyLong(), anyLong(), anyString());


        QueryType queryType = argumentCaptor.getValue();

        Assert.assertEquals(QueryType.QuerySubjectPathStore, queryType);
    }

    @Test
    public void testCorrectQueryTypeResolution_QuerySubjectPathValueStore() throws Exception{
        ArgumentCaptor<QueryType> argumentCaptor = ArgumentCaptor.forClass(QueryType.class);

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf("", "", 88, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(0).setEnd(Long.MAX_VALUE).setIncludeLower(true).setIncludeLower(true).build();

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.all).setOrgId(orgId).setRange(range).setPath("dfsadfsadf").setValue("dfd").build();
        seqStoreDAO.getSequence(criteria);

        verify(queryStore).getSequence(argumentCaptor.capture(),
                (ReferenceType)any(), anyString(), anyString(), anyLong(), anyLong(), anyString());


        QueryType queryType = argumentCaptor.getValue();

        Assert.assertEquals(QueryType.QuerySubjectPathValueStore, queryType);
    }

    @Test
    public void testGetAllColumnFamilies_NonInferred() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(100).setEnd(200).setIncludeLower(true).setIncludeLower(true).build();

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 88, false, false)); // should not be included..
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.global,"", 100, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 200, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 150, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.NonInferred).setOrgId(orgId).setRange(range).build();

        /**
         * if (criteria.getTagType() == TagType.NonInferred && sscf.tag.equals(SeqStoreCFUtility.non_inferred))
         *        cfList.add(sscf.cf);
         */

//        List<String> result = seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//
//        // One is out of range, and the other is tag = SeqStoreCFUtility.global
//        Assert.assertEquals(SeqStoreCfResult.size()-2, result.size());




        /**
         * @TODO: add test cases for each of the following cases.........

         else if (criteria.getTagType() == TagType.Annotation && sscf.tag.equals(SeqStoreCFUtility.global))
         cfList.add(sscf.cf);

         else if (criteria.getTagType() == TagType.Inferred || criteria.getTagType() == TagType.last)
         {
         if (sscf.tag.equals(SeqStoreCFUtility.global) || sscf.tag.equals(SeqStoreCFUtility.non_inferred) )
         continue;

         */
    }

    @Test
    public void testGetAllColumnFamilies_Annotation_Global() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(100).setEnd(200).setIncludeLower(true).setIncludeLower(true).build();

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.global, "", 88, false, false)); // should not be included..
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.global,"", 100, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 200, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 150, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.Annotation).setOrgId(orgId).setRange(range).build();

        /**
         * if (criteria.getTagType() == TagType.NonInferred && sscf.tag.equals(SeqStoreCFUtility.non_inferred))
         *        cfList.add(sscf.cf);
         */

//        List<String> result = seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//
//        // only one is global, and in range.... tag = SeqStoreCFUtility.global
//        Assert.assertEquals(1, result.size());




        /**
         * @TODO: add test cases for each of the following cases.........

        else if (criteria.getTagType() == TagType.Inferred || criteria.getTagType() == TagType.last)
        {
        if (sscf.tag.equals(SeqStoreCFUtility.global) || sscf.tag.equals(SeqStoreCFUtility.non_inferred) )
        continue;

         */
    }


    @Test
    public void testGetAllColumnFamilies_Inferred_Last_Excluded() throws Exception {
        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(100).setEnd(200).setIncludeLower(true).setIncludeLower(true).build();

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.global, "", 88, false, false)); // should not be included..
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.global,"", 100, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 200, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 150, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.Inferred).setOrgId(orgId).setRange(range).build();



        // criteria.getTagType() == TagType.Inferred
//
//        List<String> result = seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//
//        // only one is global, and in range.... tag = SeqStoreCFUtility.global
//        Assert.assertEquals(0, result.size());


        //criteria.getTagType() == TagType.last
//
//        criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
//                .setTagType(SeqStoreDAO.TagType.last).setOrgId(orgId).setRange(range).build();
//
//        result = seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//
//        // only one is global, and in range.... tag = SeqStoreCFUtility.global
//        Assert.assertEquals(0, result.size());
//

        /**
         * @TODO: add test cases for each of the following cases.........

        else if (criteria.getTagType() == TagType.Inferred || criteria.getTagType() == TagType.last)
        {
        if (sscf.tag.equals(SeqStoreCFUtility.global) || sscf.tag.equals(SeqStoreCFUtility.non_inferred) )
        continue;

         */
    }

    @Test
    public void testGetAllColumnFamilies_Inferred_Last_NotExcluded() throws Exception {

        /**
         * add test cases for each of the following cases.........

        else if (criteria.getTagType() == TagType.Inferred || criteria.getTagType() == TagType.last)
        {
        if (sscf.tag.equals(SeqStoreCFUtility.global) || sscf.tag.equals(SeqStoreCFUtility.non_inferred) )
        continue;

         */

        ReferenceType subject = mock(ReferenceType.class);
        String orgId = "testOrg";
        Range range = new Range.RangeBuilder().setStart(100).setEnd(200).setIncludeLower(true).setIncludeLower(true).build();

        List<SeqStoreCf> SeqStoreCfResult = new ArrayList<>();
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.historical, "", 88, false, false)); // should not be included..
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.historical,"", 100, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.global, "", 200, false, false));
        SeqStoreCfResult.add(new SeqStoreCf(SeqStoreCFUtility.non_inferred, "", 150, false, false));

        when(seqStoreCFUtility.getAllColumnFamilies((ReferenceType)any(), anyString())).thenReturn(
                SeqStoreCfResult
        );

        Criteria.TagTypeCriteria criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(SeqStoreDAO.TagType.Inferred).setOrgId(orgId).setRange(range).build();



        // criteria.getTagType() == TagType.Inferred

//        List<String> result = seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//
//        // only one is global, and in range.... tag = SeqStoreCFUtility.global
//        Assert.assertEquals(1, result.size());


        //criteria.getTagType() == TagType.last
//
//        criteria = new Criteria.TagTypeCriteria.Builder().setSubject(subject)
//                .setTagType(SeqStoreDAO.TagType.last).setOrgId(orgId).setRange(range).build();
//
//        result = seqStoreDAO.getAllColumnFamilies(criteria);
//
//        Assert.assertNotNull(result);
//
//        // only one is global, and in range.... tag = SeqStoreCFUtility.global
//        Assert.assertEquals(1, result.size());
    }

    @Test
    public void test_PutGutsIntoSingleColumn() throws Exception {
        LocalCqlCache localCqlCache = null;

        EventType eventType1 = mock(EventType.class);

        List<EventType> eventTypes = new ArrayList<>();
        eventTypes.add(eventType1);

        seqStoreDAO.put(eventTypes, "testOrgId");

        ArgumentCaptor<List> eventTypeCaptor1 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> eventTypeCaptor2 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> eventTypeCaptor3 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> eventTypeCaptor4 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> eventTypeCaptor5 = ArgumentCaptor.forClass(List.class);


        verify(subjectStore, times(1)).put(eventTypeCaptor1.capture(), anyString(), anyString(), eq(localCqlCache));
        verify(pathValueStore, times(1)).put(eventTypeCaptor2.capture(), (List)any(), anyString(), eq(localCqlCache));
        verify(subjectPathValueStore, times(1)).put(eventTypeCaptor3.capture(), (List)any(), anyString(), anyString(), eq(localCqlCache));
        verify(subjectPathStore, times(1)).put(eventTypeCaptor4.capture(), (List)any(), anyString(), anyString(), eq(localCqlCache));
        verify(addressStore, times(1)).put(eventTypeCaptor5.capture(), anyString(), anyString(), eq(localCqlCache));

        Assert.assertEquals(eventTypes, eventTypeCaptor1.getValue());
        Assert.assertEquals(eventTypes, eventTypeCaptor2.getValue());
        Assert.assertEquals(eventTypes, eventTypeCaptor3.getValue());
        Assert.assertEquals(eventTypes, eventTypeCaptor4.getValue());
        Assert.assertEquals(eventTypes, eventTypeCaptor5.getValue());
    }
}
