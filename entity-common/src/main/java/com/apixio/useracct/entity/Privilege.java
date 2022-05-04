package com.apixio.useracct.entity;

import java.util.HashMap;
import java.util.Map;

import com.apixio.utility.StringUtil;

/**
 * A Privilege encapsulates the information about a role-based privilege.  A
 * privilege applies either to a member of the target organization (which is
 * what isForMember is for) or to a non-member, but not both (at least not a
 * single Privilege).  The Operation name is used to add the permission to the
 * UserGroup (which is selected by the isForMember and role).  The actual ACL
 * Object (in the triplet) is determined at runtime via the value of
 * objectResolver.
 *
 * Privileges are contained within, and are persisted with, Roles.  The persistent
 * form of a Privilege uses StringUtil.mapToString.
 */
public class Privilege {

    /**
     * Constants that define what aspect of the "assign user to role" bundle of
     * information (basically the method params and what can be reached from them)
     * to use as the Object in the triplet [Subject, Operation, Object].
     *
     *  "member-of-org" will use the User's "member-of" organization (e.g., who
     *  they're employed by)
     *
     *  "target-org" will use the orgID of the organization that the user is being
     *  assigned a role within
     *
     *  "target-proj" will use the projectID of the project that the user is being
     *  assigned a role within.
     *
     *  "target-projorg" will use the organizationID that owns the project that
     *  the user is being assigned a role within.
     *
     *  "*" will use set the ACL Object in the tuple to "*"
     */
    public final static String RESOLVER_MEMBER_OF_ORG  = "member-of-org";
    public final static String RESOLVER_TARGET_ORG     = "target-org";
    public final static String RESOLVER_TARGET_PROJECT = "target-proj";
    public final static String RESOLVER_TARGET_PROJORG = "target-projorg";
    public final static String RESOLVER_TARGET_PROJPDS = "target-projpds";
    public final static String RESOLVER_ACL_GROUPNAME  = "acl-groupname";
    public final static String RESOLVER_ANY_ALL        = "*";

    /**
     * Names of persisted fields (fields are persisted to a String).
     */
    private final static String PERSIST_FORMEMBER = "forMember";
    private final static String PERSIST_OPERATION = "operation";
    private final static String PERSIST_RESOLVER  = "accessor";   // "accessor" is historical

    /**
     * Runtime fields
     */
    private boolean isForMember;
    private String  operationName;
    private String  objectResolver;

    public Privilege(boolean isForMember, String operationName, String objectResolver)
    {
        validateResolver(objectResolver);

        this.isForMember    = isForMember;
        this.operationName  = operationName;
        this.objectResolver = objectResolver;
    }

    public static Privilege restore(String persistedForm)
    {
        Map<String, String> pform = StringUtil.mapFromString(persistedForm);

        return new Privilege(Boolean.valueOf(pform.get(PERSIST_FORMEMBER)),
                             pform.get(PERSIST_OPERATION),
                             pform.get(PERSIST_RESOLVER));
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Privilege))
            return false;

        Privilege p2 = (Privilege) o;

        return ((p2.isForMember == this.isForMember) &&
                p2.operationName.equals(this.operationName) &&
                p2.objectResolver.equals(this.objectResolver));
    }

    @Override
    public int hashCode()
    {
        return ((isForMember) ? 13 : 19) + operationName.hashCode() + objectResolver.hashCode();
    }

    /**
     * Getters
     */ 
    public boolean forMember()
    {
        return isForMember;
    }

    public String getOperationName()
    {
        return operationName;
    }

    public String getObjectResolver()
    {
        return objectResolver;
    }

    /**
     * Convert to a form that can be restored via the restore factory method.
     */
    public String persist()
    {
        Map<String, String> pform = new HashMap<String, String>(5);

        pform.put(PERSIST_FORMEMBER, Boolean.toString(isForMember));
        pform.put(PERSIST_OPERATION, operationName);
        pform.put(PERSIST_RESOLVER,  objectResolver);

        return StringUtil.mapToString(pform);
    }

    /**
     * Make sure resolover string is one of the allowed ones.
     */
    public static boolean isValidResolver(String resolver)
    {
        return ((RESOLVER_MEMBER_OF_ORG.equals(resolver)  ||
                 RESOLVER_TARGET_ORG.equals(resolver)     ||
                 RESOLVER_TARGET_PROJECT.equals(resolver) ||
                 RESOLVER_TARGET_PROJORG.equals(resolver) ||
                 RESOLVER_TARGET_PROJPDS.equals(resolver) ||
                 RESOLVER_ACL_GROUPNAME.equals(resolver)  ||
                 RESOLVER_ANY_ALL.equals(resolver)));
    }

    /**
     * Make sure resolver string is one of the allowed ones.
     */
    private void validateResolver(String resolver)
    {
        if (!isValidResolver(resolver))
            throw new IllegalArgumentException("Unknown object resolver type [" + resolver + "]");
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[priv " + isForMember + ";" + operationName + ";" + objectResolver + "]");

    }
}
