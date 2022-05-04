package com.apixio.aclsys.buslog;

/**
 * A Permission is a tuple that indicates that an Operation can be done to
 * an Object.  The Subject that can actually do it is not part of the structure
 * and must be remembered elsewhere.
 */
public class Permission {

    public String   operation;
    public String   object;

    public Permission(String op, String object)
    {
        this.operation = op;
        this.object    = object;
    }

    public String toString()
    {
        return "[perm op=" + operation + "; obj=" + object + "]";
    }
}
