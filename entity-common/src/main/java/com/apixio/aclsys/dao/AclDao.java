package com.apixio.aclsys.dao;

import java.io.IOException;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.aclsys.entity.Operation;

/**
 * Interface for persistence of ACL structures.
 */
public interface AclDao
{
    /**
     * Return true if the rowkey that maps from Operations to the list of Subjects
     * that have been given permission to perform the Operation has been built.  This
     * test is necessary because rowkey5 was added after release to production so
     * that rowkey + columns need to be built from a key scan.
     */
    public boolean canGetSubjectsByOperation();

    /**
     * Build (or, rebuild) the contents of rowkey5 in a background thread.
     */
    public void scanForSubjectsByOperation(final List<Operation> ops);

    /**
     * Given an Operation, return the list of Subjects that have been granted
     * permission (at some point in the past) to perform that operation on
     * some Object.  Note that there can be false positives as this list
     * is not updated when a permission is removed.
     */
    public List<XUUID> getSubjectsByOperation(Operation op) throws IOException;

    /**
     * Delete an Operation by deleting ALL references to it.  THIS IS EXTREMELY DESTRUCTIVE!
     * All granted permissions will be removed, etc., and there is no recovery from it.
     *
     * We do the efficient thing here and just delete the entire rowkey for #1 and #3.
     * The alternative is to just enumerate subjects and objects and do a doubly-nested
     * loop and remove each permission...
     */
    public void deleteOperation(Operation op) throws IOException;

    /**
     * Gives the Subject the rights to perform the Operation on the Object.  This
     * consists of denormalizing into the three rowkeys since we don't know how
     * this privilege will need to be looked up in the future.
     */
    public void addPermission(XUUID subject, Operation operation, String object) throws IOException;

    /**
     * Remove from the Subject the rights to perform the Operation on the Object.
     */
    public void removePermission(XUUID subject, Operation operation, String object) throws IOException;

    /**
     * Returns list of Objects where the given Subject is allowed to perform the
     * given Operation.  This is implements Query#1.
     */
    public boolean testBySubjectOperation(XUUID subject, Operation operation, String object) throws IOException;

    /**
     * Returns list of Objects where the given Subject is allowed to perform the
     * given Operation.  This is implements Query#1:  "What objs can Subj do Op on?"
     */
    public List<String> getBySubjectOperation(XUUID subject, Operation operation) throws IOException;

    /**
     * Returns list of Operations (names of them) where the given Subject is
     * allowed to perform it on the given Object.  (Geez, that's poorly worded...)
     * This is implements Query#2:  "What ops can Subj do on Obj?"
     *
     *
     * TODO:  this should be using a cache in order to avoid making calls to Cassandra
     * for the case of AclLogic.hasAccess() when needing to determine if a User has
     * access but the privileges are granted to UserGroup(s) that the User might be
     * a part of.  Quick design:
     *
     *  * cache key is 'object'
     *
     *  * JVM structure is Map<String, Map<XUUID, List<String>>> (in English:
     *     map from ObjectID to:  a map from SubjectID to the list of op names
     *
     *  * a new rowkey is added that records lastAclUpdateTime for an Object (since
     *    that's the cache key); rowkey column name is ObjectID and column value
     *    is long epoch time of last update
     *
     *  * an ACL update will write to new rowkey
     *
     *  * AclLogic.hasAccess will call a new method to check the cache; this method
     *    will get the timestamp and if it's newer, it will delete the Map<>
     *    element for the given ObjectID.  This must be a separate operation done
     *    outside the loop in hasAccess in order to minimize the # of calls to
     *    Cassandra.
     *
     *  * this method (getBySubjectObject) will look up Map<> by ObjectID and
     *    if element exists, it will return a get on the submap.  If it doesn't
     *    exist, it will call into Cassandra and cache the results.
     *
     */
    public List<String> getBySubjectObject(XUUID subject, String object) throws IOException;

    /**
     * Returns list of Subjects that have the permission to perform the given
     * Operation on the given Object.  This implements Query#3:  "What subjs can
     * do Op on Obj?"
     */
    public List<XUUID> getByObjectOperation(String object, Operation operation) throws IOException;

    /**
     * Checks if the "last updated" date kept on Cassandra for the given Object is
     * more recent than what's been loaded into the JVM.
     */
    public void checkCache(String object) throws IOException;

    /**
     * Records the subject/object constraints.  A constraint means specifically that if the
     * given subject attempts to call AclLogic.addPermission for a given operation, that the
     * then-supplied subject and object meet the constraints recorded here.
     *
     * Constraints are recorded on a per-grantor (i.e., Subject) and Operation basis since those
     * two parameters are passed to AclLogic.addPermission()
     */
    public void recordConstraint(XUUID subject, Operation operation, String subjConstraint, String objConstraint) throws IOException;

    /**
     * Removes the subject/object constraints
     */
    public void removeConstraint(XUUID subject, Operation operation) throws IOException;

    /**
     * Return true if the given Subject is constrainted when attempting to add permissions
     * on the given Operation.
     */
    public boolean hasConstraints(XUUID subject, String operation) throws IOException;

    /**
     * Returns the subject/object constraint that was recorded in recordConstraint().
     */
    public String getSubjectConstraint(XUUID subject, String operation) throws IOException;

    public String getObjectConstraint(XUUID subject, String operation) throws IOException;

}
