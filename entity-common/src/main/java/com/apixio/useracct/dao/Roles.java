package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.Role;
import com.apixio.restbase.DaoBase;

/**
 * An Role is a named set set of Privileges and is owned by an OrgType.
 *
 * Users can be assigned an Role within the context of an Organization, where
 * that Organization must be of the same OrgType that the Role is.  When such
 * an assignment is done, the User is given the rights defined in the Privileges
 * of the Role.
 *
 * The method to manage the actual ACL permissions is via a UserGroup, where there
 * is one UserGroup for each combination of [Role, Organization, MemberFlag],
 * where MemberFlag is required because the set of Privileges in an Role can
 * assign different permissions based on whether or not the User is a member-of
 * (e.g., employee of) the target Organization.
 *
 * In order to make changes to the set of Privileges in an Role manageable, we
 * keep a Redis SET of UserGroups (referenced by name, which is what is used to
 * identify it via [Role, Org, MemberFlag]).  This SET is managed by this DAO.
 */
public final class Roles extends CachingBase<Role>
{
    /**
     * Redis key base for keeping track of UserGroup members of an Role:
     *
     *  key:     roles-ugmembers.{roleID}
     *  type:    SET
     *  members: {nameOfUserGroup}
     */
    final private static String KEY_UGMEMBERS_OF_ROLE = "roles-ugmembers.";

    /**
     * Creates a new OrgTypes DAO instance.
     */
    public Roles(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.ROLES, Role.OBJTYPE);
    }

    /**
     *
     */
    public List<Role> getAllRoles()
    {
        List<Role> all = new ArrayList<Role>();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
                all.add(new Role(fields));
        }

        return all;
    }

    /**
     * Looks for an Role instance with the given ID in the cache.
     */
    public Role findCachedRoleByID(XUUID id)
    {
        return fromParamSet(findInCacheByID(id));
    }

    /**
     * Looks for an Role instance with the given ID in Redis and if found returns
     * a restored Role instance.  Null is returned if the ID is not found.
     */
    public Role findRoleByID(XUUID id)
    {
        return fromParamSet(findByID(id));
    }

    /**
     * Methods for tracking of which UserGroups need to be dealt with when a privilege
     * change occurs.
     */
    public void trackRoleUserGroup(Role role, String userGroupName)
    {
        redisOps.sadd(makeMembersKey(role.getID()), userGroupName);
    }

    public void untrackRoleUserGroup(Role role, String userGroupName)
    {
        redisOps.srem(makeMembersKey(role.getID()), userGroupName);
    }

    public Set<String> getTrackedRoleUserGroups(Role role)
    {
        return redisOps.smembers(makeMembersKey(role.getID()));
    }

    /**
     * Restore an Role from persisted form.
     */
    private Role fromParamSet(ParamSet fields)
    {
        if (fields != null)
            return new Role(fields);
        else
            return null;
    }

    /**
     * Returns the redis key for the SET whose elements are the name of the UserGroups
     * that need to be dealt with when the Role's privileges change.
     */
    private String makeMembersKey(XUUID roleID)
    {
        return super.makeKey(KEY_UGMEMBERS_OF_ROLE + roleID.toString());
    }

}
