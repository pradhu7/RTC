package com.apixio.useracct.dw.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.aclsys.entity.AccessType;
import com.apixio.aclsys.eprops.TypeProperties;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.PrivSysServices;

/**
 * Internal service.
 */
@Path("/aclat")
@Produces("application/json")
public class AccessTypeRS extends BaseRS {

    private PrivSysServices sysServices;

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class CreateAccessTypeParams {
        public String name;
        public String description;
    }

    public AccessTypeRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /**
     * Returns all AccessTypes with all their detail
     */
    @GET
    public Response getAccessTypes(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/aclat");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    List<AccessType>          ats  = sysServices.getAccessTypeLogic().getAllAccessTypes();
                    List<Map<String, String>> all  = new ArrayList<Map<String, String>>();

                    for (AccessType at : ats)
                    {
                        Map<String, String> json = new HashMap<String, String>();

                        TypeProperties.autoProperties.copyFromEntity(at, json);
                        json.put("id", at.getID().toString());

                        all.add(json);
                    }

                    return ok(all);
                }
            });
    }

    /**
     * Returns all properties of the given role in the form of a JSON object.
     */
    @GET
    @Path("/{typeName}")
    public Response getAccessType(@Context HttpServletRequest request, @PathParam("typeName") final String typeName)
    {
        ApiLogger  logger = super.createApiLogger(request, "/aclat/{typename}");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    AccessType  type = sysServices.getAccessTypeLogic().getAccessType(typeName);

                    if (type != null)
                    {
                        Map<String, String> json = new HashMap<String, String>();

                        TypeProperties.autoProperties.copyFromEntity(type, json);

                        return ok(json);
                    }
                    else
                    {
                        return badRequestParam("typeName", typeName, "AccessType {} not known");
                    }
                }
            });
    }

    /**
     * DELETEs the given AccessType.  This is a somewhat destructive operation (it can be recovered
     * from, however, by recreating the type and adding it back to the Operations).
     */
    @DELETE
    @Path("/{typeName}")
    public Response deleteAccessType(@Context HttpServletRequest request, @PathParam("typeName") final String typeName)
    {
        ApiLogger   logger   = super.createApiLogger(request, "/aclat/{typename}");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    AccessType  type = sysServices.getAccessTypeLogic().getAccessType(typeName);

                    if (type != null)
                    {
                        sysServices.getPrivAccessTypeLogic().deleteAccessType(typeName);

                        return ok();
                    }
                    else
                    {
                        return badRequestParam("typeName", typeName, "AccessType {} not known");
                    }
                }
            });
    }

    /**
     * create a new AccessType with the given name.
     *
     * API Details:
     *
     *  * must be called with ROOT role (=> token required)
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createAccessTypeJson(
        @Context  HttpServletRequest request,
        final CreateAccessTypeParams params
        )
    {
        final ApiLogger   logger   = super.createApiLogger(request, "/aclat");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    AccessType           type = sysServices.getPrivAccessTypeLogic().createAccessType(params.name, params.description);
                    Map<String, String>  json = new HashMap<String, String>(2);

                    json.put("id", type.getID().toString());

                    return ok(json);
                }
            });
    }

}
