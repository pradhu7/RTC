package com.apixio.aclsys.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.SysServices;
import com.apixio.aclsys.AclConstraint;
import com.apixio.aclsys.dao.AclDao;
import com.apixio.aclsys.entity.Operation;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.util.TimeoutCache;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.entity.OldRole;
import com.apixio.useracct.entity.User;

/**
 * AclLogic provides application-level functionality for granting and revoking
 * access to objects.
 */
public class AclLogic extends LogicBase<SysServices> {

    /**
     * The hardcoded names of the Operations that are used as part of the
     * "assignable permissions" code/model.  If a subject is granted
     * permission on these Operations then that allows the subject to
     * delegate to some degree the real operation.
     */
    private static final String OP_ADDPERMISSION   = "addPermission";
    private static final String OP_GRANTPERMISSION = "grantPermission";

    /**
     * !!! This is a BIG HACK.  The problem is that the meta-permissions were
     * added and then role-based access control was created and the two are in
     * conflict currently.  The need is for a non-ROOT user to be able to somehow
     * add a permission (since during role assignment a userGroup might be created
     * and when it's created permissions are added to it).  The cheap way is to
     * allow the RBAC code to make a privileged call to add the permission and
     * so this "caller XUUID" (which will never be passed as a real UserID)
     * is specified instead.
     */
    public final static XUUID PRIVILEGED_CALLER_XUUID = XUUID.fromString("PRIV_00000000-1111-2222-3333-444444444444");

    /**
     * This is Object name that indicates a permission on any object (really
     * just the wildcard concept).
     */
    public static final String ANY_OBJECT = "*";

    /**
     * As part of the "assignable permissions" model we need to record Operation
     * names as Objects (ObjectID, really), and given the intentionally free-from
     * ObjectID model, we might have a collision.  This prefix is used to make
     * sure we have no collision in that we don't allow ObjectIDs passed in to
     * addPermission to have this prefix, and all Operation names that we pass
     * in as a ObjectID we add this prefix.
     */
    private static final String OP_AS_OBJ_PREFIX = "<operation:";
    private static final String OP_AS_OBJ_SUFFIX = ">";

    /**
     * The various types of ACL management failure.
     */
    public enum FailureType {
        /**
         * When assigning, removing, testing ACLs:
         */
        NO_SUCH_OPERATION
    }

    /**
     * If role operations fail they will throw an exception of this class.
     */
    public static class AclException extends BaseException {

        private FailureType failureType;

        public AclException(FailureType failureType)
        {
            this.failureType = failureType;
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * Hold on to AclDao for efficiency
     */
    protected AclDao aclDao;

    /**
     * Cache required/known Operations.  Hacky to some degree because sys initialization
     * doesn't split object-construction from post-new initialization, so we keep track
     * of needed initialization here.
     */
    private Operation opAddPermission;
    private Operation opGrantPermission;
    private boolean   opInit;

    /**
     * Simple caching support for quick hasPermission test
     */
    private static long                    HP_CACHETIMEOUT  = 5000L;   // default cache timeout of 5 seconds
    private long                           hpTimeout;                  // "hp" == hasPermission; cache is active IFF timeout > 0
    private TimeoutCache<String, Boolean>  hpCache;

    /**
     * Constructor.
     */
    public AclLogic(SysServices sysServices)
    {
        super(sysServices);

        this.aclDao = sysServices.getAclDao();

        if ((this.aclDao != null) && !this.aclDao.canGetSubjectsByOperation())
            this.aclDao.scanForSubjectsByOperation(sysServices.getOperationLogic().getAllOperations());  // asynchronous

        createHpCache(HP_CACHETIMEOUT);
    }

    /**
     * Sets the timeout of the "hasPermission" cache.  Any positive timeout will enabled
     * caching and any non-positive timeout will disable it.  Changing the timeout will
     * flush the cache.
     */
    public void setHasPermissionCacheTimeout(long timeout)
    {
        createHpCache(timeout);
    }

    /**
     * WARNING!  This should be called ONLY ONCE in the entire lifetime of the system/data:  when
     * we need to bootstrap the ROOT user group so that ROOT-roled users can add permisions.  This
     * is exposed because it needs access to the string constants OP_{ADD,GRANT}PERMISSION.
     */
    public void addRootPermission() throws IOException
    {
        OldRole rootRole = services.getOldRoleLogic().getRole("ROOT");  // this hardcoding of "ROOT" is needed as roles are dynamically defined

        if (rootRole != null)
        {
            UserGroup rootGroup = services.getUserGroupLogicExt(SysServices.UGDAO_ACL).findRoleGroup(rootRole);

            if (rootGroup != null)
            {
                checkInit();

                aclDao.addPermission(rootGroup.getID(), opAddPermission,   ANY_OBJECT);
                aclDao.addPermission(rootGroup.getID(), opGrantPermission, ANY_OBJECT);
            }
            else
            {
                System.out.println("ERROR:  addRootPermission couldn't find ROOT UserGroup");
            }
        }
        else
        {
            System.out.println("ERROR:  addRootPermission couldn't find ROOT role");
        }
    }

    /**
     * Create as necessary the hasPermission cache with the given timeout.
     */
    private void createHpCache(long timeout)
    {
        if (timeout <= 0L)
        {
            hpTimeout = 0L;
            hpCache   = null;
        }
        else if (hpTimeout != timeout)
        {
            hpTimeout = timeout;
            hpCache   = new TimeoutCache<String, Boolean>(hpTimeout);
        }
    }

    // ################################################################
    //  Useful debugging code
    // ################################################################

    public String dumpAclPermissions() throws IOException
    {
        com.apixio.aclsys.dao.Operations ops = services.getOperations();
        StringBuilder                    sb  = new StringBuilder();

        sb.append("\n\n");
        sb.append(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> START Low-level ACL permissions\n");

        // operations lead to subjects, and both together lead us to permissions...
        for (Operation op : ops.getAllOperations())
        {
            String  opName = op.getName();
            boolean logOp  = false;

            for (XUUID subject : aclDao.getSubjectsByOperation(op))
            {
                List<Permission> perms  = getBySubject(subject);
                boolean          logSub = false;

                if (perms.size() > 0)  // it's expected that we have empty perms list since AclDao's rowkey5 has limitations...
                {
                    for (Permission perm : perms)
                    {
                        if (perm.operation.equals(opName))
                        {
                            // doing it this way avoids putting out blank info
                            if (!logOp)
                            {
                                sb.append(com.apixio.DebugUtil.subargs("Permissions for operation name={} id={}\n", opName, op.getID()));
                                logOp = true;
                            }
                            if (!logSub)
                            {
                                sb.append(com.apixio.DebugUtil.subargs("  subjectID={}\n", subject));
                                logSub = true;
                            }

                            sb.append(com.apixio.DebugUtil.subargs("    object={}\n", perm.object));
                        }
                    }

                    if (logSub)
                        sb.append("\n");
                }
            }

            if (logOp)
                sb.append("\n");
        }

        sb.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< END Low-level ACL permissions\n\n");

        return sb.toString();
    }

    // ################################################################
    // ################################################################

    /**
     * Grants rights for the given subject to perform the given operation on the
     * given object.  Note that the object is identified by a String rather than
     * an XUUID in order to allow entities outside the new system to have
     * access control capabilities.
     */
    public boolean addPermission(XUUID caller, XUUID subject, String operationName, String object) throws IOException
    {
        boolean added = false;

        if (object.startsWith(OP_AS_OBJ_PREFIX))
            throw new IllegalArgumentException("Object identifier can't start with " + OP_AS_OBJ_PREFIX);

        if (caller.equals(PRIVILEGED_CALLER_XUUID) || hasPermission(caller, OP_ADDPERMISSION, operationAsObject(operationName)))
        {
            // if there are constraints on this permission, are they met?
            if (!hasConstraints(caller, operationName) || meetsConstraints(caller, operationName, subject, object))
            {
                //                com.apixio.DebugUtil.LOG("addPermission({}, {}, {})", subject.toString(), operationName, object);

                // add the tuple [subject, operation, object]
                //                com.apixio.DebugUtil.LOG("addPermission subject={} operation={} object={}", subject.toString(), operationName, object);
                aclDao.addPermission(subject, getOperation(operationName), object);

                if (cacheEnabled())
                    hpCache.remove(makeCacheKey(subject, operationName, object));

                added = true;
            }
        }
        else
        {
            throw new IllegalStateException("No permission to addpermission");
        }

        return added;
    }

    /**
     * Grants the subject the right to delegate the given Operation to another User (on a given Object), provided
     * that the constraints are met.
     */
    public boolean grantAddPermission(XUUID caller, XUUID subject, String operationName, AclConstraint subjConstraint, AclConstraint objConstraint) throws IOException
    {
        boolean added = false;

        if ((subjConstraint == null) || (objConstraint == null))
            throw new IllegalArgumentException("addAssignablePermission requires non-null subject and object constraints");

        //        System.out.println("  addAssignablePermission(" + caller + ", " + subject + ", " + operationName + ")");
        //        System.out.println("  ...:  hasPermission(" + caller + ", " + OP_GRANTPERMISSION + ", " + operationName + ") => " + hasPermission(caller, OP_GRANTPERMISSION, operationName));

        if (hasPermission(caller, OP_GRANTPERMISSION, operationAsObject(operationName)))  // does caller have permission generally for adding permissions?
        {
            checkInit();

            // if there are constraints on this permission, are they met?
            //!!TODO/figure out:            if (!hasConstraints(caller, operationName) || meetsConstraints(caller, operationName, subject, object))
            {
                // this next line is a key one:  adding this permission makes is so that the
                // hasPermission() test in the above addPermission() method succeeds:
                aclDao.addPermission(subject, opAddPermission, operationAsObject(operationName));
                aclDao.recordConstraint(subject, getOperation(operationName), subjConstraint.toPersisted(), objConstraint.toPersisted());
                added = true;
            }
        }

        return added;
    }

    /**
     * Removes from the subject the right to delegate the given Operation to another User (on a given Object), provided
     * that the constraints are met.  Basically undoes what grantAddPermission does.
     */
    public boolean removeAddPermission(XUUID caller, XUUID subject, String operationName) throws IOException
    {
        boolean removed = false;

        if (hasPermission(caller, OP_GRANTPERMISSION, operationAsObject(operationName)))  // does caller have permission generally for adding permissions?
        {
            checkInit();

            // if there are constraints on this permission, are they met?
            //!!TODO/figure out:            if (!hasConstraints(caller, operationName) || meetsConstraints(caller, operationName, subject, object))
            {
                aclDao.removePermission(subject, opAddPermission, operationAsObject(operationName));
                aclDao.removeConstraint(subject, getOperation(operationName));
                removed = true;
            }
        }

        return removed;
    }

    /**
     * Unimplemented forward-thinking method that will (should!) allow the delegation of the right
     * to call grantAddPermission.  This detail of the overall model has not been fully designed...
     */
    public boolean grantGrantPermission(XUUID caller, XUUID subject, String operationName, AclConstraint subjConstraint, AclConstraint objConstraint) throws IOException
    {
        // todo:  grant the right to grant the right... same as grantAddPermission but adds OP_GRANTPERMISSION
        return false;
    }

    /**
     * Removes rights for the given subject to perform the given operation on the
     * given object.
     */
    public boolean removePermission(XUUID caller, XUUID subject, String operationName, String object) throws IOException
    {
        boolean removed = false;

        if (hasPermission(caller, OP_ADDPERMISSION, operationAsObject(operationName)))  // does caller have permission generally for managing permissions?
        {
            //            com.apixio.DebugUtil.LOG("removePermission({}, {}, {})", subject.toString(), operationName, object);
            aclDao.removePermission(subject, getOperation(operationName), object);
            removed = true;

            if (cacheEnabled())
                hpCache.remove(makeCacheKey(subject, operationName, object));
        }

        return removed;
    }

    /**
     * Returns true iff the given subject (user or user group) has rights to perform
     * the given operation on the given object.  If the subject is a User then the
     * list of groups that the user is a member of is also included in the permissions
     * test.
     */
    public boolean hasPermission(XUUID subject, String operationName, String object) throws IOException
    {
        boolean hpCacheEnabled = cacheEnabled();
        boolean rawAllowed     = false;
        Boolean hpAllowed      = null;
        String  cacheKey       = null;

        if (hpCacheEnabled)
        {
            cacheKey  = makeCacheKey(subject, operationName, object);
            hpAllowed = hpCache.get(cacheKey);
            //            System.out.println("      SFM: hasPermission cache " + Boolean.valueOf(hpAllowed != null).toString() + " for " + cacheKey);
        }

        if (hpAllowed == null)
            rawAllowed = hasRawPermission(subject, operationName, object) || hasRawPermission(subject, operationName, ANY_OBJECT);

        if (hpCacheEnabled && (hpAllowed == null))  // put it only if it's not already in cache otherwise high activity will always keep it in cache
            hpCache.put(cacheKey, rawAllowed);

        return rawAllowed || ((hpAllowed != null) && hpAllowed.booleanValue());
    }

    private boolean cacheEnabled()
    {
        return (hpTimeout > 0L);
    }

    private String makeCacheKey(XUUID subject, String operationName, String object)
    {
        return subject.toString() + ":" + operationName + ":" + object;
    }

    private boolean hasRawPermission(XUUID subject, String operationName, String object) throws IOException
    {
        String type = subject.getType();

        //        System.out.println("++++ hasRawPermission(" + subject + ", " + operationName + ", " + object + "); type=" + type);

        if (type.equals(User.OBJTYPE))
        {
            /* logic:
              1 get list of UserGroups 'subject' is a member of
              2 get list of Objects that can do 'operationName' on 'object':
              3 test if any in list of UserGroups is in #2

              The above will work fine for reasonable list sizes; in other words
              it will not be optimal if users are in lots of groups or if lots
              of subjects are given rights to an object.  It should be fine for
              numbers up to, say, a few hundred.  Things probably start
              to get unmanageable from the administrator perspective if users are
              in that many groups or if that many subjects are granted access to
              an object...
            */

            //            System.out.println("     hasRawPermission groupsByMember(" + subject + ") => " + services.getUserGroupDaoExt(SysServices.UGDAO_ACL).getGroupsByMember(subject));
            List<XUUID>  groups  = services.getUserGroupDaoExt(SysServices.UGDAO_ACL).getGroupsByMember(subject);   // hits cassandra one time
            List<XUUID>  allowed = services.getAclDao().getByObjectOperation(object, getOperation(operationName));  // hits cassandra one time

            // include subject in intersection test also
            groups.add(subject);

            //            System.out.println("     hasRawPermission: allowed = " + allowed);
            //            System.out.println("     hasRawPermission: subject in groups = " + groups);

            for (XUUID ele : groups)
            {
                if (allowed.contains(ele))
                {
                    //                    System.out.println("     hasRawPermission: intersection of allowed and groups:  returning true");
                    return true;
                }
            }

            //            System.out.println("     hasRawPermission: no intersection of allowed and groups:  returning false");
            return false;
        }
        else
        {
            return aclDao.testBySubjectOperation(subject, getOperation(operationName), object);
        }
    }

    /**
     * Returns list of Subjects that have the permission to perform the given
     * Operation on the given Object.  This implements Query#3:  "What subjs can
     * do Op on Obj?"
     */
    public List<UserGroup> getByObjectOperation(String object, String operationName) throws IOException
    {
        List<UserGroup> allowed = new ArrayList<UserGroup>();

        for (XUUID id : aclDao.getByObjectOperation(object, getOperation(operationName)))
        {
            String type = id.getType();

            if (type.equals(UserGroup.OBJTYPE))
            {
                allowed.add(services.getUserGroupDaoExt(SysServices.UGDAO_ACL).findGroupByID(id));
            }
            else if (type.equals(User.OBJTYPE))
            {
                //!! finish:                allowed.add(services.getUsers().findUserByID(id));
            }
        }

        return allowed;
    }

    /**
     * Returns a list of <Operation,Object> tuples for the given Subject where each
     * tuple means that the Subject can perform that Operation on that Object.  This
     * tuple is called a Permission (so, you can read it as, the Subject has this
     * list of Permissions).
     *
     * This is NOT a low-cost method as it needs to make a lot of calls to Cassandra.
     */
    public List<Permission> getBySubject(XUUID subject) throws IOException
    {
        // loop through all Operations and query for all cols of the rowkeys
        //  {Subject}:{Operation}.  This is the only way to do it as this subsystem
        //  doesn't keep track of ObjectIDs in any central list (but does for
        //  Operations).

        List<Permission>  perms = new ArrayList<Permission>();
        OperationLogic    opLog = services.getOperationLogic();

        for (Operation op : opLog.getAllOperations())   // MUST call getAllOperations method to aid with deleting ops/types
        {
            for (String obj : aclDao.getBySubjectOperation(subject, op))
            {
                perms.add(new Permission(op.getName(), obj));
            }
        }

        return perms;
    }

    /**
     * Returns a list of Objects that the given Subject has permission to perform
     * the given Operation on.  For example, getBySubject(rootXUUID, "ManageUser")
     * will return all objects that rootXUUID can ManageUser on.
     */
    public List<String> getBySubjectOperation(XUUID subject, String operationName, boolean includeGroups) throws IOException
    {
        List<String>      objects = new ArrayList<>();
        OperationLogic    opLog   = services.getOperationLogic();
        List<XUUID>       subs    = new ArrayList<>();
        Operation         op      = opLog.getOperation(operationName);

        subs.add(subject);

        if (includeGroups && subject.getType().equals(User.OBJTYPE))
            subs.addAll(services.getUserGroupDaoExt(SysServices.UGDAO_ACL).getGroupsByMember(subject));

        for (XUUID sub : subs)
            objects.addAll(aclDao.getBySubjectOperation(sub, op));

        return objects;
    }

    /**
     * Returns true iff the given subject (user or user group) has rights to perform
     * the given access on the given object.  If the subject is a User then the
     * list of groups that the user is a member of is also included in the permissions
     * test.
     */
    public boolean hasAccess(XUUID subject, String accessTypeName, String object) throws IOException
    {
        /*
          If the subject is not a user (meaning it can't be in a UserGroup), then:

            1 get full list of Operations subject is allowed to perform on object
            2 expand/flatten those ops into access types
            3 intersect list from #2 with accessTypeName parameter

         but if the subject can be in a UserGroup, then it gets ugly as a lookup
         needs to be done on every group the subject is a member of.
        */

        List<XUUID>  subjects;

        if (subject.getType().equals(User.OBJTYPE))
            subjects = services.getUserGroupDaoExt(SysServices.UGDAO_ACL).getGroupsByMember(subject);
        else
            subjects = new ArrayList<XUUID>(2);

        subjects.add(subject);

        try
        {
            // since AclDao provides caching of the getBySubjectObject() results we
            // need to check here if the cache entry should be invalidated
            aclDao.checkCache(object);

            for (XUUID subj : subjects)
            {
                List<String>   opNames  = aclDao.getBySubjectObject(subj, object);  // hits cassandra once
                OperationLogic opLog    = services.getOperationLogic();

                if (opNames != null)
                {
                    for (String opName : opNames)
                    {
                        Operation op = opLog.getOperation(opName);  // cached lookup

                        if (op != null)
                        {
                            if (op.getAccessTypes().contains(accessTypeName))
                                return true;
                        }
                    }
                }
            }

            return false;
        }
        catch (Exception x)
        {
            x.printStackTrace();

            return false;
        }
    }

    /**
     * Looks up the operation by its name, throwing an exception if there isn't such an Operation.
     */
    Operation getOperation(String name)
    {
        Operation op = services.getOperationLogic().getOperation(name);

        if (op == null)
            throw new AclException(FailureType.NO_SUCH_OPERATION).description(
                "Operation {} name not found in system", name);

        return op;
    }

    /**
     * Cache Operations if not already.  We're assuming that synchronized overhead is
     * less than making calls to getOperation each time we need an Operation from a name.
     */
    synchronized private void checkInit()
    {
        if (!opInit)
        {
            opAddPermission   = getOperation(OP_ADDPERMISSION);
            opGrantPermission = getOperation(OP_GRANTPERMISSION);

            opInit = true;
        }
    }

    /**
     * In order to distinguish between app-supplied/defined Objects and what the MetaACLs code
     * records as Objects, we prefix the Operation name before treating it as a generic Object.
     */
    private String operationAsObject(String operationName)
    {
        return OP_AS_OBJ_PREFIX + operationName + OP_AS_OBJ_SUFFIX;
    }

    /**
     * Returns true if there are subject/object constraints on the subject for
     * adding permissions for the given operation.
     */
    private boolean hasConstraints(XUUID subject, String operation) throws IOException
    {
        return aclDao.hasConstraints(subject, operation);
    }

    /**
     * Tests if the constraints imposed on the caller for addPermission on the operation have
     * been met.
     */
    private boolean meetsConstraints(XUUID caller, String operation, XUUID subject, String object) throws IOException
    {
        AclConstraint subjConstraint = AclConstraint.fromPersisted(aclDao.getSubjectConstraint(caller, operation));
        AclConstraint objConstraint  = AclConstraint.fromPersisted(aclDao.getObjectConstraint(caller,  operation));

        // constraints should never be null as we check for null in addAssignablePermission

        // Perhaps not the best design choice to require XUUID for object constraints as it's
        // inconsistent with just a plain ol' string for the object as part of a permission tuple...
        // but, that's the way it is right now, so protect against a non-XUUID
        try
        {
            XUUID objX = XUUID.fromString(object);

            return (subjConstraint.meetsCriteria(services, subject) && objConstraint.meetsCriteria(services, objX));
        }
        catch (Exception x)
        {
            x.printStackTrace();
            // we'll define a poorly-formed XUUID as not meeting constraints...
            return false;
        }

    }

}
