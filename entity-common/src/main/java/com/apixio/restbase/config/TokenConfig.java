package com.apixio.restbase.config;

/**
 * TokenConfig holds the yaml-poked token configuration that all RESTful
 * services need (as all talk Token-ish).
 */
public class TokenConfig {
    // all are in seconds
    private int   internalMaxTTL;           // relative:  from time of token creation
    private int   externalMaxTTL;           // relative:  from time of token creation
    private int   externalActivityTimeout;  // deleted if no activity in that # of seconds since last activity

    public void setInternalMaxTTL(int ttl)
    {
        this.internalMaxTTL = ttl;
    }
    public int getInternalMaxTTL()
    {
        return internalMaxTTL;
    }

    public void setExternalMaxTTL(int ttl)
    {
        this.externalMaxTTL = ttl;
    }
    public int getExternalMaxTTL()
    {
        return externalMaxTTL;
    }

    public void setExternalActivityTimeout(int timeout)
    {
        this.externalActivityTimeout = timeout;
    }
    public int getExternalActivityTimeout()
    {
        return externalActivityTimeout;
    }

    public String toString()
    {
        return ("TokenConfig:  intMaxTTL=" + internalMaxTTL +
                "; extMax=" + externalMaxTTL +
                "; extTimeout=" + externalActivityTimeout
            );
    }

}
