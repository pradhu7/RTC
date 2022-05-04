package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * Represents an organization type.  Currently an OrgType has only a reference to a
 * RoleSet (which implies that all actual organizations with the same OrgType will
 * have the same set of Roles that Users can take on within such organization).
 */
public class OrgType extends NamedEntity {

    /**
     * The XUUID prefix typing letter
     */
    public static final String OBJTYPE = ObjectTypes.ORG_TYPE;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_ROLESET      = "roleset";

    /**
     * Actual fields
     */
    private XUUID roleSetID;

    public OrgType(ParamSet fields)
    {
        super(fields);

        String id = fields.get(F_ROLESET);

        if (id != null)
            roleSetID = XUUID.fromString(id, RoleSet.OBJTYPE);
    }

    public OrgType(String name)
    {
        super(OBJTYPE, name);
    }

    /**
     * Getters
     */
    public XUUID getRoleSet()
    {
        return roleSetID;
    }

    /**
     * Setters
     */
    @Override
    public void setName(String name)
    {
        throw new IllegalStateException("OrgType.name is readonly");
    }
    public void setRoleSet(XUUID roleSetID)
    {
        this.roleSetID = roleSetID;
    }

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_ROLESET, roleSetID.toString());
    }

    @Override
    public String toString()
    {
        return ("[orgtype " + super.toString() +
                "; roleSet=" + roleSetID +
                "]");
    }
}
