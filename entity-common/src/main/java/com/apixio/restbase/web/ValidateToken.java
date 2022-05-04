package com.apixio.restbase.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.AuthState;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.entity.TokenType;

/**
 * ValidateToken makes sure that the token presented is (still) valid.  For the
 * special case of public URLs (which won't, by definition, require a token, but still
 * could have a token presented), a list of public URL prefixes is kept.
 *
 * Instances of this class validate only one configured token type.
 */
public class ValidateToken extends Microfilter<SysServices>
{
    /**
     * A list of URL prefixes that do NOT require a token to access
     */
    private List<String> pubURLs;

    /**
     * A list of URL prefixes that are accessible to a token that is only partially authenticated.
     */
    private List<String> partialAuthURLs;

    /**
     * The type of token that is required to pass validation checks.
     */
    private TokenType tokenType;

    /**
     * This allows external tokens to be passed via a cookie with the configured name
     */
    private String authCookieName;

    public ValidateToken()
    {
    }

    public ValidateToken(Map<String, Object> filterConfig, SysServices services)
    {
        configure(filterConfig, services);
    }

    /**
     *  Configure the validator by grabbing the type, public and partially-auths URL lists.
     */
    @Override
    public void configure(Map<String, Object> filterConfig, SysServices services)
    {
        super.configure(filterConfig, services);

        tokenType = TokenType.valueOf((String) filterConfig.get("tokenType"));

        // these string constants are under the "tokenConfig" key:
        pubURLs         = (List<String>) filterConfig.get("publicURLs");
        partialAuthURLs = (List<String>) filterConfig.get("partialAuthURLs");
        authCookieName  = (String)       filterConfig.get("authCookieName");

        if (pubURLs == null)
            pubURLs = new ArrayList<String>();

        if (partialAuthURLs == null)
            partialAuthURLs = new ArrayList<String>();
    }

    /**
     * 
     */
    @Override
    public Action beforeDispatch(Context ctx)
    {
        String url    = RestUtil.getRequestPathInfo(ctx.req);
        XUUID  token  = RestUtil.getTokenFromRequest(ctx.req, authCookieName);
        Token  xToken = checkToken(ctx.req.getMethod(), url, token);

        if (isPublicURL(url) || (xToken != null))
        {
            //            System.out.println("Validated token [" + xToken + "] for url [" + url + "]; config'ed token type = " + tokenType);

            if (tokenType == TokenType.EXTERNAL)
            {
                RestUtil.setExternalToken(xToken);
                if (xToken != null)
                    updateTokenTTL(xToken);
            }
            else if (tokenType == TokenType.INTERNAL)
            {
                RestUtil.setInternalToken(xToken);
            }

            RestUtil.setRequestId(UUID.randomUUID().toString());

            return Action.NeedsUnwind;
        }
        else
        {
            ctx.res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401

            return Action.ResponseSent;
        }
    }

    /**
     * After the request has been dispatched we need to clear out the token from the
     * thread locals.
     */
    @Override
    public Action afterDispatch(Context ctx)
    {
        if (tokenType == TokenType.EXTERNAL)
            RestUtil.setExternalToken(null);
        else if (tokenType == TokenType.INTERNAL)
            RestUtil.setInternalToken(null);

        RestUtil.setRequestId(null);

        return Action.NextInChain;
    }

    /**
     * Update the TTL on external tokens.
     */
    private void updateTokenTTL(Token xToken)
    {
        xToken.setLastActivity(new Date());
        services.getTokens().update(xToken);
    }

    /**
     * Checks that the token is still valid and that it meets the required
     * authentication level for the URL.
     */
    private Token checkToken(String method, String url, XUUID tokenID)
    {
        String reason = "";

        // query redis; if token exists then we could also check its use count
        // and if it's too high then we could claim it invalid.  note that this
        // is a different thing than incr/decr on external token.  refcounting
        // on the external token allows us to detect if something grabbed an
        // external token on the externally-facing webserver and is using it
        // without going through the dispatcher of the actual webapp (note that
        // if we record the incoming request IP addr then we could disallow
        // requests to localhost).
        //
        // the refcounting on the internal token basically lets us do the following:
        //  * if we don't decr, then it's a counter of how many API calls are made
        //    in a single request -> too many could be a sign of a problem
        //  * if we decr, then if it's too high (+10?) then we have too many parallel
        //    service calls being made

        if (tokenID != null)
        {
            Token token = services.getTokens().findTokenByID(tokenID);

            if ((token != null) && (token.getTokenType() == tokenType))
            {
                AuthState auth = token.getAuthState();

                if ((auth == AuthState.AUTHENTICATED) ||
                    ((auth == AuthState.PARTIALLY_AUTHENTICATED) && isInPartial(url)) ||
                        ((auth == AuthState.PASSWORD_AUTHENTICATED) && isInPartial(url)))
                {
                    return token;
                }
                else
                {
                    reason = "url not in partialAuthURLs";
                }
            }
            else
            {
                if (token == null)
                    reason = "tokenID not found in redis";
                else
                    reason = "wrong token type (requires " + tokenType + ")";
            }
        }
        else
        {
            reason = "no token";
        }

        System.out.println("ValidateToken(" + tokenID + ") for url [" + method + ":" + url + "] failed due to [" + reason + "]");

        return null;
    }

    /**
     * Return true if the URL starts with one of the public URL prefixes.
     */
    private boolean isPublicURL(String url)
    {
        for (String pub : pubURLs)
        {
            if (url.startsWith(pub))
                return true;
        }

        return false;
    }

    /**
     * Return true if the URL starts with one of the partially authenticated URLs.
     */
    private boolean isInPartial(String url)
    {
        if (partialAuthURLs != null)
        {
            for (String prefix : partialAuthURLs)
            {
                if (url.startsWith(prefix))
                    return true;
            }
        }

        return false;
    }

}
