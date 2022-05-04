package com.apixio.aclsys.dao.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.DaoBase;

/**
 * A UserGroup is just a uniquely named list of zero or more Users.
 *
 * The Redis keys and HASH fields are:
 *
 *  1.  [key="grplst",          HASH fieldname={GroupID},       HASH fieldvalue={JSON Object}]
 *
 *  2.  [key="bygrpname",       HASH fieldname={CanonicalName}, HASH fieldvalue={GroupID}]
 *
 *  3.  [key="bygrp."{GroupID}, HASH fieldname={UserID},        HASH fieldvalue="x"]
 *
 *  4.  [key="bymem."{UserID},  HASH fieldname={GroupID},       HASH fieldvalue="x"]
 *
 * Group names are canonicalized by trimming and lower-casing and removing
 * internal redundant whitespace.  The cleaned up name (trimmed) is
 * stored internally so that it can be presented as-entered.
 *
 * UserGroupDao supports multiple "group spaces" (basically namespaces) which are
 * completely separated from each other.  This is done by having different
 * Cassandra key prefixes.  The default one is the one that has existed from
 * the beginning, and there are 3 "extended" group spaces that are labeled
 * non-semantically (the semantics are defined by higher up code).
 */
public class RedisUserGroupDao extends RedisDao implements UserGroupDao
{
    /**
     * Exists marker value
     */
    private final static String EXISTS_STR  = "x";

    /**
     * Redis keys
     */
    private final String  rawKey1;
    private final String  rawKey2;
    private final String  rawKey3;
    private final String  rawKey4;

    public RedisUserGroupDao(DaoBase base, String prefix)  // prefix is definitely not constant
    {
        super(base);

        rawKey1 = prefix + "grplst";
        rawKey2 = prefix + "bygrpname";
        rawKey3 = prefix + "bygrp.";
        rawKey4 = prefix + "bymem.";
    }

    /**
     * Create a group by cleaning name and making sure it's uniquely named.
     */
    public UserGroup createGroup(String name) throws IOException
    {
        return createGroup(name, null);
    }

    public UserGroup createGroup(String name, String sysType) throws IOException
    {
        String    clean = cleanName(name);
        String    canon = canonicalizeName(clean);
        boolean   done  = false;
        XUUID     groupID;
        UserGroup group;

        if (findGroupByCanon(canon) != null)
            throw new IllegalArgumentException("UserGroup [" + canon + "] exists");

        groupID = XUUID.create(UserGroup.OBJTYPE);
        group   = new UserGroup(groupID, clean, canon);

        group.setGroupType(sysType);

        try
        {
            openTransaction();

            createDenormed12(group);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }

        return group;
    }

    @Override
    public void createGroup(UserGroup ug) throws IOException  // used for ACL/group migration only; remove once ACLs are only in redis
    {
        boolean done = false;

        try
        {
            openTransaction();

            createDenormed12(ug);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Rename a group to the new name.  Fails if the name already exists.
     */
    public UserGroup renameGroup(UserGroup curGroup, String newName) throws IOException
    {
        String           clean = cleanName(newName);
        String           canon = canonicalizeName(clean);
        boolean          done  = false;
        UserGroup        newGroup;

        if (findGroupByCanon(canon) != null)
            throw new IllegalArgumentException("Rename failed:  UserGroup [" + canon + "] exists");

        newGroup = new UserGroup(curGroup.getID(), clean, canon);

        try
        {
            openTransaction();

            deleteKeyHashField(key2(), curGroup.getCanonical());
            createDenormed12(newGroup);   // inserts it as a new group (but it has existing ID)

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }

        return newGroup;
    }

    /**
     * Fix names in rowkey2 to be canonical (there was a bug in UserGroup.getCanonical where
     * it returned the cleaned-but-not-canonical name).  This needs to be done only once.
     */
    public void fixCanonicalNames() throws IOException
    {
        Map<String, String> vals = getKeyHash(key2());
        boolean             done = false;

        try
        {
            openTransaction();

            for (Map.Entry<String, String> e : vals.entrySet())
            {
                String  grpName = e.getKey();
                String  canon   = canonicalizeName(grpName);

                if (!grpName.equals(canon))
                {
                    String json = e.getValue();

                    deleteKeyHashField(key2(), grpName);
                    setKeyHashValue(key2(), canon, json);
                }
            }

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Deletes a group.
     */
    public void deleteGroup(UserGroup group) throws IOException
    {
        if (group == null)
            throw new IllegalArgumentException("deleteGroup given null for group");

        String   grpID = group.getID().toString();
        String   canon = group.getCanonical();
        boolean  done  = false;

        try
        {
            openTransaction();

            deleteKeyHashField(key1(), grpID);
            deleteKeyHashField(key2(), canon);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Return the list of all (reconstructed) UserGroups.
     */
    public List<UserGroup> getAllGroups() throws IOException
    {
        Map<String, String> vals = getKeyHash(key1());
        List<UserGroup>     grps = new ArrayList<UserGroup>();

        for (Map.Entry<String, String> e : vals.entrySet())
            grps.add(UserGroup.fromJson(e.getValue()));

        return grps;
    }

    /**
     * Find by ID.  Mostly useful after getting groups by member.
     */
    public UserGroup findGroupByID(XUUID groupID) throws IOException
    {
        String val = getKeyHashValue(key1(), groupID.toString());

        if (val != null)
            return UserGroup.fromJson(val);
        else
            return null;
    }

    /**
     * Find by name.  Name doesn't need to be in canonical form.
     */
    public UserGroup findGroupByName(String name) throws IOException
    {
        return findGroupByCanon(canonicalizeName(name));
    }

    /**
     * Adds a member to the given group.
     */
    public void addMemberToGroup(UserGroup group, XUUID member) throws IOException
    {
        addMemberToGroup(group.getID(), member);
    }
    public void addMemberToGroup(XUUID groupID, XUUID member) throws IOException
    {
        String   rk3  = key3(groupID);
        String   rk4  = key4(member);
        boolean  done = false;

        try
        {
            openTransaction();

            setKeyHashValue(rk3, member.toString(),  EXISTS_STR);
            setKeyHashValue(rk4, groupID.toString(), EXISTS_STR);

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Removes the member from the group; no failure reported if the member wasn't really
     * a member.
     */
    public void removeMemberFromGroup(UserGroup group, XUUID member) throws IOException
    {
        removeMemberFromGroup(group.getID(), member);
    }
    public void removeMemberFromGroup(XUUID groupID, XUUID member) throws IOException
    {
        String   rk3  = key3(groupID);
        String   rk4  = key4(member);
        boolean  done = false;

        try
        {
            openTransaction();

            deleteKeyHashField(rk3, member.toString());
            deleteKeyHashField(rk4, groupID.toString());

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    public void removeAllMembersFromGroup(XUUID groupID) throws IOException
    {
        String groupIDstr = groupID.toString();

        // remove col in rk4 for each member, then delete rk3

        boolean  done = false;

        try
        {
            openTransaction();

            for (XUUID member : getGroupMembers(groupID))    // guts copied from createDenormed34():
                deleteKeyHashField(key4(member), groupIDstr);

            deleteKey(key3(groupID));

            commitTransaction();
            done = true;
        }
        finally
        {
            closeTransaction(done);
        }
    }

    /**
     * Quick test to see if ID is a member of the given group
     */
    public boolean isMemberOfGroup(XUUID groupID, XUUID member) throws IOException
    {
        return getKeyHashValue(key3(groupID), member.getID()) != null;
    }

    /**
     * Return all group members for the group.
     */
    public List<XUUID> getGroupMembers(UserGroup group) throws IOException
    {
        return getGroupMembers(group.getID());
    }
    public List<XUUID> getGroupMembers(XUUID groupID) throws IOException
    {
        Map<String, String> vals = getKeyHash(key3(groupID));
        List<XUUID>         mems = new ArrayList<XUUID>();

        for (Map.Entry<String, String> e : vals.entrySet())
            mems.add(XUUID.fromString(e.getKey()));

        return mems;
    }

    /**
     * Returns GroupIDs that the given userID is a member-of
     */
    public List<XUUID> getGroupsByMember(XUUID userID) throws IOException
    {
        Map<String, String> vals = getKeyHash(key4(userID));
        List<XUUID>         grps = new ArrayList<XUUID>();

        for (Map.Entry<String, String> e : vals.entrySet())
            grps.add(XUUID.fromString(e.getKey()));

        return grps;
    }

    /**
     * Create a List of Cql operations for rowkeys 1 and 2 so they can be executed
     * in a batch:  group add/rename is atomic.
     */
    private void createDenormed12(UserGroup group)
    {
        String   grpID = group.getID().toString();
        String   canon = group.getCanonical();

        setKeyHashValue(key1(), grpID, group.toJson());
        setKeyHashValue(key2(), canon, grpID);
    }

    /**
     * Find group by canonical name.
     */
    private UserGroup findGroupByCanon(String canon) throws IOException
    {
        String val = getKeyHashValue(key2(), canon);

        if (val != null)
            return findGroupByID(XUUID.fromString(val));
        else
            return null;
    }

    /**
     * Name cleanup
     */
    private String cleanName(String name)
    {
        if ((name == null) || ((name = name.trim()).length() == 0))
            throw new IllegalArgumentException("Group name [" + name + "] is invalid");

        return name;
    }

    private String canonicalizeName(String cleanName)
    {
        // remove internal spaces also:
        return cleanName.toLowerCase();
    }

    /**
     * Rowkey creators.
     */
    private final String key1()                  { return rawKey1; }
    private final String key2()                  { return rawKey2; }
    private final String key3(XUUID groupID)     { return rawKey3 + groupID.toString(); }
    private final String key4(XUUID userID)      { return rawKey4 + userID.toString();  }

}
