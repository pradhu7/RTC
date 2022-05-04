package com.apixio.customer.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.restbase.entity.ParamSet;

/**
 * A Filter is a customer-owned copy of a FilterTemplate that has an independent
 * lifecycle from the template.
 */
public class Filter extends FilterTemplate implements OwnedEntityTrait {

    public static final String OBJTYPE = ObjectTypes.FILTER;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_OWNER     = "customer";  // 'customer' is historical--it should be 'owner' but existing data needs to be restored

    /**
     * Actual Java instance fields
     */
    private XUUID owner;

    /**
     * Create a Filter for the given owner
     */
    public Filter(PatientDataSet owner, String name, String xml)
    {
        super(OBJTYPE, name, xml);

        this.owner = owner.getID();
    }

    /**
     * For restoring from persisted form only
     */
    public Filter(ParamSet fields)
    {
        super(fields);

        this.owner = XUUID.fromString(fields.get(F_OWNER));  // note that we intentionally don't enforce type here
    }

    /**
     * Getters
     */
    @Override
    public XUUID getOwner()
    {
        return owner;
    }

    /**
     * Setters
     */

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_OWNER, owner.toString());
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[Filter: [" + super.toString() + "]" +
                ";ownerID=" + owner +
                "]"
            );
    }

}
