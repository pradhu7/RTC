package com.apixio.useracct.eprops;

import java.util.Map;

import com.apixio.restbase.eprops.E2Properties;
import com.apixio.restbase.eprops.E2Property;
import com.apixio.useracct.entity.Role;

public class RoleProperties {

    /**
     * Convenience method to create a Map that can be easily translated to a JSON
     * object. 
     */
    public static Map<String, Object> toJson(Role role)
    {
        Map<String, Object> json = roleProperties.getFromEntity(role);

        json.put("id", role.getID().toString());

        return json;
    }

    /**
     * The list of automatically handled properties of a RoleSet
     */
    private final static E2Properties<Role> roleProperties = new E2Properties<Role>(Role.class,
        new E2Property("name"),
        new E2Property("description")
        );
}
