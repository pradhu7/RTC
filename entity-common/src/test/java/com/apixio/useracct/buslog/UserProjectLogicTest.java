package com.apixio.useracct.buslog;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.datasource.redis.DistLock;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.UserProjects;
import com.apixio.useracct.entity.UserProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UserProjectLogic.class, SysServices.class,
                UserProjects.class, DistLock.class})
public class UserProjectLogicTest
{
    private UserProjectLogic userProjectLogic;
    private ExecutorService executorService;
    private SysServices sysServices;
    private DistLock distLock;
    private UserProjects userProjects;

    @Before
    public void initialize() throws IllegalAccessException
    {
        userProjectLogic = PowerMockito.spy(new UserProjectLogic(sysServices));
        PowerMockito.suppress(PowerMockito.constructor(UserProjectLogic.class));

        // Creating a executor service with two threads
        executorService = Executors.newFixedThreadPool(2);

        mockDistLock();
        mockUserProjects();
    }

    private void mockUserProjects() throws IllegalAccessException
    {
        userProjects = PowerMockito.mock(UserProjects.class);
        MemberMatcher.field(UserProjectLogic.class, "userProjects").set(userProjectLogic, userProjects);

        UserProject userProject = PowerMockito.mock(UserProject.class);
        PowerMockito.when(userProjects.findUserProject(any(XUUID.class), any(XUUID.class))).thenReturn(userProject);
    }

    private void mockDistLock() throws IllegalAccessException
    {
        distLock = PowerMockito.mock(DistLock.class);
        MemberMatcher.field(UserProjectLogic.class, "distLock").set(userProjectLogic,distLock);

        // First call return the first "somekey" next call returns null
        PowerMockito.when(distLock.lock(any(String.class), anyInt())).thenReturn("somekey", null);
    }

    /**
     * simultaneous call of same user to same project must not be allowed
     * Second call will throw bad request
     */
    @Test(expected = BaseException.class)
    public void addUserToProject() throws Throwable
    {
        XUUID userID = XUUID.fromString("U_8a88e311-fe62-4b4c-a284-1e280e1aac27");
        XUUID projID = XUUID.fromString("PRHCC_1f7dd765-01e3-47f9-b45c-ce634d6f2a90");

        List<String> phases = new ArrayList<>();
        int times = 2;

        List<Future> futures = new ArrayList<>();
        Runnable task = ()->{
            userProjectLogic.addUserToProject(userID, projID, true, phases);
        };

        // Submit the tasks
        for (int i = 0; i < times; i++)
        {
            futures.add(executorService.submit(task));
        }

        //Listen for exceptions
        try
        {
            for (Future future: futures)
            {
                //this will throw exceptions
                future.get();
            }
        }
        catch (ExecutionException | InterruptedException ex)
        {
            throw ex.getCause();
        }
    }

    @After
    public void terminate()
    {
        shutDownExecutorService();
    }

    /**
     * Shutdown executor gracefully
     */
    private void shutDownExecutorService()
    {
        try
        {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            System.err.println("tasks interrupted");
        }
        finally
        {
            executorService.shutdownNow();
        }
    }
}