package com.apixio.useracct.dw.resources;

import com.apixio.restbase.RestUtil;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.config.AuthConfig;
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
@PrepareForTest({VerifyRS.class, Tokens.class,
                 Token.class, RestUtil.class})
public class VerifyRSTest extends JerseyTest
{
    public static final String APX_TOKEN = "ApxToken";

    @Override
    protected TestContainerFactory getTestContainerFactory()
    {
        return new GrizzlyWebTestContainerFactory();
    }

    /**
     * Configuring a servlet for request and response
     * BatchRS service is just partially mocked by PowerMockito
     */
    @Override
    protected DeploymentContext configureDeployment()
    {
        PrivSysServices sysServices = PowerMockito.mock(PrivSysServices.class);
        ServiceConfiguration configuration = PowerMockito.mock(ServiceConfiguration.class);

        AuthConfig authConfig = PowerMockito.mock(AuthConfig.class);
        PowerMockito.when(configuration.getAuthConfig()).thenReturn(authConfig);
        PowerMockito.when(authConfig.getAuthCookieName()).thenReturn(APX_TOKEN);

        VerifyRS verifyRS = PowerMockito.spy(new VerifyRS(configuration, sysServices));

        return ServletDeploymentContext.forServlet(
                new ServletContainer(new ResourceConfig().register(verifyRS))).build();
    }

    private void supressLogger()
    {
        PowerMockito.suppress(PowerMockito.method(BaseRS.ApiLogger.class, "addParameter", String.class, Object.class));
    }

    private void mockRestUtil()
    {
        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(PowerMockito.mock(Token.class));
    }

    /**
     * # POST	/verifications/{id}/forgot	For input string: *
     *
     */
    @Test
    public void postVerifyForgotWithInvalidIDType()
    {
        String invalidLinkID = "U_6d6a994f-7fe3-45cd-8d0e-76d92ba81066";
        Map<String, String> params = new HashMap<>();

        supressLogger();
        mockRestUtil();

        Response response = target()
                .path("verifications").path("{id}").path("forgot")
                .resolveTemplate("id", invalidLinkID)
                .request()
                .post(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 400);
    }

    @Test
    public void postVerfyLinkValidWithInvalidLinkID()
    {
        String invalidLinkID = "invalid";
        Map<String, String> params = new HashMap<>();

        supressLogger();
        mockRestUtil();

        Response response = target()
                .path("verifications").path("{id}").path("valid")
                .resolveTemplate("id", invalidLinkID)
                .request()
                .post(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 400);
    }

}
