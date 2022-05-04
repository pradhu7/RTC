package com.apixio.aclsys.dao.cass;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.apixio.XUUID;
import com.apixio.aclsys.dao.AclDao;
import com.apixio.aclsys.dao.DaoUtil;
import com.apixio.aclsys.entity.Operation;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlRowData;
import com.apixio.restbase.DaoBase;

/**
 * ACLs are managed through denormalizing the permissions into three rowkeys so that
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
 * The rowkeys/columns to support the above are:
 *
 *  1.  [rk={Subject}:{Operation}, col={Object}, val=true/false]   # "false" allows a 'deny' exception later
 *
 *  2.  [rk={Subject}:{Object}, col={Operation}, val=true/false]
 *
 *  3.  [rk={Object}:{Operation}, col={Subject}, val=true/false]
 *
 * Additionally, to help performance, there's a rowkey (#4) that records the
 * last time an ACL update occurred for a given Object in order to be able
 * to invalidate JVM caches:
 *
 *  4.  [rk="objCache", col={Object}, val={timeUpdated}]
 *
 * In order to support a delete, albeit an expensive one, the following rowkey is
 * needed:
 *
 *  5.  [rk="allsubs:"{Operation}, col={Subject}, val=x]
 *
 * Retrieving all cols for rk#1 returns the list of objects (e.g., patient orgs, or
 *  patient groups, etc.) that the Subject can perform that operation on.  E.g.,
 *  what orgs is Joe allowed to Code for?  Call this Query#1.
 *
 * Retrieving all cols for rk#2 returns the list of Operations (e.g., CanCode,
 *  CanReview) that the Subject can perform on the given object.  E.g., what
 *  operations can Joe do within the given Org?  Call this Query#2.
 *
 * Retrieving all cols for rk#3 returns the list of subjects (users or user groups)
 *  that can perform the operation on within the object.  E.g., who can CodeFor the
 *  given patient group?  Call this Query#3.
 *
 * Note that it's really just the model where each of the 3 types (Subject,
 * Operation, Object) are stored as a column value, where the rowkey for that
 * column is comprised of the other two data items.  We try to keep a fixed order of
 * things to make it simpler (order is generally Subject, Operation, Object).
 *
 * Regarding deleting of Operations, it's possible by getting all the columns of
 * rk5, then for each of those Subjects, get all Objects in rk1, then remove
 * permission on the triple <Subject, Operation, Object>.  Unfortunately the columns
 * of rk5 can have false positives:  a removePermission can't delete the column as
 * there could be permissions for that Subject/Operation combo (on a different
 * Object).
 *
 * Rowkey5 was added after code went into production and so it needed to be built from
 * enumerating other rowkeys.  There is an rk5 for each Operation so the marker used for
 * this purpose is just "allsubs:" (the string "" for the operation name) with a
 * column "scanned" to indicate things were scanned.
 *
 * ################
 *
 * The ACL system needs to deal with delegation of the ability to grant permissions.  In
 * order to do this without allowing those people to grant permissions to anyone on any
 * object, both subjects and objects can be constrained.  In order to support these
 * constraints, the following rowkeys are needed:
 *
 *  6.  [rowkey="constraint:"{Subject}:{Operation}, col=["subject","object"], val={constraint}]
 *
 * where {constraint} is an opaque string.
 */
public class CassAclDao extends CassandraDao implements AclDao
{
    /**
     * Fixed column names for rowkey6
     */
    private final static String SUBJ_CONSTRAINT_COLNAME = "subject";
    private final static String OBJ_CONSTRAINT_COLNAME  = "object";

    /**
     * Use cache for getBySubjectObject:
     */
    private static boolean  USE_CACHE = true;

    /**
     * Store the ByteBuffer form of the strings "true" and "false" once.
     *
     * This really needs to do .getBytes("UTF-8") but that causes compilation
     * problems due to UnsupportedEncodingException...
     */
    private final static ByteBuffer TRUE_BB  = ByteBuffer.wrap("true".getBytes());
    private final static ByteBuffer FALSE_BB = ByteBuffer.wrap("false".getBytes());

    /**
     * Column name to record if scan to create rowkey5 has been done.
     */
    private final static String SCAN_MARKER_COL = "rk5-scandone";

    /**
     * make sure rowkeys for ACLs are distinct from everything else
     */
    private final static String     KEY_PFX  = "acl.";
    private final static String     RK_4     = KEY_PFX + "objCache";
    private final static String     RK_5     = KEY_PFX + "allSubs:";
    private final static String     RK_6     = KEY_PFX + "constraint:";

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
    public CassAclDao(DaoBase base, String cfName)
    {
        super(base, cfName);

        if (USE_CACHE)
        {
            cacheGetBySubjObj          = new HashMap<String, Map<XUUID, List<String>>>();
            cacheGetBySubjObjLastAdded = new HashMap<String, Long>();
        }
    }

    /**
     * Return true if the rowkey that maps from Operations to the list of Subjects
     * that have been given permission to perform the Operation has been built.  This
     * test is necessary because rowkey5 was added after release to production so
     * that rowkey + columns need to be built from a key scan.
     */
    public boolean canGetSubjectsByOperation()
    {
        ByteBuffer col = cqlCrud.getColumnValue(cfName, rowkey5(""), SCAN_MARKER_COL);

        return (col != null);
    }

    /**
     * Build (or, rebuild) the contents of rowkey5 in a background thread.
     */
    public void scanForSubjectsByOperation(final List<Operation> ops)
    {
        Thread scanner = new Thread() {
                public void run()
                {
                    try
                    {
                        rebuildRowkey5(ops);
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
        String                        rk5  = rowkey5(op);
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rk5, null);
        List<XUUID>                   subs = new ArrayList<XUUID>();

        // for now assume value is 'true' so that we care only about the col name
        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
            subs.add(XUUID.fromString(e.getKey()));

        return subs;
    }

    /**
     * Delete an Operation by deleting ALL references to it.  THIS IS EXTREMELY DESTRUCTIVE!
     * All granted permissions will be removed, etc., and there is no recovery from it.
     *
     * We do the efficient thing here and just delete the entire rowkey for #1 and #3.
     * The alternative is to just enumerate subjects and objects and do a doubly-nested
     * loop and remove each permission...
     */
    public void deleteOperation(Operation op) throws IOException
    {
        // fully delete rk1,3,5; delete cols in rk2; update rk4 cols

        List<XUUID>      subs   = getSubjectsByOperation(op);
        List<CqlRowData> rows   = new ArrayList<CqlRowData>();  // to delete
        ByteBuffer       nowBB  = longToByteBuffer(System.currentTimeMillis());
        String           opName = op.getName();
        CqlRowData       rd;

        // before deleting rk1,3,5 we need to delete specific cols on rk2
        // and update rk4 cols
        for (XUUID sub : subs)
        {
            for (String obj : getBySubjectOperation(sub, op))
            {
                rd = new CqlRowData(cfName, rowkey2(sub, obj), opName);
                rd.setCqlOps(CqlRowData.CqlOps.deleteColumn);
                rows.add(rd);

                rd = new CqlRowData(cfName, rowkey4(), obj, nowBB);
                rd.setCqlOps(CqlRowData.CqlOps.insert);
                rows.add(rd);

                rows.add(makeDeleteRow(rowkey3(obj, op)));
            }

            rows.add(makeDeleteRow(rowkey1(sub, op)));
        }

        rows.add(makeDeleteRow(rowkey5(op)));

        cqlCrud.insertOrDeleteOps(rows);
    }

    private CqlRowData makeDeleteRow(String rowkey)
    {
        CqlRowData rd = new CqlRowData(cfName, rowkey);

        rd.setCqlOps(CqlRowData.CqlOps.deleteRow);

        return rd;
    }

    /*
    private void dumprows(List<CqlRowData> rows)
    {
        for (CqlRowData row : rows)
        {
            System.out.println("  [table " + row.tableName + "] key=" + row.rowkey + ", col=" + row.column);
        }
    }
    */

    /**
     * Gives the Subject the rights to perform the Operation on the Object.  This
     * consists of denormalizing into the three rowkeys since we don't know how
     * this privilege will need to be looked up in the future.
     */
    public void addPermission(XUUID subject, Operation operation, String object) throws IOException
    {
        /*
        System.out.println("================== raw dao addPermission(" + subject + ", " + operation + ", " + object + ")");

        dumprows(createDenormedRows(subject, operation, object, true));
        */

        cqlCrud.insertRows(createDenormedRows(subject, operation, object, true));
    }

    /**
     * Remove from the Subject the rights to perform the Operation on the Object.
     */
    public void removePermission(XUUID subject, Operation operation, String object) throws IOException
    {
        /*
        System.out.println("================== raw dao removePermission(" + subject + ", " + operation + ", " + object + ")");

        dumprows(createDenormedRows(subject, operation, object, false));
        */

        cqlCrud.deleteColumns(createDenormedRows(subject, operation, object, false));
    }

    /**
     * Returns list of Objects where the given Subject is allowed to perform the
     * given Operation.  This is implements Query#1.
     */
    public boolean testBySubjectOperation(XUUID subject, Operation operation, String object) throws IOException
    {
        String     rk1 = rowkey1(subject, operation);
        ByteBuffer col = cqlCrud.getColumnValue(cfName, rk1, object);

        //        System.out.println("================== raw dao testBySubjectOperation(" + subject + ", " + operation + ", " + object + "): col = " + col);

        return (col != null);
    }

    /**
     * Returns list of Objects where the given Subject is allowed to perform the
     * given Operation.  This is implements Query#1:  "What objs can Subj do Op on?"
     */
    public List<String> getBySubjectOperation(XUUID subject, Operation operation) throws IOException
    {
        String                        rk1  = rowkey1(subject, operation);
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rk1, null);
        List<String>                  objs = new ArrayList<String>();

        // for now assume value is 'true' so that we care only about the col name
        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
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
    public List<String> getBySubjectObject(XUUID subject, String object) throws IOException
    {
        Map<XUUID, List<String>>  entry = (USE_CACHE) ? getCachedSubjectOps(object) : null;

        if (entry != null)
        {
            return entry.get(subject);
        }
        else
        {
            String                        rk2  = rowkey2(subject, object);
            SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rk2, null);
            List<String>                  ops  = new ArrayList<String>();

            // for now assume value is 'true' so that we care only about the col name
            for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
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
        String                        rk3  = rowkey3(object, operation);
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rk3, null);
        List<XUUID>                   subs = new ArrayList<XUUID>();

        // for now assume column value is 'true' so that we care only about the col name
        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
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
            ByteBuffer val = cqlCrud.getColumnValue(cfName, rowkey4(), object);
            Long       db  = longFromByteBuffer(val);                 // last time updated in the db
            Long       jvm = cacheGetBySubjObjLastAdded.get(object);  // last time updated in jvm

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
        String           rk6  = rowkey6(subject, operation);
        List<CqlRowData> rows = new ArrayList<CqlRowData>();

        rows.add(new CqlRowData(cfName, rk6, SUBJ_CONSTRAINT_COLNAME, DaoUtil.toByteBuffer(subjConstraint)));
        rows.add(new CqlRowData(cfName, rk6, OBJ_CONSTRAINT_COLNAME,  DaoUtil.toByteBuffer(objConstraint)));

        cqlCrud.insertRows(rows);
    }

    /**
     * Removes the subject/object constraints
     */
    public void removeConstraint(XUUID subject, Operation operation) throws IOException
    {
        List<CqlRowData> ops = new ArrayList<CqlRowData>(2);

        ops.add(makeDeleteRow(rowkey6(subject, operation)));

        cqlCrud.insertOrDeleteOps(ops);
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
        return getConstraint(subject, operation, SUBJ_CONSTRAINT_COLNAME);
    }

    public String getObjectConstraint(XUUID subject, String operation) throws IOException
    {
        return getConstraint(subject, operation, OBJ_CONSTRAINT_COLNAME);
    }

    private String getConstraint(XUUID subject, String operation, String type) throws IOException
    {
        String     rk6 = rowkey6(subject, operation);
        ByteBuffer col = cqlCrud.getColumnValue(cfName, rk6, type);

        return (col != null) ? DaoUtil.stringFromByteBuffer(col) : null;
    }

    /**
     * Builds the columns on rowkey5 (which gives the list of Subjects that have been given
     * permission to perform the Operation on subject Object).
     */
    private void rebuildRowkey5(final List<Operation> ops) throws IOException
    {
        List<CqlRowData>              rows = new ArrayList<CqlRowData>();
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rowkey4(), null);

        // use objCache rowkey to get objects, then get subjects from rk3

        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
        {
            String obj = e.getKey();

            for (Operation op : ops)
            {
                String rk5 = rowkey5(op);

                for (XUUID sub : getByObjectOperation(obj, op))
                    rows.add(new CqlRowData(cfName, rk5, sub.toString(), TRUE_BB));
            }
        }

        // mark it as done
        rows.add(new CqlRowData(cfName, rowkey5(""), SCAN_MARKER_COL, TRUE_BB));

        cqlCrud.insertRows(rows);
    }

    /**
     * Given the ObjectID as a cache key, return the cached per-Subject list of Operations,
     * if any exists.
     */
    private Map<XUUID, List<String>> getCachedSubjectOps(String object)
    {
        return cacheGetBySubjObj.get(object);
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
     * Converts from a ByteBuffer that contains a long value to the long value.
     * Assumes the ByteBuffer was filled by longToByteBuffer.
     */
    private Long longFromByteBuffer(ByteBuffer bb)
    {
        if (bb == null)
            return null;
        else
            return bb.asLongBuffer().get();
    }

    /**
     * Converts a long value into a ByteBuffer to be written out to Cassandra.
     */
    private ByteBuffer longToByteBuffer(long v)
    {
        ByteBuffer bb = ByteBuffer.allocate(8);  // 8 = sizeof(long)

        bb.asLongBuffer().put(v);

        return bb;
    }

    /**
     * Common method to create the 3 denormalized rows and the cache column
     */
    private List<CqlRowData> createDenormedRows(XUUID subject, Operation op, String object, boolean forAddPerm) throws IOException
    {
        List<CqlRowData> rows = new ArrayList<CqlRowData>();
        String           rk1  = rowkey1(subject, op);
        String           rk2  = rowkey2(subject, object);
        String           rk3  = rowkey3(object,  op);
        String           ss   = subject.toString();

        rows.add(new CqlRowData(cfName, rk1, object,       TRUE_BB));
        rows.add(new CqlRowData(cfName, rk2, op.getName(), TRUE_BB));
        rows.add(new CqlRowData(cfName, rk3, ss,           TRUE_BB));

        rows.add(new CqlRowData(cfName, rowkey4(), object, longToByteBuffer(System.currentTimeMillis())));

        if (forAddPerm)
            rows.add(new CqlRowData(cfName, rowkey5(op), ss, TRUE_BB));

        return rows;
    }

    /**
     * Rowkey creators.  Yes, I could save some CPU cycles by passing in Strings so
     * that I didn't need to call sub.toString twice but I would lose the type
     * safety.
     */
    private final static String rowkey1(XUUID sub,  Operation op)  { return KEY_PFX + sub.toString() + ":" + op.getName();  }
    private final static String rowkey2(XUUID sub,  String obj)    { return KEY_PFX + sub.toString() + ":" + obj;           }
    private final static String rowkey3(String obj, Operation op)  { return KEY_PFX + obj            + ":" + op.getName();  }
    private final static String rowkey4()                          { return RK_4;                                           }
    private final static String rowkey5(Operation op)              { return RK_5 + op.getName();                            }
    private final static String rowkey5(String    op)              { return RK_5 + op;                                      }
    private final static String rowkey6(XUUID sub,  Operation op)  { return RK_6 + sub.toString() + ":" + op.getName();     }
    private final static String rowkey6(XUUID sub,  String    op)  { return RK_6 + sub.toString() + ":" + op;               }

}
