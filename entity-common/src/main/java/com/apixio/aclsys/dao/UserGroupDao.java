package com.apixio.aclsys.dao;

import java.io.IOException;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.aclsys.entity.UserGroup;

/**
 * Interface for persistence of UserGroup structures
 */
public interface UserGroupDao
{
    /**
     * Create a group by cleaning name and making sure it's uniquely named.
     */
    public UserGroup createGroup(String name) throws IOException;
    public UserGroup createGroup(String name, String sysType) throws IOException;
    public void      createGroup(UserGroup group) throws IOException;

    /**
     * Rename a group to the new name.  Fails if the name already exists.
     */
    public UserGroup renameGroup(UserGroup curGroup, String newName) throws IOException;

    /**
     * Fix names in rowkey2 to be canonical (there was a bug in UserGroup.getCanonical where
     * it returned the cleaned-but-not-canonical name).  This needs to be done only once.
     */
    public void fixCanonicalNames() throws IOException;

    /**
     * Deletes a group.
     */
    public void deleteGroup(UserGroup group) throws IOException;

    /**
     * Return the list of all (reconstructed) UserGroups.
     */
    public List<UserGroup> getAllGroups() throws IOException;

    /**
     * Find by ID.  Mostly useful after getting groups by member.
     */
    public UserGroup findGroupByID(XUUID groupID) throws IOException;

    /**
     * Find by name.  Name doesn't need to be in canonical form.
     */
    public UserGroup findGroupByName(String name) throws IOException;

    /**
     * Adds a member to the given group.
     */
    public void addMemberToGroup(UserGroup group, XUUID member) throws IOException;
    public void addMemberToGroup(XUUID groupID, XUUID member) throws IOException;

    /**
     * Removes the member from the group; no failure reported if the member wasn't really
     * a member.
     */
    public void removeMemberFromGroup(UserGroup group, XUUID member) throws IOException;
    public void removeMemberFromGroup(XUUID groupID, XUUID member) throws IOException;
    public void removeAllMembersFromGroup(XUUID groupID) throws IOException;

    /**
     * Quick test to see if ID is a member of the given group
     */
    public boolean isMemberOfGroup(XUUID groupID, XUUID member) throws IOException;

    /**
     * Return all group members for the group.
     */
    public List<XUUID> getGroupMembers(UserGroup group) throws IOException;
    public List<XUUID> getGroupMembers(XUUID groupID) throws IOException;

    /**
     * Returns GroupIDs that the given userID is a member-of
     */
    public List<XUUID> getGroupsByMember(XUUID userID) throws IOException;

}
