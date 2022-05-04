package com.apixio.restbase.apiacl;

import java.util.List;
import java.util.Map;

import com.apixio.SysServices;

/**
 * RestEnforcer defines the means by which ApiCheck instances are created, where
 * one ApiCheck object is created for each actual check that must be performed
 * (as defined by the configuration) to check access across all defined APIs.
 */
public interface RestEnforcer {

    /**
     * The RestEnforcer model allows for a single level of "and/or" processing of
     * a list of tests.  This enum is the "and/or" specifier.  A RestEnforcer
     * implementation can reject a list of more than 1 by throwing an exception
     * during createApiCheck.
     */
    public enum BooleanOp { AND, OR }

    /**
     * Initialize the enforcer by letting it grab any services it needs. 
     */
    public void init(SysServices sysServices);

    /**
     * Create an ApiCheck object of the given implementation type (e.g., by role
     * or by state or by ACL permission) with the given configuration.
     */
    public ApiCheck createApiCheck(ApiAcls.InitInfo initInfo, ProtectedApi api, BooleanOp op,
                                   List<Map<String, String>> configs);

}
