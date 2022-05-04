package com.apixio.bms;

import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.dao.utility.DataStore;

/**
 * Abstraction of operations needed to manage Blob metadata.  It's expected that the
 * metadata is stored in a relational database as the rather limited number of instances
 * expected can be efficiently handled by an RDBMS.
 *
 * This interface is not to be used by clients of blob management system; only BlobManager
 * itself should use this.
 *
 * CRITICAL DESIGN POINT:  April 9, 2019.  Uploading a part now uses md5 hash to detect if
 * an identical blob has already been uploaded and if it has, the new blob entity/row will
 * make use of the existing one.  This has a major design impact when a real delete/purge
 * of data is implemented: a delete that is to also clean up S3-hosted blobs MUST NOT
 * delete the S3 object if it is being used by another blob entity!
 *
 * General rule:  a parameter of type Schema is needed ONLY if the metadata for a Blob
 * needs to be included/filled in.
 */
public interface MetaStorage extends DataStore
{
    /**
     * Convention, to allow other DAOs access to an instance of this in a controlled way
     */
    public static final String ID = "metastore";

    /**
     * Unfortunately DB transactional support is required... typical model but nested
     * transactions are not supported.  Callers of beginTransaction MUST call either
     * abortTransaction or commitTransaction.  Transactions are managed on a per-thread
     * basis.
     */
    public void beginTransaction();
    public void commitTransaction();
    public void abortTransaction();
    
    /**
     * Creates a new row in blob table.  non-null blob.xuuids must not already exist.  If the blob has
     * non-null metadata then rows are also added to the metadata table within a transaction
     */
    public void createMetadata(Blob blob);

    /**
     * Writes out only the modified parts of the blob.  If metadata was modified then all
     * existing rows in the meta table for that blob are first removed then all metadata
     * are added
     */
    public void updateMetadata(Blob blob, boolean replace);

    /**
     * Finds and restores the blob with the given XUUID, null if there is no such blob.
     */
    public Blob getByBlobID(Schema schema, XUUID id);

    /**
     * Finds and restores the blob that has the given parent blob and has the
     * given part name.
     */
    public Blob getByPartName(Schema schema, Blob parent, String part);

    /**
     * Return list of children blobs; empty list if none.
     */
    public List<Blob> getParts(Blob parent);

    /**
     * Look up the parent/containing blobs for each of the parts in the list.  The returned
     * structure is a map from the part to its parent blob.
     */
    public Map<Blob, Blob> getPartParents(List<Blob> parts);

    /**
     * Finds all blobs with the given md5 hash.  Only parts will (theoretically) be
     * returned here.  Metadata is NOT returned here (as it's not needed externally)
     */
    public List<Blob> getByMd5Hash(String md5);

    /**
     * Deletes metadata, non-shared s3 blob data, and blob, for all parts owned by the blob
     * and for the Blob itself
     */
    public List<String> deleteBlob(Blob blob);

    /**
     * Deletes metadata, non-shared s3 blob data, and blob
     */
    public List<String> deletePart(Blob parent, String part);

    /**
     *
     */
    public List<Blob> queryMatching(Schema schema, Query t);

    /**
     * Owner management; only one blob can own a given logicalID (but a given blob can
     * own more than one logicalID).
     */
    public void         setOwner(Blob owner, List<String> logicalIDs);
    public Blob         getOwner(Schema schema, String logicalID);     // can return null
    public List<String> getOwned(Blob owner);                          // can return empty list

    /**
     * addDependencies declares "leftID USES rightID" for all in rightIDs.
     * Note that for addDependencies, if replace==true and rightIDs.size()==0,
     * then that's the same as removing all dependencies for leftID.
     */
    public void addDependencies(String leftID, List<String> rightIDs, boolean replace);

    /**
     * Performs the dependency query direction specified by "rel":
     *
     *  * USES:     returns set of rightIDs specified in addDependency() for the given logicalID
     *  * USED_BY:  returns set of leftIDs  specified in addDependency() for the given logicalID
     */
    public List<String> getLogicalDependencies(String logicalID, Relationship rel);

    /**
     * Efficient convienence method that does the equivalent of:
     *
     *  return getLogicalDependencies().stream().map(l -> getOwner(l)).collect(Collectors.toList());
     */
    public List<Blob> getPhysicalDependencies(Schema schema, String logicalID, Relationship rel);

    /**
     * Marks leftID as locked; once locked it can never be unlocked.  Note that an id can be locked
     * if and only if it has already been used as leftID in addDependencies()
     */
    public void setLogicalIdLocked(List<String> leftIDs);

    /**
     * Return true iff at least one tuple of [leftID, 'uses', ?] has 'is_locked'==true
     */
    public boolean isLogicalIdLocked(String leftID);

}
