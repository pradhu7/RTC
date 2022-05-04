package com.apixio.aclsys.entity;

import java.util.ArrayList;
import java.util.List;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * Operation represents a high-level, application-defined set of access types that
 * can be granted to Subjects to grant access to services and data.
 *
 * Operations are generally referenced at the code level by their name, which must
 * be compatible with Java identifier syntax (no special chars is the big thing...).
 */
public class Operation extends NamedEntity {

    /**
     * The XUUID prefix typing letter
     */
    public static final String OBJTYPE = ObjectTypes.OPERATION;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_OP_ACCESSTYPES = "access";
    private final static String F_OP_APPLIESTO   = "applies-to";  // an optional comma-separated list of allowed ACL targets

    /**
     * Actual fields
     */
    private List<String>  accessTypes;
    private String        appliesTo;

    /**
     * Create a new AccessType from the given information
     */
    public Operation(String name)
    {
        super(OBJTYPE, name);

        this.accessTypes = new ArrayList<String>();
    }

    /**
     * For restoring from persisted form only
     */
    public Operation(ParamSet fields)
    {
        super(fields);

        this.accessTypes = unpackTypes(fields.get(F_OP_ACCESSTYPES));
        this.appliesTo   = fields.get(F_OP_APPLIESTO);
    }

    /**
     * Getters
     */
    public List<String> getAccessTypes()
    {
        return new ArrayList<String>(accessTypes);
    }
    public String getAppliesTo()
    {
        return appliesTo;
    }

    /**
     * Setters
     */
    public void setName(String name)
    {
        throw new IllegalStateException("Changing the name of an Operation is not allowed");
    }
    public void setAppliesTo(String appliesTo)
    {
        this.appliesTo = (appliesTo != null) ? appliesTo.trim() : null;
    }
    public void addAccessType(AccessType type)
    {
        if ((type != null) && !accessTypes.contains(type.getName()))
            this.accessTypes.add(type.getName());
    }
    public boolean removeAccessType(AccessType type)
    {
        if (type != null)
            return accessTypes.remove(type.getName());
        else
            return false;
    }
    public boolean removeAccessType(String type)
    {
        if (type != null)
            return accessTypes.remove(type);
        else
            return false;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_OP_ACCESSTYPES, packTypes());
        fields.put(F_OP_APPLIESTO,   appliesTo);
    }

    /**
     * Parse the packed type names into a List.
     */
    private List<String> unpackTypes(String csv)
    {
        List<String> types = new ArrayList<String>();

        if ((csv != null) && (csv.length() > 0))
        {
            for (String type : csv.split(","))
                types.add(type);
        }

        return types;
    }

    /**
     * Pack the packed type names into a List.
     */
    private String packTypes()
    {
        StringBuilder sb = new StringBuilder();

        if (accessTypes != null)
        {
            for (String name : accessTypes)
            {
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(name);
            }
        }

        return sb.toString();
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[AccessType: [" + super.toString() + "]" +
                "; name=" + getName() + 
                "; desc=" + getDescription() +
                "; types=" + accessTypes +
                "; appliesto=" + appliesTo
            );
    }

}
