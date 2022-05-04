package com.apixio.useracct.dw.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.XUUID;
import com.apixio.aclsys.AclConstraint;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dw.ServiceConfiguration;

/**
 * RESTful API to manage/query Grants.  
 *
 * Because these methods are protected by ACL checks, any User can call into them.
 *
 * Some of these methods must specify constraints on ("downline") subjects and objects.  These
 * constraints are specified in the HTTP body and is a stringified JSON object.  There are
 * several types of constraints:
 *
 *   * wildcard/any:  can be used for either subject or object constraint
 *   * member of usergroup:  used for subject
 *   * member of set:  used for object
 *
 */
@Path("/grants")
@Produces("application/json")
public class GrantRS extends BaseRS {

    private PrivSysServices sysServices;

    /**
     * Constraint holds both subject and object constraints.  It's intentionally general
     * purpose (two Maps!) as we want flexibility for the future.  We know that there
     * we must allow two separate constraints but beyond that it's generic.  The maps
     * must have a "type" key and the value can be one of {All, UserGroup, Set}.  The
     * other keys in the map are dependent on the type:
     *
     *  All:  no other keys needed
     *  UserGroup:  groupID=<XUUID>
     *  Set:  members=[XUUID, ...]
     */
    public static class Constraint {
        public Map<String, Object> subject;
        public Map<String, Object> object;
    }

    /**
     * Standard construction of RESTful service
     */
    public GrantRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /**
     * Attempts to grant the right to allow the subject to add permission on the operation.
     */
    @PUT
    @Path("/{subject}/{operation}")
    @Consumes("application/json")
    public Response addGrant(@Context HttpServletRequest request,
                             @PathParam("subject") final String subject, @PathParam("operation") final String operation,
                             final Constraint subjObjConstraint)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/grants/{subject}/{operation}");
        final Token               caller   = RestUtil.getInternalToken();

        logger.addParameter("subject",   subject);
        logger.addParameter("operation", operation);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    XUUID subj = XUUID.fromString(subject);

                    if (sysServices.getAclLogic().grantAddPermission(caller.getUserID(), subj, operation,
                                                                     makeConstraint(subjObjConstraint.subject),
                                                                     makeConstraint(subjObjConstraint.object)))
                        return ok();
                    else
                        return forbidden();
                }});
    }

    /**
     * Attempts to grant the right to allow the subject to add permission on the operation.
     */
    @DELETE
    @Path("/{subject}/{operation}")
    @Consumes("application/json")
    public Response removeGrant(@Context HttpServletRequest request,
                                @PathParam("subject") final String subject, @PathParam("operation") final String operation)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/grants/{subject}/{operation}");
        final Token               caller   = RestUtil.getInternalToken();

        logger.addParameter("subject",   subject);
        logger.addParameter("operation", operation);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    XUUID subj = XUUID.fromString(subject);

                    if (sysServices.getAclLogic().removeAddPermission(caller.getUserID(), subj, operation))
                        return ok();
                    else
                        return forbidden();
                }});
    }

    /**
     * Creates an AclConstraint based on the details in the Map<>.  The values in the Map are
     * different based on the type of the Acl constraint being created.
     */
    private AclConstraint makeConstraint(Map<String, Object> args)
    {
        Object typeObj = args.get("type");

        if ((typeObj == null) || !(typeObj instanceof String))
            badRequestParam("Constraint", args.toString(), "Constraints require 'type' field: {}");
        
        String type = (String) args.get("type");

        if (type.equals("All"))
        {
            return AclConstraint.allowAll();
        }
        else if (type.equals("UserGroup"))
        {
            Object ugID = args.get("groupID");

            if ((ugID == null) || !(ugID instanceof String))
                badRequestParam("Constraint", args.toString(), "UserGroup constraint must include 'groupID' field (as string): {}");

            return AclConstraint.userGroup(XUUID.fromString((String) ugID));
        }
        else if (type.equals("Set"))
        {
            Object members = args.get("members");

            if ((members == null) || !(members instanceof List))
                badRequestParam("Constraint", args.toString(), "Set constraint must include 'members' field (as list of string): {}");

            return AclConstraint.set(toXuuidList((List<String>) members));
        }
        else
        {
            badRequestParam("Constraint", args.toString(), "Constraint must be one of All, UserGroup, Set:  {}");  // throws an exception

            return null;
        }
    }

    private List<XUUID> toXuuidList(List<String> members)
    {
        List<XUUID> membersX = new ArrayList<>();

        for (String member : members)
            membersX.add(XUUID.fromString(member));

        return membersX;
    }
}
