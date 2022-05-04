package com.apixio.restbase.apiacl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.apixio.restbase.apiacl.UrlTemplate;
import com.apixio.restbase.apiacl.model.ApiDef;

/**
 * ProtectedApi is the runtime representation of a protected RESTful API.  It contains
 * all access checks (i.e., across all enforcer types) need to verify that the request
 * can be dispatched to the Java method.
 */
public class ProtectedApi {

    private ApiDef apiDef;
    private List<ApiCheck> checks = new ArrayList<ApiCheck>();

    /**
     *
     */
    protected ProtectedApi(ApiDef def)
    {
        this.apiDef = def;
    }

    /**
     * Adds a new check to the API.
     */
    void addCheck(ApiCheck check)
    {
        checks.add(check);
    }

    /**
     * Getters
     */
    public ApiDef getApiDef()
    {
        return apiDef;
    }

    public UrlTemplate getUrlTemplate()
    {
        return apiDef.getUrlTemplate();
    }

    /**
     * Checks if the given request passes all ApiChecks configured for the API.
     */
    public AccessResult checkPermission(CheckContext ctx, HttpRequestInfo ri, MatchResults match, Object httpEntity) throws IOException
    {
        if (checks.size() == 0)
        {
            ctx.recordDisallowed("Protected API doesn't have any configured ACL checks.  If there are no checks, then API request is denied.");
            return AccessResult.DISALLOW;
        }

        for (ApiCheck ac : checks)
        {
            AccessResult res = ac.checkPermission(ctx, ri, match, httpEntity);

            // If any explicitly disallow, then disallow immediately, but if any of
            // them needs the entity body, still check all the others for disallow
            // (to avoid the more expensive needs-entity path), but return
            // need_entity if none explicitly disallow
            if (res == AccessResult.DISALLOW)
            {
                ctx.recordDisallowed("API Check {} returned DISALLOW; skipping remaining ACL checks and returning DISALLOW", ac);
                return res;
            }
            else if (res == AccessResult.NOT_FOUND)
            {
                ctx.recordDisallowed("API Check {} returned NOT_FOUND; skipping remaining ACL checks and returning NOT_FOUND", ac);
                return res;
            }
            else if (res == AccessResult.NEED_ENTITY)
            {
                ctx.recordDetail("API Check {} returned NEED_ENTITY", ac);
                return res;
            }
        }

        return AccessResult.ALLOW;
    }

    /**
     * Debug.
     */
    @Override
    public String toString()
    {
        return ("[ProtectedApi def=" + apiDef + "]");
    }

}
