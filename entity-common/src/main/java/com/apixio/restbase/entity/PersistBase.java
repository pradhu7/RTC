package com.apixio.restbase.entity;

import java.util.Date;

import com.apixio.XUUID;

/**
 * PersistBase defines the details of (redis-based) persistable data that are common
 * across entity-based and non-entity-based constructs.
 */
public abstract class PersistBase
{
    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    protected final static String F_CREATEDAT   = "created-at";

    /**
     * actual fields
     */
    private Date createdAt;

    /**
     * Create a new PersistBase from the given information
     */
    protected PersistBase()
    {
        this.createdAt = new Date();
    }

    /**
     * For restoring from persisted form only
     */
    protected PersistBase(ParamSet fields)
    {
        this.createdAt = new Date(Long.parseLong(fields.get(F_CREATEDAT)));
    }

    /**
     * Getters
     */
    public Date getCreatedAt()
    {
        return createdAt;
    }

    /**
     * Setters
     */

    final public ParamSet produceFieldMap()
    {
        ParamSet paramSet = new ParamSet();

        toParamSet(paramSet);

        return paramSet;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    protected void toParamSet(ParamSet fields)
    {
        fields.put(F_CREATEDAT, Long.toString(createdAt.getTime()));
    }

    /**
     * Useful library functions
     */
    protected void putRequired(ParamSet ps, String field, String value)
    {
        if (value == null)
            throw new IllegalStateException("Field [" + field  + "] is required but no value is present; object = " + this.toString());
        ps.put(field, value);
    }
    protected void putOptional(ParamSet ps, String field, Date value)
    {
        if (value != null)
            ps.put(field, Long.toString(value.getTime()));
    }
    protected void putOptional(ParamSet ps, String field, Double value)
    {
        if (value != null)
            ps.put(field, value.toString());
    }
    protected void putOptional(ParamSet ps, String field, Long value)
    {
        if (value != null)
            ps.put(field, value.toString());
    }
    protected void putOptional(ParamSet ps, String field, Boolean value)
    {
        if (value != null)
            ps.put(field, value.toString());
    }
    protected Double getOptDouble(ParamSet ps, String field)
    {
        String dbl = ps.get(field);

        if (dbl == null)
            return null;
        else
            return Double.parseDouble(dbl);
    }
    protected Long getOptLong(ParamSet ps, String field)
    {
        String lv = ps.get(field);

        if (lv == null)
            return null;
        else
            return Long.parseLong(lv);
    }
    protected Date getOptDate(ParamSet ps, String field)
    {
        String epochMs = ps.get(field);

        if (epochMs == null)
            return null;
        else
            return new Date(Long.parseLong(epochMs));
    }
    protected Boolean getOptBool(ParamSet ps, String field)
    {
        String b = ps.get(field);

        if (b == null)
            return null;
        else
            return Boolean.parseBoolean(b);
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("{PersistBase: createdAt=" + createdAt + "}");
    }

}
