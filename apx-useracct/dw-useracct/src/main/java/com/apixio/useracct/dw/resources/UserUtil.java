package com.apixio.useracct.dw.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.useracct.entity.User;
import com.apixio.useracct.eprops.UserProperties;

/**
 * Utility code shared by multiple resources.
 */
class UserUtil {

    /**
     * Dumb method to get String=>String properties from generalized prop system and copy them
     * over to String=>Object (needed so we can return list of role names)
     */
    static void copyUserProperties(User user, Map<String, Object> json, String prop)
    {
        Map<String, String> tmpJson = new HashMap<String, String>();

        UserProperties.autoProperties.copyFromEntity(user, tmpJson);
        copyTimeoutOverride(user, json);
        copyNeedsTwoFactor(user, json);

        for (Map.Entry<String, String> entry : tmpJson.entrySet())
        {
            if ((prop == null) || prop.equals(entry.getKey()))
                json.put(entry.getKey(), entry.getValue());
        }

        json.put("id",     user.getID().toString());
        json.put("userID", user.getID().toString());  // to support old REST clients...
    }

    private static void copyNeedsTwoFactor(User user, Map<String, Object> json) {
        Boolean needsTwoFactor = user.getNeedsTwoFactor();

        if(needsTwoFactor != null)
            json.put(UserRS.PROP_NEEDSTWOFACTOR, needsTwoFactor.toString());
        else
            json.put(UserRS.PROP_NEEDSTWOFACTOR, "");

    }

    /**
     * Adds given roles (of user) to the json object, as a comma-separated-value list
     */
    static void addRoles(Map<String, Object> json, List<String> roles)
    {
        if ((roles != null) && (roles.size() > 0))
            json.put(UserRS.PROP_USERROLES, roles);
    }

    /**
     * Adds given allowed roles (of user) to the json object, as a comma-separated-value list
     */
    static void addAllowedRoles(Map<String, Object> json, List<String> roles)
    {
        if ((roles != null) && (roles.size() > 0))
            json.put(UserRS.PROP_ALLOWEDROLES, roles);
    }

    /**
     * Dumb method to copy over User.timeoutOverride, which is an Integer (so fails with
     * the current EntityProperties) and needs to not be present if null
     */
    static void copyTimeoutOverride(User user, Map<String, Object> json)
    {
        Integer override = user.getTimeoutOverride();

        if (override != null)
            json.put(UserRS.PROP_TIMEOUTOVERRIDE, override);
    }

}
