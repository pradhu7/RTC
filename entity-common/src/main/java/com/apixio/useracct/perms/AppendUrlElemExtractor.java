package com.apixio.useracct.perms;

import javax.ws.rs.core.MultivaluedMap;

import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.exts.UrlElementExtractor;

/**
 * Syntax within apiacls.json:  "class:com.apixio.useracct.perms.AppendUrlElemExtractor:{id}"
 */
public class AppendUrlElemExtractor extends UrlElementExtractor {

    private String value;

    @Override
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity)
    {
        String ele = (String) super.extract(ctx, match, info, null, null);

        return prevInChain.toString() + ";" + ele;
    }

}
