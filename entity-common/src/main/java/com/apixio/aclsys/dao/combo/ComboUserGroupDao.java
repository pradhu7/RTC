package com.apixio.aclsys.dao.combo;

import java.io.IOException;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.dao.cass.CassUserGroupDao;
import com.apixio.aclsys.dao.redis.RedisUserGroupDao;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.restbase.DaoBase;

/**
 *
 */
public class ComboUserGroupDao implements  UserGroupDao
{
    private UserGroupDao redisUgDao;
    private UserGroupDao cassUgDao;

    public ComboUserGroupDao(DaoBase base, String prefix, String cfName)
    {
        redisUgDao = new RedisUserGroupDao(base, prefix);
        cassUgDao  = new CassUserGroupDao(base,  prefix, cfName);
    }

    /**
     * Create a group by cleaning name and making sure it's uniquely named.
     */
    public UserGroup createGroup(String name) throws IOException
    {
        UserGroup group = redisUgDao.createGroup(name);

        // modifies

        cassUgDao.createGroup(group);
        //cassUgDao.createGroup(name);  //!! incorrect but needed for init from scratch (followed by acl migration)

        return group;
    }
    public UserGroup createGroup(String name, String sysType) throws IOException
    {
        UserGroup group = redisUgDao.createGroup(name, sysType);

        // modifies
        cassUgDao.createGroup(group);
        //cassUgDao.createGroup(name);  //!! incorrect but needed for init from scratch (followed by acl migration)

        return group;
    }
    public void createGroup(UserGroup ug) throws IOException  // used for ACL/group migration only; remove once ACLs are only in redis
    {
        throw new IOException("This should be called only during ACL migration");
    }

    /**
     * Rename a group to the new name.  Fails if the name already exists.
     */
    public UserGroup renameGroup(UserGroup curGroup, String newName) throws IOException
    {
        // modifies
        cassUgDao.renameGroup(curGroup, newName);

        return redisUgDao.renameGroup(curGroup, newName);
    }

    /**
     * Fix names in rowkey2 to be canonical (there was a bug in UserGroup.getCanonical where
     * it returned the cleaned-but-not-canonical name).  This needs to be done only once.
     */
    public void fixCanonicalNames() throws IOException
    {
        // modifies
        cassUgDao.fixCanonicalNames();
        redisUgDao.fixCanonicalNames();
    }

    /**
     * Deletes a group.
     */
    public void deleteGroup(UserGroup group) throws IOException
    {
        // modifies
        cassUgDao.deleteGroup(group);
        redisUgDao.deleteGroup(group);
    }

    /**
     * Return the list of all (reconstructed) UserGroups.
     */
    public List<UserGroup> getAllGroups() throws IOException
    {
        // doesn't modify
        return redisUgDao.getAllGroups();
    }

    /**
     * Find by ID.  Mostly useful after getting groups by member.
     */
    public UserGroup findGroupByID(XUUID groupID) throws IOException
    {
        // doesn't modify
        return redisUgDao.findGroupByID(groupID);
    }

    /**
     * Find by name.  Name doesn't need to be in canonical form.
     */
    public UserGroup findGroupByName(String name) throws IOException
    {
        // doesn't modify
        return redisUgDao.findGroupByName(name);
    }

    /**
     * Adds a member to the given group.
     */
    public void addMemberToGroup(UserGroup group, XUUID member) throws IOException
    {
        // modifies
        cassUgDao.addMemberToGroup(group, member);
        redisUgDao.addMemberToGroup(group, member);
    }
    public void addMemberToGroup(XUUID groupID, XUUID member) throws IOException
    {
        // modifies
        cassUgDao.addMemberToGroup(groupID, member);
        redisUgDao.addMemberToGroup(groupID, member);
    }

    /**
     * Removes the member from the group; no failure reported if the member wasn't really
     * a member.
     */
    public void removeMemberFromGroup(UserGroup group, XUUID member) throws IOException
    {
        // modifies
        cassUgDao.removeMemberFromGroup(group, member);
        redisUgDao.removeMemberFromGroup(group, member);
    }
    public void removeMemberFromGroup(XUUID groupID, XUUID member) throws IOException
    {
        // modifies
        cassUgDao.removeMemberFromGroup(groupID, member);
        redisUgDao.removeMemberFromGroup(groupID, member);
    }
    public void removeAllMembersFromGroup(XUUID groupID) throws IOException
    {
        // modifies
        cassUgDao.removeAllMembersFromGroup(groupID);
        redisUgDao.removeAllMembersFromGroup(groupID);
    }

    /**
     * Quick test to see if ID is a member of the given group
     */
    public boolean isMemberOfGroup(XUUID groupID, XUUID member) throws IOException
    {
        // doesn't modify
        return redisUgDao.isMemberOfGroup(groupID, member);
    }

    /**
     * Return all group members for the group.
     */
    public List<XUUID> getGroupMembers(UserGroup group) throws IOException
    {
        // doesn't modify
        return redisUgDao.getGroupMembers(group);
    }
    public List<XUUID> getGroupMembers(XUUID groupID) throws IOException
    {
        // doesn't modify
        return redisUgDao.getGroupMembers(groupID);
    }

    /**
     * Returns GroupIDs that the given userID is a member-of
     */
    public List<XUUID> getGroupsByMember(XUUID userID) throws IOException
    {
        // doesn't modify
        return redisUgDao.getGroupsByMember(userID);
    }

}
