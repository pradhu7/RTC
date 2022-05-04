package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.apixio.XUUID;
import com.apixio.restbase.dao.BaseEntities;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.User;
import com.apixio.datasource.redis.MapResponse;
import com.apixio.datasource.redis.RedisPipeline;

/**
 * Users defines the persisted level operations on User entities.
 */
public class Users extends BaseEntities<User> {

    /**
     * We need to be able to look up an account (user) by canonicalized email address.
     * This is done by putting all user emails into a single redis hash
     */
    private static final String INDEX_BYEMAIL = "users-x-byemail";

    /**
     * These are the redis keys for keeping track of email address -> userID
     *
     *  userLookupByEmailKey:  the keyname to the redis hash whose elements are the email address
     *  userLookupAllKey:      the keyname to the redis hash whose elements are the organization UUID
     */
    protected String userLookupByEmailKey;
    protected String userLookupAllKey;

    /**
     * Keep track of all users
     */
    private static final String INDEX_ALL    = "users-x-all";

    /**
     *
     */
    public Users(DaoBase seed)
    {
        super(seed);

        // create keyname here as that can't be done until after object is fully initialized.
        userLookupByEmailKey = super.makeKey(INDEX_BYEMAIL);
        userLookupAllKey     = super.makeKey(INDEX_ALL);
    }

    /**
     * Return true if the passed in password hashes to the stored value.
     */
    public boolean checkPassword(User user, String password)
    {
        return (user != null) && BCrypt.checkpw(password, user.getHashedPassword());
    }

    /**
     * Return a list of all Users, regardless of account status.
     */
    public List<User> getAllUsers()
    {
        List<String> ids   = redisOps.lrange(userLookupAllKey, 0, -1);
        List<XUUID>  users = new ArrayList<>(ids.size());

        for (String id : ids)
            users.add(XUUID.fromString(id, User.OBJTYPE));

        return getUsers(users);
    }

    /**
     * Return a list of the given Users, regardless of account status.
     */
    public List<User> getUsers(Collection<XUUID> userIDs)
    {
        List<User>         users = new ArrayList<User>(userIDs.size());
        List<MapResponse>  maps  = new ArrayList<>();
        RedisPipeline      rp    = redisOps.beginPipeline();

        try
        {
            for (XUUID id : userIDs)
                maps.add(rp.hgetAll(super.makeKey(id.toString())));

            rp.waitForAll();

            for (MapResponse mr : maps)
            {
                ParamSet ps = super.fromMapResponse(mr);

                if (ps == null)
                    System.out.println("SFM getUsers failed to restore ParamSet for key " + mr.key());
                else
                    users.add(new User(super.fromMapResponse(mr)));
            }
        }
        finally
        {
            rp.endPipeline();
        }

        return users;
    }

    /**
     * Look up a user by a non-canonicalized email address
     */
    public User findUserByEmail(String emailAddr)
    {
        XUUID userID = XUUID.fromString(redisOps.hget(userLookupByEmailKey, emailAddr), User.OBJTYPE);

        if (userID != null)
            return findUserByID(userID);
        else
            return null;
    }

    /**
     * Look up a userID by a non-canonicalized email address
     */
    public XUUID findUserIDByEmail(String emailAddr)
    {
        return XUUID.fromString(redisOps.hget(userLookupByEmailKey, emailAddr), User.OBJTYPE);
    }

    /**
     * Looks for a User instance with the given ID in Redis and if found returns
     * a restored User instance.  Null is returned if the ID is not found.
     *
     * Only scalar data fields are filled in (no counters or lists).
     */
    public User findUserByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new User(fields);
        else
            return null;
    }

}
