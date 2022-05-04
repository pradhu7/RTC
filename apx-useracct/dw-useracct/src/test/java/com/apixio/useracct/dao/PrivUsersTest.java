package com.apixio.useracct.dao;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.OldRole;
import com.apixio.useracct.entity.User;
import com.apixio.useracct.util.OTPUtil;
import com.google.common.io.Resources;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class PrivUsersTest
{
    private static final String DUMMY_EMAIL_ADDRESS = "dev-testing-dummy@apixio.com";
    private static PrivSysServices privSysServices;
    private static boolean isSetupDone;
    private static PrivUsers privUsers;

    @Before
    public void setUp() throws Exception
    {
        if (!isSetupDone)
        {
            ServiceConfiguration configuration = (new YamlConfigurationFactory<ServiceConfiguration>(ServiceConfiguration.class,
                    Validators.newValidator(), Jackson.newObjectMapper(), "dw")).build(new File(Resources.getResource("user-account-stg.yaml").toURI()));

            PersistenceServices ps = ConfigUtil.createPersistenceServices(configuration);
            DaoBase daoBase = new DaoBase(ps);
            privSysServices = new PrivSysServices(daoBase, false, configuration);
            privUsers = privSysServices.getPrivUsers();
            isSetupDone = true;
        }
    }

    /**
     * Verify the addition of otp for a user
     */
    @Test
    public void verifyAddOTP()
    {
        // setup
        User dummyUser = createDummyUser();

        String otp = OTPUtil.createOTP(8);
        privUsers.addOTPByUserWithExpiry(dummyUser, otp, 60);

        // Verify the OTP matches
        Assert.assertEquals(privUsers.findOTPByUser(dummyUser), otp);

        privUsers.deleteOTPByUser(dummyUser);

        // Verify the otp is deleted
        Assert.assertNull(privUsers.findOTPByUser(dummyUser));

        // cleanup
        deleteDummyUser(dummyUser);

    }

    @Test
    public void testCreateUserWithNoTwoFactor(){
        // setup
        User dummyUser = privUsers.createUser(new CanonicalEmail(DUMMY_EMAIL_ADDRESS),
                AccountState.NEW, privSysServices.getOldRoleLogic().getRole(OldRole.USER));
        Assert.assertNull(dummyUser.getNeedsTwoFactor());
        User user = privUsers.findUserByEmail(DUMMY_EMAIL_ADDRESS);
        Assert.assertNull(user.getNeedsTwoFactor());
        deleteDummyUser(dummyUser);

    }

    @Test
    public void testCreateUserWithTwoFactor(){
        // setup
        User dummyUser = privUsers.createUser(new CanonicalEmail(DUMMY_EMAIL_ADDRESS),
                AccountState.NEW, privSysServices.getOldRoleLogic().getRole(OldRole.USER));
        Assert.assertNull(dummyUser.getNeedsTwoFactor());
        dummyUser.setNeedsTwoFactor(true);
        privUsers.update(dummyUser);

        User user = privUsers.findUserByEmail(DUMMY_EMAIL_ADDRESS);

        user.setNeedsTwoFactor(null);
        privUsers.update(user);

        user = privUsers.findUserByEmail(DUMMY_EMAIL_ADDRESS);
        Assert.assertNull(user.getNeedsTwoFactor());

        deleteDummyUser(dummyUser);

    }

    private void deleteDummyUser(User dummyUser)
    {
        privUsers.deleteUser(dummyUser);
    }

    private User createDummyUser()
    {
        return privUsers.createUser(new CanonicalEmail(DUMMY_EMAIL_ADDRESS),
                AccountState.NEW, privSysServices.getOldRoleLogic().getRole(OldRole.USER));
    }

}
