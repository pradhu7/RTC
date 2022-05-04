package com.apixio.useracct.cmdline;

import java.io.*;
import java.util.*;

import org.yaml.snakeyaml.Yaml;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.dao.*;
import com.apixio.aclsys.dao.cass.*;
import com.apixio.aclsys.dao.redis.*;
import com.apixio.aclsys.entity.*;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.config.*;
import com.apixio.useracct.dao.*;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.entity.User;

/**
 *
 */
public class MigrateAclsToRedis extends CmdlineBase
{
    private static final String cUsage = ("Usage:  MigrateAclsToRedis -c <conn-yamlfile> -r -d\n\n" +
                                          "Default is dry-run migration of ACLs and UserGroups\n\n" +
                                          "        -c:  specify yaml connection config file\n" +
                                          "        -r:  'for real':  actually make the changes in Redis\n" +
                                          "        -d:  delete the ACL and group information in Redis (allows for re-migration)\n" +
                                          "        -x:  compare ACL info between Cassandra and Redis\n" +
                                          "        -f:  fix Cassandra group information\n" +
                                          "        -g:  test group information\n" +
                                          "        -fg: fix group information; WARNING: will ALWAYS modify (ignores '-r')\n" +
                                          "\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options
    {
        String   connectionYamlFile;
        boolean  forReals;
        boolean  delete;
        boolean  compare;
        boolean  fixCass;
        boolean  testGroups;
        boolean  fixGroups;

        Options(String connectionYaml, boolean forReals, boolean delete, boolean compare, boolean fixCass, boolean testGroups, boolean fixGroups)
        {
            this.connectionYamlFile = connectionYaml;
            this.forReals           = forReals;
            this.delete             = delete;
            this.compare            = compare;
            this.fixCass            = fixCass;
            this.testGroups         = testGroups;
            this.fixGroups          = fixGroups;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile + "]");
        }
    }

    // ################################################################

    private static SysServices        sysServices;
    private static AclDao             cassAclDao;
    private static AclDao             redisAclDao;
    private static CassUserGroupDao   cassUgExt1Dao;
    private static CassUserGroupDao   cassUgExt2Dao;
    private static RedisUserGroupDao  redisUgExt1Dao;
    private static RedisUserGroupDao  redisUgExt2Dao;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        try
        {
            Options    options   = parseOptions(new ParseState(args));
            ConfigSet  config    = null;

            if ((options == null) || ((config = readConfig(options.connectionYamlFile)) == null))
            {
                usage();
                System.exit(1);
            }

            setup(config);

            if (options.forReals)
                sysServices.getRedisTransactions().begin();

            if (options.fixGroups)
                fixGroupStuff();
            else if (options.testGroups)
                testGroupStuff();
            else if (options.fixCass)
                fixCassandra(options);
            else if (options.compare)
                compareAcls(config);
            else if (options.delete)
                deleteAcls(config, options);
            else
                migrateAcls(config, options);

            if (options.forReals)
                sysServices.getRedisTransactions().commit();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            System.exit(1);
        }

        System.exit(0);  // jedis or cql creates non-daemon thread.  boo
    }

    private static void setup(ConfigSet configuration) throws Exception
    {
        String              cfName = configuration.getString("apiaclConfig.aclColumnFamilyName");
        UserGroupDaoFactory fact;

        sysServices = setupServices(configuration);

        cassAclDao  = new CassAclDao(daoBase,  cfName);
        redisAclDao = new RedisAclDao(daoBase, null);

        fact = new CassUserGroupDaoFactory();
        cassUgExt1Dao  = (CassUserGroupDao) fact.getExtendedUserGroupDao(daoBase, SysServices.UGDAO_MEMBEROF, cfName);
        cassUgExt2Dao  = (CassUserGroupDao) fact.getExtendedUserGroupDao(daoBase, SysServices.UGDAO_ACL,      cfName);

        fact = new RedisUserGroupDaoFactory();
        redisUgExt1Dao = (RedisUserGroupDao) fact.getExtendedUserGroupDao(daoBase, SysServices.UGDAO_MEMBEROF, cfName);
        redisUgExt2Dao = (RedisUserGroupDao) fact.getExtendedUserGroupDao(daoBase, SysServices.UGDAO_ACL,      cfName);
    }

    // ################################################################
    // ################################################################
    //  Group testing
    // ################################################################
    // ################################################################

    private static void testGroupStuff() throws IOException
    {
        testGroupStuff1(redisUgExt2Dao);
        testGroupStuff1(cassUgExt2Dao);

        testGroupStuff2(redisUgExt2Dao);
        testGroupStuff2(cassUgExt2Dao);
    }

    private static void testGroupStuff1(UserGroupDao dao) throws IOException
    {
        System.out.println("################################################################ testing all groups/members correctly refer to each other");

        for (UserGroup ug : dao.getAllGroups())
        {
            XUUID ugID = ug.getID();
            int   good = 0;
            int   bad  = 0;

            for (XUUID memberID : dao.getGroupMembers(ugID))
            {
                //                System.out.println("... ugID=" + ugID + ", memberID=" + memberID + ":  " + dao.getGroupsByMember(memberID));

                if (!dao.getGroupsByMember(memberID).contains(ugID))
                {
                    System.out.println("In group " + ugID + ", the user " + memberID + " is recorded as a member but groups-by-member doesn't list this group");
                    bad++;
                }
                else
                {
                    good++;
                }
            }

            if (bad > 0)
                System.out.println("Group " + ugID + ":  good=" + good + ", bad=" + bad);
        }

        for (User user : sysServices.getUsers().getAllUsers())
        {
            XUUID userID = user.getID();
            int   good = 0;
            int   bad  = 0;

            for (XUUID groupID : dao.getGroupsByMember(userID))
            {
                if (!dao.getGroupMembers(groupID).contains(userID))
                {
                    System.out.println("For user " + userID + ", the group " + groupID + " should have user as a member but doesn't");
                    bad++;
                }
                else
                {
                    good++;
                }
            }

            if (bad > 0)
                System.out.println("User " + userID + ":  good=" + good + ", bad=" + bad);
        }
    }

    private static void testGroupStuff2(UserGroupDao dao) throws IOException
    {
        int  good = 0;
        int  bad  = 0;

        System.out.println("################################################################ testing all group ID/names match");

        for (UserGroup ug : dao.getAllGroups())
        {
            UserGroup ugByName = dao.findGroupByName(ug.getName());

            if (ugByName == null)
            {
                System.out.println("Unable to find group by name " + ug.getName());
            }
            else if (!ug.getID().equals(ugByName.getID()))
            {
                List<XUUID> list0 = dao.getGroupMembers(ug);
                List<XUUID> list1 = dao.getGroupMembers(ugByName);

                System.out.println("Lookup of group " + ug.getID() + " incorrectly returned group " + ugByName.getID());

                if (difflist(list0, list1))
                {
                    dumpUsers("  by-ID-list  ", list0);
                    dumpUsers("  by-name-list", list1);
                }

                bad++;
            }
            else
            {
                good++;
            }
        }
    }

    private static void dumpUsers(String tag, List<XUUID> ids)
    {
        Users users = sysServices.getUsers();

        for (XUUID id : ids)
        {
            User user = users.findUserByID(id);

            if (user != null)
                System.out.println(tag + ": " + user.getEmailAddress());
            else
                System.out.println(tag + ": userID " + id + " not found");
        }
    }

    private static boolean difflist(List<XUUID> l1, List<XUUID> l2)
    {
        List<XUUID> l3 = new ArrayList<>(l1);

        l1.removeAll(l2);

        if (l1.size() > 0)
            return true;

        l2.removeAll(l3);

        return l2.size() > 0;
    }


    /**
     * July 31, 2017:
     *
     * fixGroupStuff fixes a bad/corrupt state of ACL data (that was in both Redis and Cassandra).
     * ACL corruption occurred when the method fixCassndra() was called (in July 2017).  That method
     * did bi-directional synchronization of group information (since minor ACL corruption that was
     * observed a week or so prior showed that neither Redis nor Cassandra seemed to have a superset
     * of data--a superset was expected in Cassandra, but wasn't observed).  Unfortunately the mechanism
     * used to recreate group information on the side that didn't have it used UserGroupDao.createGroup(UserGroup)
     * which created the group given the group from the "other side".  This would have worked fine, as the
     * test for existence used XUUID, but the problem was that the /name/ of the group already existed
     * and that .createGroup() call bypassed the name uniqueness check so the name=>XUUID association
     * that existed (in some cases) was overwritten with the new (to either Redis or Cassandra) group.
     *
     * What was left was the situation where the list of groups had an ID of, say, G_1 that had the name
     * Name1, but when Name1 was looked up (by name) to get the ID, it got G_2 (or whatever).  This
     * made it so that there really were two groups that could be used for ACL information--which one
     * was selected was determined by whether the XUUID or name was used to look it up.
     *
     * So, this fixGroupStuff method fixes that.  It requires a change (which hasn't been checked in, on
     * purpose, because we don't want to enable the destructive overwriting of name=>ID) to ComboUserGroupDao.
     * The git diff for that file/method is:
     *
     *      public void createGroup(UserGroup ug) throws IOException  // used for ACL/group migration only; remove once ACLs are only in redis
     *      {
     * -        throw new IOException("This should be called only during ACL migration");
     * +        redisUgDao.createGroup(ug);
     * +        cassUgDao.createGroup(ug);
     *      }
     *
    */

    private static void fixGroupStuff() throws IOException
    {
        RoleLogic       rl  = sysServices.getRoleLogic();
        UserGroupDao    dao = sysServices.getUserGroupDaoExt(SysServices.UGDAO_ACL);
        Set<String>     hit = new HashSet<>();
        List<UserGroup> all = dao.getAllGroups();
        Map<User, List<RoleAssignment>> assignments = new HashMap<>();

        System.out.println("################################################################ saving/temporarily unassigning role assignments for users");
        try (FileWriter out = new FileWriter("user-roles.csv"))
        {
            for (User user : sysServices.getUsers().getAllUsers())
            {
                assignments.put(user, rl.getRolesAssignedToUser(user));

                for (RoleAssignment ra : assignments.get(user))
                {
                    try
                    {
                        out.write(user.getEmailAddress().toString());
                        out.write(",");
                        out.write(user.getID().toString());
                        out.write(",");
                        out.write(ra.role.getName());
                        out.write(",");
                        out.write(ra.role.getID().toString());
                        out.write(",");
                        out.write(ra.targetID.toString());
                        out.write("\n");
                        System.out.println("  (" + user.getEmailAddress() + ") Unassigning role (" + ra.role.getName() + " " + ra.role.getRoleSet() + ") from target " + ra.targetID);
                        rl.unassignUserFromRole(null, user, ra.role, ra.targetID);
                    }
                    catch (Exception x)
                    {
                        x.printStackTrace();
                    }
                }
            }
        }

        System.out.println("################################################################ fixing groups with bad name lookups");

        Map<String,UserGroup>  inconsistent = new HashMap<>();

        // first we simply detect and record the names/ids that are inconsistent

        for (UserGroup ug : all)
        {
            UserGroup ugByName = dao.findGroupByName(ug.getName());  // hits redis

            if (!ug.getID().equals(ugByName.getID()))
            {
                System.out.println("Group w/ ID " + ug.getID() + " name " + ug.getName() + " incorrectly retrieves group " + ugByName.getID() + " when looked up by name");
                inconsistent.put(ug.getName(), ug);
            }
        }

        // delete those with the bad name; don't delete the "good" group
        for (UserGroup ug : all)
        {
            UserGroup ic = inconsistent.get(ug.getName());

            if ((ic != null) && (ic != ug))
            {
                System.out.println("Group with name " + ug.getName() + " has incorrectly overridden name; id=" + ug.getID() + "; replaced by group " + ic.getID());
                dao.removeAllMembersFromGroup(ug.getID());
                dao.deleteGroup(ug);
            }
        }

        // then recreate with good info
        for (UserGroup ug : all)
        {
            UserGroup ic = inconsistent.get(ug.getName());

            if (ic == ug)
            {
                System.out.println("Group with name " + ug.getName() + " is being created; id=" + ug.getID());
                dao.createGroup(ug);
            }
        }

        // now see if name lookup is correct
        System.out.println("################################################################ testing fixed groups");
        for (UserGroup ug : dao.getAllGroups())
        {
            UserGroup ugByName = dao.findGroupByName(ug.getName());  // hits redis

            if (ugByName == null)
            {
                System.out.println("ERROR:  can't find group by name " + ug.getName());
            }
            else if (!ug.getID().equals(ugByName.getID()))
            {
                System.out.println("ERROR:  groupID fetched by getAllGroups() " + ug.getID() + " is not same as lookup by name " + ugByName.getID());
            }
        }

        System.out.println("################################################################ reassigning saved roles");
        for (Map.Entry<User, List<RoleAssignment>> entry : assignments.entrySet())
        {
            User user = entry.getKey();

            System.out.println("User " + user.getEmailAddress());

            for (RoleAssignment ra : entry.getValue())
            {
                try
                {
                    System.out.println("  (" + user.getEmailAddress() + ") Assigning role (" + ra.role.getName() + " " + ra.role.getRoleSet() + ") from target " + ra.targetID);
                    rl.assignUserToRole(null, user, ra.role, ra.targetID);
                }
                catch (Exception x)
                {
                    x.printStackTrace();
                }
            }
        }
    }

    // ################################################################
    // ################################################################
    //  Group Fix Functionality
    // ################################################################
    // ################################################################

    private static void fixCassandra(Options options) throws IOException
    {
        // adds the redis-master group IDs to the cassandra master group list
        // (the bug was that ComboUserGroupDao.createGroup didn't use the redis
        //  groupID when creating the cassandra group):
        fixMissingGroupIDs(options.forReals, "Cassandra to Redis:  member-of", cassUgExt1Dao,  redisUgExt1Dao);
        fixMissingGroupIDs(options.forReals, "Cassandra to Redis:  ACL",       cassUgExt2Dao,  redisUgExt2Dao);
        fixMissingGroupIDs(options.forReals, "Redis to Cassandra:  member-of", redisUgExt1Dao, cassUgExt1Dao);
        fixMissingGroupIDs(options.forReals, "Redis to Cassandra:  ACL",       redisUgExt2Dao, cassUgExt2Dao);

        // copy ACLs from one to the other:
        fixMissingACLs(options.forReals, "Redis to Cassandra", redisAclDao, cassAclDao);
        fixMissingACLs(options.forReals, "Cassandra to Redis", cassAclDao,  redisAclDao);

        // copy group membership from one to the other:
        fixMembership(options.forReals, "Redis to Cassandra, member-of", redisUgExt1Dao, cassUgExt1Dao);
        fixMembership(options.forReals, "Cassandra to Redis, member-of", cassUgExt1Dao,  redisUgExt1Dao);

        fixMembership(options.forReals, "Redis to Cassandra, ACL", redisUgExt2Dao, cassUgExt2Dao);
        fixMembership(options.forReals, "Cassandra to Redis, ACL", cassUgExt2Dao,  redisUgExt2Dao);
    }

    private static void fixMissingGroupIDs(boolean forReal, String desc, UserGroupDao src, UserGroupDao dst) throws IOException
    {
        List<XUUID> srcGroupIDs = getAllGroupIDs(src);

        srcGroupIDs.removeAll(getAllGroupIDs(dst));

        if (srcGroupIDs.size() > 0)
        {
            System.out.println(desc + " is missing groups:");

            for (XUUID id : srcGroupIDs)
                System.out.println("  " + id);

            if (forReal)
            {
                Map<XUUID, UserGroup> rev = new HashMap<>();

                for (UserGroup ug : src.getAllGroups())
                    rev.put(ug.getID(), ug);

                for (XUUID id : srcGroupIDs)
                    dst.createGroup(rev.get(id));
            }
        }
    }

    private static void fixMembership(boolean forReal, String desc, UserGroupDao src, UserGroupDao dst) throws IOException
    {
        List<String> members = new ArrayList<>();

        System.out.println(">>> Adding missing membership for:  " + desc);

        for (UserGroup ug : src.getAllGroups())
        {
            XUUID groupID = ug.getID();

            for (XUUID member : src.getGroupMembers(ug))
            {
                if (!dst.isMemberOfGroup(groupID, member))
                {
                    System.out.println("Adding " + ((forReal) ? "(for real) " : "") + "missing membership [group=" + groupID + "; user=" + member + "]");

                    if (forReal)
                        dst.addMemberToGroup(groupID, member);
                }
            }
        }
    }

    private static List<XUUID> getAllGroupIDs(UserGroupDao dao) throws IOException
    {
        List<XUUID> groupIDs = new ArrayList<>();

        for (UserGroup ug : dao.getAllGroups())
            groupIDs.add(ug.getID());

        return groupIDs;
    }

    private static void fixMissingACLs(boolean forReal, String desc, AclDao src, AclDao dst) throws IOException
    {
        System.out.println(">>> Adding missing ACLs:  " + desc);

        for (Operation op : sysServices.getOperations().getAllOperations())
        {
            for (XUUID subj : src.getSubjectsByOperation(op))
            {
                for (String object : src.getBySubjectOperation(subj, op))
                {
                    if (!dst.testBySubjectOperation(subj, op, object))
                    {
                        System.out.println("Adding " + ((forReal) ? "(for real) " : "") + "missing permission [" + subj + ", " + op.getName() + ", " + object + "]");

                        if (forReal)
                            dst.addPermission(subj, op, object);
                    }
                }
            }
        }

    }

    // ################################################################
    // ################################################################
    //  Comparison Functionality
    // ################################################################
    // ################################################################

    private static void compareAcls(ConfigSet config) throws IOException
    {
        diffAcls();

        diffGroups("membership", cassUgExt1Dao, redisUgExt1Dao);
        diffGroups("ACL",        cassUgExt2Dao, redisUgExt2Dao);
    }

    private static void diffAcls() throws IOException
    {
        List<String> redis = getAclList(redisAclDao);
        List<String> cass  = getAclList(cassAclDao);

        listDiff("################ ACLs only in Redis:", redis, cass);
        listDiff("################ ACLs only in Cassandra:", cass, redis);
    }

    private static List<String> getAclList(AclDao aclDao) throws IOException
    {
        List<String> acls = new ArrayList<>();

        for (Operation op : sysServices.getOperations().getAllOperations())
        {
            for (XUUID subj : aclDao.getSubjectsByOperation(op))
            {
                for (String object : aclDao.getBySubjectOperation(subj, op))
                {
                    acls.add(op.getName() + " : " + subj + " " + object);
                }
            }
        }

        return acls;
    }

    private static void diffGroups(String type, UserGroupDao cass, UserGroupDao redis) throws IOException
    {
        List<String> redisMembers = getGroupMembers(redis);
        List<String> cassMembers  = getGroupMembers(cass);

        listDiff("################ Type " + type + " members only in Redis:",     redisMembers, cassMembers);
        listDiff("################ Type " + type + " members only in Cassandra:", cassMembers,  redisMembers);
    }

    private static List<String> getGroupMembers(UserGroupDao dao) throws IOException
    {
        List<String> members = new ArrayList<>();

        for (UserGroup ug : dao.getAllGroups())
        {
            String name  = ug.getName();

            for (String member : xuuidToString(dao.getGroupMembers(ug)))
                members.add(name + " : " + member);

        }

        return members;
    }

    private static List<String> xuuidToString(List<XUUID> xuuids)
    {
        List<String> l = new ArrayList<>(xuuids.size());

        for (XUUID x : xuuids)
            l.add(x.getID());

        return l;
    }

    private static void listDiff(String header, List<String> one, List<String> two)
    {
        List<String>  dup = new ArrayList<String>(one);

        dup.removeAll(two);

        dumpList(header, dup);
    }

    private static void dumpList(String header, List<String> list)
    {
        System.out.println(header);

        for (String item : list)
            System.out.println(item);

        System.out.println("\n\n");
    }

    // ################################################################
    // ################################################################
    //  ACL Deletion Functionality
    // ################################################################
    // ################################################################

    /**
     *
     */
    private static void deleteAcls(ConfigSet config, Options options) throws IOException
    {
        String constraint;

        System.out.println("\n\n\nDeleting ACLs (" + ((options.forReals) ? "for real" : "dry run") + "):");

        for (Operation op : sysServices.getOperations().getAllOperations())
        {
            for (XUUID subj : redisAclDao.getSubjectsByOperation(op))
            {
                for (String object : redisAclDao.getBySubjectOperation(subj, op))
                {
                    if (options.forReals)
                    {
                        redisAclDao.removePermission(subj, op, object);
                    }
                    else
                    {
                        System.out.println("redisAclDao.removePermission(" + subj + ", " + op.getName() + ", " + object + ")");
                    }
                }

                if ((constraint = redisAclDao.getSubjectConstraint(subj, op.getName())) != null)
                {
                    if (options.forReals)
                    {
                        redisAclDao.removeConstraint(subj, op);
                    }
                    else
                    {
                        System.out.println("redisAclDao.removeConstraint(" +
                                           subj + ", " +
                                           op   + ", " +
                                           constraint + ", " +
                                           cassAclDao.getObjectConstraint(subj, op.getName()) +
                                           ")");
                    }
                }
            }
        }

        deleteGroups(redisUgExt1Dao, "membership groups", options.forReals);
        deleteGroups(redisUgExt2Dao, "ACL groups",        options.forReals);
    }

    // ################################################################
    // ################################################################
    //  ACL Migration Functionality
    // ################################################################
    // ################################################################

    /**
     *
     */
    private static void migrateAcls(ConfigSet config, Options options) throws IOException
    {
        String constraint;

        System.out.println("\n\nMigrating ACLs (" + ((options.forReals) ? "for real" : "dry run") + "):");

        for (Operation op : sysServices.getOperations().getAllOperations())
        {
            List<XUUID> subjects = cassAclDao.getSubjectsByOperation(op);

            System.out.println("\n## op=" + op.getName() + " has " + subjects.size() + " subjects w/ some ACL permission:");

            for (XUUID subj : subjects)
            {
                List<String> objsBySubjOp = cassAclDao.getBySubjectOperation(subj, op);

                System.out.println("\n    objects-by-subjobj (" + subj + ", " + op.getName() + ") has " + objsBySubjOp.size() + " elements:");

                for (String object : cassAclDao.getBySubjectOperation(subj, op))
                {
                    System.out.println("      redisAclDao.addPermission(" + subj + ", " + op.getName() + ", " + object + ")");

                    if (options.forReals)
                        redisAclDao.addPermission(subj, op, object);
                }

                if ((constraint = cassAclDao.getSubjectConstraint(subj, op.getName())) != null)
                {
                    System.out.println("redisAclDao.recordConstraint(" +
                                       subj + ", " +
                                       op   + ", " +
                                       constraint + ", " +
                                       cassAclDao.getObjectConstraint(subj, op.getName()) +
                                       ")");

                    if (options.forReals)
                        redisAclDao.recordConstraint(subj, op, constraint, cassAclDao.getObjectConstraint(subj, op.getName()));
                }
            }
        }

        migrateGroups(cassUgExt1Dao, redisUgExt1Dao, "membership groups", options.forReals);
        migrateGroups(cassUgExt2Dao, redisUgExt2Dao, "ACL groups",        options.forReals);
    }

    private static void deleteGroups(RedisUserGroupDao redisDao, String type, boolean forReals) throws IOException
    {
        System.out.println("\n\nDeleting UserGroups type " + type + "(" + ((forReals) ? "for real" : "dry run") + "):");

        for (UserGroup ug : redisDao.getAllGroups())
        {
            System.out.println("Delete UserGroup [" + ug + "] in Redis");

            for (XUUID mem : redisDao.getGroupMembers(ug))
            {
                System.out.println("redisUgExt1Dao.removeMemberFromGroup(" + ug.getName() + ", " + mem + ")");

                if (forReals)
                    redisDao.removeMemberFromGroup(ug, mem);
            }

            if (forReals)
                redisDao.deleteGroup(ug);
        }
    }

    private static void migrateGroups(UserGroupDao cassDao, RedisUserGroupDao redisDao, String type, boolean forReals) throws IOException
    {
        System.out.println("\n\nMigrating UserGroups type " + type + " (" + ((forReals) ? "for real" : "dry run") + "):");

        for (UserGroup ug : cassDao.getAllGroups())
        {
            UserGroup rg = null;

            System.out.println("Create UserGroup [" + ug + "] in Redis");

            if (forReals)
                redisDao.createGroup(ug);

            for (XUUID mem : cassDao.getGroupMembers(ug))
            {
                System.out.println("redisUgExt1Dao.addMemberToGroup(" + ug.getName() + ", " + mem + ")");

                if (forReals)
                    redisDao.addMemberToGroup(ug, mem);
            }
        }
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");

        if (connection == null)
            return null;
        else
            return new Options(connection,
                               ps.getOptionFlag("-r"),
                               ps.getOptionFlag("-d"),
                               ps.getOptionFlag("-x"),
                               ps.getOptionFlag("-f"),
                               ps.getOptionFlag("-g"),
                               ps.getOptionFlag("-fg"));
    }

    /**
     * Load configuration from .yaml file into generic Map.
     */
    private static Map<String, Object> loadYaml(String filepath) throws Exception
    {
        InputStream input = new FileInputStream(new File(filepath));
        Yaml        yaml  = new Yaml();
        Object      data = yaml.load(input);

        input.close();

        return (Map<String, Object>) data;
    }

    private static boolean isEmpty(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
