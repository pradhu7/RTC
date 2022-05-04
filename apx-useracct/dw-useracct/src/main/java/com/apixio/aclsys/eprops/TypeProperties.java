package com.apixio.aclsys.eprops;

import java.util.Map;

import com.apixio.aclsys.entity.AccessType;
import com.apixio.restbase.eprops.EntityProperties;
import com.apixio.restbase.eprops.EntityProperty;

/**
 * RoleProperties defines/centralizes the set of properties that are exposed via RESTful methods
 * in a way where the transfer of them can be done automatically while still allowing name flexibility
 * at each level of the system.
 */
public class TypeProperties {

    /**
     * It currently isn't possible to modify a role
     */
    public static class ModifyTypeParams {
        public String name;
        public String description;

        public String toString()
        {
            return ("ModifyTypeParams(name=" + name +
                    ", description=" + description +
                    ")");
        }
    }

    /**
     * The list of automatically handled properties of a Role.
     */
    public final static EntityProperties<ModifyTypeParams, AccessType> autoProperties = new EntityProperties<ModifyTypeParams, AccessType>(
        ModifyTypeParams.class, AccessType.class,
        (new EntityProperty("name")).readonly(true),
        new EntityProperty("description")
        );

}
