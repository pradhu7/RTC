package com.apixio.useracct.dw.resources;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PasswordPolicyLogic;
import com.apixio.useracct.buslog.PrivUserLogic;
import com.apixio.useracct.buslog.UserLogic;
import com.apixio.useracct.config.AuthConfig;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.PasswordPolicy;
import com.apixio.useracct.entity.User;
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
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({UserRS.class, ServiceConfiguration.class,
                PrivSysServices.class, RestUtil.class,
                Users.class, BaseRS.class,
                DaoBase.class, PasswordPolicyLogic.class,
                PrivUsers.class, SysServices.class,
                Tokens.class, LogicBase.class})
public class UserRSTest extends JerseyTest
{
    private static final String USERS = "users";
    public static final String APX_TOKEN = "ApxToken";

    private PrivSysServices sysServices;
    private ServiceConfiguration configuration;

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
        sysServices = PowerMockito.mock(PrivSysServices.class);
        configuration = PowerMockito.mock(ServiceConfiguration.class);

        PowerMockito.when(configuration.getAuthConfig()).thenReturn(PowerMockito.mock(AuthConfig.class));
        PowerMockito.when(configuration.getAuthConfig().getAuthCookieName()).thenReturn(APX_TOKEN);

        UserRS userRS = PowerMockito.spy(new UserRS(configuration, sysServices));

        return ServletDeploymentContext.forServlet(
                new ServletContainer(new ResourceConfig().register(userRS))).build();
    }

    private void supressLogger()
    {
        PowerMockito.suppress(PowerMockito.method(BaseRS.ApiLogger.class, "addParameter", String.class, Object.class));
    }

    /**
     *  GET	/users/{userID}	Invalid UUID string: *
     */
    @Test
    public void getUserIDWithInvalidID(){
        String invalidUserID = "invalid";

        supressLogger();

        Response response = target()
                .path(USERS).path("{userID}")
                .resolveTemplate("userID", invalidUserID)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    /**
     *  DELETE	/users/{userID}	Invalid UUID string: *
     */
    @Test
    public void deleteUserWithInvalidUUIDType()
    {
        String invalidUserIDType = "O_6d6a994f-7fe3-45cd-8d0e-76d92ba81066";

        supressLogger();

        Response response = target()
                .path(USERS).path("{userID}")
                .resolveTemplate("userID", invalidUserIDType)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    /**
     *  PUT	/users/{userID}/activation	Invalid UUID string: *
     */
    @Test
    public void putUserActiveWithInvaildID()
    {
        String inValidId = "invalid";
        Map<String, Boolean> params = new HashMap<>();
        params.put("state", true);

        supressLogger();

        Response response = target()
                .path(USERS).path("{userID}").path("activation")
                .resolveTemplate("userID", inValidId)
                .request()
                .put(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 400);
    }

    private void mockRestUtil()
    {
        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(PowerMockito.mock(Token.class));
    }

    private PrivUserLogic mockPrivUserLogic()
    {
        PowerMockito.suppress(PowerMockito.constructor(PrivUserLogic.class));
        PrivUserLogic privUserLogic = PowerMockito.spy(new PrivUserLogic(sysServices,configuration));

        return privUserLogic;
    }

    /**
     *  Get /users/me/{detail}
     *  This is for testing the case when the user enters the password for the first time
     *  and make sure sure his nonce is updated and his account state is set to active
     */
    @Test
    public void putUserPasswordFor() throws Exception
    {
        String detail = "me";

        // Valid Password following the password policy
        String validPassword = "Passw0rd!";
        String nonce = "3005760536376399626";
        Map<String, String> params = new HashMap<>();
        params.put("password", validPassword);
        params.put("nonce", nonce);

        supressLogger();
        mockRestUtil();

        PrivUserLogic privUserLogic = mockPrivUserLogic();
        PowerMockito.when(sysServices.getPrivUserLogic()).thenReturn(privUserLogic);

        UserLogic userLogic = PowerMockito.mock(UserLogic.class);
        PowerMockito.when(sysServices.getUserLogic()).thenReturn(userLogic);

        User user = createUser(nonce);
        PowerMockito.doReturn(user).when(userLogic).getUser(any(Token.class), any(String.class), any(Boolean.class));

        PrivUsers users = getPrivUsers(privUserLogic);
        PasswordPolicyLogic passwordPolicyLogic = getPasswordPolicyLogic(privUserLogic, user);

        PasswordPolicyLogic.CriteriaCheck criteriaCheck = new PasswordPolicyLogic.CriteriaCheck();
        criteriaCheck.ok = true;

        // Need to inject this in the base class since we are suppressing the constructors for testing
        MemberMatcher.field(LogicBase.class, "sysServices").set(privUserLogic, sysServices);
        PowerMockito.when(passwordPolicyLogic.checkPassword(any(PasswordPolicy.class), any(String.class),
                any(CanonicalEmail.class), any(List.class))).thenReturn(criteriaCheck);

        PowerMockito.doNothing().when(users).update(user);

        mockUpdatingTokens();

        PowerMockito.when(sysServices.getUsers()).thenReturn(users);

        // Check the user account state is new
        Assert.assertEquals(user.getState(), AccountState.NEW);

        Response response = target()
                .path(USERS).path("{detail}")
                .resolveTemplate("detail", detail)
                .request()
                .put(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 200);

        // Verfiy if the nonce is updated
        Assert.assertEquals(user.getUpdateNonce(),"changed");

        // Verifying that the account state is active since he is a new user
        Assert.assertEquals(user.getState(), AccountState.ACTIVE);
    }

    private PasswordPolicyLogic getPasswordPolicyLogic(PrivUserLogic privUserLogic, User user) throws IOException
    {
        PasswordPolicy passwordPolicy = PowerMockito.mock(PasswordPolicy.class);
        PowerMockito.doReturn(passwordPolicy).when(privUserLogic).getUserPasswordPolicy(user);

        PasswordPolicyLogic passwordPolicyLogic = PowerMockito.mock(PasswordPolicyLogic.class);
        PowerMockito.when(sysServices.getPasswordPolicyLogic()).thenReturn(passwordPolicyLogic);

        return passwordPolicyLogic;
    }

    private PrivUsers getPrivUsers(PrivUserLogic privUserLogic) throws IllegalAccessException
    {
        PowerMockito.suppress(PowerMockito.constructor(PrivUsers.class));
        PrivUsers users = PowerMockito.spy(new PrivUsers(PowerMockito.mock(DaoBase.class)));

        MemberMatcher.field(PrivUserLogic.class, "privSysServices").set(privUserLogic, sysServices);
        PowerMockito.when(sysServices.getPrivUsers()).thenReturn(users);

        return users;
    }

    private void mockUpdatingTokens() {
        Tokens tokens = PowerMockito.mock(Tokens.class);
        PowerMockito.when(sysServices.getTokens()).thenReturn(tokens);
        PowerMockito.when(tokens.findTokenByID(any(XUUID.class))).thenReturn(null);
    }

    public User createUser(String nonce)
    {
        Map<String,String> params = new HashMap<>();
        params.put("email-addr", "test@apixio.com");
        params.put("account-state", "NEW");
        params.put("update-nonce", nonce);
        params.put("id", "U_3eb1960c-5d78-44da-af32-a5064da1c303");
        params.put("created-at", "1494691023");

        ParamSet paramSet = new ParamSet(params);
        User user = new User(paramSet);

        return user;
    }
}
