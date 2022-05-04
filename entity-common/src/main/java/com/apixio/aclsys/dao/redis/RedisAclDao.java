package com.apixio.aclsys.dao.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.aclsys.dao.AclDao;
import com.apixio.aclsys.entity.Operation;
import com.apixio.restbase.DaoBase;

/**
 * ACLs are managed through denormalizing the permissions into three keys so that
 * we can efficiently answer the following questions:
 *
 *  1.  given a subject and an operation, what objects can that user perform that op on?
 *
 *  2.  given an object, what can a subject do within/on that object?
 *
 *  3.  given an object and operation, what subjects can do that?
 *
 *  4.  given Subject, Operation, and Object, is the Subject permitted to perform that Operation
 *      on that Object?
 *
 * Additionally, we need to be able to delete operations and all data (ACLs) associated with
 * them.
 *
 * "Subject" is either a User or a UserGroup.  If the subject is a User then the test for
 * permissions will also include all UserGroups that the User ia a member of.
 *
 * The Redis structure used for all this information is a HASH; the key, fieldname and field values
 * for the HASHes are (all keys are appropriately prefixed):
 *
 *  1.  [key={Subject}:{Operation}, HASH fieldname={Object},    HASH fieldvalue=true/false]   # "false" allows a 'deny' exception later
 *
 *  2.  [key={Subject}:{Object},    HASH fieldname={Operation}, HASH fieldvalue=true/false]
 *
 *  3.  [key={Object}:{Operation},  HASH fieldname={Subject},   HASH fieldvalue=true/false]
 *
 * Additionally, to help performance, there's a key (#4) that records the
 * last time an ACL update occurred for a given Object in order to be able
 * to invalidate JVM caches:
 *
 *  4.  [key="objCache", HASH fieldname={Object}, HASH fieldvalue={timeUpdated}]
 *
 * In order to support a delete, albeit an expensive one, the following key is
 * needed:
 *
 *  5.  [key="allsubs:"{Operation}, HASH fieldname={Subject}, HASH fieldvalue=x]    # "x" for existence
 *
 * Retrieving all HASH fields for key#1 returns the list of objects (e.g., patient orgs, or
 *  patient groups, etc.) that the Subject can perform that operation on.  E.g.,
 *  what orgs is Joe allowed to Code for?  Call this Query#1.
 *
 * Retrieving all HASH fields for key#2 returns the list of Operations (e.g., CanCode,
 *  CanReview) that the Subject can perform on the given object.  E.g., what
 *  operations can Joe do within the given Org?  Call this Query#2.
 *
 * Retrieving all HASH fields for key#3 returns the list of subjects (users or user groups)
 *  that can perform the operation on within the object.  E.g., who can CodeFor the
 *  given patient group?  Call this Query#3.
 *
 * Note that it's really just the model where each of the 3 types (Subject,
 * Operation, Object) are stored as a HASH fieldvalue, where the key for that
 * HASH fieldname is comprised of the other two data items.  We try to keep a fixed order of
 * things to make it simpler (order is generally Subject, Operation, Object).
 *
 * Regarding deleting of Operations, it's possible by getting all the HASH fields of
 * key5, then for each of those Subjects, get all Objects in key1, then remove
 * permission on the triple <Subject, Operation, Object>.  Unfortunately the HASH fields
 * of key5 can have false positives:  a removePermission can't delete the HASH field as
 * there could be permissions for that Subject/Operation combo (on a different
 * Object).
 *
 * Key5 was added after code went into production and so it needed to be built from
 * enumerating other keys.  There is a key5 for each Operation so the marker used for
 * this purpose is just "allsubs:" (the string "" for the operation name) with a
 * HASH field "scanned" to indicate things were scanned.
 *
 * ################
 *
 * The ACL system needs to deal with delegation of the ability to grant permissions.  In
 * order to do this without allowing those people to grant permissions to anyone on any
 * object, both subjects and objects can be constrained.  In order to support these
 * constraints, the following keys are needed:
 *
 *  6.  [key="constraint:"{Subject}:{Operation}, HASH fieldname=["subject","object"], HASH fieldvalue={constraint}]
 *
 * where {constraint} is an opaque string.
 */
public class RedisAclDao extends RedisDao implements AclDao
{
    /**
     * Fixed field names for key6
     */
    private final static String SUBJ_CONSTRAINT_FIELDNAME = "subject";
    private final static String OBJ_CONSTRAINT_FIELDNAME  = "object";

    /**
     * Use cache for getBySubjectObject:
     */
    private static boolean  USE_CACHE = true;

    /**
     * Store the String form of the BOOLEAN TRUE and FALSE once.
     */
    private final static String TRUE_STR  = Boolean.TRUE.toString();
    private final static String FALSE_STR = Boolean.FALSE.toString();

    /**
     * HASH fieldname to record if scan to create key5 has been done.  Note
     * that we use "rk5"--this is a holdover from when ACLs were kept in Cassandra
     */
    private final static String SCAN_MARKER = "rk5-scandone";

    /**
     * make sure keys for ACLs are distinct from everything else
     */
    private final static String     KEY_PFX  = "acl.";
    private final static String     KEY_4    = KEY_PFX + "objCache";
    private final static String     KEY_5    = KEY_PFX + "allSubs:";
    private final static String     KEY_6    = KEY_PFX + "constraint:";

    /**
     * In order to support an efficient AclLogic.hasAccess() with one level of
     * indirection (where a User is a member of a UserGroup, and it's the group
     * that's been given rights to an Object, but the User is the one that is
     * passed in as the Subject), we need to cache the results of the call
     * getBySubjectObject.  The cache key is the ObjectID (e.g., the org ID for
     * "Scripps").
     *
     * This instance variable maps from the ObjectID to the results of the calls
     * to getBySubjectObject
     */
    private Map<String, Map<XUUID, List<String>>> cacheGetBySubjObj;
    private Map<String, Long> cacheGetBySubjObjLastAdded;

    /**
     *
     */
    public RedisAclDao(DaoBase base, String cfName)  // cfName not used
    {
        super(base);

        if (USE_CACHE)
        {
            cacheGetBySubjObj          = new HashMap<String, Map<XUUID, List<String>>>();
            cacheGetBySubjObjLastAdded = new HashMap<String, Long>();
        }
    }

    /**
     * Return true if the key that maps from Operations to the list of Subjects
     * that have been given permission to perform the Operation has been built.  This
     * test is necessary because key5 was added after release to production so
     * that key + fieldnames need to be built from a key scan.
     */
    public boolean canGetSubjectsByOperation()
    {
        return (getKeyHashValue(key5(""), SCAN_MARKER) != null);
    }

    /**
     * Build (or, rebuild) the contents of key5 in a background thread.
     */
    public void scanForSubjectsByOperation(final List<Operation> ops)
    {
        Thread scanner = new Thread() {
                public void run()
                {
                    try
                    {
                        rebuildKey5(ops);
                    }
                    catch (Exception x)
                    {
                        x.printStackTrace();
                    }
                }
            };

        scanner.start();
    }

    /**
     * Given an Operation, return the list of Subjects that have been granted
     * permission (at some point in the past) to perform that operation on
     * some Object.  Note that there can be false positives as this list
     * is not updated when a permission is removed.
     */
    public List<XUUID> getSubjectsByOperation(Operation op) throws IOException
    {
        String              key5 = key5(op);
        Map<String, String> vals = getKeyHash(key5);
        List<XUUID>         subs = new ArrayList<XUUID>();

        // for now assume value is 'true' so that we care only about the fieldname
        for (Map.Entry<String, String> e : vals.entrySet())
            subs.add(XUUID.fromString(e.getKey()));

        return subs;
    }

    /**
     * Delete an Operation by deleting ALL references to it.  THIS IS EXTREMELY DESTRUCTIVE!
     * All granted permissions will be removed, etc., and there is no recovery from it.
     *
     * We do the efficient thing here and just delete the entire key for #1 and #3.
     * The alternative is to just enumerate subjects and objects and do a doubly-nested
     * loop and remove each permission...
     */
    public void deleteOperation(Operation op) throws IOException
    {
        // fully delete key1,3,5; delete fields in key2; update key4 value

        List<XUUID> subs   = getSubjectsByOperation(op);
        String      ts     = Long.toString(System.currentTimeMillis());
        String      opName = op.getName();
        boolean     done   = false;

        try
        {
            openTransaction();

            // before deleting key1,3,5 we need to delete specific fields on key2
            // and update key4 fields
            for (XUUID sub : subs)
            {
                for (String obj : getBySubjectOperation(sub, op))
                {
                    deleteKeyHashField(key2(sub, obj), opName);
                    updateObjectTimestamp(obj, ts);

                    deleteKey(key3(obj, op));
                }

                deleteKey(key1(sub, op));
            }

            deleteKey(key5(op));

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Gives the Subject the rights to perform the Operation on the Object.  This
     * consists of denormalizing into the three rowkeys since we don't know how
     * this privilege will need to be looked up in the future.
     */
    public void addPermission(XUUID subject, Operation op, String object) throws IOException
    {
        String  ss   = subject.toString();
        boolean done = false;

        try
        {
            openTransaction();

            // keep these in sync w/ removePermission()'s operations
            setKeyHashValue(key1(subject, op),     object,       TRUE_STR);
            setKeyHashValue(key2(subject, object), op.getName(), TRUE_STR);
            setKeyHashValue(key3(object, op),      ss,           TRUE_STR);
            setKeyHashValue(key5(op),              ss,           TRUE_STR);

            updateObjectTimestamp(object, null);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Remove from the Subject the rights to perform the Operation on the Object.
     */
    public void removePermission(XUUID subject, Operation op, String object) throws IOException
    {
        boolean done = false;

        try
        {
            openTransaction();

            // keep these in sync w/ addPermission()'s operations
            deleteKeyHashField(key1(subject, op),     object);
            deleteKeyHashField(key2(subject, object), op.getName());
            deleteKeyHashField(key3(object,  op),     subject.toString());
            // note that we don't delete anything in key5 here

            updateObjectTimestamp(object, null);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Returns list of Objects where the given Subject is allowed to perform the
     * given Operation.  This is implements Query#1.
     */
    public boolean testBySubjectOperation(XUUID subject, Operation operation, String object) throws IOException
    {
        return (getKeyHashValue(key1(subject, operation), object) != null);
    }

    /**
     * Returns list of Objects where the given Subject is allowed to perform the
     * given Operation.  This is implements Query#1:  "What objs can Subj do Op on?"
     */
    public List<String> getBySubjectOperation(XUUID subject, Operation operation) throws IOException
    {
        String              key1 = key1(subject, operation);
        Map<String, String> vals = getKeyHash(key1);
        List<String>        objs = new ArrayList<String>();

        // for now assume value is 'true' so that we care only about the field name
        for (Map.Entry<String, String> e : vals.entrySet())
            objs.add(e.getKey());

        return objs;
    }

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
     *  * a new key is added that records lastAclUpdateTime for an Object (since
     *    that's the cache key); HASH fieldname is ObjectID and HASH fieldvalue
     *    is long epoch time of last update
     *
     *  * an ACL update will write to new key
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
    public List<String> getBySubjectObject(XUUID subject, String object) throws IOException
    {
        Map<XUUID, List<String>>  entry = (USE_CACHE) ? getCachedSubjectOps(object) : null;

        if (entry != null)
        {
            return entry.get(subject);
        }
        else
        {
            String              key2 = key2(subject, object);
            Map<String, String> vals = getKeyHash(key2);
            List<String>        ops  = new ArrayList<String>();

            // for now assume value is 'true' so that we care only about the fieldname
            for (Map.Entry<String, String> e : vals.entrySet())
                ops.add(e.getKey());

            if (USE_CACHE)
                addCachedSubjectOps(object, subject, ops);

            return ops;
        }
    }

    /**
     * Returns list of Subjects that have the permission to perform the given
     * Operation on the given Object.  This implements Query#3:  "What subjs can
     * do Op on Obj?"
     */
    public List<XUUID> getByObjectOperation(String object, Operation operation) throws IOException
    {
        String              key3 = key3(object, operation);
        Map<String, String> vals = getKeyHash(key3);
        List<XUUID>         subs = new ArrayList<XUUID>();

        // for now assume HASH value is 'true' so that we care only about the fieldname
        for (Map.Entry<String, String> e : vals.entrySet())
            subs.add(XUUID.fromString(e.getKey()));

        return subs;
    }

    /**
     * Checks if the "last updated" date kept on Cassandra for the given Object is
     * more recent than what's been loaded into the JVM.
     */
    public void checkCache(String object) throws IOException
    {
        if (USE_CACHE)
        {
            String val = getKeyHashValue(key4(), object);
            Long   db  = safeParseLong(val);                      // last time updated in the db
            Long   jvm = cacheGetBySubjObjLastAdded.get(object);  // last time updated in jvm

            // it's valid for db to be null:  this happens when a permission is removed.  in this
            // case we need to invalidate the cache

            if ((db == null) || (jvm == null) || (db > jvm.longValue()))
            {
                cacheGetBySubjObj.remove(object);
                cacheGetBySubjObjLastAdded.remove(object);
            }
        }
    }

    /**
     * Records the subject/object constraints.  A constraint means specifically that if the
     * given subject attempts to call AclLogic.addPermission for a given operation, that the
     * then-supplied subject and object meet the constraints recorded here.
     *
     * Constraints are recorded on a per-grantor (i.e., Subject) and Operation basis since those
     * two parameters are passed to AclLogic.addPermission()
     */
    public void recordConstraint(XUUID subject, Operation operation, String subjConstraint, String objConstraint) throws IOException
    {
        String  key6  = key6(subject, operation);
        boolean done = false;

        try
        {
            openTransaction();

            setKeyHashValue(key6, SUBJ_CONSTRAINT_FIELDNAME, subjConstraint);
            setKeyHashValue(key6, OBJ_CONSTRAINT_FIELDNAME,  objConstraint);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Removes the subject/object constraints
     */
    public void removeConstraint(XUUID subject, Operation operation) throws IOException
    {
        boolean done = false;

        try
        {
            openTransaction();

            deleteKey(key6(subject, operation));

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Return true if the given Subject is constrainted when attempting to add permissions
     * on the given Operation.
     */
    public boolean hasConstraints(XUUID subject, String operation) throws IOException
    {
        return getSubjectConstraint(subject, operation) != null;
    }

    /**
     * Returns the subject/object constraint that was recorded in recordConstraint().
     */
    public String getSubjectConstraint(XUUID subject, String operation) throws IOException
    {
        return getConstraint(subject, operation, SUBJ_CONSTRAINT_FIELDNAME);
    }

    public String getObjectConstraint(XUUID subject, String operation) throws IOException
    {
        return getConstraint(subject, operation, OBJ_CONSTRAINT_FIELDNAME);
    }

    private void updateObjectTimestamp(String object, String ts)
    {
        if (ts == null)
            ts = Long.toString(System.currentTimeMillis());

        setKeyHashValue(key4(), object, ts);
    }

    private String getConstraint(XUUID subject, String operation, String type) throws IOException
    {
        return getKeyHashValue(key6(subject, operation), type);
    }

    /**
     * Builds the HASH fields on key5 (which gives the list of Subjects that have been given
     * permission to perform the Operation on subject Object).
     */
    private void rebuildKey5(final List<Operation> ops) throws IOException
    {
        Map<String, String> vals = getKeyHash(key4());
        boolean             done = false;

        // use objCache key to get objects, then get subjects from key3

        try
        {
            openTransaction();

            for (Map.Entry<String, String> e : vals.entrySet())
            {
                String obj = e.getKey();

                for (Operation op : ops)
                {
                    String key5 = key5(op);

                    for (XUUID sub : getByObjectOperation(obj, op))
                        setKeyHashValue(key5, sub.toString(), TRUE_STR);
                }
            }

            // makey it as done
            setKeyHashValue(key5(""), SCAN_MARKER, TRUE_STR);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Given the ObjectID as a cache key, return the cached per-Subject list of Operations,
     * if any exists.
     */
    private Map<XUUID, List<String>> getCachedSubjectOps(String object)
    {
        return cacheGetBySubjObj.get(object);
    }

    private Long safeParseLong(String l)
    {
        if (l == null)
            return null;
        else
            return Long.parseLong(l);
    }

    /**
     * Adds the given Subject->list-of-operations to the given Object's cache entry.
     */
    private void addCachedSubjectOps(String object, XUUID subject, List<String> ops)
    {
        Map<XUUID, List<String>> entry = getCachedSubjectOps(object);

        if (entry == null)
        {
            entry = new HashMap<XUUID, List<String>>();
            cacheGetBySubjObj.put(object, entry);
        }

        // sanity check for now:  warn if we're overwriting something in the cache
        String subjStr = subject.toString();
        if (entry.containsKey(subjStr))
            System.out.println("ERROR:  addCachedSubjectOps will overwrite entry; obj = [" + object + "] subj = [" +
                               subjStr + "]");

        // update the local JVM mod time iff it's not already in there
        if (!cacheGetBySubjObjLastAdded.containsKey(object))
            cacheGetBySubjObjLastAdded.put(object, Long.valueOf(System.currentTimeMillis()));

        entry.put(subject, ops);
    }

    /**
     * Key creators.  Yes, I could save some CPU cycles by passing in Strings so
     * that I didn't need to call sub.toString twice but I would lose the type
     * safety.
     */
    private final static String key1(XUUID sub,  Operation op)  { return KEY_PFX + sub.toString() + ":" + op.getName();  }
    private final static String key2(XUUID sub,  String obj)    { return KEY_PFX + sub.toString() + ":" + obj;           }
    private final static String key3(String obj, Operation op)  { return KEY_PFX + obj            + ":" + op.getName();  }
    private final static String key4()                          { return KEY_4;                                          }
    private final static String key5(Operation op)              { return KEY_5 + op.getName();                           }
    private final static String key5(String    op)              { return KEY_5 + op;                                     }
    private final static String key6(XUUID sub,  Operation op)  { return KEY_6 + sub.toString() + ":" + op.getName();    }
    private final static String key6(XUUID sub,  String    op)  { return KEY_6 + sub.toString() + ":" + op;              }

}
