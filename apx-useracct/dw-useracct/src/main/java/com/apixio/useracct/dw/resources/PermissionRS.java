package com.apixio.useracct.dw.resources;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.User;

/**
 * RESTful API to manage/query Permissions.
 *
 * This is meant to be a fairly minimal (initially) API that allows testing, adding, and removing
 * of permissions.  It does not yet allow generalized querying (e.g., "who has rights on this object?").
 *
 * As these methods are protected by ACL checks, any User can call into them.
 */
@Path("/perms")
@Produces("application/json")
public class PermissionRS extends BaseRS {

    private PrivSysServices sysServices;

    public PermissionRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /**
     * Checks if the Subject can perform Operation on the Object and returns OK if allowed
     * and FORBIDDEN if not allowed.  For convenience, if the value of subject is an email
     * address, a user lookup is done.
     */
    @GET
    @Path("/{subject}/{operation}/{object}")
    public Response getPermissionFor(@Context HttpServletRequest request,
                                     @PathParam("subject")   final String subject,
                                     @PathParam("operation") final String operation,
                                     @PathParam("object")    final String object)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/perms/{subject}/{operation}/{object}");

        logger.addParameter("subject",   subject);
        logger.addParameter("operation", operation);
        logger.addParameter("object",    object);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    XUUID subj = getSubjectFromString(subject);

                    if (sysServices.getAclLogic().hasPermission(subj, operation, object))
                        return ok();
                    else
                        return forbidden();
                }});
    }

    /**
     * Checks if the currently logged in user (caller) can perform Operation on the Object
     * and returns OK if allowed and FORBIDDEN if not allowed.
     */
    @GET
    @Path("/{operation}/{object}")
    public Response getPermission(@Context HttpServletRequest request,
                                  @PathParam("operation") final String operation,
                                  @PathParam("object")    final String object)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/perms/{operation}/{object}");

        logger.addParameter("operation", operation);
        logger.addParameter("object",    object);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    XUUID subj = UserUtil.getCachedUser().getID();

                    if (sysServices.getAclLogic().hasPermission(subj, operation, object))
                        return ok();
                    else
                        return forbidden();
                }});
    }

    /**
     * Attempts to add the permission such that the Subject can perform Operation on the Object.
     * Returns OK if the addition of the permission was allowed and and FORBIDDEN if not allowed.
     */
    @PUT
    @Path("/{subject}/{operation}/{object}")
    public Response addPermissions(@Context HttpServletRequest request,
                                   @PathParam("subject")   final String subject,
                                   @PathParam("operation") final String operation,
                                   @PathParam("object")    final String object)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/perms/{subject}/{operation}/{object}");
        final Token               caller   = RestUtil.getInternalToken();

        logger.addParameter("subject",   subject);
        logger.addParameter("operation", operation);
        logger.addParameter("object",    object);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    XUUID subj = XUUID.fromString(subject);

                    if (sysServices.getAclLogic().addPermission(caller.getUserID(), subj, operation, object))
                        return ok();
                    else
                        return forbidden();
                }});
    }

    /**
     * Attempts to remove the permission such that the Subject can not perform Operation on the Object.
     * Returns OK if the removal of the permission was allowed and and FORBIDDEN if not allowed.
     */
    @DELETE
    @Path("/{subject}/{operation}/{object}")
    public Response removePermissions(@Context HttpServletRequest request,
                                   @PathParam("subject")   final String subject,
                                   @PathParam("operation") final String operation,
                                   @PathParam("object")    final String object)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/perms/{subject}/{operation}/{object}");
        final Token               caller   = RestUtil.getInternalToken();

        logger.addParameter("subject",   subject);
        logger.addParameter("operation", operation);
        logger.addParameter("object",    object);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    XUUID subj = XUUID.fromString(subject);

                    if (sysServices.getAclLogic().removePermission(caller.getUserID(), subj, operation, object))
                        return ok();
                    else
                        return forbidden();
                }});
    }

    /**
     * Accepts either an arbitrary XUUID or a users' email address.  If an email address is supplied,
     * then it's looked up and the actual User XUUID is returned.
     */
    private XUUID getSubjectFromString(String emailOrID)
    {
       try
       {
           return XUUID.fromString(emailOrID);
       }
       catch (IllegalArgumentException x)
       {
           try
           {
               CanonicalEmail ce   = new CanonicalEmail(emailOrID);
               User           user = sysServices.getUsers().findUserByEmail(emailOrID);

               if (user == null)
                   throw new IllegalArgumentException("");

               return user.getID();
           }
           catch (IllegalArgumentException y)
           {
               badRequestParam("userID", emailOrID, "User {} not found");

               return null; // never hit
           }
       }
    }

}
