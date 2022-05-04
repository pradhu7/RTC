package com.apixio.useracct.dw;

import java.io.IOException;

import com.apixio.XUUID;
import com.apixio.aclsys.entity.AccessType;
import com.apixio.aclsys.entity.Operation;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.useracct.PrivSysServices;

class AclTester {

    public static void test(PrivSysServices sysServices)
    {
        //        testcql(sysServices);
        //        testAcl(sysServices);
        //        testGroups(sysServices);
        //        testGroupAcl(sysServices);
        //        testGroupDelete(sysServices);
        //        testTypeSetup(sysServices);
        //        testTypeDelete(sysServices);
        testOpDelete(sysServices, false);
    }

    private static void testOpDelete(PrivSysServices pss, boolean setup)
    {
        String     code     = "CanCode";
        String     review   = "CanReview";

        try
        {
            if (setup)
            {
                String     readAcc  = "ReadAccess";
                String     writAcc  = "WriteAccess";
                String     org1     = "Scripps";
                String     org2     = "IHC";
                XUUID      scott    = XUUID.create("SCOTT");
                XUUID      joji     = XUUID.create("JOJI");
                Operation  codeOp   = pss.getPrivOperations().createOperation(code, code);
                Operation  reviewOp = pss.getPrivOperations().createOperation(review, review);
                AccessType readAt   = pss.getPrivAccessTypes().createAccessType(readAcc, readAcc);
                AccessType writAt   = pss.getPrivAccessTypes().createAccessType(writAcc, writAcc);

                codeOp.addAccessType(readAt); codeOp.addAccessType(writAt);
                reviewOp.addAccessType(readAt);
                pss.getOperations().update(codeOp);
                pss.getOperations().update(reviewOp);

                System.out.println("################ before adding permissions");
                System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
                System.out.println("##### for " + joji);     dumpAll(pss, joji, code, review, readAcc, writAcc, org1, org2);

                pss.getAclLogic().addPermission(null, scott, code,   org1);
                pss.getAclLogic().addPermission(null, scott, code,   org2);
                pss.getAclLogic().addPermission(null, joji,  review, org2);

                System.out.println("################ after adding permissions");
                System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
                System.out.println("##### for " + joji);     dumpAll(pss, joji, code, review, readAcc, writAcc, org1, org2);
            }
            else
            {
                //                pss.getAclLogic().deleteOperation(review);
            }
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    private static void testTypeDelete(PrivSysServices pss)
    {
        try
        {
            String     code     = "CanCode";
            String     review   = "CanReview";
            String     readAcc  = "ReadAccess";
            String     writAcc  = "WriteAccess";
            
            pss.getPrivAccessTypeLogic().deleteAccessType(readAcc);
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    private static void testTypeSetup(PrivSysServices pss)
    {
        try
        {
            String     code     = "CanCode";
            String     review   = "CanReview";
            String     readAcc  = "ReadAccess";
            String     writAcc  = "WriteAccess";
            Operation  codeOp   = pss.getPrivOperations().createOperation(code, code);
            Operation  reviewOp = pss.getPrivOperations().createOperation(review, review);
            AccessType readAt   = pss.getPrivAccessTypes().createAccessType(readAcc, readAcc);
            AccessType writAt   = pss.getPrivAccessTypes().createAccessType(writAcc, writAcc);
            
            codeOp.addAccessType(readAt); codeOp.addAccessType(writAt);
            reviewOp.addAccessType(readAt);
            pss.getOperations().update(codeOp);
            pss.getOperations().update(reviewOp);
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    private static void testGroupDelete(PrivSysServices pss)
    {
        try
        {
            UserGroup  admins   = pss.getUserGroupDao().createGroup("administrators");
            XUUID      scott    = XUUID.create("u");
            XUUID      vram     = XUUID.create("u");
            String     code     = "CanCode";
            String     review   = "CanReview";
            String     org1     = "Scripps";
            String     org2     = "IHC";
            Operation  codeOp   = pss.getPrivOperations().createOperation(code, code);
            Operation  reviewOp = pss.getPrivOperations().createOperation(review, review);
            
            pss.getUserGroupDao().addMemberToGroup(admins, scott);
            pss.getUserGroupDao().addMemberToGroup(admins, vram);

            System.out.print("All group members:  " + pss.getUserGroupLogic().getAllGroups());

            pss.getAclLogic().addPermission(null, admins.getID(), code, org1);
            pss.getAclLogic().addPermission(null, admins.getID(), review, org1);
            pss.getAclLogic().addPermission(null, admins.getID(), review, org2);

            pss.getUserGroupLogic().deleteGroup(admins.getID());

            System.out.println("\n\n################ after deleting group");
            System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, null, null, org1, org2);
            System.out.println("##### for admins " + admins.getID());    dumpAll(pss, admins.getID(), code, review, null, null, org1, org2);
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    private static void testGroupAcl(PrivSysServices pss)
    {
        try
        {
            UserGroup  admins   = pss.getUserGroupDao().createGroup("administrators");
            XUUID      scott    = XUUID.create("u");
            String     code     = "CanCode";
            String     review   = "CanReview";
            String     readAcc  = "ReadAccess";
            String     writAcc  = "WriteAccess";
            String     org1     = "Scripps";
            String     org2     = "IHC";
            Operation  codeOp   = pss.getPrivOperations().createOperation(code, code);
            Operation  reviewOp = pss.getPrivOperations().createOperation(review, review);
            AccessType readAt   = pss.getPrivAccessTypes().createAccessType(readAcc, readAcc);
            AccessType writAt   = pss.getPrivAccessTypes().createAccessType(writAcc, writAcc);
            
            pss.getUserGroupDao().addMemberToGroup(admins, scott);

            System.out.print("All group members:  " + pss.getUserGroupLogic().getAllGroups());

            codeOp.addAccessType(readAt); codeOp.addAccessType(writAt);
            reviewOp.addAccessType(readAt);
            pss.getOperations().update(codeOp);
            pss.getOperations().update(reviewOp);

            System.out.println("################ before adding permissions");
            System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
            System.out.println("##### for admins " + admins.getID());    dumpAll(pss, admins.getID(), code, review, readAcc, writAcc, org1, org2);
            
            pss.getAclLogic().addPermission(null, admins.getID(), code, org1);

            System.out.println("\n\n################ after adding permissions");
            System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
            System.out.println("##### for admins " + admins.getID());    dumpAll(pss, admins.getID(), code, review, readAcc, writAcc, org1, org2);
            
            pss.getAclLogic().addPermission(null, admins.getID(), review, org1);
            pss.getAclLogic().addPermission(null, admins.getID(), review, org2);
            System.out.println("!!!! getBySubject(" + admins.getID() +"): " + pss.getAclLogic().getBySubject(admins.getID()));

            pss.getAclLogic().removePermission(null, admins.getID(), code, org1);

            System.out.println("\n\n################ after removing permissions");
            System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
            System.out.println("##### for admins " + admins.getID());    dumpAll(pss, admins.getID(), code, review, readAcc, writAcc, org1, org2);
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }


    private static void testGroups(PrivSysServices pss)
    {
        try
        {
            UserGroup group = pss.getUserGroupDao().createGroup("  Hello! \" [] ");
            XUUID     scott = XUUID.create("u");
            XUUID     vram  = XUUID.create("u");

            pss.getUserGroupDao().addMemberToGroup(group, scott);
            pss.getUserGroupDao().addMemberToGroup(group, vram);
            System.out.println("SFM:  scott = " + scott + ", vram = " + vram);
            System.out.println("SFM:  group members: " + pss.getUserGroupDao().getGroupMembers(group));
            pss.getUserGroupDao().removeMemberFromGroup(group, vram);
            System.out.println("SFM:  after remove group members: " + pss.getUserGroupDao().getGroupMembers(group));
            pss.getUserGroupDao().renameGroup(group, "goodbye");
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    private static void testAcl(PrivSysServices pss)
    {
        try
        {
            String     code     = "CanCode";
            String     review   = "CanReview";
            String     readAcc  = "ReadAccess";
            String     writAcc  = "WriteAccess";
            String     org1     = "Scripps";
            String     org2     = "IHC";
            XUUID      scott    = XUUID.create("SCOTT");
            XUUID      joji     = XUUID.create("JOJI");
            Operation  codeOp   = pss.getPrivOperations().createOperation(code, code);
            Operation  reviewOp = pss.getPrivOperations().createOperation(review, review);
            AccessType readAt   = pss.getPrivAccessTypes().createAccessType(readAcc, readAcc);
            AccessType writAt   = pss.getPrivAccessTypes().createAccessType(writAcc, writAcc);

            codeOp.addAccessType(readAt); codeOp.addAccessType(writAt);
            reviewOp.addAccessType(readAt);
            pss.getOperations().update(codeOp);
            pss.getOperations().update(reviewOp);

            System.out.println("################ before adding permissions");
            System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
            System.out.println("##### for " + joji);     dumpAll(pss, joji, code, review, readAcc, writAcc, org1, org2);

            pss.getAclLogic().addPermission(null, scott, code,   org1);
            pss.getAclLogic().addPermission(null, scott, code,   org2);
            pss.getAclLogic().addPermission(null, joji,  review, org2);

            System.out.println("################ after adding permissions");
            System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
            System.out.println("##### for " + joji);     dumpAll(pss, joji, code, review, readAcc, writAcc, org1, org2);

            //            pss.getAclLogic().removePermission(scott, code,   org1);
            //            pss.getAclLogic().removePermission(scott, code,   org2);
            pss.getAclLogic().removePermission(null, joji,  review, org2);

            System.out.println("################ after removing permissions");
            System.out.println("##### for " + scott);    dumpAll(pss, scott, code, review, readAcc, writAcc, org1, org2);
            System.out.println("##### for " + joji);     dumpAll(pss, joji, code, review, readAcc, writAcc, org1, org2);
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    private static void dumpAll(PrivSysServices pss, com.apixio.XUUID subject, String op1, String op2, String at1, String at2, String obj1, String obj2) throws IOException
    {
        dumpOpPriv(pss, subject, op1, obj1);
        dumpOpPriv(pss, subject, op1, obj2);
        dumpOpPriv(pss, subject, op2, obj1);
        dumpOpPriv(pss, subject, op2, obj2);

        if (at1 != null)
        {
            dumpAtPriv(pss, subject, at1, obj1);
            dumpAtPriv(pss, subject, at1, obj2);
            dumpAtPriv(pss, subject, at2, obj1);
            dumpAtPriv(pss, subject, at2, obj2);
        }
    }

    private static void dumpOpPriv(PrivSysServices pss, com.apixio.XUUID subject, String operation, String object) throws IOException
    {
        System.out.println("hasPerm(" + subject.toString() + ", " + operation + ", " + object + ") -> " +
                           pss.getAclLogic().hasPermission(subject, operation, object));
    }
    private static void dumpAtPriv(PrivSysServices pss, com.apixio.XUUID subject, String accessType, String object) throws IOException
    {
        System.out.println("hasAccess(" + subject.toString() + ", " + accessType + ", " + object + ") -> " +
                           pss.getAclLogic().hasAccess(subject, accessType, object));
    }

    private static void testcql(PrivSysServices pss)
    {
        CqlCrud cc = pss.getCqlCrud();
        byte[] chars = "hello".getBytes();
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(chars);

        try
        {
            String rk = "scottsrowkey_" + (System.currentTimeMillis() % 1000L);

            cc.insertRow("apx_cfAcl", rk, "scottscolumn_1", bb, false);
            cc.insertRow("apx_cfAcl", rk, "scottscolumn_2", bb, false);
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

}
