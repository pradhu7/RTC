package com.apixio.useracct.cmdline;

import java.util.LinkedList;
import java.util.List;

/**
 * ParseState manages the extraction of options and parameters from the command
 * line, with the model being that the list of options/params is reduced as things
 * are successfully extracted.
 */
class ParseState {

    /**
     * Args left to parse
     */
    private List<String> params;

    static class NVPair {
        String name;
        String value;

        NVPair(String name, String value)
        {
            this.name  = name;
            this.value = value;
        }
    }

    /**
     * Create a ParseState by copying to the more flexible List
     */
    ParseState(String[] args)
    {
        params = new LinkedList<String>();   // linked list only because it's cheaper to delete elements

        for (String arg : args)
            params.add(arg);
    }

    /**
     * Trivial constructor
     */
    ParseState(List<String> args)
    {
        params = args;
    }

    /**
     * Returns true if there are no more parameters to do anything with.
     */
    boolean isEmpty()
    {
        return params.size() == 0;
    }

    /**
     * Returns the remaining parameters as space-separated string.
     */
    String whatsLeft()
    {
        StringBuilder sb = new StringBuilder();

        for (String param : params)
        {
            if (sb.length() > 0)
                sb.append(" ");

            sb.append(param);
        }

        return sb.toString();
    }

    List<String> whatsLeftList()
    {
        return params;
    }

    /**
     * Looks for an option flag where the presence of that option results in 'true' being
     * returned, 'false' otherwise.  An option flag has no arguments; i.e., it is something
     * like "-list".
     */
    boolean getOptionFlag(String option)
    {
        if (!option.startsWith("-"))
            throw new IllegalArgumentException("Option name [" + option + "] MUST start with -");

        for (int idx = 0, max = params.size(); idx < max; idx++)
        {
            if (params.get(idx).equals(option))
            {
                params.remove(idx);

                return true;
            }
        }

        return false;
    }

    /**
     * Looks for an option of the given type and returns the param following it,
     * removing both from the list.  The actual string value of Option MUST start
     * with "-".
     */
    String getOptionArg(String option)
    {
        if (!option.startsWith("-"))
            throw new IllegalArgumentException("Option name [" + option + "] MUST start with -");

        for (int idx = 0, max = params.size(); idx < max; idx++)
        {
            if (params.get(idx).equals(option))
            {
                if ((idx + 1) < max)
                {
                    String value = params.get(idx + 1);

                    params.remove(idx);
                    params.remove(idx);

                    return value;
                }
                else
                {
                    throw new IllegalStateException("Missing value for option " + option);
                }
            }
        }

        return null;
    }

    /**
     * Returns (and removes from the args list) the next item.  Null is returned for end-of-list.
     */
    String getNext()
    {
        String next;

        if (params.size() == 0)
            return null;

        next = params.get(0);
        params.remove(0);

        return next;
    }

    /**
     * Looks for a name=value parameter where the 'name' part is identical to the supplied
     * value in the 'name' parameter.  For example, calling
     *
     *    getOptionConstNV("description")
     *
     *  will look for a parameter of the form
     *
     *    description=something
     *
     *  (where "something" is just an example value) and return the value "something".
     *
     * If something is actually empty, then "" is returned.  If the named parameter doesn't
     * exist, then null is returned.  This allows the caller to distinguish between the two
     * cases.
     */
    String getOptionConstNV(String name)
    {
        String value = null;

        for (int idx = 0, max = params.size(); idx < max; idx++)
        {
            String param = params.get(idx);
            int    eq;

            if (param.startsWith(name) && ((eq = param.indexOf('=')) != -1))
            {
                if (eq + 1 < param.length())
                    value = param.substring(eq + 1);
                else
                    value = "";

                params.remove(idx);
                break;
            }
        }

        return value;
    }

    /**
     * Returns the next param that has the form name=value, splitting name from value
     * and returning the pieces independently.
     */
    NVPair getNextOptionNV()
    {
        NVPair  nv = null;

        for (int idx = 0, max = params.size(); idx < max; idx++)
        {
            String param = params.get(idx);
            int    eq    = param.indexOf('=');

            if (eq != -1)
            {
                nv = new NVPair(param.substring(0, eq),
                                ((eq + 1 < param.length()) ? param.substring(eq + 1) : ""));

                params.remove(idx);
                break;
            }
        }

        return nv;
    }
}
