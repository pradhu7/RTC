package com.apixio.useracct.entity;

/**
 * VerifyType defines /why/ a VerifyLink is sent out to a user.
 */
public enum VerifyType {
    NEW_ACCOUNT,         // new accounts require email address verification
    NEW_EMAILADDR,       // user can't use email address until it's been verified
    RESET_PASSWORD,      // user has requested that the password be reset
    UNLOCK_ACCOUNT       // user has requested to unlock account
}
