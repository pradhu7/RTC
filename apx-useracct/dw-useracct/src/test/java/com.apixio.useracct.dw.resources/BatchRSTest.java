package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dw.ServiceConfiguration;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({BatchRS.class, BaseRS.class, XUUID.class})
public class BatchRSTest extends JerseyTest
{
    public static final String BATCHES = "batches";
    private PrivSysServices sysServices;

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    // Configuring a servlet for request and response
    // BatchRS service is just partially mocked by PowerMockito
    @Override
    protected DeploymentContext configureDeployment()
    {
        sysServices = PowerMockito.mock(PrivSysServices.class);
        ServiceConfiguration configuration = PowerMockito.mock(ServiceConfiguration.class);
        BatchRS batchRS = PowerMockito.spy(new BatchRS(configuration, sysServices));

        return ServletDeploymentContext.forServlet(
                new ServletContainer(new ResourceConfig().register(batchRS))).build();
    }

    private void supressLogger()
    {
        PowerMockito.suppress(PowerMockito.method(BaseRS.ApiLogger.class, "addParameter", String.class, Object.class));
    }

    // GET	/batches/upload/pds/{pdsId}	Invalid UUID string: *
    @Test
    public void getPdsUploadBatchesWithInvalidPdsId()
    {
        String invalidPdsId = "invalid";

        supressLogger();

        Response response = target()
                .path("batches").path("upload").path("pds").path("{pdsId}")
                .resolveTemplate("pdsId", invalidPdsId)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    // PUT	/batches/upload/{batchId}	Invalid UUID string: *
    @Test
    public void putUploadBatchUpdateWithInvalidBatchId()
    {
        String invalidBatchId = "invalid";
        Map<String,String> params = new HashMap<>();

        supressLogger();

        Response response = target()
                .path(BATCHES).path("upload").path("{batchId}")
                .resolveTemplate("batchId", invalidBatchId)
                .request()
                .put(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 400);
    }
}
