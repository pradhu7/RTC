package com.apixio.restbase.apiacl.dwutil;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import com.apixio.restbase.apiacl.AccessResult;
import com.apixio.restbase.apiacl.ApiAclException;
import com.apixio.restbase.apiacl.ApiAcls.CheckResults;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.perm.RestPermissionEnforcer;
import com.apixio.restbase.web.AclChecker;

/**
 * ApiReaderInterceptor implements JAX-RS's mechanism to intercept the HTTP entity
 * body before it's passed to the invoked Java method.  It appears to support
 * the concept we need of authorization also (in that it prioritizes auth checking
 * correctly).
 *
 * We perform the ACL check after the HTTP entity body has been reconstructed.
 */
@Priority(Priorities.AUTHORIZATION)
public class ApiReaderInterceptor implements ReaderInterceptor {
 
    private RestPermissionEnforcer enforcer;

    public ApiReaderInterceptor(RestPermissionEnforcer enforcer)
    {
        this.enforcer = enforcer;
    }

    /**
     * Check ACLs after the entity body has been constructed.  Throw an exception
     * to disallow continuation of processing the request due to an ACL check failure.
     * To avoid causing a 5xx code to be returned to the HTTP client, we set an override
     * code on the thread that's picked up by a ContainerReseponseFilter.
     */
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context)
        throws IOException, WebApplicationException {

        Object       entity = context.proceed();
        CheckResults check  = AclChecker.getCheckResults();

        if ((check != null) &&
            enforcer.checkPermission(check, new HttpRequestInfo(context.getHeaders()), entity) == AccessResult.DISALLOW)
        {
            AclChecker.overrideResponseStatus(403);

            throw new ApiAclException("Denied access via API ACL:  " + check.ctx);
        }

        return entity;
    }

}
