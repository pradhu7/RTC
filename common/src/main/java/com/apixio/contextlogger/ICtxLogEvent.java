package com.apixio.contextlogger;

/**
 * Interface representing the general services to populate a log entry and
 * the logging primitives like info, warn, etc.
 */
public interface ICtxLogEvent extends ICtxLogBase {
    void error();
    void debug();
    void warn();
    void event();
    void info();
    void fatal();
    void startClock();
}


