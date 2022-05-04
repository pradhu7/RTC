package com.apixio.restbase.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.datasource.redis.MapResponse;
import com.apixio.datasource.redis.RedisPipeline;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.entity.CustomProperty;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.util.DateUtil;
import com.apixio.restbase.DaoBase;

/**
 * CustomProperties defines the persisted level operations on CustomProperty entities.
 *
 * A CustomProperty has a type which is used to make sure acceptable values are used
 * with that type.  Properties also have a "scope" that is used to separate into groups
 * sets of related properties.  A scope is really just EntityType...
 *
 * The overall model is:
 *
 *  * properties exist in some uniquely named scope
 *
 *  * entityIDs are also associated with a scope
 *
 *  * all properties attached to an entity MUST have the same scope and all
 *    entities with those properties must also always reside in that scope
 *    (some methods accept only an EntityID and no CustomProperty so those
 *    methods also take a scope parameter)
 *
 *  * for performance of loading the caches, we must keep track, in redis, of
 *    which scope an entityID is in
 *
 * There is no support for cleanup due to deletion of entities.
 *
 * Redis keys:
 *
 *  * key to record custom properties on an entity:
 *      key = "custprop:{entityID}"; type = HASH; hashkey = "{propertyName}"; hashval = "{stringized value}"
 *
 *  * key to record entities within a scope:
 *      key = "custscope:{scope}"; type = SET; member = "{entityID}"
 *
 * EntityIDs are added to a scope when an property is added to the entity ID.
 * 
 */
public final class CustomProperties extends CachingBase<CustomProperty> {

    /**
     * To keep track of all the entities of a given scope that have properties.
     */
    final private static String KEY_SCOPEMEMBERS = "custscope:";

    /**
     * To keep track of all the actual custom properties added to each entity.
     */
    final private static String KEY_ENTITYPROPERTIES = "custprop:";  // entityID is appended

    /**
     * Keep track of the last data version, by scope, read from redis for the custom property values.
     */
    private Map<String, Integer> scopeCacheVersion = new HashMap<String, Integer>();   // Scope -> lastReadDataVersionOfScope

    /**
     * Caching of CustomProperties for an entity is done here.  Whenever a property is added
     * or deleted from an entity, the dataversion for that scope is bumped.  Whenever the set of properties
     * is accessed, a check is done to validate that we have the most recent set and things
     * are reloaded as necessary.  Note that logic similar to what's in CachingBase.cacheLatest
     * is used.
     */
    private static class CachedProperty {

        /**
         * How to interpret the String value
         */
        PropertyType type;

        /**
         * we intentionally leave it as what is read in from redis and convert on demand.
         */
        String value;

        CachedProperty(PropertyType type, String value)
        {
            this.type  = type;
            this.value = value;
        }

        Boolean asBoolean()       { return convertToBoolean(value); }
        Integer asInteger()       { return convertToInteger(value); }
        Double  asDouble()        { return convertToDouble(value);  }
        Date    asDate()          { return convertToDate(value);    }

        Object asObject()
        {
            switch (type)
            {
                case STRING:
                {
                    return value;
                }
                case BOOLEAN:
                {
                    return asBoolean();
                }
                case INTEGER:
                {
                    return asInteger();
                }
                case DOUBLE:
                {
                    return asDouble();
                }
                case DATE:
                {
                    return asDate();
                }
                default:
                {
                    return null;
                }
            }
        }
    }

    /**
     * A rather deep mapping that groups EntityIDs by scope (which really means that all properties
     * for a given entity--such as Customer--must be of the same scope), and then for each EntityID
     * it groups the set of custom properties into a single map.  This last map goes from custom
     * property name to cached property value.  So:
     *
     *  Scope -> ( EntityID -> ( PropertyName -> Value ) )
     *
     * Second level map is from Entity ID to set of properties for that Entity.  All entities of a given type
     * MUST be within the same scope (scope really means entity type for this caching).
     *
     *  EntityID -> (map from property name to CachedProperty)
     *
     * Note that CachedProperty contains only type and value.
     */
    private Map<String, Map<XUUID, Map<String, CachedProperty>>> scopeCache = Collections.synchronizedMap(new HashMap<String, Map<XUUID, Map<String, CachedProperty>>>());

    /**
     *
     */
    public CustomProperties(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.CUSTOM_PROPERTIES, CustomProperty.OBJTYPE);
    }

    /**
     * Performs migration from Customer-only extendable properties to generic (any entity) properties.
     * The original code/design assumed (rightly) that all CustomProperty objects were for Customers.
     * The new model is to have them contained within a scope (entity type) but in order to do that
     * efficiently, we keep a SET of per-scope entities.  This method pulls over the
     * full list of Customers into the "Customer" scope.
     */
    public void migrateToGenericProperties(Collection<XUUID> customers)
    {
        for (XUUID customer : customers)
            addEntityToScope(Datanames.SCOPE_CUSTOMER, customer);
    }

    /**
     * Reads and returns a list of all CustomProperties persisted in Redis.
     */
    public List<CustomProperty> getAllCustomPropertiesByScope(String scope)
    {
        List<CustomProperty> all = new ArrayList<CustomProperty>();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
            {
                CustomProperty cp = new CustomProperty(fields);

                if (cp.sameScope(scope))
                    all.add(cp);
            }
        }

        return all;
    }

    /**
     * Finds and returns the CustomProperty with the given (unique) name.
     */
    public CustomProperty findByName(String name, String scope)
    {
        name = CustomProperty.normalizeName(name);

        for (CustomProperty cp : getAllCustomPropertiesByScope(scope))
        {
            if (name.equals(cp.getName()))
                return cp;
        }

        return null;
    }

    /**
     * Sets the given property on the entity.  This does a write-through the redis and also updates
     * the local cache and bumps the data version.
     */
    public void setProperty(
        XUUID           xid,
        CustomProperty  prop,
        Object          value
        )
    {
        String                      scope   = prop.getScope();
        Map<String, CachedProperty> eCache  = getEntityCache(scope, xid);
        PropertyType                type    = prop.getType();
        String                      strVal  = convertValue(type, value);
        CachedProperty              cp      = new CachedProperty(type, strVal);

        eCache.put(prop.getName(), cp);

        // we need to record the scope somewhere so that when we read back all the entities and
        // their properties, we know what the scope is (since we reload
        redisOps.hset(makeCustomPropertyKey(xid), prop.getName(), strVal);

        // add so we can populate cache correctly since we read cache by scope (which means we
        // need to know which XIDs to read from redis)
        addEntityToScope(scope, xid);

        dataVersions.incrDataVersion(makeDataVerName(scope));
    }

    /**
     * Removes the given property from the entity.  This does a write-through the redis and also updates
     * the local cache and bumps the data version.
     */
    public void removeProperty(
        XUUID           xid,     // of entity
        CustomProperty  prop
        )
    {
        Map<String, CachedProperty> props = getEntityCache(prop.getScope(), xid);

        // remove from local cache; hmmm... not strictly necessary, I suppose, because we bump dataver on ourselves...bad
        if (props != null)
            props.remove(prop.getName());

        // we might not have pulled this into cache from redis we do this outside the "is cached" code above
        dataVersions.incrDataVersion(makeDataVerName(prop.getScope()));
        redisOps.hdel(makeCustomPropertyKey(xid), prop.getName());
    }

    /**
     * Removes the given property from ALL of the entities within the given scope.
     */
    public void deleteProperty(
        CustomProperty  prop
        )
    {
        String  propName = prop.getName();

        for (Map.Entry<XUUID, Map<String, CachedProperty>> entry : getSnapshotScopeCache(prop.getScope()))
        {
            redisOps.hdel(makeCustomPropertyKey(entry.getKey()), prop.getName());
            entry.getValue().remove(propName);
        }

        dataVersions.incrDataVersion(makeDataVerName(prop.getScope()));
    }

    /**
     * Return a map from entityID to a map (of propname -> propvalue) of properties for each entity.
     * Only entities with properties of the given scope will be included.
     *
     * Think of this as a fetch of all entities in a given scope along with their custom properties
     * (entities without properties are not included).
     */
    public Map<XUUID, Map<String, Object>> getAllCustomProperties(String scope) // <EntityID, <propname, propvalue>>
    {
        Map<XUUID, Map<String, Object>> allProperties = new HashMap<XUUID, Map<String, Object>>();

        cacheLatestProperties(scope);

        for (Map.Entry<XUUID, Map<String, CachedProperty>> entry : getSnapshotScopeCache(scope))
        {
            XUUID                       entityID       = entry.getKey();
            Map<String, CachedProperty> propsForEntity = entry.getValue();
            Map<String, Object>         props          = new HashMap<String, Object>();

            allProperties.put(entityID, props);

            for (Map.Entry<String, CachedProperty> cps : propsForEntity.entrySet())
                props.put(cps.getKey(), cps.getValue().asObject());
        }

        return allProperties;
    }

    /**
     * Given a property, return a map from Entity XUUID to the property value for that entity.
     */
    public Map<XUUID, Object> getAllEntityProperties(CustomProperty cp) // <EntityID, propvalue>
    {
        String             scope         = cp.getScope();
        Map<XUUID, Object> allProperties = new HashMap<XUUID, Object>();
        String             propName      = cp.getName();

        cacheLatestProperties(scope);

        for (Map.Entry<XUUID, Map<String, CachedProperty>> entry : getSnapshotScopeCache(scope))
        {
            // all entries are guaranteed (?) to have properties within the correct scope, so add them all
            CachedProperty chp = entry.getValue().get(propName);

            if (chp != null)
                allProperties.put(entry.getKey(), chp.asObject());
        }

        return allProperties;
    }

    /**
     * Given an entityID, return a map from property name to the property value for that entity.  This
     * requires "scope" because we don't keep track of scope of entity.
     */
    public Map<String, Object> getAllCustomProperties(String scope, XUUID xid) // <propname, propvalue>
    {
        Map<String, Object> props = new HashMap<String, Object>();
        Map<String, CachedProperty> cps;

        cacheLatestProperties(scope);

        if ((cps = getEntityCache(scope, xid)) != null)
        {
            for (Map.Entry<String, CachedProperty> cp : cps.entrySet())
                props.put(cp.getKey(), cp.getValue().asObject());
        }

        return props;
    }

    /**
     * Given an entityID, return a map from property name to the property value for that entity.  This
     * requires "scope" because we don't keep track of scope of entity.
     *
     * This method is replicated from method above to optimize getting properties
     * for single project by skipping loading all projects into local cache
     */
    public Map<String, Object> getAllCustomPropertiesForOneEntity(String scope, XUUID xid) // <propname, propvalue>
    {
        Map<String, Object> props = new HashMap<String, Object>();
        Map<String, CachedProperty> cps = getEntityProperties(scope, xid);

        for (Map.Entry<String, CachedProperty> cp : cps.entrySet())
            props.put(cp.getKey(), cp.getValue().asObject());

        return props;
    }


    /**
     * Reads and returns a list of all EntityIDS for the given scope
     */
    private List<XUUID> getAllEntityIDsInScope(String scope)
    {
        List<XUUID> all = new ArrayList<XUUID>();

        for (String member : redisOps.smembers(makeScopeMembersKey(scope)))
            all.add(XUUID.fromString(member));

        return all;
    }

    /**
     * Record the fact that the given entityID has a property within the given scope.
     */
    private void addEntityToScope(String scope, XUUID entityID)
    {
        redisOps.sadd(makeScopeMembersKey(scope), entityID.toString());
    }

    /**
     * Checks if the current version of the data kept in cache is up to date with what's
     * in redis.  If the version is out of date, then all entity properties are read
     * in from redis.
     */
    private void cacheLatestProperties(String scope)
    {
        int      latestVersion = dataVersions.getDataVersion(makeDataVerName(scope));
        Integer  cachedVer     = scopeCacheVersion.get(scope);

        if ((cachedVer == null) || (cachedVer < latestVersion))
        {
            Map<XUUID, Map<String, CachedProperty>> entityCache = getScopeCache(scope);
            Collection<XUUID>                       allEntities = getAllEntityIDsInScope(scope);
            Map<String, PropertyType>               nameToType  = nameToTypeMap(getAllCustomPropertiesByScope(scope));

            // sync on any modifications to the list of entities within a scope
            synchronized (entityCache)
            {
                //double-check cache hasn't been updated
                latestVersion = dataVersions.getDataVersion(makeDataVerName(scope));
                cachedVer     = scopeCacheVersion.get(scope);
                if ((cachedVer != null) && (cachedVer >= latestVersion))
                {
                    return; //another thread has updated latest properties while current thread was locked
                }

                RedisPipeline                   rp   = redisOps.beginPipeline();
                Map<XUUID, MapResponse>         maps = new HashMap<>();
                Map<XUUID, Map<String, String>> ents = new HashMap<>();

                entityCache.clear();  // filled via getEntityCache().put()

                try
                {
                    for (XUUID entityID : allEntities)
                    {
                        String key = makeCustomPropertyKey(entityID);

                        maps.put(entityID, rp.hgetAll(key));
                    }

                    rp.waitForAll();

                    for (Map.Entry<XUUID, MapResponse> entry : maps.entrySet())
                        ents.put(entry.getKey(), entry.getValue().get());
                }
                finally
                {
                    rp.endPipeline();
                }

                for (Map.Entry<XUUID, Map<String, String>> entry : ents.entrySet())
                {
                    XUUID                       entityID    = entry.getKey();
                    Map<String, CachedProperty> customProps = getEntityCache(scope, entityID);

                    for (Map.Entry<String, String> nameValue : entry.getValue().entrySet())
                    {
                        PropertyType type = nameToType.get(nameValue.getKey());

                        if (type != null)
                        {
                            CachedProperty prop = new CachedProperty(type, nameValue.getValue());

                            customProps.put(nameValue.getKey(), prop);
                        }
                    }
                }
            }

            scopeCacheVersion.put(scope, latestVersion);
        }
    }

    /**
     * Added this method to address by passing local cache for better performance
     * and throughput
     */
    private Map<String, CachedProperty> getEntityProperties(String scope, XUUID entityID)
    {
        Map<String, CachedProperty> result = new HashMap<>();

        RedisPipeline rp   = redisOps.beginPipeline();
        Map<String, PropertyType> nameToType  = nameToTypeMap(getAllCustomPropertiesByScope(scope));

        Map<String, String> properties = new HashMap<>();
        try
        {
            String key = makeCustomPropertyKey(entityID);
            MapResponse mapResp = rp.hgetAll(key);
            rp.waitForAll();

            properties = mapResp.get();
        }
        finally {
            rp.endPipeline();
        }

        for (Map.Entry<String, String> nameValue : properties.entrySet())
        {
            PropertyType type = nameToType.get(nameValue.getKey());

            if (type != null)
            {
                CachedProperty prop = new CachedProperty(type, nameValue.getValue());

                result.put(nameValue.getKey(), prop);
            }
        }

        return result;
    }

    /**
     * Converts a list of CustomProperties to a map from name to property type (for
     * lookup efficiency within a loop).
     */
    private Map<String, PropertyType> nameToTypeMap(List<CustomProperty> propDefs)
    {
        Map<String, PropertyType> props = new HashMap<String, PropertyType>();

        for (CustomProperty cp : propDefs)
            props.put(cp.getName(), cp.getType());

        return props;
    }

    /**
     * Retrieves, creating if it's not there, the map from entityID to its cached properties (name, type, value).
     */
    synchronized
    private Map<XUUID, Map<String, CachedProperty>> getScopeCache(String scope)
    {
        Map<XUUID, Map<String, CachedProperty>> scopeMap = scopeCache.get(scope);

        if (scopeMap == null)
        {
            scopeMap = Collections.synchronizedMap(new HashMap<XUUID, Map<String, CachedProperty>>());
            scopeCache.put(scope, scopeMap);
        }

        return scopeMap;
    }
    
    /**
     * Returns the EntrySet of the map of (entity->props) for the given scope which is suitable for a readonly
     * iterate operation.  This separates the read path from the write path on the list of entities within
     * a given scope, thereby avoiding (hopefully) multithreading issues.
     */
    private Set<Map.Entry<XUUID, Map<String, CachedProperty>>> getSnapshotScopeCache(String scope)
    {
        Map<XUUID, Map<String, CachedProperty>> scopeMap = getScopeCache(scope);

        // sync to pause modifications to the list of entities within a scope
        synchronized (scopeMap)
        {
            return (new HashMap<>(scopeMap)).entrySet();
        }
    }

    /**
     * Given a scope and entityID (the entityID MUST be within that "scope"--which is really
     * an entity type...), retrieve, creating as necessary, the entity's map of
     * cached properties.
     */
    private Map<String, CachedProperty> getEntityCache(String scope, XUUID entityID)
    {
        Map<XUUID, Map<String, CachedProperty>> scopeCache  = getScopeCache(scope);

        // sync on any modifications to the list of entities within a scope
        synchronized (scopeCache)
        {
            Map<String, CachedProperty> entityCache = scopeCache.get(entityID);

            if (entityCache == null)
            {
                entityCache = new HashMap<String, CachedProperty>();  // ?not syncronizedMap?
                scopeCache.put(entityID, entityCache);
            }

            return entityCache;
        }
    }

    /**
     * These conversions must match what's done in the convertToXyz methods
     */
    private static String convertValue(PropertyType type, Object value)
    {
        switch (type)
        {
            case STRING:
            {
                return (String) value;
            }
            case BOOLEAN:
            {
                return ((Boolean) value).toString();
            }
            case INTEGER:
            {
                return ((Integer) value).toString();
            }
            case DOUBLE:
            {
                return ((Double) value).toString();
            }
            case DATE:
            {
                return DateUtil.dateToIso8601((Date) value);
            }
            default:
            {
                return "";
            }
        }
    }

    /**
     * These conversions must match what's done in convertValue(PropertyType, Object)
     */
    private static Boolean convertToBoolean(String value)
    {
        return Boolean.valueOf(value);
    }
    private static Integer convertToInteger(String value)
    {
        return Integer.valueOf(value);
    }
    private static Double convertToDouble(String value)
    {
        return Double.valueOf(value);
    }
    private static Date convertToDate(String value)
    {
        return DateUtil.validateIso8601(value);
    }
    
    /**
     * Creates the redis key for the properties HASH for the given entity XUUID.  This
     * HASH stores the name=value actual property values for the given entity.
     */
    private String makeCustomPropertyKey(XUUID entityID)
    {
        return super.makeKey(KEY_ENTITYPROPERTIES + entityID.toString());
    }

    /**
     * Creates the redis key for the SET of entityIDs of a particular scope
     */
    private String makeScopeMembersKey(String scope)
    {
        return super.makeKey(KEY_SCOPEMEMBERS + scope);
    }

    /**
     * Creates the dataversion key name for the given scope.  The entire set of scope-specific
     * entities will be reread into cache on a version change.
     */
    private String makeDataVerName(String scope)
    {
        return Datanames.CUSTOM_SCOPE + scope;
    }

}
