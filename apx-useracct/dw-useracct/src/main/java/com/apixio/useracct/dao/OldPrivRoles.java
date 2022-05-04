package com.apixio.useracct.dao;

import java.util.List;

import com.apixio.restbase.DaoBase;
import com.apixio.useracct.entity.OldRole;

/**
 * Roles defines the persisted level operations on Role entities.
 */
public class OldPrivRoles extends OldRoles {

    /**
     *
     */
    public OldPrivRoles(DaoBase seed)
    {
        super(seed);
    }

    /**
     * Create a new instance of a Role.
     */
    public OldRole createRole(String name, String description, List<String> assignableRoles)
    {
        OldRole role = findRoleByName(name);

        if (role != null)
        {
            throw new IllegalArgumentException("Role name [" + name + "] already used:  new role cannot be created");
        }
        else
        {
            role = new OldRole(name, description);

            if (assignableRoles != null)
                role.setRoles(assignableRoles);

            addToIndexes(role);

            update(role);
        }

        return role;
    }

    /**
     * Adds given user to the role's list of users with that role
     */
    public void addUserToRole(String role, String userID)
    {
        redisOps.sadd(makeRoleMembersKey(role), userID);
    }

    /**
     * Removes the given user from the role's list of users with that role
     */
    public void removeUserFromRole(String role, String userID)
    {
        redisOps.srem(makeRoleMembersKey(role), userID);
    }

    /**
     * Adds the role to the indexed lookups so we can find all roles.
     */
    private void addToIndexes(OldRole role)
    {
        String roleID = role.getID().toString();

        redisOps.hset(roleLookupByName, role.getName(), roleID);
        redisOps.rpush(roleLookupAllKey, roleID);
    }

}
