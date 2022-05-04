package com.apixio.useracct.dw.resources;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.XUUID;
import com.apixio.restbase.web.BaseRS.ApiLogger;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.OrganizationLogic;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.Organization;

/**
 * Internal Sys (privileged) entity RESTful Service.
 */
@Path("/debug")
public class DebugRS extends BaseRS {

    private PrivSysServices sysServices;

    public DebugRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /**
     *
     */
    @GET
    @Path("/acl/orgs")
    @Produces("text/plain")
    public Response dumpOrgAcls(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/debug/acl/orgs");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    StringBuilder sb = new StringBuilder();

                    for (Organization org : sysServices.getOrganizations().getAllOrganizations(true))
                    {
                        sb.append(sysServices.getRoleLogic().dumpOrgPermissions(org.getID()));
                    }

                    return ok(sb.toString());
                }});
    }

    /**
     *
     */
    @GET
    @Path("/acl/lowlevel")
    @Produces("text/plain")
    public Response dumpLowLevelAcls(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/debug/acl/lowlevel");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    return ok(sysServices.getAclLogic().dumpAclPermissions());
                }});
    }

}
