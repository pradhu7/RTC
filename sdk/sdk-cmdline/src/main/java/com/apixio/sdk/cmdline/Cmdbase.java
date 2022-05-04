package com.apixio.sdk.cmdline;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick-n-dirty command line base class/library.  Command line options are of the form
 * name=value
 */
public class Cmdbase
{
    static
    {
        // total hack to reduce the spewage from httpclient wire logging
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http.wire")).setLevel(ch.qos.logback.classic.Level.WARN);
    }

    /**
     * Used to convert main(String...) to List that be used in require() and optional()
     */
    protected List<String> toModifiableList(String[] a)
    {
        List<String> l = new ArrayList<>();

        for (String e : a)
            l.add(e);

        return l;
    }

    /**
     * Get a command line arg of the form name=value (name is given) removing the matching
     * element from the list of args, returning the "value" part of it, and throwing an
     * exception if there is no such element in the list.
     */
    protected String require(List<String> args, String name) throws IllegalStateException
    {
        String arg = findArg(args, name);

        if (arg == null)
            throw new IllegalStateException("Missing required argument " + name);
        else
            return arg;
    }

    /**
     * Get a command line arg of the form name=value (name is given) removing the matching
     * element from the list of args, returning the "value" part of it.
     */
    protected String optional(List<String> args, String name)
    {
        return findArg(args, name);
    }

    /**
     * Look for an element of the list that starts with name= and return the substring after
     * the =
     */
    private String findArg(List<String> args, String name)
    {
        int len = name.length() + 1;

        name = name + "=";

        for (int i = 0, m = args.size(); i < m; i++)
        {
            String arg = args.get(i);

            if (arg.startsWith(name) && arg.length() > len)
            {
                String val = arg.substring(len);

                args.remove(i);
                
                return val;
            }
        }

        return null;
    }

}
