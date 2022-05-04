package com.apixio.restbase.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.utility.StringList;
import com.apixio.ObjectTypes;
import com.apixio.XUUID;

/**
 * Tokens are temporary objects that represent a state of being authenticated.
 * External tokens are longer-lived (typically as long as there is activity using
 * the token) and internal tokens have a fixed lifespan (typically short).
 *
 * Expiration is handled by setting an optional "invalidAfter" value, where the
 * value is in epoch seconds.  Internal tokens will always have that set whereas
 * external tokens might or might not.
 */
public class Token extends BaseEntity {

    private static final String OBJTYPE = ObjectTypes.TOKEN;

    /**
     * Appended to UUID to mark token as internal/external
     */
    public final static String TT_EXTERNAL = OBJTYPE + "A";
    public final static String TT_INTERNAL = OBJTYPE + "B";

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */

    /**
     * Fields common to both external and internal tokens.  Fields that DO start with "_" are
     * to be interpreted as token session fields, where the key name is the tag.  Since ParamSet
     * doesn't allow enumeration of keys (maybe I should add it...) it's necessary to keep
     * the list of tag names in order to be able to restore it
     */
    private final static String F_TYPE             = "type";
    private final static String F_INVALIDAFTER     = "invalid-after";     // 0L means not set
    private final static String F_AUTHSTATE        = "auth-state";
    private final static String F_ACTIVITYTIMEOUT  = "act-timeout-over";  // overrides default

    /**
     * Fields for external tokens
     */
    private final static String F_IPADDR           = "ip-address";
    private final static String F_USERAGENT        = "user-agent";
    private final static String F_USERID           = "userID";
    private final static String F_LASTACTIVITYTIME = "last-activity";
    private final static String F_USECOUNT         = "use-count";
    private final static String F_TAGLIST          = "taglist";           // StringList.flatten()

    /**
     * Fields for internal tokens
     */
    private final static String F_FOREXTERNALID    = "for-extID";

    /**
     * actual fields
     */
    private TokenType tokenType;
    private AuthState authState;
    private long      invalidAfter;       // optional; in epoch seconds
    private int       inactivityOverride; // optional; relative seconds

    private String        ipAddr;
    private String        userAgent;
    private XUUID         userID;
    private Date          lastActivity;
    private int           useCount;

    /**
     * Tags contain session data where each tag is a separate bucket to put session
     * data into.  Session data is probably JSON.
     */
    private List<String>        taglist;
    private Map<String, String> tags;

    private XUUID     forExternal;

    /**
     * Convenience methods
     */
    public static boolean isExternal(XUUID id)
    {
        return (id != null) && id.getType().equals(TT_EXTERNAL);
    }

    public static boolean isInternal(XUUID id)
    {
        return (id != null) && id.getType().equals(TT_INTERNAL);
    }

    /**
     * Create a new token of the given type with the given TTL/expiration
     */
    public Token(TokenType type, long invalidAfter)
    {
        super((type == TokenType.EXTERNAL) ? TT_EXTERNAL : TT_INTERNAL);  //!! it seems like it's useful to know int/ext via just the key

        this.tokenType    = type;
        this.invalidAfter = invalidAfter;
        this.authState    = AuthState.UNAUTHENTICATED;
        this.lastActivity = new Date();
    }


    /**
     * For restoring from persisted form only
     */
    public Token(ParamSet fields)
    {
        super(fields);

        String val;

        // common (note that userID needs to be common because it's possible
        // to request an internal token without an external token--when it's
        // within the internal network...)
        this.tokenType = TokenType.valueOf(fields.get(F_TYPE));
        this.authState = AuthState.valueOf(fields.get(F_AUTHSTATE));
        this.userID    = XUUID.fromString(fields.get(F_USERID));

        if ((val = fields.get(F_INVALIDAFTER)) != null)
            this.invalidAfter = Long.parseLong(val);

        if ((val = fields.get(F_ACTIVITYTIMEOUT)) != null)
            this.inactivityOverride = Integer.parseInt(val);

        if (this.tokenType == TokenType.EXTERNAL)
        {
            this.ipAddr       = fields.get(F_IPADDR);
            this.userAgent    = fields.get(F_USERAGENT);
            this.lastActivity = new Date(Long.parseLong(fields.get(F_LASTACTIVITYTIME)));

            if ((val = fields.get(F_USECOUNT)) != null)
                this.useCount = Integer.parseInt(val);

            if ((val = fields.get(F_TAGLIST)) != null)
            {
                this.taglist = StringList.restoreList(val);
                this.tags    = new HashMap<String, String>(this.taglist.size());

                for (String tag : taglist)
                    this.tags.put(tag, fields.get("_" + tag));
            }
        }

        else if (this.tokenType == TokenType.INTERNAL)
        {
            this.forExternal = XUUID.fromString(fields.get(F_FOREXTERNALID), TT_EXTERNAL);
        }
    }

    /**
     * Getters
     */
    public TokenType getTokenType()
    {
        return tokenType;
    }
    public boolean isExternal()
    {
        return tokenType == TokenType.EXTERNAL;
    }
    public boolean isInternal()
    {
        return tokenType == TokenType.INTERNAL;
    }
    public long getInvalidAfter()
    {
        return invalidAfter;
    }
    public int getInactivityOverride()
    {
        return inactivityOverride;
    }
    public AuthState getAuthState()
    {
        return authState;
    }
    public String getIpAddress()
    {
        return ipAddr;
    }
    public String getUserAgent()
    {
        return userAgent;
    }
    public XUUID getUserID()
    {
        return userID;
    }
    public Date getLastActivity()
    {
        return lastActivity;
    }
    public XUUID getExternalToken()
    {
        return forExternal;
    }
    public int getUseCount()
    {
        return useCount;
    }
    public String getTagValue(String tag)
    {
        return (tags != null) ? tags.get(tag) : null;
    }

    /**
     * Setters
     */
    public void setAuthState(AuthState authState)
    {
        this.authState = authState;
    }
    public void setIpAddress(String ipAddress)
    {
        this.ipAddr = ipAddress;
    }
    public void setUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
    }
    public void setUserID(XUUID userID)
    {
        this.userID = userID;
    }
    public void setLastActivity(Date lastActivity)
    {
        this.lastActivity = lastActivity;
    }
    public void setInactivityOverride(int inactivityOverride)
    {
        this.inactivityOverride = Math.max(0, inactivityOverride);
    }
    public void setExternalToken(XUUID tokenID)
    {
        if (!isExternal(tokenID)) throw new IllegalArgumentException("Attempt to set external tokenID to non-external token [" + tokenID + "]");
        forExternal = tokenID;
    }
    public void setTagValue(String tag, String value)  // assumes, perhaps stupidly (but probably not given the overall app model), only 1 thread
    {
        if (taglist == null)
        {
            taglist = new ArrayList<String>();
            tags    = new HashMap<String, String>();
        }

        if (!taglist.contains(tag))
            taglist.add(tag);

        tags.put(tag, value);
    }

    public void removeTagValue(String tag)
    {
        if (taglist != null)
        {
            taglist.remove(tag);
            tags.remove(tag);
        }
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_TYPE,            tokenType.toString());
        fields.put(F_AUTHSTATE,       authState.toString());
        fields.put(F_INVALIDAFTER,    Long.toString(invalidAfter));
        fields.put(F_ACTIVITYTIMEOUT, Integer.toString(inactivityOverride));

        // special case:  internal tokens created directly (without an external token) need this
        if (userID != null)
            fields.put(F_USERID, userID.toString());

        if (tokenType == TokenType.EXTERNAL)
        {
            fields.put(F_IPADDR,    ipAddr);
            fields.put(F_USERAGENT, userAgent);

            putOptional(fields, F_LASTACTIVITYTIME, lastActivity);

            if (taglist != null)
            {
                fields.put(F_TAGLIST, StringList.flattenList(taglist));

                for (String tag : taglist)
                    fields.put("_" + tag, tags.get(tag));
            }
        }

        else if (tokenType == TokenType.INTERNAL)
        {
            // this test is kind of a hack to handle the ability for the user authentication
            // to return an internal token (which is allowed only when the request comes from
            // within the internal network, however that is defined)
            if (forExternal != null)
                fields.put(F_FOREXTERNALID, forExternal.toString());
        }
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[Token: [" + super.toString() + "]" +
                "; type=" + tokenType +
                "; inactivityOverride=" + inactivityOverride +
                "; authState=" + authState +
                "; ipaddr=" + ipAddr +
                "; useragent=" + userAgent +
                "; userID=" + userID +
                "; lastActivity=" + lastActivity +
                "; forext=" + forExternal
            );
    }

}
