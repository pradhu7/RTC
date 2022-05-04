package com.apixio.aclsys;

import com.apixio.SysServices;
import com.apixio.XUUID;

/**
 * An AclConstraint that requires that the tested thing is a member of the
 * given UserGroup (identified by its XUUID).
 */
public class UserGroupConstraint extends AclConstraint {

    private XUUID userGroupID;

    /**
     * Package-protection.
     */
    UserGroupConstraint()
    {
    }

    UserGroupConstraint(XUUID groupID)
    {
        userGroupID = groupID;
    }

    @Override
    protected String toParams()
    {
        return userGroupID.toString();
    }

    @Override
    protected void fromParams(String params)
    {
        userGroupID = XUUID.fromString(params);
    }

    @Override
    public boolean meetsCriteria(SysServices sysServices, XUUID thing)
    {
        try
        {
            return sysServices.getUserGroupDaoExt(SysServices.UGDAO_MEMBEROF).isMemberOfGroup(userGroupID, thing);   //?? Is this still correct?
        }
        catch (Exception x)
        {
            x.printStackTrace();
            return false;
        }
    }

}
