package com.apixio.useracct.entity;

/**
 * AccountState defines the different states of accounts in the system.
 *
 * The string form of these enum values are used in the main system .yaml configuration
 * file so any changes made here must be reflected in the .yaml file also
 */
public enum AccountState {
    NO_USER,         // special marker to indicate no user is known
    NEW,             // user object created but email address not yet verified
    ACTIVE,          // email address verified and user can log in
    CLOSED,          // permanently closed so user can't log in
    DISABLED,        // temporarily disabled so user can't log in
    AUTO_LOCKOUT,    // too many failed attempts to log in
    EXPIRED_PASSWORD // (temporary state) user must change password
}
