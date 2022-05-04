package com.apixio.genericecc;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.apixio.XUUID;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.EccInit;
import com.apixio.sdk.FxInvokable;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.Instantiator;
import com.apixio.sdk.cmdline.Cmdbase;
import com.apixio.sdk.protos.EvalProtos.ArgList;
import com.apixio.sdk.protos.FxProtos;
import com.apixio.sdk.util.EvalLoader;
import com.apixio.sdk.util.FxEvalParser;
import com.apixio.sdk.util.ToolUtil;

/**
 * TestLoadAndRun is a temporary (?) way of instantiating an f(x) impl and running tests.
 * Instantiation is done either via files in a directory or via an MCID, and the mode of
 * instantiation is given by the "mode" parameter.
 *
 * Generally, the syntax is a list of name=value parameters (not "--option" syntax).  The
 * parameters for directory/MCID are:
 *
 *   mode=[directory,mcid]
 *
 * For instantiation via directory, there is only one paramter needed:
 *
 *   directory=[localPathToDirectory]
 *
 * and for MCID:
 *
 *   mcid=[XUUID]
 *   mcconfig=[yamlFile]     # this is needed for MCS REST client and S3 object caching
 *
 * The structure of mcconfig YAML file is similar to the publishing YAML:
 *

    # used for all MCS interactions:
    mcs:
      serverUrl:                http://localhost:8036/
      socketTimeoutMs:          1000
      connectTimeoutMs:         1500
      connectionRequestTimeout: 2000

    # used for all fetched assets:
    cache:
      directory:  /tmp/uricache.run
      options:  VERBOSE                # from UriCacheManager.Option enum: NO_MKDIR, NONE

    # used if the URIs refer to s://
    s3:
      accessKeyUnencrypted:  "blah"
      secretKeyUnencrypted:  "blah"

 */
public class TestLoadAndRun extends Cmdbase
{

    private static class Args
    {
        String daoConfig;
        String loggerConfig;
        String mode;             // "directory" or "mcid"
        String fxDirectory;
        String mcConfig;         // .yaml path for setting up from MC
        String mcID;

        String compJars;         // csv list of filepath to component .jar files
        String accessors;        // csv list of Accessor java classnames
        String converters;       // csv list of Converter java classnames
        String javaBindings;     // csv list of LanguageBinding java classnames
        String dumCreator;       // classname for UmCreator
        String evalFile;         // restored EvalProtos.Arglist
        String evalText;         // eval string to parse with FxEvalParser
        String restoreType;      // symbolic type of data to restore

        Args(String daoConfig, String loggerConfig,
             String mode, String directory, String mcConfig, String mcID, String compJars,
             String accessors, String converters, String javaBindings, String dumCreator, String evalFile, String evalText,
             String restoreType)
        {
            if ((evalFile == null) && (evalText == null))
                throw new IllegalArgumentException("evalFile or evalText is required");

            this.daoConfig    = daoConfig;
            this.loggerConfig = loggerConfig;
            this.mode         = mode;
            this.fxDirectory  = directory;
            this.mcConfig     = mcConfig;
            this.mcID         = mcID;
            this.compJars     = compJars;
            this.accessors    = accessors;
            this.converters   = converters;
            this.javaBindings = javaBindings;
            this.dumCreator   = dumCreator;
            this.evalFile     = evalFile;
            this.evalText     = evalText;
            this.restoreType  = restoreType;
        }

        @Override
        public String toString()
        {
            return ("args(" +
                    "daoConfig=" + daoConfig +
                    "loggerConfig=" + loggerConfig +
                    "mode=" + mode +
                    "directory=" + fxDirectory +
                    "mcconfig=" + mcConfig +
                    "mcid=" + mcID +
                    "compJars=" + compJars +
                    "accessors=" + accessors +
                    "converters=" + converters +
                    "umCreator=" + dumCreator +
                    "evalText" + evalText +
                    "restoreType" + restoreType +
                    "):");
        }
    }

    public static void main(String... args) throws Exception
    {
        TestLoadAndRun tlar = new TestLoadAndRun();
        Args           pargs;

        if ((pargs = tlar.parseArgs(args)) == null)
        {
            usage();
        }
        else
        {
            try
            {
                ConfigSet         daoCfg = ConfigSet.fromYamlFile(new File(pargs.daoConfig));
                ConfigSet         logCfg = (pargs.loggerConfig != null) ? ConfigSet.fromYamlFile(new File(pargs.loggerConfig)) : null;
                FxProtos.FxImpl   impl   = setupImpl(pargs);
                GenericECC        ecc;
                ArgList           evalArgs;
                FxRequest         request;
                URI               dataURI;

                System.out.println("Loaded f(x) implementation:" + impl);
                System.out.println("\n\n################");

                if (pargs.evalText != null)
                    evalArgs = FxEvalParser.parse(pargs.evalText);
                else
                    evalArgs = EvalLoader.loadArgList(ToolUtil.readBytes(new File(pargs.evalFile)));

                ecc = new GenericECC(daoCfg, logCfg,
                                     makeInitConfig(evalArgs, impl,
                                                    splitList(pargs.compJars),
                                                    splitList(pargs.accessors),
                                                    splitList(pargs.converters),
                                                    makeJavaBindings(pargs.javaBindings),
                                                    pargs.dumCreator
                                         ));

                                         
                request = makeRequest();
                request.setAttribute("algo", impl.getEntryName());
                dataURI = execute(ecc, request);
                System.out.println("SFM restored data for uri " + dataURI);
            }
            catch (Exception x)
            {
                x.printStackTrace();
            }
            catch (Error y)
            {
                y.printStackTrace();
            }
            finally
            {
                System.exit(0);
            }
        }
    }

    static List<String> splitList(String csv)
    {
        return (csv != null) ? Arrays.asList(csv.split(",")) : null;
    }

    static URI execute(GenericECC ecc, FxRequest request) throws Exception
    {
        Object            output;
        List<FxInvokable> fxis;
        URI               dataURI;
        fxis = ecc.getInvokables();
        // ecc.invokeFx(fxis.get(0), request,
        //              new ReturnedValueHandler() {
        //                  public void handleResults(Object rv)
        //                  {
        //                      System.out.println("[results] " + rv);
        //                  }
        //              });


        output  = ecc.invokeFx(fxis.get(0), request);
        dataURI = ecc.persistFxOutput(fxis.get(0), request, output);
        return dataURI;
    }
    private static List<String> makeJavaBindings(String csv)
    {
        // must return null--not empty list--for null csv
        if (csv == null)
            return null;
        else
            return Arrays.asList(csv.split(","));
    }

    private static FxProtos.FxImpl setupImpl(Args args) throws Exception
    {
        Instantiator setup = new Instantiator();

        if (args.mode.equals("directory"))
        {
            return setup.setupFromDirectoryOrFile(new File(args.fxDirectory));
        }
        else if (args.mode.equals("mcid"))
        {
            return setup.setupFromModelCombination(
                ConfigSet.fromYamlFile(new File(args.mcConfig)),
                XUUID.fromString(args.mcID), null, null);  //!! TODO allow overrides
        }
        else
        {
            throw new IllegalArgumentException("Unknown mode " + args.mode);
        }
    }

    static FxRequest makeRequest()
    {
        Map<String,String> attrs = new HashMap<>();

        // totally fake stuff here
        //        attrs.put("docuuid", "DOC_0c1fa42f-393a-4b1c-9eba-78cc5490b88e");
        attrs.put("docuuid", "027b0e4f-fdc5-4fcc-91a3-79a4d3a3d39b");      // works on staging
        attrs.put("patientuuid", "48156abe-cbe8-440a-9dfd-42b4ea7b223d"); 
        attrs.put("annopatientuuid", "e93d6b2f-c130-471b-9c25-729a405862d6");     // has annotations in tiny index
        attrs.put("patientjsonpath", "./genlambdaecc/src/test/resources/48156abe-cbe8-440a-9dfd-42b4ea7b223d.json");
        attrs.put("patientannotationindex", "apixio-science-data;david/fxannotations;v1");

        // required attributes:
        attrs.put(FxRequest.REQUEST_ID, "RQ_" + java.util.UUID.randomUUID());

        return new TestRequest(attrs);
    }

    private static EccInit makeInitConfig(ArgList evalArgs, FxProtos.FxImpl fxImpl, List<String> jars,
                                          List<String> accessorClassnames, List<String> converterClassnames,
                                          List<String> bindingClassnames, String umCreatorClassname
        )
    {
        EccInit.Builder builder = new EccInit.Builder();

        builder.evalArgs(evalArgs)
               .accessors(accessorClassnames)
               .converters(converterClassnames)
               .uriManagerCreator(umCreatorClassname)
               .rawJars(jars)
               .bindings(bindingClassnames)
               .fxImpls(Arrays.asList(new FxProtos.FxImpl[] { fxImpl }));

        return builder.build();
    }

    private Args parseArgs(String[] args)
    {
        List<String> largs = toModifiableList(args);

        try
        {
            return new Args(
                require(largs,  "config"),
                optional(largs, "logcfg"),
                require(largs,  "mode"),
                optional(largs, "impldir"),
                optional(largs, "mcconfig"),
                optional(largs, "mcid"),
                optional(largs, "compjars"),
                optional(largs, "accessors"),
                optional(largs, "converters"),
                optional(largs, "bindings"),
                require(largs,  "umcreator"),
                optional(largs, "evalfile"),
                optional(largs, "evaltext"),
                optional(largs, "restoretype")
                );
        }
        catch (IllegalArgumentException x)
        {
            System.err.println(x.getMessage());

            return null;
        }
    }

    private static void usage()
    {
        System.out.println("Usage:  TestLoadAndRun \\\n" +
                           " impldir={directory} compjars={csv-of-jars} accessors={csv-of-classes} \\\n" +
                           " converters={csv-of-classes} umcreator={UmCreatorClassname}\\\n" +
                           " [evaltext={eval text}] [evalfile={filepath}]"
            );
    }

    public static class TestRequest implements FxRequest
    {
        private Map<String,String> attrs;

        TestRequest(Map<String,String> attrs)
        {
            this.attrs = attrs;
        }

        public String getAttribute(String id)
        {
            return attrs.get(id);
        }
        public Map<String,String> getAttributes()
        {
            return Collections.unmodifiableMap(attrs);
        }

        public void setAttribute(String id, String val)
        {
            attrs.put(id, val);
        }
    }


}
