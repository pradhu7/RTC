package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.restbase.web.BaseRS.*;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.config.AuthConfig;
import com.apixio.useracct.config.VerifyLinkConfig;
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
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthRS.class, Tokens.class})
public class AuthRSTest extends JerseyTest
{

    public static final String AUTHS = "/auths";
    public static final String APX_TOKEN = "ApxToken";
    public static final String EXT_TOK = "TA_b34e1d6f-d2a6-4889-b8fe-dccb42294750";
    private PrivSysServices sysServices;
    private AuthRS authRS;

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    // Here are configuring a servlet for request and response
    // AuthRS service is just partially mocked by PowerMockito
    @Override
    protected DeploymentContext configureDeployment() {
        sysServices = PowerMockito.mock(PrivSysServices.class);
        ServiceConfiguration configuration = PowerMockito.mock(ServiceConfiguration.class);

        PowerMockito.when(configuration.getVerifyLinkConfig()).thenReturn(PowerMockito.mock(VerifyLinkConfig.class));
        PowerMockito.when(configuration.getVerifyLinkConfig().getUrlBase()).thenReturn(AUTHS);

        PowerMockito.when(configuration.getAuthConfig()).thenReturn(PowerMockito.mock(AuthConfig.class));
        PowerMockito.when(configuration.getAuthConfig().getAuthCookieName()).thenReturn(APX_TOKEN);

        authRS = PowerMockito.spy(new AuthRS(configuration, sysServices));
        return ServletDeploymentContext.forServlet(
                new ServletContainer(new ResourceConfig().register(authRS))).build();
    }

    @Test
    public void testLogOutViaCookie()
    {
        PowerMockito.suppress(PowerMockito.method(ApiLogger.class, "addParameter",
                String.class, Object.class));

        Tokens tokens = PowerMockito.mock(Tokens.class);
        PowerMockito.when(sysServices.getTokens()).thenReturn(tokens);
        Token token = PowerMockito.mock(Token.class);
        PowerMockito.when(tokens.findTokenByID(any(XUUID.class))).thenReturn(token);

        Cookie cookie = new Cookie(APX_TOKEN, EXT_TOK);

        Response response = target(AUTHS).path(EXT_TOK).request().cookie(cookie).delete();

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(response.getCookies().get(APX_TOKEN).getValue(),"");
    }
}
