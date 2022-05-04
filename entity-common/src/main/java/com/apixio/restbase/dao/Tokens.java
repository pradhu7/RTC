package com.apixio.restbase.dao;

import java.util.Map;

import com.apixio.XUUID;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.entity.TokenType;
import com.apixio.restbase.DaoBase;

/**
 * Tokens provides data access services to entities of type Token.
 *
 * Note that Tokens is also used as the prototypical (seed) instance for other
 * DAOs, since it must always exist for all API-based services (well, anything
 * that deals with internal/external tokens...).
 *
 * Tokens are stored as HASH structures in Redis, with the token ID being the
 * Redis key and the HASH containing the field names and values.
 *
 * External tokens have a use-count associated with them in order to try to
 * detect intrusions, with the thought being if an attacker compromises the
 * edge server and snoops an external token and then uses it to get internal
 * tokens, if the attacker doesn't know to delete the internal token properly,
 * then the use count will continue to go higher and higher which could be a
 * signal of an intruder.
 *
 * Because of the possibility/likelihood of parallel requests from the browser
 * the use count can't be the read/incr/write model and so it must be a separate
 * redis key that does allow atomic increment.
 *
 * Tokens can be looked up by the owning UserID as it might be necessary to
 * delete tokens for a User (for security reasons).  This index from UserID
 * to TokenID is kept as a Redis HASH structure, with the key being the UserID
 * and the hash field being the TokenID and the hash value being merely an
 * existence marker.  This structure allows a User to have an arbitrary number
 * of tokens (not possible if we use a single Redis key that doesn't contain
 * UserID) and it allows us to avoid some list management issues (like checking
 * if a token is there before adding/setting it).
 *
 * The by-user keys are expiring and their expiration times are updated
 * whenever a token expiration is updated so that the user key should be
 * automatically deleted when the last token for that user is automaticall
 * deleted.
 *
 * Note that this is an imperfect list as the token keys themselves are expiring
 * and can disappear without a chance to get deleted from the by-user list.
 */
public final class Tokens extends BaseEntities<Token>
{
    /**
     * Constants for redis key names
     */
    private final static String K_USECOUNT = "tknuse:";     // prepended to TokenID; atomic incr/decr int value
    private final static String K_XBYUSER  = "tkn-xuser:";  // prepended to UserID; LIST structure

    private int externalActivityTimeout;  // required; relative to last activity time
    private int externalMaxTTL;           // optional; 0=> no max; relative to token creation
    private int internalMaxTTL;           // relative to token creation

    /**
     * Tokens requires that we talk with redis so we need that info.  Note that
     * once we have a Tokens DAO instance we can use that to seed other Persistable-
     * derived instances.
     */
    public Tokens(DaoBase seed, ConfigSet tokenConfig)
    {
        super(seed);

        // tokenConfig will be null if we're in a normal service
        if (tokenConfig != null)
        {
            externalActivityTimeout = tokenConfig.getInteger(MicroserviceConfig.TOK_ACTIVITYTIMEOUT);
            externalMaxTTL          = tokenConfig.getInteger(MicroserviceConfig.TOK_EXTERNALMAXTTL);
            internalMaxTTL          = tokenConfig.getInteger(MicroserviceConfig.TOK_INTERNALMAXTTL);
        }
    }

    /**
     * Create a new instance of a Token
     */
    public Token createExternalToken()
    {
        Token token = new Token(TokenType.EXTERNAL, calcInvalidAfter(externalMaxTTL));

        update(token);

        return token;
    }

    /**
     * Create a new instance of a Token
     */
    public Token createInternalToken(XUUID externalID)
    {
        return createInternalToken(externalID, null, internalMaxTTL);
    }
    public Token createInternalToken(XUUID userID, int invalidAfter)
    {
        return createInternalToken(null, userID, invalidAfter);
    }

    /**
     * Delete tokens for a user, optionally not deleting the given one.
     */
    public void deleteTokensForUser(XUUID userID, XUUID keepToken)
    {
        String              userKey = makeKey(K_XBYUSER + userID.toString());
        Map<String, String> all     = redisOps.hgetAll(userKey);

        if (all != null)
        {
            for (Map.Entry<String, String> entry : all.entrySet())
            {
                String tokenStr = entry.getKey();

                if ((keepToken == null) || !tokenStr.equals(keepToken.toString()))
                {
                    Token token = findTokenByID(XUUID.fromString(tokenStr));

                    if (token != null)   // possibly null due to expiring tokens
                        delete(token);
                }
            }
        }
    }

    /**
     * Accepts a "now"-relative number of seconds and returns the absolute time, or 0L
     * if fromNow is not positive.
     */
    private long calcInvalidAfter(int fromNow)
    {
        if (fromNow > 0)
            return (System.currentTimeMillis() / 1000L) + fromNow;
        else
            return 0L;
    }

    /**
     * Creates an internal token that links to the given external token ID and is for
     * the given UserID and which is invalid after the given number of seconds.
     */
    private Token createInternalToken(XUUID externalID, XUUID userID, int invalidAfter)
    {
        //!! probably should check User status here to make sure it's not
        // been disabled/closed

        Token xToken = (externalID != null) ? findTokenByID(externalID) : null;
        Token token  = new Token(TokenType.INTERNAL,
                                 calcInvalidAfter((invalidAfter > 0) ? invalidAfter : internalMaxTTL));

        // Clone required fields
        if (externalID != null)
            token.setExternalToken(externalID);

        if (xToken != null)
        {
            userID = xToken.getUserID();
            token.setAuthState(xToken.getAuthState());
        }

        token.setUserID(userID);

        update(token);

        redisOps.expire(makeKey(token.getID()), internalMaxTTL);

        return token;
    }

    /**
     * Looks for a User instance with the given ID in Redis and if found returns
     * a restored User instance.  Null is returned if the ID is not found.
     *
     * Only scalar data fields are filled in (no counters or lists).
     */
    public Token findTokenByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new Token(fields);
        else
            return null;
    }

    /**
     * Intercepts the generic update() method in order to set the TTL on the redis
     * key.
     */
    @Override
    public void update(Token token)
    {
        // This must be done first as it destroys the TTL
        super.update(token);

        // we need to re-expire() as an HMSET will clear the TTL

        /**
         * Expiration Cases:
         *
         *  internal token:  created with a time-relative expiration; invalidAfter is set from
         *    that and expire() is called during update with the (relative) value (as required
         *    generally from redis) calculated as invalidAfter-now.
         *
         *  external token: created with optional time-relative expiration; invalidAfter is
         *    set if the optional value is there.  expire() is called during update with the
         *    (relative) value (as required generally from redis) calculated as
         *    min(invalidAfter-now, now+inactivityTimeout).
         */

        long   invalidAfter = token.getInvalidAfter();             // in epoch seconds
        long   nowSec       = System.currentTimeMillis() / 1000L;
        int    expire;
        XUUID  userID;

        if (token.isExternal())
        {
            if ((expire = token.getInactivityOverride()) == 0)
                expire = externalActivityTimeout;

            if (invalidAfter > 0L)
                expire = Math.min(expire, (int) (invalidAfter - nowSec));
        }
        else
        {
            expire = (int) (invalidAfter - nowSec);
        }

        redisOps.expire(makeKey(token.getID()), expire);

        // now we make sure the by-user key/hash/field exists and has the
        // right timeout
        if ((userID = token.getUserID()) != null)
        {
            String userKey = makeKey(K_XBYUSER + userID.toString());

            redisOps.hset(userKey, token.getID().toString(), "x");
            redisOps.expire(userKey, expire);
        }
    }

    /**
     * Intercepts the delete of the token entity so it can clean up the
     * by-user index.
     */
    @Override
    public void delete(Token token)
    {
        XUUID userID;

        super.delete(token);

        if ((userID = token.getUserID()) != null)
        {
            String userKey = makeKey(K_XBYUSER + userID.toString());

            redisOps.hdel(userKey, token.getID().toString());
        }
    }

    /**
     * Bumps the "use count" on an external token ID by the given number.  A TTL is
     * set on the key so that we don't have to clean up the separate key we use to
     * track the use count.
     */
    public void adjustUseCount(Token token, int count)
    {
        String key = makeKey(K_USECOUNT + token.getID());

        redisOps.incrDirect(key, count);

        if (token.isExternal())
        {
            long invalidAfter = token.getInvalidAfter();             // in epoch seconds
            long nowSec       = System.currentTimeMillis() / 1000L;
            int  expire       = externalActivityTimeout;

            if (invalidAfter > 0L)
                expire = Math.min(expire, (int) (invalidAfter - nowSec));

            redisOps.expire(key, expire);

            if (count > 0)
            {
                try
                {
                    int curCount = Integer.parseInt(redisOps.get(key));

                    if (curCount > 2)
                    {
                        //!! suspicious activity!! report it
                        System.out.println("SFM*************** count for token " + token.getID() + " > 2.  possible intruder");
                    }
                }
                catch (NumberFormatException x)
                {
                    // weird!!
                    x.printStackTrace();
                }
            }
        }
    }
}
