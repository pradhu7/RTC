package com.apixio.aclsys.eprops;

import java.util.Map;

import com.apixio.aclsys.entity.Operation;
import com.apixio.restbase.eprops.EntityProperties;
import com.apixio.restbase.eprops.EntityProperty;

/**
 * OpProperties defines/centralizes the set of properties that are exposed via RESTful methods
 * in a way where the transfer of them can be done automatically while still allowing name flexibility
 * at each level of the system.
 */
public class OpProperties {

    /**
     * It currently isn't possible to modify a role
     */
    public static class ModifyOpParams {
        public String name;
        public String description;

        public String toString()
        {
            return ("ModifyOpParams(name=" + name +
                    ", description=" + description +
                    ")");
        }
    }

    /**
     * The list of automatically handled properties of a Role.
     */
    public final static EntityProperties<ModifyOpParams, Operation> autoProperties = new EntityProperties<ModifyOpParams, Operation>(
        ModifyOpParams.class, Operation.class,
        (new EntityProperty("name")).readonly(true),
        new EntityProperty("description")
        );

}
