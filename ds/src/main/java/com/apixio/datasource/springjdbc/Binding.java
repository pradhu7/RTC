package com.apixio.datasource.springjdbc;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binding contains the set of actual arg values for the placeholders in a SQL statement (query,
 * insert, update, delete).  The binding is either positional or by name.
 *
 * Positional means that the client must provide the args in the same logical order as the
 * occurrence of '?'  placeholders in the SQL string.
 *
 * Binding by name (which JDBC doesn't directly support) by the typical convention of ":name"
 * instead of the "?" used in PreparedStatement.  The raw SQL string is scanned for ":xyz"
 * before submitting to a PreparedStatement and param values are bound by name.  This
 * responsibility is shared between Binding and EntitiesDS.  A given instance of Binding can
 * contain only one of the two types (by-position and by-name).
 */
public class Binding
{
    /**
     * Collects args added one by one; "by position"
     */
    private List<Object> collectedArgs = new ArrayList<>();

    /**
     * For binding by named parameters
     */
    private Map<String, Object> byNamedParams;

    /**
     * Client must choose by-position or by-name
     */
    private Binding()
    {
    }

    /**
     * args[0] is JDBC PreparedStatement's placeholder 1, ...
     */
    public static Binding byPosition(Object... args)
    {
        Binding binding = new Binding();

        binding.collectedArgs = new ArrayList<Object>();

        for (Object arg : args)
            binding.collectedArgs.add(convertToSqlType(arg));

        return binding;
    }

    /**
     * args.get(0) is JDBC PreparedStatement's placeholder 1, ...
     */
    public static Binding byPosition(List<Object> args)
    {
        Binding binding = new Binding();

        binding.collectedArgs = new ArrayList<>(args);

        return binding;
    }

    /**
     * For custom ":name" named parameter within PreparedStatement
     */
    public static Binding byName(String name, Object value)
    {
        Map<String, Object> param = new HashMap<>();

        param.put(name, value);

        return byName(param);
    }

    public static Binding byName()
    {
        Map<String, Object> param = new HashMap<>();

        return byName(param);
    }

    /**
     * Binding takes ownership of kv parameter so any calls to addByName() will add to the
     * map that's passed in here.
     */
    public static Binding byName(Map<String, Object> kv)
    {
        if (kv == null)
            throw new IllegalArgumentException("binding by name requires non-null map");

        Binding binding = new Binding();

        binding.byNamedParams = new HashMap<>();

        for (Map.Entry<String, Object> entry : kv.entrySet())
            binding.byNamedParams.put(entry.getKey(), convertToSqlType(entry.getValue()));

        return binding;
    }

    /**
     * Add args one at a time.  Disallowed if instance was created for by-name
     */
    public Binding addByPosition(Object o)
    {
        if (bindingIsByNamedParams())
            throw new IllegalStateException("Can't add arg when binding by named parameters");

        collectedArgs.add(convertToSqlType(o));

        return this;
    }

    /**
     * Adds name=value binding for by-name instance
     */
    public Binding addByName(String name, Object arg)
    {
        if (!bindingIsByNamedParams())
            throw new IllegalStateException("Can't add named arg when not binding by named parameters");

        byNamedParams.put(name, convertToSqlType(arg));

        return this;
    }

    /**
     * Use byNamedParams as marker
     */
    public boolean bindingIsByNamedParams()
    {
        return (byNamedParams != null);
    }

    /**
     * Returns arg values to bind to the placeholders in a SQL string.  Binding is by position only
     */
    public Object[] getByPosition()
    {
        if (bindingIsByNamedParams())
            throw new IllegalStateException("Can't get args when binding by named parameters");

        return collectedArgs.toArray(new Object[collectedArgs.size()]);
    }

    /**
     * Return map of name=value for binding by named parameters
     */
    public Map<String, Object> getNamedParameters()
    {
        if (!bindingIsByNamedParams())
            throw new IllegalStateException("Can't get named params when not binding by named parameters");

        return byNamedParams;
    }

    /**
     *
     */
    private static Object convertToSqlType(Object o)
    {
        if (o instanceof Date)
            return new Timestamp(((Date) o).getTime());
        else
            return o;
    }

    @Override
    public String toString()
    {
        return ("Binding(bypos=" + collectedArgs + ", byname=" + byNamedParams + ")");
    }

}
