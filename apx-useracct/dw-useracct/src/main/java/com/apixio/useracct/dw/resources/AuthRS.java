package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.AuthLogic;
import com.apixio.useracct.dw.ServiceConfiguration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthRS exposes the RESTful authentication service.  Entry points are:
 *
 *  POST /auths; params are email and password (for now)
 *
 */
@Path("/auths")
@Produces("application/json")
public class AuthRS extends BaseRS
{

    private PrivSysServices sysServices;
    private String          urlBase;  // for verification links
    private String          authCookieName;
    private Cookie          cookieKiller;

    public AuthRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.urlBase     = configuration.getVerifyLinkConfig().getUrlBase();
        this.sysServices = sysServices;

        // logout support
        authCookieName = configuration.getAuthConfig().getAuthCookieName();
        if (authCookieName == null)
            throw new IllegalStateException("Missing required configuration for authConfig.authCookieName");

        cookieKiller = CookieUtil.externalCookie(authCookieName, "", 0);
    }

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class AuthenticationParams {
        public String email;
        public String password;
        public String code;
        public String caller;
    }

    public static class CodeParam {
        public String code;
    }

    /**
     * Usage pattern:
     *
     *  1.  call with id (e.g., email address) and password and without token
     *  2.  call with code and token to complete second factor authorization
     *
     * authCheck microfilter needs to be configured with this path so that token-less
     * requests can be dispatched to it
     *
     * API Details:
     *  * no token required if email/password present
     *  * all roles allowed (if token is present)
     */
    @POST
    public Response authenticateUser(
        @Context               HttpServletRequest request,
        @Context               HttpServletResponse response,
        @FormParam("email")    String              emailAddress, // optional
        @FormParam("password") String              password,     // optional
        @FormParam("code")     String              code,         // optional, but if present token is required
        @FormParam("caller")   String              caller,
        @QueryParam("int")     Boolean             wantsInt,     // optional; if true then return internal token if internal IP addr
        @QueryParam("ttl")     Integer             ttl           // optional; if internal returned, TTL is as requested
        )
    {
        AuthenticationParams  params = new AuthenticationParams();

        params.email     = emailAddress;
        params.password  = password;
        params.code      = code;
        params.caller    = caller;

        return authenticateUserCommon(request, response, wantsInt, ttl, params);
    }

    @POST
    @Consumes("application/json")
    public Response authenticateUserJson(
        @Context               HttpServletRequest  request,
        @Context               HttpServletResponse response,
        @QueryParam("int")     Boolean             wantsInt,     // optional; if true then return internal token if internal IP addr
        @QueryParam("ttl")     Integer             ttl,          // optional; if internal returned, TTL is as requested
        AuthenticationParams   authParams
        )
    {
        return authenticateUserCommon(request, response, wantsInt, ttl, authParams);
    }

    /**
     * If the password is expired we give a 10 min window with token set to password_authenticated state
     * for the user so that they can do their password expired workflow
     *
     */
    private Cookie createExternalTokenCookie(Map<String, Object> json)
    {
        final long expirySecondsLong = 60 *
            ((boolean) json.getOrDefault("passwordExpired", false) ?
             10 :
             (long) json.getOrDefault("passwordExpiresIn", 0));

        return CookieUtil.externalCookie(authCookieName,
                                         (String) json.get("token"),
                                         (int) Math.min(Integer.MAX_VALUE, expirySecondsLong));
    }

    private Response authenticateUserCommon(
        final HttpServletRequest     request,
        final HttpServletResponse    response,
        final Boolean                wantsInt,     // optional; if true then return internal token if internal IP addr
        final Integer                ttl,          // optional; if internal returned, TTL is as requested
        final AuthenticationParams   authParams
        )
    {
        final ApiLogger     logger   = super.createApiLogger(request, "/auths");

        logger.addParameter("email", authParams.email);
        logger.addParameter("ipAddress", RestUtil.getClientIpAddress(request));
        logger.addParameter("userAgent", RestUtil.getClientUserAgent(request));

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Map<String, Object>      json = new HashMap<String, Object>();
                    AuthLogic.Authentication auth = sysServices.getAuthLogic().authenticateUser(
                        authParams,
                        RestUtil.getClientIpAddress(request),
                        RestUtil.getClientUserAgent(request),
                        ((wantsInt != null) && wantsInt.booleanValue()),
                        ((ttl != null) ? ttl.intValue() : 0));

                    json.put("token", auth.token.getID().toString());  // MUST have .toString() for json conversion to be correct
                    logger.addParameter("userId", auth.token.getUserID());

                    if (auth.needsTwoFactor)
                    {
                        json.put("needsTwoFactor", Boolean.TRUE);
                        if(auth.partialPhoneNumber != null)
                            json.put("partialPhoneNumber", auth.partialPhoneNumber);
                    }
                    if (auth.needsNewPassword)
                        json.put("needsNewPassword", Boolean.TRUE);

                    if (auth.passwordExpired)
                    {
                        json.put("passwordExpired", Boolean.TRUE);
                        json.put("nonce",           auth.expiredNonce);
                    }
                    else
                    {
                        json.put("passwordExpiresIn", auth.passwordExpiresIn);
                    }

                    response.addCookie(createExternalTokenCookie(json));

                    // if the phone number is missing/invalid we send back
                    // the { "error", ERR_PHONE_MISSING/INVALID_PHONE_NUMBER }
                    // so that the UI can catch it
                    if(auth.phoneMissing != null)
                    {
                        json.put("error", auth.phoneMissing);
                    }
                    return ok(json);
                }}.serverErrorCode(HttpServletResponse.SC_FORBIDDEN));
    }

    /**
     *  Verify two factor authentication
     */
    @POST
    @Path("/codeVerification")
    @Consumes("application/json")
    public Response verifyTwoFactor(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            CodeParam codeParam)
    {
        ApiLogger logger   = super.createApiLogger(request, "/auths/codeVerification");

        // the internal token cannot be null here if its coming through the system
        // we also know that the internal token is verified as password authenticated token
        // also this url is a partially authenticated url
        final Token iToken = RestUtil.getInternalToken();

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception
            {
                Map<String, Object> json = new HashMap<String,Object>();

                if(codeParam.code == null){
                    badRequest("Two factor code cannot be null");
                }
                // can the external token be null here ??
                AuthLogic.Verification verification = sysServices.getAuthLogic().verifyTwoFactor(logger, codeParam.code, iToken);

                logger.addParameter("userId", verification.token.getUserID());

                json.put("token", verification.token.getID().toString());

                if (verification.passwordExpired)
                {
                    json.put("passwordExpired", Boolean.TRUE);
                }
                else
                {
                    json.put("passwordExpiresIn", verification.passwordExpiresIn);
                }

                response.addCookie(createExternalTokenCookie(json));

                return ok(json);
            }
        });
    }

    /**
     *  Get the authentication status from the internal and external token
     * @return
     */
    @GET
    @Path("/status")
    public Response getAuthStatus(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response
    )
    {
        ApiLogger logger = super.createApiLogger(request, "/auths/status");
        final Token iToken = RestUtil.getInternalToken();

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception
            {
                Map<String, Object> json = new HashMap<>();
                AuthLogic.AuthenticationStatus authStatus = sysServices.getAuthLogic().getAuthenticationStatus(logger, iToken);
                if(authStatus.authenticated)
                    json.put("authenticated", Boolean.TRUE);
                else
                    json.put("authenticated", Boolean.FALSE);
                return ok(json);
            }
        }
        );

    }
    /**
     *
     */
    @DELETE
    @Path("/{id}")
    public Response logout(
        @Context          HttpServletRequest  request,
        @Context          HttpServletResponse response,
        @PathParam("id")  String              externalToken
        )
    {
        // if we've made it this far then we know the internal token in the HTTP header is valid.
        // the {id} resource value should be the external token for that internal token.

        ApiLogger logger = super.createApiLogger(request, "/auths/{id}");
        Response  resp   = null;
        Token     iToken = RestUtil.getInternalToken();

        logger.addParameter("id", externalToken);

        // The internal token is null when the xtoken provided is partially authenticated
        if(iToken == null) {
            Tokens tokens = sysServices.getTokens();
            Token  xToken = tokens.findTokenByID(XUUID.fromString(externalToken, Token.TT_EXTERNAL));

            if (xToken != null) {
                tokens.delete(xToken);
            }
            // Use the cookie killer when the internal and/or external token is null
            response.addCookie(cookieKiller);
            resp = Response.ok().build();
        }
        else if (externalToken.equals(iToken.getExternalToken().toString()))
        {
            //Normal logout case where the internal token and external token are present
            Tokens tokens = sysServices.getTokens();
            Token  xToken = tokens.findTokenByID(XUUID.fromString(externalToken, Token.TT_EXTERNAL));

            if (xToken != null)
            {
                tokens.delete(xToken);
                tokens.delete(iToken);
                response.addCookie(cookieKiller);
                resp = Response.ok().build();
            }
        }
        else
        {
            System.out.println("System error in logout:  internal token presented [" + iToken + "] " +
                               "does not refer to the external token being deleted [" + externalToken + "]");
        }

        if (resp == null)
            resp = Response.status(HttpServletResponse.SC_BAD_REQUEST).build();

        logger.apiEnd(resp);

        return resp;
    }

    /**
     * This variant pulls external token from cookie value (the name of which is
     * configured).  The normal system MUST call this with an internal token in the
     * Authorization HTTP header (as would be expected for any request from a
     * logged in user) or it will fail in the logout() method.
     */
    @DELETE  // i.e., DELETE:/auths
    public Response logoutViaCookie(
        @Context   HttpServletRequest  request,
        @Context   HttpServletResponse response
        )
    {
        XUUID tokenID = RestUtil.getTokenFromRequest(request, authCookieName);

        if (tokenID != null)
        {
            Tokens tokens = sysServices.getTokens();
            Token  token = tokens.findTokenByID(tokenID);

            if ((token != null) && token.isInternal())  // shouldn't ever be null...
                tokenID = token.getExternalToken();

            return logout(request, response, tokenID.toString());
        }
        else
        {
            return badRequest("Unable to get token from from either Authorization header or Cookie");
        }
    }

}
