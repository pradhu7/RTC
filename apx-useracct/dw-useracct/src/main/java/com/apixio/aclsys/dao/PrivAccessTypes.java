package com.apixio.aclsys.dao;

import com.apixio.aclsys.entity.AccessType;
import com.apixio.restbase.DaoBase;

/**
 * AccessTypes defines the persisted level operations on AccessType entities.
 */
public class PrivAccessTypes extends AccessTypes {

    /**
     *
     */
    public PrivAccessTypes(DaoBase seed)
    {
        super(seed);
    }

    /**
     * Create a new instance of a AccessType.
     */
    public AccessType createAccessType(String name, String description)
    {
        AccessType accessType = findAccessTypeByName(name);

        if (accessType != null)
            throw new IllegalArgumentException("AccessType name [" + name + "] already used:  new accessType cannot be created");

        accessType = new AccessType(name);
        accessType.setDescription(description);

        addToIndexes(accessType);

        update(accessType);

        setLastModificationTime(System.currentTimeMillis());

        return accessType;
    }

    /**
     * Deletes the AccessType in Redis and updates the modification time to
     * all JVMs can reload lists.
     */
    public void deleteAccessType(String name)
    {
        AccessType accessType = findAccessTypeByName(name);

        if (accessType == null)
            throw new IllegalArgumentException("Unknown AccessType name [" + name + "]");

        super.delete(accessType);

        removeFromIndexes(accessType);

        setLastModificationTime(System.currentTimeMillis());
    }

    /**
     * Adds the accessType to the indexed lookups so we can find all accessTypes.
     */
    private void addToIndexes(AccessType accessType)
    {
        String accessTypeID = accessType.getID().toString();

        redisOps.hset(accessTypeLookupByName, accessType.getName(), accessTypeID);
        redisOps.rpush(accessTypeLookupAllKey, accessTypeID);
    }

    /**
     * Removes the operation from the indexed lookups.
     */
    private void removeFromIndexes(AccessType accessType)
    {
        String accessTypeID = accessType.getID().toString();

        redisOps.hdel(accessTypeLookupByName, accessType.getName());
        redisOps.lrem(accessTypeLookupAllKey, 1, accessTypeID);
    }

}
