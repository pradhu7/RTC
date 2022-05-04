package com.apixio.contextlogger;

import java.util.Map;


/**
 * Implementation of standard logging user API.
 * Contains an ApxLogModel object and delegates all the calls
 * to populate a log entry to it.
 */

public class CtxLogEvent implements ICtxLogEvent {
    private final CtxLogModel model;
    private final ICtxLoggerAdapter logger;

    private CtxLogEvent(ICtxLoggerAdapter logger) {
        this.model = CtxLogModel.build();   // Sets the container for the LogEntry.
        this.logger = logger;
    }

    public static CtxLogEvent get(ICtxLoggerAdapter logger) {
        return new CtxLogEvent(logger);
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
        this.error();
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

    @Override
    public void pushAppMetric(String name, String value) {
        model.pushAppMetric(name, value);
    }

    // [--------- App Metrics setting ---------------------]
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

    // [--------- Logging primitives ----------------------]
    @Override
    public void error() {
        logger.error(model.emit());
    }

    @Override
    public void debug() {
        logger.debug(model.emit());
    }

    @Override
    public void info() {
        logger.info(model.emit());
    }

    @Override
    public void warn() {
        logger.warn(model.emit());
    }

    @Override
    public void fatal() {
        logger.fatal(model.emit());
    }

    @Override
    public void event() {
        logger.event(model.emit());
    }

    @Override
    public void startClock() {
        model.startClock();
    }

    // [--------- Used during development -------------------]
    public String getReleaseVersion() { return model.getReleaseVersion(); }

}
