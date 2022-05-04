package com.apixio.dao.indexsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.Patient;
import org.joda.time.DateTime;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import com.apixio.dao.DAOTestUtils;

@Ignore("Integration")
public class IndexSearchDAOTest {
    static private final String datePattern1 = "yyyy/MM/dd";
    static private final String datePattern2 = "MM/dd/yyyy";

    private DAOTestUtils util;
    private IndexSearchDAO dao;
    private CqlCache cqlCache;
    private String org;

    private int multiple = 10;

    @Before
    public void setUp() throws Exception
    {
        util = new DAOTestUtils();
        dao = util.daoServices.getIndexSearchDAO2();
        cqlCache = util.daoServices.getScienceCqlCrud().getCqlCache();
        org = util.testCassandraTables.testOrg;
    }

    @Test
    public void TestCreateIndex()
    {
        long t = System.currentTimeMillis();
        String uuid  = "uuid" + t;
        String primaryId    = "primaryId" + t;
        String firstName    = "firstName" + t;
        String lastName     = "lastName" + t;
        String alternateId  = "alternateId" + t;

        List<DemographicData> demographicDataList = new ArrayList<>();

        try
        {
            for (int i = 0; i < multiple; i++)
            {
                DemographicData demographic = new DemographicData();
                demographic.uuid = uuid + i;
                demographic.primaryId = primaryId + i;

                demographic.firstNames = new ArrayList<>();
                demographic.firstNames.add(firstName + i);

                demographic.lastNames = new ArrayList<>();
                demographic.lastNames.add(lastName + i);

                demographic.birthDates = new ArrayList<>();
                DateTime date = new DateTime();
                demographic.birthDates.add(date.toString(datePattern1));
                demographic.birthDates.add(date.toString(datePattern2));

                demographic.alternateIds = new ArrayList<>();
                demographic.alternateIds.add(alternateId + i);

                dao.indexDemographics(demographic, org);

                demographicDataList.add(demographic);
            }

            cqlCache.flush();
            Thread.sleep(2000);

            SearchResult result1 = dao.searchDemographics("uuid", org, 0, 5);
            assertNotNull(result1);
            assertTrue(!result1.hits.isEmpty());
            assertSame(result1.next, 5);

            SearchResult result2 = dao.searchDemographics("firstName", org, 0, 5);
            assertNotNull(result2);
            assertTrue(!result2.hits.isEmpty());
            assertSame(result2.next, 5);

            SearchResult result3 = dao.searchDemographics("lastName", org, 0, 5);
            assertNotNull(result3);
            assertTrue(!result3.hits.isEmpty());
            assertSame(result3.next, 5);

            SearchResult result4 = dao.searchDemographics("primaryId", org, 0, 5);
            assertNotNull(result4);
            assertTrue(!result4.hits.isEmpty());
            assertSame(result4.next, 5);

            SearchResult result5 = dao.searchDemographics("firstName lastName", org, 0, 5);
            assertNotNull(result5);
            assertTrue(!result5.hits.isEmpty());
            assertSame(result5.next, 5);

            SearchResult result6 = dao.searchDemographics("alternateId", org, 0, 5);
            assertNotNull(result6);
            assertTrue(!result6.hits.isEmpty());
            assertSame(result6.next, 5);

            SearchResult result7 = dao.searchDemographics(demographicDataList.get(0).birthDates.get(0), org, 0, 5);
            assertNotNull(result7);
            assertTrue(!result7.hits.isEmpty());
            assertSame(result7.next, 5);

            SearchResult result8 = dao.searchDemographics(demographicDataList.get(0).birthDates.get(1), org, 0, 5);
            assertNotNull(result8);
            assertTrue(!result8.hits.isEmpty());
            assertSame(result8.next, 5);
        }

        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void TestCreateIndex1()
    {
        try
        {
            long t = System.currentTimeMillis();
            String uuid = UUID.randomUUID().toString();

            Name n = new Name();
            n.setGivenNames(Arrays.asList("first1" + t, "first2" + t));
            n.setFamilyNames(Arrays.asList("last1" + t, "last2" + t));

            DateTime dt = new DateTime();

            Demographics d = new Demographics();
            d.setName(n);
            d.setDateOfBirth(dt);

            ExternalID exID = new ExternalID();
            exID.setId("exID" + t);

            ExternalID pexID = new ExternalID();
            pexID.setId("pexID" + t);

///////
            Patient p = new Patient();
            p.setPatientId(UUID.fromString(uuid));
            p.setPrimaryDemographics(d);
            p.setExternalIDs(new HashSet<>(Arrays.asList(exID)));
            p.setPrimaryExternalID(pexID);

            DemographicData data = getDemographicData(uuid, p, org);
            dao.indexDemographics(data, org);
            cqlCache.flush();
            Thread.sleep(2000);
            System.out.println("getTotalBufferWrites: " + cqlCache.getTotalBufferWrites());

//////// same patient

            Patient p1 = new Patient();
            p1.setPatientId(UUID.fromString(uuid));
            p1.setPrimaryDemographics(d);
            p1.setExternalIDs(new HashSet<>(Arrays.asList(exID)));
            p1.setPrimaryExternalID(pexID);

            DemographicData data1 = getDemographicData(uuid, p1, org);
            dao.indexDemographics(data1, org);
            cqlCache.flush();
            Thread.sleep(2000);
            System.out.println("getTotalBufferWrites1 (should not change): " + cqlCache.getTotalBufferWrites());

//////// same external ids

            Patient p2 = new Patient();
            p2.setPatientId(UUID.fromString(uuid));
            p2.setExternalIDs(new HashSet<>(Arrays.asList(exID)));
            p2.setPrimaryExternalID(pexID);

            DemographicData data2 = getDemographicData(uuid, p2, org);
            dao.indexDemographics(data2, org);
            cqlCache.flush();
            Thread.sleep(2000);
            System.out.println("getTotalBufferWrites2 (should not change): " + cqlCache.getTotalBufferWrites());

//////// different external ids

            ExternalID exID3 = new ExternalID();
            exID3.setId("exID3" + t);

            Patient p3 = new Patient();
            p3.setPatientId(UUID.fromString(uuid));
            p3.setExternalIDs(new HashSet<>(Arrays.asList(exID3)));
            p3.setPrimaryExternalID(pexID);

            DemographicData data3 = getDemographicData(uuid, p3, org);
            dao.indexDemographics(data3, org);
            cqlCache.flush();
            Thread.sleep(2000);
            System.out.println("getTotalBufferWrites3: " + cqlCache.getTotalBufferWrites());

//////// same patient id and primary id

            Patient p4 = new Patient();
            p4.setPatientId(UUID.fromString(uuid));
            p4.setPrimaryExternalID(pexID);

            DemographicData data4 = getDemographicData(uuid, p4, org);
            dao.indexDemographics(data4, org);
            cqlCache.flush();
            Thread.sleep(2000);
            System.out.println("getTotalBufferWrites4 (should not change): " + cqlCache.getTotalBufferWrites());

//////// different demographics

            Name n1 = new Name();
            n1.setGivenNames(Arrays.asList("first11" + t, "first2" + t));
            n1.setFamilyNames(Arrays.asList("last11" + t, "last2" + t));

            DateTime dt1 = new DateTime();

            Demographics d1 = new Demographics();
            d1.setName(n1);
            d1.setDateOfBirth(dt1);

            Patient p5 = new Patient();
            p5.setPatientId(UUID.fromString(uuid));
            p5.setPrimaryDemographics(d1);
            p5.setExternalIDs(new HashSet<>(Arrays.asList(exID)));
            p5.setPrimaryExternalID(pexID);

            DemographicData data5 = getDemographicData(uuid, p5, org);
            dao.indexDemographics(data5, org);
            cqlCache.flush();
            Thread.sleep(2000);
            System.out.println("getTotalBufferWrites5: " + cqlCache.getTotalBufferWrites());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private DemographicData getDemographicData(String patientUUID, Patient apo, String orgID)
    {
        boolean alreadyIndexed = true;

        DemographicData demographicData = new DemographicData();
        demographicData.uuid = patientUUID;
        demographicData.firstNames = new ArrayList<>();
        demographicData.lastNames = new ArrayList<>();
        demographicData.birthDates = new ArrayList<>();
        demographicData.alternateIds = new ArrayList<>();

        if (apo.getPrimaryDemographics() != null)
        {
            if (apo.getPrimaryDemographics().getName() != null && apo.getPrimaryDemographics().getName().getGivenNames() != null)
            {
                for (String firstName : apo.getPrimaryDemographics().getName().getGivenNames())
                {
                    if (!dao.isIndexed(firstName, patientUUID, DemographicData.DemographicType.fn, orgID))
                    {
                        alreadyIndexed = false;
                        demographicData.firstNames.add(firstName);
                    }
                }
            }

            if (apo.getPrimaryDemographics().getName() != null && apo.getPrimaryDemographics().getName().getFamilyNames() != null)
            {
                for (String lastName : apo.getPrimaryDemographics().getName().getFamilyNames())
                {
                    if (!dao.isIndexed(lastName, patientUUID, DemographicData.DemographicType.ln, orgID))
                    {
                        alreadyIndexed = false;
                        demographicData.lastNames.add(lastName);
                    }
                }
            }

            if (apo.getPrimaryDemographics().getDateOfBirth() != null)
            {
                if (!dao.isIndexed(apo.getPrimaryDemographics().getDateOfBirth().toString(datePattern1),
                        patientUUID, DemographicData.DemographicType.bd, orgID))
                {
                    alreadyIndexed = false;
                    demographicData.birthDates.add(apo.getPrimaryDemographics().getDateOfBirth().toString(datePattern1));
                    demographicData.birthDates.add(apo.getPrimaryDemographics().getDateOfBirth().toString(datePattern2));
                }
            }
        }

        if (apo.getExternalIDs() != null)
        {
            for (ExternalID externalID : apo.getExternalIDs())
            {
                String id = externalID.getId();
                if (id != null && !id.equals(demographicData.primaryId) &&
                        !dao.isIndexed(id, patientUUID, DemographicData.DemographicType.aid, orgID))
                {
                    alreadyIndexed = false;
                    demographicData.alternateIds.add(id);
                }
            }
        }

        String pid = apo.getPrimaryExternalID().getId();
        if (!alreadyIndexed && !dao.isIndexed(pid, patientUUID, DemographicData.DemographicType.pid, orgID))
        {
            alreadyIndexed = false;
        }

        if (alreadyIndexed)
        {
            demographicData.indexUuid = false;
        }
        else
        {
            demographicData.primaryId = pid;
            demographicData.indexUuid = true;
        }

        return demographicData;
    }
}
