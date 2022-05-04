package com.apixio.sdk.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility methods to convert Throwables to Strings
 */
public class ExceptionUtil
{
    public static String stackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();

        t.printStackTrace(new PrintWriter(sw));

        return sw.toString();
    }

    public static String oneLineStackTrace(Throwable t)
    {
        if (t != null)
            return stackTrace(t).replace('\n', '/');
        else
            return "";
    }
}
