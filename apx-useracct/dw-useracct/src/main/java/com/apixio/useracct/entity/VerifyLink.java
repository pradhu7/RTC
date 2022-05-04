package com.apixio.useracct.entity;

import java.util.Date;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.util.NonceUtil;

/**
 * VerifyLinks are medium-life entities (e.g., TTL on the order of a day) whose
 * IDs are sent to users (via email messages) in order for the user to confirm
 * some action (e.g., confirming the validity and ownership of an email address).
 */
public class VerifyLink extends BaseEntity {

    public static final String OBJTYPE = ObjectTypes.VERIFY_LINK;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */

    /**
     */
    private final static String F_TYPE             = "type";
    private final static String F_INVALIDAFTER     = "invalid-after";    // 0L means not set
    private final static String F_USERID           = "userID";
    private final static String F_USED             = "used";
    private final static String F_NONCE            = "nonce";

    /**
     * actual fields
     */
    private VerifyType type;
    private long       invalidAfter;  // optional (but would mean a link would never expire); in epoch seconds
    private XUUID      userID;
    private boolean    used;
    private long       nonce;

    /**
     *
     */
    public VerifyLink(VerifyType type, long invalidAfter)
    {
        super(OBJTYPE);

        this.type         = type;
        this.invalidAfter = invalidAfter;
        this.nonce        = NonceUtil.createNonce();
    }

    /**
     * For restoring from persisted form only
     */
    public VerifyLink(ParamSet fields)
    {
        super(fields);

        String val;

        this.type = VerifyType.valueOf(fields.get(F_TYPE));

        if ((val = fields.get(F_INVALIDAFTER)) != null)
            this.invalidAfter = Long.parseLong(val);

        this.userID = XUUID.fromString(fields.get(F_USERID), User.OBJTYPE);
        this.used   = Boolean.valueOf(fields.get(F_USED));
        this.nonce  = Long.valueOf(fields.get(F_NONCE));
    }

    /**
     * Getters
     */
    public VerifyType getVerifyType()
    {
        return type;
    }
    public long getInvalidAfter()
    {
        return invalidAfter;
    }
    public XUUID getUserID()
    {
        return userID;
    }
    public boolean getUsed()
    {
        return used;
    }
    public long getNonce()
    {
        return nonce;
    }

    /**
     * Setters
     */
    public void setUserID(XUUID userID)
    {
        this.userID = userID;
    }
    public void setUsed(boolean used)
    {
        this.used = used;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        putRequired(fields, F_TYPE,    type.toString());

        if (userID != null)
            fields.put(F_USERID, userID.toString());

        fields.put(F_USED,  Boolean.toString(used));
        fields.put(F_NONCE, Long.toString(nonce));
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[VerifyLink: [" + super.toString() + "]" +
                "; type=" + type +
                "; userID=" + userID +
                "; used=" + used +
                "; nonce=" + nonce +
                "]"
            );
    }

}
