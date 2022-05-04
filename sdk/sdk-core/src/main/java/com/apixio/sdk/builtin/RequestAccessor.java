package com.apixio.sdk.builtin;

import java.util.List;

import com.apixio.sdk.FxEnvironment;

/**
 * RequestAccessor looks up attribute values from the request
 */
public class RequestAccessor extends AttributeAccessor
{
    private final static String ACCESSOR_NAME = "request";

    @Override
    public void setEnvironment(FxEnvironment env)
    {
    }

    @Override
    public String getID()
    {
        return ACCESSOR_NAME;
    }

    @Override
    public Object eval(AccessorContext context, List<Object> args) throws Exception
    {
        return super.evalAttribute(ACCESSOR_NAME, context.getFxRequest(), args);
    }

}
