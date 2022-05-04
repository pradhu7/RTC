package com.apixio.useracct.dw.resources;

import javax.servlet.http.Cookie;

/**
 * HACK ALERT:  this code really shouldn't have to deal with cookie
 * stuff in general but here we are...
 */
class CookieUtil
{
    // Due to the `HttpOnly` cookie feature, we also want the server
    // to request that the external token cookie value be invalidated.

    static Cookie externalCookie(String authCookieName, String value, int expirySeconds)
    {
        Cookie cookie = new Cookie(authCookieName, value);

        cookie.setVersion(1);
        cookie.setComment("Apixio External Token");
        cookie.setDomain(".apixio.com");                // this is really gross
        cookie.setPath("/");
        cookie.setMaxAge(expirySeconds);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);

        return cookie;
    }

}
