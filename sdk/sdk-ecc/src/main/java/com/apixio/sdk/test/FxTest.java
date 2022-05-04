package com.apixio.sdk.test;

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
import com.apixio.sdk.Converter;

import com.apixio.sdk.builtin.ApxQueryDataUriManager;
import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.genericecc.EccEnvironment;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.ArgsEvaluator;
import com.apixio.sdk.EccInit;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxInvokable;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.ReturnedValueHandler;
import com.apixio.sdk.builtin.ApxQueryDataUriManager;
import com.apixio.sdk.ecc.EccSystem;
import com.apixio.sdk.logging.DefaultLogger;
import com.apixio.sdk.logging.LoggerFactory;
import com.apixio.sdk.protos.EvalProtos;
import com.apixio.sdk.protos.FxProtos;
import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.util.FxBuilder;
import com.apixio.sdk.util.FxEvalParser;
import com.apixio.sdk.util.FxIdlParser;
import com.apixio.sdk.util.TypeUtil;
import com.apixio.sdk.util.Util;

public class FxTest {
    EccEnvironment    eccEnv;
    EccSystem         eccSystem;
    ApxQueryDataUriManager dm;

    public FxTest(String daoConfig, String logConfig) throws Exception
    {
        ConfigSet       daoCfg = ConfigSet.fromYamlFile(new File(daoConfig));
        ConfigSet       logCfg = (logConfig != null) ? ConfigSet.fromYamlFile(new File(logConfig)) : null;
        FxLogger        logger = (logConfig != null) ? LoggerFactory.createLogger(logCfg) : new DefaultLogger();
        EccInit.Builder builder = new EccInit.Builder();
        ConfigSet       fxconfig = daoCfg.getConfigSubtree("fxConfig");
        List<String>    jars = Arrays.asList(fxconfig.getString("jars").split(","));
        List<String>    accessorNames = Arrays.asList(fxconfig.getString("accessors").split(","));
        List<String>    converterNames = Arrays.asList(fxconfig.getString("converters").split(","));

        // FxProtos.FxImpl[] impls = 
        builder
        // .evalArgs(evalArgs)
               .accessors(accessorNames)
               .converters(converterNames)
               .uriManagerCreator("com.apixio.sdk.builtin.FullFxRequestUmCreator")
               .rawJars(jars);
            //    .bindings(bindingClassnames)
            //    .fxImpls(Arrays.asList(new FxProtos.FxImpl[] { fxImpl }));

        EccInit init = builder.build();
        // ecc = new GenericECC(daoCfg, logCfg, init);
        eccEnv = new EccEnvironment(DaoServicesSet.createDaoServices(daoCfg), logger);
        eccSystem = new EccSystem(eccEnv);
        eccEnv.setExecutor(eccSystem);
        eccSystem.initialize(init);
        dm = new ApxQueryDataUriManager();
    }
    
    public Object runAccessor(String accessorString, String... requestParamsAsList) throws Exception
    {
        // Create a request with parameters needed to access data
        FxRequest request = new TestRequest(requestParamsAsList);

        // Pull the input data. Only return one; comma-separated accessors not allowed
        return this.runAccessor(accessorString, request);
    }

    public Object runAccessor(String accString, FxRequest request) throws Exception 
    {
        ArgsEvaluator argsEval = eccSystem.getArgsEval();
        EvalProtos.ArgList  arglist = FxEvalParser.parse(accString);
        List<Object> methodArgs = argsEval.evaluate(request, arglist);
        return methodArgs.get(0);
    }

    public FxInvokable initFx(String jarPath, String className, String fxDef, Map<String, String> assets) throws Exception 
    {
        FxProtos.FxDef  def = FxIdlParser.parse(fxDef);
        FxProtos.FxImpl    impl   = FxBuilder.makeFxImpl(def, Util.ensureUri(jarPath).toURL(), className, assets);
        FxInvokable invokable = eccSystem.loadFx(impl);
        return invokable;
    }
    public Object runFx(FxInvokable invokable, Object... methodArgs) throws Exception 
    {
        List<Object> results = new ArrayList<>();
        ReturnedValueHandler rvh = new ReturnedValueHandler() {
                             public void handleResults(Object rv)
                             {
                                 results.add(rv);
                             }
                         };

        boolean iterated = eccSystem.invokeFx(invokable, Arrays.asList(methodArgs), rvh);
        if (iterated)
        {
            // since we've made mutiple calls to f(x), each of which might return a list, we need to
            // undo the list-of-lists structure.
            return TypeUtil.flattenListOfLists(results);
        }
        else if (results.size() > 0)
        {
            return results.get(0);  // if code below is accurate in terms of predicting exceptions during .invoke(), this will be fine
        }
        else
        {
            return null;
        }
    }

    public Object runAndPersist(String jarPath, String className, String fxDef, Map<String, String> assets, URI dataURI, Object... methodArgs) throws Exception
    {
        // Run the Fx in a way that simulates the real environment
        FxInvokable fxinst = this.initFx(jarPath, className, fxDef, assets);
        Object  output = this.runFx(fxinst, methodArgs);
        
        // Persist the data
        this.persist(fxinst, dataURI, output);  

        return output;
    }
    
    public URI generateUri(String... mapAsList) throws Exception
    {
        return generateUri(new UriParams(mapAsList));
    }

    public URI generateUri(UriParams urip) throws Exception
    {
        return ApxQueryDataUriManager.makeDataURI(eccEnv.getAttribute(FxEnvironment.DOMAIN), urip.getAttributes());
    }

    public URI persist(FxInvokable invokable, URI uri, Object data) throws Exception
    {
        FxType fxt     = invokable.getFxDef().getReturns();
        FxRequest req = requestFromDataURI(uri);
        return eccSystem.persistOutput(fxt, req, data);
    }

    public URI persist(String fxDef, URI uri, Object data) throws Exception
    {
        FxProtos.FxDef  def = FxIdlParser.parse(fxDef);
        FxType returnType = def.getReturns();
        return persist(returnType, uri, data);
    }

    public URI persist(FxType dataType, URI uri, Object data) throws Exception
    {
        FxRequest req = requestFromDataURI(uri);
        URI finalDataUri = eccSystem.persistOutput(dataType, req, data);
        return finalDataUri;
    }
    
    public FxRequest requestFromDataURI(URI dataURI) throws Exception {
        QueryKeys qk = dm.getQueryKeys(dataURI);
        return new TestRequest(qk.getKeys());
    }

    public Object restoreOutput(String fxDef, URI dataURI) throws Exception {
        FxProtos.FxDef  def = FxIdlParser.parse(fxDef);
        FxType returnType = def.getReturns();
        String innerType = returnType.getSequenceInfo().getOfType().getContainerInfo().getName();
        return eccSystem.restoreOutput(dataURI, innerType);
    }

    public Converter getConverter(FxType type) {
        return eccSystem.getConverter(type);
    }

    public static class UriParams extends TestRequest
    {
        public UriParams() {
            super();
        }
        public UriParams(String... mapAsList) {
            super(mapAsList);
        }
    }
    public static class TestRequest extends ListAsMap implements FxRequest
    {
        
        public TestRequest()
        {
        }
        
        public TestRequest(String... mapAsList)
        {
            super(mapAsList);
        }
        
        TestRequest(Map<String,? extends Object> map)
        {
            this.attrs = map.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue()));       
        }
        
    }
    
    public static Map<String,String> Mp(String... mapAsList)
    {
        return ListAsMap.asMap(mapAsList);
    }

    public static class ListAsMap
    {
        protected Map<String,String> attrs;
        
        public ListAsMap(String... mapAsList) {
            attrs = asMap(mapAsList);
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

        public static Map<String, String> asMap(String... mapAsList)
        {
            Map<String, String> attrs = new HashMap<>();
            for (int i = 0; i < mapAsList.length; i += 2) {
                attrs.put(mapAsList[i], mapAsList[i+1]);
            }
            return attrs;
        }
    }
}
