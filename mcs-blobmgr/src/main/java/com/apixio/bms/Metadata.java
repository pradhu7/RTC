package com.apixio.bms;

import java.util.HashMap;
import java.util.Map;

/**
 * An instance of Metadata is, at this low level, optional information associated
 * with a blob.  Metadata is the full set of extra fields and their values, each field
 * requiring a MetadataDef to declare name and type
 *
 * Metadata has a type which is retrievable via getType() and the calling code
 * should use that info (or know already, based its knowledge of the schema) to
 * call the right "asXyz()" method.
 *
 * Metadata is persisted by generic code reading the type and pulling the correct
 * getter and storing the value "natively" in the database.
 */
public class Metadata
{

    /**
     * Note that for this to really work clients will have to have a central place
     * of static fields for all the MetadataDef instances...
     */
    private Map<MetadataDef, Object> allPairs = new HashMap<>();

    private Metadata()
    {
    }

    public static Metadata create()
    {
        return new Metadata();
    }

    public boolean isEmpty()
    {
        return allPairs.size() == 0;
    }

    public Metadata add(MetadataDef def, Object val)
    {
        def.checkValue(val);

        allPairs.put(def, val);

        return this;
    }

    /**
     * Caller must know how to cast...
     */
    public Object get(MetadataDef def)
    {
        return allPairs.get(def);
    }

    public String getString(MetadataDef def)
    {
        // we shouldn't need to check values here as only valid-type values are accept in add()
        return (String) allPairs.get(def);
    }

    public Boolean getBoolean(MetadataDef def)
    {
        return (Boolean) allPairs.get(def);
    }

    public Integer getInteger(MetadataDef def)
    {
        return (Integer) allPairs.get(def);
    }

    public Double getDouble(MetadataDef def)
    {
        return (Double) allPairs.get(def);
    }

    /**
     * Shallow clones only
     */
    public Map<MetadataDef, Object> getAll()
    {
        return new HashMap<>(allPairs);
    }

    // convenience only supposedly
    public Map<MetadataDef, Object> getAll(MetadataDef... defs)
    {
        Map<MetadataDef, Object> selected = new HashMap<>();

        for (MetadataDef def : defs)
            selected.put(def, allPairs.get(def));

        return selected;
    }

    // ...

    @Override
    public String toString()
    {
        return "(metadata " + allPairs + ")";
    }
}
