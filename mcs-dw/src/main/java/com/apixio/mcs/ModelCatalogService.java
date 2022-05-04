package com.apixio.mcs;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;

import com.apixio.XUUID;
import com.apixio.bms.Blob;
import com.apixio.bms.BlobManager;
import com.apixio.bms.Metadata;
import com.apixio.bms.PartSource;
import com.apixio.bms.Query;
import com.apixio.bms.Relationship;
import com.apixio.bms.Schema;
import com.apixio.dao.utility.DaoServices;
import com.apixio.mcs.util.ConversionUtil;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.util.LruCache;

/**
 * ModelCatalogService is the interface from MCS code to Blob Management Service
 * code.  It contains state-based business logic.
 */
public class ModelCatalogService
{
    /**
     * Type prefix for XUUIDs
     */
    public static final String TYPE_PREFIX = "B";

    /**
     * For selecting/returning only the latest model from a query.  This "latest" concept
     * is orthogonal to "order by".
     */
    public enum Latest { BY_VERSION, BY_CREATION_DATE, BY_LAST_UPLOAD_DATE }

    /**
     * This enum declares which portion the JSON data is for.  One use of it
     * is as the last part of the URL path that is specific to updating a portion of
     * the JSON metadata, and the enum value names MUST be what's desired in the
     * URL (case-insensitive).
     */
    public enum JsonScope { CORE, SEARCH }

    /**
     * This an implementation-only enum so we can quickly check if an operation
     * can be done in a particular Lifecycle state, expressed in a data-driven
     * way.  There is pretty much one enum value for each method in this class
     * that modifies the model in one way or another.
     */
    public enum Operation
    {
        UPDATE_MCSMETA, UPDATE_COREJSON, UPDATE_SEARCHJSON, UPLOAD_DATA,
        MODIFY_DEPENDENCY, MODIFY_OWNER,
        DELETE_MC
    }

    /**
     * Used only for downloading part data
     */
    public static class DownloadInfo
    {
        public InputStream data;
        public String      mimeType;

        public DownloadInfo(InputStream data, String mimeType)
        {
            this.data     = data;
            this.mimeType = mimeType;
        }
    }

    /**
     * Thrown when a lifecycle state-dependent operation fails
     */
    public static class LifecycleException extends RuntimeException
    {
        /**
         * One of the following will be non-null and will indicate either the
         * attempted operation or transition that failed state constraints
         */
        private Operation operation;
        private Lifecycle from;
        private Lifecycle to;

        /**
         *
         */
        public LifecycleException(Operation operation)
        {
            super("");
            this.operation = operation;
        }
        public LifecycleException(Lifecycle from, Lifecycle to)
        {
            super("");
            this.from = from;
            this.to   = to;
        }

        /**
         * Getters
         */
        public Operation getOperation()
        {
            return operation;
        }
        public Lifecycle getTransitionFrom()
        {
            return from;
        }
        public Lifecycle getTransitionTo()
        {
            return to;
        }
    }

    /**
     * Schema is just a bunch of metadata definitions.
     */
    private static Schema schema = new Schema(
        McsMetadataDef.MD_EXECUTOR,
        McsMetadataDef.MD_NAME,
        McsMetadataDef.MD_OUTPUT_TYPE,
        McsMetadataDef.MD_PDSID,
        McsMetadataDef.MD_PRODUCT,
        McsMetadataDef.MD_STATE,
        McsMetadataDef.MD_VERSION,
        McsMetadataDef.MD_STUFF
        );

    /**
     * Allowed Lifecycle state transitions
     */
    private static Map<Lifecycle, List<Lifecycle>> allowedTransitions = new HashMap<>();
    static
    {
        allowedTransitions.put(Lifecycle.DRAFT,      Arrays.asList(Lifecycle.DISCARDED, Lifecycle.EVALUATED, Lifecycle.ACCEPTED));
        allowedTransitions.put(Lifecycle.EVALUATED,  Arrays.asList(Lifecycle.DISCARDED, Lifecycle.ACCEPTED));
        allowedTransitions.put(Lifecycle.ACCEPTED,   Arrays.asList(Lifecycle.DISCARDED, Lifecycle.RELEASED));
        allowedTransitions.put(Lifecycle.RELEASED,   Arrays.asList(Lifecycle.DISCARDED, Lifecycle.ARCHIVED));
        allowedTransitions.put(Lifecycle.DISCARDED,  Arrays.asList());
        allowedTransitions.put(Lifecycle.ARCHIVED,   Arrays.asList());
    }

    /**
     * Allowed operations in each state.  If there isn't a Hash/List combo for the given
     * <state, operation> then the operation is disallowed.
     */
    private static Map<Lifecycle, List<Operation>> allowedOperations = new HashMap<>();
    static
    {
        allowedOperations.put(Lifecycle.DRAFT,
                              Arrays.asList(Operation.UPDATE_MCSMETA, Operation.UPDATE_COREJSON, Operation.UPDATE_SEARCHJSON, Operation.UPLOAD_DATA,
                                            Operation.MODIFY_DEPENDENCY, Operation.MODIFY_OWNER, Operation.DELETE_MC));

        allowedOperations.put(Lifecycle.EVALUATED,  Arrays.asList(Operation.UPDATE_SEARCHJSON));
        allowedOperations.put(Lifecycle.ACCEPTED,   Arrays.asList(Operation.UPDATE_SEARCHJSON));
        allowedOperations.put(Lifecycle.RELEASED,   Arrays.asList(Operation.UPDATE_SEARCHJSON));
        allowedOperations.put(Lifecycle.DISCARDED,  Arrays.asList());
        allowedOperations.put(Lifecycle.ARCHIVED,   Arrays.asList());
    }

    /**
     * Does all the hard work
     */
    private BlobManager blobManager;

    /**
     * We can cache logID->list<meta> for RELEASED (locked) only
     */
    private LruCache<String, List<LogicalMeta>> mergedDepsCache = new LruCache(100, 1000 * 60 * 10);  // 10 minutes ttl

    /**
     * We can cache meta for a long period of time; since models are not mutable once they are not in DRAFT State. This cache
     * will cache metadata for NON-DRAFT State MCIDs.
     */
    private LruCache<XUUID, Blob> metaMap     = new LruCache(100, 1000 * 60 * 60 * 24);  // 24 hour ttl
    private Map<XUUID, Object>    metaLockMap = Collections.synchronizedMap(new HashMap<>());

    /**
     *
     */
    public ModelCatalogService(DaoServices daos, ConfigSet config)
    {
        String loggingConfig = config.getString("log4jConfiguration", null);

        blobManager = new BlobManager(daos, config, schema);

        if (loggingConfig != null)
        {
            System.out.println("Setting log4j configuration from file " + loggingConfig);
            PropertyConfigurator.configure(loggingConfig);
        }
    }

    /**
     * "Uploads" a model file by creating a blob with the given metadata
     */
    public Blob createModelMeta(ModelMeta meta)
    {
        Metadata   md     = formMetadata(meta, true);
        JSONObject core   = (meta.core   != null) ? new JSONObject(meta.core)   : null;
        JSONObject search = (meta.search != null) ? new JSONObject(meta.search) : null;

        md.add(McsMetadataDef.MD_STATE, Lifecycle.DRAFT.toString());

        return blobManager.createBlob(TYPE_PREFIX, meta.createdBy, md, core, search);
    }

    /**
     * deletes a blob
     */
    public void deleteModel(Blob blob)
    {
        if (!isAllowedStateOperation(getLifecycleState(blob), Operation.DELETE_MC))
            throw new LifecycleException(Operation.DELETE_MC);

        blobManager.deleteBlob(blob);
    }

    /**
     * Loads and returns the given blob by ID
     */
    public Blob getModelMeta(XUUID id)
    {
        //!! results of blobManager.getBlobMeta can be cached if lifecycle state != DRAFT
        Blob blob = metaMap.get(id);

        if (blob == null)
        {
            final Object lock = metaLockMap.computeIfAbsent(id, k -> new Object());

            // To prevent stampeding herd problem; make sure we lock on the XUUID so that a potentially
            // expensive blob meta call is called only once.
            synchronized (lock)
            {
                blob = metaMap.get(id);

                if (blob == null)
                {
                    blob = blobManager.getBlobMeta(id);

                    if (blob != null && getLifecycleState(blob) != Lifecycle.DRAFT)
                        metaMap.put(id, blob);
                }
            }
        }

        return blob;
    }

    public List<Blob> getModelParts(Blob parent)
    {
        return blobManager.getParts(parent);
    }

    /**
     * Note that this is not parallel to updatePartMeta in that we use JsonScope
     * here but not up updatePartMeta...
     */
    public JSONObject getPartMeta(XUUID parentID, String partName, JsonScope which)
    {
        Blob blob = blobManager.getBlobMeta(parentID);

        if ((blob != null) && ((blob = blobManager.getPart(blob, partName)) != null))
        {
            if (which == JsonScope.CORE)
                return blob.getExtra1();
            else if (which == JsonScope.SEARCH)
                return blob.getExtra2();
            else
                return new JSONObject();
        }

        return null;
    }

    /**
     * Updates model file metadata.  An exception is thrown if lifecycle state disallows
     * the operation
     */
    public void updateModelMeta(Blob blob, ModelMeta meta, boolean replace) throws LifecycleException
    {
        Lifecycle current = getLifecycleState(blob);
        Metadata  md      = formMetadata(meta, false);

        if ((meta.core != null) && !isAllowedStateOperation(current, Operation.UPDATE_COREJSON))
            throw new LifecycleException(Operation.UPDATE_COREJSON);
        else if ((meta.search != null) && !isAllowedStateOperation(current, Operation.UPDATE_SEARCHJSON))
            throw new LifecycleException(Operation.UPDATE_SEARCHJSON);
        else if (!md.isEmpty() && !isAllowedStateOperation(current, Operation.UPDATE_MCSMETA))
            throw new LifecycleException(Operation.UPDATE_MCSMETA);

        JSONObject core   = (meta.core   != null) ? new JSONObject(meta.core)   : null;
        JSONObject search = (meta.search != null) ? new JSONObject(meta.search) : null;

        md.add(McsMetadataDef.MD_STATE, blob.getMetadata().getString(McsMetadataDef.MD_STATE));  // special case--can't update state this way so keep existing

        if (meta.deleted == Boolean.TRUE)  // allows deleted to be null
            blob.setSoftDeleted(true);

        blobManager.updateBlobMeta(blob, replace, md, false, core, search);
    }

    /**
     *
     */
    public void updateLifecycleState(Blob blob, String newState) throws LifecycleException
    {
        Lifecycle current = getLifecycleState(blob);
        Lifecycle newLfs  = validateLifecycleState(newState, false);
        Metadata  meta    = Metadata.create();

        meta.add(McsMetadataDef.MD_STATE, newLfs.toString());

        if (!isAllowedStateTransition(current, newLfs))
            throw new LifecycleException(current, newLfs);

        blobManager.updateBlobMeta(blob, false, meta,
                                   (newLfs == Lifecycle.RELEASED),  // if we're changing to released then lock all owned logical IDs
                                   null, null);
    }

    public void uploadPartData(Blob blob, String uploadedBy, String partName, String mimeType, InputStream data) throws IOException, LifecycleException
    {
        if (!isAllowedStateOperation(getLifecycleState(blob), Operation.UPLOAD_DATA))
            throw new LifecycleException(Operation.UPLOAD_DATA);

        // either uploadPart() works or an exception is thrown
        blobManager.uploadPart(blob, partName, uploadedBy, mimeType, data);
    }

    public boolean validatePartData(PartSource partInfo)
    {
        if (partInfo.source.startsWith(PartSource.SOURCE_MD5))
        {
            String hash = partInfo.source.substring(PartSource.SOURCE_MD5.length());

            if (blobManager.getBlobsWithMd5Hash(hash).size() == 0)
                return false;
        }

        return true;
    }

    public boolean referencePartData(Blob blob, String uploadedBy, PartSource partInfo) throws IOException, LifecycleException
    {
        if (!isAllowedStateOperation(getLifecycleState(blob), Operation.UPLOAD_DATA))  // uploaddata is good enough...
            throw new LifecycleException(Operation.UPLOAD_DATA);

        // true is returned if matching MD5 blob found
        return (blobManager.uploadPart(blob, partInfo, uploadedBy) != null);
    }

    /**
     * blob param is parent blob
     */
    public DownloadInfo downloadPartData(Blob blob, String partName) throws IOException
    {
        if ((blob = blobManager.getPart(blob, partName)) != null)
            return new DownloadInfo(blobManager.getPartData(blob),
                                    (blob.getMimeType() != null) ? blob.getMimeType() : "application/octet-stream");
        else
            return null;
    }

    /**
     * Updates part-level metadata.  Only one of core and search will be non-null.
     * False is returned iff part w/ name doesn't exist
     */
    public boolean updatePartMeta(Blob blob, String partName, Map<String,Object> coreMeta, Map<String,Object> searchMeta) throws LifecycleException
    {
        Lifecycle current = getLifecycleState(blob);

        if ((coreMeta != null) && !isAllowedStateOperation(current, Operation.UPDATE_COREJSON))
            throw new LifecycleException(Operation.UPDATE_COREJSON);
        else if ((searchMeta != null) && !isAllowedStateOperation(current, Operation.UPDATE_SEARCHJSON))
            throw new LifecycleException(Operation.UPDATE_SEARCHJSON);

        JSONObject core   = (coreMeta   != null) ? new JSONObject(coreMeta)   : null;
        JSONObject search = (searchMeta != null) ? new JSONObject(searchMeta) : null;

        return blobManager.updatePartMeta(blob, partName, core, search);
    }

    /**
     *
     */
    public boolean deletePart(Blob parent, String name)
    {
        if (!isAllowedStateOperation(getLifecycleState(parent), Operation.DELETE_MC))
            throw new LifecycleException(Operation.DELETE_MC);

        return blobManager.deletePart(parent, name);
    }

    public boolean partWithMd5HashExists(String md5)
    {
        return blobManager.getBlobsWithMd5Hash(md5).size() > 0;
    }

    /**
     * Searches blobs by the given Query details.
     */
    public List<ModelMeta> searchBlobs(Query q)
    {
        List<Blob>  blobs = blobManager.queryBlobMeta(q);

        return blobs.stream().map(b -> ConversionUtil.blobToModelMeta(b)).collect(Collectors.toList());
    }

    /**
     * Searches parts; currently the only supported (needed?) search is by md5 hash value.
     * This hash value should be created via com.apixio.mcs.xyz
     */
    public List<PartMeta> searchParts(String md5)
    {
        Map<Blob, Blob>  blobs = blobManager.getPartsParentsByMd5(md5);
        List<PartMeta>   parts = new ArrayList<>();

        blobs.forEach((k,v) -> parts.add(ConversionUtil.blobToPartMeta(v.getUuid().toString(), k)));

        return parts;
    }

    /**
     * Search blobs but limit return to just the "latest" one.
     */
    public ModelMeta searchBlobs(Latest latest, Query q)
    {
        List<Blob>  blobs = blobManager.queryBlobMeta(q);

        return selectLatest(latest, blobs);
    }

    /**
     * Returns true if the current lifecycle state can be transitioned to the given one.
     */
    public boolean isAllowedStateTransition(Lifecycle current, Lifecycle desired)
    {
        return allowedTransitions.get(current).contains(desired);
    }

    /**
     * Returns true if the current lifecycle state allows the given operation
     */
    public boolean isAllowedStateOperation(Lifecycle current, Operation op)
    {
        return allowedOperations.get(current).contains(op);
    }

    /**
     * Dependency management
     */
    public void setOwner(Blob blob, List<String> logicalIDs)
    {
        if (!isAllowedStateOperation(getLifecycleState(blob), Operation.MODIFY_OWNER))
            throw new LifecycleException(Operation.MODIFY_OWNER);

        // owner can't be set on locked logicalIDs so check all IDs
        for (String id : logicalIDs)
        {
            if (blobManager.isLogicalIdLocked(id))
                throw new LifecycleException(Operation.MODIFY_OWNER);  //!! i don't like this as there's no info about why it's failing (e.g., what id is locked)
        }

        blobManager.setOwner(blob, logicalIDs);
    }

    public ModelMeta getOwner(String logicalID)
    {
        Blob owner = blobManager.getOwner(logicalID);

        if (owner != null)
            return ConversionUtil.blobToModelMeta(owner);
        else
            return null;
    }

    public List<String> getOwned(Blob blob)
    {
        //!! results of blobManager.getOwned can be cached if lifecycle state != DRAFT

        return blobManager.getOwned(blob);
    }

    public void addDependencies(String leftID, List<String> rightIDs, boolean replace)
    {
        if (blobManager.isLogicalIdLocked(leftID))
            throw new LifecycleException(Operation.MODIFY_DEPENDENCY);

        blobManager.addDependencies(leftID, rightIDs, replace);
    }

    public List<String> getLogicalDependencies(String id, boolean uses)
    {
        return blobManager.getLogicalDependencies(id, (uses) ? Relationship.USES : Relationship.USED_BY);
    }

    public List<ModelMeta> getPhysicalDependencies(String id, boolean uses)
    {
        return blobManager.getPhysicalDependencies(id,
                                                   (uses) ? Relationship.USES : Relationship.USED_BY).stream().map(
            b -> ConversionUtil.blobToModelMeta(b)).collect(Collectors.toList());
    }

    /**
     * This is a convenience method where the client needs to know both the logical id and the
     * mc_id of the dependencies of a model combination. Rather than do two queries on the client,
     * one for logical and one for physical, we do both queries in the api and merge the results.
     */
    public List<LogicalMeta> getMergedDependencies(String id, boolean uses, boolean allowCache)
    {
        final Relationship rel  = (uses) ? Relationship.USES : Relationship.USED_BY;
        final String       key  = id + ":" + rel.toString();
        List<LogicalMeta>  deps = (allowCache) ? mergedDepsCache.get(key) : null;

        if (deps == null)
        {
            deps = blobManager.getLogicalDependencies(id, rel).stream().map(
                logId -> {
                    ModelMeta meta = getOwner(logId);
                    if (meta != null)
                    {
                        LogicalMeta logicalMeta = new LogicalMeta();

                        logicalMeta.logicalId = logId;
                        logicalMeta.meta      = meta;

                        return logicalMeta;
                    }
                    else
                    {
                        return null;
                    }
                }).filter(logicalMeta -> logicalMeta != null)
            .collect(Collectors.toList());

            if (allowCache)  //!! ought to be: blobManager.isLogicalIdLocked(id))
                mergedDepsCache.put(key, deps);
        }

        return deps;
    }

    /**
     * Support methods
     */
    private Lifecycle getLifecycleState(Blob blob)
    {
        return Lifecycle.valueOf(blob.getMetadata().getString(McsMetadataDef.MD_STATE));
    }

    private Metadata formMetadata(ModelMeta meta, boolean replace)
    {
        Metadata md = Metadata.create();

        if (replace || (meta.name != null))
            md.add(McsMetadataDef.MD_NAME, meta.name);

        if (replace || (meta.executor != null))
            md.add(McsMetadataDef.MD_EXECUTOR, meta.executor);

        if (replace || (meta.outputType != null))
            md.add(McsMetadataDef.MD_OUTPUT_TYPE, meta.outputType);

        if (replace || (meta.pdsId != null))
            md.add(McsMetadataDef.MD_PDSID, meta.pdsId);

        if (replace || (meta.version != null))
            md.add(McsMetadataDef.MD_VERSION, meta.version);

        if (replace || (meta.product != null))
            md.add(McsMetadataDef.MD_PRODUCT, meta.product);

        return md;
    }

    private Lifecycle validateLifecycleState(String state, boolean allowNull)
    {
        Lifecycle lcs = null;

        if (state == null)
        {
            if (allowNull)
                lcs = Lifecycle.DRAFT;
            else
                throw new IllegalArgumentException("Lifecycle state cannot be null");
        }
        else
        {
            lcs = Lifecycle.valueOf(state.toUpperCase());
        }

        return lcs;
    }

    private ModelMeta selectLatest(Latest selector, List<Blob> blobs)
    {
        Blob latest = null;

        for (Blob blob : blobs)
        {
            if (latest == null)
            {
                latest = blob;
            }
            else if (selector == Latest.BY_VERSION)
            {
                //!! TODO?
            }
            else if (selector == Latest.BY_CREATION_DATE)
            {
                if (blob.getCreatedAt().after(latest.getCreatedAt()))
                    latest = blob;
            }
            else if (selector == Latest.BY_LAST_UPLOAD_DATE)
            {
                //!! TODO?
            }
        }

        return ConversionUtil.blobToModelMeta(latest);
    }

}
