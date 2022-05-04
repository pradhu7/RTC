package com.apixio.restbase.apiacl;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import com.apixio.restbase.apiacl.model.HttpMethod;

/**
 * Manages a list of ProtectedApis and allows searching across all members
 * for the purpose of selecting the best match with a real request URL.
 */
public class ProtectedApiSet {

    private List<ProtectedApi> apiSet = new ArrayList<ProtectedApi>();

    /**
     * This needs to be optimized as a "best match" operation will be done for each
     * request made, and it's inefficient to loop through the entire list.  One
     * method to speed it up is to use the documented JAX-RS selection method
     * which does a first pass based on top-level matching possibilities followed
     * by searching through the rest of the APIs within that.
     *
     * So, this idea could be used here with a Map<String, List<ProtectedApi>> where
     * the String key is the first component of the URL (which works well since the
     * first component of a UrlTemplate can't be a placeholder).
     *
     * ... this will be faster if the hash look up based on the first component
     * doesn't take longer than doing the match() operation... this is probably
     * the case ...
     */

    /**
     * Adds the API to the list of ones that can be matched against.
     */
    public void addApi(ProtectedApi api)
    {
        //?? check for overlap here?

        apiSet.add(api);
    }

    /**
     * Selects the best match.  This algorithm is *supposed* to be the same as what's
     * (sort of) documented in JAX-RS, but the observed behavior of dropwizard (which
     * uses glassfish, which is the reference implementation of JAX-RS) is not what
     * is documented.
     *
     * Note that the explicitly stated intention here is for the matching against the
     * declared APIs to be entirely consistent with how JAX-RS does it so that the
     * configuration info that specifies a method/URL that looks like it should be
     * mapped to the JAX-RS-annotated one is really protecting the actual invocation
     * of that (hopefully) Java method.
     *
     * The matching algorithm must take into account the HTTP method and the URL.
     * APIs that have incompatible HTTP methods won't be considered for URL matching.
     */
    public MatchResults selectBestMatch(String httpMethod, String url)
    {
        ProtectedApi        bestApi      = null;
        Map<String, String> bestMatches  = null;
        HttpMethod          actualMethod = HttpMethod.fromString(httpMethod);
        int                 compCount    = 0;  // component count of bestApi
        int                 staticCount  = 0;  // non-placeholder count of bestApi

        // the best match is defined to be the one that has the most
        // static URL components matched and the most placeholders matched
        // (intuitively, the greater the number of specific criteria matched).
        // trailing wildcard matches less desired.  If two templates have the
        // same number of components, the one with the most non-placeholders
        // is better

        for (ProtectedApi api : apiSet)
        {
            UrlTemplate tpl = api.getUrlTemplate();

            if (tpl.compatibleMethod(actualMethod))
            {
                Map<String, String> matches = api.getUrlTemplate().match(httpMethod, url);

                //!! this doesn't match observed matching algorithm used by dropwizard:
                // that algorithm appears to favor URLs with the left-most placeholder
                // and with the most specificity
                if (matches != null)
                {
                    int tplCount  = tpl.getComponentCount();
                    int tplStatic = tplCount - matches.size();

                    if ((bestApi == null) ||
                        (tplCount > compCount) ||
                        ((tplCount == compCount) && (tplStatic > staticCount)))
                    {
                        bestMatches = matches;
                        bestApi     = api;
                        compCount   = tplCount;
                        staticCount = tplStatic;
                    }
                }
            }
        }

        if (bestApi != null)
            return new MatchResults(bestApi, bestMatches);
        else
            return null;
    }

}
