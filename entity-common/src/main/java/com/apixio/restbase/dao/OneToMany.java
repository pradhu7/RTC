package com.apixio.restbase.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.datasource.redis.RedisOps;

/**
 * Manages an association between IDs where one ID can have many children (for lack
 * of a better word) and each child can have only a single parent (or no parent).
 * The management of these associations also includes the ability to retrieve the
 * set of entities that are not attached to a parent.
 *
 * A good way to think of this is that it represents a portion of a tree, where the
 * parent node has zero or more children nodes, and the non-parent nodes can be
 * attached to at most parent.
 *
 * It is expected that the types of the parent and children will be different.
 *
 * Instances of this class are expected to be embedded/contained within a real DAO
 * instance as this is just a helper class.  Each embedded instance across ALL
 * "real" DAO intances MUST have a unique prefix (specified in the config).
 *
 * The methods are quite aggressive in that they'll change the structure to
 * be as requested regardless of the current structure (e.g., it's easy
 * to reparent a child by making a single call).
 *
 * Example uses:
 *  * recording which Organization owns which PatientDataSets; in this case the
 *    parent is an Organization and the children are the PDSs that it owns
 *
 *  * recording which Projects use which PatientDataSets; in this case the
 *    parent is the PDS and the children are the Projects (this assumes that
 *    a Project can use only a single PDS and that a single PDS can be used
 *    by more than one project).
 *
 * Queries/operations to support:
 *
 *  1.  record dangling child
 *  2.  add dangling child to a parent
 *  3.  remove child from a parent (making it dangle)
 *  4.  retrieve all children of a parent
 *  5.  retrieve parent of a child
 *  6.  retrieve all dangling children
 *  7.  remove child entirely (so it doesn't show up in the dangling list)
 *
 * Redis structures:
 *
 *  * all dangling children
 *    redis key:  {prefix}floating-children
 *         type:  LIST
 *       values:  XUUIDs of floating children
 *
 *  * parent to children association:  given a parent, return all its children
 *    redis key:  {prefix}children-of.{parentID}
 *         type:  LIST
 *       values:  IDs of children
 *
 *  * child to parent association:  given a child ID find it parent
 *    redis key:  {prefix}parents-of
 *         type:  HASH
 *     HASH key:  child ID
 *     HASH val:  parent ID
 *
 */
public class OneToMany {

    /**
     * Redis keys.  WARNING:  changing these will invalidate data
     */
    private final static String KEY_FLOATING   = "floating-children";
    private final static String KEY_PARENTSOF  = "parents-of";
    private final static String KEY_CHILDRENOF = "children-of.";

    /**
     * AssocConfig comprises the information about the namespace and the tuple
     * details (which is, right now, only a unique name).  Each DAO class that
     * extends NAssoc must provide an instance of AssocConfig with a unique
     * namespace value.
     */
    public static class OneToManyConfig {
        private String   namespace;   // must be unique across ALL instances of OneToMany
        private String   parentType;  // XUUID prefix (optional)
        private String   childType;   // XUUID prefix (optional)

        public OneToManyConfig(String namespace, String parentType, String childType)
        {
            this.namespace  = namespace;
            this.parentType = parentType;
            this.childType  = childType;
        }

        public boolean parentTypeMatch(XUUID test)
        {
            return (parentType == null) || parentType.equals(test.getType());
        }
        public boolean childTypeMatch(XUUID test)
        {
            return (childType == null) || childType.equals(test.getType());
        }
    }

    /**
     * 
     */
    private OneToManyConfig config;     // the config from the extended class.
    private RedisOps        redisOps;
    private String          prefix;     // master from redisOps
    private String          danglingPrefix;
    private String          parentsOfPrefix;
    private String          childrenOfPrefix;

    /**
     * Create a new OneToManyConfig instance.
     */
    public OneToMany(RedisOps redisOps, String prefix, OneToManyConfig config)
    {
        this.config   = config;
        this.redisOps = redisOps;
        this.prefix   = prefix + config.namespace + ":";

        // caching of static keys
        this.danglingPrefix   = this.prefix + KEY_FLOATING;
        this.parentsOfPrefix  = this.prefix + KEY_PARENTSOF;
        this.childrenOfPrefix = this.prefix + KEY_CHILDRENOF;
    }

    public void recordChild(XUUID childID)
    {
        if (!config.childTypeMatch(childID))
            throw new IllegalArgumentException("Mismatch in child type allowed [" + config.childType + "] and supplied [" + childID.getType() + "]");

        String childStr = childID.toString();

        if (!childAttached(childStr))
            addToDangling(childStr);
    }

    /**
     * Removes all traces of childID (i.e., disconnects from any parent and from dangling list).
     */
    public void unrecordChild(XUUID childID)
    {
        if (!config.childTypeMatch(childID))
            throw new IllegalArgumentException("Mismatch in child type allowed [" + config.childType + "] and supplied [" + childID.getType() + "]");

        String childStr = childID.toString();
        String parent   = getParent(childStr);

        if (parent != null)
            removeFromParent(parent, childStr);

        // purposely not an 'else' clause due to possible existence by forced XUUID
        redisOps.lrem(makeDanglingChildrenKey(), 0, childStr);
    }

    public void addChildToParent(XUUID parentID, XUUID childID)
    {
        if (!config.childTypeMatch(childID))
            throw new IllegalArgumentException("Mismatch in child type allowed [" + config.childType + "] and supplied [" + childID.getType() + "]");
        else if (!config.parentTypeMatch(parentID))
            throw new IllegalArgumentException("Mismatch in parent type allowed [" + config.parentType + "] and supplied [" + parentID.getType() + "]");

        String childStr  = childID.toString();
        String parentStr = parentID.toString();
        String curParent = getParent(childStr);

        if (curParent != null)
            removeFromParent(curParent, childStr);

        // it's okay for client to bypass recordChild...
        redisOps.lrem(makeDanglingChildrenKey(), 0, childStr);

        // actually add to children-of-parent and child-to-parent structures:
        redisOps.rpush(makeChildrenOfKey(parentStr), childStr);
        redisOps.hset(makeParentsOfKey(), childStr, parentStr);
    }

    public void removeChildFromParent(XUUID childID)
    {
        if (!config.childTypeMatch(childID))
            throw new IllegalArgumentException("Mismatch in child type allowed [" + config.childType + "] and supplied [" + childID.getType() + "]");

        String childStr = childID.toString();
        String parentID = getParent(childStr);

        if (parentID != null)
            removeFromParent(parentID, childStr);

        addToDangling(childStr);
    }

    /**
     * 
     */
    public List<XUUID> getDanglingChildren()
    {
        List<XUUID> ids = new ArrayList<>();

        for (String id : redisOps.lrange(makeDanglingChildrenKey(), 0, -1))
            ids.add(XUUID.fromString(id));

        return ids;
    }

    public List<XUUID> getChildren(XUUID parentID)
    {
        if (!config.parentTypeMatch(parentID))
            throw new IllegalArgumentException("Mismatch in parent type allowed [" + config.parentType + "] and supplied [" + parentID.getType() + "]");

        List<XUUID> ids = new ArrayList<>();

        for (String id : redisOps.lrange(makeChildrenOfKey(parentID.toString()), 0, -1))
            ids.add(XUUID.fromString(id));

        return ids;
    }

    private void addToDangling(String child)
    {
        // for robustness remove first; note that it's possible (due to forced XUUIDs) to
        // recordChild(id1), addChildToParent(id1), recordChild(id1), removeChildFromParent(id1),
        // which would--without doing the following lrem()--end up adding id1 to the list twice
        redisOps.lrem(makeDanglingChildrenKey(), 0, child);
        redisOps.rpush(makeDanglingChildrenKey(), child);
    }

    /**
     * Low-level remove from the parent-to-child and child-to-parent structures.
     */
    private void removeFromParent(String parent, String child)
    {
        redisOps.lrem(makeChildrenOfKey(parent), 0, child);
        redisOps.hdel(makeParentsOfKey(), child);
    }

    /**
     *
     */
    private String getParent(String childID)
    {
        return redisOps.hget(makeParentsOfKey(), childID);
    }

    private boolean childAttached(String childID)
    {
        return getParent(childID) != null;
    }

    /**
     * Redis key construction.
     */
    private String makeDanglingChildrenKey()
    {
        return danglingPrefix;
    }

    private String makeParentsOfKey()
    {
        return parentsOfPrefix;
    }

    private String makeChildrenOfKey(String parentID)
    {
        return childrenOfPrefix + parentID;
    }

}
