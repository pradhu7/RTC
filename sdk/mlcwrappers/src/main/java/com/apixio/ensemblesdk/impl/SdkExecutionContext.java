package com.apixio.ensemblesdk.impl;

import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTime;

import com.apixio.ensemble.ifc.ExecutionContext;
import com.apixio.ensemble.ifc.ModelFile;
import com.apixio.ensemble.ifc.PlatformServices;
import com.apixio.ensemble.ifc.Reporter;
import com.apixio.sdk.FxEnvironment;

/**
 * This class functions as a bridge between what the SDK needs and what the
 * existing ensemble f(x) implementations expect from the enclosing environment.
 * Specifically, it implements the interfaces needed by ensemble f(x) by using
 * what's provided by the SDK.
 */
public class SdkExecutionContext implements ExecutionContext
{
    /**
     * Bridge objects
     */
    private Reporter         sdkLogger;
    private PlatformServices sdkPlatformServices;

    /**
     * As loaded from assets
     */
    private Map<String,String> configuration;
    private ModelFile          modelFile;

    /**
     *
     */
    public SdkExecutionContext(FxEnvironment sdkEnv, Map<String,String> configuration, ModelFile modelFile
        )
    {
        sdkLogger = new SdkReporter(sdkEnv.getLogger());

        this.configuration = configuration;
        this.modelFile     = modelFile;
    }

    // ################################################################
    //  These methods implement ExecutionContext
    // ################################################################

    /**
     * Get the current configuration of the node.
     * @return The current configuration of the node.
     */
    @Override
    public Map<String,String> getConfiguration()
    {
        return configuration;
    }

    /**
     * Get the current map of stashed values for the process.
     * @return A collection of stashed values.
     */
    @Override
    public Map<String,Object> getStash()
    {
        throw new RuntimeException("getStash is not implemented");
    }

    /**
     * The reporter object for the context.
     * @return The reporter object for the context.
     */
    @Override
    public Reporter getReporter()
    {
        return sdkLogger;
    }

    /**
     * This is the connector to any/all platform services that include data retrieval and model access.
     * @return The platform connector.
     */
    @Override
    public PlatformServices getPlatformServices()
    {
        return sdkPlatformServices;
    }

    /**
     * The batch ID.
     * @return The batch ID.
     */
    @Override
    public String getBatchId()
    {
        //        throw new RuntimeException("Who's calling getBatchId?");
        return "";  // !! HACK for now as CAPV app doesn't require any real value
    }

    /**
     * The Job Name.
     * @return The Job Name.
     */
    @Override
    public String getJobName()
    {
        //        throw new RuntimeException("Who's calling getJobName?");
        return "";  // !! HACK for now as CAPV app doesn't require any real value
    }

    /**
     * The patient data set.
     * @return The patient data set.
     */
    @Override
    public String getPatientDataSet()
    {
        //        throw new RuntimeException("Who's calling getPatientDataSet?");
        return "";  // !! HACK for now as CAPV app doesn't require any real value
    }

    /**
     * The submit time.
     * @return The submit time.
     */
    @Override
    public DateTime getSubmitTime()
    {
        throw new RuntimeException("Who's calling getSubmitTime?");
    }

    /**
     * The work name.
     * @return The work name.
     */
    @Override
    public String getWorkName()
    {
        //        throw new RuntimeException("Who's calling getWorkName?");
        return "";  // !! HACK for now as CAPV app doesn't require any real value
    }

    /**
     * Return the model file available at this execution context for loading model
     * related resources
     * @return  the model file
     */
    @Override
    public Optional<ModelFile> getModelFile()
    {
        return Optional.of(modelFile);
    }

}
