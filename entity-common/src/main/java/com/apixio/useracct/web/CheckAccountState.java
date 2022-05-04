package com.apixio.useracct.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.apixio.restbase.DataServices;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.Microfilter;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.User;

/**
 * CheckAccountState is a filter that applies configurable account state checks.
 * The user making the request is represented by an INTERNAL TOKEN, so this
 * filter must be after any token exchange filter.  If there is no token (which is
 * the case during authentication) then there are no privileges so if access to a URL is
 * required it must be listed explicitly and must explicitly give permission.
 *
 * The reason for this filter is that some URLs require the account to be in a particular
 * state(s).  For example, an account must be ACTIVE in order to access any URL other
 * than the activate one(s).
 *
 * This also handles the case where accounts are disabled, etc.
 *
 * The actual state check is done by checking for a URL match, in configured order,
 * looking for a match, and for each match, testing each specific state check.  Both 'allow'
 * (+) and 'deny' (-) can be returned.  Each check can be against the user's role.
 *
 * A proper configuration has the more specific URL prefixes before the more general ones.
 *
 * A user's state is one of:  NOUSER (pseudo role), NEW, ACTIVE, DISABLED, AUTO_LOCKOUT, CLOSED
 *
 * Sample configuration (using syntax of "URL STATE=+-,...", where STATE can be "*" for all
 * states, + means allow, - means deny.  
 *
 *  /user/verify NEW=+                    # allow all accounts states into verification area
 *  /user/reset  AUTO_LOCKOUT=+
 *  /            ~ACTIVE=-
 *
 * If there is a URL prefix match but no role match within the associated ACLs, then
 * access is denied.  If no prefix matches, then access is denied.
 */
public class CheckAccountState extends Microfilter<DataServices>
{
    // eventually this should all be pulled from redis for dynamic ACL changes
    // that don't require a restart of the service

    private final static String[] ALL_STATES;

    static
    {
        List<String> names = new ArrayList<String>();

        for (AccountState as : AccountState.values())
            names.add(as.name());

        ALL_STATES = names.toArray(new String[names.size()]);
    }

    private Users users;

    /**
     *
     */
    private AccessSpec stateAccess;

    /**
     * 
     */
    @Override
    public void configure(Map<String, Object> filterConfig, DataServices sysServices)
    {
        super.configure(filterConfig, sysServices);

        stateAccess = new AccessSpec(ALL_STATES, (List<String>) filterConfig.get("access"));
    }

    @Override
    public Action beforeDispatch(Context ctx)
    {
        String     url     = RestUtil.getRequestPathInfo(ctx.req);
        String     method  = ctx.req.getMethod();
        String     state   = getUserState(RestUtil.getInternalToken(ctx.req));
        boolean    allowed;

        // to allow any access that's not more specifically controlled, the last
        // element of the prefixes list must be "/" to match everything (if desired)

        allowed = stateAccess.isAllowed(state, method, url);

        //        System.out.println("CheckAccountState (" + method + ":" + url + "  " + state + "): " + allowed);

        if (allowed)
        {
            return Action.NextInChain;
        }
        else
        {
            ctx.res.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return Action.ResponseSent;
        }
    }

    private String getUserState(Token reqToken)
    {
        User         user  = UserUtil.getCachedUser();
        AccountState state = (user != null) ? user.getState() : AccountState.NO_USER;

        //!! token is not used!

        return state.toString();
    }

}
