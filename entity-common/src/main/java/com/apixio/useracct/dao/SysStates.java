package com.apixio.useracct.dao;

import com.apixio.XUUID;
import com.apixio.restbase.dao.BaseEntities;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.SysState;

/**
 * SysStates manages the persistence of the SysState singleton.  Notably this is
 * where the forced ID for the SysState is defined.
 */
public final class SysStates extends BaseEntities<SysState> {

    private final static XUUID SYSSTATE_ID = XUUID.fromString(SysState.OBJTYPE + "_" + "00000000-0000-0000-0000-000000000001");  // HACK

    /**
     * Constructor
     */
    public SysStates(DaoBase seed)
    {
        super(seed);
    }

    /**
     * Restores, if it exists, the singleton SysState.  The singleton is created (blank)
     * if nothing in Redis with the singleton ID exists.
     */
    public SysState getSysState()
    {
        ParamSet fields = findByID(SYSSTATE_ID);

        if (fields != null)
        {
            return new SysState(fields);
        }
        else
        {
            // hack create it with the desired ID, then save
            SysState ss = new SysState(SYSSTATE_ID);

            update(ss);

            return ss;
        }
    }

}
