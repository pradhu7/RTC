package com.apixio.sdk;

import com.apixio.dao.utility.DaoServices;

/**
 */
public interface FxExecEnvironment extends FxEnvironment
{

    /**
     * This is the main mechanism for retrieving data needed for f(x) evaluation
     */
    public DaoServices getDaoServices();

    /**
     * Only function execution environments (e.g., lambda ECC) should need to get the
     * FxExecutor
     */
    public FxExecutor getFxExecutor();

}
