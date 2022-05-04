package com.apixio.useracct.buslog;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.datasource.redis.DistLock;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.OldRole;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PrivUserLogic.class, PrivSysServices.class,
        DistLock.class, ServiceConfiguration.class,
        Organizations.class, OrganizationLogic.class,
        LogicBase.class, SysServices.class})
public class PrivUserLogicTest
{
    private PrivUserLogic privUserLogic;
    private ExecutorService executorService;
    private PrivSysServices privSysServices;
    private SysServices sysServices;

    @Before
    public void setUp() throws Exception
    {
        privSysServices = PowerMockito.mock(PrivSysServices.class);
        ServiceConfiguration configuration = PowerMockito.mock(ServiceConfiguration.class);
        PowerMockito.suppress(PowerMockito.constructor(PrivUserLogic.class));
        privUserLogic = PowerMockito.spy(new PrivUserLogic(privSysServices, configuration));
        MemberMatcher.field(PrivUserLogic.class, "privSysServices")
                .set(privUserLogic, privSysServices);
        // Need to inject this in the base class since we are suppressing the constructors for testing
        sysServices = PowerMockito.mock(SysServices.class);
        MemberMatcher.field(LogicBase.class, "sysServices").set(privUserLogic, sysServices);

        // Create a executor service
        executorService = Executors.newFixedThreadPool(2);

        mockDistLock();
        mockOrgInteraction();
        mockUsersInteraction();
    }

    private void mockOrgInteraction()
    {
        OrganizationLogic organizationLogic = PowerMockito.mock(OrganizationLogic.class);
        PowerMockito.when(privSysServices.getOrganizationLogic()).thenReturn(organizationLogic);

        Organizations organizations = PowerMockito.mock(Organizations.class);
        PowerMockito.when(privSysServices.getOrganizations()).thenReturn(organizations);
        PowerMockito.when(organizations.findOrganizationByID(any(XUUID.class)))
                .thenReturn(PowerMockito.mock(Organization.class));
    }

    private void mockUsersInteraction()
    {
        PowerMockito.when(privSysServices.getPrivUsers())
                .thenReturn(PowerMockito.mock(PrivUsers.class));
        Users users = PowerMockito.mock(Users.class);
        PowerMockito.when(sysServices.getUsers()).thenReturn(users);
        PowerMockito.when(users.findUserByEmail(any(String.class)))
                .thenReturn(PowerMockito.mock(User.class));
    }

    private void mockDistLock() throws IllegalAccessException
    {
        DistLock distLock = PowerMockito.mock(DistLock.class);
        MemberMatcher.field(PrivUserLogic.class, "distLock").set(privUserLogic, distLock);

        // First call return a lock key and return null on the second call
        PowerMockito.when(distLock.lock(any(String.class), anyInt())).thenReturn("lockString", null);
    }

    @After
    public void tearDown() throws Exception
    {
        shutDownExecutor();
    }

    private void shutDownExecutor()
    {
        try
        {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e)
        {
            System.err.println("task interrupted");
        } finally
        {
            executorService.shutdownNow();
        }
    }


    /**
     * Multiple users created with same email and orgID should not be allowed
     */
    @Test(expected = UserLogic.UserException.class)
    public void createUser() throws Throwable
    {
        XUUID orgID = XUUID.fromString("UO_57faf530-e6e3-4677-afc5-bc87ff00ace9");
        String email = "test@apixio.com";
        OldRole role = PowerMockito.mock(OldRole.class);

        int times = 2;

        List<Future> futures = new ArrayList<>();
        Runnable task = () -> {
            try {
                privUserLogic.createUser(email, orgID, role, false, null, null);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        };

        //Submit 2 tasks to create with same email
        for (int i = 0; i < times; i++)
        {
            futures.add(executorService.submit(task));
        }

        for (Future future : futures)
        {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw e.getCause();
            }
        }
    }

    @Test
    public void verifyPhoneNumber() {
        String validPhNum = "+12139344655";
        privUserLogic.verifyPhoneNumber(validPhNum);
    }

    /**
     * The first digit should not be a zero
     */
    @Test(expected = BaseException.class)
    public void invalidPhoneNumberWithZeroFirstDigit()
    {
        String inValidPhNum = "+0139344655";
        privUserLogic.verifyPhoneNumber(inValidPhNum);
    }

    /**
     * The first charector should be a +
     */
    @Test(expected = BaseException.class)
    public void invalidPhoneNumberWithoutFirstPlusSign()
    {
        String inValidPhNum = "139344655";
        privUserLogic.verifyPhoneNumber(inValidPhNum);
    }

    /**
     * Number of digits has to less than 16
     */
    @Test(expected = BaseException.class)
    public void invalidPhoneNumberWithLenGreatorthan16()
    {
        String inValidPhNum = "+1234567891234562";
        privUserLogic.verifyPhoneNumber(inValidPhNum);
    }

    /**
     *
     */
    @Test(expected = UserLogic.UserException.class)
    public void verifyEmptyPhoneNumber() {
        String validPhNum = "";
        privUserLogic.verifyPhoneNumber(validPhNum);
    }
}