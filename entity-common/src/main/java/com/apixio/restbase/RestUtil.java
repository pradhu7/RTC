package com.apixio.restbase;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.Microfilter.Context;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.entity.User;

/**
 * RestUtil provides APi-specific support for REST-based API services.
 */
public class RestUtil {

    /**
     * The name of the HTTP header where the auth token is passed.  The name
     * is the HTTP standard "Authorization" header.
     */
    public static final String HTTP_AUTHHEADER = "Authorization";

    /**
     * The name of the User-Agent header, as defined in the JAX-RS code
     */
    public static final String HTTP_USERAGENT = "User-Agent";

    /**
     * X-Forwarded-For is the de-facto standard for passing along IP address from
     * a proxy.  X-Forwarded-User-Agent is (now) the Apixio standard for passing
     * along the User-Agent value via HTTP headers.
     */
    public static final String HTTP_XFF_IP = "X-Forwarded-For";
    public static final String HTTP_XFF_UA = "X-Forwarded-User-Agent";

    /**
     * (Unique) attribute names for putting data on the HttpServletRequest
     */
    private static final String REQATTR_EXTERNALTOKEN = "com.apixio.restbase.externalToken";
    private static final String REQATTR_INTERNALTOKEN = "com.apixio.restbase.internalToken"; // also referred to as request token

    /**
     * Thread locals to keep track of information.
     */
    private static final ThreadLocal<Token>   tlExternalToken   = new InheritableThreadLocal<Token>();
    private static final ThreadLocal<Token>   tlInternalToken   = new InheritableThreadLocal<Token>();
    private static final ThreadLocal<Context> tlDispatchContext = new InheritableThreadLocal<Context>();
    private static final ThreadLocal<String>  tlRequestId       = new InheritableThreadLocal<String>();

    /**
     * The method of authorization (authentication, really).  This is in contrast to the
     * normal "Basic" method.  A well-formed request to an Apixio service will include
     * something like the following as an HTTP header:
     *
     *  Authorization: Apixio 28aad2d0-7e2c-4095-a06b-5daa65ef3575a
     *
     * Any other format will fail token validation for syntax alone.
     */
    public  static final String AUTH_METHOD     = "Apixio ";  // note the trailing space char
    private static final int    AUTH_METHOD_LEN = AUTH_METHOD.length();

    /**
     * Retrieve the IP address of the client, giving a preference to forwards first
     * (conceptually the remoteAddr of the request belongs at the end of the
     * X-Forwarded-For header; see https://en.m.wikipedia.org/wiki/X-Forwarded-For for
     * info).
     *
     * Note that because of the possibility that the request went through one or more
     * proxies, the string returned could be a comma-separated list, with the real
     * client IP being the first element.
     */
    public static String getClientIpAddress(HttpServletRequest req)
    {
        String ip = req.getHeader(HTTP_XFF_IP);

        if (ip == null)
            ip = req.getRemoteAddr();

        return ip;
    }

    /**
     * Retrieve the UserAgent of the client, giving a preference to forwards first
     */
    public static String getClientUserAgent(HttpServletRequest req)
    {
        String ua = req.getHeader(HTTP_XFF_UA);

        if (ua == null)
            ua = req.getHeader(HTTP_USERAGENT);

        return ua;
    }

    /**
     * Set/get tokens on threads and HttpServletRequest objects
     */
    public static void setExternalToken(HttpServletRequest req, Token token)
    {
        req.setAttribute(REQATTR_EXTERNALTOKEN, token);
    }
    public static void setExternalToken(Token token)  // token will be null to clear
    {
        tlExternalToken.set(token);
    }
    public static void setInternalToken(HttpServletRequest req, Token token)
    {
        req.setAttribute(REQATTR_INTERNALTOKEN, token);
    }
    public static void setInternalToken(Token token)  // token will be null to clear
    {
        tlInternalToken.set(token);
    }
    public static void setRequestId(String id)        // id will be null to clear
    {
        tlRequestId.set(id);
    }
    public static Token getExternalToken(HttpServletRequest req)
    {
        return (Token) req.getAttribute(REQATTR_EXTERNALTOKEN);
    }
    public static Token getExternalToken()
    {
        return tlExternalToken.get();
    }
    public static Token getInternalToken(HttpServletRequest req)
    {
        return (Token) req.getAttribute(REQATTR_INTERNALTOKEN);
    }
    public static Token getInternalToken()
    {
        return tlInternalToken.get();
    }
    public static String getRequestId()
    {
        return tlRequestId.get();
    }

    /**
     * Because of the differences between at least Jetty and Tomcat in how they
     * interpret part of the Servlet spec, we need to deal with how to get the
     * request path from the HttpServletRequest (at least within a filter).
     */
    public static String getRequestPathInfo(HttpServletRequest req)
    {
        String servletPath = req.getServletPath(); // will be "" in jetty
        String pathInfo    = req.getPathInfo();    // will be null in tomcat

        if (pathInfo == null)
            return servletPath;
        else
            return servletPath + pathInfo;
    }

    /**
     * Extracts the authorization header from the request, validates that it's an
     * Apixio-compatible header and returns the token value.
     *
     * Null is returned if the header isn't present or if it's not an Apixio-compatible
     * header.
     */
    public static XUUID getTokenFromRequest(HttpServletRequest req)
    {
        return getTokenFromHeaderValue(req.getHeader(HTTP_AUTHHEADER));
    }

    /**
     * This form of retrieving a token will also check for the given cookie
     * and use its value (if it can be converted to an XUUID).  The value in
     * the HTTP header takes precedence over a cookie value.
     */
    public static XUUID getTokenFromRequest(HttpServletRequest req, String cookieName)
    {
        XUUID token = getTokenFromRequest(req);

        if ((cookieName != null) && (token == null))
        {
            Cookie[] cookies = req.getCookies();

            if (cookies != null)
            {
                for (Cookie cookie : cookies)
                {
                    if (cookie.getName().equals(cookieName))
                    {
                        try
                        {
                            token = XUUID.fromString(cookie.getValue());
                        }
                        catch (Exception x)
                        {
                            // willful ignorance
                        }

                        break;
                    }
                }
            }
        }

        return token;
    }

    /**
     * Extracts the authorization header from the request, validates that it's an
     * Apixio-compatible header and returns the token value.
     *
     * Null is returned if the header isn't present or if it's not an Apixio-compatible
     * header.
     */
    public static XUUID getTokenFromHeaderValue(String auth)  // auth is value of Authorization: HTTP header
    {
        if (auth != null)
        {
            auth = auth.trim();  //?? is this required??

            if (auth.startsWith(AUTH_METHOD))
            {
                auth = auth.substring(AUTH_METHOD_LEN).trim();
                if (auth.length() == 0)
                    auth = null;
            }
            else
            {
                auth = null;
            }
        }

        if (auth != null)
            return XUUID.fromString(auth);
        else
            return null;
    }

    /**
     * Get some common fields for resource endpoints.
     */
    public static Map<String,Object> getCommonResourceInfo(HttpServletRequest req)
    {
        User                   user = UserUtil.getCachedUser();
        HashMap<String,Object> m    = new HashMap<String,Object>();

        m.put("user",       (user != null) ? user.getEmailAddr() : "");
        m.put("user_agent", getClientUserAgent(req));
        m.put("ip", "ip-" + getClientIpAddress(req).replace('.', ','));

        return m;
    }

    /**
     * Sets the dispatch context for error setting purposes.
     */
    public static void setDispatchContext(Context ctx)
    {
        tlDispatchContext.set(ctx);
    }

    /**
     * Flag the current request as having failed; this will abort redis transactions
     */
    public static void setRequestError(boolean error)
    {
        Context ctx = tlDispatchContext.get();

        if (ctx != null)
            ctx.error = error;
    }
}
