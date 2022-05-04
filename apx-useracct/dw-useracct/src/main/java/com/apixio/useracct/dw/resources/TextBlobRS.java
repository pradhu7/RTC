package com.apixio.useracct.dw.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.apixio.restbase.web.BaseRS.ApiLogger;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.TextBlobDTO;
import com.apixio.useracct.buslog.TextBlobLogic;
import com.apixio.useracct.buslog.TextBlobProperties;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.TextBlob;

/**
 *
 */
@Path("/texts")
@Produces("application/json")
public class TextBlobRS extends BaseRS {

    /**
     * PUT:/texts/{blobID}    <= {"name":"blah", "description":"blah", "contents":"blah"}
     * GET:/texts/{blobID}    => {"id":"xuuid", "blobID":"blah", "name":"blah", "description":"blah", "contents":"blah"}
     * DELETE:/texts/{blobID}
     */

    private PrivSysServices sysServices;
    private TextBlobLogic textBlobLogic;

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class PutTextParams {
        public String name;
        public String description;
        public String contents;
    }

    public TextBlobRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices   = sysServices;
        this.textBlobLogic = sysServices.getTextBlobLogic();
    }

    /**
     *
     */
    @PUT
    @Path("/{blobID}")
    @Consumes("application/json")
    public Response putBlob(@Context HttpServletRequest request, @PathParam("blobID") final String blobID, final PutTextParams params)
    {
        ApiLogger           logger   = super.createApiLogger(request, "PUT:/texts/{blob}");

        logger.addParameter("blobID", blobID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    TextBlob tb = textBlobLogic.putTextBlob(blobID, validateBlobParams(params, true));

                    return ok(TextBlobProperties.toJson(tb, false));
                }});
    }

    private TextBlobDTO validateBlobParams(PutTextParams reqParams, boolean forCreate)
    {
        TextBlobDTO params = new TextBlobDTO();

        params.name         = reqParams.name;
        params.description  = reqParams.description;
        params.contents     = reqParams.contents;

        return params;
    }

    /**
     *
     */
    @GET
    @Produces("application/json")
    public Response getAllBlobs(@Context HttpServletRequest request, @QueryParam("content") final Boolean includeContent)
    {
        ApiLogger           logger   = super.createApiLogger(request, "GET:/texts");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    List<Map<String, Object>> blobs   = new ArrayList<>();
                    boolean                   content = (includeContent != null) && includeContent.booleanValue();

                    for (TextBlob tb : textBlobLogic.getAllTextBlobs())
                        blobs.add(TextBlobProperties.toJson(tb, content));

                    return ok(blobs);
                }});
    }

    /**
     *
     */
    @GET
    @Path("/{blobID}")
    @Produces("application/json")
        public Response getBlob(@Context HttpServletRequest request, @PathParam("blobID") final String blobID)
    {
        ApiLogger           logger   = super.createApiLogger(request, "GET:/texts/{blob}");

        logger.addParameter("blobID", blobID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    TextBlob tb = textBlobLogic.getTextBlob(blobID);

                    if (tb != null)
                        return ok(TextBlobProperties.toJson(tb, true));
                    else
                        return notFound("TextBlob", blobID);
                }});
    }

}
