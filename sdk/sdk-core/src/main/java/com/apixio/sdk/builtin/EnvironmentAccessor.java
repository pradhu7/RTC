package com.apixio.sdk.builtin;

import java.util.List;

import com.apixio.sdk.FxEnvironment;

/**
 * EnvironmentAccessor looks up attribute values from the configured FxEnvironment
 */
public class EnvironmentAccessor extends AttributeAccessor
{
    private final static String ACCESSOR_NAME = "environment";

    private FxEnvironment environment;

    @Override
    public void setEnvironment(FxEnvironment env)
    {
        this.environment = env;
    }

    @Override
    public String getID()
    {
        return ACCESSOR_NAME;
    }

    @Override
    public Object eval(AccessorContext context, List<Object> args) throws Exception
    {
        return super.evalAttribute(ACCESSOR_NAME, environment, args);
    }

}
