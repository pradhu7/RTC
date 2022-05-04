package com.apixio.restbase.dao;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.datasource.redis.MapResponse;
import com.apixio.datasource.redis.RedisPipeline;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.DaoBase;

/**
 * Provides a DAO layer for keeping track of all instances of a particular entity
 * type.  It supports further subdivision of the entire set by a "domain".
 *
 * No caching of anything is done--only list management, really.
 */
public abstract class EntitySetBase<T extends BaseEntity> extends BaseEntities<T>
{
    private String       domain;
    private String       datatypeName;
    private List<String> allowedObjTypes;   // from BlahEntity.OBJTYPE

    /**
     * So we can keep track of all entities of this type.
     */
    private String entityLookupAllKey;

    public EntitySetBase(DaoBase seed, String domain, String datatype, String... allowedObjTypes)
    {
        super(seed);

        if ((datatype == null) || (datatype.trim().length() == 0))
            throw new IllegalArgumentException("Datatype name must not be null/empty");

        if ((domain != null) && domain.trim().length() == 0)
            domain = null;

        this.domain        = domain;
        this.datatypeName  = (domain == null) ? datatype : (datatype + ":" + domain);
        this.allowedObjTypes = Arrays.asList(allowedObjTypes);

        entityLookupAllKey = super.makeKey(datatypeName + "_all");
    }

    /**
     * Returns the calculated datatype name (augmented with domain)
     */
    protected String getDatatypeName()
    {
        return datatypeName;
    }

    /**
     * Returns the string type prefixes that are allowed in this entity set.
     */
    protected List<String> getAllowedObjType()
    {
        return allowedObjTypes;
    }

    /**
     * A bit dangerous to expose but there have been instances where the "all" index
     * needed to be rebuilt.
     */
    protected String getAllKey()
    {
        return entityLookupAllKey;
    }

    /**
     * General business logic entity creation pattern is to do a new() on the POJO entity
     * (which creates/assigns the XUUID), and then call this method.
     */
    public void create(T entity)
    {
        XUUID id = validateEntity(entity);

        addToIndexes(entity);

        super.update(entity);
    }
    
    /**
     * Delete the entity by removing it from 'all' index.
     */
    @Override
    public void delete(T base)
    {
        XUUID id = validateEntity(base);

        removeFromIndexes(base);

        super.delete(base);
    }
    
    /**
     * Validates that the given entity's ID (as XUUID) is of the type that is managed
     * by this CachingBase instance.  An exception is thrown if it isn't and the ID
     * returned if it is good.
     */
    protected XUUID validateEntity(T entity)
    {
        XUUID id = entity.getID();

        if (!allowedType(id.getType()))
            throw new IllegalArgumentException("Expected entity with XUUID of type to be one of " + allowedObjTypes + " but got '" + id.getType() + "'");
        else if (!sameDomain(entity))
            throw new IllegalArgumentException("Expected entity with XUUID to be in same domain as cache controller: " + entity.getDomain() + " :: " + domain);

        return id;
    }

    /**
     * Returns a (copied) collection of all the known entity IDs.  It's expected generally
     * that the calling code will follow this with calls to findInCacheByID in a loop.
     */
    public Collection<XUUID> getAllEntityIDs()
    {
        List<XUUID> all = new ArrayList<>();

        for (String id : redisOps.smembers(entityLookupAllKey))
        {
            XUUID  xid = XUUID.fromString(id);

            if (!allowedType(xid.getType()))
                throw new IllegalArgumentException("Loaded objectID [" + id + "] but allowed types are " + allowedObjTypes);

            all.add(xid);
        }

        return all;
    }

    protected boolean sameDomain(T entity)
    {
        String ed = entity.getDomain();

        return (
            ((ed == null)     && (domain == null))  ||
            ((domain != null) && domain.equals(ed)) ||
            ((ed != null)     && ed.equals(domain))
            );
    }

    /**
     * Returns true if the passed in type is in the set of types in allowedObjTypes
     */
    protected boolean allowedType(String type)
    {
        for (String allowed : allowedObjTypes)
        {
            boolean wild = allowed.endsWith("*");

            if (wild)
                allowed = allowed.substring(0, allowed.length() - 1);

            // if allowed ends with "*" then just test via startsWith

            if ((wild && type.startsWith(allowed)) || (!wild && allowed.equals(type)))
                return true;
        }

        return false;
    }

    /**
     * Adds the entity to the indexed lookups so we can find all entities.
     */
    private void addToIndexes(T entity)
    {
        redisOps.sadd(entityLookupAllKey, entity.getID().toString());
    }

    /**
     * Removes the entity from the indexed lookups key.
     */
    private void removeFromIndexes(T entity)
    {
        redisOps.srem(entityLookupAllKey, entity.getID().toString());
    }

}
