package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.utility.StringList;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * Organization is an entity that contains users and is used in role-based access
 * control.
 */
public class Organization extends NamedEntity {

    public static final String OBJTYPE = ObjectTypes.ORGANIZATION;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_ORG_TYPE          = "type";         // from list of OrgTypes in redis
    private final static String F_ORG_EXTERNALID    = "external-id";
    private final static String F_ORG_PASSPOLICY    = "password-policy";
    private final static String F_ORG_ISACTIVE      = "is-active";
    private final static String F_MAX_FAILED_LOGINS = "max-failed-logins";
    private final static String F_ALLOWED_CIDRS     = "allowed-cidrs";     // whitelist of IP/CIDR
    private final static String F_ACTIVITY_TO_OVER  = "activity-timeout-override";
    private final static String F_NEEDS_TWO_FACTOR  = "needs-two-factor";

    /**
     * actual fields
     */
    private XUUID       typeID;
    private boolean     isActive;
    private String      externalOrgID;
    private String      passwordPolicy;
    private Integer     maxFailedLogins;
    private String[]    allowedCidrs;
    private int         activityTimeoutOverride;   // added April 2019; default of 0 means no override
    private boolean     needsTwoFactor;             //

    /**
     * Create a new Organization from the given information
     */
    public Organization(String name, XUUID typeID, String orgID)
    {
        super(OBJTYPE, name);

        this.typeID        = typeID;
        this.isActive      = true;
        this.externalOrgID = orgID;
    }

    /**
     * For restoring from persisted form only
     */
    public Organization(ParamSet fields)
    {
        super(fields);

        this.typeID   = XUUID.fromString(fields.get(F_ORG_TYPE));
        this.isActive = Boolean.parseBoolean(fields.get(F_ORG_ISACTIVE));

        commonConstruction(fields);
    }

    /**
     * This constructor is for migration from UserOrg to Organization, where we can restore
     * from the fields of a UserOrg HASH but we need to add the required OrgType XUUID.
     */
    public Organization(ParamSet fields, XUUID orgType)
    {
        super(fields);

        // do NOT do "this(fields);" as it will fail on "this.typeID = ..."
        this.typeID   = orgType;
        this.isActive = true;

        commonConstruction(fields);
    }

    private void commonConstruction(ParamSet fields)
    {
        String val;

        this.externalOrgID  = fields.get(F_ORG_EXTERNALID);
        this.passwordPolicy = fields.get(F_ORG_PASSPOLICY);

        if ((val = fields.get(F_MAX_FAILED_LOGINS)) != null)
            this.maxFailedLogins = Integer.parseInt(val);

        if ((val = fields.get(F_ALLOWED_CIDRS)) != null)
            this.allowedCidrs = StringList.restoreList(val).toArray(new String[0]);

        if ((val = fields.get(F_ACTIVITY_TO_OVER)) != null)
            this.activityTimeoutOverride = Integer.parseInt(val);

        if ((val = fields.get(F_NEEDS_TWO_FACTOR)) != null)
            this.needsTwoFactor = Boolean.parseBoolean(val);
    }

    /**
     * Testers
     */
    public static boolean eqExternalID(String extID, ParamSet fields)
    {
        return extID.equals(fields.get(F_ORG_EXTERNALID));
    }

    /**
     * Getters
     */
    public void setOrgType(XUUID type)   { typeID = type; }   //!! temporary for migration only

    public XUUID getOrgType()
    {
        return typeID;
    }
    public boolean getIsActive()
    {
        return isActive;
    }
    public String getExternalID()
    {
        return externalOrgID;
    }
    public String getPasswordPolicy()
    {
        return passwordPolicy;
    }
    public Integer getMaxFailedLogins()
    {
        return maxFailedLogins;
    }
    public String[] getIpAddressWhitelist()
    {
        return allowedCidrs;
    }
    public int getActivityTimeoutOverride()
    {
        return activityTimeoutOverride;
    }
    public boolean getNeedsTwoFactor() { return needsTwoFactor; }

    /**
     * Setters
     */
    public void setIsActive(boolean isActive)
    {
        this.isActive = isActive;
    }
    public void setPasswordPolicy(String policyName)
    {
        this.passwordPolicy = policyName;
    }
    public void setMaxFailedLogins(Integer maxFailedLogins)
    {
        this.maxFailedLogins = maxFailedLogins;
    }
    public void setIpAddressWhitelist(String[] allowedCidrs)
    {
        this.allowedCidrs = allowedCidrs;
    }
    public void setActivityTimeoutOverride(int override)
    {
        this.activityTimeoutOverride = override;
    }
    public void setNeedsTwoFactor(boolean needsTwoFactor) { this.needsTwoFactor = needsTwoFactor; }
    /**
     * Return a Map of field=value for persisting the object.
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_ORG_TYPE,       typeID.toString());
        fields.put(F_ORG_ISACTIVE,   Boolean.toString(isActive));
        fields.put(F_ORG_EXTERNALID, externalOrgID);

        if (passwordPolicy != null)
            fields.put(F_ORG_PASSPOLICY, passwordPolicy);

        if (maxFailedLogins != null)
            fields.put(F_MAX_FAILED_LOGINS, String.valueOf(maxFailedLogins));

        if (allowedCidrs != null)
            fields.put(F_ALLOWED_CIDRS, StringList.flattenList(allowedCidrs));

        fields.put(F_ACTIVITY_TO_OVER, Integer.toString(activityTimeoutOverride));
        fields.put(F_NEEDS_TWO_FACTOR, Boolean.toString(needsTwoFactor));

    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[Organization: [" + super.toString() + "]" +
                "; name=" + getName() + 
                "; desc=" + getDescription() + 
                "; type=" + typeID +
                "; isActive=" + isActive +
                "; orgID=" + externalOrgID +
                "; passwordPolicy=" + passwordPolicy +
                "; maxFailedLogins=" + maxFailedLogins +
                "; activityTimeoutOverride=" + activityTimeoutOverride +
                "; needsTwoFactor=" + needsTwoFactor +
                "]"
            );
    }

}
