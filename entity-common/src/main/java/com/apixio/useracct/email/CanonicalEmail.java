package com.apixio.useracct.email;

/**
 * CanonicalEmail provides email syntax validation and normalization in a new
 * type that lets code not worry about checking again (and again) the String form
 * of an email address.
 */
public class CanonicalEmail {

    private String  addr;

    /**
     * Checks the syntax of the email address and throws an exception if it doesn't
     * conform to the RFCs.
     */
    public CanonicalEmail(String addr)
    {
        if ((addr == null) || ((addr = addr.trim()).length() == 0))
            throw new IllegalArgumentException("Empty email address");
        else if (!EmailAddress.isValidMailbox(addr))
            throw new IllegalArgumentException("Invalid email address [" + addr + "]");

        this.addr = addr.toLowerCase();
    }

    /**
     * Returns normalized/canonicalized address.
     */
    public String getEmailAddress()
    {
        return addr;
    }

    /**
     * Returns the stuff before the "@"
     */
    public String getLocalName()
    {
        return addr.substring(0, addr.indexOf('@'));
    }

    /**
     * Returns the stuff after the "@"
     */
    public String getDomain()
    {
        return addr.substring(addr.indexOf('@') + 1);
    }

    /**
     * This is NOT for debug!  It MUST return addr!
     */
    public String toString()
    {
        return addr;
    }

}
