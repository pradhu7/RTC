package com.apixio.useracct.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.util.DateUtil;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.utility.StringList;

/**
 * User is the core entity of the user account service.  Users are identified
 * uniquely by a verified email address.  User passwords are stored as bcrypt hash
 * values.
 *
 * A user has 0 or more allowed roles and this list of allowed roles is stored as a list of
 * {RoleSet}/{RoleName} elements.
 */
public class User extends BaseEntity {

    public static final String OBJTYPE = ObjectTypes.USER;

    private static final String PASSWORD_INFO_SEPARATOR      = ":";
    private static final char   PASSWORD_INFO_SEPARATOR_CHAR = PASSWORD_INFO_SEPARATOR.charAt(0);

    /**
     * timeoutOverride requires a bit of special handling...
     */
    private static final String TOV_NOOVERRIDE = "none";

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_ACCOUNT_STATE     = "account-state";
    private final static String F_ACCOUNT_ROLES     = "roles";           // deprecated Aug/2015.  persisted as comma separated list of role names
    private final static String F_ALLOWED_ROLES     = "allowed-roles";   // persisted as comma separated list of roleset/role names
    private final static String F_BCRYPTED_PASS     = "bcrypt-pass";
    private final static String F_DT_CHANGED_PASS   = "passwd-chg-date"; // epoch ms
    private final static String F_OLD_PASSWORDS     = "old-passwords";   // StringList.flatten()'ed; element format:  {chg-date-ms}:{bcrypted-hash}
    private final static String F_UPDATE_NONCE      = "update-nonce";
    private final static String F_FAILED_COUNT      = "failed-logins";   // reset to 0 on success
    private final static String F_EMAIL_ADDRESS     = "email-addr";
    private final static String F_FIRST_NAME        = "fname";
    private final static String F_LAST_NAME         = "lname";
    private final static String F_MIDDLE_INITIAL    = "mname";
    private final static String F_DATE_OF_BIRTH     = "dob";             // ISO8601 (yyyy-mm-dd)
    private final static String F_OFFICE_PHONE      = "phone-office";
    private final static String F_CELL_PHONE        = "phone-cell";
    private final static String F_TIMEOUT_OVERRIDE  = "timeout-override";   // persisted value of TOV_NOOVERRIDE means no override
    private final static String F_NEEDS_TWO_FACTOR  = "needs-two-factor";

    /**
     * actual fields
     */
    // required:
    private CanonicalEmail emailAddr;           //!! do we need to support more than just a single email address?
    private AccountState   state;
    private List<String>   roleNames;           // deprecated
    private List<String>   allowedRoles;        // like "System/ROOT,Vendor/CODER"; added Aug 2015
    private String         hashedPassword;      // it's acceptable for this to be null (just until password is set)
    private List<String>   oldPasswords;        // new items appended => oldest password change is at [0]
    private long           lastPasswordChange;  // epoch milliseconds (suitable for 'new Date(lastPasswordChange)'
    private String         updateNonce;
    private int            failedLogins;

    // not required:
    private String         firstName;
    private String         lastName;
    private String         middleInitial;
    private String         dateOfBirth;   // ISO8601:  yyyy-mm-dd
    private String         officePhone;   // not verified
    private String         cellPhone;     // not verified
    private Integer        timeoutOverride;  // # of seconds of inactivity before being logged out; null means no override
    private Boolean        needsTwoFactor;

    /**
     * Create a new User from the given information
     */
    public User(CanonicalEmail emailAddr, AccountState state, OldRole role)
    {
        super(OBJTYPE);

        this.oldPasswords = new ArrayList<String>();
        this.emailAddr    = emailAddr;
        this.state        = state;

        this.roleNames = new ArrayList<String>();
        if (role != null)
            roleNames.add(role.getName());
    }

    /**
     * For restoring from persisted form only
     */
    public User(ParamSet fields)
    {
        super(fields);

        String v;

        // required
        this.emailAddr          = new CanonicalEmail(fields.get(F_EMAIL_ADDRESS));
        this.state              = AccountState.valueOf(fields.get(F_ACCOUNT_STATE));
        this.hashedPassword     = fields.get(F_BCRYPTED_PASS);
        this.lastPasswordChange = getLong(fields.get(F_DT_CHANGED_PASS), 0L);
        this.oldPasswords       = StringList.restoreList(fields.get(F_OLD_PASSWORDS));
        this.updateNonce        = fields.get(F_UPDATE_NONCE);
        this.failedLogins       = getInt(fields.get(F_FAILED_COUNT), 0);
        this.roleNames          = EntityUtil.unpackRoles(fields.get(F_ACCOUNT_ROLES));
        this.allowedRoles       = EntityUtil.unpackRoles(fields.get(F_ALLOWED_ROLES));  // could be list w/ 0 elements

        // optional
        this.firstName     = fields.get(F_FIRST_NAME);
        this.lastName      = fields.get(F_LAST_NAME);
        this.middleInitial = fields.get(F_MIDDLE_INITIAL);
        this.officePhone   = fields.get(F_OFFICE_PHONE);
        this.cellPhone     = fields.get(F_CELL_PHONE);

        if (((v = fields.get(F_TIMEOUT_OVERRIDE)) != null) && !v.equals(TOV_NOOVERRIDE))
            this.timeoutOverride = Integer.valueOf(v);

        if ((v = fields.get(F_DATE_OF_BIRTH)) != null)
            this.dateOfBirth = v;

        if((v = fields.get(F_NEEDS_TWO_FACTOR)) != null)
            if("".equals(v))
                this.needsTwoFactor = null;
            else
                this.needsTwoFactor = Boolean.valueOf(v);
    }

    /**
     * Getters
     */
    public CanonicalEmail getEmailAddress()
    {
        return emailAddr;
    }
    public String getEmailAddr()
    {
        return emailAddr.toString();
    }
    public AccountState getState()
    {
        return state;
    }
    public String getStateStr()
    {
        return state.toString();
    }

    @Deprecated
    public List<String> getRoleNames()
    {
        return new ArrayList<String>(roleNames);
    }

    public List<String> getAllowedRoles()
    {
        return new ArrayList<String>(allowedRoles);
    }

    public String getHashedPassword()
    {
        return hashedPassword;
    }
    public Date getLastPasswordChange()     // null is returned if password was never set
    {
        if (lastPasswordChange > 0L)
            return new Date(lastPasswordChange);
        else
            return null;
    }

    /**
     * Returned items can be passed to getPasswordChangeDateMs() and getPasswordChangeHash()
     */
    public List<String> getOldPasswords()
    {
        return oldPasswords;  // ought to clone...
    }
    /**
     * Extracts and returns the epoch millisecond date of the changed password.  0L is
     * returned for a poorly formatted password history string.
     */
    public static long getPasswordChangeDateMs(String oldPasswordInfo)
    {
        int col = oldPasswordInfo.indexOf(PASSWORD_INFO_SEPARATOR_CHAR);

        if (col != -1)
        {
            try
            {
                return Long.parseLong(oldPasswordInfo.substring(0, col));
            }
            catch (NumberFormatException x)
            {
            }
        }


        return 0L;
    }
    public static String getPasswordChangeHash(String oldPasswordInfo)
    {
        int col = oldPasswordInfo.indexOf(PASSWORD_INFO_SEPARATOR_CHAR);

        if ((col != -1) && (col + 1 < oldPasswordInfo.length()))
            return oldPasswordInfo.substring(col + 1);
        else
            return null;
    }

    public String getUpdateNonce()
    {
        return updateNonce;
    }
    public int getFailedLogins()
    {
        return failedLogins;
    }
    public String getFirstName()
    {
        return firstName;
    }
    public String getLastName()
    {
        return lastName;
    }
    public String getMiddleInitial()
    {
        return middleInitial;
    }
    public String getDateOfBirth()
    {
        return dateOfBirth;
    }
    public String getOfficePhone()
    {
        return officePhone;
    }
    public String getCellPhone()
    {
        return cellPhone;
    }
    public Integer getTimeoutOverride()
    {
        return timeoutOverride;
    }
    public Boolean getNeedsTwoFactor()
    {
        return needsTwoFactor;
    }

    /**
     * Setters
     */
    public void setState(AccountState state)
    {
        this.state = state;
    }

    @Deprecated
    public void addRole(OldRole role)
    {
        if ((role != null) && !roleNames.contains(role.getName()))
            this.roleNames.add(role.getName());
    }
    public void addRole(String role)
    {
        if ((role != null) && !roleNames.contains(role))
            this.roleNames.add(role);
    }
    public void setRoles(List<String> roles)
    {
        this.roleNames = new ArrayList<String>();

        for (String role : roles)
            addRole(role);
    }
    public void removeRole(OldRole role)
    {
        if (role != null)
            roleNames.remove(role.getName());
    }
    public void removeRole(String role)
    {
        if (role != null)
            roleNames.remove(role);
    }

    /**
     * AllowedRole management
     */
    public void addAllowedRole(String fullRoleName)
    {
        if ((fullRoleName != null) && !allowedRoles.contains(fullRoleName))
            this.allowedRoles.add(fullRoleName);
    }
    public void setAllowedRoles(List<String> roles)
    {
        this.allowedRoles = new ArrayList<String>();

        for (String role : roles)
            addAllowedRole(role);
    }
    public void removeAllowedRole(String fullRoleName)
    {
        if (fullRoleName != null)
            allowedRoles.remove(fullRoleName);
    }



    public void setHashedPassword(String hashedPassword)
    {
        this.hashedPassword = hashedPassword;
    }
    public void setLastPasswordChange(long epochMs)
    {
        this.lastPasswordChange = epochMs;
    }
    public void setUpdateNonce(String updateNonce)
    {
        this.updateNonce = updateNonce;
    }

    /**
     * Old password low-level management methods.
     */
    public void addOldPassword(String passwordHash, long changeDateMs)
    {
        oldPasswords.add(Long.toString(changeDateMs) + PASSWORD_INFO_SEPARATOR + passwordHash);
    }
    public void cleanOldPasswordsByCount(int maxAllowed)
    {
        while (oldPasswords.size() > maxAllowed)
            oldPasswords.remove(0);
    }
    public void cleanOldPasswordsByDate(long discardBeforeMs)
    {
        while (oldPasswords.size() > 0)
        {
            if (getPasswordChangeDateMs(oldPasswords.get(0)) < discardBeforeMs)
                oldPasswords.remove(0);
        }
    }
    public void setFailedLogins(int failedCount)
    {
        this.failedLogins = failedCount;
    }
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }
    public void setMiddleInitial(String middleInitial)
    {
        this.middleInitial = middleInitial;
    }
    public void setDateOfBirth(String dateOfBirth)
    {
        if ((dateOfBirth != null) && (dateOfBirth.length() > 0))
            DateUtil.validateYMD(dateOfBirth);
        this.dateOfBirth = dateOfBirth;
    }
    public void setOfficePhone(String phone)
    {
        this.officePhone = phone;
    }
    public void setCellPhone(String phone)
    {
        this.cellPhone = phone;
    }
    public void setTimeoutOverride(Integer seconds)  // null for no override
    {
        this.timeoutOverride = seconds;
    }
    public void setNeedsTwoFactor(Boolean needsTwoFactor)
    {
        this.needsTwoFactor = needsTwoFactor;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_EMAIL_ADDRESS, emailAddr.toString());
        fields.put(F_ACCOUNT_STATE, state.toString());
        fields.put(F_ACCOUNT_ROLES, EntityUtil.packRoles(roleNames));
        fields.put(F_ALLOWED_ROLES, (allowedRoles != null) ? EntityUtil.packRoles(allowedRoles) : "");  // semi-optional:  required but old users won't have it
        fields.put(F_FAILED_COUNT,  Integer.toString(failedLogins));
        fields.put(F_OLD_PASSWORDS, StringList.flattenList(oldPasswords));

        // "not required" fields:
        fields.put(F_DT_CHANGED_PASS, Long.toString(lastPasswordChange));
        fields.put(F_BCRYPTED_PASS,   hashedPassword);
        fields.put(F_UPDATE_NONCE,    updateNonce);
        fields.put(F_FIRST_NAME,      firstName);
        fields.put(F_LAST_NAME,       lastName);
        fields.put(F_MIDDLE_INITIAL,  middleInitial);
        fields.put(F_DATE_OF_BIRTH,   dateOfBirth);
        fields.put(F_OFFICE_PHONE,    officePhone);
        fields.put(F_CELL_PHONE,      cellPhone);

        // record TOV_NOOVERRIDE as marker for no override--no other way to remove the value, unfortunately
        fields.put(F_TIMEOUT_OVERRIDE, (timeoutOverride != null) ? timeoutOverride.toString() : TOV_NOOVERRIDE);

        fields.put(F_NEEDS_TWO_FACTOR, (needsTwoFactor != null) ? needsTwoFactor.toString(): "");

    }

    /**
     * Parse string as an integral value.  Return defValue for any error.  This model supports
     * reading required fields that were added after-the-fact.
     */
    private int getInt(String str, int defValue)
    {
        try
        {
            return Integer.parseInt(str);
        }
        catch (Exception x)
        {
            return defValue;
        }
    }

    private long getLong(String str, long defValue)
    {
        try
        {
            return Long.parseLong(str);
        }
        catch (Exception x)
        {
            return defValue;
        }
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[User: [" + super.toString() + "]" +
                "; email=" + emailAddr + 
                "; state=" + state + 
                "; roleNames=" + EntityUtil.packRoles(roleNames) +
                "; allowedRoles=" + ((allowedRoles != null) ? EntityUtil.packRoles(allowedRoles) : "") +
                "; bcrypt=" + hashedPassword +
                "; lastPasswordChange=" + lastPasswordChange +
                "; updateNonce=" + updateNonce +
                "; needsTwoFactor=" + needsTwoFactor +
                "]"
            );
    }

}
