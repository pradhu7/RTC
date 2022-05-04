package com.apixio.useracct.dw.resources;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.VerifyLogic;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.dw.resources.AuthRS;
import com.apixio.useracct.dw.util.XUUIDUtil;
import com.apixio.useracct.entity.VerifyLink;

/**
 * Internal service.
 */

@Path("/verifications")
public class VerifyRS extends BaseRS {

    private PrivSysServices sysServices;
    private String          authCookieName;

    public VerifyRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices  = sysServices;
        this.authCookieName = configuration.getAuthConfig().getAuthCookieName();
    }

    // activate account (which is a side-effect of verifying an email address)
    //
    // API Details:
    //  * no token required
    @POST
    @Path("/{id}/valid")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateDone(@Context final HttpServletRequest req,
                                 @Context final HttpServletResponse resp,
                                 @PathParam("id") final String linkID)
    {
        ApiLogger    logger   = super.createApiLogger(req, "/verifications/{id}/valid");

        logger.addParameter("id", linkID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    VerifyLogic.Verification verification = sysServices.getVerifyLogic().verify(
                        RestUtil.getInternalToken(),
                        RestUtil.getClientIpAddress(req),
                        RestUtil.getClientUserAgent(req),
                        XUUIDUtil.getXuuid(linkID, VerifyLink.OBJTYPE));

                    return encodeVerification(verification, resp);

                }});
    }

    /**
     * Return information from a "forgot password" Verify link that the client can use
     * to do things like restrict new password contents.
     */
    @GET
    @Path("/{id}/forgot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResetPasswordLinkInfo(@Context final HttpServletRequest req, @PathParam("id") final String linkID)
    {
        ApiLogger   logger = super.createApiLogger(req, "/verifications/{id}/forgot");

        logger.addParameter("id", linkID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    VerifyLogic.VerifyInfo info = sysServices.getVerifyLogic().getVerifyLinkInfo(
                        RestUtil.getClientIpAddress(req),
                        RestUtil.getClientUserAgent(req),
                        XUUID.fromString(linkID, VerifyLink.OBJTYPE));

                    if (info != null)
                        return ok(info);
                    else
                        return notFound();
                }});
    }

    /**
     * Validate that the "reset password" link is valid and issue a token if it is
     */
    @POST
    @Path("/{id}/forgot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateResetPasswordLink(@Context final HttpServletRequest req,
                                              @Context final HttpServletResponse resp,
                                              @PathParam("id") final String linkID)
    {
        ApiLogger                logger   = super.createApiLogger(req, "/verifications/{id}/forgot");

        logger.addParameter("id", linkID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    VerifyLogic.Verification verification = sysServices.getVerifyLogic().verifyResetLink(
                        RestUtil.getInternalToken(),
                        RestUtil.getClientIpAddress(req),
                        RestUtil.getClientUserAgent(req),
                        XUUIDUtil.getXuuid(linkID, VerifyLink.OBJTYPE));

                    return encodeVerification(verification, resp);
                }});
    }

    private Response encodeVerification(final VerifyLogic.Verification verification,
                                        final HttpServletResponse resp)
    {
        if (verification != null)
        {
            Map<String, String>   json = new HashMap<String, String>();

            json.put("status", verification.status.toString());

            if (verification.partialAuthToken != null)
            {
                final String token = verification.partialAuthToken.getID().toString();

                json.put("token", token);

                // TODO: Avoid hard coding the external token TTL.
                // Allow the token to be valid for 30 minutes.
                resp.addCookie(CookieUtil.externalCookie(authCookieName, token, 30*60));
            }

            if (verification.nonce > 0L)
                json.put("nonce", Long.toString(verification.nonce));

            if (verification.linkUsed)
                json.put("linkUsed", "true");

            return ok(json);
        }
        else
        {
            return badRequest("Verification ID not found");
        }
    }

}
