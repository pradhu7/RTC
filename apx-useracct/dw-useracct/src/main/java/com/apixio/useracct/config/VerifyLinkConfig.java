package com.apixio.useracct.config;

/**
 * Contains the yaml-poked link verification related configuration:
 *
 *  * linkTimeout is the number of seconds after creation of the link that the user
 *     can still use the link.  This is translated to a Redis TTL so the link actually
 *     will disappear after the timeout
 *
 *  * urlBase is the full "http://host:port/blah/blah/blah" prefix that will be prepended
 *     to the linkID when the full URL is created.
 */
public class VerifyLinkConfig {
    private int    linkTimeout;    // in seconds
    private String urlBase;        // full http://host/...

    public void setLinkTimeout(int ttl)
    {
        this.linkTimeout = ttl;
    }
    public int getLinkTimeout()
    {
        return linkTimeout;
    }

    public void setUrlBase(String urlBase)
    {
        this.urlBase = urlBase;
    }
    public String getUrlBase()
    {
        return urlBase;
    }

    public String toString()
    {
        return ("VerifyLinkConfig:  linkTimeout=" + linkTimeout +
                "; urlBase=" + urlBase
            );
    }

}
