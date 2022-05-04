package com.apixio.restbase.apiacl.perm;

import java.io.IOException;

import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;

public interface Extractor {

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     *
     * If the extractor is incompatible with some aspect of the API definition, the extractor
     * MUST throw an exception to prevent bootup.
     */
    public void init(InitInfo initInfo, ApiDef api, String config);

    /**
     * Returns true if the only way for the extractor to return information is by examining
     * the HTTP entity body.
     */
    public boolean requiresHttpEntity();

    /**
     * The only info that can be supplied here is what's available in ReaderInterceptor.readAround
     * which is ReaderInterceptorContext (which has headers) and the object returned from the
     * proceed() method.
     */
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity) throws IOException;

}
