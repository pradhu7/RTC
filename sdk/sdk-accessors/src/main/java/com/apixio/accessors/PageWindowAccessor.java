package com.apixio.accessors;

import java.util.List;

import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxLogger;

public class PageWindowAccessor extends BaseAccessor
{

    /**
     * String config(String)
     */
    @Override
    public String getID()
    {
        return "pageWindow";
    }

    /**
     * Params will have already had all non-primitives evaluated
     */
    @Override
    public Object eval(AccessorContext context, List<Object> params) throws Exception
    {
        logger.info("PageWindowAccessor.eval(%s) returning null", params);

        return null;
    }
}
