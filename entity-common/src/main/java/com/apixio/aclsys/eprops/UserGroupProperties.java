package com.apixio.aclsys.eprops;

import java.util.Map;

import com.apixio.restbase.eprops.EntityProperties;
import com.apixio.restbase.eprops.EntityProperty;
import com.apixio.aclsys.entity.UserGroup;

public class UserGroupProperties {

    /**
     */
    public static class ModifyUserGroupParams {
        // automatically handled properties:
        public String name;

        public void copyFrom(UserGroup group)
        {
            autoProperties.copyFromEntity(group, this);
        }

        public void copyFrom(Map<String, String> props)
        {
            autoProperties.copyToModifyParams(props, this);
        }

        public String toString()
        {
            return ("ModifyUserGroup(name=" + name + ")");
        }
    }

    /**
     * The list of automatically handled properties of a UserGroup
     */
    public final static EntityProperties<ModifyUserGroupParams, UserGroup> autoProperties = new EntityProperties<ModifyUserGroupParams, UserGroup>(
        ModifyUserGroupParams.class, UserGroup.class,
        (new EntityProperty("name")).readonly(true)
        );

}
