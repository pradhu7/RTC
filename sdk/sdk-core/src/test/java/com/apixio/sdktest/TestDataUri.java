package com.apixio.sdktest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.apixio.dao.utility.DaoServices;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxExecutor;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.builtin.ApxQueryDataUriManager;
import com.apixio.sdk.protos.EvalProtos.ArgList;
import com.apixio.sdk.util.FxEvalParser;

public class TestDataUri
{

    public static class TestEnvironment implements FxEnvironment
    {
        private Map<String,String> attrs;

        TestEnvironment(Map<String,String> attrs)
        {
            this.attrs = attrs;
        }

        public FxLogger getLogger()
        {
            return null;
        }

        public DaoServices getDaoServices()
        {
            return null;
        }
        public FxExecutor getFxExecutor()
        {
            return null;
        }
        public Map<String,String> getConfig()
        {
            return null;
        }
        public String getAttribute(String id)
        {
            return attrs.get(id);
        }
        public Map<String,String> getAttributes()
        {
            return Collections.unmodifiableMap(attrs);
        }
    }

    public static class TestRequest implements FxRequest
    {
        private Map<String,String> attrs;

        TestRequest(Map<String,String> attrs)
        {
            this.attrs = attrs;
        }

        public String getAttribute(String id)
        {
            return attrs.get(id);
        }
        public Map<String,String> getAttributes()
        {
            return Collections.unmodifiableMap(attrs);
        }

        @Override
        public void setAttribute(String id, String val) {
            attrs.put(id, val);
        }
    }

    public static void main(String... args) throws Exception
    {
        TestEnvironment te = new TestEnvironment(makeEnvironment());
        TestRequest     tr = new TestRequest(makeRequest());

        if (args.length != 1)
            throw new IllegalArgumentException("Usage:  TestDataUri {argsEval}");

        testApxQuery(te, tr, FxEvalParser.parse(args[0]));
    }

    private static void testApxQuery(FxEnvironment env, FxRequest req, ArgList argList) throws Exception
    {
        ApxQueryDataUriManager aqdm = new ApxQueryDataUriManager();

        aqdm.setEnvironment(env);
        aqdm.setPrimaryKeyArgs(argList, Arrays.asList("mcid", "docuuid"));
        System.out.println("groupingID:  " + aqdm.makeGroupingID(req));
        System.out.println("dataURI:     " + aqdm.makeDataURI(req));
        System.out.println("querykeys:   " + aqdm.makeQueryKeys(req).getKeys());

        System.out.println("rt qk:       " + aqdm.getQueryKeys(aqdm.makeDataURI(req)).getKeys());
    }


    private static Map<String,String> makeEnvironment()
    {
        Map<String,String> attrs = new HashMap<>();

        attrs.put("mcid", "B_11111111-3c11-4745-b6aa-4c35d51e0c88");

        return attrs;
    }

    private static Map<String,String> makeRequest()
    {
        Map<String,String> attrs = new HashMap<>();

        attrs.put("docuuid", "DOC_22222222-1a54-4db9-8250-9d40b585887d");

        return attrs;
    }

}
