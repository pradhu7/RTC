package com.apixio.mcs.admin.dw.resources;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.apixio.mcs.ModelCatalogService;
import com.apixio.mcs.PartMeta;
import com.apixio.mcs.admin.McsSysServices;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.web.BaseRS;

/**
 * Handles REST APIs for /mcs/models URL prefix
 */
@Path("/mcs/parts")
public class PartRS extends BaseRS
{
    /**
     * fields
     */
    protected McsSysServices      mcsServices;
    private   ModelCatalogService catalogService;

    /**
     *
     */
    public PartRS(MicroserviceConfig configuration, McsSysServices sysServices)
    {
        super(configuration, sysServices);

        this.mcsServices    = sysServices;
        this.catalogService = mcsServices.getModelCatalogService();
    }

    /**
     */
    @GET
    @Produces("application/json")
    public Response searchParts(@Context HttpServletRequest request,
                                @Context UriInfo            uriInfo
        )
    {
        ApiLogger   logger = super.createApiLogger(request, "GET:/mcs/parts", "mcsserver");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    Map<String, List<String>> params = uriInfo.getQueryParameters();
                    String                    md5    = getSingleParam(params, "md5");

                    if (md5 != null)
                    {
                        List<PartMeta>  parts  = catalogService.searchParts(md5);

                        return ok(parts);
                    }
                    else
                    {
                        return badRequest("Expected query parameter 'md5'");
                    }
                }});
    }

    private String getSingleParam(Map<String, List<String>> params, String name)
    {
        List<String> vals = params.get(name);

        if ((vals != null) && (vals.size() > 0))
            return vals.get(0);
        else
            return null;
    }

}
