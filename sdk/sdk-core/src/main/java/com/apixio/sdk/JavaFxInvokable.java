package com.apixio.sdk;
import java.lang.reflect.Method;
import java.util.List;

import com.apixio.sdk.protos.FxProtos.FxImpl;
import com.apixio.sdk.protos.FxProtos.FxType;

public class JavaFxInvokable<T> extends FxInvokable<T> {
    private Class<T> implClass;    // as loaded from impl URI and classloader
    private T        implInstance; // the single instance created and ready to use for invocations
    private Method   fxMethod;     // cache of Method ready for invocation
    
    public JavaFxInvokable(FxLogger logger, FxImpl impl, Class<T> implClass, T instance, Method fx)
    {
        super(logger, impl);
        this.implClass    = implClass;
        this.implInstance = instance;
        this.fxMethod     = fx;
    }


    public Class<T> getImplClass()
    {
        return implClass;
    }
    public T getImplInstance()
    {
        return implInstance;
    }

    public Class<?>[] getParameterTypes()
    {
        return fxMethod.getParameterTypes();
    }

        /**
     *
     */
    public void callSetup(FxEnvironment env) throws Exception
    {
        if (implInstance instanceof FxImplementation)
        {
            FxImplementation impl = new FxImplLogWrapper((FxImplementation) implInstance, fxImpl.getEntryName());

            impl.setEnvironment(env);  // !! TODO somehow include FxImpl info (like MCID, etc.) for logging, etc.
            impl.setAssets(fxImpl.getAssets());
        }
    }

    public Object invokeInner(List<FxType> argTypes, Object[] args) throws Exception {
        Object rv = fxMethod.invoke(implInstance, args);
        return rv;
    }

}
