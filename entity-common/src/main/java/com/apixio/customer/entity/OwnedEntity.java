package com.apixio.customer.entity;

import com.apixio.XUUID;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 *
 */
public abstract class OwnedEntity extends NamedEntity implements OwnedEntityTrait {

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_OWNER     = "customer";  // 'customer' is historical--it should be 'owner' but existing data needs to be restored

    /**
     * Actual Java instance fields
     */
    private XUUID owner;

    /**
     * Create a OwnedEntity for the given owner
     */
    public OwnedEntity(String objType, XUUID owner, String name)
    {
        super(objType, name);

        this.owner = owner;
    }

    /**
     * For restoring from persisted form only
     */
    public OwnedEntity(ParamSet fields)
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
        return ("[OwnedEntity: [" + super.toString() + "]" +
                ";ownerID=" + owner +
                "]"
            );
    }

}
