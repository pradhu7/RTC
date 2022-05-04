package com.apixio.ensemblesdk.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.apixio.ensemble.ifc.ManagedLifecycle;
import com.apixio.ensemble.impl.model.ModelFile;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxImplementation;
import com.apixio.sdk.FxLogger;

/**
 * This base class is a bridge between SDK-land and MLC-land in that it will behave
 * as a component (i.e., some SDK-based f(x) implementation) within the SDK
 * environment but will also setup and call into an MLC-based f(x), with some
 * runtime caveats.
 *
 * The intent is to provide a migration/shim strategy for existing MLC-based code
 * to allow that code to run within an SDK-compatible system.  In order to do this,
 * any MLC code wrapped with this shim must be published as a ModelCombination
 * using the tools for publishing SDK-based implementations.
 *
 * Configuration is needed by this wrapper class to dynamically create an instance
 * of the real MLC object; currently the configuration needed by the MLC code
 * itself is packaged with this wrapper-required configuration.  The details of
 * this configuration are:
 *
 *  * an asset with the name "config" must be part of the MC; the contents of
 *    this must be a yaml config file that can be converted to Map<String,String>
 *    and the Map<> is passed unmodified to MLC code (retrieved by
 *    ExecutionContext.getConfiguration())
 *
 *  * an asset with the name "fxconfig" must be part of the MC; the contents
 *    of this must be a yaml config file that has the structure:
 *    * fx:
 *        implClass: "the full java classname of the real MLC-based implementation"
 *
 */
public abstract class FxWrapperBase implements FxImplementation
{
    /**
     * As poked into this via SDK
     */
    private ImplementationInfo implInfo;
    private FxEnvironment      environment;
    private FxLogger           logger;

    /**
     * Loaded from asset named "config"
     */
    private Map<String,String> fxConfig;      // used by this code
    private Map<String,String> implConfig;    // passed as config to f(x)

    /**
     * Bridge objects to MLC
     */
    private SdkExecutionContext executionContext;
    private ModelFile           modelFile;

    /**
     * SDK/ECC calls into these
     */
    @Override
    public void setEnvironment(FxEnvironment env)
    {
        this.environment = env;
        this.logger      = env.getLogger();
    }

    @Override
    public void setImplementationInfo(ImplementationInfo info)
    {
        this.implInfo = info;    // no real need to keep this, I think
    }

    /**
     * setAssets converts from the arbitrary and extensible set of files/assets that
     * the SDK model allows into the MLC-required model.zip and config files and
     * sets up the MLC environment based on those.  Note that all assets are given
     * via a URI to a local file, so they'll be of the form file:///blah/asset, and
     * MLC code (as well as other code) wants local OS file paths so we convert
     * from URIs to local paths.
     */
    @Override
    public void setAssets(Map<String,String> assets) throws Exception
    {
        ManagedLifecycle mlc;

        // yaml structure for fxconfig is:
        //
        //  fx:
        //    implClass:  "scala classname that extends PageSignalGeneratorBase"
        fxConfig = loadConfiguration(assets.get("fxconfig"), null);  // used by this code

        // used by MLC code; pushed to SdkExecutionContext as config to be returned from
        // Executioncontext.getConfiguration.  NOTE THAT THE CONVENTION FOR general.yaml
        // IS TO HAVE A SINGLE TOP-LEVEL KEY "generationConfig" THAT NEEDS TO BE
        // EFFECTIVELY IGNORED:  it's the children of that key that we need to load
        // as config.
        implConfig = loadConfiguration(assets.get("config"), "generationConfig");

        modelFile        = new ModelFile(uriToLocalPath(assets.get("model.zip")));
        executionContext = new SdkExecutionContext(environment,
                                                   implConfig,
                                                   modelFile);

        mlc = (ManagedLifecycle) Class.forName(fxConfig.get("fx.implClass")).newInstance();
        mlc.setup(executionContext);
        setFx(mlc);
    }

    /**
     * Transforms the contents of the local (presumed to be yaml) file into two instances
     * of Map<String,String>
     */
    private Map<String,String> loadConfiguration(String localUri, String topKey) throws Exception
    {
        try (InputStream is = new FileInputStream(uriToLocalPath(localUri)))
        {
            ConfigSet          cfg  = ConfigSet.fromYamlStream(is);
            Map<String,String> smap = new HashMap<>();

            if (topKey != null)
                cfg = cfg.getConfigSubtree(topKey);

            for (Map.Entry<String,Object> entry : cfg.getProperties().entrySet())
                smap.put(entry.getKey(), entry.getValue().toString());

            logger.info("Loaded FxWrapper config from %s:  %s", localUri, smap);

            return smap;
        }
    }

    /**
     * Convert from a URI to a local file path, throwing an exception if the URI is not
     * of the form file:///blah/path
     */
    private String uriToLocalPath(String uriPath) throws Exception
    {
        URI    uri    = new URI(uriPath);
        String scheme = uri.getScheme();

        // assume null scheme is local file
        if ((scheme != null) && !scheme.equals("file"))
            throw new IllegalStateException("Attempt to load FxWrapperBase config from non-file:/// asset URI:  " + uriPath);

        return uri.getPath();
    }

    /**
     * Each distinct f(x) signature must extend this class and declare the method with
     * the f(x) signature.  The dynamically created MLC object (whose classname is
     * given by the fx.implClass configuration value) is poked into the subclass; the
     * subclass really should just check that the MLC instance is instanceof what it
     * must be and then cast it and hold onto it.
     */
    protected abstract void setFx(ManagedLifecycle mlc) throws Exception;

}
