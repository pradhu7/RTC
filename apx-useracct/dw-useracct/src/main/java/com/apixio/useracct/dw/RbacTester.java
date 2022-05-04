package com.apixio.useracct.dw;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.apixio.DebugUtil;
import com.apixio.aclsys.entity.*;
import com.apixio.aclsys.buslog.*;
import com.apixio.useracct.entity.*;
import com.apixio.useracct.entity.RoleSet.RoleType;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.PrivSysServices;

/**
 * To be used with a db that has only first-level redis bootstrap data (from
 * bootstrap.sh and NOT RbacBootstrap).
 */
class RbacTester {

    static Operation    opViewDocs;
    static Operation    opViewReps;
    static Operation    opViewUsers;

    static Organization  apixio;
    static Organization  codeBusters;
    static Organization  weAreCoders;
    static Organization  wellcare;
    static Organization  wellpoint;

    static Project  proj1;

    static User   rootUser;
    static User   joji;
    static User   eric;
    static User   alex;
    static User   sally;

    public static void test(PrivSysServices sysServices) throws IOException
    {
        setupRoles(sysServices);
        setupOrgsUsers(sysServices);
        setupProjects(sysServices);

        com.apixio.DebugUtil.LOG("================= test setup done\n");

        System.out.println(sysServices.getRoleLogic().dumpProjPermissions(proj1.getID()));

        //        test6(sysServices);
        //        test7(sysServices);
        //        System.exit(1);

        System.out.print(sysServices.getRoleLogic().dumpOrgPermissions(wellcare.getID()));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(joji));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(alex));

        test1(sysServices);
        System.out.print(sysServices.getRoleLogic().dumpOrgPermissions(wellcare.getID()));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(joji));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(alex));

        test2(sysServices);
        System.out.print(sysServices.getRoleLogic().dumpOrgPermissions(wellcare.getID()));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(joji));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(alex));

        test3(sysServices);
        System.out.print(sysServices.getRoleLogic().dumpOrgPermissions(wellcare.getID()));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(joji));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(alex));

        test4(sysServices);
        System.out.print(sysServices.getRoleLogic().dumpOrgPermissions(wellcare.getID()));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(joji));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(alex));

        test5(sysServices);
        System.out.print(sysServices.getRoleLogic().dumpOrgPermissions(wellcare.getID()));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(joji));
        System.out.print(sysServices.getRoleLogic().dumpUserRoles(alex));

        teardownRoles(sysServices);
        System.out.print(sysServices.getRoleLogic().dumpOrgPermissions(wellcare.getID()));
        System.out.print(sysServices.getAclLogic().dumpAclPermissions());
    }

    private static void setupRoles(PrivSysServices pss) throws IOException
    {
        RoleSetLogic rsl           = pss.getRoleSetLogic();
        AccessType   atRead        = pss.getPrivAccessTypeLogic().createAccessType("ReadStuff",   "Reads things");
        AccessType   atWrite       = pss.getPrivAccessTypeLogic().createAccessType("WriteStuff",  "Writes things");
        RoleSet      systemRoles   = new RoleSet(RoleType.ORGANIZATION, "System",   "System");
        RoleSet      codingRoles   = new RoleSet(RoleType.ORGANIZATION, "Vendor",   "Vendor");
        RoleSet      customerRoles = new RoleSet(RoleType.ORGANIZATION, "Customer", "Customer");
        RoleSet      projectRoles  = new RoleSet(RoleType.PROJECT,      "Project.Science", "Science");
        OrgType      coding        = new OrgType("Vendor");
        OrgType      customer      = new OrgType("Customer");
        OrgType      system        = new OrgType("System");

        pss.getRoleSets().create(systemRoles);
        pss.getRoleSets().create(codingRoles);
        pss.getRoleSets().create(customerRoles);
        pss.getRoleSets().create(projectRoles);

        system.setRoleSet(systemRoles.getID());
        coding.setRoleSet(codingRoles.getID());
        customer.setRoleSet(customerRoles.getID());

        pss.getOrgTypes().create(system);
        pss.getOrgTypes().create(coding);
        pss.getOrgTypes().create(customer);

        opViewDocs  = pss.getPrivOperationLogic().createOperation("ViewDocuments", "waawaa", java.util.Arrays.asList(new String[] {"ReadStuff"}));
        opViewReps  = pss.getPrivOperationLogic().createOperation("ViewReports",   "waawaa", java.util.Arrays.asList(new String[] {"ReadStuff"}));
        opViewUsers = pss.getPrivOperationLogic().createOperation("ViewUsers",     "waawaa", java.util.Arrays.asList(new String[] {"ReadStuff"}));

        rsl.createRole(codingRoles, "QA", "QA stuff", asList(new Privilege(true, opViewReps.getName(), Privilege.RESOLVER_TARGET_ORG)));

        rsl.createRole(customerRoles, "QA",    "QA stuff", asList(new Privilege(true, opViewReps.getName(), Privilege.RESOLVER_TARGET_ORG)));
        rsl.createRole(customerRoles, "CODER", "Coding stuff",
                       asList(
                           new Privilege(true,  opViewDocs.getName(), Privilege.RESOLVER_TARGET_ORG),
                           new Privilege(false, opViewDocs.getName(), Privilege.RESOLVER_TARGET_ORG),
                           new Privilege(false, opViewReps.getName(), Privilege.RESOLVER_MEMBER_OF_ORG)
                           ));

        // project roles.  ALL Privileges MUST have "member=false" as the code in RoleLogic.getAclUserGroup tests for "member-of"
        // (organization) and the user will NEVER be "member-of" a project in the Organization "member-of" sense.
        rsl.createRole(projectRoles, "MANAGER", "Manager stuff", asList(new Privilege(false, opViewReps.getName(), Privilege.RESOLVER_TARGET_PROJECT)));

        com.apixio.DebugUtil.LOG("##### Customer/CODER = {}", rsl.lookupRoleByName("Customer/CODER").getID());
    }

    private static List<Privilege> asList(Privilege... privileges)
    {
        return Arrays.asList(privileges);
    }

    private static void teardownRoles(PrivSysServices pss) throws IOException
    {
        PrivRoleLogic rl = pss.getPrivRoleLogic();

        com.apixio.DebugUtil.LOG(".... deleting orgrole Vendor/QA");
        rl.deleteRole(rootUser.getID(), rl.lookupRoleByName("Vendor/QA"));

        com.apixio.DebugUtil.LOG(".... deleting orgrole Customer/QA");
        rl.deleteRole(rootUser.getID(), rl.lookupRoleByName("Customer/QA"));

        com.apixio.DebugUtil.LOG(".... deleting orgrole Customer/CODER");
        rl.deleteRole(rootUser.getID(), rl.lookupRoleByName("Customer/CODER"));
    }

    private static void setupOrgsUsers(PrivSysServices pss) throws IOException
    {
        OrganizationLogic ol          = pss.getOrganizationLogic();
        OldRole           root        = pss.getOldRoleLogic().getRole("ROOT");    // needed to set up users...
        OldRole           user        = pss.getOldRoleLogic().getRole("USER");    // needed to set up users...

        apixio      = ol.createOrganization("System",   "Apixio",      "Whatever", "externalID0");
        codeBusters = ol.createOrganization("Vendor",   "CodeBusters", "Whatever", "externalID1");
        weAreCoders = ol.createOrganization("Vendor",   "WeAreCoders", "Whatever", "externalID2");
        wellcare    = ol.createOrganization("Customer", "WellCare",    "Whatever", "externalID3");
        wellpoint   = ol.createOrganization("Customer", "WellPoint",   "Whatever", "externalID4");

        rootUser = pss.getPrivUserLogic().createUser("root@bad.apixio.com", apixio.getID(), root, false, null, null);

        joji   = pss.getPrivUserLogic().createUser("joji@bad.codebusters.com", codeBusters.getID(), user, false, null, null);
        eric   = pss.getPrivUserLogic().createUser("eric@bad.wearecoders.com", weAreCoders.getID(), user, false, null, null);
        alex   = pss.getPrivUserLogic().createUser("alex@bad.wellcare.com",    wellcare.getID(),    user, false, null, null);
        sally  = pss.getPrivUserLogic().createUser("sally@bad.wellpoint.com",  wellpoint.getID(),   user, false, null, null);

        DebugUtil.LOG("##### joji = {}", joji.getID());
        DebugUtil.LOG("##### alex = {}", alex.getID());

        DebugUtil.LOG("##### codebusters = {}", codeBusters.getID());
        DebugUtil.LOG("##### wearecoders = {}", weAreCoders.getID());
        DebugUtil.LOG("##### wellcare    = {}", wellcare.getID());
        DebugUtil.LOG("##### wellpoint   = {}", wellpoint.getID());
    }

    private static void setupProjects(PrivSysServices pss) throws IOException
    {
        ProjectLogic        pl = pss.getProjectLogic();
        HccProjectDTO       ep = new HccProjectDTO();

        ep.type           = HccProject.Type.SCIENCE;
        ep.organizationID = codeBusters.getID();
        ep.name           = "A Science Project";
        ep.description    = "rbac testing science project";
        ep.paymentYear    = "2015";

        proj1 = pl.createHccProject(ep);

        com.apixio.DebugUtil.LOG("##### Proj1   = {}", proj1.getID());
    }

    private static void assertOp(String claim, boolean shouldBe, boolean actuallyIs)
    {
        com.apixio.DebugUtil.LOG("==> [" + ((shouldBe != actuallyIs) ? "FAIL" : "PASS") + "] " + claim + " (should be " + shouldBe + "):  " + actuallyIs);
    }

    private static void test1(PrivSysServices pss) throws IOException
    {
        RoleLogic    rl    = pss.getRoleLogic();
        AclLogic     acl   = pss.getAclLogic();
        Role         coder = rl.lookupRoleByName("Customer/CODER");

        com.apixio.DebugUtil.LOG("######################## test1");

        rl.assignUserToRole(rootUser.getID(), joji, coder, wellcare.getID());

        assertOp("Joji has ViewDoc permission on wellcare", true,  acl.hasPermission(joji.getID(), "ViewDocuments", wellcare.getID().toString()));
        assertOp("Joji has ViewDoc permission on codebusters", false, acl.hasPermission(joji.getID(), "ViewDocuments", codeBusters.getID().toString()));
        assertOp("Joji has ViewReports permission on wellcare", false, acl.hasPermission(joji.getID(), "ViewReports", wellcare.getID().toString()));
        assertOp("Joji has ViewReports permission on codebusters", true, acl.hasPermission(joji.getID(), "ViewReports", codeBusters.getID().toString()));

        com.apixio.DebugUtil.LOG("Users with Role Customer/CODER:  " + rl.getUsersWithRole(rootUser.getID(), wellcare.getID(), coder.getID()));
    }

    private static void test2(PrivSysServices pss) throws IOException
    {
        RoleLogic    rl    = pss.getRoleLogic();
        AclLogic     acl   = pss.getAclLogic();
        Role         coder = rl.lookupRoleByName("Customer/CODER");

        com.apixio.DebugUtil.LOG("######################## test2");

        rl.assignUserToRole(rootUser.getID(), alex, coder, wellcare.getID());

        assertOp("Alex has ViewDoc permission on wellcare", true,  acl.hasPermission(alex.getID(), "ViewDocuments", wellcare.getID().toString()));
        assertOp("Alex has ViewReports permission on wellcare", false, acl.hasPermission(alex.getID(), "ViewReports", wellcare.getID().toString()));
        assertOp("Alex has ViewDoc permission on wellpoint", false,  acl.hasPermission(alex.getID(), "ViewDocuments", wellpoint.getID().toString()));
        assertOp("Alex has ViewReports permission on wellpoint", false, acl.hasPermission(alex.getID(), "ViewReports", wellpoint.getID().toString()));

        com.apixio.DebugUtil.LOG("Users with Role Customer/CODER:  " + rl.getUsersWithRole(rootUser.getID(), wellcare.getID(), coder.getID()));
    }

    private static void test3(PrivSysServices pss) throws IOException
    {
        RoleLogic   rl    = pss.getRoleLogic();
        AclLogic    acl   = pss.getAclLogic();
        Role        coder = rl.lookupRoleByName("Customer/CODER");

        com.apixio.DebugUtil.LOG("######################## test3");

        //        com.apixio.DebugUtil.LOG("Before:  Alex is member of groups {}", pss.getUserGroupDaoExt(1).getGroupsByMember(alex.getID()));
        rl.unassignUserFromRole(rootUser.getID(), alex, coder, wellcare.getID());
        //        com.apixio.DebugUtil.LOG("After1:  Alex is member of groups {}", pss.getUserGroupDaoExt(1).getGroupsByMember(alex.getID()));
        //        com.apixio.DebugUtil.LOG("After2:  Alex is member of groups {}", pss.getUserGroupDaoExt(1).getGroupsByMember(alex.getID()));
        //        com.apixio.DebugUtil.LOG("After3:  Alex is member of groups {}", pss.getUserGroupDaoExt(1).getGroupsByMember(alex.getID()));

        assertOp("Alex has ViewDoc permission on wellcare", false,  acl.hasPermission(alex.getID(), "ViewDocuments", wellcare.getID().toString()));
        assertOp("Alex has ViewReports permission on wellcare", false, acl.hasPermission(alex.getID(), "ViewReports", wellcare.getID().toString()));
        assertOp("Alex has ViewDoc permission on wellpoint", false,  acl.hasPermission(alex.getID(), "ViewDocuments", wellpoint.getID().toString()));
        assertOp("Alex has ViewReports permission on wellpoint", false, acl.hasPermission(alex.getID(), "ViewReports", wellpoint.getID().toString()));

        com.apixio.DebugUtil.LOG("Users with Role Customer/CODER:  " + rl.getUsersWithRole(rootUser.getID(), wellcare.getID(), coder.getID()));
    }

    private static void test4(PrivSysServices pss) throws IOException
    {
        RoleLogic rl    = pss.getRoleLogic();
        AclLogic  acl   = pss.getAclLogic();
        Role      coder = rl.lookupRoleByName("Customer/CODER");

        com.apixio.DebugUtil.LOG("######################## test4");

        rl.unassignUserFromRole(rootUser.getID(), joji, coder, wellcare.getID());

        assertOp("Joji has ViewDoc permission on wellcare", false,  acl.hasPermission(joji.getID(), "ViewDocuments", wellcare.getID().toString()));
        assertOp("Joji has ViewDoc permission on codebusters", false, acl.hasPermission(joji.getID(), "ViewDocuments", codeBusters.getID().toString()));
        assertOp("Joji has ViewReports permission on wellcare", false, acl.hasPermission(joji.getID(), "ViewReports", wellcare.getID().toString()));
        assertOp("Joji has ViewReports permission on codebusters", false, acl.hasPermission(joji.getID(), "ViewReports", codeBusters.getID().toString()));

        com.apixio.DebugUtil.LOG("Users with Role Customer/CODER:  " + rl.getUsersWithRole(rootUser.getID(), wellcare.getID(), coder.getID()));
    }

    private static void test5(PrivSysServices pss) throws IOException
    {
        PrivRoleLogic rl  = pss.getPrivRoleLogic();
        AclLogic      acl = pss.getAclLogic();

        com.apixio.DebugUtil.LOG("######################## test5");

        rl.assignUserToRole(rootUser.getID(), joji, rl.lookupRoleByName("Customer/CODER"), wellcare.getID());
        rl.assignUserToRole(rootUser.getID(), alex, rl.lookupRoleByName("Customer/CODER"), wellcare.getID());

        // copied from test1
        assertOp("Joji has ViewDoc permission on wellcare", true,  acl.hasPermission(joji.getID(), "ViewDocuments", wellcare.getID().toString()));
        assertOp("Joji has ViewDoc permission on codebusters", false, acl.hasPermission(joji.getID(), "ViewDocuments", codeBusters.getID().toString()));
        assertOp("Joji has ViewReports permission on wellcare", false, acl.hasPermission(joji.getID(), "ViewReports", wellcare.getID().toString()));
        assertOp("Joji has ViewReports permission on codebusters", true, acl.hasPermission(joji.getID(), "ViewReports", codeBusters.getID().toString()));

        // copied from test2
        assertOp("Alex has ViewDoc permission on wellcare", true,  acl.hasPermission(alex.getID(), "ViewDocuments", wellcare.getID().toString()));
        assertOp("Alex has ViewReports permission on wellcare", false, acl.hasPermission(alex.getID(), "ViewReports", wellcare.getID().toString()));
        assertOp("Alex has ViewDoc permission on wellpoint", false,  acl.hasPermission(alex.getID(), "ViewDocuments", wellpoint.getID().toString()));
        assertOp("Alex has ViewReports permission on wellpoint", false, acl.hasPermission(alex.getID(), "ViewReports", wellpoint.getID().toString()));

        rl.addPrivilegeToRole(rootUser.getID(), rl.lookupRoleByName("Customer/CODER"),
                                 new Privilege(true,  opViewUsers.getName(), Privilege.RESOLVER_TARGET_ORG));

        assertOp("Joji has ViewUsers permission on wellcare", false, acl.hasPermission(joji.getID(), "ViewUsers", wellcare.getID().toString()));
        assertOp("Alex has ViewUsers permission on wellcare", true, acl.hasPermission(alex.getID(), "ViewUsers", wellcare.getID().toString()));
        assertOp("Alex has ViewUsers permission on codebusters", false, acl.hasPermission(alex.getID(), "ViewUsers", codeBusters.getID().toString()));
    }

    private static void test6(PrivSysServices pss) throws IOException
    {
        RoleLogic    rl    = pss.getRoleLogic();
        AclLogic     acl   = pss.getAclLogic();
        Role         mgr   = rl.lookupRoleByName("Project.Science/MANAGER");

        com.apixio.DebugUtil.LOG("######################## test6");

        rl.assignUserToRole(rootUser.getID(), joji, mgr, proj1.getID());

        assertOp("Joji has ViewDoc permission on proj1",     false, acl.hasPermission(joji.getID(), "ViewDocuments", proj1.getID().toString()));
        assertOp("Joji has ViewReports permission on proj1", true,  acl.hasPermission(joji.getID(), "ViewReports",   proj1.getID().toString()));

        com.apixio.DebugUtil.LOG("Users with Role Project.Science/MANAGER:  " + rl.getUsersWithRole(rootUser.getID(), proj1.getID(), mgr.getID()));
    }

    private static void test7(PrivSysServices pss) throws IOException
    {
        AclLogic     acl   = pss.getAclLogic();
        RoleLogic    rl    = pss.getRoleLogic();
        Role         mgr   = rl.lookupRoleByName("Project.Science/MANAGER");

        com.apixio.DebugUtil.LOG("######################## test7:  deleting project to see if cleanup works");

        pss.getProjectLogic().deleteProject(proj1);

        assertOp("Joji has ViewDoc permission on proj1",     false, acl.hasPermission(joji.getID(), "ViewDocuments", proj1.getID().toString()));
        assertOp("Joji has ViewReports permission on proj1", false, acl.hasPermission(joji.getID(), "ViewReports",   proj1.getID().toString()));

        com.apixio.DebugUtil.LOG("Users with Role Project.Science/MANAGER:  " + rl.getUsersWithRole(rootUser.getID(), proj1.getID(), mgr.getID()));
    }

}
