package com.apixio.restbase.apiacl.perm;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.apixio.SysServices;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.restbase.apiacl.ApiAcls.CheckResults;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.ApiCheck;
import com.apixio.restbase.apiacl.AccessResult;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.ProtectedApi;
import com.apixio.restbase.apiacl.RestEnforcer;

public class RestPermissionEnforcer implements RestEnforcer {

    /**
     * Names of configuration keys whose values give the info about how to extract
     * that info from a request.
     */
    private final static String FIELD_SUBJECT    = "subject";
    private final static String FIELD_OPERATION  = "operation";
    private final static String FIELD_OBJECT     = "object";

    /**
     * This is to handle the case where no configuration for one of the above FIELD_*
     * is supplied.
     */
    private final static String NULL_EXTRACTOR = ExtractorFactory.EXTR_NULL + ":";

    /**
     *
     */
    private SysServices sysServices;
    private AclLogic    aclLogic;

    /**
     *
     */
    @Override
    public void init(SysServices sysServices)
    {
        this.sysServices = sysServices;

        if (sysServices != null)  // cmdline doesn't have a sysServices (for now)
            this.aclLogic = sysServices.getAclLogic();
    }

    /**
     * Creates an ApiCheck for the given protected API that checks ACL permissions on the
     * request.
     */
    @Override
    public ApiCheck createApiCheck(InitInfo initInfo, ProtectedApi api, BooleanOp op, List<Map<String, String>> configs)
    {
        return new ApiCheckPerm(initInfo, api, sysServices, aclLogic, op, configs);
    }

    /**
     * Check if the request as described by method and URL is allowed access.  HttpEntity
     * will be null if this is being called from the webfilter and will be non-null if
     * called from the ReaderInterceptor.
     *
     * If at least one extactor requires the HTTP entity and it's not supplied, then
     * NEED_ENTITY will be returned, otherwise ALLOW or DISALLOW will be returned.
     */
    public AccessResult checkPermission(CheckResults checkResults, HttpRequestInfo info, Object httpEntity) throws IOException
    {
        MatchResults match = checkResults.match;

        //        System.out.println("SFM:  RestPermissionEnforcer #1 httpEntity.class=" + ((httpEntity != null) ? httpEntity.getClass() : "<null>"));
        //        System.out.println("SFM:  RestPermissionEnforcer #2 httpEntity=" + httpEntity);

        return match.api.checkPermission(checkResults.ctx, info, match, httpEntity);
    }

}
