package com.apixio.sdk.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.yaml.snakeyaml.Yaml;

/**
 * This class processes a .yaml file that has macros in it that allow referencing YAML keys
 * within the same file.  A macro is a dotted identifier contained within a {% %} construct
 * where the dotted list of identifiers defines the "path" of YAML keys to traverse to get
 * the actual value.  For example, a .yaml file like:
 *
 *  topKey:
 *    key1: hello
 *  midKey:
 *    key2:  {% topKey.hello %}
 *
 * would end up with the value "hello" for the YAML key "key2".
 *
 * Macros can reference other macros to any depth and any set of macros that is recursively
 * defined (which would cause infinite recursion when evaluating) will be detected and
 * an exception will be thrown.
 *
 * Note that there is no support for quoting the two character sequence {%
 *
 * If the macro string begins with "$" then it's taken as the name of an environment
 * variable instead of a reference to a yaml key.  For example:
 *
 *    s3:
 *      accessKey: {% $S3_ACCESS_KEY %}
 *
 * will assign to s3.accessKey whatever the value of the S3_ACCESS_KEY environment
 * varialbe is.  It's an error if there is no environment variable (value).
 */
public class MacroedYaml
{

    /**
     * Start/end of macro strings.  Macro names within these begin/end will be trimmed before
     * being looked up as YAML (dotted) keys.
     */
    private final static String MACRO_OPEN  = "{%";
    private final static String MACRO_CLOSE = "%}";

    /**
     * The structure of this is defined by snakeyaml and the keys will generally be one of 
     * [scalar, map, list].  The snakeyaml-parsed structure is visible to the client of this
     * class.
     */
    private Map<String,Object> loadedYaml;

    /**
     * Create a new MacroedYaml from the contents of the given local file
     */
    public static MacroedYaml readYaml(String filepath) throws Exception
    {
        MacroedYaml my   = new MacroedYaml();
        Yaml        yaml = new Yaml();

        try (InputStream is = new FileInputStream(filepath))
        {
            my.loadedYaml = (Map<String,Object>) yaml.load(is);
            my.processMacros();
        }

        return my;
    }

    /**
     * Create an instance from the more generic Map<String,Object>.  The supplied
     * Map is presumed to be from either getYaml() or findValue()--any map constructed
     * differently will have unpredictable results.
     *
     * This construction method is really for the convenience of the client to be able
     * to do a dotted-path findValue on the result of a previous findValue().
     */
    public static MacroedYaml fromMap(Map<String,Object> yaml, boolean processMacros)
    {
        MacroedYaml my = new MacroedYaml();

        my.loadedYaml = yaml;

        if (processMacros)
            my.processMacros();

        return my;
    }

    /**
     * Return the macro-processed YAML data with macros replaced.
     */
    public Map<String,Object> getYaml()
    {
        return loadedYaml;
    }

    /**
     * Return the YAML value at the dotted path, where nested keys are separated by 
     * dot.  Accessing elements of a list is not supported.  If a non-terminal value
     * is requested/found, then a Map<String,Object> is returned and it's up to the client
     * to walk that structure as necessary as it reflects the yaml file structure.  This
     * is not an issue if the map is just one level deep
     */
    public Object findValue(String dottedPath)
    {
        return findValue(loadedYaml, dottedPath);
    }

    /**
     * Starts the recursive evaluation
     */
    private void processMacros()
    {
        processMacros("", loadedYaml);
    }

    /**
     * Replaces values with macros with expanded/replaced values.  Only String values can have macro
     * references.  Macros are (dotted) identifiers surrounded by {% %}s.
     *
     * The macro trigger is "{%" and there is no support currently for that string to be kept as-is
     * via quoting (for example).
     */

    /**
     * Iterate through all values of a Map, replacing macros in strings and recursing if the value
     * is a a list or map
     */
    private void processMacros(String context, Map<String,Object> root)
    {
        Set<String> evaluating = new HashSet<>();

        for (Map.Entry<String,Object> entry : root.entrySet())
        {
            final String key   = entry.getKey();
            final Object value = entry.getValue();

            // last param 'updater' will update the value of the current map entry
            processValue(evaluating, (context + "." + key), value,
                         nv -> root.put(key, nv) );
        }
    }

    /**
     * Iterate through all values of a List, replacing macros in strings and recursing if the value
     * is a a list or map
     */
    private void processMacros(String context, List<Object> list)
    {
        Set<String> evaluating = new HashSet<>();

        for (int i = 0, m = list.size(); i < m; i++)
        {
            final int idx = i;

            // last param 'updater' will update the value at the current list index
            processValue(evaluating, (context + "[" + Integer.toString(i) + "]"), list.get(i),
                         nv -> list.set(idx, nv) );
        }
    }

    /**
     * Common method to examine a value that might contain a macro or that migh be an aggregate of values
     * that might.  Recurses into lists or map and replaces macros in strings.
     */
    private void processValue(Set<String> evaluating, String context, Object value, Consumer<String> updater)
    {
        if (value instanceof String)
        {
            String strVal = (String) value;

            if (hasMacro(strVal))
                updater.accept(replaceMacros(evaluating, context, strVal));
        }
        else if (value instanceof Map)
        {
            processMacros(context, (Map<String,Object>) value);
        }
        else if (value instanceof List)
        {
            processMacros(context, (List<Object>) value);
        }
    }

    /**
     * We know there's at least an embedded macro and could contain multiple macros.
     */
    private String replaceMacros(Set<String> evaluating, String key, String s)
    {
        StringBuilder replaced = new StringBuilder();
        int           pos      = 0;
        int           max      = s.length();
        
        // chop string into sequences of "preStuff" {% macro %}, followed by "postStuff"

        while (pos < max)
        {
            int start = s.indexOf(MACRO_OPEN, pos);

            if (start != -1)
            {
                int    end = s.indexOf(MACRO_CLOSE, start);
                String macro;
                Object val;
                String strVal;

                replaced.append(s.substring(pos, start));

                start += MACRO_OPEN.length();

                if (end == -1)
                    macro = s.substring(start);
                else
                    macro = s.substring(start, end);

                macro = macro.trim();

                if (evaluating.contains(macro))
                    throw new IllegalStateException("Recursively defined macros: " + evaluating);

                val = replaceMacro(key, macro);

                if (val == null)
                    throw new IllegalStateException("Macro name '" + macro + "' in " + key + " wasn't found or had no value");

                strVal = (String) val;

                // recursive macro references will be picked up by keeping track of the macros that
                // are still in process ("evaluating" set of strings, which are the macro names)
                if (hasMacro(strVal))
                {
                    evaluating.add(macro);
                    strVal = replaceMacros(evaluating, key, strVal);
                    evaluating.remove(macro);
                }

                replaced.append(strVal);

                if (end == -1)
                    break;
                pos = end + MACRO_CLOSE.length();
            }
            else
            {
                replaced.append(s.substring(pos));
                break;
            }
        }

        return replaced.toString();
    }

    /**
     * Replaces what's in {% ... %} (i.e., the "..." trimmed of whitespace) with whatever it's
     * referencing.  There are two things that can be referenced:  an environment variable and
     * a yaml key.  If the name starts with "$" then the rest of the ref is taken to be the
     * environment variable name; otherwise it's taken to be a yaml key
     */
    private Object replaceMacro(String key, String ref)
    {
        if (ref.startsWith("$"))
        {
            if (ref.length() > 1)
                return System.getenv(ref.substring(1));
            else
                throw new IllegalArgumentException("Empty environment variable name in key " + key);
        }
        else
        {
            return findValue(ref);  // call top-level/public method
        }
    }

    /**
     * Cheap test to see if there is at least one macro ref in the string.  This
     * is slightly inefficient overall as another scan will be done in replaceMacros.
     */
    private static boolean hasMacro(String s)
    {
        return (s.indexOf(MACRO_OPEN) != -1);
    }

    /**
     * Given a dotted path to some map's key, locate it.  This is done by pulling off the
     * first element of the path to the '.', and locating that key in the map and if
     * there is no more to the path, then return the value, otherwise recursively call
     * this method with the remainder and the element (which must be a map).
     */
    private Object findValue(Map<String,Object> ref, String dottedPath)
    {
        int dot = dottedPath.indexOf('.');

        if (dot == -1)
        {
            return ref.get(dottedPath);
        }
        else
        {
            Object value = ref.get(dottedPath.substring(0, dot));

            if (dot == dottedPath.length())
                throw new IllegalStateException("It appears dotted path ends with a '.'--that's bad");
            else if (!(value instanceof Map))
                throw new IllegalStateException("Yaml value at ref is NOT a Map!");

            return findValue((Map<String,Object>) value, dottedPath.substring(dot + 1));
        }
    }

    /**
     * Test/debug
     */
    public static void main(String... args) throws Exception
    {
        if (args.length == 0)
        {
            System.err.println("Usage:  MacroedYaml .yaml [key...]");
        }
        else
        {
            MacroedYaml yaml = readYaml(args[0]);

            for (int i = 1; i < args.length; i++)
            {
                System.out.println(yaml.findValue(args[i]));
            }
        }
    }

}
