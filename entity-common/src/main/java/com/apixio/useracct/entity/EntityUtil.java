package com.apixio.useracct.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Class of static utility methods
 */
public class EntityUtil {

    /**
     * Parse the packed role names into a List.
     */
    public static List<String> unpackRoles(String csv)
    {
        List<String> roles = new ArrayList<String>();

        if ((csv != null) && (csv.length() > 0))
        {
            for (String role : csv.split(","))
                roles.add(role);
        }

        return roles;
    }

    /**
     * Pack the packed role names into a List.
     */
    public static String packRoles(List<String> roleNames)
    {
        StringBuilder sb = new StringBuilder();

        for (String name : roleNames)
        {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(name);
        }

        return sb.toString();
    }

}
