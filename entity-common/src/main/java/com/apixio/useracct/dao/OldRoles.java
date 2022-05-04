package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.apixio.XUUID;
import com.apixio.restbase.dao.BaseEntities;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.OldRole;

/**
 * Roles defines the persisted level operations on Role entities.
 */
public class OldRoles extends BaseEntities {

    /**
     * We need to be able to look up a role by name.
     * This is done by putting all names into a single Redis HASH with the
     * the RoleName as the field and the RoleID as the field value.
     */
    private static final String INDEX_BYNAME = "roles-x-byname";

    /**
     * Keep track of all roles.  Redis type used is a LIST whose elements
     * are the RoleID value
     */
    private static final String INDEX_ALL    = "roles-x-all";

    /**
     * Keep track of all users that have each role.  This is just the base/prefix of
     * the Redis key, with the whole Redis key having the RoleID appended to this
     * value.
     *
     * The Redis type of each of these keys is a SET, where the elements of each set
     * are the UserIDs.
     */
    protected static final String INDEX_ROLE_MEMBERS = "role-members-";

    /**
     * These are the redis keys for keeping track of email address -> userID
     *
     *  roleLookupByName:  the keyname to the redis hash with ROLENAME=ROLE_ID hashelements
     *  roleLookupAllKey:  the keyname to the redis list whose elements are all the role IDs
     */
    protected String roleLookupByName;
    protected String roleLookupAllKey;

    /**
     *
     */
    public OldRoles(DaoBase seed)
    {
        super(seed);

        // create keyname here as that can't be done until after object is fully initialized.
        roleLookupByName = super.makeKey(INDEX_BYNAME);
        roleLookupAllKey = super.makeKey(INDEX_ALL);
    }

    /**
     * Looks up a role by name and reads from Redis and returns it, if it exists.
     */
    public OldRole findRoleByName(String name)
    {
        XUUID roleID = XUUID.fromString(redisOps.hget(roleLookupByName, name), OldRole.OBJTYPE);

        if (roleID != null)
            return findRoleByID(roleID);
        else
            return null;
    }

    /**
     * Reads and returns a list of all Roles persisted in Redis.
     */
    public List<OldRole> getAllRoles()
    {
        List<OldRole> all = new ArrayList<OldRole>();

        for (String id : redisOps.lrange(roleLookupAllKey, 0, -1))
            all.add(findRoleByID(XUUID.fromString(id, OldRole.OBJTYPE)));

        return all;
    }

    /**
     * Looks for a Role instance with the given ID in Redis and if found returns
     * a restored Role instance.  Null is returned if the ID is not found.
     *
     * Only scalar data fields are filled in (no counters or lists).
     */
    public OldRole findRoleByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new OldRole(fields);
        else
            return null;
    }

    /**
     * Returns the list of (user) members of the given Role
     */
    public Set<String> getRoleMembers(OldRole role)
    {
        return redisOps.smembers(makeRoleMembersKey(role.getName()));
    }

    /**
     * Make the dyanmic redis key that keeps track of the users that have a given role.
     */
    protected String makeRoleMembersKey(String role)
    {
        return super.makeKey(INDEX_ROLE_MEMBERS + role);
    }

}
