package com.apixio.sdk.builtin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.net.URI;

import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.util.FxEvalParser;
import com.apixio.sdk.FxRequest;

public class MakeUriAccessor extends AttributeAccessor
{

    private   FxEnvironment env;

    /**
     * String config(String)
     */
    @Override
    public String getID()
    {
        return "makefsuri";
    }

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        this.env = env;
    }

    private URI makeUriFromElements(List<Object> params) throws Exception
    {
        ApxQueryDataUriManager dum = new ApxQueryDataUriManager();

        dum.setEnvironment(env);
        dum.setPrimaryKeyArgs(FxEvalParser.parse("'" + params.get(0) + "','" + params.get(1) + "'"),
                              Arrays.asList("itemuuid", "algo"));

        return dum.makeDataURI(new DummyRequest());
    }

    /**
     * Params will have already had all non-primitives evaluated
     */
    @Override
    public Object eval(AccessorContext context, List<Object> params) throws Exception
    {
        // logger.info("MakeUriAccessor.eval(%s)", params);

        return makeUriFromElements(params).toString();
    }

    private static class DummyRequest implements FxRequest
    {
        DummyRequest()
        {
        }

        public String getAttribute(String id)
        {
            return null;
        }

        public Map<String, String> getAttributes()
        {
            return null;
        }

        public void setAttribute(String name, String value)
        {
        }
    }
}
