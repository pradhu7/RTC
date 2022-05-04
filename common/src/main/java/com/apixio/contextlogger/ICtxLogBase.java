package com.apixio.contextlogger;

import java.util.Map;

/**
 * Interface representing the general services to populate a log entry.
 * Both user level APIs IApxLogEvent and IApxLogEventContext interfaces
 * extend IApxLogBase.
 *
 * The IApxLogEvent adds logging primitives like info, warn, etc.
 * The IApxLogEventContext uses context logging and adds a single
 * function that can tell if an error occurred.
 */

public interface ICtxLogBase {
    // [--------- Properties ------------------------------]
    void setAppName(String value);
    void setAppVersion(String value);
    void setException(Throwable e);
    void setLogLevel(CtxLogLevel level);

    // [--------- Raw Key/Value setting -------------------]
    void pushKeyValue(String name, String value);
    void pushKeyValue(String name, byte value);
    void pushKeyValue(String name, short value);
    void pushKeyValue(String name, int value);
    void pushKeyValue(String name, long value);
    void pushKeyValue(String name, float value);
    void pushKeyValue(String name, double value);

    // void pushKeyTimestamp(String timestampKey, millisKey);
    // return ts.getSeconds() * 1000L + (long)(ts.getNanos() / 1000000);

    // [--------- App Metrics setting ---------------------]
    void pushAppMetric(String name, String value);
    void pushAppMetric(String name, byte value);
    void pushAppMetric(String name, short value);
    void pushAppMetric(String name, int value);
    void pushAppMetric(String name, long value);
    void pushAppMetric(String name, float value);
    void pushAppMetric(String name, double value);

    // [--------- User Info setting  ----------------------]
    void pushUserInfo(String name, String value);
    void pushUserInfo(String name, byte value);
    void pushUserInfo(String name, short value);
    void pushUserInfo(String name, int value);
    void pushUserInfo(String name, long value);
    void pushUserInfo(String name, float value);
    void pushUserInfo(String name, double value);

    // [--------- Setting using Collections --------------]
    void pushAppMetrics(Map<String, String> appInfo);
    void pushUserInfos(Map<String, String> userInfo);

    // [--------- Used during development ----------------]
    String getReleaseVersion();

}
