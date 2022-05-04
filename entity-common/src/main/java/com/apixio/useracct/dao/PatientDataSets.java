package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.dao.OneToMany;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.PatientDataSet;

/**
 * PatientDataSets defines the persisted level operations on PatientDataSet entities.
 *
 * The set of PatientDataSets is cached via CachingBase.  The set of custom properties for PatientDataSets
 * is cached in this class.
 *
 * PatientDataSets also keeps track of the association of PDS IDs with Organization IDs that own
 * the PDS.  It also allows for querying for PDS instances that aren't owned by an OrgID.
 * This is done by a non-cached set of redis keys, as it's anticipated that queries and
 * updates on these will be infrequent.  This association allows only one OrgID to be
 * associated with a PDS ID (but an OrgID can be associated with multiple PDS IDs), so for each
 * OrgID we conceptually have a list of PDS IDs and a given PDS ID can belong in at most one such list.
 *
 * There is a second type of association with PDSs that allows use to keep track of what
 * projects are referring to (or rely on) a PDS.  This association is conceptually the
 * opposite in that a project can refer to at most 1 PDS, but a PDS can be referred to by
 * multiple projects, so for each PDS ID we have a list of Project IDs and a given Project ID
 * can belong to at most one such list.
 *
 * The whole Organization, PDS, Project relationship is a tree, with Organization at the root
 */
public final class PatientDataSets extends CachingBase<PatientDataSet> {

    /**
     * Unique "one to many" prefix for recording organization-to-PDS
     * relationships.  This MUST NOT CHANGE and MUST BE UNIQUE across all OneToMany uses.
     */
    private static final String ORG_TO_PDS_1TOMANY     = "org-to-pds";

    private static OneToMany.OneToManyConfig orgToPdsConfig = new OneToMany.OneToManyConfig(
        ORG_TO_PDS_1TOMANY, Organization.OBJTYPE, PatientDataSet.OBJTYPE
        );

    /**
     * Auto-incrementing number for assiging as the "external" (now internal) PDS ID.  Note that
     * we store the LAST VALUE RETURNED (due to how Redis' INCRBY works)!
     */
    private static final String AUTO_INCR_KEY = "pds-last-id";
    private String autoIncrKey;

    /**
     * Embedded OneToMany that manages one organization owning many PDS instances and
     * one PDS instance being owned by at most one Organization.
     */
    private OneToMany orgToPds;

    /**
     * Creates a new PatientDataSets DAO instance.
     */
    public PatientDataSets(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.PATIENT_DATASETS, PatientDataSet.OBJTYPE);

        orgToPds = new OneToMany(this.redisOps, makeKey(""), orgToPdsConfig);
        autoIncrKey = makeKey(AUTO_INCR_KEY);
    }

    /**
     * Get the next auto-incremented value for the external (i.e., was defined by
     * an external system, like CareOptimizer) but now internal (managed via this
     * mechanism) and increment the redis-backed "next number".
     */
    public Long getNextPdsID(boolean increment)
    {
        // Note that we MUST use incrDirect as there is no way in redis to
        // atomically (within a redis "transaction") increment & read the "next
        // number" key.  This means that holes in the ID range are possible if
        // a call is made here but the value not persisted within the (assumed)
        // enclosing transaction.

        if (increment)
        {
            return redisOps.incrDirect(autoIncrKey, +1);
        }
        else
        {
            String val = redisOps.get(autoIncrKey);

            return (val != null) ? Long.valueOf(val) : null;
        }
    }

    public void setNextPdsID(long nextID)
    {
        redisOps.set(autoIncrKey, Long.toString(nextID));
    }

    /**
     * Reads and returns a list of all PatientDataSets persisted in Redis.
     */
    public List<PatientDataSet> getAllPatientDataSets(boolean activeOnly)
    {
        List<PatientDataSet> all = new ArrayList<PatientDataSet>();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
            {
                PatientDataSet c = new PatientDataSet(fields);

                if (!activeOnly || c.getActive())
                    all.add(c);
            }
        }

        return all;
    }

    /**
     * Looks for a PatientDataSet instance with the given ID in Redis and if found returns
     * a restored PatientDataSet instance.  Null is returned if the ID is not found.
     */
    public PatientDataSet findPatientDataSetByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new PatientDataSet(fields);
        else
            return null;
    }

    /**
     * Looks for a PatientDataSet instance with the given ID in the cache and if found returns
     * a restored PatientDataSet instance.  Null is returned if the ID is not found.
     */
    public PatientDataSet findPatientDataSetInCache(XUUID id)
    {
        ParamSet fields = findInCacheByID(id);

        if (fields != null)
            return new PatientDataSet(fields);
        else
            return null;
    }

    /**
     * Bulk translate from XUUID to PDS, throwing exception if any ID can't be found
     */
    public List<PatientDataSet> findPatientDataSetsByIDs(List<XUUID> ids)
    {
        List<PatientDataSet> pdsList = new ArrayList<>();

        cacheLatest();

        for (XUUID id : ids)
        {
            PatientDataSet pds = findPatientDataSetInCache(id);

            if (pds == null)
                throw new IllegalArgumentException("Unknown PDS id [" + id + "]");

            pdsList.add(pds);
        }

        return pdsList;
    }

    /**
     * Intercept the creation so we can keep track of unassociated PDSs
     */
    @Override
    public void create(PatientDataSet pds)
    {
        super.create(pds);

        // initial state of PDS is unassociated with anything.
        orgToPds.recordChild(pds.getID());
    }

    @Override
    public void delete(PatientDataSet pds)
    {
        super.delete(pds);
    }

    /**
     * Return a list of PDS XUUIDs that aren't associated with another ID.
     * This list can be fed to findPatientDataSetsByIDs
     */
    public List<XUUID> getUnownedPds()
    {
        return orgToPds.getDanglingChildren();
    }

    /**
     * Associate the given PDS ID with the otherID, returning false if the PDS ID
     * is already associated.  In that case, the client must first unassociate
     * before reassociating.
     */
    public boolean ownPds(XUUID pdsID, XUUID orgID)
    {
        orgToPds.addChildToParent(orgID, pdsID);

        return true;
    }

    /**
     * Remove the association between the PDS ID and whatever it is already associated
     * with.
     */
    public void disownPds(XUUID pdsID)
    {
        orgToPds.removeChildFromParent(pdsID);
    }

    /**
     * Get the list of PDS IDs that are associated with the given otherID.
     */
    public List<XUUID> getPdsOwnedbyOrg(XUUID orgID)
    {
        return orgToPds.getChildren(orgID);
    }

}
