package com.apixio.useracct.dw.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.aclsys.entity.UserGroup;
import com.apixio.aclsys.eprops.UserGroupProperties;

/**
 * Utility code
 */
class UserGroupUtil {

    /**
     * Dumb method to get String=>String properties from generalized prop system and copy them
     * over to String=>Object (needed so we can return list of role names)
     */
    static void copyUserGroupProperties(UserGroup userGroup, Map<String, Object> json)
    {
        Map<String, String> tmpJson = new HashMap<String, String>();

        UserGroupProperties.autoProperties.copyFromEntity(userGroup, tmpJson);

        for (Map.Entry<String, String> entry : tmpJson.entrySet())
            json.put(entry.getKey(), entry.getValue());
    }

}
