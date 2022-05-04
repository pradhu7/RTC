package com.apixio.useracct.util;

import java.util.Random;

import com.apixio.useracct.entity.User;

/**
 * Nonce management notes.
 *
 *  1.  before user has set the password for the first time user.nonce is null
 *  2.  when user submits a "forgot password" request user.nonce is set to "sent"
 *  3.  when user clicks on link in received email user.nonce is set to a random number
 *  4.  when user sets the password user.nonce is set to "changed"
 *
 * Therefore, a link is used if the nonce is set to "set".
 */
public class NonceUtil {

    private static final Random  nonceGenerator = new Random(System.currentTimeMillis() % 10000L);

    private static final String LINK_SENT        = "sent";
    private static final String PASSWORD_CHANGED = "changed";

    /**
     * Returns a random number (nonce) to be used when changing a password without
     * requiring the current password for the user.
     */
    public static long createNonce()
    {
        return Math.abs(nonceGenerator.nextLong());
    }

    /**
     * Marks the user nonce with the status that indicates a link to change the password has been
     * sent the user.
     */
    public static void markLinkSent(User user)
    {
        user.setUpdateNonce(LINK_SENT);
    }

    /**
     * Marks the user nonce with the status that indicates the password has been set.
     */
    public static void markPasswordChanged(User user)
    {
        user.setUpdateNonce(PASSWORD_CHANGED);
    }

    /**
     * Returns true iff the state of the user nonce indicates that the most recent changes
     * was that the user changed the password.
     */
    public static boolean passwordWasChanged(User user)
    {
        return PASSWORD_CHANGED.equals(user.getUpdateNonce());
    }

    /**
     * Marks the user nonce with a real nonce value.
     */
    public static void setRealNonce(User user, long nonce)
    {
        user.setUpdateNonce(Long.toString(nonce));
    }

    /**
     * Returns true iff the passed in nonce (from the app level) matches the
     * stored user.nonce value (which must be a parseable number).
     */
    public static boolean nonceMatches(User user, String nonce)
    {
        String userNonce = user.getUpdateNonce();

        // userNonce is one of:  null, LINK_SENT, PASSWORD_CHANGED, or a parseable integer value
        // return true iff it's parseable and equal to nonce

        try
        {
            long tmp = Long.parseLong(userNonce);

            if (tmp >= 0L)  // nonces are created with Math.abs(random)
                return userNonce.equals(nonce);
        }
        catch (NumberFormatException x)
        {
        }

        return false;
    }

}
