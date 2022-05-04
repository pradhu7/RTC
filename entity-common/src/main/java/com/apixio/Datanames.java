package com.apixio;

/**
 * Datanames is a centralized collection of Datatype names used with CachingBase
 * (to make it easier to make sure they're all uniquely named).
 */
public class Datanames {

    /**
     * DO NOT CHANGE THESE!  If they are changed then caching will break.
     */

    /**
     * Keep alphabetized by the actual datatype name.  Note that it can't include
     * dynamically defined datatype names...
     */

    public static final String CUSTOMERS          = "customers";
    public static final String CUSTOM_PROPERTIES  = "customproperties";   // used or not?
    public static final String CUSTOM_SCOPE       = "customprops:";       // prefix:  "scope" value is appended.  See SCOPE_* below...
    public static final String ORGANIZATIONS      = "organizations";      // note that this isn't "userorgs" because UserOrgs.java didn't extend CachingBase
    public static final String ORGTYPES           = "orgtypes";
    public static final String PASSWORD_POLICIES  = "passwordpolicy";
    public static final String PATIENT_DATASETS   = CUSTOMERS;
    public static final String PROJECTS           = "projects";
    public static final String ROLES              = "roles";
    public static final String ROLESETS           = "rolesets";
    public static final String ROLESET_MEMBERS    = "rolesetmembers";
    public static final String TEXT_BLOBS         = "textblobs";

    /**
     * The only really, truly expandable one is CUSTOM_SCOPE.  Here are the values for that
     */
    public static final String SCOPE_ORGANIZATION       = "UserOrg";            // for historical reasons...
    public static final String SCOPE_USER               = "User";
    public static final String SCOPE_PROJ_GENERIC       = "Project.generic";
    public static final String SCOPE_PROJ_PHASE         = "Project.phase";
    public static final String SCOPE_PROJ_HCC           = "Project.hcc";
    public static final String SCOPE_PROJ_QUALITY       = "Project.quality";
    public static final String SCOPE_PROJ_PROSPECTIVE   = "Project.prospective";
    public static final String SCOPE_PROJ_LABEL         = "Project.label";
    public static final String SCOPE_USERORG            = "UserOrg";
    
    /**
     * Note re SCOPE_CUSTOMER: This is really ugly but there really isn't a good solution.
     * This constant is needed to centralize the must-be-unique and must-never-change scope
     * identifier for the pre-generic version of the custom property code.  Because the
     * original use of CustomProperties was by the Customer entity we need to support existing
     * (in production) custom properties using the now-generic code/design.  This is done by
     * assuming that an empty custom property scope is really the Customer scope.  This scope
     * ID is used several places so they all reference this contant.
     */
    public static final String SCOPE_CUSTOMER           = "Customer";         // for historical reasons...
    public static final String SCOPE_PATIENT_DATASET    = SCOPE_CUSTOMER;
}
