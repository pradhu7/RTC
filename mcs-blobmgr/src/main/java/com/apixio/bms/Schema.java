package com.apixio.bms;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Schema is just a bunch of MetadataDefs packaged together.
 */
public class Schema
{

    /**
     * Map from name of MetadataDef to actual struct
     */
    private Map<String, MetadataDef> metadatas = new HashMap<>();

    public Schema(MetadataDef... defs)
    {
        for (MetadataDef def : defs)
            addDef(def);
    }

    /**
     * Adds a new metadata def to the set, throwing an exception if there's
     * a name collision with an existing def.
     */
    public void addDef(MetadataDef def)
    {
        if ((metadatas.put(def.getKeyName(), def)) != null)
            throw new IllegalArgumentException("Attempt to add MetadataDef to schema that already has a def with the keyname: " + def.getKeyName());
    }

    /**
     *
     */
    public MetadataDef getDef(String key)
    {
        return metadatas.get(key);
    }

    /**
     *
     */
    public Collection<MetadataDef> metadatas()
    {
        return metadatas.values();
    }

}
