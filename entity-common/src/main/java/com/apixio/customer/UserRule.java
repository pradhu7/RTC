package com.apixio.customer;

import com.apixio.XUUID;

/**
 * UserRule captures the entirety of the user-to-rule association/assignment relationship.
 */
public class UserRule {
    public XUUID  ownerID;
    public XUUID  ruleID;
    public XUUID  userID;
    public int    priority;
    public String state;

    public UserRule()
    {
    }

    public UserRule(XUUID ownerID, XUUID ruleID, XUUID userID, int priority, String state)
    {
        this.ownerID    = ownerID;
        this.ruleID     = ruleID;
        this.userID     = userID;
        this.priority   = priority;
        this.state      = state;
    }

    public String toString()
    {
        return ("[UserRule owner=" + ownerID +
                "; ruleID=" + ruleID +
                "; userID=" + userID +
                "; priority=" + priority +
                "; state=" + state +
                "]");
    }
}

