package com.apixio.aclsys.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.restbase.dao.BaseEntities;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.aclsys.entity.Operation;

/**
 * Operations defines the persisted level operations on Operation entities.
 */
public class Operations extends BaseEntities {

    /**
     * We need to be able to look up a Operation by name.
     * This is done by putting all names into a single redis hash
     */
    private static final String INDEX_BYNAME = "oper-x-byname";

    /**
     * Keep track of all Operations
     */
    private static final String INDEX_ALL    = "oper-x-all";

    /**
     * Keep track of modification time
     */
    private static final String MODIFICATION_TIME = "oper-last-modified";

    /**
     * These are the redis keys for keeping track of email address -> userID
     *
     *  operationLookupByName:  the keyname to the redis hash whose elements are the names
     *  operationLookupAllKey:  the keyname to the redis hash whose elements are all the operation IDs
     */
    protected String operationLookupByName;
    protected String operationLookupAllKey;
    private   String operationMod;

    /**
     *
     */
    public Operations(DaoBase seed)
    {
        super(seed);

        // create keyname here as that can't be done until after object is fully initialized.
        operationLookupByName = super.makeKey(INDEX_BYNAME);
        operationLookupAllKey = super.makeKey(INDEX_ALL);
        operationMod          = super.makeKey(MODIFICATION_TIME);
    }

    /**
     * Returns the epoch milliseconds that the last modification was made to Redis
     * for any Operation object (create/update/delete).
     */
    public long getLastModificationTime()
    {
        try
        {
            String lm = redisOps.get(operationMod);

            if (lm != null)
                return Long.parseLong(lm);
            else
                return 0L;
        }
        catch (Exception x)
        {
            return 0L;
        }
    }

    /**
     * Looks up a operation by name and reads from Redis and returns it, if it exists.
     */
    public Operation findOperationByName(String name)
    {
        XUUID operationID = XUUID.fromString(redisOps.hget(operationLookupByName, name), Operation.OBJTYPE);

        if (operationID != null)
            return findOperationByID(operationID);
        else
            return null;
    }

    /**
     * Reads and returns a list of all Operations persisted in Redis.
     */
    public List<Operation> getAllOperations()
    {
        List<Operation> all = new ArrayList<Operation>();

        for (String id : redisOps.lrange(operationLookupAllKey, 0, -1))
            all.add(findOperationByID(XUUID.fromString(id, Operation.OBJTYPE)));

        return all;
    }

    /**
     * Looks for a Operation instance with the given ID in Redis and if found returns
     * a restored Operation instance.  Null is returned if the ID is not found.
     *
     * Only scalar data fields are filled in (no counters or lists).
     */
    public Operation findOperationByID(XUUID id)
    {
        ParamSet fields = findByID(id);

        if (fields != null)
            return new Operation(fields);
        else
            return null;
    }

    /**
     * Returns the epoch milliseconds that the last modification was made to Redis
     * for any Operation object (create/update/delete).
     */
    protected void setLastModificationTime(
        long time
        )
    {
        redisOps.set(operationMod, Long.toString(time));
    }

}
