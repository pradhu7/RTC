package com.apixio.restbase.config;

import java.util.HashMap;
import java.util.Map;

/**
 * MacroUtil provides name=value substitutions within String values where
 * the macro name marker is between {} or {{}}.  The name=value substitutions
 * are contained in a Map<String, String> object.
 */
public class MacroUtil
{
    /**
     * Invoked when the {name} or {{name}} needs to be translated to the macro value
     */
    public interface MacroReplacer
    {
        public String replaceMacro(String key, Map<String, String> macros);
    }

    private static MacroReplacer cSimpleReplacer = new MacroReplacer()
        {
            @Override
            public String replaceMacro(String key, Map<String, String> macros)
            {
                return simpleReplaceMacro(key, macros);
            }
        };

    /**
     * Looks for occurrences of {id} and looks up the id in the macro map and
     * replaces it with the value.  Instances of {{id}} are not substituted.
     *
     * The macro substitution allows a default by following the macro name with
     * a "|value", like "|3" for a default of 3 if the macro value isn't found.
     */
    public static String replaceMacros(Map<String, String> macros, boolean repDouble, String str)
    {
        return replaceMacros(macros, repDouble, str, cSimpleReplacer);
    }

    public static String replaceMacros(Map<String, String> macros, boolean repDouble, String str, MacroReplacer replacer)
    {
        if (replacer == null)
            throw new IllegalArgumentException("MacroReplacer can't be null");

        boolean       noMac = ((macros == null) || (macros.size() == 0));
        StringBuilder sb    = new StringBuilder();
        int           off   = 0;
        int           pos1;

        while ((pos1 = str.indexOf('{', off)) != -1)
        {
            // deal with {{ }}
            if (isSpecialStart(str, pos1))
            {
                int pos2 = pos1 + 2;  // first char after {{

                if (repDouble)  // replace if we allow it, otherwise keep it all
                {
                    int pos3 = str.indexOf("}}", pos2);

                    sb.append(str.substring(off, pos1));

                    if (pos3 != -1)
                    {
                        String key = str.substring(pos2, pos3);
                        String val = (noMac) ? "" : replacer.replaceMacro(key, macros);

                        if (val != null)
                            sb.append(val);

                        off = pos3 + 2;
                    }
                    else
                    {
                        off = str.length();   // ignore rest of string beginning at {{
                        break;
                    }
                }
                else
                {
                    sb.append(str.substring(off, pos2));  // keep ...{{
                    off = pos2;
                }
            }
            else
            {
                int pos2;

                sb.append(str.substring(off, pos1));

                pos2 = str.indexOf('}', pos1);

                if (pos2 != -1)
                {
                    String key = str.substring(pos1 + 1, pos2);
                    String val = (noMac) ? "" : replacer.replaceMacro(key, macros);

                    if (val != null)
                        sb.append(val);
                }
                else
                {
                    off = str.length();
                    break;
                }

                off = pos2 + 1;
            }
        }

        sb.append(str.substring(off));

        return sb.toString();
    }

    private static String simpleReplaceMacro(String key, Map<String, String> macros)
    {
        int    qu  = key.indexOf('|');
        String val = null;

        // use default as necessary
        if (qu == -1)
        {
            val = macros.get(key.trim());
        }
        else if (qu + 1 < key.length())
        {
            String defVal = key.substring(qu + 1).trim();

            key = key.substring(0, qu).trim();
            val = macros.get(key);

            if (val == null)
                val = defVal;
        }

        return val;
    }

    private static boolean isSpecialStart(String str, int pos)
    {
        return (pos + 2 < str.length()) && (str.charAt(pos) == '{') && (str.charAt(pos + 1) == '{');
    }

    /**
     * For testing.  Usage:  java com.apixio.restbase.config.MacroUtil n1:v1 n2:v2 n3:v3 ... stringWithMacros
     */
    public static void main(String[] args)
    {
        Map<String, String> macros = new HashMap<>();

        for (int i = 0; i < args.length - 1; i++)
        {
            int eq = args[i].indexOf(':');

            macros.put(args[i].substring(0, eq), args[i].substring(eq + 1));
        }

        System.out.println("Macros:  " + macros);
        System.out.println("{} replacement:   " + replaceMacros(macros, false, args[args.length - 1]));
        System.out.println("{{}} replacemnt:  " + replaceMacros(macros, true,  args[args.length - 1]));
    }

}
