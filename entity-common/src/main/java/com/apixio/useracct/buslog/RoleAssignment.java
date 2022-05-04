package com.apixio.useracct.buslog;

import com.apixio.XUUID;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.User;

/**
 * RoleAssignment records the info about a user being assigned a role within the context of a target.
 */
public class RoleAssignment {

    public User   user;
    public XUUID  targetID;    // organization or project ID
    public Role   role;

    public RoleAssignment(User user, XUUID targetID, Role role)
    {
        this.user     = user;
        this.targetID = targetID;
        this.role     = role;
    }
}
