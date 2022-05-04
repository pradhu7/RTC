package com.apixio.useracct.dw;

import java.io.IOException;
import java.util.Arrays;

import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.aclsys.*;
import com.apixio.aclsys.entity.*;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.entity.*;

class MetaAclTest {

    private PrivSysServices pss;
    private AclLogic        acls;

    //
    private AccessType readAccess;
    private AccessType writeAccess;
    private Operation  canCode;
    private Operation  canReview;
    //    private OldRole    adminRole;
    private OldRole    rootRole;
    private OldRole     userRole;
    private User       alex;
    private User       brooke;
    private User       eric;
    private User       garth;
    private User       kim;
    private User       stacy;
    private UserGroup  apixioUserGroup;
    private UserGroup  codeBustersUserGroup;
    private Organization    apixioOrg;
    private Organization    chmc;
    private Organization    scripps;

    private void setupAccessTypes()
    {
        long st = System.currentTimeMillis();

        readAccess  = pss.getPrivAccessTypes().createAccessType("ReadAccess", "Reads user data");
        writeAccess = pss.getPrivAccessTypes().createAccessType("WriteAccess", "Writes user data");

        System.out.println("setupAccessTypes took " + (System.currentTimeMillis() - st) + "ms");
    }

    private void setupOperations()
    {
        long st = System.currentTimeMillis();

        canCode   = pss.getPrivOperationLogic().createOperation("CanCode", "Can do coding for a customer",
                                                                Arrays.asList(new String[] {"ReadAccess", "WriteAccess"}));
        canReview = pss.getPrivOperationLogic().createOperation("CanReview", "Can review coding for a customer",
                                                                Arrays.asList(new String[] {"ReadAccess"}));

        System.out.println("setupOperations took " + (System.currentTimeMillis() - st) + "ms");
    }

    private void setupRoles() throws IOException
    {
        long st = System.currentTimeMillis();

        //        adminRole = pss.getOldPrivRoleLogic().createRole("ADMIN", "non-ROOT administrator", "");
        rootRole  = pss.getOldRoleLogic().getRole("ROOT");
        userRole  = pss.getOldRoleLogic().getRole("USER");

        System.out.println("setupRoles took " + (System.currentTimeMillis() - st) + "ms");
    }

    private void setupUserGroups() throws IOException
    {
        long st = System.currentTimeMillis();

        apixioUserGroup      = pss.getUserGroupLogic().createGroup("ApixioEmployees");
        codeBustersUserGroup = pss.getUserGroupLogic().createGroup("CodeBusters");

        System.out.println("setupUserGroups took " + (System.currentTimeMillis() - st) + "ms");
    }

    private void setupUserOrgs() throws IOException
    {
        long st = System.currentTimeMillis();

        apixioOrg = pss.getOrganizationLogic().createOrganization("Customer", "Scripps", "Scripps (Customer)", "SCRIPPS");
        scripps   = pss.getOrganizationLogic().createOrganization("Customer", "Scripps", "Scripps (Customer)", "SCRIPPS");
        chmc      = pss.getOrganizationLogic().createOrganization("Customer", "Apixio",  "Apixio Inc (as Customer, for now)", "Apixio");

        System.out.println("setupUserOrgs took " + (System.currentTimeMillis() - st) + "ms");
    }

    private void setupUsers() throws IOException
    {
        long st = System.currentTimeMillis();

        alex   = pss.getPrivUserLogic().createUser("alex@bad.apixio.com",   apixioOrg.getID(), rootRole,  false, null, null);
        //        eric   = pss.getPrivUserLogic().createUser("eric@bad.apixio.com",   apixioOrg.getID(), adminRole, false);
        stacy  = pss.getPrivUserLogic().createUser("stacy@bad.apixio.com",  apixioOrg.getID(), userRole,  false, null, null);
        garth  = pss.getPrivUserLogic().createUser("garth@bad.coders.com",  apixioOrg.getID(), userRole,  false, null, null);
        kim    = pss.getPrivUserLogic().createUser("kimd@bad.coders.com",   apixioOrg.getID(), userRole,  false, null, null);
        brooke = pss.getPrivUserLogic().createUser("brooke@bad.coders.com", apixioOrg.getID(), userRole,  false, null, null);

        pss.getUserGroupDao().addMemberToGroup(apixioUserGroup, alex.getID());
        pss.getUserGroupDao().addMemberToGroup(apixioUserGroup, eric.getID());
        pss.getUserGroupDao().addMemberToGroup(apixioUserGroup, stacy.getID());
        pss.getUserGroupDao().addMemberToGroup(codeBustersUserGroup, garth.getID());
        pss.getUserGroupDao().addMemberToGroup(codeBustersUserGroup, brooke.getID());
        pss.getUserGroupDao().addMemberToGroup(codeBustersUserGroup, kim.getID());

        System.out.println("setupUsers took " + (System.currentTimeMillis() - st) + "ms");
    }

    private void setup() throws IOException
    {
        setupAccessTypes();
        setupOperations();
        setupRoles();
        setupUserGroups();
        setupUserOrgs();
        setupUsers();
    }

    private MetaAclTest(PrivSysServices sysServices)
    {
        pss  = sysServices;
        acls = pss.getAclLogic();
    }

    // ################

    private void testHasPermission(User user, Operation op, Organization customer) throws IOException
    {
        boolean permission = acls.hasPermission(user.getID(), op.getName(), customer.getID().toString());

        if (permission)
            System.out.println("hasPermission(" + user.getEmailAddress() + ", " + op.getName() + ", " + customer.getName() + ") => " + permission);
    }

    private void testPermsByUser() throws IOException
    {
        System.out.println("\n>>>>>>>>> All non-false permissions:");

        testHasPermission(alex, canCode, chmc);
        testHasPermission(eric, canCode, chmc);
        testHasPermission(stacy, canCode, chmc);
        testHasPermission(garth, canCode, chmc);
        testHasPermission(brooke, canCode, chmc);
        testHasPermission(kim, canCode, chmc);

        testHasPermission(alex, canCode, scripps);
        testHasPermission(eric, canCode, scripps);
        testHasPermission(stacy, canCode, scripps);
        testHasPermission(garth, canCode, scripps);
        testHasPermission(brooke, canCode, scripps);
        testHasPermission(kim, canCode, scripps);

        testHasPermission(alex, canReview, chmc);
        testHasPermission(eric, canReview, chmc);
        testHasPermission(stacy, canReview, chmc);
        testHasPermission(garth, canReview, chmc);
        testHasPermission(brooke, canReview, chmc);
        testHasPermission(kim, canReview, chmc);

        testHasPermission(alex, canReview, scripps);
        testHasPermission(eric, canReview, scripps);
        testHasPermission(stacy, canReview, scripps);
        testHasPermission(garth, canReview, scripps);
        testHasPermission(brooke, canReview, scripps);
        testHasPermission(kim, canReview, scripps);

        System.out.println("<<<<<<<<<\n");
    }

    // ################

    private void validate(boolean actual, boolean expected, String description)
    {
        System.out.println("(should be " + expected + ") " + description + ":  " + actual);

        if (actual != expected)
            throw new RuntimeException("TEST FAILURE");
    }

    private void test1() throws IOException
    {
        System.out.println("\n################################################################ test #1");

        testPermsByUser();
    }

    private void test2() throws IOException
    {
        System.out.println("\n################################################################ test #2");

        validate(acls.addPermission(alex.getID(), garth.getID(), canCode.getName(), scripps.getID().toString()),
                 true, "Alex was able to add permission 'CanCode' for Garth for scripps only");

        testPermsByUser();
    }

    private void test3() throws IOException
    {
        System.out.println("\n################################################################ test #3");

        validate(acls.addPermission(eric.getID(), garth.getID(), canCode.getName(), scripps.getID().toString()),
                 false, "Eric was able to add permission 'CanCode' for Garth for scripps");

        testPermsByUser();

        validate(acls.grantAddPermission(eric.getID(), garth.getID(), canCode.getName(), AclConstraint.allowAll(), AclConstraint.allowAll()),
                 false, "Eric was able to add delegatable 'CanCode' for Garth");
    }

    private void test4() throws IOException
    {
        System.out.println("\n################################################################ test #4");

        validate(acls.grantAddPermission(alex.getID(), eric.getID(), canCode.getName(),
                                         AclConstraint.userGroup(codeBustersUserGroup.getID()),
                                         AclConstraint.set(scripps.getID())),
                 true, "Alex was able to add delegatable permission 'CanCode' for Eric for [codebusters:scripps]: ");

        // NOW eric should be able to do this
        validate(acls.addPermission(eric.getID(), brooke.getID(), canCode.getName(), scripps.getID().toString()),
                 true, "Eric was able to add permission 'CanCode' for Brooke for scripps");

        // but NOT this
        validate(acls.addPermission(eric.getID(), stacy.getID(), canCode.getName(), scripps.getID().toString()),
                 false, "Eric was able to add permission 'CanCode' for Stacy for scripps");

        // or this
        validate(acls.addPermission(eric.getID(), brooke.getID(), canCode.getName(), chmc.getID().toString()),
                 false, "Eric was able to add permission 'CanCode' for Brooke for CHMC");
        
        validate(acls.grantAddPermission(eric.getID(), garth.getID(), canCode.getName(), AclConstraint.allowAll(), AclConstraint.allowAll()),
                 false, "Eric was able to add delegatable 'CanCode' for Garth");

        testPermsByUser();
    }

    private void test5() throws IOException
    {
        System.out.println("\n################################################################ test #5");

        validate(acls.removeAddPermission(alex.getID(), eric.getID(), canCode.getName()),
                 true, "Alex was able to remove delegatable permission 'CanCode' for Eric for [codebusters:scripps]");

        // now eric should NOT be able to do this
        validate(acls.addPermission(eric.getID(), kim.getID(), canCode.getName(), scripps.getID().toString()),
                 false, "Eric was able to add permission 'CanCode' for Kim for scripps");

        testPermsByUser();
    }

    private void test6() throws IOException
    {
        System.out.println("\n################################################################ test #6");

        validate(acls.grantAddPermission(alex.getID(), kim.getID(), canCode.getName(), AclConstraint.allowAll(), AclConstraint.allowAll()),
                 true, "Alex was able to grant delegatable permission 'CanCode' for kim for [*:*]");

        validate(acls.addPermission(kim.getID(), eric.getID(), canCode.getName(), scripps.getID().toString()),
                 true, "Kim was able to add permission 'CanCode' for eric for [scripps]");

        validate(acls.removePermission(kim.getID(), eric.getID(), canCode.getName(), scripps.getID().toString()),
                 true, "Kim was able to remove permission 'CanCode' for eric for [scripps]");

        validate(acls.hasPermission(eric.getID(), canCode.getName(), scripps.getID().toString()),
                 false, "Eric shouldn't have permission to canCode on [scripps]");

        validate(acls.addPermission(kim.getID(), eric.getID(), canCode.getName(), scripps.getID().toString()),
                 true, "Kim was able to add permission 'CanCode' for eric for [scripps]");

        validate(acls.removeAddPermission(alex.getID(), kim.getID(), canCode.getName()),
                 true, "Alex was able to remove delegatable permission 'CanCode' for kim");

        validate(acls.hasPermission(eric.getID(), canCode.getName(), scripps.getID().toString()),
                 true, "Eric should have permission to canCode on [scripps]");

        validate(acls.addPermission(kim.getID(), eric.getID(), canCode.getName(), scripps.getID().toString()),
                 false, "Kim was able to add permission 'CanCode' for eric for [cmhc]");

        validate(acls.removePermission(kim.getID(), eric.getID(), canCode.getName(), scripps.getID().toString()),
                 false, "Kim isn't able to remove permission 'CanCode' for eric for [scripps]");

        testPermsByUser();
    }

    // ################

    public static void test(PrivSysServices sysServices) throws IOException
    {
        MetaAclTest mat = new MetaAclTest(sysServices);

        mat.setup();

        mat.test1();
        mat.test2();
        mat.test3();
        mat.test4();
        mat.test5();
        mat.test6();

        System.out.println("\n################################################################ tests completed");
    }

}
