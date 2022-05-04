package com.apixio.tokenizer.dw.resources;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;

/**
 * TokenRS is the RESTful entry point for the internal tokenization service that edge
 * servers are expected to call to exchange external tokens for internal ones.
 *
 * Edge servers MUST exchange an external token for an internal one at the beginning
 * of the sequence of (any) calls to internal RESTful services, and edge servers MUST
 * dispose of the internal token at the end of the sequence of such calls.  The easy
 * and recommended approach is to set up a web request filter that can intercept all
 * incoming requests and do this exchange automatically.
 *
 * If the deletion of the internal request is not done, internal use counts will increase
 * without bound and that will trigger intruder alerts which monitor these use counts.
 *
 * March 20, 2019:  a new way of dealing with auth tokens on exchange was introduced.
 * Rather than requiring that the "Authorization: Apixio ..." HTTP header exist the
 * code will accept a cookie that has the same external token in it (the cookie name
 * is configurable).  Additionally, the internal token is now returned both in the
 * HTTP response body and in the "Authorization: Apixio ..." HTTP header (which is
 * mostly an abuse of that header).
 *
 * March 26, 2019: token refcounting was removed because the new API gateway won't
 * be doing the corresponding DELETE calls to bump the counts down and no code
 * actually does anything with the intruder alerts anyway.

 */
@Path("/tokens")
public class TokenRS extends BaseRS
{
    private SysServices sysServices;

    private String authCookieName;
    private String gatewayHeaderName;
    private String gatewayHeaderValue;

    public TokenRS(MicroserviceConfig configuration, SysServices sysServices)
    {
        super(configuration, sysServices);

        ConfigSet config   = configuration.getMicroserviceConfig();
        String    tokenKey = MicroserviceConfig.ConfigArea.TOKEN.getYamlKey() + ".";
        String    gateway;

        authCookieName = config.getString(tokenKey + MicroserviceConfig.TOK_AUTHCOOKIENAME, null);
        gateway        = config.getString(tokenKey + MicroserviceConfig.TOK_GATEWAYHEADER,  null);  // header: value

        if (gateway != null)
        {
            String[] parts = gateway.split(":");

            if (parts.length == 2)
            {
                gatewayHeaderName  = parts[0].trim();
                gatewayHeaderValue = parts[1].trim();
            }
        }

        this.sysServices = sysServices;
    }

    /**
     * Create an internal token from the external token.  This is invoked
     * if and only if the external token is valid.
     */
    @POST
    @Produces("application/json")
    public Response createInternal(@Context final HttpServletRequest request)
    {
        ApiLogger logger = super.createApiLogger(request);

        return createToken(request, logger);
    }

    /**
     * Due to API gateway needs, tokenizer needs to accept arbitrary URLs as the gateway
     * will just prepend "/tokens/" to the actual request URL.  Similarly, the HTTP verb
     * will just be passed along so tokenizer needs to respond to GET, POST, PUT (and possibly
     * more).
     */
    @POST
    @Path("/{url:.+}")
    @Produces("application/json")
    public Response createInternalPost(@Context final HttpServletRequest request, @PathParam("url") String urlComponents)
    {
        ApiLogger logger = super.createApiLogger(request);

        return createToken(request, logger);
    }

    @GET
    @Path("/{url:.+}")
    @Produces("application/json")
    public Response createInternalGet(@Context final HttpServletRequest request, @PathParam("url") String urlComponents)
    {
        ApiLogger logger = super.createApiLogger(request);

        return createToken(request, logger);
    }

    @PUT
    @Path("/{url:.+}")
    @Produces("application/json")
    public Response createInternalPut(@Context final HttpServletRequest request, @PathParam("url") String urlComponents)
    {
        ApiLogger logger = super.createApiLogger(request);

        return createToken(request, logger);
    }

    @DELETE
    @Path("/{url:.+}")
    @Produces("application/json")
    public Response createInternalDelete(@Context final HttpServletRequest request, @PathParam("url") String urlComponents)
    {
        ApiLogger logger = super.createApiLogger(request);

        return createToken(request, logger);
    }

    /**
     * The super-smart token creation code that deals with behavior needed by API gateway logic.
     * If the HTTP header "Gateway" exists and has the value specified in gatewayTriggerName
     * then the special behavior is to return a 200 code rather than 201 (created) code
     * and to not send back anything in the body.
     */
    private Response createToken(final HttpServletRequest request, final ApiLogger logger)
    {
        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    XUUID       xToken  = RestUtil.getTokenFromRequest(request, authCookieName);
                    Token       rToken  = getRequestToken(xToken);
                    String      gateway = (gatewayHeaderName != null) ? request.getHeader(gatewayHeaderName) : null;
                    String      tokStr;

                    tokStr = rToken.getID().toString();

                    // return a 201 code and JSON body if request isn't from gateway
                    if ((gateway == null) || !gateway.equals(gatewayHeaderValue))
                    {
                        Map<String, String> json = new HashMap<String, String>(4);

                        json.put("token", tokStr);

                        return (Response.created(new URI(request.getRequestURI() + "/" + rToken.getID()))
                                .entity(json)
                                .build());
                    }
                    else  // return 200 code and no body if from gateway
                    {
                        return (Response.ok()
                                .header(RestUtil.HTTP_AUTHHEADER, RestUtil.AUTH_METHOD + tokStr)
                                .build());
                    }
                }
            }.serverErrorCode(HttpServletResponse.SC_FORBIDDEN).badRequestCode(HttpServletResponse.SC_FORBIDDEN) );
    }

    /**
     * Delete an internal token that was retrieved via createInternal()
     */
    @DELETE
    @Path("{id}")
    public Response deleteInternal(
        @Context         final HttpServletRequest request,
        @PathParam("id") final String             rTokenID
        )
    {
        ApiLogger  logger   = super.createApiLogger(request, "/tokens/{id}");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Token  rToken = sysServices.getTokens().findTokenByID(XUUID.fromString(rTokenID, Token.TT_INTERNAL));
                    XUUID  xToken = RestUtil.getTokenFromRequest(request);

                    if (rToken != null)
                    {
                        if (!xToken.equals(rToken.getExternalToken()))
                            throw new IllegalArgumentException("Attempt to delete an internal token that isn't for the Authorization token: " +
                                                       xToken + " :: " + rToken.getExternalToken());
                        sysServices.getTokens().delete(rToken);
                    }

                    return ok();
                }}.serverErrorCode(HttpServletResponse.SC_FORBIDDEN).badRequestCode(HttpServletResponse.SC_FORBIDDEN));
    }

    /**
     * Create the short-lived request (internal) token from the ID of the external token.
     */
    private Token getRequestToken(XUUID xToken)
    {
        return sysServices.getTokens().createInternalToken(xToken);
    }

}
