package com.apixio.contextlogger;

/**
 * Interface representing the general services to populate a log entry and
 * the single call used in context-based logging.
 */
interface ICtxLogEventContext extends ICtxLogBase, AutoCloseable {
    boolean hasError();
}