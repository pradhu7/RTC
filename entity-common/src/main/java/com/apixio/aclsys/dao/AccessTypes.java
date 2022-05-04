package com.apixio.aclsys.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.aclsys.entity.AccessType;
import com.apixio.restbase.dao.BaseEntities;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;

/**
 * AccessTypes defines the persisted level operations on AccessType entities.
 */
public class AccessTypes extends BaseEntities {

    /**
     * We need to be able to look up a AccessType by name.
     * This is done by putting all names into a single redis hash
     */
    private static final String INDEX_BYNAME = "access-x-byname";

    /**
     * Keep track of all AccessTypes
     */
    private static final String INDEX_ALL    = "access-x-all";

    /**
     * Keep track of modification time
     */
    private static final String MODIFICATION_TIME = "access-last-modified";

    /**
     * These are the redis keys for keeping track of email address -> userID
     *
     *  accessTypeLookupByName:  the keyname to the redis hash whose elements are the names
     *  accessTypeLookupAllKey:  the keyname to the redis hash whose elements are all the accessType IDs
     */
    protected String accessTypeLookupByName;
    protected String accessTypeLookupAllKey;
    private   String accessTypeMod;

    /**
     *
     */
    public AccessTypes(DaoBase seed)
    {
        super(seed);

        // create keyname here as that can't be done until after object is fully initialized.
        accessTypeLookupByName = super.makeKey(INDEX_BYNAME);
        accessTypeLookupAllKey = super.makeKey(INDEX_ALL);
        accessTypeMod          = super.makeKey(MODIFICATION_TIME);
    }

    /**
     * Returns the epoch milliseconds that the last modification was made to Redis
     * for any AccessType object (create/update/delete).
     */
    public long getLastModificationTime()
    {
        try
        {
            String lm = redisOps.get(accessTypeMod);

            if (lm != null)
                return Long.parseLong(lm);
            else
                return 0L;
        }
        catch (Exception x)
        {
            return 0L;
        }
    }

    /**
     * Looks up a accessType by name and reads from Redis and returns it, if it exists.
     */
    public AccessType findAccessTypeByName(String name)
    {
        XUUID accessTypeID = XUUID.fromString(redisOps.hget(accessTypeLookupByName, name), AccessType.OBJTYPE);

        if (accessTypeID != null)
            return findAccessTypeByID(accessTypeID);
        else
            return null;
    }

    /**
     * Reads and returns a list of all AccessTypes persisted in Redis.
     */
    public List<AccessType> getAllAccessTypes()
    {
        List<AccessType> all = new ArrayList<AccessType>();

        for (String id : redisOps.lrange(accessTypeLookupAllKey, 0, -1))
            all.add(findAccessTypeByID(XUUID.fromString(id, AccessType.OBJTYPE)));

        return all;
    }

    /**
     * Looks for a AccessType instance with the given ID in Redis and if found returns
     * a restored AccessType instance.  Null is returned if the ID is not found.
     *
     * Only scalar data fields are filled in (no counters or lists).
     */
    public AccessType findAccessTypeByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new AccessType(fields);
        else
            return null;
    }

    /**
     * Returns the epoch milliseconds that the last modification was made to Redis
     * for any AccessType object (create/update/delete).
     */
    protected void setLastModificationTime(
        long time
        )
    {
        redisOps.set(accessTypeMod, Long.toString(time));
    }

}
