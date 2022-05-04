package com.apixio.dao.seqstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.dao.seqstore.store.AddressStore;
import com.apixio.dao.seqstore.store.PathValueStore;
import com.apixio.dao.seqstore.store.QueryStore;
import com.apixio.dao.seqstore.store.QueryType;
import com.apixio.dao.seqstore.store.SubjectPathStore;
import com.apixio.dao.seqstore.store.SubjectPathValueStore;
import com.apixio.dao.seqstore.store.SubjectStore;
import com.apixio.dao.seqstore.utility.Criteria;
import com.apixio.dao.seqstore.utility.Criteria.TagCriteria;
import com.apixio.dao.seqstore.utility.Criteria.TagTypeCriteria;
import com.apixio.dao.seqstore.utility.Range;
import com.apixio.dao.seqstore.utility.SeqStoreCf;
import com.apixio.dao.seqstore.utility.SeqStoreCFUtility;
import com.apixio.dao.seqstore.utility.SeqStoreUtility;

import com.apixio.model.event.EventType;
import com.apixio.model.event.ReferenceType;
import com.apixio.model.event.EventAddress;
import com.apixio.model.event.EventEquality;
import com.apixio.utility.TimeHelper;

import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.datasource.cassandra.CqlCrud;

public class SeqStoreDAO
{
    public enum TagType
    {
        all,
        last,
        Inferred,
        NonInferred,
        Annotation
    }

    private static final Logger logger = LoggerFactory.getLogger(SeqStoreDAO.class);
    private static final int    DEFAULT_THREAD_POOL_SIZE = 5;

    private CqlCrud                cqlCrud;
    private SeqStoreCFUtility      seqStoreCFUtility;
    private SubjectStore           subjectStore;
    private PathValueStore         pathValueStore;
    private SubjectPathValueStore  subjectPathValueStore;
    private SubjectPathStore       subjectPathStore;
    private AddressStore           addressStore;
    private QueryStore             queryStore;
    private List<String>           paths = new ArrayList<>();

    private Map<TagType, List<String>> pathsMap = new HashMap<>();

    private int threadPoolSize;

    private final ThreadPoolExecutor executorService;

    public SeqStoreDAO()
    {
        this(DEFAULT_THREAD_POOL_SIZE);
    }

    public SeqStoreDAO(int threadPoolSize)
    {
        this.threadPoolSize = threadPoolSize;
        executorService = SeqStoreUtility.createDefaultExecutorService(this.threadPoolSize);
        executorService.setCorePoolSize((this.threadPoolSize + 1) / 2);
        executorService.setMaximumPoolSize(this.threadPoolSize);
    }

    public void setCqlCrud(CqlCrud cqlCrud)
    {
    	this.cqlCrud = cqlCrud;
    }

    public void setSeqStoreCFUtility(SeqStoreCFUtility seqStoreCFUtility)
    {
        this.seqStoreCFUtility = seqStoreCFUtility;
    }

    public void setSubjectStore(SubjectStore subjectStore)
    {
    	this.subjectStore = subjectStore;
    }

    public void setPathValueStore(PathValueStore pathValueStore)
    {
        this.pathValueStore = pathValueStore;
    }

    public void setSubjectPathValueStore(SubjectPathValueStore subjectPathValueStore)
    {
        this.subjectPathValueStore = subjectPathValueStore;
    }

    public void setSubjectPathStore(SubjectPathStore subjectPathStore)
    {
        this.subjectPathStore = subjectPathStore;
    }

    public void setAddressStore(AddressStore addressStore)
    {
        this.addressStore = addressStore;
    }

    public void setQueryStore(QueryStore queryStore)
    {
        this.queryStore = queryStore;
    }

    @Deprecated
    public void setPaths(String allPaths)
    {
        paths.clear();

        String[] pathList = allPaths.split(",");

        for (int index = 0; index < pathList.length; index++)
        {
            paths.add(pathList[index].trim());
        }

        logger.info("set old paths [" + allPaths + "]");
    }

    public void setPaths(TagType tagType, String allPaths)
    {
        logger.warn("setPaths for [" + tagType.name() + "] = [" + allPaths + "]");
        if (allPaths == null)
            return;

        String[] pathList = allPaths.split(",");

        List<String> pathsForTagType = new ArrayList<>();
        for (int index = 0; index < pathList.length; index++)
        {
            pathsForTagType.add(pathList[index].trim());
        }

        pathsMap.put(tagType, pathsForTagType);

        logger.warn("setPaths - setting tagType [" + tagType.toString() + "] = [" +
                StringUtils.join(pathsForTagType.toArray(), ",") + "]");
    }

    public void createSequenceStoreColumnFamily(String tag, boolean createNewInferredCF, String orgId)
        throws Exception
    {
        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("Cluster One - No Schema Agreement");

        seqStoreCFUtility.createColumnFamily(tag, createNewInferredCF, orgId);

        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("Cluster One - No Schema Agreement");
    }

    public void deleteSequenceStoreColumnFamily(String tag, String orgId)
       throws Exception
    {
        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("Cluster One - No Schema Agreement");

        seqStoreCFUtility.deleteColumnFamily(tag, orgId);

        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("Cluster One - No Schema Agreement");
    }

    public String getSequenceStoreColumnFamily(String tag, String orgId)
        throws Exception
    {
        SeqStoreCf seqStoreCf =  seqStoreCFUtility.getColumnFamily(tag, orgId);

        return (seqStoreCf != null ? seqStoreCf.cf : null);
    }

    public void refreshSequenceStore(String orgId)
        throws Exception
    {
        seqStoreCFUtility.refreshSequenceStore(orgId);
    }

    public void markSequenceStoresHaveData()
        throws Exception
    {
        seqStoreCFUtility.markSequenceStoresHaveData();
    }

    public void markSequenceStoreHasData(String tag, String orgId)
       throws Exception
    {
        seqStoreCFUtility.markSequenceStoreHasData(tag, orgId);
    }

    public void markSequenceStoreMigrationCompleted(String tag, String orgId)
        throws Exception
    {
        seqStoreCFUtility.markSequenceStoreMigrationCompleted(tag, orgId);
    }

    /**
     * This method adds a list of events to all sequence stores:
     * 1. Subject Store (two level indexing) - For each subject, a time series of events
     * 2. Path Value Store (single level indexing) - For each subject, a list of events indexed by path+value combination
     * 3. Subject Path Value Store (two level indexing) - For each combination of subject + path + value, a time series of events
     * 4. Subject Path Store (two level indexing) - For each combination of subject + path, a time series of events
     * 5. Address Store (single level indexing) - Each event accessed by event address
     *
     * - Subject Store (two level indexing):
     * ** First level:   sp_ + subject -> end time
     * ** Second level:  s_  + subject -> (end time + insertion time + random number; list of events)
     *
     * Path Value Store (single level indexing)
     *
     *
     *
     * @param eventTypes
     * @param orgId
     * @throws Exception
     * @return Map<EventType, Boolean> to verify writes
     */
    public Map<EventType, Boolean> putAndCheck(List<EventType> eventTypes, String orgId)
        throws Exception
    {
        putGuts(eventTypes, orgId, false);

        Map<EventType, Boolean> verifyDB = new HashMap<>();

        for (EventType eventType: eventTypes)
        {
            String addressId = EventAddress.getEventAddress(eventType);
            SeqStoreCFUtility.ColumnFamilyMetadata columnFamilyMetadata = getWriteColumnFamily(eventType, orgId, false);
            String columnFamily = columnFamilyMetadata.columnFamily;
            if (columnFamily != null)
            {
                List<EventType> events = queryStore.getAddressableSequenceElements(addressId, columnFamily);

                if (eventContains(events, eventType))
                    verifyDB.put(eventType, true);
                else
                    verifyDB.put(eventType, false);
            }
            else
            {
                verifyDB.put(eventType, false);
            }
        }

        return verifyDB;
    }

    private boolean eventContains(List<EventType> events, EventType eventType)
        throws Exception
    {
        for (EventType e : events)
        {
            if (EventEquality.equals(e, eventType) == true)
                return true;
        }

        return false;
    }

    /**
     * This method adds a list of events to all sequence stores:
     * 1. Subject Store (two level indexing) - For each subject, a time series of events
     * 2. Path Value Store (single level indexing) - For each subject, a list of events indexed by path+value combination
     * 3. Subject Path Value Store (two level indexing) - For each combination of subject + path + value, a time series of events
     * 4. Subject Path Store (two level indexing) - For each combination of subject + path, a time series of events
     * 5. Address Store (single level indexing) - Each event accessed by event address
     *
     * - Subject Store (two level indexing):
     * ** First level:   sp_ + subject -> end time
     * ** Second level:  s_  + subject -> (end time + insertion time + random number; list of events)
     *
     * Path Value Store (single level indexing)
     *
     *
     *
     * @param eventTypes
     * @param orgId
     * @throws Exception
     */
    public void put(List<EventType> eventTypes, String orgId)
        throws Exception
    {
        putGuts(eventTypes, orgId, true);
    }

    /**
     * This method adds a list of events to all sequence stores:
     * 1. Subject Store (two level indexing) - For each subject, a time series of events
     * 2. Path Value Store (single level indexing) - For each subject, a list of events indexed by path+value combination
     * 3. Subject Path Value Store (two level indexing) - For each combination of subject + path + value, a time series of events
     * 4. Subject Path Store (two level indexing) - For each combination of subject + path, a time series of events
     * 5. Address Store (single level indexing) - Each event accessed by event address
     *
     * - Subject Store (two level indexing):
     * ** First level:   sp_ + subject -> end time
     * ** Second level:  s_  + subject -> (end time + insertion time + random number; list of events)
     *
     * Path Value Store (single level indexing)
     *
     * @param eventTypes
     * @param orgId
     * @throws Exception
     */
    private void putGuts(List<EventType> eventTypes, String orgId, boolean useThreadLocalCache)
        throws Exception
    {
        LocalCqlCache localCqlCache = useThreadLocalCache ? null: getLocalCqlCache();

        boolean batchSyncEnabled = true;

        //
        //
        //@TODO: Investigate this:
        //       java.lang.NoSuchMethodError: com.apixio.datasource.cassandra.CqlCrud.isBatchSyncEnabled()Z
        //
        try
        {
            batchSyncEnabled = cqlCrud.isBatchSyncEnabled();
        }
        catch (NoSuchMethodError ex)
        {
            logger.error(ex.getMessage(), ex);
        }

        if (localCqlCache != null)
        {
            cqlCrud.setBatchSyncEnabled(true);
        }

        for (EventType eventType : eventTypes)
        {
            List<EventType> singlePass = new ArrayList<>();
            singlePass.add(eventType);
            SeqStoreCFUtility.ColumnFamilyMetadata columnFamilyMetadata = getWriteColumnFamily(eventType, orgId, true);
            if (columnFamilyMetadata != null)
                putGutsIntoSingleColumn(singlePass, columnFamilyMetadata, localCqlCache);
        }

        if (localCqlCache != null)
        {
            localCqlCache.flush();
            cqlCrud.setBatchSyncEnabled(batchSyncEnabled);
        }
    }


    private SeqStoreCFUtility.ColumnFamilyMetadata getWriteColumnFamily(EventType eventType, String orgId, boolean updateHasData)
    {
        try
        {
            return seqStoreCFUtility.getWriteColumnFamily(eventType, orgId, updateHasData);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void putGutsIntoSingleColumn(List<EventType> eventTypes, SeqStoreCFUtility.ColumnFamilyMetadata columnFamilyMetadata, LocalCqlCache localCqlCache)
        throws Exception
    {
        String insertionTime = TimeHelper.nowAsString();

        String columnFamily = columnFamilyMetadata.columnFamily;

        logger.info("tagType = [" + columnFamilyMetadata.tagType.toString() + "]");

        List<String> pathsForTagType = pathsMap.get(TagType.Inferred);

        if (pathsForTagType == null)
        {
            logger.warn("paths set for [" + TagType.Inferred.name() + "]");
        }

        if (SeqStoreCFUtility.global.equalsIgnoreCase(columnFamilyMetadata.tagType))
        {
            pathsForTagType = pathsMap.get(TagType.Annotation);

            if(pathsForTagType == null)
            {
                logger.warn("paths set for [" + TagType.Annotation.name() + "]");
            }
        }
        else if (SeqStoreCFUtility.non_inferred.equalsIgnoreCase(columnFamilyMetadata.tagType))
        {
            pathsForTagType = pathsMap.get(TagType.NonInferred);
            if(pathsForTagType == null)
            {
                logger.warn("paths set for [" + TagType.NonInferred.name() + "]");
            }
        }

        if (pathsForTagType == null)
        {
            pathsForTagType = paths;
            logger.warn("could not find path for [" + columnFamilyMetadata.tagType  + "]");
        }
        else
        {
            logger.info("Found paths for tagType [" + columnFamilyMetadata.tagType.toString() + "] = [" +
                    StringUtils.join(pathsForTagType.toArray(), ",") + "]");
        }

        subjectStore.put(eventTypes, insertionTime, columnFamily, localCqlCache);
        pathValueStore.put(eventTypes, pathsForTagType, columnFamily, localCqlCache);
        subjectPathValueStore.put(eventTypes, pathsForTagType, insertionTime, columnFamily, localCqlCache);
        subjectPathStore.put(eventTypes, pathsForTagType, insertionTime, columnFamily, localCqlCache);
        addressStore.put(eventTypes, insertionTime, columnFamily, localCqlCache);
    }

    private LocalCqlCache getLocalCqlCache()
    {
        LocalCqlCache localCqlCache = new LocalCqlCache();
        localCqlCache.setBufferSize(0);
        localCqlCache.setDontFlush(true);
        localCqlCache.setCqlCrud(cqlCrud);

        return localCqlCache;
    }

///// ------ For backwards compatibility ---------- /////////

    @Deprecated
    public List<EventType> getSequence(ReferenceType subject, long qst, long qet, String orgId)
            throws Exception
    {
        TagTypeCriteria criteria = new TagTypeCriteria.Builder().setSubject(subject)
                .setTagType(TagType.all).setOrgId(orgId)
                .setRange(new Range.RangeBuilder().setStart(qst).setEnd(qet).build()).build();

        return getSequence(criteria);
    }

    @Deprecated
    public List<EventType> getSequence(ReferenceType subject, String path, String value, long qst, long qet, String orgId)
            throws Exception
    {
        TagTypeCriteria criteria =  new TagTypeCriteria.Builder().setSubject(subject).setPath(path).setValue(value)
                .setTagType(TagType.all).setOrgId(orgId)
                .setRange(new Range.RangeBuilder().setStart(qst).setEnd(qet).build()).build();
        return getSequence(criteria);
    }

    @Deprecated
    public EventType getAddressableSequenceElement(String addressId, String orgId)
            throws Exception
    {
        TagTypeCriteria criteria =  new TagTypeCriteria.Builder().setTagType(TagType.all)
                .setAddressId(addressId).setOrgId(orgId).setRange(Range.DEFAULT_RANGE).build();

        return getAddressableSequenceElement(criteria);
    }


///// ------ New Methods ---------- /////////

    public Pair<UUID, Set<ReferenceType>> fanOutPatientUUID(final Criteria criteria) throws Exception
    {
        Set<ReferenceType> referenceTypes = new HashSet<>();
        ReferenceType referenceType = criteria.getSubject();
        MutablePair<UUID, Set<ReferenceType>> result = new MutablePair<>();

        if(referenceType.getType().equalsIgnoreCase("patient"))
        {
            Pair<UUID, List<UUID>> authoritativePatientUUIDWithAll = seqStoreCFUtility.getAuthoritativePatientUUIDWithAll(UUID.fromString(referenceType.getUri()));
            if (authoritativePatientUUIDWithAll != null)
            {
                for (UUID uuid : authoritativePatientUUIDWithAll.getRight())
                {
                    ReferenceType rt = new ReferenceType();
                    rt.setType("patient");
                    rt.setUri(uuid.toString());
                    referenceTypes.add(rt);
                }

                result.setLeft(authoritativePatientUUIDWithAll.getLeft());
                result.setRight(referenceTypes);
            }
        }

        return result;
    }

    public List<EventType> getSequence(final Criteria criteria)
            throws Exception
    {
        List<String> cfList = getColumnFamily(criteria, false);
        List<EventType> events = new ArrayList<>();

        Set<ReferenceType> referenceTypes;
        if(criteria.isAllowMultipleUUIDCorrection())
        {
            Pair<UUID, Set<ReferenceType>> authIdAndFanOut = fanOutPatientUUID(criteria);
            referenceTypes = authIdAndFanOut.getRight();
        }
        else
        {
            referenceTypes = Collections.singleton(criteria.getSubject());
        }

        for(final ReferenceType referenceType : referenceTypes)
        {
            if (criteria.isAllowConcurrentQuery() && cfList.size() > 1)
            {
                List<Future<List<EventType>>> futures = new ArrayList<>();
                for (final String cf : cfList)
                {
                    Future<List<EventType>> future = executorService.submit(new Callable<List<EventType>>()
                    {
                        @Override
                        public List<EventType> call() throws Exception
                        {
                            return queryStore.getSequence(resolveQueryType(criteria), referenceType, criteria.getPath(), criteria.getValue(),
                                    criteria.getRange().getStart(), criteria.getRange().getEnd(), cf);
                        }
                    });

                futures.add(future);
            }

                for (Future<List<EventType>> future : futures)
                {
                    events.addAll(future.get());
                }
            } else
            {
                for (final String cf : cfList)
                {
                    events.addAll(queryStore.getSequence(resolveQueryType(criteria), referenceType, criteria.getPath(), criteria.getValue(),
                            criteria.getRange().getStart(), criteria.getRange().getEnd(), cf));
                }
            }
        }

        if(referenceTypes.size() > 1)
        {
            reconcileEventsWithAuthoritativeId(events, UUID.fromString(criteria.getSubject().getUri()));
        }

        return events;
    }

    public List<Iterator<EventType>> getIteratorSequence(final Criteria criteria)
        throws Exception
    {
        List<String>               cfList = getColumnFamily(criteria, false);
        List<Iterator<EventType>>  events = new ArrayList<>();

        Set<ReferenceType> referenceTypes;
        if(criteria.isAllowMultipleUUIDCorrection())
        {
            Pair<UUID, Set<ReferenceType>> authIdAndFanOut = fanOutPatientUUID(criteria);
            referenceTypes = authIdAndFanOut.getRight();
        }
        else
        {
            referenceTypes = Collections.singleton(criteria.getSubject());
        }

        for(final ReferenceType referenceType : referenceTypes)
        {
            if (criteria.isAllowConcurrentQuery() && cfList.size() > 1)
            {
                List<Future<Iterator<EventType>>> futures = new ArrayList<>();
                for (final String cf : cfList)
                {
                    Future<Iterator<EventType>> future = executorService.submit(new Callable<Iterator<EventType>>()
                    {
                        @Override
                        public Iterator<EventType> call() throws Exception
                        {
                            return queryStore.getIteratorSequence(resolveQueryType(criteria), referenceType, criteria.getPath(), criteria.getValue(),
                                    criteria.getRange().getStart(), criteria.getRange().getEnd(), cf);
                        }
                    });

                    futures.add(future);
                }

                for (Future<Iterator<EventType>> future : futures)
                {
                    events.add(future.get());
                }
            } else
            {
                for (final String cf : cfList)
                {
                    events.add(queryStore.getIteratorSequence(resolveQueryType(criteria), referenceType, criteria.getPath(), criteria.getValue(),
                            criteria.getRange().getStart(), criteria.getRange().getEnd(), cf));
                }
            }
        }

        if(referenceTypes.size() > 1)
        {
            List<Iterator<EventType>> reconcilingEvents = new ArrayList<>();

            for(Iterator<EventType> iterator : events)
            {
                reconcilingEvents.add(new ReconcilingEventTypeIterator(iterator, UUID.fromString(criteria.getSubject().getUri())));
            }

            events = reconcilingEvents;
        }

        return events;
    }

    public List<String> getSequencePaths(final Criteria criteria)
            throws Exception
    {
        List<String> cfList = getColumnFamily(criteria, false);

        Set<String>   paths  = new TreeSet<>();


        Set<ReferenceType> referenceTypes;
        if(criteria.isAllowMultipleUUIDCorrection())
        {
            Pair<UUID, Set<ReferenceType>> authIdAndFanOut = fanOutPatientUUID(criteria);
            referenceTypes = authIdAndFanOut.getRight();
        }
        else
        {
            referenceTypes = Collections.singleton(criteria.getSubject());
        }

        for(final ReferenceType referenceType : referenceTypes)
        {
            if (criteria.isAllowConcurrentQuery() && cfList.size() > 1)
            {
                List<Future<List<String>>> futures = new ArrayList<>();
                for (final String cf : cfList)
                {
                    Future<List<String>> future = executorService.submit(new Callable<List<String>>()
                    {
                        @Override
                        public List<String> call() throws Exception
                        {
                            return queryStore.getSequencePaths(referenceType, cf);
                        }
                    });

                    futures.add(future);
                }

                for (Future<List<String>> future : futures)
                {
                    paths.addAll(future.get());
                }
            } else
            {
                for (final String cf : cfList)
                {
                    paths.addAll(queryStore.getSequencePaths(referenceType, cf));
                }
            }
        }

        return new ArrayList<>(paths);
    }

    public List<String> getSequenceValues(final Criteria criteria)
            throws Exception
    {
        List<String> cfList = getColumnFamily(criteria, false);

        Set<String>   values = new TreeSet<>();

        Set<ReferenceType> referenceTypes;
        if(criteria.isAllowMultipleUUIDCorrection())
        {
            Pair<UUID, Set<ReferenceType>> authIdAndFanOut = fanOutPatientUUID(criteria);
            referenceTypes = authIdAndFanOut.getRight();
        }
        else
        {
            referenceTypes = Collections.singleton(criteria.getSubject());
        }

        for(final ReferenceType referenceType : referenceTypes)
        {
            if (criteria.isAllowConcurrentQuery() && cfList.size() > 1)
            {
                List<Future<List<String>>> futures = new ArrayList<>();
                for (final String cf : cfList)
                {
                    Future<List<String>> future = executorService.submit(new Callable<List<String>>()
                    {
                        @Override
                        public List<String> call() throws Exception {
                            return queryStore.getSequenceValues(referenceType, criteria.getPath(), cf);
                        }
                    });

                    futures.add(future);
                }

                for (Future<List<String>> future : futures)
                {
                    values.addAll(future.get());
                }
            }
            else
            {
                for (final String cf : cfList)
                {
                    values.addAll(queryStore.getSequenceValues(referenceType, criteria.getPath(), cf));
                }
            }
        }

        return new ArrayList<>(values);
    }

    public List<EventType> getAddressableSequenceElements(final Criteria criteria)
            throws Exception
    {
        List<String> cfList = getColumnFamily(criteria, true);
        List<EventType>  events = new ArrayList<>();

        if ( criteria.isAllowConcurrentQuery() && cfList.size() > 1 )
        {
            List<Future<List<EventType>>> futures = new ArrayList<>();
            for (final String cf : cfList)
            {
                Future<List<EventType>> future = executorService.submit(new Callable<List<EventType>>()
                {
                    @Override
                    public List<EventType> call() throws Exception
                    {
                        return queryStore.getAddressableSequenceElements(criteria.getAddressId(), cf);
                    }
                });

                    futures.add(future);
                }

            for (Future<List<EventType>> future : futures)
            {
                events.addAll(future.get());
            }
        }
        else
        {
            for (final String cf : cfList)
            {
                events.addAll(queryStore.getAddressableSequenceElements(criteria.getAddressId(), cf));
            }
        }

        return events;
    }

    public EventType getAddressableSequenceElement(final Criteria criteria)
            throws Exception
    {
        List<String> cfList = getColumnFamily(criteria, true);
        List<EventType>  events = new ArrayList<>();

        if (criteria.isAllowConcurrentQuery() && cfList.size() > 1) {
            List<Future<EventType>> futures = new ArrayList<>();

            for (final String cf : cfList)
            {
                Future<EventType> future = executorService.submit(new Callable<EventType>()
                {
                    @Override
                    public EventType call() throws Exception {
                        return queryStore.getAddressableSequenceElement(criteria.getAddressId(), cf);
                    }
                });

                futures.add(future);
            }

            for (Future<EventType> future : futures)
            {
                events.add(future.get());
            }
        }
        else
        {
            for (final String cf : cfList)
            {
                events.add(queryStore.getAddressableSequenceElement(criteria.getAddressId(), cf));
            }
        }

        if (events.isEmpty())
            return null;

        return events.get(events.size() - 1);
    }

    public List<String> getModels(TagTypeCriteria criteria) throws Exception
    {
        List<String> tags = new ArrayList<>();

        List<SeqStoreCf> sscfs = getColumnFamiliesGut(criteria, false);

        for (SeqStoreCf inferredCF: sscfs)
        {
            tags.add(inferredCF.tag);
        }

        return tags;
    }

    public List<SeqStoreCf> getAllColumnFamilies(TagTypeCriteria criteria) throws Exception
    {
        return getColumnFamiliesGut(criteria, false);
    }

    // --- common methods -----

    private List<String> getColumnFamily(Criteria criteria, boolean isAddressableSequenceRequest)
        throws Exception
    {
        List<SeqStoreCf> sscfs = getColumnFamiliesGut(criteria, isAddressableSequenceRequest);
        return getAllColumnFamilyNames(sscfs);
    }

    private List<SeqStoreCf> getColumnFamiliesGut(Criteria criteria, boolean isAddressableSequenceRequest)
        throws Exception
    {
        List<SeqStoreCf> sscfs = null;

        if (criteria instanceof  TagTypeCriteria)
        {
            sscfs = getAllColumnFamiliesGut((TagTypeCriteria) criteria);
        }
        else if (criteria instanceof TagCriteria)
        {
            sscfs = new ArrayList<>();

            for (String tag : ((TagCriteria)criteria).getTags())
            {
                ReferenceType subject = isAddressableSequenceRequest ? null : criteria.getSubject();
                SeqStoreCf sscf = seqStoreCFUtility.getColumnFamily(subject, tag, criteria.getOrgId());

                if (sscf != null && sscf.hasData)
                {
                    sscfs.add(sscf);
                }
            }
        }

        return sscfs;
    }

    private QueryType resolveQueryType(Criteria criteria)
    {
        QueryType queryType = QueryType.QuerySubjectStore;

        //resolve query type from criteria
        if(criteria.getPath() != null)
        {
            queryType = (criteria.getValue() == null) ? QueryType.QuerySubjectPathStore : QueryType.QuerySubjectPathValueStore;
        }

        return queryType;
    }

    /*
     all,
     last, // noy yet supported
     Inferred,
     NonInferred,
     Annotation
     */
    private List<SeqStoreCf> getAllColumnFamiliesGut(TagTypeCriteria criteria)
        throws Exception
    {
        List<SeqStoreCf> cfList = new ArrayList<>();
        SeqStoreCf historical   = null;

        List<SeqStoreCf> cfs = seqStoreCFUtility.getAllColumnFamilies(criteria.getSubject(), criteria.getOrgId());

        SeqStoreCf lastOne = null;

        for (SeqStoreCf sscf : cfs)
        {
            if (sscf.tag.equals(SeqStoreCFUtility.historical))
                historical = sscf;

            // IMPORTANT - WE DON'T FILTER TABLES  BASED ON TIME - VK

            if (!sscf.hasData)
            {
                logger.debug("Skipping: [" + sscf.toString() + "]");
                continue;
            }

            if (criteria.getTagType() == TagType.all)
                cfList.add(sscf);

            else if (criteria.getTagType() == TagType.NonInferred && sscf.tag.equals(SeqStoreCFUtility.non_inferred))
                cfList.add(sscf);

            else if (criteria.getTagType() == TagType.Annotation && sscf.tag.equals(SeqStoreCFUtility.global))
                cfList.add(sscf);

            else if (criteria.getTagType() == TagType.Inferred || criteria.getTagType() == TagType.last)
            {
                if ( sscf.tag.equals(SeqStoreCFUtility.historical) ||
                        sscf.tag.equals(SeqStoreCFUtility.global) ||
                        sscf.tag.equals(SeqStoreCFUtility.non_inferred) )
                    continue;

                cfList.add(sscf);

                if (lastOne == null)
                    lastOne = sscf;
                else if (lastOne.submitTime < sscf.submitTime)
                    lastOne = sscf;
            }
        }

        if (criteria.getTagType() == TagType.last)
        {
            if (lastOne != null)
            {
                return Arrays.asList(lastOne);
            }
            else
            {
                return new ArrayList<>();
            }
        }

        if (logger.isDebugEnabled())
        {
            for (SeqStoreCf cf : cfList)
            {
                logger.debug("Reading from: [" + cf.toString() + "]");
            }
        }

        logger.debug("Reading from: [" + cfList + "]");

        addHistoricalCfIfNecessary(cfList, historical);
        seqStoreCFUtility.sortCF(cfList);

        return cfList;
    }

    private List<String> getAllColumnFamilyNames(List<SeqStoreCf> seqStoreCfs)
    {
        // We use set since multiple models might point to the same CF (including historical)
        Set<String> cfs = new HashSet<>();
        for (SeqStoreCf cf : seqStoreCfs)
        {
            cfs.add(cf.cf);
        }

        return new ArrayList<>(cfs);
    }

    private void addHistoricalCfIfNecessary(List<SeqStoreCf> cfList, SeqStoreCf historical)
    {
        if (historical == null)
            return;

        boolean addHistorical = false;

        if (cfList.isEmpty())
        {
            addHistorical = true;
        }
        else
        {
            for (SeqStoreCf sscf : cfList)
            {
                if (!sscf.migrationCompleted)
                    addHistorical = true;
            }
        }

        if (addHistorical && !cfList.contains(historical) && historical.hasData)
            cfList.add(historical);
    }

    private void reconcileEventsWithAuthoritativeId(List<EventType> events, UUID authUUID)
    {
        for (EventType event : events)
        {
            event.getSubject().setUri(authUUID.toString());
        }
    }

    public static class ReconcilingEventTypeIterator implements Iterator<EventType> {

        final private Iterator<EventType> eventIterator;
        final private UUID authUUID;

        public ReconcilingEventTypeIterator(Iterator<EventType> iterator, UUID uuid)
        {
            eventIterator = iterator;
            authUUID = uuid;
        }

        @Override
        public boolean hasNext()
        {
            return eventIterator.hasNext();
        }

        @Override
        public EventType next()
        {
            EventType eventType = eventIterator.next();
            eventType.getSubject().setUri(authUUID.toString());
            return eventType;
        }

        @Override
        public void remove()
        {
            eventIterator.remove();
        }

    }
}
