package com.apixio.sdk.builtin;

import java.util.List;

import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxAttributes;

/**
 * AttributeAccessor looks up a (string) attribute value by name.
 */
public abstract class AttributeAccessor implements Accessor
{
    /**
     * Look it up and return, but check for correct number of args (1) and non-null return value
     */
    protected Object evalAttribute(String id, FxAttributes attributes, List<Object> args) throws Exception
    {
        Object arg;
        Object val;

        if (args.size() != 1)
            throw new IllegalStateException("'" + id + "' accessor expects 1 arg but got " + args.size());
        else if ((arg = args.get(0)) == null)
            throw new IllegalArgumentException("'" + id + "' accessor requires non-null arg");

        val = attributes.getAttribute((String) arg);

        if (val == null)
            throw new IllegalStateException("'" + id + "' accessor got null for attribute '" + arg + "'");

        return val;
    }

}
