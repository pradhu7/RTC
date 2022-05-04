package com.apixio.useracct.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AccessSpec parses and manages a list of access specifiers to URLs.  Each individual
 * URL that is to have access control attached to it is specified in a single line of
 * text with the following syntax (with '{}' signifying replaceable info)
 *
 *  {METHOD} {URL-prefix} : {type}={access} , ...
 *
 * where:
 *
 *    METHOD is one of:  GET POST PUT DELETE or "*" (to include all of those)
 *    URL-prefix is app-defined
 *    type is app-defined (can start with ~ (for "all but") or be "*" (for all)
 *    access is one of:  + allow - deny
 *
 * The basic use after parsing is to call isAllowed(HttpServletRequest), where the request
 * is used to get both the method and the actual URL.
 *
 * The runtime structure is a Map<String, List<Acl>> to map from the verb to the list
 * of access controls.
 *
 *
 * To make the ACL check quick, a map from METHOD:TYPE to a list of prefix:allow
 * is created, with enumeration of wildcards.  Note that we can't pre-cache prefix
 * information because we don't know how much of the actual URL to match on.
 *
 */
public class AccessSpec {

    // these MUST BE IN UPPERCASE (since code normalizes user-specified verbs to uppercase)
    private static final String[] VERBS = new String[] {"GET", "POST", "PUT", "DELETE"};

    static class Acl {
        String   prefix;
        boolean  allow;

        Acl(String prefix, boolean allow)
        {
            this.prefix = prefix;
            this.allow  = allow;
        }

        public String toString()
        {
            return "(" + prefix + ":" + Boolean.toString(allow) + ")";
        }
    }

    /**
     * The keys in this Map are METHOD:TYPE (where TYPE is something like
     * a role name or account state).
     */
    private Map<String, List<Acl>> access = new HashMap<String, List<Acl>>();

    private String[] allTypes;

    /**
     * Accepts a list of lines of the form:
     *
     *   METHOD URL:TYPE=perm,...
     *
     * where METHOD is an HTTP verb, URL is the prefix after the host part of a URL, and
     * perm is +/allow or -/deny and 'something' can start with a '~' to denote "all but
     * that".
     */
    public AccessSpec(String[] allTypes, List<String> details)
    {
        this.allTypes = allTypes;  // e.g., ["NO_USER", "ROOT", "ADMIN", "ROOT"]

        for (String detail : details)
            parseLine(detail);
    }

    /**
     * Return the configured allow/deny value for the combo of type/method/urlprefix.
     */
    public boolean isAllowed(String type, String method, String url)
    {
        List<Acl> acls  = access.get(method.toUpperCase() + ":" + type);
        boolean   allow = false;

        if (acls != null)
        {
            for (Acl acl : acls)
            {
                if (url.startsWith(acl.prefix))
                {
                    allow = acl.allow;
                    break;
                }
            }
        }

        return allow;
    }

    /**
     * Parses text lines of the form {VERB} {URL} : {TYPE} = {PERMISSION}, ...
     * where VERB is one of POST,GET,PUT,DELETE and URL is something like "/auths"
     * and TYPE is a string and PERMISSION is one of "+" "-" "allow" "deny".
     */
    private void parseLine(String line)
    {
        int           sp;          // where the first space char is
        int           colon;       // where the : char is
        List<String>  types;       // e.g., NO_USER, ROOT, ...
        List<Boolean> allows;      // elements matched with 'types' list
        String        method;
        String        prefix;

        line = line.trim();

        sp    = line.indexOf(' ');
        colon = line.indexOf(':');

        if (sp == -1)
            throw new IllegalArgumentException("Expected <space> after HTTP method in [" + line + "]");
        else if (colon == -1)
            throw new IllegalArgumentException("Expected : after URL in [" + line + "]");
        else if (sp > colon)
            throw new IllegalArgumentException("Expected <space> before <:> in [" + line + "]");
        else if (colon + 1 == line.length())
            throw new IllegalArgumentException("Expected ACL list after : in [" + line + "]");

        types  = new ArrayList<String>();
        allows = new ArrayList<Boolean>();

        method = line.substring(0, sp).trim().toUpperCase();
        prefix = line.substring(sp, colon).trim();

        line = line.substring(colon + 1).trim();

        for (int pos = 0, max = line.length(); pos < max;)
        {
            int    comma  = line.indexOf(',', pos);
            String access = line.substring(pos, (comma != -1) ? comma : max);  // acl should be like type=permission
            String orig   = access;
            String type;
            int    eq;

            access = access.trim();

            eq = access.indexOf('=');
            if (eq == -1)
                throw new IllegalArgumentException("Expected '=' in [" + orig + "]");
            else if (eq + 1 == access.length())
                throw new IllegalArgumentException("Expected TYPE in [" + orig + "]");

            type   = access.substring(0, eq).trim();
            access = access.substring(eq + 1).trim();

            if (type.length() == 0)
                throw new IllegalArgumentException("Empty TYPE in [" + orig + "]");
            else if (access.length() == 0)
                throw new IllegalArgumentException("Empty permission in [" + orig + "]");

            // like:  addAcl("POST", "/admin", "ROOT", "allow");
            addAcl(method, prefix, type, convertPerm(access));

            if (comma == -1)
                break;
            else
                pos = comma + 1;
        }
    }

    /**
     * Adds a parsed combo of verb/prefix/type/permission to the set of ACLs.
     */
    private void addAcl(String method, String prefix, String type, boolean allow)
    {
        String[] methods = method.equals("*") ? VERBS : new String[] {method};
        boolean  allBut  = type.startsWith("~");
        boolean  all     = type.equals("*");

        if (allBut)
            type = type.substring(1);

        for (String verb : methods)
        {
            if (allBut || all)
            {
                for (String ty : allTypes)
                    add1Acl(verb, ty, prefix, allow);
            }
            else
            {
                add1Acl(verb, type, prefix, allow);
            }
        }
    }

    private void add1Acl(String verb, String type, String prefix, boolean allow)
    {
        String    key  = verb + ":" + type;
        List<Acl> acls = access.get(key);
        boolean   add  = true;

        if (acls == null)
        {
            acls = new ArrayList<Acl>();
            access.put(key, acls);
        }

        // we need to overwrite if prefix matches exactly
        for (Acl acl : acls)
        {
            if (prefix.equals(acl.prefix))
            {
                acl.allow = allow;
                add = false;
                break;
            }
        }

        // this is an optimization here:  if allow is false, then don't add since we default to disallowing anyway
        if (add && allow)
            acls.add(new Acl(prefix, allow));
    }

    /**
     * Converts '+' and 'allow' to a true and '-' and 'deny' to false.
     */
    private static Boolean convertPerm(String perm)
    {
        if (perm.equals("+") || perm.equals("allow"))
            return Boolean.TRUE;
        else if (perm.equals("-") || perm.equals("deny"))
            return Boolean.FALSE;
        else
            throw new IllegalArgumentException("Expected one of '[+ - allow deny]' but got " + perm);
    }

    /**
     * debug only
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("[alltypes=");
        sb.append(java.util.Arrays.asList(allTypes));
        sb.append("; ");
        sb.append(access);
        sb.append("]");

        return sb.toString();
    }

    public static void main(String[] args)
    {
        String[]   all = new String[] {"NO_USER", "USER", "ADMIN", "ROOT"};
        AccessSpec as  = new AccessSpec(all, java.util.Arrays.asList(args));

        System.out.println(as.toString());
    }

}
