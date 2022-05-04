package com.apixio.restbase.apiacl.acctrole;

import java.util.ArrayList;
import java.util.List;

import com.apixio.restbase.apiacl.AccessResult;
import com.apixio.restbase.apiacl.ApiCheck;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.Constants;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.ProtectedApi;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.entity.User;

/**
 * ApiCheckRole records the actual role-check configuration for a specific protected
 * API.
 */
class ApiCheckRole extends ApiCheck {

    private final static String       NO_ROLE = "NO_USER";
    private final static List<String> ROLE_FOR_NO_USER = new ArrayList<String>();
    static
    {
        ROLE_FOR_NO_USER.add(NO_ROLE);
    }

    // general form:  <roleset> = <accessSpecifier>
    // <roleset> is csv of AccountRole enum
    // <accessSpecifier> is:  "allow" or "deny"

    private List<String> roles;
    private String       rolesAsString;   // access logging only
    private boolean      matchAll;
    private boolean      isAllowed;

    ApiCheckRole(ProtectedApi def, String roleSet, String access)
    {
        if (!(isAllowed = access.equalsIgnoreCase(Constants.ACCESS_ALLOW)) && !access.equalsIgnoreCase(Constants.ACCESS_DENY))
            throw new IllegalArgumentException("Unknown access specifier '" + access + "'");

        if (roleSet.equals(Constants.WILDCARD))
        {
            matchAll      = true;
            rolesAsString = "<all roles>";
        }
        else
        {
            roles         = parseRoles(roleSet);
            rolesAsString = roles.toString();
        }
    }

    @Override
    public AccessResult checkPermission(CheckContext ctx, HttpRequestInfo ri, MatchResults match, Object httpEntity)
    {
        boolean allowed;

        if (matchAll)
        {
            ctx.recordDetail("Check user role:  role spec was '*' so no need to check for user's role; allow '*':  {}", isAllowed);
            allowed = isAllowed;
            ctx.recordAccess(allowed, (allowed) ? null : "denied as role-config configured to deny all access");
        }
        else
        {
            List<String> userRoles = getUserRoles();

            allowed = containsAny(userRoles);

            if (!allowed)
            {
                ctx.recordDetail("Check user role:  user roles [{}] isn't in the allowed list [{}]", userRoles, roles);
                ctx.recordAccess(allowed, "denied as user's role [" + userRoles + "] not in allowed list " + rolesAsString);
            }
            else
            {
                ctx.recordAccess(allowed, null);
            }
        }

        return (allowed) ? AccessResult.ALLOW : AccessResult.DISALLOW;
    }

    /**
     *
     */
    private List<String> getUserRoles()
    {
        User  user  = UserUtil.getCachedUser();

        return (user != null) ? user.getRoleNames() : ROLE_FOR_NO_USER;
    }

    private boolean containsAny(List<String> testRoles)
    {
        for (String test : testRoles)
        {
            if (roles.contains(test))
                return true;
        }

        return false;
    }

    private List<String> parseRoles(String set)
    {
        List<String> roles = new ArrayList<String>();

        for (String role : set.split(","))
        {
            if (!role.isEmpty())
                roles.add(role);
        }

        return roles;
    }

}
