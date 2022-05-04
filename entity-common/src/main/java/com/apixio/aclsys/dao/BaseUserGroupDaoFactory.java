package com.apixio.aclsys.dao;

import com.apixio.restbase.DaoBase;

/**
 * BaseUserGroupDaoFactory collects common definitions to be used by UserGroupDao
 * implementations
 */
public abstract class BaseUserGroupDaoFactory
{
    /**
     * Persistence constants.  Don't ever change these unless you want to forget
     * real data.
     */
    protected final static String  DEFAULT_KEY_PFX = "grp.";

    /**
     * Parameterized DAO instance key prefix.  The names are intended to be generic in
     * nature BUT SHOULD NEVER BE CHANGED!  Semantics of these generic names are
     * outside this core ACL system.
     */
    protected final static String[] EXTENDED_DAO_KEYS = { "extended-1.grp.", "extended-2.grp.", "extended-3.grp." };
    public    final static int      MAX_EXTENDED_KEYS = EXTENDED_DAO_KEYS.length;

}
