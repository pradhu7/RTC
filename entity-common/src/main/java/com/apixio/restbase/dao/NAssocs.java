package com.apixio.restbase.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.entity.NAssoc;
import com.apixio.restbase.DaoBase;

/**
 * Manages an n-element assocation between strings in a way that provides fast queries
 * for getting all such associations and getting all associations whose element N
 * has a particular value.
 *
 * For example, if a 3-element association between [UserID, ProjectID, Status] is
 * needed, using NAssocs it would be possible to get, for a particular user, the
 * set of projects and status for that user; or, it would be possible to get the
 * user and project that have a particular status (assuming that a user could be
 * in/have a different status for each project).
 *
 * This class supports distinct namespaces for associations where all associations
 * within each namespace are treated independently of associations in other
 * namespaces.
 *
 * It's helpful to think of the n values in a tuple as forming the primary key
 * of a row in an RDB; extra fields (columns) can be stored along with the
 * primary key values.  In this analogy the system allows efficient queries like
 *
 *   SELECT * FROM table WHERE field=value
 *
 * where table is the namespace and field is an Assoc.key (its value) and value
 * is an actual tuple value (e.g., a projectID).
 *
 * Each actual association that's recorded has a unique ID (XUUID) along with the
 * following data (think of this as Oracle's "rowid"):
 *
 *  * the namespace it was created in
 *  * all the actual tuple values
 *
 * Each position in a tuple is named by a "key" (a string).
 *
 * Redis structures:
 *
 *  * the actual association object; this contains namespace and elements (key=val, ...)
 *    * redis key:  {prefix}{XUUID}          # XUUID type is "AS_"
 *    * redis object:  HASH
 *      * field name:  pojo name
 *      * field value: string value of pojo field 
 *
 *  * the key to find association obj (above) with all elements:
 *    * redis key:  {prefix}nassoc-{type}-map
 *    * redis object:  HASH
 *      * field name:  {element1value}.{element2value}.{etc...}
 *      * field value: XUUID of actual association object            # note that HVALS can be used to get all of type
 *
 *  * the key(s) to look up by values of each element of associations:
 *    * redis key:  {prefix}nassoc-{type}-{elename}:{elevalue}
 *    * redis object:  SET
 *      * members:     XUUID of actual association object
 * 
 */
public abstract class NAssocs extends BaseEntities {

    /**
     * Used as part of all redis keys managed by this class
     */
    private final static String NASSOC_KEY = "nassoc-";

    /**
     * An Assoc is an actual binding of a value to a (logical) position in the tuple.
     */
    public static class Assoc {
        public String key;
        public String val;

        public Assoc(String key, String val)
        {
            this.key = key;
            this.val = val;
        }

        @Override
        public String toString()
        {
            return "(" + key + ":" + val + ")";
        }
    }

    /**
     * AssocConfig comprises the information about the namespace and the tuple
     * details (which is, right now, only a unique name).  Each DAO class that
     * extends NAssoc must provide an instance of AssocConfig with a unique
     * namespace value.
     */
    public static class AssocConfig {
        private String   namespace;   // must be unique
        private String[] keys;

        public AssocConfig(String namespace, String[] keys)
        {
            this.namespace = namespace;
            this.keys      = keys;
        }

        public String getNamespace()
        {
            return namespace;
        }

        public String[] getKeys()
        {
            return keys;
        }
    }

    /**
     * Keep track of the config from the extended class.
     */
    private AssocConfig config;

    /**
     * Cache calculated values
     */
    private String allMapKey;

    /**
     * Create a new NAssoc instance.
     */
    protected NAssocs(DaoBase seed, AssocConfig config)
    {
        super(seed);

        this.config    = config;
        this.allMapKey = makeAllMapkey();
    }

    /**
     * Creates a new association as described in the NAssoc-extended instance.  This
     * will create the unique XUUID and store the contents of the NAssoc POJO as a
     * redis HASH.  Note that if, for whatever reason, the exact same tuple values
     * are used in two calls to createAssoc(), only the last XUUID "wins"--the first
     * one created is lost forever (because the HASH field value is overwritten).
     */
    protected void createAssoc(NAssoc assoc)
    {
        if (assoc.getElements().size() != config.getKeys().length)
            throw new IllegalArgumentException("Number of elements in association to be saved [" + assoc.getElements().size() +
                                               "] != number of configured elements in tuple [" + config.getKeys().length + "]");

        List<Assoc> tuple = makeTuple(assoc.getElements());

        // save the actual object
        super.update(assoc);

        // add to each "by element" index
        for (Assoc ele : tuple)
            redisOps.sadd(makeByElementValue(ele), assoc.getID().toString());

        // save in main map so we can get back to it via full tuple values
        redisOps.hset(allMapKey, makeHashFieldName(tuple), assoc.getID().toString());
    }

    /**
     * Given the same information about a tuple that was supplied to createAssoc
     * (via the NAssoc.getElements list), return the actual object.
     */
    protected ParamSet findAssoc(List<String> values)
    {
        if (values.size() != config.getKeys().length)
            throw new IllegalArgumentException("Number of elements in association to be retrieved [" + values.size() +
                                               "] != number of configured elements in tuple [" + config.getKeys().length + "]");

        List<Assoc> tuple = makeTuple(values);
        String      id    = redisOps.hget(allMapKey, makeHashFieldName(tuple));

        if (id != null)
            return findByID(XUUID.fromString(id));
        else
            return null;
    }

    /**
     * Given an association (e.g., ProjID=PR_someid), return the XUUIDs of all the
     * associations that have the value ("PR_someid") for the ProjID element of the
     * association.
     */
    protected List<XUUID> findByElement(Assoc assoc)
    {
        String       redisKey = makeByElementValue(assoc);
        List<XUUID > assocs   = new ArrayList<>();

        for (String xuuid : redisOps.smembers(redisKey))
            assocs.add(XUUID.fromString(xuuid, NAssoc.OBJTYPE));

        return assocs;
    }

    /**
     * Returns ALL association records within namespace.
     */
    protected List<XUUID> getAllAssociations()
    {
        throw new IllegalStateException("Unimplemented:  RedisOps needs to add HVALS command");
    }

    /**
     * Delete an NAssoc association by undoing all that was done in createAssoc().
     */
    public void delete(NAssoc assoc)
    {
        List<Assoc> tuple = makeTuple(assoc.getElements());

        // delete from main map
        redisOps.hdel(allMapKey, makeHashFieldName(tuple));

        // remove from each "by element" index
        for (Assoc ele : tuple)
            redisOps.srem(makeByElementValue(ele), assoc.getID().toString());

        // delete the actual object
        super.delete(assoc);
    }

    /**
     * Convenience method to convert from a list of values to a list of Assoc objects.
     * The order of string values in "values" is aligned one-to-one with the declared
     * keys (in the AssocConfig object).
     */
    private List<Assoc> makeTuple(List<String> values)
    {
        String[]    keys  = config.getKeys();
        List<Assoc> tuple = new ArrayList<>(config.getKeys().length);

        for (int i = 0; i < keys.length; i++)
        {
            Assoc ele = new Assoc(keys[i], values.get(i));

            tuple.add(ele);
        }

        return tuple;
    }

    /**
     * Creates the redis key used to keep track of all associations of the given namespace
     */
    private String makeAllMapkey()
    {
        return super.makeKey(NASSOC_KEY + config.getNamespace() + "-map"); // make once...
    }

    /**
     * Creates the HASH field name with the form {element1Value}.{element2value}.{...}
     * where element values are taken, in order, from the NAssoc.
     */
    private String makeHashFieldName(List<Assoc> tuple)
    {
        StringBuilder sb = new StringBuilder();

        for (Assoc assoc : tuple)
        {
            if (sb.length() > 0)
                sb.append(".");

            sb.append('[');
            sb.append(assoc.val);
            sb.append(']');
        }

        return sb.toString();
    }

    /**
     * Creates the redis key used to provide fast query by an association element value
     */
    private String makeByElementValue(Assoc assoc)
    {
        return super.makeKey(NASSOC_KEY + config.getNamespace() + "-" + assoc.key + ":" + assoc.val);
    }

}
