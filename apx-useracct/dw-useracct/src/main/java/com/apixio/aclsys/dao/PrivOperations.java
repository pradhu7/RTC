package com.apixio.aclsys.dao;

import com.apixio.restbase.DaoBase;
import com.apixio.aclsys.entity.Operation;

/**
 * Operations defines the persisted level operations on Operation entities.
 */
public class PrivOperations extends Operations {

    /**
     *
     */
    public PrivOperations(DaoBase seed)
    {
        super(seed);
    }

    /**
     * Create a new instance of a Operation.
     */
    public Operation createOperation(String name, String description)
    {
        return createOperation(name, description, null);
    }

    public Operation createOperation(String name, String description, String appliesTo)
    {
        Operation operation = findOperationByName(name);

        if (operation != null)
        {
            throw new IllegalArgumentException("Operation name [" + name + "] already used:  new operation cannot be created");
        }
        else
        {
            operation = new Operation(name);
            operation.setDescription(description);
            operation.setAppliesTo(appliesTo);

            addToIndexes(operation);

            update(operation);
        }

        return operation;
    }

    /**
     * Deletes the Operation in Redis and updates the modification time to
     * all JVMs can reload lists.
     */
    public void deleteOperation(String name)
    {
        Operation op = findOperationByName(name);

        if (op == null)
            throw new IllegalArgumentException("Unknown Operation name [" + name + "]");

        super.delete(op);

        removeFromIndexes(op);

        setLastModificationTime(System.currentTimeMillis());
    }

    /**
     * Adds the operation to the indexed lookups so we can find all operations.
     */
    private void addToIndexes(Operation operation)
    {
        String operationID = operation.getID().toString();

        redisOps.hset(operationLookupByName, operation.getName(), operationID);
        redisOps.rpush(operationLookupAllKey, operationID);
    }

    /**
     * Removes the operation from the indexed lookups.
     */
    private void removeFromIndexes(Operation operation)
    {
        String operationID = operation.getID().toString();

        redisOps.hdel(operationLookupByName, operation.getName());
        redisOps.lrem(operationLookupAllKey, 1, operationID);
    }

}
