package com.apixio.customer.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.ParamSet;

/**
 * A FilterGroup is a customer-owned and named list of Filters.
 */
public class FilterGroup extends OwnedEntity {

    public static final String OBJTYPE = ObjectTypes.FILTER_GROUP;

    /**
     * Actual Java instance fields
     */

    /**
     * Create a FilterGroup for the given customer
     */
    public FilterGroup(XUUID owner, String name)
    {
        super(OBJTYPE, owner, name);
    }

    /**
     * For restoring from persisted form only
     */
    public FilterGroup(ParamSet fields)
    {
        super(fields);
    }

    /**
     * Getters
     */

    /**
     * Setters
     */

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[FilterGroup: [" + super.toString() + "]" +
                "]"
            );
    }

}
