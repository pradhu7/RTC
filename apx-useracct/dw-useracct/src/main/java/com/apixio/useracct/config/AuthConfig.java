package com.apixio.useracct.config;

/**
 * Contains the yaml-poked authentication-related configuration:
 *
 *  * internalIP is a java.util.regex.Pattern-compatible regex that defines what
 *     an internal IP address looks like.  Internal IPs are allowed to directly
 *     create internal tokens during authentication
 *
 *  * internalAuthTTL gives the default number of seconds that an internal token
 *     is allowed to exist if the authentication call (that requests an internal
 *     token) doesn't specify.  It should be much larger than the TTL for an
 *     internal token that was created as part of an exchange from an external one.
 *
 *  * maxFailedLogins gives the maximum number of login failures that are allowed
 *     before the account is auto-locked out.  If it is set to 0 then any number
 *     of failed logins are allowed
 *
 *  * failedLoginDelayFactor gives the factor by which a failed login will increase
 *     its delay before returning.  The delay is this configured (float) number
 *     multiplied by the number of failed login attempts.  If it is 0. then there
 *     will be no delay added to the authentication when it fails.
 */
public class AuthConfig {
    
    private String internalIP;
    private int    internalAuthTTL;   // default if not specified in web request
    private int    maxFailedLogins;
    private double failedLoginDelayFactor;
    private String authCookieName;

    public void setInternalIP(String pattern)
    {
        this.internalIP = pattern;
    }
    public String getInternalIP()
    {
        return internalIP;
    }

    public void setInternalAuthTTL(int ttl)
    {
        this.internalAuthTTL = ttl;
    }
    public int getInternalAuthTTL()
    {
        return internalAuthTTL;
    }

    public void setMaxFailedLogins(int mfl)
    {
        this.maxFailedLogins = mfl;
    }
    public int getMaxFailedLogins()
    {
        return maxFailedLogins;
    }

    public void setFailedLoginDelayFactor(double factor)
    {
        this.failedLoginDelayFactor = factor;
    }
    public double getFailedLoginDelayFactor()
    {
        return failedLoginDelayFactor;
    }

    public void setAuthCookieName(String name)
    {
        this.authCookieName = name;
    }
    public String getAuthCookieName()
    {
        return authCookieName;
    }

}
