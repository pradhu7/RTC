package com.apixio.restbase.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Various useful date utility methods.
 */
public class DateUtil {

    private static final int[] DAYS_IN_MONTH = new int[] {
        31, 29, 31,
        30, 31, 30,
        31, 31, 30,
        31, 30, 31
    };

    private final static String ISOFORMAT      = "yyyy-MM-dd'T'HH:mm:ssX";
    private final static String ISOFORMAT_MS   = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private final static String MMDDYYYYFORMAT = "MM/dd/yyyy";

    private final static DateFormat Iso8601Parser   = new SimpleDateFormat(ISOFORMAT);
    private final static DateFormat Iso8601MsParser = new SimpleDateFormat(ISOFORMAT_MS);
    private final static DateFormat MmDdYyyyParser  = new SimpleDateFormat(MMDDYYYYFORMAT);

    static {
        TimeZone utc = TimeZone.getTimeZone("UTC");

        Iso8601Parser.setTimeZone(utc);
        Iso8601MsParser.setTimeZone(utc);
        MmDdYyyyParser.setTimeZone(utc);
    }

    /**
     * Converts the given Date to an ISO8601 string
     */
    public static String dateToIso8601(Date date)
    {
        synchronized (Iso8601Parser)
        {
            return Iso8601Parser.format(date);
        }
    }

    /**
     * Returns a Date from the given ISO8601 formatted string value.
     */
    public static Date validateIso8601(String date)
    {
        Date dt = null;

        // stupid but necessary hack to allow the date to optionally contain ".sss" millisecond
        // time specification.

        synchronized (Iso8601MsParser)
        {
            try
            {
                dt = Iso8601MsParser.parse(date);
            }
            catch (Exception x)
            {
            }
        }

        if (dt == null)
        {
            synchronized (Iso8601Parser)
            {
                try
                {
                    dt = Iso8601Parser.parse(date);
                }
                catch (Exception x)
                {
                }
            }
        }

        return dt;
    }

    /**
     * Returns a Date from a string with the format mm/dd/yyyy.
     */
    public static Date validateMMDDYYYY(String date)
    {
        synchronized (MmDdYyyyParser)
        {
            try
            {
                return MmDdYyyyParser.parse(date);
            }
            catch (Exception x)
            {
            }
        }

        return null;
    }

    /**
     * Must be in the form of yyyy-mm-dd.
     */
    public static void validateYMD(String date)
    {
        // yeah, it's stupid to parse it myself but it's sooo trivial (except, true, i'm not
        // checking # of days in a month...)

        if ((date == null) || (date.length() != 10) || (date.charAt(4) != '-') || (date.charAt(7) != '-'))
            throw new IllegalArgumentException("Bad ISO8601 date (yyyy-mm-dd):  " + date);

        int year = Integer.parseInt(date.substring(0, 4));
        int mon  = Integer.parseInt(date.substring(5, 7));
        int day  = Integer.parseInt(date.substring(8));

        if ((year <= 0) ||
            (mon  <  1) || (mon > 12) ||
            (day  <  1) || (day > DAYS_IN_MONTH[mon - 1]))
            throw new IllegalArgumentException("Bad ISO8601 date (yyyy-mm-dd):  " + date);
    }

    /**
     * Must be in the form of yyyy with year being "reasonable".
     */
    public static void validateY(String date)
    {
        int year = Integer.parseInt(date.substring(0, 4));

        if (year <= 1970)
            throw new IllegalArgumentException("Bad ISO8601 year (yyyy):  " + date);
    }

    public static void main(String[] args)
    {
        for (String a : args)
            System.out.println(validateIso8601(a));
    }

}
