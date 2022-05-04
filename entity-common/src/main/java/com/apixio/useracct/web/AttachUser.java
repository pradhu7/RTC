package com.apixio.useracct.web;

import java.util.Map;

import com.apixio.restbase.DataServices;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.Microfilter;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.dao.Users;

/**
 * The AttachUser microfilter looks up and attaches the User object for the
 * token that's already attached on the thread.
 */
public class AttachUser extends Microfilter<DataServices>
{
    private Users users;

    /**
     * 
     */
    @Override
    public void configure(Map<String, Object> filterConfig, DataServices sysServices)
    {
        super.configure(filterConfig, sysServices);

        users = sysServices.getUsers();
    }

    @Override
    public Action beforeDispatch(Context ctx)
    {
        Token rToken = RestUtil.getInternalToken();

        if (rToken != null)
            UserUtil.setCachedUser(users.findUserByID(rToken.getUserID()));

        return Action.NeedsUnwind;
    }

    @Override
    public Action afterDispatch(Context ctx)
    {
        UserUtil.setCachedUser(null);

        return Action.NextInChain;
    }


}
