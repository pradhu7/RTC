package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * Represents a RoleSet
 */
public class RoleSet extends NamedEntity {

    /**
     * The XUUID prefix typing letter
     */
    public static final String OBJTYPE = ObjectTypes.ROLE_SET;

    /**
     * What thing is allowed to reference this RoleSet?
     */
    public enum RoleType { ORGANIZATION, PROJECT }

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_ROLE_TYPE   = "type";
    private final static String F_NAMEID      = "nameid";

    /**
     * Actual fields
     */
    private RoleType   type;
    private String     nameID;     // a never-changed and unique ID assigned at creation time

    public RoleSet(ParamSet fields)
    {
        super(fields);

        this.type   = RoleType.valueOf(fields.get(F_ROLE_TYPE));
        this.nameID = fields.get(F_NAMEID);
    }

    public RoleSet(RoleType type, String nameID, String name)
    {
        super(OBJTYPE, name);

        this.type   = type;
        this.nameID = nameID;
    }

    /**
     * Getters
     */
    public RoleType getRoleType()
    {
        return type;
    }

    public String getNameID()
    {
        return nameID;
    }

    /**
     * Setters
     */

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_ROLE_TYPE, type.toString());
        fields.put(F_NAMEID,    nameID);
    }

    @Override
    public String toString()
    {
        return ("[ne " + super.toString() +
                "; nameID=" + nameID +
                "; type=" + type +
                "]");
    }

}
