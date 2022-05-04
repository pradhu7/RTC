package com.apixio.contextlogger;

import java.util.Map;

/**
 * Implementation of the 'Context based logging' user API.
 * Creates an ApxLogModel object and delegates all the calls
 * that populate a log-entry to it.
 * Once the try block exits, we call the close method.
 */

public class CtxLogEventContext implements ICtxLogEventContext {
    private final CtxLogModel model;
    private final ICtxLoggerAdapter logger;
    private final CtxLogLevel level;

    private CtxLogEventContext(ICtxLoggerAdapter logger, CtxLogLevel level) {
        this.model = CtxLogModel.build();
        this.logger = logger;
        this.level = level;
    }

    public static CtxLogEventContext get(ICtxLoggerAdapter logger, CtxLogLevel level) {
        return new CtxLogEventContext(logger, level);
    }

    /**
     * Satisfies the Closeable interface.
     * As the resource closes we push the content of the log entry to
     * the Logger.
     */
    @Override
    public void close() {
        if (model.hasError()) {
            logger.error(model.emit());
            return;
        }
        switch (level) {
            case LOG_INFO:
                logger.info(model.emit());
                break;
            case LOG_EVENT:
                logger.event(model.emit());
                break;
            case LOG_WARNING:
                logger.warn(model.emit());
                break;
            case LOG_ERROR:
                logger.error(model.emit());
                break;
            case LOG_DEBUG:
                logger.debug(model.emit());
                break;
            case LOG_FATAL:
                logger.fatal(model.emit());
                break;
            default:
                model.pushKeyValue("fatal.error", "Invalid log state");
                logger.fatal(model.emit());
                break;
        }
    }

    // [--------- Properties ------------------------------]
    @Override
    public void setAppName(String value) {
        model.setAppName(value);
    }

    @Override
    public void setAppVersion(String value) {
        model.setAppVersion(value);
    }

    @Override
    public void setException(Throwable e) {
        model.setException(e);
    }

    @Override
    public void setLogLevel(CtxLogLevel level) {
        model.setLogLevel(level);
    }

    // [--------- Raw Key/Value setting -------------------]
    @Override
    public void pushKeyValue(String name, String value) {
        model.pushKeyValue(name, value);
    }

    @Override
    public void pushKeyValue(String name, byte value) {
        model.pushKeyValue(name, value);
    }

    @Override
    public void pushKeyValue(String name, short value) {
        model.pushKeyValue(name, value);
    }

    @Override
    public void pushKeyValue(String name, int value) {
        model.pushKeyValue(name, value);
    }

    @Override
    public void pushKeyValue(String name, long value) {
        model.pushKeyValue(name, value);
    }

    @Override
    public void pushKeyValue(String name, float value) {
        model.pushKeyValue(name, value);
    }

    @Override
    public void pushKeyValue(String name, double value) {
        model.pushKeyValue(name, value);
    }

    // [--------- App Metrics setting ---------------------]
    @Override
    public void pushAppMetric(String name, String value) {
        model.pushAppMetric(name, value);
    }

    @Override
    public void pushAppMetric(String name, byte value) {
        model.pushAppMetric(name, value);
    }

    @Override
    public void pushAppMetric(String name, short value) {
        model.pushAppMetric(name, value);
    }

    @Override
    public void pushAppMetric(String name, int value) {
        model.pushAppMetric(name, value);
    }

    @Override
    public void pushAppMetric(String name, long value) {
        model.pushAppMetric(name, value);
    }

    @Override
    public void pushAppMetric(String name, float value) {
        model.pushAppMetric(name, value);
    }

    @Override
    public void pushAppMetric(String name, double value) {
        model.pushAppMetric(name, value);
    }

    // [--------- User Info setting  ----------------------]
    @Override
    public void pushUserInfo(String name, String value) {
        model.pushUserInfo(name, value);
    }

    @Override
    public void pushUserInfo(String name, byte value) {
        model.pushUserInfo(name, value);
    }

    @Override
    public void pushUserInfo(String name, short value) {
        model.pushUserInfo(name, value);
    }

    @Override
    public void pushUserInfo(String name, int value) {
        model.pushUserInfo(name, value);
    }

    @Override
    public void pushUserInfo(String name, long value) {
        model.pushUserInfo(name, value);
    }

    @Override
    public void pushUserInfo(String name, float value) {
        model.pushUserInfo(name, value);
    }

    @Override
    public void pushUserInfo(String name, double value) {
        model.pushUserInfo(name, value);
    }

    @Override
    public void pushAppMetrics(Map<String, String> appInfo) {
        model.pushAppMetrics(appInfo);
    }

    @Override
    public void pushUserInfos(Map<String, String> userInfo) {
        model.pushUserInfos(userInfo);
    }

    // [--------- Context logging primitives --------------]
    @Override
    public boolean hasError() {
        return model.hasError();
    }

    // [--------- Used during development -------------------]
    public String getReleaseVersion() { return model.getReleaseVersion(); }

}
