package com.apixio.nassembly.assemblygraph;

import com.apixio.dao.utility.DaoServices;
import com.apixio.datasource.utility.LockUtility;

public class AssemblyGraphLock
{
    private String      lockPrefix;
    private LockUtility lockUtility;

    public AssemblyGraphLock(DaoServices daoServices)
    {
        lockPrefix = daoServices.getRedisKeyPrefix() +  "assemblyFramework-data2.0";
        lockUtility = new LockUtility(daoServices.getRedisOps(), lockPrefix);
    }

    public String getLock(String graphNodeType, String pdsId, String dataTypeName, long period, int maxAttempts)
    {
        return lockUtility.getLock(makeKey(graphNodeType, pdsId, dataTypeName), period, maxAttempts);
    }

    public boolean isLocked(String graphNodeType, String pdsId, String dataTypeName, String lock)
    {
        return lockUtility.isLocked(makeKey(graphNodeType, pdsId, dataTypeName), lock);
    }

    public boolean anyLock(String graphNodeType, String pdsId, String dataTypeName)
    {
        return lockUtility.anyLock(makeKey(graphNodeType, pdsId, dataTypeName));
    }

    public void unlock(String graphNodeType, String pdsId, String dataTypeName, String lock)
    {
        lockUtility.unlock(makeKey(graphNodeType, pdsId, dataTypeName), lock);
    }

    public boolean refreshLock(String graphNodeType, String pdsId, String dataTypeName, String lock, long period, int maxAttempts)
    {
        return lockUtility.refreshLock(makeKey(graphNodeType, pdsId, dataTypeName), lock, period, maxAttempts);
    }

    private String makeKey(String graphNodeType, String pdsId, String dataTypeName)
    {
        return String.join("-", graphNodeType, pdsId, dataTypeName);
    }
}














