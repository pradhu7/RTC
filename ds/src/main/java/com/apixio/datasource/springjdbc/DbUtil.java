package com.apixio.datasource.springjdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

import com.apixio.XUUID;

/**
 * DB column utilities. 
 */
public class DbUtil
{
    /**
     * Since db schema is just a single char...
     */
    private final static String TRUE_AS_CHAR1  = "y";
    private final static String FALSE_AS_CHAR1 = "n";

    private final static ZoneId defaultZone = ZoneId.systemDefault();

    /**
     * For reading from a java.sql.Timestamp (i.e., the standard SQL TIMESTAMP type) column type into a java.util.Date value
     */
    static public Date getTimestamp(ResultSet rs, String col, boolean required) throws SQLException
    {
        Timestamp val = rs.getTimestamp(col);

        if (required && (val == null))
            throw new IllegalStateException("Expected result set to have non-null Timestamp value in column " + col);
        else if (val != null)
            return new Date(val.getTime());
        else
            return null;
    }

    /**
     * For reading from a java.sql.Date (i.e., the standard SQL DATE type) column type into a LocalDate value
     */
    static public LocalDate getLocalDate(ResultSet rs, String col, boolean required) throws SQLException
    {
        Date val = rs.getDate(col);       // sub-day fields will be set to 0 (java.sql.Date extends java.util.Date...)

        if (required && (val == null))
        {
            throw new IllegalStateException("Expected result set to have non-null LocalDate value in column " + col);
        }
        else if (val != null)
        {
            Calendar cal = Calendar.getInstance();

            // sadly almost all Date methods are deprecated so we have to use Calendar
            cal.setTime(val);

            return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
        else
        {
            return null;
        }
    }

    /**
     * Required DB field to be declared as "VARCHAR(n)" where n >= 50 (reasonable upper bound of max len of XUUID)
     */
    static public XUUID getXuuid(ResultSet rs, String col, boolean required) throws SQLException
    {
        String val = rs.getString(col);

        if ((val != null) && ((val = val.trim()).length() == 0))
            val = null;

        if (required && (val == null))
            throw new IllegalStateException("Expected result set to have non-null XUUID (as varchar) value in column " + col);
        else if (val != null)
            return XUUID.fromString(val);
        else
            return null;
    }

    /**
     * Requires DB field to be declared as "CHAR(1)"
     */
    static public Boolean getBoolean(ResultSet rs, String col, boolean required) throws SQLException
    {
        String val = rs.getString(col);

        if (required && (val == null))
            throw new IllegalStateException("Expected result set to have non-null Boolean (as char(1)) value in column " + col);
        else if (val != null)
            return convertToBoolean(val);
        else
            return null;
    }

    /**
     * Central method to convert from VARCHAR(1) in RDB that models a Boolean into java.lang.Boolean
     */
    static public Boolean convertToBoolean(String varchar1)
    {
        return (varchar1 != null) ? Boolean.valueOf(varchar1.equalsIgnoreCase(TRUE_AS_CHAR1)) : null;
    }

    /**
     * Parses into JSONObject; arrays are not supported at this level
     */
    static public JSONObject getJson(String serialized)
    {
        return (serialized != null) ? new JSONObject(serialized) : null;
    }

    /**
     * For writing a boolean to CHAR(1).  This works only because the model is to write out all fields...
     */
    static public String putBoolean(Boolean val)
    {
        return convertFromBoolean(val, false);  // false => interpret val==null as false
    }
    static public String putBoolean(Boolean val, boolean keepNull)
    {
        return convertFromBoolean(val, keepNull);
    }

    /**
     * Central method to convert from java.lang.Boolean to VARCHAR(1) in RDB.  The converted value
     * will be one of [null, n, y], but a null can be forced into a 'n' by passing in true for
     * nullToFalse
     */
    static public String convertFromBoolean(Boolean bool)
    {
        return convertFromBoolean(bool, false);
    }
    static public String convertFromBoolean(Boolean bool, boolean keepNull)
    {
        if (keepNull && (bool == null))
            return null;
        else if (bool != null)
            return (bool) ? TRUE_AS_CHAR1 : FALSE_AS_CHAR1;
        else
            return FALSE_AS_CHAR1;
    }

    /**
     * For writing an XUUID
     */
    static public String putXuuid(XUUID val)
    {
        return (val != null) ? val.toString() : null;
    }

    /**
     * Stupid symmetric method (i.e., for clarity, really) for putting to a TIMESTAMP column.
     */
    static public Date putTimestamp(Date timestamp)
    {
        return timestamp;
    }

    /**
     * For writing a java.time.LocalDate as java.sql.Date.  Note this is a bit bogus as timezones get involved
     * here.
     */
    static public java.sql.Date putLocalDate(LocalDate date)
    {
        if (date != null)
            return new java.sql.Date(date.atStartOfDay(defaultZone).toEpochSecond() * 1000L);
        else
            return null;
    }

    /**
     * Serializes JSONObject to String.  JSONArray is not supported at this level
     */
    static public String putJson(JSONObject json)
    {
        return (json != null) ? json.toString() : null;
    }

}
