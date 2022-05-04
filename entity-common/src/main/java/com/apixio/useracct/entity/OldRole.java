package com.apixio.useracct.entity;

import java.util.ArrayList;
import java.util.List;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * Role is an attribute of a User.  A role has a name and description and a list of
 * other rolenames that a User with the first role can assign to other Users.
 * This list is referred to as "assignable" roles.
 */
public class OldRole extends BaseEntity {

    /**
     * The predefined role names that the user-account code depends on.  Bootstrapping
     * MUST create structures in Redis for these roles.
     */
    public static final String USER = "USER";

    /**
     * The XUUID prefix typing letter
     */
    public static final String OBJTYPE = ObjectTypes.OLD_ROLE;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_ROLE_NAME       = "name";
    private final static String F_ROLE_DESC       = "description";
    private final static String F_ROLE_ASSIGNABLE = "assignable";      // CSV of role names

    /**
     * Actual fields
     */
    private String        name;
    private String        description;
    private List<String>  assignableRoles;

    /**
     * Create a new Role from the given information
     */
    public OldRole(String name, String description)
    {
        super(OBJTYPE);

        this.name        = name;
        this.description = description;
    }

    /**
     * For restoring from persisted form only
     */
    public OldRole(ParamSet fields)
    {
        super(fields);

        this.name            = fields.get(F_ROLE_NAME);
        this.description     = fields.get(F_ROLE_DESC);
        this.assignableRoles = EntityUtil.unpackRoles(fields.get(F_ROLE_ASSIGNABLE));
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
        return description;
    }
    public List<String> getAssignableRoles()
    {
        return new ArrayList<String>(assignableRoles);
    }

    /**
     * Setters
     */
    public void setDescription(String desc)
    {
        this.description = desc;
    }
    public void addRole(OldRole role)
    {
        if ((role != null) && !assignableRoles.contains(role.getName()))
            this.assignableRoles.add(role.getName());
    }
    public void addRole(String role)
    {
        if ((role != null) && !assignableRoles.contains(role))
            this.assignableRoles.add(role);
    }
    public void setRoles(List<String> roles)
    {
        this.assignableRoles = new ArrayList<String>();

        for (String role : roles)
            addRole(role);
    }
    public void removeRole(OldRole role)
    {
        if (role != null)
            assignableRoles.remove(role.getName());
    }
    public void removeRole(String role)
    {
        if (role != null)
            assignableRoles.remove(role);
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_ROLE_NAME, name);
        fields.put(F_ROLE_DESC, description);
        fields.put(F_ROLE_ASSIGNABLE, EntityUtil.packRoles(assignableRoles));
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[OldRole: [" + super.toString() + "]" +
                "; name=" + name + 
                "; desc=" + description
            );
    }

}
