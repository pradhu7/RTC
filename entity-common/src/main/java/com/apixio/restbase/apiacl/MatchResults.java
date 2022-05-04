package com.apixio.restbase.apiacl;

import java.util.Map;

/**
 * Records the results of matching a real request URL against a set of ProtectedApis.
 * Such a matching operation will end up selecting a single Api, which will have a
 * binding from placeholder names to actual URL component values.
 */
public class MatchResults {
    public ProtectedApi        api;
    public Map<String, String> pathParams;

    MatchResults(ProtectedApi api, Map<String, String> params)
    {
        this.api        = api;
        this.pathParams = params;
    }

}
