package com.apixio.restbase.util;

//import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtil {
    /**
     * Returns the Field accessor for the class' field, returning null instead of throwing an exception if
     * there is no such field.
     */
    public static Field safeGetField(Class<?> clz, String field)
    {
        try
        {
            return clz.getField(field);
        }
        catch (Exception x)
        {
            return null;
        }
    }

    /**
     * Returns the Method accessor for the class' field, returning null instead of throwing an exception if
     * there is no such field.
     */
    public static Method safeGetMethod(Class<?> clz, String method, Class<?>... args)
    {
        try
        {
            return clz.getMethod(method, args);
        }
        catch (Exception x)
        {
            //            x.printStackTrace();
            return null;
        }
    }

    /**
     * Make the getter or setter name.  The rule is to convert the first character to an uppercase
     * and prepend the "get" or "set" prefix.
     */
    public static String makeMethodName(String base, String propName)
    {
        char first = propName.charAt(0);

        if (!Character.isUpperCase(first))
            return base + ((char) Character.toUpperCase(first)) + safeSubstring(propName, 1);
        else
            return base + propName;
    }

    private static String safeSubstring(String s, int at)
    {
        if (at < s.length())
            return s.substring(at);
        else
            return "";
    }

}
