package com.apixio.mcs.admin.dw.resources;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.apixio.XUUID;
import com.apixio.bms.Blob;
import com.apixio.fxs.FunctionDefinition;
import com.apixio.fxs.FxCatalogService;
import com.apixio.sdk.util.FxIdlParser;
import com.apixio.mcs.admin.McsSysServices;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.web.BaseRS;
import com.apixio.sdk.protos.FxProtos.FxDef;

/**
 * Handles REST APIs for /fx/fxdefs
 */
@Path("/fx/fxdefs")
public class FxDefRS extends BaseRS
{
    private static Logger LOGGER;

    /**
     * fields
     */
    protected McsSysServices   mcsServices;
    private   FxCatalogService fxCatalogService;

    /**
     *
     */
    public FxDefRS(MicroserviceConfig configuration, McsSysServices sysServices)
    {
        super(configuration, sysServices);

        this.mcsServices      = sysServices;
        this.fxCatalogService = mcsServices.getFxCatalogService();

        LOGGER = Logger.getLogger(FxDefRS.class);
    }

    /**
     * Create/check for an f(x) definition.  If a parsed-then-protobuf'ed f(x) IDL def is a duplicate of
     * one already in the system, the the FX_ for it is returned instead of a new blob being created.
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createFxDef(@Context HttpServletRequest request, final FunctionDefinition fd)
    {
        ApiLogger logger = super.createApiLogger(request, "POST:/fx/fxdefs", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    FxDef              def  = FxIdlParser.parse(fd.fxdef);
                    Blob               blob = fxCatalogService.findOrCreateFxDef(def, fd.createdBy, fd.name, fd.description);
                    Map<String,String> body = new HashMap<>();

                    body.put("fxid", blob.getUuid().toString());

                    return created("/fx/fxdefs/" + blob.getUuid(), body);
                }
            });
    }

    /**
     * Returns the actual FxDef as JSON
     */
    @GET
    @Path("/{id}")
    @Produces("application/json")
    public Response getBlobMeta(@Context HttpServletRequest request,
                                @PathParam("id") final String id
        )
    {
        ApiLogger logger = super.createApiLogger(request, "GET:/fx/fxdefs/{id}", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = fxCatalogService.getFxDef(XUUID.fromString(id));

                    if (blob != null)
                    {
                        return ok(blob.getExtra1().toString());
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

}
