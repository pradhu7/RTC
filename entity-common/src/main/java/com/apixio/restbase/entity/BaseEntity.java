package com.apixio.restbase.entity;

import java.util.Date;

import com.apixio.XUUID;

/**
 * BaseEntity defines the fields that all persisted entities have in common.
 * These common fields are:
 *
 *  ID:      a UUID with a single character prefix that denotes the type (e.g., "U" for user)
 *  domain:  an optional string that declares which unique domain the entity exists in.
 */
public class BaseEntity extends PersistBase
{
    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    protected final static String F_ID          = "id";
    protected final static String F_DOMAIN      = "obj-domain";

    /**
     * actual fields
     */
    private XUUID    id;
    private String   domain;

    /**
     * Create a new BaseEntity from the given information
     */
    protected BaseEntity(String objType)
    {
        this(objType, null);
    }

    protected BaseEntity(String objType, String domain)
    {
        super();

        if ((objType == null) || (objType.length() == 0))
            throw new IllegalArgumentException("Object creation requires a non-empty type specifier");

        if ((domain != null) && domain.trim().length() == 0)
            domain = null;

        this.id     = XUUID.create(objType);
        this.domain = domain;
    }

    /**
     * For restoring from persisted form only
     */
    protected BaseEntity(ParamSet fields)
    {
        super(fields);

        this.id     = XUUID.fromString(fields.get(F_ID));
        this.domain = fields.get(F_DOMAIN);
    }

    /**
     * Getters
     */
    public XUUID getID()
    {
        return id;
    }
    public String getDomain()
    {
        return domain;
    }

    /**
     * Setters
     */

    /**
     * Return a Map of field=value for persisting the object.
     */
    protected void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_ID,     id.toString());
        fields.put(F_DOMAIN, domain);
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("{" + super.toString() + "; BaseEntity: ID=" + id + "; domain=" + domain + "}");
    }

}
