package com.apixio.restbase.entity;

import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * A NamedEntity exposes both a name and an optional description and is intended
 * to be used as base class for any persisted entity that needs a name and description,
 * where the name is editable and doesn't need to be unique.
 */
public abstract class NamedEntity extends BaseEntity {

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    protected final static String F_NAME    = "name";   // trimmed, but that's all
    protected final static String F_DESC    = "desc";   // trimmed, but that's all

    /**
     * actual fields
     */
    private String  name;
    private String  desc;

    /**
     * Create a new entity with a new XUUID with the given name and optional description.
     */
    protected NamedEntity(String objType, String name)
    {
        this(objType, null, name);
    }

    protected NamedEntity(String objType, String domain, String name)
    {
        super(objType, domain);

        this.name = normalizeName(name);

        if ((this.name == null) || (this.name.length() == 0))
            throw new IllegalArgumentException("Name of NamedEntity can't be null or empty");
    }

    /**
     * For restoring from persisted form only
     */
    protected NamedEntity(ParamSet fields)
    {
        super(fields);

        this.name = fields.get(F_NAME);
        this.desc = fields.get(F_DESC);
    }

    /**
     * Testers
     */
    public static boolean eqName(String name, ParamSet fields)
    {
        if (name == null)
            return false;
        else
            return normalizeName(name).equals(fields.get(F_NAME));
    }

    /**
     * Getters
     */
    public String getName()
    {
        return name;
    }
    public String getDescription()
    {
        return desc;
    }

    /**
     * Setters
     */
    public void setName(String name)
    {
        this.name = normalizeName(name);
    }
    public void setDescription(String desc)
    {
        this.desc = normalizeDesc(desc);
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_NAME, name);
        fields.put(F_DESC, desc);
    }

    /**
     * Canonical stuff
     */
    public static String normalizeName(String name)
    {
        if (name != null)
            return name.trim();
        else
            return null;
    }
    public static String normalizeDesc(String desc)
    {
        if (desc != null)
            return desc.trim();
        else
            return null;
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[NamedEntity: [" + super.toString() + "]" +
                ";name=" + name +
                ";desc=" + desc +
                "]"
            );
    }

}

