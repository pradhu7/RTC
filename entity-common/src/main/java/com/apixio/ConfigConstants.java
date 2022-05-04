package com.apixio;

/**
 * ConfigConstants contains all values that are generally in configuration
 * (in the sense that other values similar in nature are in configuration)
 * that are also needing to be referenced in code in order for the system
 * to work.
 *
 * For example, the types of organizations are configurable but because of
 * the need to migrate via code, we need to know some of those values for
 * the migration.
 */
public class ConfigConstants {

    /**
     * Predefined types of organizations.  These MUST be the names of the type.  Required
     * minimally for migration of old UserOrgs.
     */
    public final static String CUSTOMER_ORG_TYPE = "Customer";
    public final static String SYSTEM_ORG_TYPE =   "System";
    public final static String VENDOR_ORG_TYPE =   "Vendor";

    /**
     * Predefined names of Operations that are needed for various ACL-types of things.
     * These MUST match the boot-strapped operations.
     */
    public final static String VIEWUSER_OPERATION              = "ViewUsers";
    public final static String MANAGEUSER_OPERATION            = "ManageUser";
    public final static String MANAGEACLGROUP_OPERATION        = "ManageAclGroupMembership";
    public final static String ALLOWMULTIPLESESSIONS_OPERATION = "AllowMultipleSessions";

}
