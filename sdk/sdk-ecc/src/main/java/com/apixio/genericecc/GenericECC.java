package com.apixio.genericecc;

import java.net.URI;
import java.util.List;

import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.EccInit;
import com.apixio.sdk.FxInvokable;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.ReturnedValueHandler;
import com.apixio.sdk.ecc.EccSystem;
import com.apixio.sdk.logging.DefaultLogger;
import com.apixio.sdk.logging.LoggerFactory;

// this is representative of the LambdaECC code that needs to be generic and needs to
// be able to deal with all the f(x)s without having to have code change, etc.

public class GenericECC
{
    private EccSystem      eccSystem;
    private EccEnvironment eccEnv;

    public GenericECC(ConfigSet daoConfig, ConfigSet loggerConfig, EccInit init) throws Exception
    {
        FxLogger logger = (loggerConfig != null) ? LoggerFactory.createLogger(loggerConfig) : new DefaultLogger();

        eccEnv    = new EccEnvironment(DaoServicesSet.createDaoServices(daoConfig), logger);
        eccSystem = new EccSystem(eccEnv);

        // unfortunately we have to poke in things:
        eccEnv.setExecutor(eccSystem);    // because of circular dep:  EccSystem is executor but requires Environment
        logger.setEnvironment(eccEnv);    // because core ECC doesn't deal with configuration

        eccSystem.initialize(init);
    }

    public List<FxInvokable> getInvokables()
    {
        return eccSystem.getFxs();
    }

    /**
     * Invoke the process method on the given implClass using reflection to build the parameter
     * list and to actually invoke.  The building of the parameter list is non-trivial as it
     * uses the FxTypes of the parameters to determine if the data needs to be loaded from
     * persistent storage or fetched via a data accessor, etc.
     */
    public Object invokeFx(FxInvokable invokable, FxRequest request) throws Exception
    {
        return eccSystem.invokeFx(invokable, request);
    }

    public void invokeFx(FxInvokable invokable, FxRequest request, ReturnedValueHandler rvh) throws Exception
    {
        eccSystem.invokeFx(invokable, request, rvh);
    }

    public URI persistFxOutput(FxInvokable invokable, FxRequest request, Object o) throws Exception
    {
        return eccSystem.persistOutput(invokable, request, o);
    }

    public List<Object> restoreOutput(URI dataURI, String datatype) throws Exception
    {
        return eccSystem.restoreOutput(dataURI, datatype);
    }

    /*
    public T restoreFxData(Class<T> clz, byte[] saved)
    {
        //?? question is how does client (and who is client??) get Class<> obj?  that's part of
        // dynamic loading of stuff, but i need to figure that out.

        //?? need to deal with iterability here as we know from experience that large lists of
        // protobuf Messages can cause heap problems

        //!! ApxDataDao required registration of DataType as it groups together the following:
        //   Class<T>, unique identifier for this type for storage purposes.  it seems like this
        //   might be able to be replaced by clz.getCanonicalName().
        //
        // look for "parseFrom" method on clz and invoke:
        //
        //  return clz.invoke("parseFrom", saved);   


    }
    */

}
