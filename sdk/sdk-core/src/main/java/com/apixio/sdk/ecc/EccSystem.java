package com.apixio.sdk.ecc;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.protobuf.Message;

import org.apache.commons.lang3.NotImplementedException;

import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.dao.apxdata.ApxDataDao;
import com.apixio.sdk.ArgsEvaluator;
import com.apixio.sdk.Converter;
import com.apixio.sdk.DataUriManager;
import com.apixio.sdk.EccInit;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxExecEnvironment;
import com.apixio.sdk.FxExecutor;
import com.apixio.sdk.FxInvokable;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.JavaFxInvokable;
import com.apixio.sdk.LanguageBinding.Binding;
import com.apixio.sdk.LanguageBinding;
import com.apixio.sdk.ReturnedValueHandler;
import com.apixio.sdk.SvcFxInvokable;
import com.apixio.sdk.UmCreator;
import com.apixio.sdk.builtin.ApxQueryDataUriManager;
import com.apixio.sdk.protos.EvalProtos.ArgList;
import com.apixio.sdk.protos.FxProtos.FxDef;
import com.apixio.sdk.protos.FxProtos.FxImpl;
import com.apixio.sdk.protos.FxProtos.FxTag;
import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.protos.FxProtos;
import com.apixio.sdk.util.TypeUtil;
import com.apixio.sdk.util.Util;

/**
 * EccSystem is the central coordinating class for the SDK runtime and provides registration/init
 * services as well as f(x)impl invocation and persistence services.  There should be only one
 * instance of this per JVM.
 */
public class EccSystem implements FxExecutor
{

    /**
     * dynJars includes all accessor jars and f(x) implementation jar, in that order.  The
     * dynLoader is the single URLClassLoader that looks in all those jars.
     */
    private URLClassLoader dynLoader;

    /**
     *
     */
    private List<FxInvokable> invokables = new ArrayList<>();
    private FxExecEnvironment environment;
    private FxLogger          logger;
    private ArgList           eccEval;
    private Storage           storage;
    private Accessors         accessors;
    private DataUriManager    outputDataManager;
    private ArgsEvaluator     argsEval;

    /**
     * Language bindings allow the conversion from symbolic data type name to a language-specific
     * identifier (Java classname, here).  These symbolic type names are used for in f(x) parameter
     * lists and return value.  It is an error for a type name not to be able to be converted to
     * a Java classname.
     */
    private Map<String,String> javaBindings;

    /**
     *
     */
    public EccSystem(FxEnvironment fxenv)
    {
        environment = Util.checkExecEnvironment(fxenv);
        logger      = environment.getLogger();
    }

    /**
     * Enclosing generic function execution environment must collect all the required configuration
     * somehow and then call initialize().
     */
    @Override
    public List<FxInvokable> initialize(EccInit init) throws Exception  //!! must return List<FxInvokable> or something
    {
        String logName = logger.getCurrentComponentName();

        // Note that for FxTest case, we don't have an FxImpl specified so this code
        // must handle that case gracefully

        try
        {
            List<FxImpl> implDefs = init.getFxImpls();   // will be null for FxTest
            FxImpl       fxImpl   = null;
            String       implUrl  = null;
            List<URL>    dynJars;

            if (implDefs != null)
            {
                if (implDefs.size() == 0)
                    throw new IllegalStateException("Attempt to initialize ECC in non-FxTest mode but with no FxImpl objects");
                else if (implDefs.size() > 1)
                    throw new IllegalStateException("More than one FxImpl in a single ECC instance is not yet supported");

                fxImpl  = implDefs.get(0);
                implUrl = fxImpl.getImplUrl();

                // this is overkill at this point in time since we're not supporting implDefs.size() > 1, but
                // let's leave it in as a reminder for the future
                checkSameJar(implDefs);
            }

            logger.setCurrentComponentName("ECC-boot");

            eccEval = init.getEvalArgs();

            logger.info("Plugin jars: %s", init.getPluginJars());

            // plugin jars are referenced by URIs to allow "asset://blah/assetName"
            dynJars = convertJarUris(fxImpl, init.getPluginJars());

            if ((implUrl != null ) && implUrl.endsWith(".jar"))
                dynJars.add(Util.ensureUri(implUrl).toURL());

            setupClassloader(dynJars);

            // must be done after setting up classloader
            setupLanguageBindings(init.getLanguageBindings());

            logger.info("Non-builtin Accessor classnames: %s", init.getAccessorClassnames());
            accessors = new Accessors(environment, dynLoader, init.getAccessorClassnames());

            logger.info("Non-builtin Converter classnames: %s", init.getConverterClassnames());
            storage   = new Storage(environment, dynLoader, init.getConverterClassnames());

            setupUriManager(init.getUmCreatorClassname());

            List<FxType> paramList = fxImpl != null ? fxImpl.getFxDef().getParametersList() : null;
            argsEval = new ArgsEvaluator(eccEval, environment, accessors.getAccessors(), paramList);

            if (fxImpl != null)
                invokables.add(loadFx(fxImpl));

            return invokables;
        }
        finally
        {
            logger.setCurrentComponentName(logName);
        }
    }

    /**
     * Plugin jars is a list of URIs (as compared to URLs) to allow a reference to an asset, like
     *
     *  asset://devel/extra.jar
     *
     * where "devel" is ignored (required for URI syntax) and "extra.jar" is the asset name that will
     * have been downloaded locally (see Instantiator.setupFromModelCombination)
     *
     * FxImpl is required to locate the asset given the name
     */
    private List<URL> convertJarUris(FxImpl fxImpl, List<URI> jars) throws Exception
    {
        List<URL> jarUrls = new ArrayList<>();

        for (URI jarUri : jars)
        {
            String scheme = jarUri.getScheme();

            if ("asset".equals(scheme))
            {
                String assetName;
                String localAsset;

                if (fxImpl == null)
                    throw new IllegalStateException("Plugin jar URI has scheme 'asset' but no FxImpl exists to map to actual URI: " + jarUri);

                assetName  = jarUri.getPath().substring(1);
                localAsset = fxImpl.getAssets().get(assetName);   // getPath should NOT be returning null here

                if (localAsset == null)
                    throw new IllegalStateException("Plugin jar " + jarUri + " needs (local) asset " + jarUri.getPath() + " but no such asset exists");

                logger.info("Component jar %s supplied by asset %s at path %s", jarUri, assetName, localAsset);

                jarUrls.add(new URL(localAsset));
            }
            else
            {
                jarUrls.add(jarUri.toURL());
            }
        }

        return jarUrls;
    }

    /**
     * TODO:  fix this so it can return a list
     */
    @Override
    public List<FxInvokable> getFxs()
    {
        return invokables;
    }

    /**
     * Invoke the process method on the given implClass using reflection to build the parameter list and
     * to actually invoke.  The building of the parameter list is non-trivial as it uses the FxTypes of
     * the parameters to determine if the data needs to be loaded from persistent storage or fetched via a
     * data accessor, etc.
     *
     * Note that this code needs to handle the desired operation of possibly invoking f(x) for each
     * element returned from an accessor, such as when an accessor returns a list of PageWindow and the
     * need is for f(x) to be invoked for each single PageWindow.  This behavior is straightforward for
     * the case where only a single arg needs to be iterated on, like so, where f(x) is
     *
     *  extractSignals(PageWindow pw)
     *
     * and the single evaluated arg is determined to be List<PageWindow> (which can be determined at
     * runtime even with Java type erasure by list.get(0).getClass()).  But it's more interesting what
     * should be done if the signature is:
     *
     *  extractSignals(PageWindow pw, String something)
     *
     * and the actual arg list has types [List<PageWindow>, List<String>].  For the time being this code
     * will only support a single arg iteration.  If two args are lists and both need to be iterated, then
     * an exception will be thrown
     *
     * The ReturnedValueHandler manages the multiple return values from multiple invocations of f(x) and
     * is used only in that case; if it's required but is null, then an exception will be thrown.
     */
    @Override
    public Object invokeFx(FxInvokable invokable, FxRequest request) throws Exception
    {
        // if we're doing auto-iteration (because f(x) is declared with a param of type T and the actual
        // arg is List<T>) then the return type *must* be a list of the objects returned from the
        // invocation.  what we do to support this is to always 
        List<Object> results = new ArrayList<>(); 
        boolean      iterated;

        iterated = invokeFx(invokable, request, new ReturnedValueHandler() {
                public void handleResults(Object rv)
                {
                    results.add(rv);
                }
            });

        if (iterated)
        {
            // since we've made mutiple calls to f(x), each of which might return a list, we need to
            // undo the list-of-lists structure.  this is just gross
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

    // this form will return an object if arglist doesn't need to be iterated;
    // if arg needs to be iterated, then calls rvh (unless null) and returns null.
    // true is returned if auto-iterating was done
    @Override
    public boolean invokeFx(FxInvokable invokable, FxRequest request, ReturnedValueHandler rvh) throws Exception
    {
        List<Object> methodArgs;
        
        if (rvh == null)
            throw new IllegalArgumentException("invoking f(x) might require iteration on list arg but ReturnedValueHandler is null so no per-invocation results can be handled");
        
        try
        {
            logger.setRequestThreadLocal(request);
            methodArgs = argsEval.evaluate(request);
            
            return invokeFx(invokable, methodArgs, rvh);
        }
        finally
        {
            logger.setRequestThreadLocal(null);
        }
    }

    public boolean invokeFx(FxInvokable invokable, List<Object> methodArgs, ReturnedValueHandler rvh) throws Exception
    {
        boolean      autoIterate = false;
        

        if (!argsHasList(methodArgs)) // if nothing's a list then it's easy
        {
            rvh.handleResults(invokable.invoke(methodArgs.toArray())); 
        }
        else
        {
            Class<?>[] ptypes = invokable.getParameterTypes();
            boolean    invoke = true;

            if (ptypes.length != methodArgs.size())
                throw new IllegalStateException("Size of method parameter list and actual argument list doesn't match:  " + ptypes.length + "::" + methodArgs.size());

            // check each arg to see if it's a list and the corresponding param type is NOT a list; if that's
            // the case then try iterating on the list elements, making a call to f(x) for each one.

            for (int pi = 0, m = methodArgs.size(); pi < m; pi++)
            {
                Object arg = methodArgs.get(pi);

                if ((arg instanceof List) && (ptypes[pi] != List.class))
                {
                    List<?> listArg = (List<?>) arg;

                    if (listArg.size() > 0)
                    {
                        // since we *can* check type of list element (assuming homogenous types) confirm it
                        // can be converted to paramtype
                        if (ptypes[pi].isAssignableFrom(listArg.get(0).getClass()))
                        {
                            List<Object> args = new ArrayList<>(methodArgs);

                            autoIterate = true;

                            for (Object larg : listArg)
                            {
                                args.set(0, larg);
                                rvh.handleResults(invokable.invoke(args.toArray()));
                            }

                            invoke = false;
                        }
                        else
                        {
                            logger.debug("Actual list arg elements can't be assigned as param type");
                            // actual type of elements in list arg != type needed by method; this *should* fail during method.invoke().
                            // (empty "else" clause on purpose--exists only for documentation)
                        }

                        // always break:  if we iterated, then we're done, otherwise let .invoke() (eventually?) throw the param mismatch exception
                        break;
                    }
                    else
                    {
                        // we know method.invoke() will fail as we're passing in an empty list when paramtype != list;
                        // break out of the loop as there's no reason to go on and let invoke() fail
                        invoke = false;
                        break;
                    }
                }
            }

            if (invoke)  // let param type errors be detected during .invoke():
                rvh.handleResults(invokable.invoke(methodArgs.toArray()));
        }

        return autoIterate;
    }

    /**
     * Used to persist output from f(x) invocation if enclosing system deems it necessary.
     */
    @Override
    public URI persistOutput(FxInvokable invokable, FxRequest request, Object o) throws Exception
    {
        FxType fxt     = invokable.getFxDef().getReturns();
        return persistOutput(fxt, request, o);
    }

    public URI persistOutput(FxType fxt, FxRequest request, Object o) throws Exception
    {
        URI    dataUri;
        FxTag  tag     = fxt.getTag();
        if (tag == FxTag.SEQUENCE)
        {
            FxProtos.SequenceInfo si = fxt.getSequenceInfo();
            
            fxt = si.getOfType();
            
            if (fxt.getTag() == FxTag.CONTAINER)  // a list of classes, so we can persist them
            {
                FxProtos.ContainerInfo ci        = fxt.getContainerInfo();                
                String                 symType   = ci.getName();
                ApxDataDao             dao       = storage.getDataDao();
                Converter              converter = storage.getConverter(javaBindings.get(symType));
                String                 grouping;
                
                if (converter == null)
                throw new IllegalStateException("Unable to persist objects of symbolic type " + symType + " as there is no registered converter");
                
                // it's a SEQUENCE of CONTAINER (object):
                
                if (o != null)
                o = ((List<Object>) o).stream().filter(i -> (i != null)).map(i -> converter.convertToProtobuf(i)).collect(Collectors.toList());
                else
                o = new ArrayList();
                
                dataUri = outputDataManager.makeDataURI(request);
                if ((grouping = outputDataManager.makeGroupingID(request)) == null)
                    throw new IllegalStateException("Programming error:  dataUriManager returned null for groupingID");
                
                //                System.out.println("SFM persisting list of symtype " + symType);
                //                System.out.println("    persisting dataUri " + dataUri);
                //                System.out.println("    persisting groupingID " + grouping);
                //                System.out.println("    persisting partitionID " + outputDataManager.makePartitionID(request));
                //                System.out.println("    persisting querykeys " + outputDataManager.makeQueryKeys(request));

                dao.putData(converter.getMetadata().protoClass,
                            grouping,
                            outputDataManager.makePartitionID(request),
                            outputDataManager.makeQueryKeys(request),
                            false,          // DataType.canContainPHI determines encryption
                            (List) o);

            }
            else
            {
                throw new RuntimeException("Persisting a list of non-classes is not yet supported in SDK");
            }
        }
        else if (tag == FxTag.CONTAINER)
        {
            throw new RuntimeException("Persisting a single value is not yet supported in SDK");
        }
        else
        {
            throw new RuntimeException("Persisting value of type " + tag + " is not yet supported in SDK");
        }

        return dataUri;
    }


    public Converter getConverter(FxType fxt) {
        FxTag  tag     = fxt.getTag();
        if (tag == FxTag.SEQUENCE)
        {
            FxProtos.SequenceInfo si = fxt.getSequenceInfo();
            
            fxt = si.getOfType();
            
            if (fxt.getTag() == FxTag.CONTAINER)  // a list of classes, so we can persist them
            {
                FxProtos.ContainerInfo ci        = fxt.getContainerInfo();                
                String                 symType   = ci.getName();
                return storage.getConverter(javaBindings.get(symType));
            }
        }
        throw new NotImplementedException();
                
    }

    /**
     * Reads and restores the data identified by the given data URI that was written during the call to
     * persistOutput().  This method handles the case where the data URI doesn't directly record the
     * groupingID but can supply query keys to retrieve it.
     */
    @Override
    public List<Object> restoreOutput(URI dataURI, String datatype) throws Exception
    {
        String       groupingID = outputDataManager.getGroupingID(dataURI);
        ApxDataDao   dao        = storage.getDataDao();
        List<Object> restored;

        if (groupingID == null)
        {
            QueryKeys    qk = outputDataManager.getQueryKeys(dataURI);
            List<String> ids;

            if (qk == null)
                throw new IllegalStateException("Can't restore data from URI due to lack of both groupingID and query keys: " + dataURI);

            ids = dao.getGroupingIDs(qk);

            if (ids.size() == 0)
                throw new IllegalStateException("Can't restore data from URI as query keys produce no groupingID: " + dataURI);
            else if (ids.size() > 1)
                throw new IllegalStateException("Can't restore data from URI as query keys produce more than one groupingID: " + dataURI);

            groupingID = ids.get(0);
        }

        restored = dao.getData(groupingID);

        if (datatype != null)
        {
            Converter converter = storage.getConverter(javaBindings.get(datatype));

            if (converter == null)
                throw new IllegalStateException("Unable to find converter for converting to symbolic type " + datatype);

            restored = restored.stream().map(i -> converter.convertToInterface((Message) i)).collect(Collectors.toList());
        }

        return restored;
    }                                                     

    /**
     * Make sure that all the FxImpls that are to be located/loaded are within a single jar.  This is
     * required (currently) as the system supports loading only a single impl.jar due to likely
     * class version conflicts otherwise.
     */
    private void checkSameJar(List<FxImpl> impls)
    {
        if ((impls != null) && (impls.size() > 1))
        {
            Set<String> urls = new HashSet<>();

            urls.add(impls.get(0).getImplUrl());

            for (int i = 1, m = impls.size(); i < m; i++)
            {
                String url = impls.get(i).getImplUrl();

                if (urls.contains(url))
                    throw new IllegalStateException("Specifying multiple FxImpls requires that all use the same jar, but this impl fails: " + impls.get(i));
                else
                    urls.add(url);
            }
        }
    }

    /**
     * Sets up Java language bindings via the list of classnames, each of which should refer to a class
     * that implements com.apixio.sdk.LanguageBinding.  Bindings are added in the same order as the
     * list of classnames, allowing overrides (might be dangerous?).
     *
     * The default bindings as registered by the class of LanguageBinding.DEFAULT_BINDING_CLASSNAME
     * (com.apixio.sdk.binding.DefaultJavaBindings) is added unless the first element in the list
     * of binding classnames is "-" (a marker to indicate to the system to start fresh).  Default
     * bindings will take precedence, namewise, over supplied bindings.
     */
    private void setupLanguageBindings(List<String> bindingClassnames) throws Exception
    {
        // if first element is "-" then DON'T add default binding
        if ((bindingClassnames.size() == 0) || !bindingClassnames.get(0).equals("-"))
            bindingClassnames.add(LanguageBinding.DEFAULT_BINDING_CLASSNAME);
        else
            bindingClassnames.remove(0);  // kill -

        javaBindings = new HashMap<>();

        for (String javaClassname : bindingClassnames)
        {
            logger.info("Attempting to load language binding class %s", javaClassname);

            for (Binding binding : ((Class<LanguageBinding>) dynLoader.loadClass(javaClassname)).newInstance().getJavaBindings())
                javaBindings.put(binding.getTypename(), binding.getJavaClassname());
        }
    }

    /**
     * Use the UmCreator to set up the actual URI manager instance.  This delegation is required in order
     * to keep classes like ApxQueryDataUriManager generic within the SDK but still allows small helper
     * classees at the application level (code lives outside the SDK proper) to centralize setup.
     */
    private void setupUriManager(String classname) throws Exception
    {
        UmCreator creator;

        logger.info("Attempting to set up UriManager via class %s", classname);

        creator = ((Class<UmCreator>) dynLoader.loadClass(classname)).newInstance();

        creator.setEnvironment(environment);

        outputDataManager = creator.createUriManager();
        outputDataManager.setEnvironment(environment);
    }

    /**
     * This is the generic way to load an arbitrary JVM-based f(x) implementation.  The FxImpl must have
     * been somehow (magically) restored/known and is used to locate/load the actual .class that implements
     * the f(x).  It's this .class (as expressed via Class<?>) that's returned and is suitable for passing
     * to invokeFx() where runtime reflection is used to build the parameters and to invoke the actual
     * method.
     *
     * This method also performs initialization of the implementation by invoking the compiled-in method
     * for that (i.e., all fxj-produced abstract base classes automatically include setup() and teardown()
     * method declarations).
     */
    public FxInvokable loadFx(FxImpl impl) throws Exception
    {
        FxInvokable invokable = createInvokable(impl);

        invokeSetup(invokable);

        return invokable;
    }

    /**
     * Return true if any of the args is a java.util.List, as the invocation of f(x) deals with list
     * args specially by auto-iterating on elements in some cases.
     */
    private boolean argsHasList(List<Object> args)
    {
        for (Object arg : args)
        {
            if (arg instanceof List)
                return true;
        }

        return false;
    }

    /**
     * Creates the single URLClassLoader that loads all dynamically loaded classes.  This includes
     * accessor and f(x) implementation jars
     */
    private void setupClassloader(List<URL> jars) throws Exception
    {
        logger.info("Initializing classloader from jars %s", jars);

        dynLoader = new URLClassLoader(jars.toArray(new URL[jars.size()]), Thread.currentThread().getContextClassLoader());
    }

    /**
     *
     */
    private FxInvokable createInvokable(FxImpl impl) throws Exception
    {
        Class<?> clz;
        Method   fx;

        logger.info("Attempt to load F(x) implementation class %s", impl.getEntryName());
        String implUrl = impl.getImplUrl();
        if (impl.getEntryName().startsWith("svc::")) {
            HashMap<FxType, Converter> implConverters = new HashMap<>();
            implConverters.put(impl.getFxDef().getReturns(), getConverter(impl.getFxDef().getReturns()));
            for (FxType argType: impl.getFxDef().getParametersList()) {
                implConverters.put(argType, getConverter(argType));
            }
            return new SvcFxInvokable(logger, impl, implUrl, implConverters);
        } else if (implUrl.endsWith(".jar")) {
            clz = dynLoader.loadClass(impl.getEntryName());
            fx = findFxMethod(clz, impl);
            return new JavaFxInvokable(logger, impl, clz, clz.newInstance(), fx);
        }
        return null;
    }

    private Method findFxMethod(Class<?> clz, FxImpl impl) throws Exception
    {
        FxDef          def        = impl.getFxDef();
        List<Class<?>> paramTypes = new ArrayList<>();

        for (FxType type : def.getParametersList())
            paramTypes.add(convertToClassType(clz, type));

        // using Class.getDeclareMethod() will limit search to just that class
        return clz.getMethod(def.getName(), paramTypes.toArray(new Class<?>[paramTypes.size()]));
    }

    private Class<?> convertToClassType(Class<?> clz, FxType type) throws Exception
    {
        FxTag    tag = type.getTag();
        Class<?> tClz;

        // note that because Java discards generic type info, we can match only on the
        // structure-level argument type.  for example, even if the actual compile-time
        // definition of some f(x) contains, say, List<String>, we can only select on
        // List.class (dropping the String type)

        // this code makes assumptions as to what the fxj-generated .java files produce for
        // the various types

        if ((tClz = checkScalar(tag)) != null)
        {
            return tClz;
        }
        else if (tag == FxTag.MAP)
        {
            return Map.class;
        }
        else if (tag == FxTag.SEQUENCE)
        {
            return List.class;
        }
        else if (tag == FxTag.ENUM)
        {
            logger.error("convertToClassType hit (unsupported) enumeration " + type.getEnumInfo());
            return null; //!! wrong--need to locate/load the emitted enum.class
        }
        else if (tag == FxTag.CONTAINER)
        {
            String typeName      = type.getContainerInfo().getName();
            String javaClassname = javaBindings.get(typeName);

            //            System.out.println("################ SFM convertToClassType on container with typename " + typeName);

            if (javaClassname == null)
                throw new IllegalStateException("Unable to convert symbolic typename " + typeName + " to a Java classname.  Check JavaLanguageBinding settings");

            // struct or union--both expressed as java class... union is ONLY interesting for persistence
            return clz.getClassLoader().loadClass(javaClassname);
        }
        else
        {
            throw new IllegalArgumentException("FxType " + type + " can't be converted to Class<?> object");
        }
    }

    private Class<?> checkScalar(FxTag tag)
    {
        if (tag == FxTag.INT)
            return Integer.class;
        else if (tag == FxTag.LONG)
            return Long.class;
        else if (tag == FxTag.FLOAT)
            return Float.class;
        else if (tag == FxTag.DOUBLE)
            return Double.class;
        else if (tag == FxTag.STRING)
            return String.class;
        else if (tag == FxTag.BOOLEAN)
            return Boolean.class;
        else
            return null;
    }

    private void invokeSetup(FxInvokable invokable) throws Exception
    {
        invokable.callSetup(environment);
    }

    public ArgsEvaluator getArgsEval()
    {
        return argsEval;
    }

    public Storage getStorage()
    {
        return storage;
    }
}
