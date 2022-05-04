package com.apixio.dao.apxdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.XUUID;
import com.apixio.dao.apxdata.BlobStore.BlobData;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DataStore.InitContext;
import com.apixio.security.exception.ApixioSecurityException;
import com.apixio.util.BytesUtil;
import com.apixio.util.EncryptUtil;
import com.apixio.util.Pair;
import com.apixio.util.ZipUtil;

/**
 * The basic functionality of this class is to persist a list of serialized Google
 * protobuf Message objects organized for efficient access by a groupingID.  The
 * groupingID is client-defined and -managed and is meant for efficient read and delete
 * operations.  The groupingID is stored by this system.
 *
 * In order to support parallel writes/updates to the objects identified by the
 * groupingID, an optional partitionID can be supplied, with the semantics being that
 * two clients writing different objects to the same groupingID but using different
 * partitionID will not conflict in their write operations.
 *
 * Read and restore at the lowest level is only via groupingID and all objects
 * written with different partitionIDs are read as a group (if separate groups of
 * objects need to be restored separately, then a different groupingID must be used)
 *
 * The implementation provides efficient access and object restoration if the client
 * can supply the groupingID.  If the client can't do that, or needs to be able to
 * retrieve groupingIDs based on some client-defined data "dimension", then insertion
 * of data can include "query keys" that the DAO stores along with the objects &
 * limited metadata.  The query key concept allows an arbitrary, client-defined set of
 * name=value "query points."  While these are supplied by the client, there is a
 * convenience class to help maintain a naming convention on standard query points
 * (e.g., pdsID, docID, etc.).  For example, if projectID was supplied as a querykey,
 * then all groupingIDs could be retrieved for a given projectID.
 *
 * As implied by the above, there are two modes/use-models for clients of ApxDataDao:
 *
 *  1) runtime client has all data to be written for a given groupingID
 *
 *  2) runtime client has partial data to be written for a given groupingID
 *
 * The general operational model for 1) is as follows:
 *
 *  * the value of partitionID in putData should be null or ""
 *
 *  * there should be only 1 call to putData; subsequent calls to that method will do a
 *    possibly faulty replace operation of the data (due to possible shrinkage of the
 *    number of chunks needed to persist the list of objects)
 *
 *  * any "re-run" of the operation that's intended by the client to replace/update
 *    data should first delete existing data for the groupingID
 *
 *  * querykeys "just work" because of the "1-call" paradigm
 *
 * The general operational model for 2) is as follows:
 *
 *  * each concurrent call to putData must supply a unique partitionID
 *
 *  * for a given groupingID it's expected that multiple, possibly concurrent calls to
 *    putData will be made
 *
 *  * the objects supplied in these separate calls are conceptually concatenated into a
 *    single list for object reconstruction
 *
 *  * any "re-run" that's intended to by the client to replace/update data MUST first
 *    delete existing data
 *
 *  * query keys supplied by the individual calls to putData are accumulated for the
 *    given groupingID, where the last call to putData with a given querykey name
 *    "wins".  For example if the following two calls are made (pidgin sytax):
 *
 *     putData(Foo.class, "group1", "part1", {"proj":"123"}, false, theDataList1);
 *     putData(Foo.class, "group1", "part2", {"pds":"well"}, false, theDataList2);
 *
 *    then the final querykeys would include both "proj=123" and "pds=well".  This 
 *    difference in querykeys between the calls is somewhat odd semantically.
 *
 * A note on the use of generic typing in the code (June 2021): in general this code is
 * meant to deal with protobuf Message-extended classes and except for the "raw" read
 * capability, all input/output types are Message-extended objects.  That said, Java
 * generics are generally not typed this way in the code (or not typed as well as
 * possible) due to the need to be able to return byte[] objects instead of
 * deserialized protobuf objects (for "raw" read, the client must do the
 * deserialization).
 */
public class ApxDataDao
{
    private static final Logger    LOGGER = LoggerFactory.getLogger(ApxDataDao.class);

    private static final List<byte[]> emptyBytes  = Collections.emptyList();
    private static final List<String> emptyString = Collections.emptyList();

    private Map<Class<?>, DataType<? extends Message>> datatypes  = new HashMap<>();
    private Set<String>                                storageIDs = new HashSet<>();  // to prevent double-usage of DataType.getStorageIDs

    private BlobStore                  blobStore;
    private MetadataStore              metaStore;

    // blob name is "" for simple forms of putData/getData
    private final static String       EMPTY_KEY     = "";
    private final static List<String> STRLIST_EMPTY = Arrays.asList(new String[] { EMPTY_KEY });

    /**
     * Supports streaming of reconstructed signals and predictions from an iterator on byte arrays,
     * where those byte arrays are pulled from blob storage
     */
    private class ChunkIterator implements Iterator
    {
        // supplied by caller
        private DataType<? extends Message> datatype;
        private String                      blobID;
        private Iterator<String>            partitionIDs;
        private String                      listName;

        // used during iteration
        private Iterator<byte[]> source;
        private Iterator<byte[]> unpacked;
        private boolean          decryptit;
        private boolean          unzipit;

        ChunkIterator(DataType<? extends Message> datatype, String blobID, Iterator<String> partitionIDs, String listName)
        {
            this.datatype     = datatype;       //!! FIX:  look up datatype by tag byte
            this.blobID       = blobID;
            this.partitionIDs = partitionIDs;
            this.listName     = listName;
            this.source       = emptyBytes.iterator();  // just in cse startNextPartition is empty

            emptyUnpacked();
            startNextPartition();
        }

        ChunkIterator(DataType<? extends Message> datatype, Iterator<byte[]> chunks)
        {
            this.datatype     = datatype;
            this.partitionIDs = emptyString.iterator();
            this.source       = chunks;

            emptyUnpacked();
            checkTag();
        }

        private void emptyUnpacked()
        {
            List<byte[]> empty = Collections.emptyList();    // two steps due to javac not being smart enough

            this.unpacked = empty.iterator();
        }

        /**
         * Since each partition is written separately from the others, each one
         * will be the full format where first chunk is just tag byte, and the
         * rest are zipped byte arrays.
         */
        private boolean startNextPartition()
        {
            if (partitionIDs.hasNext())
            {
                source = blobStore.iterateBlobs(blobID, partitionIDs.next(), listName);
                checkTag();

                return true;
            }
            else
            {
                return false;
            }
        }

        private void checkTag()
        {
            if (source.hasNext())
            {
                byte[] tag = source.next();

                decryptit = ChunkUtil.encryptFlag(tag);
                unzipit   = ChunkUtil.zipFlag(tag);
            }
        }

        @Override
        public boolean hasNext()
        {
            if (!(unpacked.hasNext() || source.hasNext()))
            {
                // we still could have another partitionID to use

                return startNextPartition() && hasNext();
            }
            else
            {
                return true;
            }
        }

        @Override
        public Object next()
        {
            byte[]  item;

            if (!hasNext())
                throw new NoSuchElementException();

            // this assumes that the next from source unpacks into a non-empty iterator...
            if (!unpacked.hasNext())
            {
                item = source.next();

                if (decryptit)
                {
                    // 10/6/2020 big hack.  this try/catch is required because some data was
                    // persisted by ApxDataDao with the encrypted bit flag set but without actual
                    // encryption of the data.  this was due to a DataType flag any value of 0x3
                    // being passed in and the code in this package didn't clear the encrypted bit
                    // (it wasn't known that a 0x3 was being passed in at that time).  because of
                    // this mismatch in flag vs data, we ignore the security exception.  since
                    // Security code wraps some lower level exceptions, there's still a possibility
                    // that the flag is correctly set and that there's a real security error.
                    // in that case, the unzip should fail (and, worst case, the protobuf read
                    // will fail).
                    try
                    {
                        item = EncryptUtil.decryptBytes(item);
                    }
                    catch (ApixioSecurityException x)
                    {
                        // do nothing as we don't know if this data really was encrypted
                    }
                }

                if (unzipit)
                    item = ZipUtil.unzipBytesSafe(item);

                unpacked = BytesUtil.unpackArrays(item).iterator();
            }

            item = unpacked.next();

            // allows for returning raw byte[] from the iterator
            if (datatype != null)
            {
                try
                {
                    return datatype.deserialize(item);
                }
                catch (IOException x)
                {
                    throw new RuntimeException("Failed to move to next item in ChunkIterator", x);
                }
            }
            else
            {
                return item;
            }
        }
    }

    /**
     *
     */
    public ApxDataDao(DaoServices daos) throws IOException
    {
        InitContext ctx = new InitContext(daos);

        blobStore = new CassBlobStore();
        metaStore = new RdbMetadataStore();

        ctx.initializeAll(blobStore, metaStore);
    }

    public void registerDataType(DataType<? extends Message> type)
    {
        String storageID = type.getStorageID();

        if (getDataType(type.getDataClass()) != null)
            LOGGER.warn("Datatype for type " + type + " already registered");
        else if (storageIDs.contains(storageID))
            throw new IllegalStateException("Datatype.storageID for class " + type + " already registered; storageID=" + storageID);

        LOGGER.info("ApxData registering class " + type.getDataClass() + " with storageID " + storageID);

        datatypes.put(type.getDataClass(), type);
        storageIDs.add(storageID);
    }

    private DataType<? extends Message> getDataType(String canonicalClass)
    {
        for (Map.Entry<Class<?>, DataType<? extends Message>> entry : datatypes.entrySet())
        {
            if (entry.getKey().getCanonicalName().equals(canonicalClass))
                return entry.getValue();
        }

        return null;
    }

    private DataType<? extends Message> getDataType(Class<?> type)
    {
        return datatypes.get(type);
    }

    private <T extends Message> DataType<T> getDataTypeWithCheck(Class<?> type)
    {
        DataType<T> datatype = (DataType<T>) datatypes.get(type);

        if (datatype == null)
            throw new IllegalArgumentException("No registered DataType for class " + type);

        return datatype;
    }

    /**
     * QueryKeys provides a generalized mechanism for clients to be able to query on
     * client-defined RDB field values to retrieve groupingIDs to be able to get back
     * aggregate (i.e., across multiple groupingIDs) data.
     *
     * Supported value types: string, boolean, and integer values.
     *
     * This is a generalized structure; good design calls for query key naming
     * conventions above this level.
     */
    public static class QueryKeys
    {
        private Map<String, Object> keys  = new HashMap<>();

        public Map<String, Object> getKeys()
        {
            return keys;
        }

        public QueryKeys with(String key, Object val)
        {
            return with(key, val, false);
        }

        public QueryKeys with(String key, Object val, boolean replace)
        {
            String lc = key.toLowerCase();
            Object v  = keys.get(lc);

            if (val == null)
                throw new IllegalArgumentException("null query key value is not supported; key=" + key);
            else if (!replace && (v != null))
                throw new IllegalArgumentException("Query key [" + lc + "] was already added:  " + v);

            if (!supportedPrimitive(val))
                val = val.toString();

            keys.put(lc, val);

            return this;
        }

        private boolean supportedPrimitive(Object o)
        {
            return (o instanceof String) || (o instanceof Boolean) || (o instanceof Integer);
        }

        @Override
        public String toString()
        {
            return keys.toString();
        }
    }

    // ################################################################
    // ################################################################
    //  The form of these next two methods (putData, getData) are intended for
    //  theuse-cases where only one rowkey needs to be added at a time--i.e., if
    //  multiple rowkeys/groupingIDs need to be atomic, then these are NOT the right
    //  methods to call!
    // ################################################################
    // ################################################################
   
    /**
     * Adds/replaces data associated with the given groupingID.  It also adds/replaces the
     * querykey values for the given groupingID (+partitionID)
     */ 
    @Deprecated  // as of 8/24/2020 because we want per-PDS encryption key
    public <T extends Message> void putData(Class<T>  datatype,
                                            String    groupingID,       // required; e.g., "run parameters" or doc+generatorid
                                            String    partitionID,      // optional; null is the same as ""
                                            QueryKeys queryKeys,        // optional
                                            boolean   encrypt,          // force encryption; if false, DataType.canContainPHI determines encryption
                                            List<T>   data
        ) throws IOException
    {
        putData(null, datatype, groupingID, partitionID, queryKeys, encrypt, data);
    }

    public <T extends Message>  void putData(String    pdsID,            // non-null to get a per-PDS key
                                             Class<T>  datatype,
                                             String    groupingID,       // required; e.g., "run parameters" or doc+generatorid
                                             String    partitionID,      // optional; null is the same as ""
                                             QueryKeys queryKeys,        // optional
                                             boolean   encrypt,          // force encryption; if false, DataType.canContainPHI determines encryption
                                             List<T>   data
        ) throws IOException
    {
        DataType<T>                 type      = getDataTypeWithCheck(datatype);
        ChunkUtil<T>                chunkUtil = new ChunkUtil<>(); // instance only because of syntax error on typed static method call
        Pair<Integer, List<byte[]>> chunks    = chunkUtil.serializeAndChunkData(makeEncryptInfo(pdsID, encrypt, type), type, data);
        Map<String, BlobData>       blobs     = new HashMap<>();
        int                         size      = 0;

        if (partitionID == null)
            partitionID = "";

        // serialize and chunk data objects, compress, and encrypt (as necessary) and store
        // in Cassandra with rowkey {datatype}-{groupingID}.
        //
        // note that this particular code doesn't make use of the multi-rowkey "transaction" feature
        // of BlobStore.writeBlobs() as it accepts only a single (grouping+partition):List<T> data.
        // IF/WHEN multi-rowkey support is  required then a new method/signature will be required!

        // chunks.left is total size of byte arrays that are in chunks.right; useful for logging and/or
        // size queries...

        // blob name is "" => a single rowkey; the "" must match STRLIST_EMPTY contents
        blobs.put(EMPTY_KEY, new BlobData(partitionID, chunks.right));

        blobStore.writeBlobs(makeBlobGroupingID(type, groupingID), blobs, false);  // false => don't delete rowkeys

        for (Map.Entry<String, BlobData> entry : blobs.entrySet())
        {
            for (byte[] ba : entry.getValue().getData())
                size += ba.length;
        }

        metaStore.addOrUpdateMeta(ApxMeta.newBlobMeta(datatype.getCanonicalName(), groupingID, partitionID, data.size(), size), queryKeys);
    }

    /**
     * If the Type can contain PHI, then always encrypt it, otherwise encrypt only if client
     * wants it.
     */
    private EncryptInfo makeEncryptInfo(String pdsID, boolean encrypt, DataType<? extends Message> type)
    {
        return new EncryptInfo((encrypt || type.canContainPHI()), pdsID);
    }

    /**
     * For a given groupingID, return all the partitionIDs specified for it during calls to
     * putData()
     */
    public List<String> getPartitionIDs(Class<?> datatype, String groupingID) throws IOException
    {
        DataType<? extends Message> type  = getDataTypeWithCheck(datatype);  // datacheck
        List<ApxMeta>               metas = metaStore.getMetasForGrouping(groupingID);

        return metas.stream().map(m -> m.getPartitionID()).collect(Collectors.toList());
    }

    /**
     * Reads and reconstructs the list of objects with the given groupingID by first reading the
     * type of the data persisted followed by reading the data with the corresponding DataType.
     * Note that this works only for data that was saved after protoClass field was added to
     * RDB table apx_data (approx April 2021)
     *
     * Null is returned if there was no groupingID (vs empty list if there were just no objects
     * to read/restore).
     */
    public List<Object> getData(String groupingID)
    {
        List<ApxMeta>               metas = metaStore.getMetasForGrouping(groupingID);
        String                      proto;
        DataType<? extends Message> datatype;

        if (metas.size() == 0)
            return null;

        // multiple metas if we have partitions; check that we have the same protoclass
        proto = metas.get(0).getProtoClass();
        if ((proto == null) || (proto.length() == 0))
            throw new IllegalStateException("ApxDataDao.getData(groupingID) requires a persisted protoClass name but groupingID " + groupingID + " has empty value");

        for (int i = 1, m = metas.size(); i < m; i++)
        {
            String thisClass = metas.get(i).getProtoClass();

            if (!proto.equals(thisClass))
                throw new IllegalStateException("protoClass mismatch in ApxMeta for groupingID " + groupingID +
                                                proto + " vs " + thisClass);
        }

        datatype = getDataType(proto);
        if (datatype == null)
            throw new IllegalStateException("Unregistered protoClass for restoring groupingID " + groupingID);

        return getDataGuts(datatype, groupingID, null);
    }

    /**
     * Reads and reconstructs the list of objects supplied during the last matching
     * call to putData() with the same datatype and groupingID.
     */
    public <T> List<T> getData(Class<T> datatype, String groupingID)
    {
        return (List<T>) getDataGuts(getDataTypeWithCheck(datatype), groupingID, null);
    }

    public <T> List<T> getData(Class<T> datatype, String groupingID, String partitionID)
    {
        return (List<T>) getDataGuts(getDataTypeWithCheck(datatype), groupingID, partitionID);
    }

    public <T extends Message> Iterator<T> getDataIterator(Class<T> datatype, String groupingID)
    {
        DataType<T>     type  = getDataTypeWithCheck(datatype);
        List<ApxMeta>   metas = metaStore.getMetasForGrouping(groupingID);
        List<String>    parts = metas.stream().map(e -> e.getPartitionID()).collect(Collectors.toList());

        return new ChunkIterator(type, makeBlobGroupingID(type, groupingID), parts.iterator(), EMPTY_KEY);
    }

    /**
     * Raw data iteration requires that the client know all about the DataType as this method is really
     * meant to be a way to generically read data and to let the client deal with it all.  Because
     * the DataType is used to help form the final groupingID/rowkey, the client must pass in what
     * is normally pulled from the DataType for this.
     */
    public Iterator<byte[]> getRawDataIterator(String typeStorageID, String groupingID)
    {
        List<ApxMeta>   metas = metaStore.getMetasForGrouping(groupingID);
        List<String>    parts = metas.stream().map(e -> e.getPartitionID()).collect(Collectors.toList());

        return (Iterator<byte[]>) new ChunkIterator(null, makeBlobGroupingID(typeStorageID, groupingID), parts.iterator(), EMPTY_KEY);
    }

    public List<byte[]> getRawData(String storageID, String groupingID)
    {
        Map<String, List<BlobData>> blobs = blobStore.getBlobs(makeBlobGroupingID(storageID, groupingID), STRLIST_EMPTY, EMPTY_KEY);
        List<byte[]>                data  = new ArrayList<>();
        Iterator<byte[]>            iter  = getRawDataIterator(storageID, groupingID);

        while (iter.hasNext())
            data.add(iter.next());

        return data;
    }

    /**
     * Common code that reads and restores data from the grouping/partition where the data is assumed
     * to be of the given DataType
     */
    private List<Object> getDataGuts(DataType<? extends Message> type, String groupingID, String partitionID)
    {
        Map<String, List<BlobData>> blobs = blobStore.getBlobs(makeBlobGroupingID(type, groupingID), STRLIST_EMPTY, partitionID);
        List<Object>                recon = new ArrayList<>();

        // each BlobData.data should be unzipped/chunked and objects restored from it
        // and added to the main list
        blobs.get(EMPTY_KEY).forEach(e -> {
                recon.addAll(unchunkAndDeserializeData(type, e));
            });

        return recon;
    }

    private List<Object> unchunkAndDeserializeData(DataType<? extends Message> datatype, BlobData blob)
    {
        Iterator<Object> iter = new ChunkIterator(datatype, blob.getData().iterator());  //!! wrong--getData returns list anyway
        List<Object>     all  = new ArrayList<>();

        while (iter.hasNext())
            all.add(iter.next());

        return all;
    }

    /**
     * Queries the organizational metadata (supplied via the QueryKeys parameter to
     * all calls to putData()) to retrieve and return the set of groupingID values
     * that match the given query name=value constraints.  The interpretation of
     * these querykey params is to do an 'and' operation on all of them when doing
     * the query.
     */
    public List<String> getGroupingIDs(QueryKeys query)
    {
        return metaStore.queryGroupingIDs(query);
    }

    /**
     * Convenience method that queries for all groupingIDs, then fetches and unions
     * the restored objects
     */
    public List<Object> getData(Class<?> datatype, QueryKeys query)
    {
        List<Object>                 all  = new ArrayList<>();
        DataType<? extends Message>  type = getDataTypeWithCheck(datatype);

        for (String groupingID : getGroupingIDs(query))
            all.addAll(getDataGuts(type, groupingID, null));

        return all;
    }
     
    /**
     * Deletes both blob and metadata (including query keys) for the given groupingID
     */
    public void deleteData(Class<?> datatype, String groupingID) throws IOException
    {
        DataType<? extends Message> type = getDataTypeWithCheck(datatype);

        blobStore.deleteBlobs(makeBlobGroupingID(type, groupingID), STRLIST_EMPTY);
        metaStore.deleteMetaByGrouping(groupingID);
    }

    /**
     * For writing to blob store we include the datatype's storageID to avoid conflict
     * across datatypes for a given groupingID.  While this does allow two DataTypes to
     * use the same groupingID (e.g., patientUUID) it's not really anticipated that
     * this will be used.  If this case does happen then a getGroupingIDs() could return
     * IDs that span multiple DataTypes.
     */
    private String makeBlobGroupingID(DataType<? extends Message> datatype, String groupingID)
    {
        return makeBlobGroupingID(datatype.getStorageID(), groupingID);
    }
    private String makeBlobGroupingID(String storageID, String groupingID)
    {
        return storageID + "-" + groupingID;
    }

}
