package com.apixio.dao.apxdata;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.apixio.dao.apxdata.BlobStore.BlobData;
import com.apixio.dao.utility.DaoServices;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlRowData;
import com.apixio.datasource.cassandra.CqlTransactionCrud;
import com.apixio.restbase.PersistenceServices.CassandraCluster;

/**
 * The high level need is to store uniquely named lists of byte[]s for a given
 * groupingID.  The source + unique names are used to form the rowkeys and the
 * list element index is the column name.
 *
 * The rowkey consists of a groupingID that's the base prefix of the
 * rowkey while the last part is taken from the name of the blob/list.  More explicitly
 *
 *  rowkey={groupingID} : {blobName}
 *  colname={partition-}{n}
 *  colval=byte[]
 *
 * For example, if writeBlobs is called with the groupingID of "theDocument" and
 * the blobs (map from blob name to BlobData) being the following
 *
 *  { "para1" => BlobData("partitionX", [ "item1", "item2" ]),
 *    "para1" => BlobData("partitionZ", [ "item3", "item4" ]),
 *    "para2" => BlobData(null,         [ "item5", "item6" ]) }
 *
 * and 
 * will end up with the following rowkeys and column names & values:
 *
 *  * rk = theDocument:para1
 *    colname/value = partitionX-0/item1
 *    colname/value = partitionX-1/item2
 *    colname/value = partitionZ-0/item3
 *    colname/value = partitionZ-1/item4
 *
 *  * rk = theDocument:para2
 *    colname/value = 0/item5
 *    colname/value = 1/item6
 */
public class CassBlobStore implements BlobStore
{
    // column name separate character
    private static final String CN_SEP      = ":";
    private static final char   CN_SEP_CHAR = ':';

    /**
     * Sending large amounts of data in blob columns can result in server-side timeouts.  This const is
     * use to limit the number of columns sent in a single write request.  THIS DESTROYS Cassandra
     * "transactions" but appears to be unavoidable currently.
     */
    private static final int MAX_COLUMN_WRITE = 10;

    /**
     * centralizes Cassandra column name construction and deconstruction
     */
    private static class ColumnName
    {
        String partitionID;
        int    index;

        /**
         * Form colname in a way that params can be parsed from constructor
         */
        static String composeFrom(String partition, int index)
        {
            return partition + CN_SEP + Integer.toString(index);
        }

        ColumnName(String colname)
        {
            int idx = colname.lastIndexOf(':');

            if (idx == -1)
                throw new IllegalArgumentException("Column name [" + colname + "] can't be decomposed into partition+index");

            partitionID = colname.substring(0, idx);
            index       = Integer.parseInt(colname.substring(idx + 1));
        }

        @Override
        public String toString()
        {
            return "{partition=" + partitionID + ", index=" + index + "}";
        }
    }

    /**
     * To segregate keys from any other keys that might end up in the configured
     * keyspace.  Do NOT change as all existing data will become inaccessible.
     */
    private final static String KEY_PFX_SOURCE = "blobsrc.";

    private final static ByteBuffer EXISTS_BB  = ByteBuffer.wrap("x".getBytes());

    /**
     * Define a new CassandraCluster for us.  The string ID is used as part of yaml key name
     */
    public static final CassandraCluster SIGNAL_CLUSTER = CassandraCluster.newCluster("signal");

    /**
     * The all-powerful connection to Cassandra.
     */
    private CqlTransactionCrud cqlCrud;
    private String             cfName;

    /**
     * BlobIterator reads through the column values for the constructed rowkey starting at
     * "0" and incrementing until no column value exists for that index.
     */
    private class BlobIterator implements Iterator<byte[]>
    {
        private String       rk;
        private int          nextCol  = 0;   // "0" is first col name used in writeBlobs()
        private byte[]       nextBlob = null;
        private String       partID;

        BlobIterator(String groupingID, String listName, String partID)
        {
            this.rk     = rowkey1(groupingID, listName);
            this.partID = partID;
        }

        @Override
        public boolean hasNext()
        {
            if (nextBlob == null)
            {
                ByteBuffer bb = cqlCrud.getColumnValue(cfName, rk, ColumnName.composeFrom(partID, nextCol));

                if (bb != null)
                    nextBlob = bbToArray(bb);
            }

            return (nextBlob != null);
        }

        @Override
        public byte[] next()
        {
            byte[]  blob = nextBlob;

            if (blob == null)
                throw new NoSuchElementException();

            nextCol++;
            nextBlob = null;

            return blob;
        }

    }

    /**
     *
     */
    @Override
    public String getDataStoreID()
    {
        return ID;
    }

    @Override
    public void initialize(InitContext initContext)
    {
        if (initContext.phase == InitPhase.CREATE)
        {
            CqlCrud crud = initContext.daoServices.getGenericCqlCrud(SIGNAL_CLUSTER);

            cfName  = "apx_algoclouddata"; //!! should be from config!

            if (!(crud instanceof CqlTransactionCrud))
                throw new IllegalStateException("Cassandra-based blob store requires CqlTransactionCrud as configured CqlCrud instance; type is " + crud.getClass().getName());

            cqlCrud = (CqlTransactionCrud) crud;

            cqlCrud.setBatchSyncEnabled(true);  // forces sync for insert & delete
        }
    }

    /**
     * Write the set of named blobs to the storage engine.  The groupingID is used as a base for
     * the rowkey, and the keys of the Map (which are the BlobData names) are appended to form
     * the full rowkey.  This allows multiple rowkeys to be added in a pseudo-transaction.  The
     * values of the Map contain the BlobData objects (actual data + partitionID) and the
     * partitionIDs in those objects are used so that concurrent writes using the same-named
     * groupingID+keys won't overwrite each other
     */
    @Override
    public void writeBlobs(String groupingID, Map<String, BlobData> blobs, boolean replace)  // map from blob name to data
    {
        List<CqlRowData>  rows = new ArrayList<CqlRowData>();
        int               rowCount;

        blobs.forEach((name, blob) ->
            {
                String       rk1    = rowkey1(groupingID, name);
                List<byte[]> data   = blob.getData();
                String       partID = blob.getPartitionID();

                //                System.out.println("#### writeBlobs, partID=" + partID + ", list.len=" + data.size());

                if (partID == null)
                    partID = "";

                if (replace)
                    cqlCrud.deleteRow(cfName, rk1, false);  // false forces sync operation

                for (int i = 0; i < data.size(); i++)
                {
                    //                    System.out.println("writeBlobs ## rk1=" + rk1 + ", cn= " + ColumnName.composeFrom(partID, i) + ", byte.len=" + data.get(i).length);
                    rows.add(new CqlRowData(cfName, rk1, ColumnName.composeFrom(partID, i), ByteBuffer.wrap(data.get(i))));
                }
            });

        // chunk into MAX_COLUMN_WRITE at a time to avoid Cassandra server issues due to size
        rowCount = rows.size();

        for (int idx = 0; idx < rowCount; idx += MAX_COLUMN_WRITE)
        {
            List<CqlRowData> sublist = rows.subList(idx, Math.min(idx + MAX_COLUMN_WRITE, rowCount));
            try
            {
                cqlCrud.beginTransaction();
                cqlCrud.insertRows(sublist);
                //                DebugUtil.debug("CassBlobStore.writeBlobs inserted {} rows to Cassandra", rows.size());
                cqlCrud.commitTransaction();
            }
            catch (RuntimeException rx)
            {
                //                DebugUtil.warn("CassBlobStore.writeBlobs Caught RuntimeException {}", rx.toString());
                throw rx;
            }
        }
    }

    /**
     * Read in given blobs for the groupingID.  Client must supply exact list names that
     * were supplied as map keys in writeBlobs().
     *
     * If partitionID==null get all columns (i.e., get all partitions).  If it's null, then get
     * only those columns for that partition (multiple columns because each partition can have
     * more than one byte[]).
     */
    @Override
    public Map<String, List<BlobData>> getBlobs(String groupingID, List<String> blobNames, String partID)
    {
        if ((blobNames == null) || (blobNames.size() == 0))
            throw new IllegalArgumentException("getBlobs requires a list of blob names");

        final Map<String, List<BlobData>> blobs = new HashMap<>();

        for (String name : blobNames)  // one rowkey == name
        {
            SortedMap<String, ByteBuffer>   allCols = cqlCrud.getColumnsMap(cfName, rowkey1(groupingID, name), partID);
            final List<BlobData>            blist   = new ArrayList<>(allCols.size() / 2);
            final Map<String, List<byte[]>> rawData = new HashMap<>();
            int                             idx     = 0;
            ByteBuffer                      bb;

            blobs.put(name, blist);

            // keys are like:  partitionX:0, partitionX:1, ..., partitionY:1, partitionY:3
            // where there is no ordering guarantee on column names.  this code reassembles
            // the disordered columns back into [partitionID,list<bytedata>]
            allCols.forEach((colname, data) -> {
                    ColumnName   cn = new ColumnName(colname);
                    List<byte[]> bd = rawData.get(cn.partitionID);

                    if (bd == null)
                    {
                        bd = new ArrayList<byte[]>();
                        rawData.put(cn.partitionID, bd);
                    }

                    // note that this is the line that makes more than 9 items (columns) for a given partitionID
                    // actually work:  by decomposing the colname back to index+id (where id is used to
                    // select the list<> for the partitionID) we add the coldata to the list<> at the right
                    // index.
                    addToList(bd, cn.index, bbToArray(data));
                });

            // sanity check that all elements of the lists are non-null
            rawData.forEach((partitionID, dlist) -> {
                    for (byte[] bt : dlist)
                        if (bt == null)
                            throw new RuntimeException("Restored list of byte[] didn't fill in all elements of BlobData!" +
                                                       "  groupingID=" + groupingID + ", blobname=" + name + ", partitionID=" + partitionID);
                });

            //            rawData.forEach((partitionID, dlist) -> System.out.println(" partitionID=" + partitionID + ", dlist.len=" + dlist.size()));

            rawData.forEach((partitionID, dlist) -> blist.add(new BlobData(partitionID, dlist)));
        }

        return blobs;
    }

    /**
     * Ensures that there's enough elements in the list to set the given index to the given
     * value (since java.util.List doesn't do this directly)
     */
    static void addToList(List<byte[]> list, int idx, byte[] data)
    {
        while (list.size() <= idx)
            list.add(null);

        list.set(idx, data);
    }

    /**
     * Return iterators that will allow streaming access to actual byte arrays
     * for each groupingID.  Since byte arrays are stored with column names being
     * just 0...n, we just need to read the next column via an incrementing int.
     *
     * Key of returned Map is the blobName and the value is an iterator to fetch
     * the byte arrays
     */
    @Override
    public Iterator<byte[]> iterateBlobs(final String groupingID,
                                         final String partitionID,
                                         final String listName)
    {
        if (listName == null)
            throw new IllegalArgumentException("iterateBlobs requires a non-null listname");

        return new BlobIterator(groupingID, listName, partitionID);
    }

    /**
     * Deletes blob elements that were created by writeBlobs().  elementNames MUST NOT BE NULL
     * as the writeBlobs created rowkeys based on the blob names!
     */
    @Override
    public void deleteBlobs(String groupingID, List<String> blobNames)
    {
        if (blobNames == null)
            throw new IllegalArgumentException("deleteBlobs requires a list of blob names");

        List<CqlRowData> dels = new ArrayList<>();

        for (String ele : blobNames)
        {
            String     rk1  = rowkey1(groupingID, ele);
            CqlRowData crd = new CqlRowData(cfName, rk1);

            crd.setCqlOps(CqlRowData.CqlOps.deleteRow);

            dels.add(crd);
        }

        cqlCrud.insertOrDeleteOps(dels);
    }

    /**
     *
     */
    private byte[] bbToArray(ByteBuffer bb)
    {
        int    size  = bb.limit() - bb.position();
        byte[] bytes = new byte[size];

        bb.get(bytes);

        return bytes;
    }

    /**
     *
     */
    private static String rowkey1(String groupingID, String listName)
    {
        return KEY_PFX_SOURCE + groupingID + ":" + listName;
    }

}
