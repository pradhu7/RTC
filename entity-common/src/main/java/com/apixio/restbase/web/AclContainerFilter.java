package com.apixio.restbase.web;

import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

/**
 * Using a ContainerResponseFilter is the only way to wrest control from the
 * JAX-RS container to force a 403 response (e.g.) from within the ReaderInterceptor
 * code that determines that the request should be denied.  This uses a thread local
 * flag (status code) to override the default returned code (which would be a 500
 * code since the only way to abort the ReaderInterceptor.aroundReadFrom is to throw
 * an exception, which JAX-RS interprets as an internal server error).
 */
public class AclContainerFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
    {
        int statusOverride = AclChecker.getOverrideResponseStatus();

        if (statusOverride != -1)
            responseContext.setStatus(statusOverride);
    }

}
