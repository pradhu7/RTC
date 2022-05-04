package com.apixio.restbase.config;

/**
 * ConfigExpression is an extra native-like scalar that is just a String but has its
 * own type.  It's needed in order to allow clients to know how to interpret a generic
 * string value.
 */
public class ConfigExpression
{
    /**
     *
     */
    private String expression;

    private ConfigExpression(String exp)
    {
        this.expression = exp;
    }

    public static ConfigExpression valueOf(String exp)
    {
        return new ConfigExpression(exp);
    }

    @Override
    public String toString()
    {
        return expression;
    }
}
