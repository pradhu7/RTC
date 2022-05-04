package com.apixio.aclsys;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.apixio.SysServices;
import com.apixio.XUUID;

/**
 * AclConstraint records value constraints for both the Subject and Object of a
 * permission tuple.  There are different types of constraints that need to be
 * supported:
 *
 *  1.  explicit set:  the value tested MUST be one of the members of the set
 *
 *  2.  user group:  the value tested MUST be a member of the given UserGroup
 *
 *  3.  any (all):  all values are acceptable.
 *
 * Constraints must be converted to/from a persisted format.  Constraints are checked
 * each time a permission is added, which is probably not too frequently overall, so
 * the convert-from-persisted form doesn't need to be that optimized.
 *
 * Because the needs overall are not complex, the persisted form can be simple.  It is:
 *
 *  classname=parameters
 *
 * where classname is the full Java classname, like com.apixio.useracct.constraints.Wildcard.
 * Parameters are defined by the needs of the class; in the case of a user group, the parameter
 * is the XUUID of the user group.
 */
public abstract class AclConstraint {

    /**
     * Factory methods to construct well-understood constraints.
     */
    public static AclConstraint allowAll()
    {
        return new WildcardConstraint();
    }
    public static AclConstraint userGroup(XUUID groupID)
    {
        return new UserGroupConstraint(groupID);
    }
    public static AclConstraint set(Collection<XUUID> allowed)
    {
        return new SetConstraint(allowed);
    }
    public static AclConstraint set(XUUID... allowed)
    {
        return new SetConstraint(Arrays.asList(allowed));
    }
    public static AclConstraint set(List<XUUID> allowed)
    {
        return new SetConstraint(allowed);
    }

    /**
     * Given the output of toPersisted(), reconstruct the original AclConstraint.
     */
    public static AclConstraint fromPersisted(String persistedForm)
    {
        int     eq = persistedForm.indexOf('=');
        String  cls;

        if (eq == -1)
            throw new IllegalArgumentException("AclConstraint persisted form must be like 'classname=parameters'; no '=' character found in '" + persistedForm + "'");

        cls = persistedForm.substring(0, eq);

        try
        {
            AclConstraint con = (AclConstraint) Class.forName(cls).newInstance();

            con.fromParams( (persistedForm.length() > eq + 1) ? persistedForm.substring(eq + 1) : "" );

            return con;
        }
        catch (Exception x)
        {
            throw new IllegalArgumentException("Failure (" + x.getMessage() + ") restoring AclConstraint '" + persistedForm + "'");
        }
    }

    /**
     * Convert from in-memory POJO to persisted form.  The string returned from this will
     * be passed to fromPersisted.
     */
    public String toPersisted()
    {
        return getClass().getName() + "=" + toParams();
    }

    /**
     * This method must return a string that captures all parameters for the constraint.
     * The value returned will be passed to fromParams() to initialize the in-memory form
     * of the constraint when being reconstructed.
     */
    protected abstract String toParams();

    /**
     * Accepts the string returned from toParams() and establishes the in-memory representation
     * of the constraint suitable for testing critera (via meetsCriteria).
     */
    protected abstract void fromParams(String params);

    /**
     * Returns true if the XXUID of the thing meets the constraint criteria.
     */
    public abstract boolean meetsCriteria(SysServices sysServices, XUUID thing);

}
