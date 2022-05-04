package com.apixio.restbase.apiacl.perm.exts;

import java.util.HashMap;
import java.util.Map;

import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;

/**
 * HttpMethodExtractor selectively returns a String based on the HTTP method.
 *
 * The config syntax is:
 *
 *  <METHOD>=<String>, ...
 *
 * The last METHOD can be "*" to act as a catch-all.
 */
public class HttpMethodExtractor implements Extractor {

    private final static String CATCHALL = "*";

    private String              catchAll;
    private Map<String, String> methodMap = new HashMap<String, String>();;

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     */
    @Override
    public void init(InitInfo initInfo, ApiDef api, String config)
    {
        String[] split = config.split(",");

        for (int i = 0, m = split.length; i < m; i++)
        {
            String ele   = split[i];
            int    eq    = ele.indexOf('=');
            String value = "";
            String method;

            if (eq == -1)
                throw new IllegalArgumentException("http-method extractor has syntax of METHOD=VALUE,... Bad format in " + config);

            method = ele.substring(0, eq);

            if (eq + 1 < ele.length())
                value = ele.substring(eq + 1);

            if (method.equals(CATCHALL))
            {
                if (i + 1 < m)
                    throw new IllegalArgumentException("http-method wildcard can only be at the end of the list:  " + config);
                catchAll = value;
            }
            else
            {
                methodMap.put(method.toLowerCase(), value);
            }
        }
    }

    /**
     * Returns true if the only way for the extractor to return information is by examining
     * the HTTP entity body.
     */
    @Override
    public boolean requiresHttpEntity()
    {
        return false;
    }

    @Override
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity)
    {
        String value = methodMap.get(info.getMethod());

        return (value != null) ? value : catchAll;
    }

}
