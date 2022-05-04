package com.apixio.tokenizer.dw.resources;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.SysServices;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.web.BaseRS;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * @author lschneider
 * created on 6/17/15.
 *
 * place to put version endpoint
 */
@Path("/util")
public class UtilRS extends BaseRS
{
    private SysServices sysServices;

    public UtilRS(MicroserviceConfig configuration, SysServices sysServices)
    {
        super(configuration, sysServices);
        this.sysServices = sysServices;
    }

    @GET
    @Path("/version")
    public Response getVersion(@Context HttpServletRequest request)
    {
        final ApiLogger logger = super.createApiLogger(request, "/util/version");

        return super.restWrap(new RestWrap(logger)
        {
            public Response operation() throws IOException
            {
                String path = "META-INF/maven/apixio/apixio-tokenizer-dw/pom.properties";
                URL file = this.getClass().getClassLoader().getResource(path);
                String lines = Resources.toString(file, Charsets.UTF_8);
                return ok(lines);

            }});
    }
}
