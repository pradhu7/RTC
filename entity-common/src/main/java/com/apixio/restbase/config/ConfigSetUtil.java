package com.apixio.restbase.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.apixio.utility.StringList;

/**
 * ConfigSetUtil provides lower-level support functionality for ConfigSet objects.
 * Most specifically it provides serialization and deserialization support for
 * ConfigSet instances.
 */
public class ConfigSetUtil
{
    private final static char   SEP_AS_CHAR = ':';
    private final static String SEP_AS_STR  = ":";

    private final static String TYPE_NULL    = "null";        // yes, a value of null is supported...
    private final static String TYPE_BOOLEAN = "boolean";
    private final static String TYPE_DOUBLE  = "double";
    private final static String TYPE_STRING  = "string";
    private final static String TYPE_INTEGER = "integer";
    private final static String TYPE_LONG    = "long";
    private final static String TYPE_EXP     = "expression";
    private final static String TYPE_LIST    = "list";

    private final static String PFX_NULL    = TYPE_NULL    + SEP_AS_STR;
    private final static String PFX_BOOLEAN = TYPE_BOOLEAN + SEP_AS_STR;
    private final static String PFX_DOUBLE  = TYPE_DOUBLE  + SEP_AS_STR;
    private final static String PFX_STRING  = TYPE_STRING  + SEP_AS_STR;
    private final static String PFX_INTEGER = TYPE_INTEGER + SEP_AS_STR;
    private final static String PFX_LONG    = TYPE_LONG    + SEP_AS_STR;
    private final static String PFX_EXP     = TYPE_EXP     + SEP_AS_STR;
    private final static String PFX_LIST    = TYPE_LIST    + SEP_AS_STR;

    /**
     * Converts from the java.lang.Class of a supported scalar type into the
     * string constant name for it (which is used as the prefix).
     */
    private final static Map<Class<?>, String> classTypeMap = new HashMap<>();
    static
    {
        classTypeMap.put(java.lang.String.class,  TYPE_STRING);
        classTypeMap.put(java.lang.Double.class,  TYPE_DOUBLE);
        classTypeMap.put(java.lang.Integer.class, TYPE_INTEGER);
        classTypeMap.put(java.lang.Boolean.class, TYPE_BOOLEAN);
        classTypeMap.put(ConfigExpression.class,  TYPE_EXP);
    }

    /**
     * Returns true IFF the object's type is a supported scalar type.
     */
    public static boolean isScalar(Object o)
    {
        return ((o instanceof Boolean) ||
                (o instanceof Double)  ||
                (o instanceof Integer) ||
                (o instanceof Long)    ||
                (o instanceof String)  ||
                (o instanceof ConfigExpression));
    }

    /**
     * Converts a Map<String, Object> to a linearized String that can be easily
     * persisted and reconstituted into a Map.  Only some Java scalar types and a
     * list of scalar types are supported; for those types, a tagging prefix is
     * added before the string form of the scalar value (e.g., "integer:123").
     *
     * Lists are supported by tagging the value with "list:{size}" where {size} is
     * the # of elements in the list.  The actual values in the list have keys of
     * the form {mainkey}.{index} and the value is the typical tagged value.  This
     * allows a list of different object types (but nested lists are not supported).
     *
     * Note that the resulting string is NOT COMPATIBLE with StringUtil.mapToString!!
     */
    public static String mapToString(Map<String, Object> map)
    {
        int          size = map.size();
        List<String> eles = new ArrayList<String>(size * 2 + 1);  // assumes no list values

        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            String key = entry.getKey();
            Object val = entry.getValue();

            if ((val == null) || isScalar(val))
            {
                eles.add(key);
                eles.add(tagValue(val));
            }
            else if (val instanceof List)
            {
                List<Object> listOfObj = (List<Object>) val;

                validateList(listOfObj);

                eles.add(key);
                eles.add(tagList(listOfObj.size()));

                for (int i = 0, m = listOfObj.size(); i < m; i++)
                {
                    eles.add(key + "." + i);
                    eles.add(tagValue(listOfObj.get(i)));
                }
            }
        }

        return StringList.flattenList(eles);
    }

    public static Map<String, String> mapToStringMap(Map<String, Object> map)
    {
        Map<String, String> eles = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            String key = entry.getKey();
            Object val = entry.getValue();

            if ((val == null) || isScalar(val))
            {
                eles.put(key, tagValue(val));
            }
            else if (val instanceof List)
            {
                List<Object> listOfObj = (List<Object>) val;

                validateList(listOfObj);

                eles.put(key, tagList(listOfObj.size()));

                for (int i = 0, m = listOfObj.size(); i < m; i++)
                    eles.put((key + "." + i), tagValue(listOfObj.get(i)));
            }
        }

        return eles;
    }

    /**
     * Undoes the conversion made by mapToString by parsing the string and returning
     * the resulting Map<String, Object>.
     */
    public static Map<String, Object> mapFromString(String mapped)
    {
        List<String>        eles = StringList.restoreList(mapped);
        Map<String, Object> map  = new HashMap<>();

        try
        {
            int size = eles.size();
            int pos  = 0;

            while (pos < size)
            {
                String key = eles.get(pos++);
                String val = eles.get(pos++);

                if (!isTaggedList(val))
                {
                    map.put(key, untagValue(val, true));
                }
                else
                {
                    String[]     listSize = val.split(SEP_AS_STR);
                    List<Object> list;
                    int          lsize;

                    if (listSize.length != 2)
                        throw new IllegalArgumentException("Invalid list tag; should be 'list:{size}' but is " + val);

                    lsize = Integer.parseInt(listSize[1]);
                    list  = new ArrayList<Object>(lsize);

                    for (int i = 0; (i < lsize) && (pos < size); i++)
                    {
                        String elekey = eles.get(pos++);

                        if (!elekey.startsWith(key))
                            throw new IllegalArgumentException("Corrupted tagged list; expected key to start with " + key + " but got " + elekey);

                        list.add(untagValue(eles.get(pos++), true));
                    }

                    map.put(key, list);
                }
            }
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }

        return map;
    }

    /**
     * 
     */
    public static Object validateTypeAndConvert(String type, String val)
    {
        // default to String
        if (type == null)
            return val;
        else if (type.equals(TYPE_NULL))
            return null;
        else if (type.equals(TYPE_BOOLEAN))
            return Boolean.valueOf(val);
        else if (type.equals(TYPE_DOUBLE))
            return Double.valueOf(val);
        else if (type.equals(TYPE_STRING))
            return val;
        else if (type.equals(TYPE_EXP))
            return ConfigExpression.valueOf(val);
        else if (type.equals(TYPE_INTEGER))
            return Integer.valueOf(val);
        else if (type.equals(TYPE_LONG))
            return Long.valueOf(val);
        else
            throw new IllegalArgumentException("Unsupported type " + type);
    }

    /**
     * Tags a scalar value.  Lists are tagged with tagList().
     */
    public static String tagValue(Object val)
    {
        if (val instanceof Boolean)
            return PFX_BOOLEAN + val;
        else if (val instanceof Double)
            return PFX_DOUBLE + val;
        else if (val instanceof String)
            return PFX_STRING + val;
        else if (val instanceof ConfigExpression)
            return PFX_EXP + val;
        else if (val instanceof Integer)
            return PFX_INTEGER + val;
        else if (val instanceof Long)
            return PFX_LONG + val;
        else
            throw new IllegalArgumentException("Unsupported type " + val.getClass());
    }

    /**
     * Untags a scalar value.  List values must be handled specially by determining it
     * is a list (via isTaggedList()) and then calling untagList().
     */
    public static Object untagValue(String val, boolean requireType)
    {
        int co = val.indexOf(SEP_AS_CHAR);

        if ((co == -1) && requireType)
            throw new IllegalArgumentException("Tagged value '" + val + "' is missing required type:");

        String type;

        if (co != -1)
        {
            type = val.substring(0, co);
            val  = val.substring(co + 1);
        }
        else
        {
            type = null;
        }

        return validateTypeAndConvert(type, val);
    }

    /**
     * Creates the tagged value for a list of scalars, which is of the form
     *  "list:" type ":" size.
     */
    private static String tagList(int length)
    {
        return PFX_LIST + Integer.toString(length);
    }

    /**
     * Returns true IFF the value was created via tagList().
     */
    private static boolean isTaggedList(String val)
    {
        return (val != null) && val.startsWith(PFX_LIST);
    }

    /**
     * Makes sure that all elements in the list are scalar values.
     */
    private static void validateList(List<Object> list)
    {
        if (list.size() > 0)
        {
            for (Object ele : list)
            {
                if ((ele != null) && !isScalar(ele))
                    throw new IllegalArgumentException("mapToString support for list value requires that all list elements be scalar, but found " + ele);
            }
        }
    }

    /**
     * Testing
     */
    public static void main(String... args)
    {
        Map<String, Object> map = new HashMap<>();

        map.put("hi", ConfigExpression.valueOf("hello"));
        System.out.println(mapToString(map));
        System.out.println(mapToStringMap(map));

        System.out.println("=======");
        Map<String, Object> recon = mapFromString(mapToString(map));

        for (Map.Entry<String, Object> entry : recon.entrySet())
        {
            System.out.println(entry.getKey() + " => type " + entry.getValue().getClass() + " with value " + entry.getValue());
        }

    }

}
