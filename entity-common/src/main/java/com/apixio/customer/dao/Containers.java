package com.apixio.customer.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apixio.XUUID;
import com.apixio.customer.entity.OwnedEntityTrait;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.DaoBase;

/**
 * Container defines the persistence level operations on Container-like
 * entities.
 *
 * Container-extended instances are managed globally and per-customer.  The
 * global management (including caching) is done via CachingBase) and the
 * per-owner management is done here.
 *
 * The model is that an Owner (e.g., a Customer) owns Container entities, the order of Container
 * entities within an Owner is not important, and Containers own an ordered
 * list of references to "Contained" entities.
 */

/* Note the "&" syntax for template classes:  it just says T must extend both */
public abstract class Containers<T extends BaseEntity & OwnedEntityTrait, C extends BaseEntity & OwnedEntityTrait> extends CachingBase<T> {

    /**
     * Not all information a Containers instance needs is given in generic class info,
     * so this class is used to communicate the extra (static) information.
     */
    static class ContainerParams {
        /**
         * Dataname gives the unique name for detecting persisted version changes
         */
        String dataname;

        /**
         * containerObjType is the XUUID tag for the container object.
         * containedObjType is the XUUID tag for the contained object.
         */
        String containerObjType;
        String containedObjType;

        /**
         * memberListKey is the redis prefix used to manage the list of
         * contained objects for each container object.
         */
        String memberListKey;

        ContainerParams(String dataname, String containerObjType, String containedObjType, String memberListKey)
        {
            this.dataname         = dataname;
            this.containerObjType = containerObjType;
            this.containedObjType = containedObjType;
            this.memberListKey    = memberListKey;
        }

        @Override
        public String toString()
        {
            return "[container dataname=" + dataname + "]";
        }
    }

    /**
     * Keep track of the extra parameters.
     */
    private ContainerParams containerParams;

    /**
     * Generate a new instance of a Container type of object from the persisted
     * ParamSet fields.
     */
    abstract T genContainerType(ParamSet fields);

    /**
     * Really ugly that we need access to contained DAO here, but necessary because we have to
     * load them up in cache during cache reload
     */
    abstract List<C> loadAllContained();

    /**
     * This is built up from the raw Container data in redis; this is possible because
     * each Container is owned by exactly one Owner.  Doing it this way avoids yet
     * another key/structure to maintain in redis.  If the 1::1 rule is ever changed, then
     * it will need to be done a different way.
     *
     * Note that this structure maps from a OwnerID to another map.  This second map
     * goes from ContainerID to actual Container POJO.  The reason for this is that a
     * client could hold on to a Container POJO across cache reloads and if we were to try
     * to remove the Container POJO it would fail.  The alternative is to define
     * Container.equals() and Container.hashCode() but the general entity approach isn't
     * that way so this is more in line with the current designs.
     */
    private Map<XUUID, Map<XUUID, T>> cachedContainers = Collections.synchronizedMap(new HashMap<XUUID, Map<XUUID, T>>());  // OwnerXUUID => (ContainerID => Containers)

    /**
     * This is built up from the raw member lists data in redis.  Note that remove operations on
     * the List<Contained> need to be done by comparing .getID() values.
     */
    private Map<XUUID, List<C>> cachedContainerMembers = Collections.synchronizedMap(new HashMap<XUUID, List<C>>());  // ContainerID => list of Contained

    /**
     * Create a new Containers DAO
     */
    public Containers(ContainerParams params, DaoBase seed, DataVersions dv)
    {
        super(seed, dv, params.dataname, params.containerObjType);

        this.containerParams = params;
    }

    /**
     * Reads and returns a list of all Container entities persisted in Redis via the cache kept
     * by CachingBase.
     */
    public List<T> getAllContainers()
    {
        List<T> all = new ArrayList<T>();

        rebuildCache();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
                all.add(genContainerType(fields));
        }

        return all;
    }

    /**
     * Looks up ID and returns the Container, if it exists.
     */
    public T getByID(XUUID id)
    {
        ParamSet ps = super.findByID(id);

        return (ps != null) ? genContainerType(ps) : null;
    }

    /**
     * Persists the new Container
     */
    @Override
    public void create(T container)
    {
        super.create(container);

        addContainerToOwnerCache(container);
    }

    /**
     * Return all Containers owned by the given customer.  An empty set is returned if there are no containers.
     */
    public Set<T> getOwnedContainers(XUUID customerID)
    {
        Map<XUUID, T> containers;

        rebuildCache();

        containers = cachedContainers.get(customerID);

        if (containers != null)
            return new HashSet<T>(containers.values());  // return a copy of the List, but not of the elements
        else
            return new HashSet<T>();
    }

    /**
     * Adds the given Contained to the Container.  If any of the listed Contained entities is already in the
     * Container, then it is not moved.
     */
    public void addContained(T container, List<C> contained)
    {
        XUUID    containerID  = container.getID();
        List<C>  containedElements;
        List<C>  toAdd;

//        if (!validateOwners(container, contained))
//            throw new IllegalArgumentException("Attempt to add a Contained entity to a container that's owned by a different Owner");

        containedElements = getContainedElements(containerID);

        toAdd = new ArrayList<C>();

        for (C con : contained)
        {
            XUUID    conID   = con.getID();
            boolean  already = false;

            for (C inList : containedElements)
            {
                if (conID.equals(inList.getID()))
                {
                    already = true;
                    break;
                }
            }

            if (!already)
                toAdd.add(con);
        }

        if (toAdd.size() > 0)
        {
            String  memberKey = makeContainerMemberKey(containerID);

            for (C con : toAdd)
            {
                redisOps.rpush(memberKey, con.getID().toString());
                containedElements.add(con);
            }

            dataVersions.incrDataVersion(containerParams.dataname);
        }
    }

    /**
     * Replaces the list of Contained entities with what's supplied.
     */
    public void setContained(T container, List<C> contained)
    {
//        if (!validateOwners(container, contained))
//            throw new IllegalArgumentException("Attempt to set a Contained entities to a container that's owned by a different Owner");

        XUUID    containerID       = container.getID();
        List<C>  containedElements = getContainedElements(containerID);
        String   memberKey         = makeContainerMemberKey(containerID);
        boolean  changed           = false;

        if (containedElements.size() > 0)
        {
            containedElements.clear();
            redisOps.del(memberKey);
            changed = true;
        }

        if (contained.size() > 0)
        {
            for (C con : contained)
            {
                redisOps.rpush(memberKey, con.getID().toString());
                containedElements.add(con);
            }

            changed = true;
        }

        if (changed)
            dataVersions.incrDataVersion(containerParams.dataname);
    }

    /**
     * Return the list of contained entities in the container.
     */
    public List<C> getContained(T container)
    {
        return new ArrayList<C>(getContainedElements(container.getID()));
    }

    /**
     * We allow a delete so expose as public.  Deletes the Container from its owning customer.  This operation
     * is NOT cascaded to the customer's Contained entities that are part of the Container.
     */
    @Override
    public void delete(T container)
    {
        Map<XUUID, T> containers = cachedContainers.get(container.getOwner());
        XUUID         conID      = container.getID();

        if (containers != null)
        {
            redisOps.del(makeContainerMemberKey(conID));
            containers.remove(conID);
            super.delete(container);   // this bumps data version
        }
    }

    /**
     * Removes the given Contained entities from the given Container
     */
    public void removeContained(T container, List<C> contained)
    {
//        if (!validateOwners(container, contained))
//            throw new IllegalArgumentException("Attempt to remove a contained entity from a container that's owned by a different Owner");

        XUUID   containerID       = container.getID();
        List<C> containedElements = getContainedElements(containerID);
        String  memberKey         = makeContainerMemberKey(containerID);

        for (C con : contained)
        {
            XUUID conID = con.getID();

            redisOps.lrem(memberKey, 1, conID.toString());

            for (Iterator<C> it = containedElements.iterator(); it.hasNext(); )
            {
                C itCon = it.next();

                if (itCon.getID().equals(conID))
                {
                    it.remove();
                    break;
                }
            }
        }

        if (contained.size() > 0)
            dataVersions.incrDataVersion(containerParams.dataname);
    }

    /**
     * Removes the given Contained entities from the given Container
     */
    public void removeContainedByID(T container, List<XUUID> contained)
    {
        XUUID   containerID       = container.getID();
        List<C> containedElements = getContainedElements(containerID);
        String  memberKey         = makeContainerMemberKey(containerID);

        for (XUUID conID : contained)
        {
            redisOps.lrem(memberKey, 1, conID.toString());

            for (Iterator<C> it = containedElements.iterator(); it.hasNext(); )
            {
                C itCon = it.next();

                if (itCon.getID().equals(conID))
                {
                    it.remove();
                    break;
                }
            }
        }

        if (contained.size() > 0)
            dataVersions.incrDataVersion(containerParams.dataname);
    }

    /**
     * Makes sure that all contained entities in the list have the same owning entity as the
     * container.  False is returned if there is a mismatch in ownership.
     */
    private boolean validateOwners(T container, List<C> contained)
    {
        XUUID owner = container.getOwner();

        for (C con : contained)
        {
            if (!owner.equals(con.getOwner()))
                return false;
        }

        return true;
    }

    /**
     * Rebuilds the cachedContainers by iterating through all containers and building the
     * set of containers per customer and contained lists per container.
     */
    protected boolean rebuildCache()
    {
        boolean reloaded = cacheLatest();

        if (reloaded)
        {
            Map<XUUID, C> allContained = new HashMap<XUUID, C>();

            // this is a throw-away structure
            for (C con : loadAllContained())  //!! cross-DAO call
                allContained.put(con.getID(), con);

            synchronized (cachedContainers)
            {
                cachedContainers.clear();
                cachedContainerMembers.clear();

                for (T container : getAllContainers())
                {
                    XUUID   containerID       = container.getID();
                    List<C> containedElements = getContainedElements(containerID);

                    addContainerToOwnerCache(container);

                    //!! use pipelining here since it's in a loop:
                    // really ugly that we're making calls to redis in a sync block... fix later
                    for (String member : redisOps.lrange(makeContainerMemberKey(containerID), 0, -1))
                        containedElements.add(allContained.get(XUUID.fromString(member, containerParams.containedObjType)));
                }
            }
        }

        return reloaded;
    }

    /**
     * Adds the Container to the cached set of Container for the container's owning customer.
     */
    private void addContainerToOwnerCache(T container)
    {
        XUUID         custID     = container.getOwner();
        Map<XUUID, T> containers = cachedContainers.get(custID);

        if (containers == null)
        {
            containers = new HashMap<XUUID, T>();
            cachedContainers.put(custID, containers);
        }

        containers.put(container.getID(), container);
    }

    /**
     * Get the List<C> from the container member cache for the given containerID, creating the
     * list as necessary
     */
    private List<C> getContainedElements(XUUID containerID)
    {
        List<C> containedElements = cachedContainerMembers.get(containerID);

        if (containedElements == null)
        {
            containedElements = new ArrayList<C>();
            cachedContainerMembers.put(containerID, containedElements);
        }

        return containedElements;
    }

    /**
     * Creates the redis key for the container's LIST of members (which are contained XUUIDs).
     */
    private String makeContainerMemberKey(XUUID containerID)
    {
        return super.makeKey(containerParams.memberListKey + containerID.toString());
    }

}
