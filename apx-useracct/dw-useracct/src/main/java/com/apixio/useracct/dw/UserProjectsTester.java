package com.apixio.useracct.dw;

import java.io.IOException;
import java.util.*;

import com.apixio.XUUID;
import com.apixio.useracct.entity.*;
import com.apixio.useracct.dao.*;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.PrivSysServices;

class UserProjectsTester {

    static UserProjects  userProjects;
    static UserProjectLogic   userProject;

    public static void test(PrivSysServices sysServices) throws IOException
    {
        XUUID user1 = XUUID.fromString("U_aaaaaaaa-496e-41c0-b7aa-efcce9a58dfe");
        XUUID user2 = XUUID.fromString("U_bbbbbbbb-496e-41c0-b7aa-efcce9a58dfe");
        XUUID user3 = XUUID.fromString("U_cccccccc-496e-41c0-b7aa-efcce9a58dfe");
        XUUID proj1 = XUUID.fromString("PR_dddddddd-1195-42ad-99d0-3ec12ab51dc6");
        XUUID proj2 = XUUID.fromString("PR_eeeeeeee-1195-42ad-99d0-3ec12ab51dc6");
        XUUID proj3 = XUUID.fromString("PR_ffffffff-1195-42ad-99d0-3ec12ab51dc6");
        XUUID proj4 = XUUID.fromString("PR_abcdfe01-1195-42ad-99d0-3ec12ab51dc6");

        userProjects = sysServices.getUserProjects();
        userProject  = sysServices.getUserProjectLogic();

        userProject.addUserToProject(user1, proj1, true, null);
        userProject.addUserToProject(user1, proj2, true, Arrays.asList(new String[] {"phase1"}));
        userProject.addUserToProject(user1, proj2, true, Arrays.asList(new String[] {"phase1", "hello"}));
        userProject.addUserToProject(user2, proj3, true, Arrays.asList(new String[] {"phase2", "goodboy"}));

        dumpByUser("Projects for User1", user1);
        dumpByUser("Projects for User2", user2);

        dumpByProject("Users for Proj1", proj1);
        dumpByProject("Users for Proj2", proj2);
        dumpByProject("Users for Proj3", proj3);

        for (UserProject up : userProjects.getProjectsForUser(user1))
            userProjects.delete(up);

        dumpByUser("(redux) Projects for User1", user1);
        dumpByUser("(redux) Projects for User2", user2);

        dumpByProject("(redux) Users for Proj1", proj1);
        dumpByProject("(redux) Users for Proj2", proj2);
        dumpByProject("(redux) Users for Proj3", proj3);

    }

    private static void dumpByUser(String desc, XUUID userID)
    {
        System.out.println("######## " + desc + " (" + userID + ")");

        for (UserProject up : userProjects.getProjectsForUser(userID))
            System.out.println("  " + up.getProjectID());
    }

    private static void dumpByProject(String desc, XUUID projID)
    {
        System.out.println("######## " + desc + " (" + projID + ")");

        for (UserProject up : userProjects.getUsersForProject(projID))
            System.out.println("  " + up.getUserID());
    }

    private static void confirm(XUUID userID, XUUID projID, boolean expectation)
    {
        UserProject upa = userProjects.findUserProject(userID, projID);

        //        System.out.println(userID + "," + projID + " => " + upa);

        if ((upa != null) && !expectation)
            System.out.println("[FAIL] expected [" + userID + ", " + projID + "] to exist but it didn't");
        else if ((upa == null) && expectation)
            System.out.println("[FAIL] expected [" + userID + ", " + projID + "] not to exist but it did");
        else
            System.out.println("[PASS]");
    }

}
