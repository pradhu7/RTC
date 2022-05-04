package com.apixio.aclsys.dao.cass;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.apixio.XUUID;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.dao.DaoUtil;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlRowData;
import com.apixio.restbase.DaoBase;

/**
 * A UserGroup is just zero or more Users with a unique name.
 *
 * The rowkeys/columns are:
 *
 *  1.  [rk="grplst", cols={GroupID}, vals={JSON Object}]
 *
 *  2.  [rk="bygrpname", cols={CanonicalName}, vals={GroupID}]
 *
 *  3.  [rk="bygrp."{GroupID}, cols={UserID}, vals="x"]
 *
 *  4.  [rk="bymem."{UserID}, cols={GroupID}, vals="x"]
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
public class CassUserGroupDao extends CassandraDao implements UserGroupDao
{
    /**
     * Store the ByteBuffer form of the marker string.
     *
     * This really needs to do .getBytes("UTF-8") but that causes compilation
     * problems due to UnsupportedEncodingException...
     */
    private final static ByteBuffer EXISTS_BB  = ByteBuffer.wrap("x".getBytes());

    /**
     * Cassandra rowkeys
     */
    private final String  rawRk1;
    private final String  rawRk2;
    private final String  rawRk3;
    private final String  rawRk4;

    public CassUserGroupDao(DaoBase base, String prefix, String cfName)
    {
        super(base, cfName);

        rawRk1 = prefix + "grplst";
        rawRk2 = prefix + "bygrpname";
        rawRk3 = prefix + "bygrp.";
        rawRk4 = prefix + "bymem.";
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
        XUUID     groupID;
        UserGroup group;

        if (findGroupByCanon(canon) != null)
            throw new IllegalArgumentException("UserGroup [" + canon + "] exists");

        groupID = XUUID.create(UserGroup.OBJTYPE);
        group   = new UserGroup(groupID, clean, canon);

        group.setGroupType(sysType);

        cqlCrud.insertRows(createDenormed12(group));

        return group;
    }

    @Override
    public void createGroup(UserGroup ug) throws IOException  // used for Redis(master) -> Cassandra(slave) group maintenance
    {
        cqlCrud.insertRows(createDenormed12(ug));
    }

    /**
     * Rename a group to the new name.  Fails if the name already exists.
     */
    public UserGroup renameGroup(UserGroup curGroup, String newName) throws IOException
    {
        String           clean = cleanName(newName);
        String           canon = canonicalizeName(clean);
        List<CqlRowData> ops;
        CqlRowData       del;

        UserGroup newGroup;

        if (findGroupByCanon(canon) != null)
            throw new IllegalArgumentException("Rename failed:  UserGroup [" + canon + "] exists");

        newGroup = new UserGroup(curGroup.getID(), clean, canon);

        ops = createDenormed12(newGroup);   // contains operations to insert it as a new group (but it has existing ID)

        del = new CqlRowData(cfName, rowkey2(), curGroup.getCanonical());
        del.setCqlOps(CqlRowData.CqlOps.deleteColumn);

        // unfortunately Insert doesn't appear to be the default
        for (CqlRowData rd : ops)
            rd.setCqlOps(CqlRowData.CqlOps.insert);

        ops.add(del);

        // this point we have ops to add the new name to rk2, to overwrite rk1's value and
        // to delete the old rk2 column:
        cqlCrud.insertOrDeleteOps(ops);

        return newGroup;
    }

    /**
     * Fix names in rowkey2 to be canonical (there was a bug in UserGroup.getCanonical where
     * it returned the cleaned-but-not-canonical name).  This needs to be done only once.
     */
    public void fixCanonicalNames() throws IOException
    {
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rowkey2(), null);
        List<CqlRowData>              ops  = new ArrayList<>();

        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
        {
            String  grpName = e.getKey();
            String  canon   = canonicalizeName(grpName);

            if (!grpName.equals(canon))
            {
                ByteBuffer jsonb = e.getValue();
                CqlRowData del   = new CqlRowData(cfName, rowkey2(), grpName);
                CqlRowData add   = new CqlRowData(cfName, rowkey2(), canon, jsonb);

                del.setCqlOps(CqlRowData.CqlOps.deleteColumn);
                ops.add(del);

                add.setCqlOps(CqlRowData.CqlOps.insert);
                ops.add(add);
            }
        }

        cqlCrud.insertOrDeleteOps(ops);
    }

    /**
     * Deletes a group.
     */
    public void deleteGroup(UserGroup group) throws IOException
    {
        if (group == null)
            throw new IllegalArgumentException("deleteGroup given null for group");

        cqlCrud.deleteColumns(createDenormed12(group));
    }

    /**
     * Return the list of all (reconstructed) UserGroups.
     */
    public List<UserGroup> getAllGroups() throws IOException
    {
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rowkey1(), null);
        List<UserGroup>               grps = new ArrayList<UserGroup>();

        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
        {
            XUUID      grpID = XUUID.fromString(e.getKey());
            ByteBuffer jsonb = e.getValue();

            grps.add(UserGroup.fromJson(DaoUtil.stringFromByteBuffer(jsonb)));
        }

        return grps;
    }

    /**
     * Find by ID.  Mostly useful after getting groups by member.
     */
    public UserGroup findGroupByID(XUUID groupID) throws IOException
    {
        ByteBuffer val = cqlCrud.getColumnValue(cfName, rowkey1(), groupID.toString());

        if (val != null)
            return UserGroup.fromJson(DaoUtil.stringFromByteBuffer(val));
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
        cqlCrud.insertRows(createDenormed34(group.getID(), member));
    }
    public void addMemberToGroup(XUUID groupID, XUUID member) throws IOException
    {
        cqlCrud.insertRows(createDenormed34(groupID, member));
    }

    /**
     * Removes the member from the group; no failure reported if the member wasn't really
     * a member.
     */
    public void removeMemberFromGroup(UserGroup group, XUUID member) throws IOException
    {
        cqlCrud.deleteColumns(createDenormed34(group.getID(), member));
    }
    public void removeMemberFromGroup(XUUID groupID, XUUID member) throws IOException
    {
        cqlCrud.deleteColumns(createDenormed34(groupID, member));
    }
    public void removeAllMembersFromGroup(XUUID groupID) throws IOException
    {
        String groupIDstr = groupID.toString();

        // remove col in rk4 for each member, then delete rk3

        for (XUUID member : getGroupMembers(groupID))    // guts copied from createDenormed34():
            cqlCrud.deleteColumn(cfName, rowkey4(member), groupIDstr, false);

        cqlCrud.deleteRow(cfName, rowkey3(groupID), false);
    }

    /**
     * Quick test to see if ID is a member of the given group
     */
    public boolean isMemberOfGroup(XUUID groupID, XUUID member) throws IOException
    {
        return cqlCrud.getColumnValue(cfName, rowkey3(groupID), member.getID()) != null;
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
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rowkey3(groupID), null);
        List<XUUID>                   mems = new ArrayList<XUUID>();

        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
            mems.add(XUUID.fromString(e.getKey()));

        return mems;
    }

    /**
     * Returns GroupIDs that the given userID is a member-of
     */
    public List<XUUID> getGroupsByMember(XUUID userID) throws IOException
    {
        SortedMap<String, ByteBuffer> cols = cqlCrud.getColumnsMap(cfName, rowkey4(userID), null);
        List<XUUID>                   grps = new ArrayList<XUUID>();

        for (Map.Entry<String, ByteBuffer> e : cols.entrySet())
            grps.add(XUUID.fromString(e.getKey()));

        return grps;
    }

    /**
     * Create a List of Cql operations for rowkeys 1 and 2 so they can be executed
     * in a batch:  group add/rename is atomic.
     */
    private List<CqlRowData> createDenormed12(UserGroup group)
    {
        List<CqlRowData> rows  = new ArrayList<CqlRowData>();
        String           grpID = group.getID().toString();
        String           canon = group.getCanonical();

        rows.add(new CqlRowData(cfName, rowkey1(), grpID, DaoUtil.toByteBuffer(group.toJson())));
        rows.add(new CqlRowData(cfName, rowkey2(), canon, DaoUtil.toByteBuffer(grpID)));

        return rows;
    }

    /**
     * Create a List of Cql operations for rowkeys 3 and 4 so they can be executed
     * in a batch:  member add/remove is atomic.
     */
    private List<CqlRowData> createDenormed34(XUUID groupID, XUUID member)
    {
        List<CqlRowData> rows  = new ArrayList<CqlRowData>();
        String           rk3   = rowkey3(groupID);
        String           rk4   = rowkey4(member);

        rows.add(new CqlRowData(cfName, rk3, member.toString(),  EXISTS_BB));
        rows.add(new CqlRowData(cfName, rk4, groupID.toString(), EXISTS_BB));

        return rows;
    }

    /**
     * Find group by canonical name.
     */
    private UserGroup findGroupByCanon(String canon) throws IOException
    {
        ByteBuffer val = cqlCrud.getColumnValue(cfName, rowkey2(), canon);

        if (val != null)
            return findGroupByID(XUUID.fromString(DaoUtil.stringFromByteBuffer(val)));
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
    private final String rowkey1()                  { return rawRk1; }
    private final String rowkey2()                  { return rawRk2; }
    private final String rowkey3(XUUID groupID)     { return rawRk3 + groupID.toString(); }
    private final String rowkey4(XUUID userID)      { return rawRk4 + userID.toString();  }

}
