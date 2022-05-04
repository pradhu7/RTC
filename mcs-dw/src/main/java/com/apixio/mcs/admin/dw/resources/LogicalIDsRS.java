package com.apixio.mcs.admin.dw.resources;

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.apixio.mcs.ModelCatalogService.LifecycleException;
import com.apixio.mcs.ModelCatalogService.Operation;
import com.apixio.mcs.ModelCatalogService;
import com.apixio.mcs.admin.McsSysServices;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.web.BaseRS;

/**
 * Handles REST APIs for /mcs/logids (logical IDs) URL prefix
 */
@Path("/mcs/logids")
public class LogicalIDsRS extends BaseRS
{
    private static Logger LOGGER;

    /**
     * Allowed values for the query parameter "method" in PUT:/mcs/models/{id}/metadata
     */
    private final static String MODE_USES   = "uses";
    private final static String MODE_USEDBY = "used-by";

    /**
     * fields
     */
    protected McsSysServices      mcsServices;
    private   ModelCatalogService catalogService;

    /**
     *
     */
    public LogicalIDsRS(MicroserviceConfig configuration, McsSysServices sysServices)
    {
        super(configuration, sysServices);

        this.mcsServices    = sysServices;
        this.catalogService = mcsServices.getModelCatalogService();

        LOGGER = Logger.getLogger(LogicalIDsRS.class);
    }

    /**
     *
     */
    @PUT
    @Path("{logidleft}/deps")
    @Consumes("application/json")
    @Produces("application/json")
    public Response replaceDeps(@Context HttpServletRequest request,
                                @PathParam("logidleft") String leftID,
                                final List<String> deps   // not really application/json but it works w/ jackson
        )
    {
        ApiLogger   logger = super.createApiLogger(request, "POST:/mcs/logids", "mcsserver");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    try
                    {
                        catalogService.addDependencies(leftID, deps, true);

                        return ok();
                    }
                    catch (LifecycleException lx)
                    {
                        return lifecycleProblem(lx);
                    }
                }});
    }

    /**
     *
     */
    @POST
    @Path("{logidleft}/deps")
    @Consumes("application/json")
    @Produces("application/json")
    public Response addDeps(@Context HttpServletRequest request,
                            @PathParam("logidleft") String leftID,
                            final List<String> deps   // not really application/json but it works w/ jackson
        )
    {
        ApiLogger   logger = super.createApiLogger(request, "POST:/mcs/logids", "mcsserver");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    try
                    {
                        catalogService.addDependencies(leftID, deps, false);
                        return ok();
                    }
                    catch (LifecycleException lx)
                    {
                        return lifecycleProblem(lx);
                    }
                }});
    }

    /**
     *
     */
    @DELETE
    @Path("{logidleft}/deps")
    @Produces("application/json")
    public Response removeDeps(@Context HttpServletRequest request,
                               @PathParam("logidleft") String leftID
        )
    {
        ApiLogger   logger = super.createApiLogger(request, "POST:/mcs/logids", "mcsserver");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    try
                    {
                        catalogService.addDependencies(leftID, Collections.emptyList(), true);  // i.e., replace with empty
                        return ok();
                    }
                    catch (LifecycleException lx)
                    {
                        return lifecycleProblem(lx);
                    }
                }});
    }

    /**
     *
     */
    @GET
    @Path("{logid}/deps")
    @Produces("application/json")
    public Response getDeps(@Context HttpServletRequest    request,
                            @PathParam("logid")     String id,
                            @QueryParam("mode")     String mode
        )
    {
        ApiLogger     logger = super.createApiLogger(request, "POST:/mcs/logids", "mcsserver");
        final boolean uses   = !MODE_USEDBY.equals(mode);  // default is 'uses'

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    return ok(catalogService.getLogicalDependencies(id, uses));
                }});
    }

    /**
     * Returns JSON Blob that are owners of logical deps
     */
    @GET
    @Path("{logid}/deps/owners")
    @Produces("application/json")
    public Response getDepsOwners(@Context HttpServletRequest    request,
                                  @PathParam("logid")     String id,
                                  @QueryParam("mode")     String mode
        )
    {
        ApiLogger     logger = super.createApiLogger(request, "POST:/mcs/logids", "mcsserver");
        final boolean uses   = !MODE_USEDBY.equals(mode);  // default is 'uses'

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    return ok(catalogService.getMergedDependencies(id, uses, false));
                }});
    }

    /**
     *
     */
    @GET
    @Path("{logid}/owner")
    @Produces("application/json")
    public Response getDepOwner(@Context HttpServletRequest request,
                                @PathParam("logid") String  id
        )
    {
        ApiLogger   logger = super.createApiLogger(request, "POST:/mcs/logids", "mcsserver");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    return ok(catalogService.getOwner(id));
                }});
    }

    /**
     * Converts from exception to useful REST return
     */
    private Response lifecycleProblem(LifecycleException lx)
    {
        Operation op = lx.getOperation();

        if (op != null)
            return badRequest("Unable to perform operation " + op + " due to lifecycle state constraints");
        else
            return badRequest("Changing lifecycle state from " + lx.getTransitionFrom() + " to " + lx.getTransitionTo() + " is disallowed");
    }

}
