package com.apixio.useracct.web;

import java.util.Map;

import com.apixio.restbase.DataServices;
import com.apixio.restbase.web.Microfilter;

/**
 * Entire class has been DEPRECATED (and methods gutted)!
 *
 * CheckAccountRole is a filter that applies configurable access control checks on a per-URL
 * basis.  The user making the request is represented by an INTERNAL TOKEN, so this
 * filter must be after any token exchange filter.  If there is no token (which is
 * the case during authentication) then there are no privileges so if access to a URL is
 * required it must be listed explicitly and must explicitly give permission.
 *
 * The actual permission check is done by checking for a URL match, in configured order,
 * looking for a match, and for each match, testing each specific ACL check.  Both 'allow'
 * (+) and 'deny' (-) can be returned.  Each check can be against the user's role.
 *
 * A proper configuration has the more specific URL prefixes before the more general ones.
 *
 * A user's role is one of:  NOUSER (pseudo role), USER, ADMIN, ROOT.
 *
 * Sample configuration (using syntax of "URL ROLE=+-,...", where ROLE can be "*" for all
 * roles, + means allow, - means deny.  
 *
 *  /user/auth *=+                    # allow all roles into authentication area
 *  /admin     ADMIN=+,ROOT=+         # allow only ADMIN role into /admin URLspace
 *  /su        ROOT=+                 # allow only ROOT role into /su (superuser) URLspace
 *  /          NOUSER=-,*=+           # allow all authenticated users access to all, but disallow to non-authenticated
 *
 * If there is a URL prefix match but no role match within the associated ACLs, then
 * access is denied.  If no prefix matches, then access is denied.
 */
public class CheckAccountRole extends Microfilter<DataServices>
{
    /**
     * 
     */
    @Override
    public void configure(Map<String, Object> filterConfig, DataServices sysServices)
    {
        super.configure(filterConfig, sysServices);
    }

    @Override
    public Action beforeDispatch(Context ctx)
    {
        return Action.NextInChain;
    }

}
