package com.apixio.useracct.dao;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.apixio.restbase.DaoBase;
import com.apixio.useracct.entity.OldRole;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.User;
import com.apixio.useracct.email.CanonicalEmail;

/**
 * Users defines the persisted level operations on User entities.
 */
public class PrivUsers extends Users {

    public static final String OTP_LOOKUP_BY_USERID = "-OTP-X-BY-UID";

    /**
     *
     */
    public PrivUsers(DaoBase seed)
    {
        super(seed);
    }

    /**
     * Create a new instance of a User
     */
    public User createUser(CanonicalEmail emailAddr, AccountState state, OldRole role)
    {
        User user = findUserByEmail(emailAddr.toString());

        if (user != null)
        {
            throw new IllegalArgumentException("email address [" + emailAddr.toString() + "] already used:  new account cannot be created");
        }
        else
        {
            user = new User(emailAddr, state, role);

            addToIndexes(user);

            update(user);
        }

        return user;
    }

    public void setUserPassword(User user, String newPass)
    {
        user.setHashedPassword(BCrypt.hashpw(newPass, BCrypt.gensalt()));
    }

    public void addOldPassword(User user, String hash, long changedMs)
    {
        user.addOldPassword(hash, changedMs);
    }

    public void deleteUser(User user)
    {
        removeFromIndexes(user);

        super.delete(user);
    }

    /**
     * Adds the user to the index-by-email and -all lookups so we can find that User by its
     * email address and enumerate all users.
     */
    private void addToIndexes(User user)
    {
        redisOps.rpush(userLookupAllKey,    user.getID().toString());
        redisOps.hset(userLookupByEmailKey, user.getEmailAddress().toString(), user.getID().toString());
    }

    /**
     * Removes the user from the index-by-email and -all lookups (undoes addToIndexes)
     */
    private void removeFromIndexes(User user)
    {
        redisOps.lrem(userLookupAllKey,     0, user.getID().toString());
        redisOps.hdel(userLookupByEmailKey, user.getEmailAddress().toString());
    }

    /**
     * add OTP by user
     */
    public void addOTPByUserWithExpiry(User user, String otp, int ttls)
    {
        String otpUserKey = getOTPUserKey(user);

        redisOps.set(otpUserKey, otp);
        redisOps.expire(otpUserKey, ttls);
    }

    /**
     * find OTP by user
     */
    public String findOTPByUser(User user)
    {
        String otpUserKey = getOTPUserKey(user);
        return redisOps.get(otpUserKey);
    }

    /**
     * delete OTP by user
     */
    public void deleteOTPByUser(User user)
    {
        String otpUserKey = getOTPUserKey(user);
        redisOps.del(otpUserKey);
    }

    private String getOTPUserKey(User user)
    {
        return makeKey(user.getID()) + OTP_LOOKUP_BY_USERID;
    }

    /**
     *
     */
    public static void main(String[] args)
    {
        // DO NOT CHANGE/DELETE THIS AT ALL as it's used by the bootstrap script to get the bcrypt value!

        for (String password : args)
            System.out.println(password + " -> " + BCrypt.hashpw(password, BCrypt.gensalt()) + "");
    }
}
