package com.apixio.contextlogger;

/**
 * Apixio log level type to replace constants from any specific logging library.
 * Will avoid leakage of log specific constants outside the library code.
 */

public enum CtxLogLevel {
    LOG_INFO,
    LOG_EVENT,
    LOG_WARNING,
    LOG_ERROR,
    LOG_DEBUG,
    LOG_FATAL
}
