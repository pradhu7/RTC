package com.apixio.dao.seqstore.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.model.event.AttributeType;
import com.apixio.model.event.AttributesType;
import com.apixio.model.event.EventType;
import com.apixio.model.event.EvidenceType;
import com.apixio.model.event.ReferenceType;
import com.apixio.utility.StringUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class SeqStoreCFUtility
{
    public static final String modelVersionName = "$modelVersion";
    public static final String non_inferred     = "non_inferred";
    public static final String inferred         = "inferred";
    public static final String global           = "global";
    public static final String historical       = "historical";

    private CustomerProperties customerProperties;
    private String             globalColumnFamily;
    private CqlCrud            scienceCqlCrud;
    private CqlCrud            applicationCqlCrud;
    private String             linkCF;

    public Map<String, Set<SeqStoreCf>> orgIdToSeqStoreCfSet = new HashMap<>();

    public static class ColumnFamilyMetadata
    {

        public ColumnFamilyMetadata(String columnFamily, String tagType)
        {
            this.columnFamily = columnFamily;
            this.tagType = tagType;
        }

        public String columnFamily;
        public String tagType;
    }

    public void setCustomerProperties(CustomerProperties customerProperties)
    {
        this.customerProperties = customerProperties;
    }

    public void setGlobalCF(String globalColumnFamily)
    {
        this.globalColumnFamily = globalColumnFamily;
    }

    public void setScienceCqlCrud(CqlCrud scienceCqlCrud)
    {
        this.scienceCqlCrud = scienceCqlCrud;
    }

    public void setApplicationCqlCrud(CqlCrud applicationCqlCrud)
    {
        this.applicationCqlCrud = applicationCqlCrud;
    }

    public void setLinkCF(String linkCF)
    {
        this.linkCF = linkCF;
    }

    public ColumnFamilyMetadata getWriteColumnFamily(EventType eventType, String orgId, boolean updateHasData)
        throws Exception
    {
        String tag = getTag(eventType);
        if (tag == null)
            throw new Exception("tag can't be null");

        orgId = getOrgId(eventType.getSubject(), orgId);
        if (orgId == null)
            throw new Exception("orgId can't be null");

        SeqStoreCf seqStoreCf = getColumnFamily(tag, orgId);
        if (seqStoreCf == null)
            throw new Exception("cf can't be null");

        if (updateHasData)
            markHasData(seqStoreCf, orgId);

        return new ColumnFamilyMetadata(seqStoreCf.cf, tag);
    }

    private void markHasData(SeqStoreCf seqStoreCf, String orgId)
    {
        if (seqStoreCf.hasData)
            return;

        Set<SeqStoreCf> seqStoreCfSet = orgIdToSeqStoreCfSet.get(orgId);
        if (seqStoreCfSet == null)
        {
            seqStoreCfSet = new HashSet<>();
            orgIdToSeqStoreCfSet.put(orgId, seqStoreCfSet);
        }

        seqStoreCfSet.add(seqStoreCf);
    }

    //
    // NOTE: External callers of this function **may** wish to update the cache via
    //       the refreshCache() method call. Make sure you understand why this is required
    //       - our design decision was to avoid unnecessary calls to redis, so we require clients
    //       to understand and optimize
    //
    public void markSequenceStoresHaveData()
       throws Exception
    {
        for (Map.Entry<String, Set<SeqStoreCf>> entry : orgIdToSeqStoreCfSet.entrySet())
        {
            Map<String, String> cfMap = getAllColumnFamiliesGut(entry.getKey());

            for (SeqStoreCf seqStoreCf : entry.getValue())
            {
                String decoratedCF = cfMap.get(seqStoreCf.tag);
                if (decoratedCF == null)
                    continue;

                SeqStoreCf sscf = SeqStoreCf.newSeqStoreCF(seqStoreCf.tag, decoratedCF);
                if (sscf.hasData)
                    continue;

                sscf.hasData = true;
                cfMap.put(seqStoreCf.tag, sscf.decorateCF());
            }

            customerProperties.setSequenceStoreColumnFamily(StringUtil.mapToString(cfMap), entry.getKey());
        }

        orgIdToSeqStoreCfSet.clear();
    }

    public void markSequenceStoreHasData(String tag, String orgId)
        throws Exception
    {
        Map<String, String> cfMap = getAllColumnFamiliesGut(orgId);

        String decoratedCF = cfMap.get(tag);
        if (decoratedCF == null)
            return;

        SeqStoreCf sscf = SeqStoreCf.newSeqStoreCF(tag, decoratedCF);
        if (sscf.hasData)
            return;

        sscf.hasData = true;
        cfMap.put(tag, sscf.decorateCF());

        customerProperties.setSequenceStoreColumnFamily(StringUtil.mapToString(cfMap), orgId);
    }

    //
    // NOTE: External callers of this function **may** wish to update the cache via
    //       the refreshCache() method call. Make sure you understand why this is required
    //       - our design decision was to avoid unnecessary calls to redis, so we require clients
    //       to understand and optimize
    //
    public void markSequenceStoreMigrationCompleted(String tag, String orgId)
        throws Exception
    {
        Map<String, String> cfMap = getAllColumnFamiliesGut(orgId);

        String decoratedCF = cfMap.get(tag);
        if (decoratedCF == null)
            return;

        SeqStoreCf sscf = SeqStoreCf.newSeqStoreCF(tag, decoratedCF);
        if (!sscf.hasData) // you should have data to mark migration completed!!!
            return;

        if (sscf.migrationCompleted)
            return;

        sscf.migrationCompleted = true;
        cfMap.put(tag, sscf.decorateCF());

        customerProperties.setSequenceStoreColumnFamily(StringUtil.mapToString(cfMap), orgId);
    }

    public void createColumnFamily(String tag, boolean createNewInferredCF, String orgId)
        throws Exception
    {
        // global cf is created by dev ops && non_inferred is created during setup.
        if (tag.equals(global) || tag.equals(non_inferred))
            return;

        Map<String, String> cfMap = getAllColumnFamiliesGut(orgId);

        //
        // If a mapping to a tag already exists, we must route to it. Never override this, or we will lose data
        // because we will replace the mapping, but never delete the CF....
        //
        if (cfMap.containsKey(tag))
            return;

        String cf = createNewInferredCF ?  makeCF(tag, orgId) : getLatestInferredCF(orgId);

        String decoratedCF;

        // Continue using historical
        if (cf == null)
        {
            //
            // Complicated logic for historical, because we want the tag to reference the CF, but not the metadata
            //
            cf = cfMap.get(historical);

            if (cf == null) throw new Exception("Historical CF should not be missing");

            //
            //This is complicated, because we want the historical cf, but we do not want to keep
            //track of whether or not this particular tag has data and has completed migration or not!
            //
            //It is not correct to do a decoratedCF = cf...
            //
            decoratedCF = SeqStoreCf.decorateCF(SeqStoreCf.newSeqStoreCF(tag, cf).cf);
        }
        else
        {
            decoratedCF = SeqStoreCf.decorateCF(cf);
        }

        cfMap.put(tag, decoratedCF);
        customerProperties.setSequenceStoreColumnFamily(StringUtil.mapToString(cfMap), orgId);

        if (createNewInferredCF)
            scienceCqlCrud.createSizeTieredTable(cf);
    }

    public void deleteColumnFamily(String tag, String orgId)
        throws Exception
    {
        // global cf and non_inferred are never deleted
        if (tag.equals(global) || tag.equals(non_inferred))
            return;

        Map<String, String> cfMap       = getAllColumnFamiliesGut(orgId);
        String              decoratedCf = cfMap.get(tag);

        if (decoratedCf != null)
        {
            cfMap.remove(tag);
            customerProperties.setSequenceStoreColumnFamily(StringUtil.mapToString(cfMap), orgId);

            SeqStoreCf sscf = SeqStoreCf.newSeqStoreCF(tag, decoratedCf);

            if (!isTableReferenced(sscf.cf, orgId))
                scienceCqlCrud.dropTable(sscf.cf);
        }
    }

    private boolean isTableReferenced(String cf, String orgId)
        throws Exception
    {
        List<SeqStoreCf> sscfs = getAllColumnFamilies(orgId);

        for (SeqStoreCf sscf : sscfs)
        {
            if (sscf.cf.equals(cf))
                return true;
        }

        return false;
    }

    private String getLatestInferredCF(String orgId)
        throws Exception
    {
        List<SeqStoreCf> seqStoreCFList = getAllColumnFamilies(orgId);
        sortCF(seqStoreCFList);

        List<SeqStoreCf> inferredSeqStoreCFList = includeOnlyInferredCFs(seqStoreCFList);

        SeqStoreCf sscf = inferredSeqStoreCFList.isEmpty() ? null : inferredSeqStoreCFList.get(inferredSeqStoreCFList.size() - 1);

        return (sscf != null ? sscf.cf : null);
    }

    private List<SeqStoreCf> includeOnlyInferredCFs(List<SeqStoreCf> sscfs)
    {
        List<SeqStoreCf> inferredSscfs = new ArrayList<>();

        for (SeqStoreCf sscf: sscfs)
        {
            if (sscf.tag.equals(global) || sscf.tag.equals(non_inferred) || sscf.tag.equals(historical))
                continue;

            inferredSscfs.add(sscf);
        }

        return inferredSscfs;
    }

    public void refreshSequenceStore(String orgId)
        throws Exception
    {
        customerProperties.refreshSequenceStore(orgId);
    }

    public SeqStoreCf getColumnFamily(ReferenceType subject, String tag, String orgId)
        throws Exception
    {
        orgId = getOrgId(subject, orgId);
        if (orgId == null)
            throw new Exception("orgId can't be null");

        return getColumnFamily(tag, orgId);
    }

    public SeqStoreCf getColumnFamily(String tag, String orgId)
            throws Exception
    {
        List<SeqStoreCf> sscfs = getAllColumnFamilies(orgId);

        for (SeqStoreCf sscf : sscfs)
        {
            if (tag.equals(sscf.tag))
                return sscf;
        }

        return null;
    }

    public List<SeqStoreCf> getAllColumnFamilies(ReferenceType subject, String orgId)
            throws Exception
    {
        orgId = getOrgId(subject, orgId);
        if (orgId == null)
            throw  new Exception("orgId can't be null");

        return getAllColumnFamilies(orgId);
    }

    public List<SeqStoreCf> getAllColumnFamilies(String orgId)
        throws Exception
    {
        List<SeqStoreCf>    cfs   = new ArrayList<>();
        Map<String, String> cfMap = getAllColumnFamiliesGut(orgId);

        for (Map.Entry<String, String> entry : cfMap.entrySet())
        {
            cfs.add(SeqStoreCf.newSeqStoreCF(entry.getKey(), entry.getValue()));
        }

        return cfs;
    }

    private Map<String, String> getAllColumnFamiliesGut(String orgId)
        throws Exception
    {
        String property = customerProperties.getSequenceStoreColumnFamily(orgId);
        return StringUtil.mapFromString(property);
    }

    public void sortCF(List<SeqStoreCf> sscfList)
    {
        Collections.sort(sscfList, new Comparator<SeqStoreCf>()
        {
            @Override
            public int compare(SeqStoreCf o1, SeqStoreCf o2)
            {
                if (o1.submitTime < o2.submitTime)
                    return -1;
                if (o1.submitTime > o2.submitTime)
                    return 1;

                return 0;
            }
        });
    }

    private String getOrgId(ReferenceType subject, String orgId)
        throws Exception
    {
        if (orgId == null && subject != null && subject.getType().equals("patient"))
        {
            return LinkDataUtility.getPdsIDByPatientUUID(applicationCqlCrud, UUID.fromString(subject.getUri()), linkCF);
        }
        else
        {
            return orgId;
        }
    }

    public Pair<UUID, List<UUID>> getAuthoritativePatientUUIDWithAll(UUID patientUUID) throws Exception
    {
        LinkDataUtility.PdsIDAndPatientUUIDs pdsIDAndPatientUUIDsByPatientUUID = LinkDataUtility.getPdsIDAndPatientUUIDsByPatientUUID(applicationCqlCrud, patientUUID, linkCF);
        LinkDataUtility.PdsIDAndPatientUUID  pdsIDAndAuthPatientUUID = LinkDataUtility.getPdsIDAndAuthPatientUUIDdByPatientUUID(applicationCqlCrud, patientUUID, linkCF);

        UUID       authUUID = pdsIDAndAuthPatientUUID != null ? pdsIDAndAuthPatientUUID.patientUUID : null;
        List<UUID> UUIDs    = pdsIDAndPatientUUIDsByPatientUUID != null ? pdsIDAndPatientUUIDsByPatientUUID.patientUUIDs : null;

        if (authUUID != null && !UUIDs.isEmpty())
        {
            return new ImmutablePair(authUUID, UUIDs);
        }
        else
        {
            return null;
        }

    }

    protected String getTag(EventType eventType)
    {
        EvidenceType evidenceType = eventType.getEvidence();
        if (evidenceType != null && evidenceType.isInferred())
        {
            String version =  getModelVersion(eventType);
            if(version == null)
            {
                version = historical;
            }

            return version;
        }

        if (isAnnotation(eventType))
            return global;
        else
            return non_inferred;

    }

    private String getModelVersion(EventType eventType)
    {
        AttributesType attributesType = eventType.getAttributes();
        if (attributesType == null )
            return null;

        List<AttributeType> attributes = attributesType.getAttribute();
        for (AttributeType attribute :attributes)
        {
            if (attribute.getName().equals(modelVersionName))
                return attribute.getValue();
        }

        return null;
    }

    private boolean isAnnotation(EventType eventType)
    {
        AttributesType attributesType = eventType.getAttributes();
        for(AttributeType attributeType : attributesType.getAttribute())
        {
            if (attributeType.getName().equals("sourceType")  && attributeType.getValue().equals("USER_ANNOTATION"))
                return true;

            //legacy...
            if (attributeType.getName().equals("sourceType")  && attributeType.getValue().equals("GOLD_STANDARD_ANNOTATION"))
                return true;
        }

        return false;
    }

    private String makeCF(String tag, String orgId)
    {
        if (tag.equals(global))
            return globalColumnFamily;
        else if (tag.equals(non_inferred))
            return "store" + non_inferred + orgId;
        return "store" + inferred + System.currentTimeMillis() + orgId;
    }
}
