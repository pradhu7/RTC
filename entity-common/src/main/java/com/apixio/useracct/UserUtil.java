package com.apixio.useracct;

import com.apixio.useracct.entity.User;

/**
 * UserUtil exposes a few useful, static method only, utility functions for the User Account
 * service code.
 */
public class UserUtil {

    private static final ThreadLocal<User> tlUser = new ThreadLocal<User>();

    public static void setCachedUser(User user)
    {
        tlUser.set(user);
    }

    public static User getCachedUser()
    {
        return tlUser.get();
    }
    
}

