package com.apixio.contextlogger;

public interface ICtxLoggerAdapter {
    void error(String msg);
    void debug(String msg);
    void warn(String msg);
    void event(String msg);
    void info(String msg);
    void fatal(String msg);
}
