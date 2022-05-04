package com.apixio.restbase.apiacl.acctstate;

import java.util.EnumSet;

import com.apixio.restbase.apiacl.AccessResult;
import com.apixio.restbase.apiacl.ApiCheck;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.Constants;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.ProtectedApi;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.User;

/**
 * ApiCheckState records the actual state-check configuration for a specific protected
 * API.
 */
class ApiCheckState extends ApiCheck {

    // general form:  <stateset> = <accessSpecifier>
    // <stateset> is csv of AccountRole enum
    // <accessSpecifier> is:  "allow" or "deny"

    private EnumSet<AccountState> states;
    private String                statesAsString;  // for access logging only
    private boolean               matchAll;
    private boolean               isAllowed;

    ApiCheckState(ProtectedApi def, String stateSet, String access)
    {
        if (!(isAllowed = access.equalsIgnoreCase(Constants.ACCESS_ALLOW)) && !access.equalsIgnoreCase(Constants.ACCESS_DENY))
            throw new IllegalArgumentException("Unknown access specifier '" + access + "'");

        if (stateSet.equals(Constants.WILDCARD))
        {
            matchAll       = true;
            statesAsString = "<all states>";
        }
        else
        {
            states         = parseStates(stateSet);
            statesAsString = states.toString();
        }
    }

    @Override
    public AccessResult checkPermission(CheckContext ctx, HttpRequestInfo ri, MatchResults match, Object httpEntity)
    {
        boolean allowed;

        if (matchAll)
        {
            ctx.recordDetail("Check user state:  state spec was '*' so no need to check for user's state; allow '*':  {}", isAllowed);
            allowed = isAllowed;
            ctx.recordAccess(allowed, (allowed) ? null : "denied as staste-config configured to deny all access");
        }
        else
        {
            AccountState userState = getUserState();

            allowed = states.contains(userState);

            if (!allowed)
            {
                ctx.recordDetail("Check user state:  user state [{}] isn't in the allowed list [{}]", userState, states);
                ctx.recordAccess(allowed, "denied as user's state [" + userState + "] not in allowed list " + statesAsString);
            }
            else
            {
                ctx.recordAccess(allowed, null);
            }
        }

        return (allowed) ? AccessResult.ALLOW : AccessResult.DISALLOW;
    }

    private AccountState getUserState()
    {
        User         user  = UserUtil.getCachedUser();
        AccountState state = (user != null) ? user.getState() : AccountState.NO_USER;

        // token is not used!

        return state;
    }

    private EnumSet<AccountState> parseStates(String set)
    {
        EnumSet<AccountState> states = EnumSet.noneOf(AccountState.class);

        for (String state : set.split(","))
            states.add(AccountState.valueOf(state));

        return states;
    }

}
