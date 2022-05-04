package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.AuthState;
import com.apixio.restbase.entity.Token;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.dw.UserApplication;
import com.apixio.useracct.entity.User;
import com.apixio.useracct.util.PhoneUtil;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * This test class contains test for login
 */

public class AuthRSResourceTest extends BaseResourceTest
{
    private static final String APX_TOKEN = "ApxToken";
    private static final String EXT_TOK_EXPIRED = "TA_b34e1d6f-d2a6-4889-b8fe-dccb42294750";

    private static final String TEST_CODER = "codertestdontdelete@apixio.com";
    private static Client client = null;
    private static Logger LOGGER = LoggerFactory.getLogger(AuthRSResourceTest.class);

    private static Tokens tokens;
    private static PrivUsers privUsers;
    private static String testUserPassKey;

    @ClassRule
    public static final DropwizardAppRule<ServiceConfiguration> RULE =
            new DropwizardAppRule<>(UserApplication.class, ResourceHelpers.resourceFilePath("user-account-stg.yaml"));


    static
    {
        client = new JerseyClientBuilder().build();
        mainSetup();
        tokens = sysServices.getTokens();
        privUsers = sysServices.getPrivUsers();
        testUserPassKey = sysServices.getRedisKeyPrefix() + "testUserPassKey";
    }


    private Response verifyTwoFactorCode(String itokenStr, String otp)
    {
        Cookie cookie = new Cookie(APX_TOKEN, itokenStr);
        Map<String, String> codeParams = new HashMap<>();
        Response response;
        codeParams.put("code", otp);

        response = client.target(
                String.format("http://localhost:%d/", RULE.getLocalPort()))
                .path("auths").path("codeVerification")
                .request().cookie(cookie)
                .post(Entity.json(codeParams));

        // hack due to race condition:  the http headers and entity body are returned on the socket
        // (and are then processed here) before the server thread actually ends the redis transaction
        // and so the request here to get the Token from redis fails because it's not yet persisted
        delay(500);

        return response;
    }

    private Response authenticateUser(String user, int retry)
    {
        // Verify if the user exists
        User userOj = sysServices.getUsers().findUserByEmail(user);
        assertNotNull(userOj);

        Map<String, String> authParams = new HashMap<>();
        Response response;

        authParams.put("email", user);
        authParams.put("password", sysServices.getRedisOps("").get(getTestUserPassKey(userOj.getID())));

        response = client.target(
                String.format("http://localhost:%d", RULE.getLocalPort())).path("auths")
                .request()
                .post(Entity.json(authParams));

        // hack due to race condition:  the http headers and entity body are returned on the socket
        // (and are then processed here) before the server thread actually ends the redis transaction
        // and so the request here to get the Token from redis fails because it's not yet persisted
        delay(500);

        try
        {
            if ((response.getStatus() != 200) && retry < 1)
            {
                // sometimes account can get locked out so adding a
                // hack to reset the account to active state :(
                String pass = getRandomValidPass();
                sysServices.getPrivUserLogic().forcePasswordSet(userOj.getID(), pass);
                sysServices.getRedisOps("").set(getTestUserPassKey(userOj.getID()), pass);
                authenticateUser(user, retry + 1);
            }
        } catch (IOException ex)
        {
        }

        return response;
    }

    private String getRandomValidPass() {
        Random r = new Random();
        int v = Math.abs(r.nextInt()) % 100000 + 10000;
        String pass = "Passw0rd@!" + v;
        return pass;
    }

    private String getTestUserPassKey(XUUID id) {
        return testUserPassKey + id.toString();
    }

    private void delay(long ms)
    {
        try
        {
            Thread.currentThread().sleep(ms);
        } catch (InterruptedException x)
        {
        }
    }

    private Response resendOTP(String itokenStr)
    {
        Cookie cookie = new Cookie(APX_TOKEN, itokenStr);

        return client.target(
                String.format("http://localhost:%d/", RULE.getLocalPort()))
                .path("users").path("code")
                .request().cookie(cookie)
                .post(Entity.json(null));
    }

    private Response getAuthStatus(String itokenStr)
    {
        Cookie cookie = new Cookie(APX_TOKEN, itokenStr);

        return client.target(
                String.format("http://localhost:%d/", RULE.getLocalPort()))
                .path("auths").path("status")
                .request().cookie(cookie)
                .get();
    }

    @Test
    public void userAuthTestWithTwoFactor()
    {
        Token xtoken = verifyLogin();

        // before i run my next api command I need to set my internal token in the cookie
        Token itoken = tokens.createInternalToken(xtoken.getID());

        // need to get the right otp for success check
        User user = privUsers.findUserByID(itoken.getUserID());
        String otp = privUsers.findOTPByUser(user);

        // Verify if the user is authenticated using auths/status/
        verifyauthstatus(itoken, false);

        Response respVerify = verifyTwoFactorCode(itoken.getID().toString(), otp);
        assertEquals(respVerify.getStatus(), 200);

        Map<String, String> respParamsVer = respVerify.readEntity(HashMap.class);

        // Verify that the token returned is external token
        assertTrue(respParamsVer.get("token").startsWith("TA"));

        xtoken = tokens.findTokenByID(XUUID.fromString(respParamsVer.get("token")));
        assertSame(xtoken.getAuthState(), AuthState.AUTHENTICATED);

        assertNull(privUsers.findOTPByUser(user));

        //check the auth status again for confirmation that the user is authenticated
        Token it = tokens.createInternalToken(xtoken.getID());
        verifyauthstatus(it, true);
    }

    private void verifyauthstatus(Token itoken, boolean status)
    {
        Response preLoginStatus = getAuthStatus(itoken.getID().toString());
        assertEquals(preLoginStatus.getStatus(), 200);
        assertEquals(preLoginStatus.readEntity(HashMap.class).get("authenticated"), status);
    }

    /**
     *  verify the output from logging in for a user/ org who requires two factor
     * @return external token which has a password authenticated state
     */
    private Token verifyLogin()
    {
        String userNeedsTwoFactor = TEST_CODER;
        Response respLogin = authenticateUser(userNeedsTwoFactor, 0);

        assertEquals(respLogin.getStatus(), 200);

        Map<String, String> respParamsAuths = respLogin.readEntity(HashMap.class);
        LOGGER.info(" Login response parameters " + respParamsAuths);
        assertEquals(respParamsAuths.get("needsTwoFactor"), true);

        Token xtoken = getTokenAndVerifyState(XUUID.fromString(respParamsAuths.get("token")), AuthState.PASSWORD_AUTHENTICATED);

        User user = privUsers.findUserByEmail(userNeedsTwoFactor);
        assertEquals(respParamsAuths.get("partialPhoneNumber"), PhoneUtil.getLast2DigitsPhNo(user.getCellPhone()));

        return xtoken;
    }

    @Test
    public void verifyCodeWithInValidToken()
    {
        String someCode = "354644";
        Response respTwoFactor = verifyTwoFactorCode(EXT_TOK_EXPIRED, someCode);
        assertEquals(respTwoFactor.getStatus(), 400);
    }

    @Test
    public void userAuthWithTwoFactorInvalidOTP()
    {
        Token loginToken = verifyLogin();

        // before i run my next api command I need to set my internal token in the cookie
        Token iToken = tokens.createInternalToken(loginToken.getID());

        // Invalid invalidOTP
        String invalidOTP = "343242";

        Response respVerify = verifyTwoFactorCode(iToken.getID().toString(), invalidOTP);
        assertEquals(respVerify.getStatus(), 400);

        //Verify that the authstate is still the same for the external token in Redis
        Token xToken = getTokenAndVerifyState(loginToken.getID(), AuthState.PASSWORD_AUTHENTICATED);

        // Verify the otp has not been deleted
        User user = privUsers.findUserByID(xToken.getUserID());
        assertNotNull(privUsers.findOTPByUser(user));
    }

    @Test
    public void userAuthWithTwoFactorAndResendOTP()
    {
        Token loginToken = verifyLogin();

        // before i run my next api command I need to set my internal token in the cookie
        Token iToken = tokens.createInternalToken(loginToken.getID());

        User user = privUsers.findUserByID(loginToken.getUserID());
        String otpBeforeResend = privUsers.findOTPByUser(user);

        // verify auths status
        verifyauthstatus(iToken, false);

        // resend otp will overwrite the old otp
        Response respResend = resendOTP(iToken.getID().toString());
        assertEquals(respResend.getStatus(), 200);

        String otpAfterResend = privUsers.findOTPByUser(user);

        // OTP is changed after the resend
        assertNotEquals(otpBeforeResend, otpAfterResend);

        Response respVerify = verifyTwoFactorCode(iToken.getID().toString(), otpAfterResend);
        assertEquals(respVerify.getStatus(), 200);

        //Verify that the authstate is still the same for the external token in Redis
        Token xToken = getTokenAndVerifyState(loginToken.getID(), AuthState.AUTHENTICATED);

        // verify final auth status
        Token it = tokens.createInternalToken(loginToken.getID());
        verifyauthstatus(it, true);

        // Verify the otp has been deleted
        User user2 = privUsers.findUserByID(xToken.getUserID());
        assertNull(privUsers.findOTPByUser(user2));

    }

    @Test
    public void userAuthTwoFactorWithoutPhoneNumber()
    {
        String userNeedsTwoFactor = "codertestdontdelete10@apixio.com";
        Response respLogin = authenticateUser(userNeedsTwoFactor, 0);
        assertEquals(respLogin.getStatus(), 200);

        Map<String, Object> respParamsAuths = respLogin.readEntity(HashMap.class);

        LOGGER.info(" Login response parameters " + respParamsAuths);

        assertEquals(respParamsAuths.get("needsTwoFactor"), true);
        assertEquals(respParamsAuths.get("error"), "ERR_PHONE_MISSING");

        getTokenAndVerifyState(XUUID.fromString(respLogin.getCookies().get("ApxToken").getValue()), AuthState.PASSWORD_AUTHENTICATED);
    }

    private Token getTokenAndVerifyState(XUUID tokenID, AuthState authenticated)
    {
        LOGGER.info("Login token ID " + tokenID.toString());

        Token xToken = tokens.findTokenByID(tokenID);
        assertSame(xToken.getAuthState(), authenticated);
        return xToken;
    }

}