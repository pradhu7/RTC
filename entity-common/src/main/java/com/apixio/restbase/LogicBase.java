package com.apixio.restbase;

import com.apixio.logger.EventLogger;

/**
 * BaseLogic provides centralized services location for all business logic code.
 */
public class LogicBase<T extends PersistenceServices>
{
    protected T sysServices;
    protected T services;

    protected LogicBase(T sysServices)
    {
        this.sysServices = sysServices;
        this.services    = sysServices;
    }

    public void postInit()
    {
        // let subclasses do something interesting.
    }

    protected EventLogger getEventLogger(String loggerName)
    {
        return sysServices.getEventLogger(loggerName);
    }
}
