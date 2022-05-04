package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * PasswordPolicy.
 *
 * The only interesting thing (currently) about PasswordPolicy is the "maxDays" concept.
 * Externally to the world it is indeed the maximum number of days and it is reported
 * as such via API return data.  However, internally it is kept as a combination of
 * unit (minute, day, week, month) and amount (of that unit).  This is done for two reasons:
 * testing is much simpler if the password expiration can be specified down to the order of
 * minutes, and longer-term it's likely that the larger units will be useful when configuring
 * (e.g., expiration times are expected to be on the order of months).
 *
 * Implementation-wise, only the unit & amount are stored, and the computed maxDays is available
 * as a pseudo-property so that things like EntityProperty work as expected.
 */
public class PasswordPolicy extends NamedEntity {

    public static final String OBJTYPE = ObjectTypes.PASSWORD_POLICY;

    public static final int MIN_PASSWORD_LENGTH = 1;

    /**
     * Units for maxDays (now badly named).
     */
    public static final int UNIT_MINUTE = 1;
    public static final int UNIT_DAY    = 2;
    public static final int UNIT_WEEK   = 3;
    public static final int UNIT_MONTH  = 4;

    /**
     * Convenience:  # of milliseconds in the various units
     */
    private final static long MS_IN_A_MIN  = 1000L * 60;
    private final static long MS_IN_A_DAY  = MS_IN_A_MIN * 60 * 24;
    private final static long MS_IN_A_WEEK = MS_IN_A_DAY * 7;
    private final static long MS_IN_A_MON  = MS_IN_A_DAY * 30;   // 30 is good enough for now

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_TIME_UNIT      = "time-unit";       // as in UNIT_*
    private final static String F_MAX_TIME       = "max-time";        // max # of (units) between forced password changes
    private final static String F_MIN_LENGTH     = "min-chars";       // min # of chars in password (trimmed)
    private final static String F_MAX_LENGTH     = "max-chars";       // max # of chars in password (trimmed)????
    private final static String F_MIN_LOWERCASE  = "min-lower";       // min # of chars that must be lowercase
    private final static String F_MIN_UPPERCASE  = "min-upper";       // min # of chars that must be uppercase
    private final static String F_MIN_DIGITS     = "min-digit";       // min # of chars that must be a digit
    private final static String F_MIN_SYMBOLS    = "min-symbol";      // min # of chars that must be a symbol
    private final static String F_NO_USERID      = "no-userid";       // true => username and non-domain email NOT allowed in password
    private final static String F_NO_REUSE_NUM   = "no-reuse-num";    // history size of old passwords that can't be reused; 0 => no check, 1 => can't reuse current, 2 => not current & not previous
    private final static String F_NO_REUSE_TIME  = "no-reuse-days";   // # of days of life of old, unreusable passwords; 0 => no check, 1 => ???

    /**
     * Actual fields
     */
    private int      timeUnit;
    private int      maxTime;
    private int      maxDays;  // Manufactured (not stored) value, from timeUnit * maxTime

    private int      minChars = MIN_PASSWORD_LENGTH;
    private int      maxChars;
    private int      minLower;
    private int      minUpper;
    private int      minDigits;
    private int      minSymbols;
    private boolean  noUserID;   // no good name for this
    private int      noReuseCount;
    private int      noReuseDays;

    /**
     * Create a new UserOrg from the given information
     */
    public PasswordPolicy(String name)
    {
        super(OBJTYPE, name);
    }

    /**
     * For restoring from persisted form only
     */
    public PasswordPolicy(ParamSet fields)
    {
        super(fields);

        this.timeUnit       = getInt(fields.get(F_TIME_UNIT), UNIT_DAY);
        this.maxTime        = getInt(fields.get(F_MAX_TIME),  0);
        this.minChars       = Integer.parseInt(fields.get(F_MIN_LENGTH));
        this.maxChars       = Integer.parseInt(fields.get(F_MAX_LENGTH));
        this.minLower       = Integer.parseInt(fields.get(F_MIN_LOWERCASE));
        this.minUpper       = Integer.parseInt(fields.get(F_MIN_UPPERCASE));
        this.minDigits      = Integer.parseInt(fields.get(F_MIN_DIGITS));
        this.minSymbols     = Integer.parseInt(fields.get(F_MIN_SYMBOLS));
        this.noUserID       = Boolean.valueOf(fields.get(F_NO_USERID));
        this.noReuseCount   = Integer.parseInt(fields.get(F_NO_REUSE_NUM));
        this.noReuseDays    = Integer.parseInt(fields.get(F_NO_REUSE_TIME));

        calcMaxDays();
    }

    /**
     * Getters
     */
    public int getTimeUnit()
    {
        return timeUnit;
    }
    public int getMaxTime()
    {
        return maxTime;
    }
    public int getMaxDays()
    {
        return maxDays;
    }
    public int getMinChars()
    {
        return minChars;
    }
    public int getMaxChars()
    {
        return maxChars;
    }
    public int getMinLower()
    {
        return minLower;
    }
    public int getMinUpper()
    {
        return minUpper;
    }
    public int getMinDigits()
    {
        return minDigits;
    }
    public int getMinSymbols()
    {
        return minSymbols;
    }
    public boolean getNoUserID()
    {
        return noUserID;
    }
    public int getNoReuseCount()
    {
        return noReuseCount;
    }
    public int getNoReuseDays()
    {
        return noReuseDays;
    }

    /**
     * Setters
     */
    public void setTimeUnit(int n)
    {
        this.timeUnit = Math.min(Math.max(n, UNIT_MINUTE), UNIT_MONTH);
        calcMaxDays();
    }
    public void setMaxTime(int n)
    {
        this.maxTime = Math.max(n, 0);
        calcMaxDays();
    }
    public void setMaxDays(int n)
    {
        // ignore:  needed only for EntityProperty reflection...
    }
    public void setMinChars(int n)
    {
        this.minChars = Math.max(n, MIN_PASSWORD_LENGTH);
    }
    public void setMaxChars(int n)
    {
        this.maxChars = Math.max(n, 0);
    }
    public void setMinLower(int n)
    {
        this.minLower = Math.max(n, 0);
    }
    public void setMinUpper(int n)
    {
        this.minUpper = Math.max(n, 0);
    }
    public void setMinDigits(int n)
    {
        this.minDigits = Math.max(n, 0);
    }
    public void setMinSymbols(int n)
    {
        this.minSymbols = Math.max(n, 0);
    }
    public void setNoUserID(boolean disallow)
    {
        this.noUserID = disallow;
    }
    public void setNoReuseCount(int n)
    {
        this.noReuseCount = Math.max(n, 0);
    }
    public void setNoReuseDays(int n)
    {
        this.noReuseDays = Math.max(n, 0);
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_TIME_UNIT,     Integer.toString(timeUnit));
        fields.put(F_MAX_TIME,      Integer.toString(maxTime));
        fields.put(F_MIN_LENGTH,    Integer.toString(minChars));
        fields.put(F_MAX_LENGTH,    Integer.toString(maxChars));
        fields.put(F_MIN_LOWERCASE, Integer.toString(minLower));
        fields.put(F_MIN_UPPERCASE, Integer.toString(minUpper));
        fields.put(F_MIN_DIGITS,    Integer.toString(minDigits));
        fields.put(F_MIN_SYMBOLS,   Integer.toString(minSymbols));
        fields.put(F_NO_USERID,     Boolean.toString(noUserID));
        fields.put(F_NO_REUSE_NUM,  Integer.toString(noReuseCount));
        fields.put(F_NO_REUSE_TIME, Integer.toString(noReuseDays));
    }

    /**
     * Returns the number of milliseconds equal to the given number of units.  Units are taken
     * from PasswordPolicy.UNIT_*
     */
    public long getExpirationTime()
    {
        if (timeUnit == PasswordPolicy.UNIT_MINUTE)
            return maxTime * MS_IN_A_MIN;
        else if (timeUnit == PasswordPolicy.UNIT_DAY)
            return maxTime * MS_IN_A_DAY;
        else if (timeUnit == PasswordPolicy.UNIT_WEEK)
            return maxTime * MS_IN_A_WEEK;
        else if (timeUnit == PasswordPolicy.UNIT_MONTH)
            return maxTime * MS_IN_A_MON;
        else
            return 0L;
    }

    /**
     * Safe parsing of Strings to ints.
     */
    private static int getInt(String value, int defValue)
    {
        if (value == null)
            return defValue;

        try
        {
            return Integer.parseInt(value);
        }
        catch (Exception x)
        {
            return defValue;
        }
    }

    /**
     * Calculates the maxDays value from the time unit and max time.  A value less than
     * a day will result in a value of 0.
     */
    private void calcMaxDays()
    {
        maxDays = (int) (getExpirationTime() / MS_IN_A_DAY);
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[passwordpolicy: [" + super.toString() + "]" +
                "; name=" + getName() + 
                "; timeUnit=" + timeUnit + 
                "; maxTime=" + maxTime + 
                "; maxDays=" + maxDays + 
                "; minLen=" + minChars + 
                "; maxLen=" + maxChars + 
                "; minLower=" + minLower + 
                "; minUpper=" + minUpper + 
                "; minDigits=" + minDigits + 
                "; minSymbols=" + minSymbols + 
                "; noUserID=" + noUserID + 
                "; noReuseNum=" + noReuseCount + 
                "; noReuseTime=" + noReuseDays +
                "]"
            );
    }

}
