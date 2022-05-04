package com.apixio.restbase.apiacl;

import java.io.IOException;

/**
 * An instance of ApiCheck represents the actual role-check configuration for a specific protected
 * API.  Because there are multiple types of API checks, each subclass of ApiCheck must record
 * its type-specific configuration data.
 *
 * So, if the configured set of protected APIs has, say, 20 APIs each of which has 2 types of checks
 * role, state, permission) then there will be 40 instances of subclasses of this class.
 */
public abstract class ApiCheck {

    abstract public AccessResult checkPermission(CheckContext ctx, HttpRequestInfo ri, MatchResults match, Object httpEntity) throws IOException;

}
