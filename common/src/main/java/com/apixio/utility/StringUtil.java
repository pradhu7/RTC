package com.apixio.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StringUtil contains useful utility methods for Strings
 */
public class StringUtil
{
    /**
     * Does a simple (fast) subtring replacement.  This does not use the
     * far more general (and more costly) String.replace() and is specifically
     * offered as a way to avoid the overhead of regular expression pattern
     * matching.
     */
    public static String replaceSubstring(String str, String substr, String replaceWith)
    {
        int  len = substr.length();
        int  idx = str.indexOf(substr);

        if (idx == -1)
            return str;

        return (str.substring(0, idx) + replaceWith +
                ((idx + len == str.length()) ? "" : str.substring(idx + len)));
    }

    /**
     * Converts a Map<String, String> to a linearized String that can be
     * easily persisted and reconstituted into a Map.
     *
     * The format of the String is:
     *
     *  <n>;<str>;<str>;...
     *
     * Where <n> gives the number of name=value pairs in the map, followed by
     * the n=v pairs.
     */
    public static String mapToString(Map<String, String> map)
    {
        int          size = map.size();
        List<String> eles = new ArrayList<String>(size * 2 + 1);

        eles.add(Integer.toString(size));

        for (Map.Entry<String, String> entry : map.entrySet())
        {
            eles.add(entry.getKey());
            eles.add(entry.getValue());
        }

        return StringList.flattenList(eles);
    }

    /**
     * Undoes the conversion made by mapToString by parsing the string and
     * returning the resulting Map<String, String>.
     */
    public static Map<String, String> mapFromString(String mapped)
    {
        List<String>        eles = StringList.restoreList(mapped);
        Map<String, String> map  = new HashMap<String, String>();

        if (eles.size() > 0)
        {
            try
            {
                int  count = Integer.parseInt(eles.get(0));
                int  size  = eles.size();

                if (size == count * 2 + 1)
                {
                    for (int i = 1; i < size; i += 2)
                        map.put(eles.get(i), eles.get(i + 1));
                }
            }
            catch (Exception x)
            {
                x.printStackTrace();
            }
        }

        return map;
    }

    /**
     * Perform "{xyz}" substitution on format string, stuffing in the values of the
     * Map<String,String> as supplied.
     */
    public static String subargs(String fmt, Map<String, String> args)
    {
        StringBuilder sb  = new StringBuilder();
        int           off = 0;
        int           pos1;

        while ((pos1 = fmt.indexOf('{', off)) != -1)
        {
            int pos2;

            sb.append(fmt.substring(off, pos1));

            pos2 = fmt.indexOf('}', pos1);

            if (pos2 != -1)
            {
                String key = fmt.substring(pos1 + 1, pos2);
                String val = args.get(key);

                if (val != null)
                    sb.append(val);
            }
            else
            {
                off = fmt.length();
                break;
            }

            off = pos2 + 1;
        }

        sb.append(fmt.substring(off));

        return sb.toString();
    }

    public static String subargsPos(String fmt, Object... args)
    {
        StringBuilder sb  = new StringBuilder();
        int           off = 0;
        int           idx = 0;
        int           pos;

        while ((pos = fmt.indexOf("{}", off)) != -1)
        {
            String v;

            sb.append(fmt.substring(off, pos));

            try
            {
                if (idx < args.length)
                {
                    Object vv = args[idx++];

                    if (vv == null)
                        v = "<null>";
                    else
                        v = vv.toString();
                }
                else
                {
                    v = "<no arg value>";
                }
            }
            catch (Exception x)
            {
                v = "<exception " + x.getMessage() + ">";
            }

            sb.append(v);

            off = pos + 2;
        }

        sb.append(fmt.substring(off));

        return sb.toString();
    }

    public static void main(String[] args)
    {
        Map<String, String> kv = new HashMap<String, String>();

        for (int idx = 1; idx < args.length; idx++)
        {
            String[] kvv = args[idx].split("=");

            if (kvv.length == 2)
                kv.put(kvv[0], kvv[1]);
        }

        System.out.println(subargs(args[0], kv));
    }
}
