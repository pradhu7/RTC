package com.apixio.aclsys.dao.redis;

import com.apixio.aclsys.dao.BaseUserGroupDaoFactory;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.dao.UserGroupDaoFactory;
import com.apixio.aclsys.dao.redis.RedisUserGroupDao;
import com.apixio.restbase.DaoBase;

/**
 * A UserGroupDaoFactory is
 */
public class RedisUserGroupDaoFactory extends BaseUserGroupDaoFactory implements UserGroupDaoFactory
{

    public UserGroupDao getDefaultUserGroupDao(DaoBase base, String cfName)
    {
        return new RedisUserGroupDao(base, DEFAULT_KEY_PFX);
    }

    public UserGroupDao getExtendedUserGroupDao(DaoBase base, int extensionNum, String cfName)
    {
        if ((extensionNum < 0) || (extensionNum >= MAX_EXTENDED_KEYS))
            throw new IllegalArgumentException("Request for extended UserGroupDao outside allowed range of [0, " + MAX_EXTENDED_KEYS + ")");

        return new RedisUserGroupDao(base, EXTENDED_DAO_KEYS[extensionNum]);
    }

}
