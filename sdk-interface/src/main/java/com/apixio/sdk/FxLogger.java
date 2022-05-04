package com.apixio.sdk;

import java.util.List;
import java.util.Map;

import com.apixio.sdk.metric.Metric;

/**
 * The philosophy/architecture of logging within the SDK is guided by the view that there
 * are two main use cases that must be supported:
 *
 *  1) debug/diagnostic information; this is temporary information and the "audience" for
 *     it is data science and/or engineering.  It is meant to help answer questions like:
 *     why did it fail (e.g., an exception); is the output as expected (e.g., no exception
 *     but still not right output).  A critical obsservation is that the structure of the
 *     logged info is UP TO THE PERSON WHO ADDS THE LOGGING CODE.  The logged info must be
 *     easily searchable without requiring structure--full text search is useful, for
 *     example (via ssh+grep or centralized graylog).
 *
 *  2) operational/status/metric information; this is possibly permanently stored info (or
 *     at least stored until some decision is made to delete it).  It is meant to help
 *     create metrics/statistics on individual and aggregate operations such as
 *     success/failure rate, processing time, work item completion.  While the structure
 *     of the logged info might be up to the individual developer, there will be
 *     constraints on it as the info is specifically meant to be usable beyond the
 *     debug/diagnostic scope and is intended to be queried by something other than (just)
 *     full-text search.
 *
 * Orthogonal to the above split is the categorization of the logging info into
 * infrastructure and non-infrastructure (i.e., f(x)-specific), where non-infrastructure
 * can be further subdivided.  This is handled with the concept of "component" (the name
 * is to mirror FxComponent, to some degree) within the logging API.  The component
 * portion of the logging is roughly equivalent to the .class-specific Logger in, say,
 * SLF4J.  Because client code of FxLogger is not statically bound with the actual logger
 * implementation but is set implicitly as part of FxComponent.setEnvironment(), this
 * component part of logging is explicitly managed by the infrastructure in that it
 * automatically sets/resets the known component info at the component boundaries.
 *
 * Effective logging requires inclusion of known contextual information and the SDK system
 * automatically provides it by including FxRequest.getAttribute(REQUEST_ID) along with
 * FxEnvironment.getAttribute(DOMAIN).
 *
 * The debug/diagnostic part of the interface is intentionally a subset of SLF4J
 * capabilities.  The String for the messages will be used with String.format() with the
 * supplied Object... passed as the arguments to the format().  THIS IS DIFFERENT FROM
 * SLF4J's use of "{}" for positional parameters!
 *
 * Throwables are converted to a single line format of the stack trace.
 *
 * For operational/status logging, the client provides a Map<String,Object> where the
 * value is possibly a (nested) Map or Collection; this structure then gets logged via
 * com.apixio.logger.EventLogger.event().
 */
public interface FxLogger extends FxComponent
{
    /**
     * FxRequest is used for contextual information
     */
    public void setRequestThreadLocal(FxRequest request);

    /**
     * Current component info is managed by the infrastructure code and is thread-specific
     */
    public void   setCurrentComponentName(String component);
    public String getCurrentComponentName();

    /**
     * Returns the String that contains useful contextual information including
     * execution environment, current component, request information, environment, etc.
     * Null should be returned to signal no context.
     */
    public String getContext();

    /**
     * Standard logging levels as methods
     */
    public void info(String format, Object... args);
    public void warn(String format, Object... args);
    public void debug(String format, Object... args);

    /**
     * "Error" level logging; if throwable form is called then stack trace is
     * included automatically 
     */
    public void error(String format, Object... args);
    public void error(String format, Throwable t, Object... args);

    /**
     * Metrics are a special type of "event" and are broken out as it's useful to have
     * some regularity across all the different code that produces metrics.
     */
    public void metric(Metric metric);
    public void metrics(List<Metric> metrics);

    /**
     *
     */
    public void event(Map<String,Object> packet);

}
