package com.apixio.useracct.dao;

import com.apixio.XUUID;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.dao.BaseEntities;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.config.VerifyLinkConfig;
import com.apixio.useracct.entity.VerifyLink;
import com.apixio.useracct.entity.VerifyType;

/**
 * Defines the persistence-level operations on verification links.
 */
public class VerifyLinks extends BaseEntities {

    private int linkTimeout;  // required; relative to when link is created

    /**
     * VerifyLinks requires that we talk with redis so we need that info.
     */
    public VerifyLinks(DaoBase seed, VerifyLinkConfig vlConfig)
    {
        super(seed);

        linkTimeout = vlConfig.getLinkTimeout();
    }

    /**
     * Create a verification link used when creating a new account.
     */
    public VerifyLink createNewAccountLink(XUUID userID)
    {
        VerifyLink vl = new VerifyLink(VerifyType.NEW_ACCOUNT, calcInvalidAfter(linkTimeout));

        vl.setUserID(userID);
        update(vl);

        return vl;
    }

    /**
     * Create a reset password link.
     */
    public VerifyLink createResetPasswordLink(XUUID userID, int timeout)
    {
        VerifyLink vl = new VerifyLink(VerifyType.RESET_PASSWORD, calcInvalidAfter(timeout));

        vl.setUserID(userID);
        update(vl);

        return vl;
    }

    /**
     * Create an unlock account link.
     */
    public VerifyLink createUnlockAccountLink(XUUID userID, int timeout)
    {
        String linkKey = makeKey(userID) + "-" + VerifyLink.OBJTYPE + "-" + VerifyType.UNLOCK_ACCOUNT;

        // check if an old verify link exists
        String lastCreated = redisOps.get(linkKey);
        if (lastCreated != null) {
            VerifyLink vl = findVerifyLinkByID(XUUID.fromString(lastCreated, VerifyLink.OBJTYPE));
            if (vl != null)
                return vl;
        }

        // If not found, create a new verify link
        VerifyLink vl = new VerifyLink(VerifyType.UNLOCK_ACCOUNT, calcInvalidAfter(timeout));

        vl.setUserID(userID);
        update(vl);

        // Link from user to verify link
        redisOps.set(linkKey, vl.getID().toString());
        redisOps.expire(linkKey, timeout);

        return vl;
    }

    /**
     * Find the VerifyLink entity by its ID and return it, or null if it can't
     * be found (likely due to expiration).
     */
    public VerifyLink findVerifyLinkByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new VerifyLink(fields);
        else
            return null;
    }

    /**
     * Intercept the general entity update method in order to re-set the TTL for
     * the entity in Redis.  If this isn't done then we lose the original TTL.
     */
    public void update(BaseEntity base)
    {
        VerifyLink vl = (VerifyLink) base;

        super.update(vl);

        // we need to re-expire() as an HMSET will clear the TTL

        /**
         *  VerifyLinks are created with a time-relative expiration; invalidAfter is set from
         *    that and expire() is called during update with the (relative) value (as required
         *    generally from redis) calculated as invalidAfter-now.
         */

        long invalidAfter = vl.getInvalidAfter();             // in epoch seconds
        long nowSec       = System.currentTimeMillis() / 1000L;
        int  expire;

        expire = (int) (invalidAfter - nowSec);

        redisOps.expire(makeKey(vl.getID()), expire);
    }

    public void delete(VerifyLink vl) {
        super.delete(vl);
    }

    /**
     * If 0, then return 0 (which means no max TTL); otherwise return the absolute
     * epoch time in seconds after which the link should be invalid.
     */
    private long calcInvalidAfter(int fromNow)
    {
        if (fromNow > 0)
            return (System.currentTimeMillis() / 1000L) + fromNow;
        else
            return 0L;
    }

}
