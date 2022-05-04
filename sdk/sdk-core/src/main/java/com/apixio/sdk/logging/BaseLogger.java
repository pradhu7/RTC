package com.apixio.sdk.logging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.metric.Metric;

/**
 * All FxLogger implementatios MUST extend this class as this class is compatible with
 * the current bootup/initialization code (if init code changes then FxLoggers can
 * do whatever).
 */
public abstract class BaseLogger implements FxLogger
{

    /**
     * Keep track of current FxRequest and current component name on the thread
     */
    private static final ThreadLocal<FxRequest> cCurrentRequest   = new ThreadLocal<>();
    private static final ThreadLocal<String>    cCurrentComponent = new ThreadLocal<>();

    /**
     * Cached context info
     */
    private String envDomain;

    /**
     * Setting the component name in the constructor can cause problems for subclasses if they
     * need to set up instance fields to track things (because instance fields are intialized
     * after call to super()).  This means that all constructors of subclasses must call this
     * method after super() in their constructors.
     */
    protected void setDefaultComponentName()
    {
        setCurrentComponentName("EccCore");  //!! TODO better name?  is ECC meaningful??
    }

    /**
     * Implementation configuration (intentionally not a method of FxLogger
     */
    public abstract void configure(ConfigSet config) throws Exception;

    /**
     * FxRequest is used for contextual information
     */
    @Override
    public void setRequestThreadLocal(FxRequest request)
    {
        cCurrentRequest.set(request);
    }

    /**
     * Current component info is managed by the infrastructure code and is thread-specific
     */
    @Override
    public void setCurrentComponentName(String component)
    {
        cCurrentComponent.set(component);
    }

    @Override
    public String getCurrentComponentName()
    {
        return cCurrentComponent.get();
    }

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        envDomain = env.getAttribute(FxEnvironment.DOMAIN);

        if (envDomain == null)
            envDomain = "<no_domain>";
    }

    /**
     * The context string included in logging lines consists of the execution domain and the
     * current requestID, if it exists.
     */
    @Override
    public String getContext()
    {
        FxRequest req = cCurrentRequest.get();

        if (req != null)
        {
            return envDomain + ":" + req.getAttribute(FxRequest.REQUEST_ID);
        }
        else
        {
            return envDomain;
        }
    }

    /**
     * Metrics are a special type of "event" and are broken out as it's useful to have
     * some regularity across all the different code that produces metrics.
     */
    @Override
    public void metric(Metric metric)
    {
        Map<String,Object> converted = new HashMap<>();

        metric.report(converted);

        event(addMetricContext(converted));
    }

    @Override
    public void metrics(List<Metric> metrics)
    {
        Map<String,Object> converted = new HashMap<>();

        for (Metric metric : metrics)
            metric.report(converted);

        event(addMetricContext(converted));
    }

    /**
     * The name of each metric in the map will *not* have the contextual information that
     * is needed to organize them effectively for querying, so this method prepends the
     * component name and ...
     */
    private Map<String,Object> addMetricContext(Map<String,Object> metrics)
    {
        String             prefix     = getCurrentComponentName() + ".";
        Map<String,Object> pfxMetrics = new HashMap<>();

        metrics.forEach((k,v) -> pfxMetrics.put(prefix + k, v));

        return pfxMetrics;
    }

}
