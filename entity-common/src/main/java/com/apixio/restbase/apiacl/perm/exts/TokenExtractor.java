package com.apixio.restbase.apiacl.perm.exts;

import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.Token;

public class TokenExtractor implements Extractor {

    private Tokens tokens;

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     */
    @Override
    public void init(InitInfo initInfo, ApiDef api, String config)
    {
        if (initInfo.sysServices != null)
            tokens = initInfo.sysServices.getTokens();
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
     * Gets the UserID (as a String) from the HTTP "Authorization: Apxio xxx" header line by
     * looking up the token and getting its UserID.
     */
    @Override
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity)
    {
        String auth = info.getAuthHeader();

        if (auth != null)
        {
            XUUID tokenID = RestUtil.getTokenFromHeaderValue(auth);

            if (tokenID != null)
            {
                Token token = tokens.findTokenByID(tokenID);

                if (token != null)
                    return token.getUserID().toString();
            }
        }

        return null;
    }

}
