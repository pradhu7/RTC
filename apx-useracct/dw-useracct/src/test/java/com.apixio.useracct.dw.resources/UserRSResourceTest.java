package com.apixio.useracct.dw.resources;

import com.apixio.restbase.RestUtil;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.AuthState;
import com.apixio.restbase.entity.Token;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.User;
import com.apixio.useracct.messager.AWSSNSMessenger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;


/**
 * Here I am using jersey test for testing the user resources
 * Also power mock needs to ignore javax.management.* class
 * since power mock does not work with the security classes
 */
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({PrivSysServices.class, RestUtil.class, AWSSNSMessenger.class})
public class UserRSResourceTest extends JerseyTest {

    private static final String DUMMY_EMAIL_ADDRESS = "dev-testing-dummy";
    public static final String TEST_CODER = "codertestdontdelete@apixio.com";
    private static Tokens tokens;
    private static PrivUsers privUsers;

    private static int counter = 1;

    static
    {
        PowerMockito.suppress(PowerMockito.method(PrivSysServices.class, "setupMessenger"));
        BaseResourceTest.mainSetup();
        tokens = BaseResourceTest.sysServices.getTokens();
        privUsers = BaseResourceTest.sysServices.getPrivUsers();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    @BeforeClass
    public static void initialize() throws IllegalAccessException
    {
        // Mocking messenger and the send call to return true
        AWSSNSMessenger awsMessenger = PowerMockito.mock(AWSSNSMessenger.class);
        PowerMockito.when(awsMessenger.sendMessage(any(String.class), any(String.class), any(String.class))).thenReturn(true);
        MemberMatcher.field(PrivSysServices.class, "messenger").set(BaseResourceTest.sysServices, awsMessenger);
    }

    @Override
    protected DeploymentContext configureDeployment()
    {
        ResourceConfig resourceConfig = new ResourceConfig()
                .register(new UserRS(BaseResourceTest.serviceConfiguration, BaseResourceTest.sysServices));
        Map<String, Object> props = new HashMap<>();
        props.put(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, "true");
        resourceConfig.addProperties(props);
        return ServletDeploymentContext.forServlet(
                new ServletContainer(resourceConfig)).build();
    }

    @Test
    public void resendOTP()
    {
        // create internal token
        String userStr = "codertestdontdelete@apixio.com";
        User user = privUsers.findUserByEmail(userStr);
        Token iToken = createToken(user, AuthState.PASSWORD_AUTHENTICATED);

        // Making sure that the user you are creating actually exist
        assertNotNull("User does not exist " + userStr, user);

        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(iToken);

        Response resp = target()
                .path("users").path("code")
                .request()
                .post(Entity.json(null));

        assertEquals(resp.getStatus(), 200);
    }

    private Token createToken(User user, AuthState state)
    {
        Token xToken = tokens.createExternalToken();
        xToken.setAuthState(state);
        xToken.setUserID(user.getID());
        tokens.update(xToken);
        return tokens.createInternalToken(xToken.getID());
    }

    @Test
    public void resendOTPWithInvalidIToken()
    {
        String userStr = "codertestdontdelete@apixio.com";
        User user = privUsers.findUserByEmail(userStr);
        Token iToken = createToken(user, AuthState.PARTIALLY_AUTHENTICATED);

        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(iToken);

        Response response = target()
                .path("users").path("code")
                .request()
                .post(Entity.json(null));

        assertEquals(response.getStatus(), 400);
    }

    private static String getUniqueUserStr(int i) {
        return DUMMY_EMAIL_ADDRESS + i + "@apixio.com";
    }

    @Test
    public void getUser()
    {
        String userStr = TEST_CODER;

        // need a authenticated token for this
        User user = privUsers.findUserByEmail(userStr);
        Token iToken = createToken(user, AuthState.AUTHENTICATED);

        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(iToken);

        Response response = target()
                .path("users").path(userStr)
                .request()
                .get();

        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void createUserWithInvalidPhoneNumberForTwoFactorOrg() throws IOException {
        String orgName = "OrgForTest";
        String userStr = getUniqueUserStr(counter++);
        String orgID = verifyOrgAndUser(orgName, userStr);

        // Params
        Map<String, String> params = new HashMap<>();
        params.put("email", userStr);
        params.put("organizationID", orgID);
        params.put("cellPhone", "236");
        params.put("skipEmail", "true");

        Response response = target()
                .path("users")
                .request()
                .post(Entity.json(params));

        assertEquals(response.getStatus(), 400);
        assertEquals(response.readEntity(HashMap.class).get("reason"),"INVALID_PHONE_NUMBER");
    }

    /**
     * Create a user without a phone number for a two factor org does not fail
     * if the user does not have a phone number at creation he can add it
     * during the first 2fa login
     */
    @Test
    public void createUserWithoutPhoneNumberForTwoFactorOrg() throws IOException
    {
        String orgName = "OrgForTest";
        SecureRandom secureRandomGen = new SecureRandom();

        String userStr = getUniqueUserStr(secureRandomGen.nextInt(1000));
        String orgID = verifyOrgAndUser(orgName, userStr);

        // Params
        Map<String, String> params = new HashMap<>();
        params.put("email", userStr);
        params.put("organizationID", orgID);
        params.put("skipEmail", "true");

        Response response = target()
                .path("users")
                .request()
                .post(Entity.json(params));

        assertEquals(response.getStatus(), 200);
        assertNotNull(response.readEntity(Map.class).get("id"));

        deleteUser(userStr);
    }

    /**
     * create user with needs two factor flag
     */
    @Test
    public void createUserWithNeedsTwoFactorFlag() throws IOException {
        String orgName = "OrgForTest";
        SecureRandom secureRandom = new SecureRandom();

        String userStr = getUniqueUserStr(secureRandom.nextInt(1000));
        String orgID = verifyOrgAndUser(orgName, userStr);

        // Parameters
        Map<String, String> params = new HashMap<>();
        params.put("email", userStr);
        params.put("organizationID", orgID);
        params.put("skipEmail", "true");
        params.put("needsTwoFactor", "true");

        Response response = target()
                .path("users")
                .request()
                .post(Entity.json(params));

        assertEquals(response.getStatus(), 200);
        assertNotNull(response.readEntity(Map.class).get("id"));

        deleteUser(userStr);

    }

    /**
     * Update user two factor to true
     */
    @Test
    public void updateUserWithNeedsTwoFactorTrue() {

        String testUser = "codertestdontdelete7@apixio.com";

        User user = privUsers.findUserByEmail(testUser);
        Token iToken = createToken(user, AuthState.AUTHENTICATED);

        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(iToken);

        Map<String, String> params = new HashMap<>();
        params.put("needsTwoFactor", "true");

        Response response = target()
                .path("users").path(testUser)
                .request()
                .put(Entity.json(params));

        assertEquals(response.getStatus(), 200);
        assertEquals(privUsers.findUserByEmail(testUser).getNeedsTwoFactor(), true);
    }

    /**
     *  Update user two factor to null
     */
    @Test
    public void updateUserWithNeedsTwoFactorNull(){

        String testUser = "codertestdontdelete7@apixio.com";

        User user = privUsers.findUserByEmail(testUser);
        Token iToken = createToken(user, AuthState.AUTHENTICATED);

        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(iToken);

        Map<String, String> params = new HashMap<>();
        params.put("needsTwoFactor", "");

        Response response = target()
                .path("users").path(testUser)
                .request()
                .put(Entity.json(params));

        assertEquals(response.getStatus(), 200);
        assertEquals(privUsers.findUserByEmail(testUser).getNeedsTwoFactor(), null);
    }

    @Test
    public void deleteUserCellPhoneNumber()
    {
        User user = privUsers.findUserByEmail(TEST_CODER);

        Response response = target()
                .path("users").path("priv").path(user.getID().toString())
                .path("cellPhone")
                .request().delete();

        assertEquals(response.getStatus(), 200);
        assertEquals(privUsers.findUserByEmail(TEST_CODER).getCellPhone(),"");

        // clean up
        privUsers.update(user);
    }

    public String verifyOrgAndUser(String orgName, String userStr) throws IOException
    {
        // Verify org exits
        Organization org = BaseResourceTest.sysServices.getOrganizations().findOrganizationByName(orgName);
        assertNotNull(orgName + " does not exist ", org);

        User user = privUsers.findUserByEmail(userStr);
        // delete the user when the user exists
        if(user != null)
        {
            deleteUser(userStr);
        }
        return org.getID().toString();
    }

    private static void deleteUser(String userStr) throws IOException
    {
        User user = privUsers.findUserByEmail(userStr);
        if(user != null)
            BaseResourceTest.sysServices.getPrivUserLogic().deleteUser(user.getID());
    }


    @AfterClass
    public static void cleanup() throws IOException
    {
        for(int i=1;i<counter;i++)
        {
            deleteUser(getUniqueUserStr(i));
        }
    }
}