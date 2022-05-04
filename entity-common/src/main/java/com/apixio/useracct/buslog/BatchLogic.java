package com.apixio.useracct.buslog;

import com.apixio.XUUID;
import com.apixio.datasource.redis.DistLock;
import com.apixio.restbase.DataServices;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.entity.PatientDataSet;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Batch Logic first prototype will implement batch entities inside pds properties. Once first class fields are
 * finalized and access patterns are established this should become the logic layer over independent redis entities.
 *
 * A batch represent a set of data files processed in our pipeline.
 * There will be initially two types of batch: Upload and Extract
 *
 * @author lschneider
 */
public class BatchLogic extends LogicBase<DataServices> {

    // ttl for upload batches by pds
    private static final String uploadBatchCacheDuration = "uploadBatchConfig.cacheDuration";

    private static Logger log = LoggerFactory.getLogger(BatchLogic.class);

    private static final String UPLOAD_BATCHES = "uploadbatches";
    private static final String EXTRACT_BATCHES = "extractbatches";
    
    /**
     * Prototype requires only pdsLogic. TODO: Batch DAO
     */
    private PatientDataSetLogic pdsLogic;
    private DistLock distLock;

    private int cacheDuration = 0; //no cache as default, and use second as the time unit.
    
    //Use the cache with a configurable ttl setting (uploadBatchConfig.cacheDuration), default is 0 second which means no local cache.
    //To reduce frequency to hit redis when call getPatientDataSetsProperty and time spent to deserialize object by pds,
    //we can configure the setting to a reasonable value depends on application services.
    //e.g. we can set its value as 900 seconds in the apx-sessionmanager.conf
    private Cache<String, List<UploadBatch>> uploadBatchCacheByPds;
    
    /**
     * The various types of Batch management failure.
     */
    public enum FailureType {
        /**
         * for modify
         */
        NOT_EXISTING,
        ALREADY_STARTED,
        /**
         * for create
         */
        NAME_TAKEN
    }

    /**
     * If batch operations fail they will throw an exception of this class.
     */
    public static class BatchException extends BaseException {

        private FailureType failureType;

        public BatchException(FailureType failureType, String details, Object... args) {
            super(failureType);
            super.description(details, args);
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * Constructor.
     */
    public BatchLogic(DataServices dataServices, ConfigSet configuration) {
        super(dataServices);
        if (configuration != null) {
            try {
                cacheDuration = configuration.getInteger(uploadBatchCacheDuration);
            } catch (IllegalArgumentException e) {
                log.debug(String.format("Failed to find the config setting (%s) and use 0 as default.", uploadBatchCacheDuration));
            }
            log.info(String.format("%s is %ds.", uploadBatchCacheDuration, cacheDuration));
        }
    }

    @Override
    public void postInit() {
        pdsLogic = sysServices.getPatientDataSetLogic();

        Map<String, String> propDefs = pdsLogic.getPropertyDefinitions(false);
        log.debug("BatchLog DEBUG: propDefs = " + propDefs.toString());

        if (propDefs.containsKey(UPLOAD_BATCHES)) {
            log.debug("BatchLogic DEBUG: uploadbatches pds property already defined.");
        } else {
            log.debug("BatchLogic DEBUG: uploadbatches pds property not defined.");
            pdsLogic.addPropertyDef(UPLOAD_BATCHES, PropertyType.STRING);
            log.debug("BatchLogic DEBUG: uploadbatches pds property created.");
        }
        if (propDefs.containsKey(EXTRACT_BATCHES)) {
            log.debug("BatchLogic DEBUG: extractbatches pds property already defined.");
        } else {
            log.debug("BatchLogic DEBUG: extractbatches pds property not defined.");
            pdsLogic.addPropertyDef(EXTRACT_BATCHES, PropertyType.STRING);
            log.debug("BatchLogic DEBUG: extractbatches pds property created.");
        }

        distLock = new DistLock(sysServices.getRedisOps(), "batch-lock-");

        if (cacheDuration > 0) {
            uploadBatchCacheByPds = CacheBuilder.newBuilder().
                expireAfterWrite(cacheDuration, TimeUnit.SECONDS).build();
        }
    }
    
    public UploadBatch createUploadBatch(String name, XUUID pdsId) throws InterruptedException {
        PatientDataSet pds = pdsLogic.getPatientDataSetByID(pdsId);

        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(UPLOAD_BATCHES);
        List<UploadBatch> pdsBatches;
        if (allBatches.containsKey(pdsId)) {
            pdsBatches = UploadBatch.fromJSON((String) allBatches.get(pdsId));
            for (UploadBatch b : pdsBatches)
                if (b.getName().equals(name))
                    throw new BatchException(FailureType.NAME_TAKEN, "This batch already exits, no need to create it.");
        } else {
            pdsBatches = new ArrayList<>();
        }

        UploadBatch newBatch = new UploadBatch();
        newBatch.setName(name);
        newBatch.setId(XUUID.create(UploadBatch.OBJTYPE_PREFIX).toString());
        newBatch.setStartDate(new Date()); // assuming that the constructor using current machine time is fine
        newBatch.setPds(pdsId.toString());

        pdsBatches.add(newBatch);
        String batchesJson = UploadBatch.toJSON(pdsBatches);
        String key = name + pdsId.toString();
        String lock = distLock.lock(key, 5000);
        if (lock != null) {
            pdsLogic.setPatientDataSetProperty(pds, UPLOAD_BATCHES, batchesJson);
            distLock.unlock(lock, key);
            return newBatch;
        } else {
            // batch is already being created, we should wait out the lock and then fetch it
            int tries = 0;
            while (tries++ < 10) {
                Thread.sleep(1000);
                UploadBatch b = getUploadBatch(name, pdsId);
                if (b != null)
                    return b;

                log.debug("TVS:  waiting for 1 second to see if batch gets uploaded: iter " + tries + ": " + key);
            }

            log.debug("TVS:  No new batch was uploaded after 10 seconds!  key = " + key);
            throw new BatchException(FailureType.NOT_EXISTING, "TVS:  No new batch was uploaded after 10 seconds! " + key);
        }
    }
    public UploadBatch createUploadBatch(String name, long coid) throws InterruptedException {
        return createUploadBatch(name, PatientDataSetLogic.patientDataSetIDFromLong(coid));
    }

    public static String computeExtractBatchName(String parentBatchName, String sourceSystem,
                                                 String modelVersion, String pipelineVersion) {
        String space = "^^";
        return new StringBuffer("").append(parentBatchName).append(space).append(standardizeEmpty(sourceSystem)).append(space)
            .append(modelVersion).append(space).append(pipelineVersion).toString();
    }
    public static String standardizeEmpty(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("n/a") || value.equalsIgnoreCase("null")
            || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("unknown"))
            return "";
        else
            return value.trim();
    }

    public EventExtractBatch createExtractBatch(String parentBatchName, XUUID pdsId,
                                                String sourceSystem, String modelVersion, String pipelineVersion) throws InterruptedException {
        PatientDataSet pds = pdsLogic.getPatientDataSetByID(pdsId);
        //UploadBatch parentBatch = getUploadBatch(parentBatchName, pdsId); // parent batch may or may not match another entity. else it is just an arbitrary pipeline batch.
        String name = computeExtractBatchName(parentBatchName, sourceSystem, modelVersion, pipelineVersion);

        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(EXTRACT_BATCHES);
        List<EventExtractBatch> pdsBatches;
        if (allBatches.containsKey(pdsId)) {
            pdsBatches = EventExtractBatch.fromJSON((String) allBatches.get(pdsId));
            for (EventExtractBatch b : pdsBatches)
                if (b.getName().equals(name))
                    throw new BatchException(FailureType.NAME_TAKEN, "This batch already exits, no need to create it.");
        } else {
            pdsBatches = new ArrayList<>();
        }

        EventExtractBatch newBatch = new EventExtractBatch();
        newBatch.setName(name);
        newBatch.setId(XUUID.create(EventExtractBatch.OBJTYPE_PREFIX).toString());
        newBatch.setStartDate(new Date());
        newBatch.setPds(pdsId.toString());
        newBatch.setParentUploadBatch(parentBatchName);
        newBatch.setSourceSystem(standardizeEmpty(sourceSystem));
        newBatch.setModelVersion(modelVersion);
        newBatch.setPipelineVersion(pipelineVersion);

        pdsBatches.add(newBatch);
        String batchesJson = EventExtractBatch.toJSON(pdsBatches);
        String key = name + pdsId.toString();
        String lock = distLock.lock(key, 5000);
        if (lock != null) {
            pdsLogic.setPatientDataSetProperty(pds, EXTRACT_BATCHES, batchesJson);
            distLock.unlock(lock, key);
            return newBatch;
        } else {
            int tries = 0;
            while (tries++ < 10) {
                Thread.sleep(1000);
                EventExtractBatch b = getExtractBatch(name, pdsId);
                if (b != null)
                    return b;

                log.debug("TVS:  waiting for 1 second to see if batch gets created: iter " + tries + ": " + key);
            }

            log.debug("TVS:  No new batch was created after 10 seconds!  key = " + key);
            throw new BatchException(FailureType.NOT_EXISTING, "TVS:  No new batch was created after 10 seconds! " + key);
        }
    }

    public EventExtractBatch createExtractBatch(String parentBatchName, long coid,
                                                String sourceSystem, String modelVersion, String pipelineVersion) throws InterruptedException {
        return createExtractBatch(parentBatchName, PatientDataSetLogic.patientDataSetIDFromLong(coid),
            sourceSystem, modelVersion, pipelineVersion);
    }

    /////////
    // get //
    /////////

    public UploadBatch getUploadBatch(String name, XUUID pdsId) {
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(UPLOAD_BATCHES);
        if (allBatches.containsKey(pdsId)) {
            List<UploadBatch> pdsBatches = UploadBatch.fromJSON((String) allBatches.get(pdsId));
            for (UploadBatch b : pdsBatches)
                if (b.getName().equals(name))
                    return b;
        }
        return null;
    }
    public UploadBatch getUploadBatch(String name, long coid) {
        return getUploadBatch(name, PatientDataSetLogic.patientDataSetIDFromLong(coid));
    }

    public UploadBatch getUploadBatch(XUUID batchId, XUUID pdsId) {
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(UPLOAD_BATCHES);
        if (allBatches.containsKey(pdsId)) {
            List<UploadBatch> pdsBatches = UploadBatch.fromJSON((String) allBatches.get(pdsId));
            for (UploadBatch b : pdsBatches)
                if (b.getId().equals(batchId.toString()))
                    return b;
        }
        return null;
    }

    public UploadBatch getUploadBatch(XUUID batchId) {
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(UPLOAD_BATCHES);
        for (XUUID pdsId : allBatches.keySet()) {
            List<UploadBatch> pdsBatches = UploadBatch.fromJSON((String) allBatches.get(pdsId));
            for (UploadBatch b : pdsBatches)
                if (b.getId().equals(batchId.toString()))
                    return b;
        }
        return null;
    }

    public List<UploadBatch> getBadUploadBatches(XUUID pdsId, long start, long end) {
        List<UploadBatch> badBatches = new ArrayList<>();
        List<UploadBatch> batchesByPds;
        if (uploadBatchCacheByPds != null) {
            batchesByPds = uploadBatchCacheByPds.getIfPresent(pdsId.getID());
            if (batchesByPds != null) {
                badBatches = getBadUploadBatches(batchesByPds, start, end);
                log.debug(String.format("Get all the upload batches per pds from the local cache: %s, bad batches: %s",
                    Arrays.toString(batchesByPds.toArray()), Arrays.toString(badBatches.toArray())));
                return badBatches;
            }
        }
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(UPLOAD_BATCHES);
        if (allBatches.containsKey(pdsId)) {
            batchesByPds = UploadBatch.fromJSON((String) allBatches.get(pdsId));
            if (uploadBatchCacheByPds != null) {
                uploadBatchCacheByPds.put(pdsId.getID(), batchesByPds);
            }
            badBatches = getBadUploadBatches(batchesByPds, start, end);
            log.debug(String.format("Put all the upload batches per pds into the local cache: %s, bad batches: %s",
                Arrays.toString(batchesByPds.toArray()), Arrays.toString(badBatches.toArray())));
            return badBatches;
        }

        //The condition keeps untact without changing the existing logic.
        //though not sure why we did bot check it in the begining of the method.
        //if the pds id doe not exist in pds, should we always return an empty bad batch?
        if (pdsLogic.getPatientDataSetByID(pdsId) != null) {
            // check that its a real pds
            return badBatches;
        }
        return null;
    }

    private List<UploadBatch> getBadUploadBatches(final List<UploadBatch> batchesByPds, final long start, final long end) {
        Date startDate = start == 0L ? null : new Date(start);
        Date endDate = end == 0L ? null : new Date(end);

        List<UploadBatch> badBatches = new ArrayList<>();
        for (UploadBatch b : batchesByPds) {
            // If the batch is bad, and it is within our start/end date add it to valid bad batches.
            if (b.getBad() &&
                ((startDate == null || startDate.before(b.getStartDate())) &
                    (endDate == null || endDate.after(b.getCloseDate())))) {
                badBatches.add(b);
            }
        }
        return badBatches;
    }

    public List<UploadBatch> getUploadBatches(XUUID pdsId, boolean filterBad) {
        List<UploadBatch> goodBatches = new ArrayList<>();
        List<UploadBatch> batchesByPds;
        if (uploadBatchCacheByPds != null) {
            batchesByPds = uploadBatchCacheByPds.getIfPresent(pdsId.getID());
            if (batchesByPds != null) {
                if (!filterBad) {
                    log.debug(String.format("Get all the unfiltered upload batches per pds from the local cache: %s",
                        Arrays.toString(batchesByPds.toArray())));
                    return batchesByPds;
                }
                goodBatches = getGoodUploadBatches(batchesByPds);
                log.debug(String.format("Get all the upload batches per pds from the local cache: %s, good batches: %s",
                    Arrays.toString(batchesByPds.toArray()), Arrays.toString(goodBatches.toArray())));
                return goodBatches;
            }
        }
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(UPLOAD_BATCHES);
        if (allBatches.containsKey(pdsId)) {
            batchesByPds = UploadBatch.fromJSON((String) allBatches.get(pdsId));
            if (uploadBatchCacheByPds != null) {
                uploadBatchCacheByPds.put(pdsId.getID(), batchesByPds);
            }
            if (!filterBad) {
                log.debug(String.format("Put all the upload batches per pds into the local cache: %s and return them.",
                    Arrays.toString(batchesByPds.toArray())));
                return batchesByPds;
            }
            goodBatches = getGoodUploadBatches(batchesByPds);
            log.debug(String.format("Put all the upload batches per pds into the local cache: %s, good batches: %s",
                Arrays.toString(batchesByPds.toArray()), Arrays.toString(goodBatches.toArray())));
            return goodBatches;
        }

        if (pdsLogic.getPatientDataSetByID(pdsId) != null) {
            // check that its a real pds
            return goodBatches;
        }
        return null;
    }

    private List<UploadBatch> getGoodUploadBatches(final List<UploadBatch> batchesByPds) {
        List<UploadBatch> goodBatches = new ArrayList<>();
        for (UploadBatch b : batchesByPds) {
            if (!b.getBad()) {
                goodBatches.add(b);
            }
        }
        return goodBatches;
    }

    public List<UploadBatch> getUploadBatches(XUUID pdsId) {
        return getUploadBatches(pdsId, true);
    }

    public EventExtractBatch getExtractBatch(String name, XUUID pdsId) {
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(EXTRACT_BATCHES);
        if (allBatches.containsKey(pdsId)) {
            List<EventExtractBatch> pdsBatches = EventExtractBatch.fromJSON((String) allBatches.get(pdsId));
            for (EventExtractBatch b : pdsBatches)
                if (b.getName().equals(name))
                    return b;
        }
        return null;
    }
    public EventExtractBatch getExtractBatch(String name, long coid) {
        return getExtractBatch(name, PatientDataSetLogic.patientDataSetIDFromLong(coid));
    }

    public EventExtractBatch getExtractBatch(XUUID batchId, XUUID pdsId) {
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(EXTRACT_BATCHES);
        if (allBatches.containsKey(pdsId)) {
            List<EventExtractBatch> pdsBatches = EventExtractBatch.fromJSON((String) allBatches.get(pdsId));
            for (EventExtractBatch b : pdsBatches)
                if (b.getId().equals(batchId.toString()))
                    return b;
        }
        return null;
    }

    public EventExtractBatch getExtractBatch(XUUID batchId) {
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(EXTRACT_BATCHES);
        for (XUUID pdsId : allBatches.keySet()) {
            List<EventExtractBatch> pdsBatches = EventExtractBatch.fromJSON((String) allBatches.get(pdsId));
            for (EventExtractBatch b : pdsBatches)
                if (b.getId().equals(batchId.toString()))
                    return b;
        }
        return null;
    }

    public List<EventExtractBatch> getExtractBatches(XUUID pdsId, boolean filterBad) {
        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(EXTRACT_BATCHES);
        if (allBatches.containsKey(pdsId)) {
            if (filterBad) {
                List<EventExtractBatch> goodBatchesOnly = new ArrayList<>();
                for (EventExtractBatch b : EventExtractBatch.fromJSON((String) allBatches.get(pdsId)))
                    if (!b.getBad())
                        goodBatchesOnly.add(b);
                return goodBatchesOnly;
            }
            return EventExtractBatch.fromJSON((String) allBatches.get(pdsId));
        } else {
            if (pdsLogic.getPatientDataSetByID(pdsId) != null) // check that its a real pds
                return new ArrayList<>();
        }
        return null;
    }
    public List<EventExtractBatch> getExtractBatches(XUUID pdsId) {
        return getExtractBatches(pdsId, true);
    }

    ////////////
    // update //
    ////////////

    public void updateBatch(XUUID batchId, XUUID pdsId, UploadBatch batch) {
        PatientDataSet pds = pdsLogic.getPatientDataSetByID(pdsId);
        String batchesJson = (String) pdsLogic.getPatientDataSetProperties(pds).get(UPLOAD_BATCHES);

        if (batchesJson == null || batchesJson.isEmpty())
            throw new BatchException(FailureType.NOT_EXISTING, "Cannot update a batch that does not exist.");

        List<UploadBatch> existingPdsBatches = UploadBatch.fromJSON(batchesJson);
        List<UploadBatch> updatedPdsBatches = new ArrayList<>();
        UploadBatch existingBatch = null;
        for (UploadBatch b : existingPdsBatches) {
            if (b.getId().equals(batchId.toString()))
                existingBatch = b;
            else
                updatedPdsBatches.add(b);
        }

        if (existingBatch == null)
            throw new BatchException(FailureType.NOT_EXISTING, "Cannot update a batch that does not exist.");

        // copy populated fields that are mutable
        if (batch.getDesc() != null)
            existingBatch.setDesc(batch.getDesc());
        if (batch.getCloseDate() != null)
            existingBatch.setCloseDate(batch.getCloseDate());
        if (batch.getBad() != existingBatch.getBad())
            existingBatch.setBad(batch.getBad());
        if (batch.getPipelineVersions() != null && batch.getPipelineVersions().size() > 0)
            for (String version : batch.getPipelineVersions())
                existingBatch.addPipelineVersion(version);
        if (batch.getCompletenessRatio() > existingBatch.getCompletenessRatio()) // TODO: is it good to assume this value should never decrease?
            existingBatch.setCompletenessRatio(batch.getCompletenessRatio());
        if (batch.getProperties() != null)
            for (String key : batch.getProperties().keySet())
                existingBatch.setProperty(key, batch.getProperty(key));

        updatedPdsBatches.add(existingBatch);
        pdsLogic.setPatientDataSetProperty(pds, UPLOAD_BATCHES, UploadBatch.toJSON(updatedPdsBatches));
    }

    public void updateBatch(UploadBatch batch) {
        updateBatch(XUUID.fromString(batch.getId()), XUUID.fromString(batch.getPds()), batch);
    }

    // update batch metadata. make sure its a field that is allowed to change (not name or start date)
    public void updateBatch(XUUID batchId, XUUID pdsId, EventExtractBatch batch) {
        PatientDataSet pds = pdsLogic.getPatientDataSetByID(pdsId);
        String batchesJson = (String) pdsLogic.getPatientDataSetProperties(pds).get(EXTRACT_BATCHES);

        if (batchesJson == null || batchesJson.isEmpty())
            throw new BatchException(FailureType.NOT_EXISTING, "Cannot update a batch that does not exist.");

        List<EventExtractBatch> existingPdsBatches = EventExtractBatch.fromJSON(batchesJson);
        List<EventExtractBatch> updatedPdsBatches = new ArrayList<>();
        EventExtractBatch existingBatch = null;
        for (EventExtractBatch b : existingPdsBatches) {
            if (b.getId().equals(batchId.toString()))
                existingBatch = b;
            else
                updatedPdsBatches.add(b);
        }

        if (existingBatch == null)
            throw new BatchException(FailureType.NOT_EXISTING, "Cannot update a batch that does not exist.");

        // copy populated fields that are mutable
        if (batch.getDesc() != null)
            existingBatch.setDesc(batch.getDesc());
        if (batch.getCloseDate() != null)
            existingBatch.setCloseDate(batch.getCloseDate());
        if (batch.getBad() != existingBatch.getBad())
            existingBatch.setBad(batch.getBad());
        if (batch.getCompletenessRatio() > existingBatch.getCompletenessRatio()) // TODO: is it good to assume this value should never decrease?
            existingBatch.setCompletenessRatio(batch.getCompletenessRatio());
        if (batch.getProperties() != null)
            for (String key : batch.getProperties().keySet())
                existingBatch.setProperty(key, batch.getProperty(key));

        updatedPdsBatches.add(existingBatch);
        pdsLogic.setPatientDataSetProperty(pds, EXTRACT_BATCHES, EventExtractBatch.toJSON(updatedPdsBatches));
    }
    public void updateBatch(EventExtractBatch batch) {
        updateBatch(XUUID.fromString(batch.getId()), XUUID.fromString(batch.getPds()), batch);
    }

    ////////////
    // delete (for test. we probably don't want to support batch deletion)
    ////////////

    public void deleteUploadBatch(XUUID batchId, XUUID pdsId) {
        PatientDataSet pds = pdsLogic.getPatientDataSetByID(pdsId);

        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(UPLOAD_BATCHES);
        List<UploadBatch> pdsBatches = new ArrayList<>();
        boolean found = false;
        if (allBatches.containsKey(pdsId)) {
            List<UploadBatch> existingPdsBatches = UploadBatch.fromJSON((String) allBatches.get(pdsId));
            for (UploadBatch b : existingPdsBatches) {
                if (b.getId().equals(batchId.toString()))
                    found = true;// make sure batch exists
                else
                    pdsBatches.add(b);
            }
        } // else found stays false

        if (found) {
            String batchesJson = UploadBatch.toJSON(pdsBatches);
            pdsLogic.setPatientDataSetProperty(pds, UPLOAD_BATCHES, batchesJson);
        } else {
            throw new BatchException(FailureType.NOT_EXISTING, "Cannot delete a batch that does not exist.");
        }
    }
    public void deleteExtractBatch(XUUID batchId, XUUID pdsId) {
        PatientDataSet pds = pdsLogic.getPatientDataSetByID(pdsId);

        Map<XUUID, Object> allBatches = pdsLogic.getPatientDataSetsProperty(EXTRACT_BATCHES);
        List<EventExtractBatch> pdsBatches = new ArrayList<>();
        boolean found = false;
        if (allBatches.containsKey(pdsId)) {
            List<EventExtractBatch> existingPdsBatches = EventExtractBatch.fromJSON((String) allBatches.get(pdsId));
            for (EventExtractBatch b : existingPdsBatches) {
                if (b.getId().equals(batchId.toString()))
                    found = true;// make sure batch exists
                else
                    pdsBatches.add(b);
            }
        } // else found stays false

        if (found) {
            String batchesJson = EventExtractBatch.toJSON(pdsBatches);
            pdsLogic.setPatientDataSetProperty(pds, EXTRACT_BATCHES, batchesJson);
        } else {
            throw new BatchException(FailureType.NOT_EXISTING, "Cannot delete a batch that does not exist.");
        }
    }

    // TODO: create a NamedEntity class. probably an abstract Batch and then two types: UploadBatch and ExtractBatch
    public static abstract class Batch {
        //public static final String OBJTYPE_PREFIX = "B"; // should never need an entity thats just a 'Batch'
        // what about summary generation batches? should we track those?

        // XUUIDs saved as strings for easy JSON conversion
        private String id; // immutable
        private String name; // immutable
        private String desc;
        private String pds; // immutable
        private Date startDate; // immutable
        private Date closeDate; // TODO... maybe I should call this latestActivityDate or something
        //private List<String> targetProjects; // does it make more sense to have it this way, or should projects have target batches? or project could target data uploaded during a certain period of time from a certain source system...
        private boolean bad = false;
        private double completenessRatio = 0;
        private Map<String, Object> properties;

        public void setId(String id) {
            if (this.id != null)
                throw new BatchException(FailureType.ALREADY_STARTED, "Cannot change a batch id after batch has started.");
            XUUID.fromString(id);
            this.id = id;
        }
        public String getId() {
            return id;
        }
        public void setName(String name) {
            if (this.name != null)
                throw new BatchException(FailureType.ALREADY_STARTED, "Cannot change the batch name after batch has started.");
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public void setDesc(String desc) {
            this.desc = desc;
        }
        public String getDesc() {
            return desc;
        }
        public void setPds(String pds) {
            if (this.pds != null)
                throw new BatchException(FailureType.ALREADY_STARTED, "Cannot change the owning PDS after batch has started.");
            XUUID.fromString(pds, PatientDataSet.OBJTYPE);
            this.pds = pds;
        }
        public String getPds() {
            return pds;
        }
        public void setStartDate(Date startDate) {
            if (this.startDate != null)
                throw new BatchException(FailureType.ALREADY_STARTED, "Cannot change the start date after batch has started.");
            this.startDate = startDate;
        }
        public Date getStartDate() {
            return startDate;
        }
        public void setCloseDate(Date closeDate) {
            this.closeDate = closeDate;
        }
        public Date getCloseDate() {
            return closeDate;
        }
        public void setBad(boolean bad) {
            this.bad = bad;
        }
        public boolean getBad() {
            return bad;
        }
        //        public boolean isBad() { return bad; } // json serialization complains
        public void setCompletenessRatio(double completenessRatio) {
            this.completenessRatio = completenessRatio;
        }
        public double getCompletenessRatio() {
            // TODO: may want to auto update on get if ratio is < 1
            return completenessRatio;
        }
        //abstract double updateCompletenessRatio(); // I don't think this can exist here because this is lower level than DAO which will be required for completeness calculations...

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
        public void setProperty(String name, Object value) {
            if (properties == null)
                properties = new HashMap<>();
            properties.put(name, value);
        }
        public Map<String, Object> getProperties() {
            return properties;
        }
        public Object getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public String toString() {
            return "Batch{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", pds='" + pds + '\'' +
                ", startDate=" + startDate +
                ", closeDate=" + closeDate +
                ", bad=" + bad +
                ", completenessRatio=" + completenessRatio +
                ", properties=" + properties +
                '}';
        }
    }

    public static class UploadBatch extends Batch {
        public static final String OBJTYPE_PREFIX = "UPB";

        private Set<String> pipelineVersions;

        public void setPipelineVersions(Set<String> pipelineVersions) {
            this.pipelineVersions = pipelineVersions;
        }
        public Set<String> getPipelineVersions() {
            return pipelineVersions;
        }
        public void addPipelineVersion(String pipelineVersion) {
            if (pipelineVersions == null)
                pipelineVersions = new HashSet<>();
            pipelineVersions.add(pipelineVersion);
        }

        private static ObjectMapper jsonMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        private static List<UploadBatch> fromJSON(String batchListJson) {
            try {
                return (List<UploadBatch>) jsonMapper.readValue(batchListJson, new TypeReference<List<UploadBatch>>(){});
            } catch (IOException e) {
                e.printStackTrace();
                log.debug("BatchLogic DEBUG: problem parsing json: \n" + batchListJson);
            }
            return new ArrayList<>(); // This is very dangerous! could erase batches by accident.
        }

        private static String toJSON(List<UploadBatch> batchList) {
            try {
                return jsonMapper.writeValueAsString(batchList);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                log.debug("BatchLogic DEBUG: problem converting to json.");
            }

            return "[]"; // This is very dangerous! could erase batches by accident.
        }

//        @Override
//        double updateCompletenessRatio() {
//            return 0; // TODO
//        }
    }

    public static class EventExtractBatch extends Batch {
        public static final String OBJTYPE_PREFIX = "EXB";

        // event extract batch name will be generated from upload batch name, model version,
        // source system, and health plan information ... and maybe pipeline version
        private String parentUploadBatch; // pipeline batch name or upload batch name. may or may not have an entity associated, but should be indexed
        private String sourceSystem;
        private String modelVersion;
        private String pipelineVersion;

        public void setParentUploadBatch(String parentUploadBatch) {
            this.parentUploadBatch = parentUploadBatch;
        }
        public String getParentUploadBatch() {
            return parentUploadBatch;
        }
        public void setSourceSystem(String sourceSystem) {
            this.sourceSystem = sourceSystem;
        }
        public String getSourceSystem() {
            return sourceSystem;
        }
        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }
        public String getModelVersion() {
            return modelVersion;
        }
        public void setPipelineVersion(String pipelineVersion) {
            this.pipelineVersion = pipelineVersion;
        }
        public String getPipelineVersion() {
            return pipelineVersion;
        }

        private static ObjectMapper jsonMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        private static List<EventExtractBatch> fromJSON(String batchListJson) {
            try {
                return (List<EventExtractBatch>) jsonMapper.readValue(batchListJson, new TypeReference<List<EventExtractBatch>>(){});
            } catch (IOException e) {
                e.printStackTrace();
                log.debug("BatchLogic DEBUG: problem parsing json: \n" + batchListJson);
            }
            return new ArrayList<>(); // This is very dangerous! could erase batches by accident.
        }

        private static String toJSON(List<EventExtractBatch> batchList) {
            try {
                return jsonMapper.writeValueAsString(batchList);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                log.debug("BatchLogic DEBUG: problem converting to json.");
            }

            return "[]"; // This is very dangerous! could erase batches by accident.
        }

//        @Override
//        double updateCompletenessRatio() {
//            return 0; // TODO
//        }
    }

    // test json conversions
//    public static void main(String[] args){
//
//        ObjectMapper jsonMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
//
//        UploadBatch test = new UploadBatch();
//        test.setId(UploadBatch.OBJTYPE_PREFIX + "_" + UUID.randomUUID().toString());
//        test.setPds("O_" + UUID.randomUUID().toString());
//        test.setName("batch_name_1");
//        test.setStartDate(new Date());
//        test.setProperty("metadatathing", "metadatavalue");
//
//        try {
//            String testJson = jsonMapper.writeValueAsString(test);
//            log.debug(testJson);
//            log.debug(jsonMapper.readValue(testJson, UploadBatch.class).getName());
//
//
//            List<UploadBatch> testList = new ArrayList<>();
//            testList.add(test);
//            String testListJson = UploadBatch.toJSON(testList);
//            log.debug(testListJson);
//            log.debug((UploadBatch.fromJSON(testListJson)).get(0).getName());
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
