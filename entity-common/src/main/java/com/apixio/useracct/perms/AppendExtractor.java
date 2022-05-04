package com.apixio.useracct.perms;

import javax.ws.rs.core.MultivaluedMap;

import com.apixio.SysServices;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;

public class AppendExtractor implements Extractor {

    private String value;

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     */
    @Override
    public void init(InitInfo initInfo, ApiDef api, String config)
    {
        this.value = config;
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
        return prevInChain.toString() + ";" + value;
    }

}
