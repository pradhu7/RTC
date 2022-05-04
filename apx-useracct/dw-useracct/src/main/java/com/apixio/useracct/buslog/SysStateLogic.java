package com.apixio.useracct.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.restbase.LogicBase;
import com.apixio.ConfigConstants;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.buslog.PrivOperationLogic;
import com.apixio.aclsys.buslog.UserGroupLogic;
import com.apixio.aclsys.entity.UserGroup;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.restbase.entity.ParamSet;;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dao.OrgTypes;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dao.OldPrivRoles;
import com.apixio.useracct.dao.SysStates;
import com.apixio.useracct.entity.OldRole;
import com.apixio.useracct.entity.OrgType;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.SysState;
import com.apixio.useracct.entity.User;

/**
 * SysStateLogic is a special business logic module that deals with system-level stuff
 * such as schema changes/migrations, etc.  As such, it doesn't get exposed to
 * anything external so there's little need to protect things here.
 */
public class SysStateLogic extends LogicBase<SysServices> {

    private PrivSysServices privSysServices;

    /**
     * Latest schema version.  Anything less that this will trigger an automatic update.
     */
    private final static int SCHEMA_VERSION = 8;

    /**
     * Dumb little class so we can do a loop to upgrade through versions.
     */
    private static abstract class Upgrader {
        abstract boolean upgrade(PrivSysServices sysServices) throws Exception;
    }

    /**
     * Schema history (note that some of these are, strictly speaking, unnecessary
     * as there was either no real data to worry about (e.g., fixing canonical names
     * in a usergroup space that ended up not being used anymore) or things were undone
     * after (but not known about at the time and so the sys version got bumped anyway).
     *
     *  0:   (actually was prior to putting in SysState stuff); up through 12/17/2014
     *  1:   12/17/2014:  added indexes to Roles to keep track of users in each role
     *  2:   02/09/2015:  added Role-based UserGroups; upgrade creates groups and populates them
     *  3:   02/09/2015:  add rights for RootUserGroup to all Operations; separate from #3 due to transaction needs
     *  4:   04/20/2015:  migrate to generic CustomProperties DAO model to support properties on any entity(XUUID).
     *  5:   06/23/2015:  initial schema change for role based access control; superseded by version 6 (kept due to
     *                    being tested on staging)
     *  6:   07/22/2015:  fix UserGroup canonical name problem (cleaned name was being stored in rowkey2)
     *  7:   08/04/2015:  re-upgrade to 1,2,3 due to new Cassandra keyprefix (blech)
     *  8:   08/05/2015:  in-place migration of UserOrgs (migrate to Organization.java)
     */

    /**
     * Constructor.
     */
    public SysStateLogic(PrivSysServices sysServices)
    {
        super(sysServices);

        this.privSysServices = sysServices;
    }

    /**
     * Generic entry point into code that deals with updates to schemas.
     */
    public void performSchemaUpgrades() throws Exception
    {
        SysStates sss = privSysServices.getSysStates();
        SysState  ss  = sss.getSysState();
        int       cv  = ss.getSchemaVersion();

        if (cv < SCHEMA_VERSION)
        {
            Transactions   trans     = privSysServices.getRedisTransactions();
            boolean        upgrade   = true;
            List<Upgrader> upgraders = new ArrayList<>();

            System.out.println("############# Performing upgrade of system schema from " + ss.getSchemaVersion() + " to version " + SCHEMA_VERSION);

            // There MUST be the same # of upgraders in here as the version #
            // this full upgrade is useful only for entirely new systems (basically
            // developer systems).  Please don't remove this as it's good to be
            // able to start from scratch.
            upgraders.add(new Upgrade0to1());
            upgraders.add(new Upgrade1to2());
            upgraders.add(new Upgrade2to3());
            upgraders.add(new Upgrade3to4());
            upgraders.add(new Upgrade4to5());
            upgraders.add(new Upgrade5to6());
            upgraders.add(new Upgrade6to7());
            upgraders.add(new Upgrade7to8());

            while (cv < SCHEMA_VERSION)
            {
                long start = System.currentTimeMillis();

                trans.begin();

                try
                {
                    if (upgrade = upgraders.get(cv).upgrade(privSysServices))
                    {
                        cv++;
                        ss.setSchemaVersion(cv);
                        sss.update(ss);
                        trans.commit();

                        System.out.println("Upgrade from version " + (cv - 1) + " to " + cv + " took " + (System.currentTimeMillis() - start) + "ms");
                    }
                    else
                    {
                        trans.abort();

                        throw new IllegalStateException("Upgrade from version " + cv + " to " + (cv + 1) + " FAILED");
                    }
                }
                catch (Exception x)
                {
                    x.printStackTrace();
                    trans.abort();
                    throw x;
                }
            }
        }
    }

    /**
     * Update to schema version 1 by adding users to role indexes.
     */
    private static class Upgrade0to1 extends Upgrader {
        boolean upgrade(PrivSysServices sysServices)
        {
            OldPrivRoles pr = sysServices.getOldPrivRoles();

            for (User user : sysServices.getUsers().getAllUsers())
            {
                String userID = user.getID().toString();

                for (String role : user.getRoleNames())
                {
                    OldRole r = pr.findRoleByName(role);

                    if (r != null)
                        pr.addUserToRole(role, userID);
                }
            }

            return true;
        }
    }

    /**
     * Update to schema version 2 by creating Role-based UserGroups and populating them.
     */
    private static class Upgrade1to2 extends Upgrader {
        boolean upgrade(PrivSysServices sysServices) throws IOException
        {
            UserGroupLogic     ugl = sysServices.getUserGroupLogicExt(1);
            PrivUserLogic      pul = sysServices.getPrivUserLogic();
            PrivOperationLogic pol = sysServices.getPrivOperationLogic();

            for (OldRole role : sysServices.getOldRoleLogic().getAllRoles())
            {
                // this upgrade is called also during upgrade to v7 so don't fail out:
                if (ugl.findRoleGroup(role) == null)
                {
                    UserGroup group = ugl.createRoleGroup(role);

                    for (User user : pul.getUsersInRole(role))
                    {
                        // the "if (user != null) ..." is a bit of a hack due to
                        // the need to cleanup staging data that had two account records
                        // with the same email address (race condition:  accounts created
                        // 1ms apart); deleting from redis didn't delete from this usergroup list
                        if (user != null)
                            ugl.addMemberToGroup(group, user.getID());
                    }
                }
            }

            if (pol.getOperation("addPermission") == null)
            {
                // yucky to hardcode operation names but is the less hacky way.  These must match
                // com.apixio.aclsys.buslog.AclLogic.OP_{ADD,GRANT}PERMISSION
                pol.createOperation("addPermission",   "Operation to add permissions",      new ArrayList<String>(0));
                pol.createOperation("grantPermission", "Operation to delegate permissions", new ArrayList<String>(0));
            }

            return true;
        }
    }

    /**
     * Update to schema version 3 by adding permissions for the RootUserGroup
     */
    private static class Upgrade2to3 extends Upgrader {
        boolean upgrade(PrivSysServices sysServices) throws IOException
        {
            // After this call ROOT-roled users will have permission to call AclLogic.addPermission()
            // Note, however, that calling AclLogic.hasPermission(rootUser, operationName) will FAIL
            // until we add permission for each Operation as the hasPermission test requires an explicit
            // granting of the right to a specific Operation.
            sysServices.getAclLogic().addRootPermission();

            return true;
        }
    }

    /**
     * Update to schema version 4 by copying over list of CustomerIDs to generic property management area.
     */
    private static class Upgrade3to4 extends Upgrader {
        boolean upgrade(PrivSysServices sysServices)
        {
            sysServices.getCustomProperties().migrateToGenericProperties(sysServices.getPatientDataSets().getAllEntityIDs());

            return true;
        }
    }

    private static class Upgrade4to5 extends Upgrader {
        boolean upgrade(PrivSysServices sysServices)
        {
            // intentionally nothing since we're superseding what it did (w/ version 6); v6 was
            // run on staging so can't just reuse the version.

            return true;
        }
    }

    private static class Upgrade5to6 extends Upgrader {
        boolean upgrade(PrivSysServices sysServices) throws IOException
        {
            sysServices.getUserGroupDao().fixCanonicalNames();

            return true;
        }
    }

    /**
     * 6 -> 7:  re-migrate (old/simple) Roles into the new usergroup namespace (keyprefix)
     */
    private static class Upgrade6to7 extends Upgrader {
        boolean upgrade(PrivSysServices sysServices) throws IOException
        {
            // Because we've changed the Cassandra keyprefix for where we keep the permissions
            // (from "grp." to "extended-1.grp") and user group membership we need to re-upgrade
            // to place that info in the new keyspace.
            return ((new Upgrade0to1()).upgrade(sysServices) &&
                    (new Upgrade1to2()).upgrade(sysServices) &&
                    (new Upgrade2to3()).upgrade(sysServices));
        }
    }

    /**
     * 7 -> 8:  convert UserOrgs' way of keeping track of all UserOrgs to
     *          how CachingBase keeps track of all items.
     */
    private static class Upgrade7to8 extends Upgrader {
        private OrgType       vendorOrgType;
        private OrgType       custrOrgType;
        private OrgType       systemOrgType;

        boolean upgrade(PrivSysServices sysServices) throws IOException
        {
            RedisOps      redisOps      = sysServices.getRedisOps();
            Organizations organizations = sysServices.getOrganizations();
            OrgTypes      orgTypes      = sysServices.getOrgTypes();
            boolean       upgraded      = false;

            vendorOrgType = orgTypes.findOrgTypeByName(ConfigConstants.VENDOR_ORG_TYPE);
            custrOrgType  = orgTypes.findOrgTypeByName(ConfigConstants.CUSTOMER_ORG_TYPE);
            systemOrgType = orgTypes.findOrgTypeByName(ConfigConstants.SYSTEM_ORG_TYPE);

            if ((vendorOrgType == null) || (custrOrgType == null) || (systemOrgType == null))
            {
                // This is NOT a fatal error (mostly)
                System.out.println("ERROR:  Upgrading from schema 7 to 8 requires all OrgTypes to exist but they don't.  Skipping rest of upgrades");
            }
            else
            {
                // Organizations keeps track of all Organization instance by:
                //      private static final String INDEX_BY_XID = "uorgs-x-byxid";
                //      private static final String INDEX_ALL    = "uorgs-x-all";
                //
                // We're not trying to keep membership so we can lose the following keys:
                //      super.makeKey("uorgs-c1-" + orgID);   // for makeMembersListKey
                //      super.makeKey("uorgs-m1-" + userID);  // for makeMemberOfListKey

                // Organizations keeps track of things:
                //    String CachingBase.entityLookupAllKey = "organizations_all";

                for (String userOrgID : redisOps.lrange(organizations.makeRedisKey("uorgs-x-all"), 0, -1))
                {
                    ParamSet     params = new ParamSet(redisOps.hgetAll(organizations.makeRedisKey(userOrgID)));
                    XUUID        type   = getOrgType(sysServices, params);

                    if (type != null)
                    {
                        Organization newOrg = new Organization(params, type);

                        System.out.println("UserOrg migration:  migrated " + newOrg);

                        organizations.create(newOrg);
                    }
                    else
                    {
                        System.out.println("UserOrg migration:  did NOT migrate [" + userOrgID + "] because it didn't have a property 'orgType'");
                    }
                }

                upgraded = true;
            }

            return upgraded;
        }

        private XUUID getOrgType(SysServices sysServices, ParamSet userOrg)
        {
            String               id    = userOrg.get("id");  //!! constant taken from BaseEntity.F_ID
            Map<String, Object>  props = sysServices.getOrganizationLogic().getOrganizationProperties(XUUID.fromString(id, Organization.OBJTYPE));
            String               value = (String) props.get("orgtype");  // agreed-upon propname for migration
            
            if ((value == null) || value.equals(ConfigConstants.VENDOR_ORG_TYPE))
                return vendorOrgType.getID();
            else if (value.equals(ConfigConstants.CUSTOMER_ORG_TYPE))
                return custrOrgType.getID();
            else if (value.equals(ConfigConstants.SYSTEM_ORG_TYPE))
                return systemOrgType.getID();
            else
                throw new IllegalArgumentException("Unknown 'orgType' property value [" + value + "]; must be one of System, Vendor, Customer");
        }
    }

}
