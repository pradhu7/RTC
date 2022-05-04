package com.apixio.aclsys.dao.cass;

import com.apixio.aclsys.dao.BaseUserGroupDaoFactory;
import com.apixio.aclsys.dao.UserGroupDaoFactory;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.restbase.DaoBase;

/**
 * A UserGroupDaoFactory is
 */
public class CassUserGroupDaoFactory extends BaseUserGroupDaoFactory implements UserGroupDaoFactory
{

    public UserGroupDao getDefaultUserGroupDao(DaoBase base, String cfName)
    {
        return new CassUserGroupDao(base, DEFAULT_KEY_PFX, cfName);
    }

    public UserGroupDao getExtendedUserGroupDao(DaoBase base, int extensionNum, String cfName)
    {
        if ((extensionNum < 0) || (extensionNum >= MAX_EXTENDED_KEYS))
            throw new IllegalArgumentException("Request for extended UserGroupDao outside allowed range of [0, " + MAX_EXTENDED_KEYS + ")");

        return new CassUserGroupDao(base, EXTENDED_DAO_KEYS[extensionNum], cfName);
    }

}
