package com.apixio.restbase.web;

import java.util.Map;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;

/**
 * XTokenExchange creates a short-lived internal token given an external token.
 * The intent of this is to centralize and automate this exchange so that actual
 * REST API services get it right, and so that automated detection of attacks
 * can be feasibly done.
 */
public class XTokenExchange extends Microfilter<SysServices>
{
    /**
     * Default constructor needed for bootup with dropwizard
     */
    public XTokenExchange()
    {
    }

    /**
     * Needed for Spring bootup
     */
    public XTokenExchange(Map<String, Object> filterConfig, SysServices services)
    {
        configure(filterConfig, services);
    }

    /**
     * Gets an internal token and sets it on threads/request
     */
    @Override
    public Action beforeDispatch(Context ctx)
    {
        XUUID  xToken = RestUtil.getTokenFromRequest(ctx.req);
        Token  rToken = getRequestToken(xToken);

        // not sure if i need to set on both:
        RestUtil.setInternalToken(rToken);
        RestUtil.setInternalToken(ctx.req, rToken);

        return Action.NeedsUnwind;
    }

    /**
     * Deletes the internal token and clears it on threads/request.
     */
    @Override
    public Action afterDispatch(Context ctx)
    {
        XUUID  xToken = RestUtil.getTokenFromRequest(ctx.req);
        Token  rToken = RestUtil.getInternalToken(ctx.req);

        if (rToken != null)
            services.getTokens().delete(rToken);

        RestUtil.setInternalToken(null);    
        
        return Action.NextInChain;
    }

    /**
     * Create the short-lived request (internal) token from the ID of the external token.
     */
    private Token getRequestToken(XUUID xToken)
    {
        if (xToken == null)
            return null;
        else
            return services.getTokens().createInternalToken(xToken);
    }

}
