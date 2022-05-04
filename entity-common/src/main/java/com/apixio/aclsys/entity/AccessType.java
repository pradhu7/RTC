package com.apixio.aclsys.entity;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * AccessType represents a low-level and generic access to data or services.  ReadData
 * and WriteData are good examples.
 */
public class AccessType extends NamedEntity {

    /**
     * The XUUID prefix typing letter
     */
    public static final String OBJTYPE = ObjectTypes.ACCESS_TYPE;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */

    /**
     * Actual fields
     */

    /**
     * Create a new AccessType from the given information
     */
    public AccessType(String name)
    {
        super(OBJTYPE, name);
    }

    /**
     * For restoring from persisted form only
     */
    public AccessType(ParamSet fields)
    {
        super(fields);
    }

    /**
     * Getters
     */

    /**
     * Setters
     */
    public void setName(String name)
    {
        throw new IllegalStateException("Changing the name of an AccessType is not allowed");
    }

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
        return ("[AccessType: [" + super.toString() + "]" +
                "; name=" + getName() + 
                "; desc=" + getDescription()
            );
    }

}
