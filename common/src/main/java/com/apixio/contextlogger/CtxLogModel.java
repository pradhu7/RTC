package com.apixio.contextlogger;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import com.github.wnameless.json.unflattener.JsonUnflattener;

/**
 * Contains the information about a log entry to be posted in the
 * future. Decoupled from any particular logging system.
 *
 * It includes the properties to set up the log payload and a method to transform
 * the payload into Json string.
 */
public class CtxLogModel {

    final String APP_NAME_KEY = "app.app_name";
    final String APP_VERSION_KEY = "app.app_version";
    final String EXCEPTION_KEY = "app.app_data.exception";
    final String LOG_LEVEL_KEY = "level";
    final String ELAPSED_TIME_KEY = "elapsed_time";
    final String APP_METRIC_PREFIX = "app.app_data.";
    final String USER_INFO_PREFIX = "app.app_user_info.";
    private CtxLogLevel level;
    private String tagName;
    private boolean clockStarted = false;
    private boolean hasError = false;
    private long startTime;
    private final Map<String, String> payload;

    private CtxLogModel(boolean ordered) {
        if (ordered) {
            this.payload = new TreeMap<>();
        } else {
            this.payload = new LinkedHashMap<>();
        }
        this.level = CtxLogLevel.LOG_INFO;
        this.tagName = "xx";
    }

    public static CtxLogModel build(boolean ordered) {
        return new CtxLogModel(ordered);
     }

    public static CtxLogModel build() {
        return new CtxLogModel(false);
    }

    public void putKeyValue(String name, String value) {
        payload.put(name, value);
        if (name.endsWith(".bytes")) {
            String counter = name.substring(0, name.length() - 5) + "count";
            if (!value.equals("")) {
                payload.put(counter, "1");
            }
        } else if (name.endsWith(".millis")) {
            String counter = name.substring(0, name.length() - 6) + "count";
            if (!value.equals("")) {
                payload.put(counter, "1");
            }
        }
    }

    // [--------- Properties -------------------------------]

    public void setAppName(String value) {
        pushKeyValue(APP_NAME_KEY, value);
    }

    public void setAppVersion(String value) {
        pushKeyValue(APP_VERSION_KEY, value);
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setException(Throwable e) {
        hasError = true;
        pushKeyValue(EXCEPTION_KEY, e.toString());
    }

    public void setLogLevel(CtxLogLevel level) {
        this.level = level;
        pushKeyValue(LOG_LEVEL_KEY, level.name() );
    }

    public CtxLogLevel getLogLevel() {
        return level;
    }

    // [--------- Push Key/Value ---------------------------]
    public void pushKeyValue(String name, String value) {
        payload.put(name, value);
    }

    public void pushKeyValue(String name, byte value) {
        payload.put(name, String.valueOf(value));
    }

    public void pushKeyValue(String name, short value) {
        payload.put(name, String.valueOf(value));
    }

    public void pushKeyValue(String name, int value) {
        payload.put(name, String.valueOf(value));
    }

    public void pushKeyValue(String name, long value) {
        payload.put(name, String.valueOf(value));
    }

    public void pushKeyValue(String name, float value) {
        payload.put(name, String.valueOf(value));
    }

    public void pushKeyValue(String name, double value) {
        payload.put(name, String.valueOf(value));
    }

    // [--------- App Metric -----------------------------]
    public void pushAppMetric(String name, String value) {
        pushKeyValue(APP_METRIC_PREFIX + name, value);
    }

    public void pushAppMetric(String name, byte value) {
        pushKeyValue(APP_METRIC_PREFIX + name, value);
    }

    public void pushAppMetric(String name, short value) {
        pushKeyValue(APP_METRIC_PREFIX + name, value);
    }

    public void pushAppMetric(String name, int value) {
        pushKeyValue(APP_METRIC_PREFIX + name, value);
    }

    public void pushAppMetric(String name, long value) {
        pushKeyValue(APP_METRIC_PREFIX + name, value);
    }

    public void pushAppMetric(String name, float value) {
        pushKeyValue(APP_METRIC_PREFIX + name, value);
    }

    public void pushAppMetric(String name, double value) {
        pushKeyValue(APP_METRIC_PREFIX + name, value);
    }

    // [--------- User Info ------------------------------]
    public void pushUserInfo(String name, String value) {
        pushKeyValue(USER_INFO_PREFIX + name, value);
    }

    public void pushUserInfo(String name, byte value) {
        pushKeyValue(USER_INFO_PREFIX + name, value);
    }

    public void pushUserInfo(String name, short value) {
        pushKeyValue(USER_INFO_PREFIX + name, value);
    }

    public void pushUserInfo(String name, int value) {
        pushKeyValue(USER_INFO_PREFIX + name, value);
    }

    public void pushUserInfo(String name, long value) {
        pushKeyValue(USER_INFO_PREFIX + name, value);
    }

    public void pushUserInfo(String name, float value) {
        pushKeyValue(USER_INFO_PREFIX + name, value);
    }

    public void pushUserInfo(String name, double value) {
        pushKeyValue(USER_INFO_PREFIX + name, String.valueOf(value));
    }

    // [--------- Push for Collections ------------------------]

    public void pushAppMetrics(Map<String, String> appInfo) {
        for (Map.Entry<String,String> entry : appInfo.entrySet())
            pushAppMetric(entry.getKey(), entry.getValue());
    }
    public void pushUserInfos(Map<String, String> userInfo) {
        for (Map.Entry<String,String> entry : userInfo.entrySet())
            pushAppMetric(entry.getKey(), entry.getValue());
    }

    // [--------- Internal helpers & timing ------------------]

    private long getTime() {
        return System.nanoTime();
    }

    /**
     * Used to check if we need to override log message with an error in case
     * of an exception message received.
     */
    public boolean hasError() {
        return this.hasError;
    }

    public void startClock() {
        this.clockStarted = true;
        this.startTime = getTime();
    }

    public void setElapsedTime() {
        long elapsed = Math.subtractExact(getTime(), this.startTime);
        Duration value = Duration.ofNanos(elapsed);
        pushKeyValue(ELAPSED_TIME_KEY, value.toString());
    }

    // [--------- Payload generation -------------------------]

    public String emit() {
        if (this.clockStarted) {
            setElapsedTime();
        }
        return JsonUnflattener.unflatten(this.payload);
    }

    // [--------- Used during development -------------------]
    String getReleaseVersion() {
        return "r1.0.15";
    }

}
