package com.apixio.restbase.apiacl.perm.exts;

import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;
import com.apixio.utility.StringUtil;

public class UrlElementExtractor implements Extractor {

    private String template;

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     */
    @Override
    public void init(InitInfo initInfo, ApiDef api, String config)
    {
        template = config;   // config will contain a string like "{userID}"
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

    /**
     * Forms/extracts by substituting in the actual matched URL placeholders into the config
     * string.  This allows a config string of:
     *
     *   {userID}:{groupID}          # note that the config MUST include { and }
     *
     * to return a value of
     *
     *   U_someuuidhere:UG_anotheruuidhere
     *
     * from the API definition of
     *
     *   /users/{userID}/group/{groupID}
     *
     * for the actual requested URL of:
     *
     *   /users/U_someuuidhere/group/UG_anotheruuidhere
     *
     * Fun!
     */
    @Override
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity)
    {
        return StringUtil.subargs(template, match.pathParams);  // .subargs substitutes all {id} placeholders
    }

}
