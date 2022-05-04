package com.apixio.utility;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TimeHelper
{
    /**
     * Convenient method over roundDate
     */
    public static String makeTimeChunk(Date date, long timeGranularity)
    {
        return Long.toString( roundDate(date, timeGranularity) );
    }

    /**
     * Convenient method over roundDate
     */
    public static String makeTimeChunk(long timeInMillis, long timeGranularity)
    {
        return Long.toString( roundDate(timeInMillis, timeGranularity) );
    }

    /**
     * roundDate converts a Date into the string form of a Long, where the value
     * of the Long is the number of epoch seconds rounded back by the configured
     * granularity of time.  If the time granularity is, say, 1 week, then all Dates
     * within a week period will get mapped to a single time period value.
     */
    public static long roundDate(Date date, long timeGranularity)
    {
        // assumes integer arithmetic:
        return ( ( (date.getTime() / 1000L) / timeGranularity) * timeGranularity );
    }

    /**
     * roundDate converts a timeInMillis into the string form of a Long, where the value
     * of the Long is the number of epoch seconds rounded back by the configured
     * granularity of time.  If the time granularity is, say, 1 week, then all Dates
     * within a week period will get mapped to a single time period value.
     */
    public static long roundDate(long timeInMillis, long timeGranularity)
    {
        // assumes integer arithmetic:
        return ( ( (timeInMillis / 1000L) / timeGranularity) * timeGranularity) ;
    }

    /**
    * calcTimePeriods produces a list of time period values (see roundDate)
    * that cover the start and end dates completely.
    */
    public static List<String> calculateTimePeriods(Date startDt, Date endDt, long timeGranularity)
    {
        List<String>  periods = new ArrayList<String>();

        for (long epoch = roundDate(startDt, timeGranularity), end = roundDate(endDt, timeGranularity);
             epoch <= end; epoch += timeGranularity)
        {
            periods.add(Long.toString(epoch));
        }

        return periods;
    }

    /**
    * calcTimePeriods produces a list of time period values (see roundDate)
    * that cover the start and end dates completely.
    */
    public static List<String> calculateTimePeriods(long startTime, long endTime, long timeGranularity)
    {
        List<String>  periods = new ArrayList<String>();

        for (long epoch = roundDate(startTime, timeGranularity), end = roundDate(endTime, timeGranularity);
             epoch <= end; epoch += timeGranularity)
        {
            periods.add(Long.toString(epoch));
        }

        return periods;
    }

    public static String nowAsString()
    {
        TimeZone   tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
        df.setTimeZone(tz);

        return df.format(new Date());
    }

    public static Date stringToDate(String dateStr)
        throws ParseException
    {
        TimeZone   tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
        df.setTimeZone(tz);

        return df.parse(dateStr);
    }

    public static String convertToString(long time)
    {
        return convertToString(new Date(time));
    }

    public static String convertToString(Date date)
    {
        TimeZone   tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
        df.setTimeZone(tz);

        return df.format(date);
    }

    public static void main(String[] args)
    {
        System.out.println(convertToString(new Date()));
    }
}