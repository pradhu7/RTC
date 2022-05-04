package com.apixio.useracct.eprops;

import java.util.HashMap;
import java.util.Map;

import com.apixio.restbase.eprops.E2Properties;
import com.apixio.restbase.eprops.E2Property;
import com.apixio.useracct.entity.RoleSet;
import com.apixio.useracct.entity.RoleSet.RoleType;

public class RoleSetProperties {

    public static final String RST_ORGANIZATION = "organization";
    public static final String RST_PROJECT      = "project";

    private static Map<RoleType, String> roleTypeToString = new HashMap<>();
    static
    {
        roleTypeToString.put(RoleType.ORGANIZATION, RST_ORGANIZATION);
        roleTypeToString.put(RoleType.PROJECT,      RST_PROJECT);
    }

    /**
     * Convenience method to create a Map that can be easily translated to a JSON
     * object.
     */
    public static Map<String, Object> toJson(RoleSet rs)
    {
        Map<String, Object> json = rsProperties.getFromEntity(rs);

        json.put("id",   rs.getID().toString());
        json.put("type", roleTypeToString.get(rs.getRoleType()));

        return json;
    }

    /**
     * The list of automatically handled properties of a RoleSet
     */
    private final static E2Properties<RoleSet> rsProperties = new E2Properties<RoleSet>(RoleSet.class,
        new E2Property("name"),
        new E2Property("description"),
        (new E2Property("nameID")).readonly(true)
        );
}
