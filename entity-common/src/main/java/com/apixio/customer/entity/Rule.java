package com.apixio.customer.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.ParamSet;

/**
 * A Rule is a customer-owned and named list of Filters.
 */
public class Rule extends OwnedEntity {

    public static final String OBJTYPE = ObjectTypes.RULE;

    /**
     * Create a Rule for the given customer
     */
    public Rule(XUUID owner, String name)
    {
        super(OBJTYPE, owner, name);
    }

    /**
     * For restoring from persisted form only
     */
    public Rule(ParamSet fields)
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
        return ("[Rule: [" + super.toString() + "]" +
                "]"
            );
    }

}
