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

import com.apixio.aclsys.entity.Operation;
import com.apixio.aclsys.eprops.OpProperties;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dw.ServiceConfiguration;

/**
 * Internal service.  A Operation is a high-level construct such as "CanCodeFor" and
 * is verb-ish in nature.  Operations are defined by the application and not the
 * lower-level services.  They are referenced externally by their unique name (as
 * compared to their XUUID) since they are not frequently created and are managed
 * by application developers.
 *
 * A Operation has a name, a description, and a list of AccessTypes that really define
 * what it means using the lower-level (service-level) constructs.
 *
 * The URL model is that a Operation is a collection:
 *
 *  POST /aclp:                   creates a new Operation resource; can include List<AccessType>
 *  POST /aclp/{operationName}   adds an AccessType to the given resource
 *  GET  /aclp/{operationName}   returns name, desc, list of AccessTypes (names)
 */
@Path("/aclop")
@Produces("application/json")
public class OperationRS extends BaseRS {

    private PrivSysServices sysServices;

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class CreateOperationParams {
        public String name;
        public String description;
        public List<String> accessTypes;
    }

    public OperationRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /**
     * Returns all Operations with all their details.
     */
    @GET
    public Response getOperations(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/aclop");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    List<Operation>           ops  = sysServices.getOperationLogic().getAllOperations();
                    List<Map<String, String>> all  = new ArrayList<>();

                    for (Operation op : ops)
                    {
                        Map<String, String> json = new HashMap<>();

                        OpProperties.autoProperties.copyFromEntity(op, json);
                        json.put("id", op.getID().toString());
                        json.put("accessTypes", flatten(op.getAccessTypes()));

                        all.add(json);
                    }

                    return ok(all);
                }
            });
    }

    private String flatten(List<String> list)
    {
        StringBuilder sb = new StringBuilder();

        for (String s : list)
        {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(s);
        }

        return sb.toString();
    }

    /**
     * Returns all properties of the given Operation in the form of a JSON object.
     */
    @GET
    @Path("/{opName}")
    public Response getOperation(@Context HttpServletRequest request, @PathParam("opName") final String opName)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/aclop/{opname}");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Operation  op = sysServices.getOperationLogic().getOperation(opName);

                    if (op != null)
                    {
                        Map<String, String> json = new HashMap<>();

                        OpProperties.autoProperties.copyFromEntity(op, json);

                        return ok(json);
                    }
                    else
                    {
                        return badRequestParam("opName", opName, "Operation name {} not known");
                    }
                }
            });
    }

    /**
     * DELETEs the given Operation.  This is an EXTREMELY DESTRUCTIVE operation as it deletes
     * ALL access control information related to the Operation, including permissions granted
     * to entities.  There is no recovery from this aside from database restore.
     */
    @DELETE
    @Path("/{opName}")
    public Response deleteOperation(@Context HttpServletRequest request, @PathParam("opName") final String opName)
    {
        ApiLogger   logger   = super.createApiLogger(request, "/aclop/{opName}");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    Operation  op = sysServices.getOperationLogic().getOperation(opName);

                    // Note that we hit two data stores here:  Redis (for the Operation definition)
                    // and Cassandra (for the ACL data related to the Operation).  There is no
                    // transactional support across these two sources so we have to decide which
                    // one is easier to recover from should there be a failure after the first
                    // succeeds but before second one does.

                    if (op != null)
                    {
                        sysServices.getPrivAclLogic().deleteOperation(opName);

                        return ok();
                    }
                    else
                    {
                        return badRequestParam("opName", opName, "Operation name {} not known");
                    }
                }
            });
    }

    /**
     * create a new Operation with the given name.
     *
     * API Details:
     *
     *  * must be called with ROOT role (=> token required)
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createOperationJson(
        @Context  HttpServletRequest request,
        final CreateOperationParams  params
        )
    {
        ApiLogger  logger = super.createApiLogger(request, "/aclop");
        logger.addParameter("name", params.name);
        logger.addParameter("description", params.description);
        logger.addParameter("accessTypes", params.accessTypes);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Operation           op   = sysServices.getPrivOperationLogic().createOperation(params.name, params.description, params.accessTypes);
                    Map<String, String> json = new HashMap<>(2);

                    json.put("id", op.getID().toString());

                    return ok(json);
                }});
    }

}
