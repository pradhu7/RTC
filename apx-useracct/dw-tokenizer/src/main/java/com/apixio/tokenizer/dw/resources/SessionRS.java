package com.apixio.tokenizer.dw.resources;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;

/**
 *
 */
@Path("/session")
public class SessionRS extends BaseRS
{
    private SysServices sysServices;
    private Tokens      tokens;

    public SessionRS(MicroserviceConfig configuration, SysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
        this.tokens      = sysServices.getTokens();
    }

    /**
     */
    @PUT
    @Path("/{tag}")
    public Response setSessionData(@Context final HttpServletRequest request,
                                   @PathParam("tag") final String tag,
                                                     final String body
        )
    {
        ApiLogger logger   = super.createApiLogger(request);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    Token token = getRequestToken(request);

                    if (token != null)
                    {
                        token.setTagValue(tag, body);
                        tokens.update(token);

                        return Response.ok().build();
                    }
                    else
                    {
                        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
                    }
                }
            });
    }

    /**
     */
    @GET
    @Path("/{tag}")
    @Produces("application/json")
    public Response getSessionData(@Context final HttpServletRequest request,
                                    @PathParam("tag") final String tag)
    {
        ApiLogger logger   = super.createApiLogger(request);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    Token token = getRequestToken(request);

                    if (token != null)
                    {
                        return Response.ok(token.getTagValue(tag)).build();
                    }
                    else
                    {
                        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
                    }
                }
            });
    }

    /**
     */
    @DELETE
    @Path("/{tag}")
    @Produces("application/json")
    public Response deleteSessionData(@Context final HttpServletRequest request,
                                      @PathParam("tag") final String tag)
    {
        ApiLogger logger   = super.createApiLogger(request);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    Token token = getRequestToken(request);

                    if (token != null)
                    {
                        token.removeTagValue(tag);
                        tokens.update(token);

                        return Response.ok().build();
                    }
                    else
                    {
                        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
                    }
                }
            });
    }

    /**
     * Create the short-lived request (internal) token from the ID of the external token.
     */
    private Token getRequestToken(HttpServletRequest req)
    {
        return sysServices.getTokens().findTokenByID(RestUtil.getTokenFromRequest(req));
    }

}
