package com.apixio.aclsys.dao;

import com.apixio.restbase.DaoBase;

/**
 * UserGroupDaoFactory creates UserGroupDao instances, each of which has
 * its own namespace separate from all other instances.
 */
public interface UserGroupDaoFactory
{
    public UserGroupDao getDefaultUserGroupDao(DaoBase base, String cfName);
    public UserGroupDao getExtendedUserGroupDao(DaoBase base, int extensionNum, String cfName);
}
