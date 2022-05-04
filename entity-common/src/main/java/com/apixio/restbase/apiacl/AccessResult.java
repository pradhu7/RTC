package com.apixio.restbase.apiacl;

/**
 * Result of checking the access to a particular API against a specific AclCheck.
 */
public enum AccessResult
{
    NOT_FOUND,         // something required for ACL check wasn't found
    ALLOW,             // request should be allowed through
    DISALLOW,          // request should NOT be allowed through
    NEED_ENTITY        // test needs access to the HTTP entity body in order to determine access
}
