package com.apixio;

/**
 * ObjectTypes is a centralized definition of XUUID prefixes (to make it easier
 * to avoid reusing an existing prefix).
 */
public class ObjectTypes {

    /**
     * Keep alphabetized by the actual prefix value.
     */

    public static final String ACCESS_TYPE      = "A";
    public static final String ASSOCIATION      = "AS";  // generalized n-way association
    public static final String FILTER           = "CF";  // Customer filter
    public static final String CUSTOM_PROPERTY  = "CP";
    public static final String FILTER_GROUP     = "FG";
    public static final String FILTER_TEMPLATE  = "FT";
    public static final String CUSTOMER         = "O";       // "O" for Organization, because there are already existing refs to XUUIDs of that type...
    public static final String PATIENT_DATASET  = CUSTOMER;  // sigh... backwards compatibility
    public static final String ORG_TYPE         = "OT";
    public static final String OPERATION        = "P";
    public static final String PASSWORD_POLICY  = "PP";
    public static final String PROJECT_BASEPFX  = "PR";  // ("PR" because of too many "P" ones); actual XUUID prefix will be anything that starts with PR.
    public static final String OLD_ROLE         = "R";   // deprecated july 2015
    public static final String ROLE_SET         = "RS";
    public static final String ROLE             = "R2";  // v2 of Role
    public static final String RULE             = "RU";
    public static final String SYS_STATE        = "SS";
    public static final String TOKEN            = "T";
    public static final String TEXT_BLOB        = "TX";
    public static final String USER             = "U";
    public static final String USER_ORG         = "UO";
    public static final String ORGANIZATION     = USER_ORG;  // ONLY because it eases the data migration...
    public static final String VERIFY_LINK      = "V";

}
