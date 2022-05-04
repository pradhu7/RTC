package com.apixio.dao.apxdata;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.apixio.dao.utility.DataStore;  // TODO move to better package

/**
 * Defines the mechanism to write and read sets of named, opaque data to some storage engine.
 *
 * The organization of this opaque data is:
 *
 *  * raw opaque data is presented as a unit (BlobData) via a list of byte arrays (i.e.,
 *    List<byte[]>);
 *
 *  * a BlobData instance collects this List<byte[]> along with a "partitionID" that can be
 *    empty/null or is a unique name to support concurrent writes
 *
 *  * each BlobData is given a unique String name (which is different from the partitionID); this
 *    is the thing that's called a blob
 *
 *  * a set of these uniquely named BlobDatas are grouped by a groupingID such that
 *    Cassandra-atomic writes of a set is done.
 *
 * Think of partitionID as defining which portion of the BlobData is being written.
 *
 * Retrieving this opaque data is efficiently done via the groupingID--all data for a given
 * BlobData name is restored with one low-level fetch operation, and includes all data from
 * all partitionIDs for that groupingID + BlobData-name.
 *
 * Some real Apixio examples of the above organization:
 *
 *  * storing signal data for a document: for this case the siggen cluster creates ALL
 *    signal data for a given document and generatorid.  no concurrent writing is done.
 *    groupingID is DocumentUUID, BlobData name is the generatorid, partitionID is "" and
 *    all actual signal data is in the single BlobData instance.
 *
 *  * IRR: multiple JVMs (one per docset) will be producing data for the same
 *    [project:from:to] run parameters so concurrent writing will be done.  groupingID is
 *    probably [project:from:to] in order to efficiently restore all data for that param
 *    set.  The partitionID for each BlobData instance is the JVM number.  The name of the
 *    BlobData can be a constant (perhaps "") since we're only writing one BlobData at a time
 *    for the JVM (i.e., no need for ACID-ish writes across multiple BlobDatas).
 *
 * If a client writes different opaque data with the same groupingID and partitionID then
 * a replace is done only up to the max index of the List<byte[]>; i.e., if fewer elements
 * are in the list than what's already persisted, then old data will be left around unless
 * "replace" param is true (which is logically incompatible with non-null partitionIDs, which
 * means that a multi-partition re-run of blob data generation must first delete the blob).
 */
public interface BlobStore extends DataStore
{
    /**
     * BlobData collects the blob of data (as List<byte[]>) along with a partitionID.
     */
    public static class BlobData
    {
        private String       partitionID;  // prefixed to "0", "1", ... for column names
        private List<byte[]> data;

        public String getPartitionID()
        {
            return partitionID;
        }
        public List<byte[]> getData()
        {
            return data;
        }

        public BlobData(String partitionID, List<byte[]> data)
        {
            this.partitionID = (partitionID != null) ? partitionID.trim() : "";
            this.data        = data;
        }

        @Override
        public String toString()
        {
            return ("{partition=" + partitionID +
                    "; list.len=" + data.size() +
                    "}");
        }
    }

    /**
     * Convention, to allow other DAOs access to an instance of this in a controlled way
     */
    public static final String ID = "blobstore";

    /**
     * Write the set of named blobs to the storage engine.  The groupingID is used as a base for
     * the rowkey, and the keys of the Map (which are the BlobData names) are appended to form
     * the full rowkey.  This allows multiple rowkeys to be added in a pseudo-transaction.  The
     * values of the Map contain the BlobData objects (actual data + partitionID) and the
     * partitionIDs in those objects are used so that concurrent writes using the same-named
     * groupingID+keys won't overwrite each other
     */
    public void writeBlobs(String groupingID, Map<String, BlobData> blobs, boolean replace);

    /**
     * Read in given blobs for the groupingID.  This will be suboptimal if list size is more than
     * several thousand.  Client must supply exact element Map keys as given in the Map<> keys to
     * writeBlobs().
     *
     * Because we assume that clients that are calling writeBlobs in parallel are writing a
     * list<byte[]> that is self-contained and are not supposed to be just appended to a single
     * (merged) list, the return value reflects the partition grouping of the data.  For clients
     * that don't write blobs concurrently (i.e., a single partitionID), a list with a single
     * element will be returned.
     *
     * If partitionID=null then all partitions are restored, otherwise just the one is restored.
     * Note that a single partitionID could (will, most likely) have more than one column for it.
     */
    public Map<String, List<BlobData>> getBlobs(String groupingID, List<String> blobNames, String partitionID);

    /**
     * Creates Iterator that will return the persisted byte arrays for the list name of the
     * given groupingID.  NO attempt is made to do any concurrency checking so if the
     * source:listname is rewritten during iteration, bad things will probably happen.
     *
     * The partitionID is required so that all partitions that were written can be restored.
     */
    public Iterator<byte[]> iterateBlobs(final String groupingID,
                                         final String partitionID,
                                         final String listName);

    /**
     * Deletes blob elements that were created by writeBlobs().  If blobNames is null then
     * the groupingID itself is deleted (i.e., it acts as though all existing element names
     * were supplied).
     *
     * The deletion is a synchronous operation against the actual datastore.
     */
    public void deleteBlobs(String groupingID, List<String> blobNames);

}
