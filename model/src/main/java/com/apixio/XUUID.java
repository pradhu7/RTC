package com.apixio;

import java.util.UUID;

/**
 * An eXtended UUID is basically a tagged UUID that prefixes a normal UUID with
 * a short string/letter type-indicator.
 *
 * The type of the identifier indicates the type of what is identified.
 * It should generally be a single (uppercase) letter but can be "" or
 * multiple letters (in special cases).
 */
public class XUUID {

    private static final String SEP_AS_STRING = "_";
    private static final char   SEP_AS_CHAR   = '_';

    /**
     * The actual identifier value.
     */
    private String xuuid;   // This includes the type
    private String type;

    /**
     * Purely optimization for getUUID():  cache it so we don't recreate every time
     */
    private UUID uuid;

    /**
     * Disallow construction by client code.
     */
    private XUUID()
    {
    }

    /**
     * This constructor will help to deserialize JSON object of form
     * "projectDataSetUuid": {
     * 		"type": "DS",
     * 		"uuid": "c9bfee50-30e6-434c-9733-8d05d7b412c2",
     * 		"id": "DS_c9bfee50-30e6-434c-9733-8d05d7b412c2"
     *  }
     *  into an XUUID object automatically when using json4s.
     *
     *  Note: it should not be used to construct an
     *  XUUID object manually.
     */
    public XUUID(String type, String uuid, String id)
    {
        this.type = type;
        this.uuid = UUID.fromString(uuid);
        this.xuuid = id;
    }

    /**
     * Create an XUUID of the given type.  The type can be null or "" if it is
     * to be just a UUID (mostly for backwards compatibility).
     */
    public static XUUID create(String type)
    {
        UUID uuid  = UUID.randomUUID();

        return create(type, uuid, false, false);
    }

    /**
     * Create an XUUID of the given type and uuid.  The type can be null or "" if it is
     * to be just a UUID.
     *
     * This method is for backwards compatibility with existing DAOs.
     */
    public static XUUID create(String type, UUID uuid, boolean typeIncludesSeparator, boolean obeyTypeCase)
    {
        // this saves a lot of headache as some code pass uuid as null.
        if (uuid == null)
            return null;

        XUUID xuuid = new XUUID();

        if (type != null)
        {
            if ((type = type.trim()).length() == 0)
                type = null;
            else
                type = obeyTypeCase ? type : type.toUpperCase();

            xuuid.type = type;
        }

        if (type != null)
            type = typeIncludesSeparator ? type : type + SEP_AS_STRING;
        else
            type = "";

        xuuid.xuuid = type + uuid.toString();

        return xuuid;
    }

    /**
     * Construct an XUUID from a String (that should have been retrieved via
     * the .toString() method).
     */
    public static XUUID fromString(String xuuid)
    {
        // this saves a lot of headache as some code does a fromString() on the
        // return from map.get().
        if (xuuid == null)
            return null;

        // let's ensure that we have a syntactically valid ID

        XUUID recon = new XUUID();
        int   co    = xuuid.indexOf(SEP_AS_CHAR);

        recon.xuuid = xuuid;

        if (co >= 0)
        {
            if (co + 1 >= xuuid.length())
                throw new IllegalArgumentException("Malformed XUUID '" + xuuid + "'");

            UUID.fromString(xuuid.substring(co + 1));  // syntax check only
            recon.type = xuuid.substring(0, co);
        }
        else  // just do a simple syntax check
        {
            UUID.fromString(xuuid);
        }

        return recon;
    }

    /**
     * Construct an XUUID from a String (that should have been retrieved via
     * the .toString() method) and check that it is of the given type.  The
     * check will succeed if the String is null, for convenience.
     */
    public static XUUID fromString(String xuuid, String requiredType)
    {
        XUUID x = fromString(xuuid);

        if (x != null)
            x.assertType(requiredType);

        return x;
    }

    /**
     * Construct an XUUID from a String (that should have been retrieved via
     * the .toString() method), and force the type of the final XUUID to be
     * the passed in type.  If type is null or "", then a prefix-free XUUID
     * is created
     */
    public static XUUID fromStringWithType(String xuuid, String forcedType)
    {
        // this saves a lot of headache as some code does a fromString() on the
        // return from map.get().
        if (xuuid == null)
            return null;

        // let's ensure that we have a syntactically valid ID

        XUUID recon = new XUUID();
        int   co    = xuuid.indexOf(SEP_AS_CHAR);

        if (co >= 0)
        {
            if (co + 1 >= xuuid.length())
                throw new IllegalArgumentException("Malformed XUUID '" + xuuid + "'");

            UUID.fromString(xuuid.substring(co + 1));  // syntax check only

            xuuid = xuuid.substring(co + 1);  // drop prefix
        }
        else  // just do a simple syntax check
        {
            UUID.fromString(xuuid);
        }

        if ((forcedType == null) || ((forcedType = forcedType.trim()).length() == 0))
        {
            recon.type = "";
            recon.xuuid = xuuid;
        }
        else
        {
            recon.type  = forcedType;
            recon.xuuid = forcedType + SEP_AS_STRING + xuuid;
        }

        return recon;
    }

    /**
     * Construct an UUID from a String (that should have been retrieved via
     * the .toString() method).
     */
    public static UUID fromXUUID(String xuuid, char sep)
    {
        // this saves a lot of headache as some code does a fromString() on the
        // return from map.get().
        if (xuuid == null)
            return null;

        // let's ensure that we have a syntactically valid ID
        int co = (sep != '\0') ? xuuid.indexOf(sep) :  xuuid.indexOf(SEP_AS_CHAR);

        UUID uuid;
        if (co >= 0)
        {
            if (co + 1 >= xuuid.length())
                throw new IllegalArgumentException("Malformed XUUID '" + xuuid + "'");

            uuid = UUID.fromString(xuuid.substring(co + 1));
        }
        else  // just do a simple syntax check
        {
            uuid = UUID.fromString(xuuid);
        }

        return uuid;
    }

    /**
     * Construct an UUID from XUUID.
     */
    public static UUID fromXUUID(XUUID xuuid, char sep)
    {
        return xuuid == null ? null : fromXUUID(xuuid.getID(), sep);
    }

    /**
     * Returns the UUID part of XUUID including type.
     */
    public final String getID()
    {
        return xuuid;
    }

    /**
     * Extract out the UUID portion and return it as a UUID
     */
    public UUID getUUID()
    {
        if (uuid == null)
        {
            int len = (type != null) ? type.length() : 0;

            uuid = UUID.fromString((len == 0) ? xuuid : xuuid.substring(len + 1));
        }

        return uuid;
    }

    /**
     * Returns the type of the XUUID.  Null is returned if there is no type.
     */
    public String getType()
    {
        return type;
    }

    /**
     * Checks that the actual type of the ID is what is declared.
     */
    public void assertType(String requiredType)
    {
        if ((requiredType != null) && !requiredType.equals(type))
            throw new IllegalArgumentException("XUUID type mismatch:  expected [" + requiredType +
                                               "] but identifier has type [" + type + "]");
    }

    /**
     * Be a good Java citizen by implementing these methods.
     */
    public boolean equals(Object obj)
    {
        if ((obj == null) || !(obj instanceof XUUID))
            return false;

        return ((XUUID) obj).xuuid.equals(this.xuuid);
    }

    public int hashCode()
    {
        return xuuid.hashCode();
    }

    public String toString()
    {
        return xuuid;
    }

    public static void main(String[] args)
    {
        for (String a : args)
            System.out.println(a + " -> " + XUUID.fromString(a).getUUID());
    }
}
